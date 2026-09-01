package app.nook.r2.policy;

import app.nook.global.config.R2Properties;
import app.nook.global.exception.CustomException;
import app.nook.global.response.FileErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStoragePolicyTest {

    private final R2Properties r2 = new R2Properties(
            "https://account.r2.cloudflarestorage.com",
            "public-bucket",
            "private-bucket",
            "access-key",
            "secret-key",
            "https://public.r2.dev"
    );

    @Test
    void book과_profile은_public_버킷을_사용한다() {
        assertThat(ImageStoragePolicy.BOOK.bucketName(r2)).isEqualTo("public-bucket");
        assertThat(ImageStoragePolicy.PROFILE.bucketName(r2)).isEqualTo("public-bucket");
        assertThat(ImageStoragePolicy.BOOK.isPubliclyReadable()).isTrue();
        assertThat(ImageStoragePolicy.PROFILE.isPubliclyReadable()).isTrue();
    }

    @Test
    void record는_private_버킷을_사용한다() {
        assertThat(ImageStoragePolicy.RECORD.bucketName(r2)).isEqualTo("private-bucket");
        assertThat(ImageStoragePolicy.RECORD.isPubliclyReadable()).isFalse();
    }

    @Test
    void key의_첫_경로로_저장_정책을_찾는다() {
        assertThat(ImageStoragePolicy.fromKey("profile/users/1/image.png"))
                .isEqualTo(ImageStoragePolicy.PROFILE);
        assertThat(ImageStoragePolicy.fromKey("record/users/1/image.png"))
                .isEqualTo(ImageStoragePolicy.RECORD);
    }

    @Test
    void 지원하지_않는_이미지_타입은_거부한다() {
        assertThatThrownBy(() -> ImageStoragePolicy.fromUploadType("unknown"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(FileErrorCode.INVALID_FILE_TYPE));
    }
}
