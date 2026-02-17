package app.nook.book.service;

import app.nook.book.exception.BookErrorCode;
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
    @Value("${file.upload-dir:uploads/covers}")
    private String uploadDir;

    @Override
    public String uploadCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String ext = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_CONTENT_TYPES.contains(ext)) {
            throw new CustomException(BookErrorCode.INVALID_FILE_TYPE);
        }

        try {
            Path dirPath = Paths.get(uploadDir);
            Files.createDirectories(dirPath);

            String fileName = UUID.randomUUID() + "." + ext;
            Path targetPath = dirPath.resolve(fileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("[COVER_UPLOAD_SUCCESS] storedFileName={}", fileName);
            return "/uploads/covers/" + fileName;
        } catch (IOException e) {
            log.error("[COVER_UPLOAD_FAILED] originalFilename={}", file.getOriginalFilename(), e);
            throw new CustomException(BookErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void deleteByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        String marker = "/uploads/covers/";
        int idx = fileUrl.indexOf(marker);
        if (idx < 0) {
            return;
        }

        String fileName = fileUrl.substring(idx + marker.length());
        Path path = Paths.get(uploadDir).resolve(fileName);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private String extractExtension(String originalFilename) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename : "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new CustomException(BookErrorCode.INVALID_FILE_TYPE);
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}

