package app.nook.book.service;

import app.nook.book.exception.FileErrorCode;
import app.nook.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService();
        ReflectionTestUtils.setField(storageService, "coverUploadDir", tempDir.resolve("covers").toString());
        ReflectionTestUtils.setField(storageService, "profileUploadDir", tempDir.resolve("profiles").toString());
        ReflectionTestUtils.setField(storageService, "coverUrlPrefix", "/uploads/covers/");
        ReflectionTestUtils.setField(storageService, "profileUrlPrefix", "/uploads/profiles/");
    }

    @Test
    @DisplayName("표지 업로드 실패 - 허용되지 않은 확장자(txt)")
    void uploadCover_fail_invalidExtension_txt() {
        MockMultipartFile file = new MockMultipartFile(
                "coverImage", "cover.txt", "text/plain", "dummy".getBytes()
        );

        CustomException ex = assertThrows(CustomException.class, () -> storageService.uploadCover(file));

        assertThat(ex.getErrorCode()).isEqualTo(FileErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    @DisplayName("프로필 업로드 실패 - 확장자 없음")
    void uploadProfile_fail_noExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "profileImage", "profile", "image/png", "dummy".getBytes()
        );

        CustomException ex = assertThrows(CustomException.class, () -> storageService.uploadProfile(file));

        assertThat(ex.getErrorCode()).isEqualTo(FileErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    @DisplayName("표지 업로드 성공 - png")
    void uploadCover_success_png() {
        MockMultipartFile file = new MockMultipartFile(
                "coverImage", "cover.png", "image/png", "dummy".getBytes()
        );

        String url = storageService.uploadCover(file);

        assertThat(url).startsWith("/uploads/covers/");
        String fileName = url.substring("/uploads/covers/".length());
        Path saved = tempDir.resolve("covers").resolve(fileName);
        assertThat(Files.exists(saved)).isTrue();
    }
}
