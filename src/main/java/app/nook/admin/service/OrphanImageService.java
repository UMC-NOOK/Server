package app.nook.admin.service;

import app.nook.admin.dto.OrphanScanResult;
import app.nook.book.repository.BookRepository;
import app.nook.global.config.R2Properties;
import app.nook.record.repository.RecordImageRepository;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 고아 이미지 정리 — R2 에는 존재하지만 DB 어디에서도 참조하지 않는 이미지를 찾아 삭제한다.
 * <p>
 * 안전장치:
 * - 업로드 직후 아직 DB 커밋이 안 된 파일은 고아처럼 보이므로, 생성 후 {@link #SAFETY_WINDOW}
 *   이내 객체는 무조건 제외한다.
 * - 관리 대상 prefix({@link #MANAGED_PREFIXES}) 밖의 객체는 손대지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanImageService {

    /** 키 포맷: {type}/users/{id}/{file}. PresignedUrlService 의 업로드 타입과 일치. */
    private static final List<String> MANAGED_PREFIXES = List.of("profile/", "record/", "book/");

    /** 이 시간보다 최근에 생성된 객체는 "업로드 중/커밋 대기" 로 보고 삭제 대상에서 제외 */
    private static final Duration SAFETY_WINDOW = Duration.ofHours(24);

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
        for (String key : orphans) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(r2.bucketName())
                        .key(key)
                        .build());
                deleted++;
            } catch (Exception e) {
                log.warn("[ORPHAN_CLEANUP] 삭제 실패 key={}", key, e);
            }
        }
        log.info("[ORPHAN_CLEANUP] 스캔 {}건 중 고아 {}건, 삭제 {}건", scanned.size(), orphans.size(), deleted);
        return new OrphanScanResult(scanned.size(), deleted, List.of(), true);
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
        for (String prefix : MANAGED_PREFIXES) {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(r2.bucketName())
                    .prefix(prefix)
                    .build();
            s3Client.listObjectsV2Paginator(request)
                    .contents()
                    .forEach(all::add);
        }
        return all;
    }
}
