package app.nook.book.service;

import app.nook.book.exception.FileErrorCode;
import app.nook.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    // 로컬 개발 환경에서 허용하는 표지 확장자
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("jpg", "jpeg", "png", "gif", "webp");

    // TODO: S3 전환 전까지 로컬 디렉터리에 저장
    @Value("${file.cover-upload-dir:uploads/covers}")
    private String coverUploadDir;

    @Value("${file.profile-upload-dir:uploads/profiles}")
    private String profileUploadDir;

    @Override
    public String uploadCover(MultipartFile file) {
        return upload(file, coverUploadDir);
    }

    @Override
    public String uploadProfile(MultipartFile file) {
        return upload(file, profileUploadDir);
    }

    @Override
    public void deleteCoverByUrl(String fileUrl) {
        deleteByUrl(fileUrl, coverUploadDir);
    }

    @Override
    public void deleteProfileByUrl(String fileUrl) {
        deleteByUrl(fileUrl, profileUploadDir);
    }

    private String upload(MultipartFile file, String uploadDir) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String ext = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_CONTENT_TYPES.contains(ext)) {
            throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
        }

        try {
            String urlPrefix = "/" + uploadDir + "/";
            Path dirPath = Paths.get(uploadDir);
            Files.createDirectories(dirPath);

            String fileName = UUID.randomUUID() + "." + ext;
            Path targetPath = dirPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return urlPrefix + fileName;
        } catch (IOException e) {
            log.error("[FILE_UPLOAD_FAILED] originalFilename={}", file.getOriginalFilename(), e);
            throw new CustomException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }


    private void deleteByUrl(String fileUrl, String baseDir) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        String marker = "/" + baseDir + "/";

        int idx = fileUrl.indexOf(marker);
        if (idx < 0) {
            return;
        }

        String fileName = fileUrl.substring(idx + marker.length());

        // 부적절한 경로 차단
        if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            log.warn("[FILE_DELETE_REJECTED] invalid fileName extracted from url. fileUrl={}", fileUrl);
            return;
        }

        // 경로 정규화 + 루트 이탈 방지
        Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(fileName).normalize();

        if (!targetPath.startsWith(basePath)) {
            log.warn("[FILE_DELETE_REJECTED] path traversal detected. fileUrl={}, targetPath={}", fileUrl, targetPath);
            return;
        }

        try {
            boolean deleted = Files.deleteIfExists(targetPath);
            if (!deleted) {
                log.info("[FILE_DELETE_SKIPPED] file not found. path={}", targetPath);
            }
        } catch (IOException e) {
            log.warn("[FILE_DELETE_FAILED] path={}", targetPath, e);
        }
    }

    private String extractExtension(String originalFilename) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename : "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}

