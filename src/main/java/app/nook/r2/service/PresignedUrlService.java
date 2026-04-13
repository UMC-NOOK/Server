package app.nook.r2.service;

import app.nook.r2.dto.ImageUploadRequestDto;
import app.nook.r2.dto.ImageUrlResponseDto;
import app.nook.global.exception.CustomException;
import app.nook.global.response.FileErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {


    // 만료 시간 필수 지정
    private static final int EXPIRATION_SECONDS = 300;
    // 현재 업로드 정책은 PNG만 지원한다.
    private static final String FIXED_IMAGE_MIME_TYPE = "image/png";
    private static final String FIXED_IMAGE_EXTENSION = ".png";
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L;
    private static final int MAX_FILE_COUNT = 5;

    private final S3Presigner s3Presigner;

    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    private static final Set<String> ALLOWED = Set.of(
            "book", "profile", "record"
    );


    // presigned 다운로드
    public ImageUrlResponseDto getPresignedUrl(String key) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(EXPIRATION_SECONDS))
                .getObjectRequest(objectRequest)
                .build();
        return new ImageUrlResponseDto(
                s3Presigner.presignGetObject(presignRequest).url().toString(),
                key
        );
    }

    // 업로드 - 단건
    public ImageUrlResponseDto generateUploadUrl(Long userId, ImageUploadRequestDto requestDto){
        validateUserId(userId);
        NormalizedUpload normalized = validateAndNormalizeUploadRequest(requestDto);
        String key = buildObjectKey(userId, normalized.contentType(), normalized.extension());
        return generateUploadUrl(key);
    }

    // 키 기반으로 presigned 업로드 url을 생성
    private ImageUrlResponseDto generateUploadUrl(String key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(FIXED_IMAGE_MIME_TYPE)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(request)
                .signatureDuration(Duration.ofSeconds(EXPIRATION_SECONDS))
                .build();
        return new ImageUrlResponseDto(
                s3Presigner.presignPutObject(presignRequest).url().toString(),
                key
        );
    }

    // 조회
    public String getImageUrl(Long userId, String key) {
        validateOwnedImageKey(userId, key, null);
        return getPresignedUrl(key).imageUrl();
    }

    public String resolveImageUrl(Long userId, String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) {
            return keyOrUrl;
        }
        try {
            return getImageUrl(userId, keyOrUrl);
        } catch (CustomException e) {
            if (e.getErrorCode() == FileErrorCode.INVALID_FILE_TYPE) {
                return keyOrUrl;
            }
            throw e;
        }
    }

    public void validateOwnedImageKey(Long userId, String key, String expectedContentType) {
        validateUserId(userId);
        ParsedKey parsedKey = parseKey(key);

        if (expectedContentType != null && !expectedContentType.equals(parsedKey.contentType())) {
            throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
        }
        if (!exists(key)) {
            throw new CustomException(FileErrorCode.FILE_NOT_FOUND);
        }
        validateFileSizeByMetadata(key);
        if (!isOwnedByUser(userId, parsedKey.ownerUserId())) {
            throw new CustomException(FileErrorCode.FILE_NOT_FOUND);
        }
    }


    // 삭제
     public void deleteFile(String key) {
         // 파일 존재 여부 확인
         if (!exists(key)) {
             throw new CustomException(FileErrorCode.FILE_NOT_FOUND);
         }

         // 파일 삭제
         DeleteObjectRequest request = DeleteObjectRequest.builder()
                 .bucket(bucketName)
                 .key(key)
                 .build();
         s3Client.deleteObject(request);
     }

     // 파일 수정 - 업로드된 기존 이미지를 삭제하고 새 이미지 업로드
     public ImageUrlResponseDto updateFile(Long userId, String key, ImageUploadRequestDto requestDto) {
         validateUserId(userId);
         deleteFile(key);
         return generateUploadUrl(userId, requestDto);
     }

     // 사진 여러장 업로드
     public List<ImageUrlResponseDto> generateMultipleUploadUrls(Long userId, List<ImageUploadRequestDto> requestDtos) {
         validateUserId(userId);
         if (requestDtos == null || requestDtos.isEmpty()) {
             return List.of();
         }
         validateUploadRequests(requestDtos);

         return requestDtos.stream()
                 .map(requestDto -> {
                     NormalizedUpload normalized = validateAndNormalizeUploadRequest(requestDto);
                     String key = buildObjectKey(userId, normalized.contentType(), normalized.extension());
                     return generateUploadUrl(key);
                 })
                 .toList();
     }


    /*  Util, 검증 메서드 */

    // 타입/확장자 정규화 + 검증
    private NormalizedUpload validateAndNormalizeUploadRequest(ImageUploadRequestDto requestDto) {
        if (requestDto == null) {
            throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
        }
        String normalizedContentType = normalizeAndValidateContentType(requestDto.contentType());
        String extension = FIXED_IMAGE_EXTENSION;
        return new NormalizedUpload(normalizedContentType, extension);
    }

    private void validateUploadRequests(List<ImageUploadRequestDto> requestDtos) {
        if (requestDtos == null || requestDtos.isEmpty()) {
            return;
        }

        if (requestDtos.size() > MAX_FILE_COUNT) {
            throw new CustomException(FileErrorCode.FILE_NUM_EXCEEDED);
        }

        requestDtos.forEach(this::validateAndNormalizeUploadRequest);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
        }
    }

    // 타입 정규화, 검증
    private String normalizeAndValidateContentType(String contentType) {
        if (contentType == null) {
            throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(normalized)) {
            throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
        }
        return normalized;
    }

     // 고유 키 생성
     private String buildObjectKey(Long userId, String contentType, String extension) {
         String uuid = UUID.randomUUID().toString();
         return contentType + "/users/" + userId + "/" + uuid + extension;
     }

    // 파일 존재 여부 확인 - 메타데이터 확인
    public boolean exists(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(request);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    // S3 메타데이터 기반 파일 크기 검증
    private void validateFileSizeByMetadata(String key) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        HeadObjectResponse response = s3Client.headObject(request);
        Long contentLength = response.contentLength();
        if (contentLength != null && contentLength > MAX_FILE_SIZE_BYTES) {
            throw new CustomException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    // key에서 타입 파싱
    private ParsedKey parseKey(String key) {
        if (key == null || key.isBlank()) {
            throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
        }

        String[] parts = key.split("/");
        if (parts.length == 4 && ALLOWED.contains(parts[0]) && "users".equals(parts[1])) {
            Long ownerUserId;
            try {
                ownerUserId = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
            }
            return new ParsedKey(parts[0], ownerUserId);
        }

        throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
    }

    private boolean isOwnedByUser(Long userId, Long ownerUserId) {
        if (userId == null || ownerUserId == null) {
            return false;
        }
        return userId.equals(ownerUserId);
    }

    private record NormalizedUpload(String contentType, String extension) {}
    private record ParsedKey(String contentType, Long ownerUserId) {}

}
