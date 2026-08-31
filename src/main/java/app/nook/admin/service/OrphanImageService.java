package app.nook.admin.service;

import app.nook.admin.dto.OrphanScanResult;
import app.nook.book.repository.BookRepository;
import app.nook.global.config.R2Properties;
import app.nook.r2.policy.ImageStoragePolicy;
import app.nook.record.repository.RecordImageRepository;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 고아 이미지 정리 — R2 에 존재하나 DB 어디에서도 미참조 이미지 탐색 및 삭제
 * <p>
 * 안전장치
 * - 생성 후 {@link #SAFETY_WINDOW} 이내 객체 제외 (업로드 직후 DB 커밋 대기 파일 보호)
 * - 관리 대상 이미지 타입 prefix 밖 객체 제외
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanImageService {

    /** 해당 시간 이내 생성 객체 삭제 제외 — "업로드 중/커밋 대기" 간주 */
    private static final Duration SAFETY_WINDOW = Duration.ofHours(24);

    /** S3 DeleteObjects 배치 최대 크기 */
    private static final int DELETE_BATCH_SIZE = 1000;

    private final S3Client s3Client;
    private final R2Properties r2;
    private final UserRepository userRepository;
    private final RecordImageRepository recordImageRepository;
    private final BookRepository bookRepository;

    /** dry-run: 삭제하지 않고 고아 목록만 반환 */
    public OrphanScanResult scan() {
        List<S3Object> scanned = listManagedObjects();
        List<String> orphans = findOrphanKeys(scanned);
        return new OrphanScanResult(scanned.size(), orphans.size(), orphans, false);
    }

    /** 고아 이미지를 실제로 S3 에서 삭제 */
    public OrphanScanResult deleteOrphans() {
        List<S3Object> scanned = listManagedObjects();
        List<String> orphans = findOrphanKeys(scanned);

        int deleted = 0;
        for (Map.Entry<String, List<String>> entry : groupKeysByBucket(orphans).entrySet()) {
            List<String> bucketKeys = entry.getValue();
            for (int i = 0; i < bucketKeys.size(); i += DELETE_BATCH_SIZE) {
                List<String> batch = bucketKeys.subList(
                        i,
                        Math.min(i + DELETE_BATCH_SIZE, bucketKeys.size())
                );
                deleted += deleteBatch(entry.getKey(), batch);
            }
        }
        log.info("[ORPHAN_CLEANUP] 스캔 {}건 중 고아 {}건, 삭제 {}건", scanned.size(), orphans.size(), deleted);
        return new OrphanScanResult(scanned.size(), deleted, List.of(), true);
    }

    // 최대 1000개 단위 배치 삭제, 삭제 성공 수 반환
    private int deleteBatch(String bucketName, List<String> keys) {
        try {
            List<ObjectIdentifier> objects = keys.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .toList();
            DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objects).build())
                    .build());
            response.errors().forEach(error ->
                    log.warn("[ORPHAN_CLEANUP] 삭제 실패 key={}, code={}", error.key(), error.code()));
            return keys.size() - response.errors().size();
        } catch (Exception e) {
            log.warn("[ORPHAN_CLEANUP] 배치 삭제 실패 size={}", keys.size(), e);
            return 0;
        }
    }

    // 테스트를 위해 package-private (24h 안전창 + known 필터 로직 직접 검증)
    List<String> findOrphanKeys(List<S3Object> objects) {
        Set<String> known = collectKnownKeys();
        Instant cutoff = Instant.now().minus(SAFETY_WINDOW);

        List<String> orphans = new ArrayList<>();
        for (S3Object obj : objects) {
            if (known.contains(obj.key())) {
                continue;
            }
            // 최근 업로드(커밋 대기 가능성) 보호
            if (obj.lastModified() != null && obj.lastModified().isAfter(cutoff)) {
                continue;
            }
            orphans.add(obj.key());
        }
        return orphans;
    }

    private Set<String> collectKnownKeys() {
        Set<String> known = new HashSet<>();
        known.addAll(userRepository.findAllProfileImageKeys());
        known.addAll(recordImageRepository.findAllKeys());
        known.addAll(bookRepository.findAllCoverImageKeys());
        return known;
    }

    private List<S3Object> listManagedObjects() {
        List<S3Object> all = new ArrayList<>();
        for (ImageStoragePolicy storagePolicy : ImageStoragePolicy.values()) {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(storagePolicy.bucketName(r2))
                    .prefix(storagePolicy.prefix())
                    .build();
            s3Client.listObjectsV2Paginator(request)
                    .contents()
                    .forEach(all::add);
        }
        return all;
    }

    private Map<String, List<String>> groupKeysByBucket(List<String> keys) {
        Map<String, List<String>> keysByBucket = new LinkedHashMap<>();
        for (String key : keys) {
            String bucketName = ImageStoragePolicy.fromKey(key).bucketName(r2);
            keysByBucket.computeIfAbsent(bucketName, ignored -> new ArrayList<>()).add(key);
        }
        return keysByBucket;
    }
}
