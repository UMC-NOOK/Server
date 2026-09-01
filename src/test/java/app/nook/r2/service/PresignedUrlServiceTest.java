package app.nook.r2.service;

import app.nook.global.config.R2Properties;
import app.nook.r2.dto.ImageUploadRequestDto;
import app.nook.r2.dto.ImageUrlResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PresignedUrlServiceTest {

    @Mock
    private S3Presigner s3Presigner;
    @Mock
    private S3Client s3Client;
    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;
    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private PresignedUrlService presignedUrlService;

    @BeforeEach
    void setUp() {
        R2Properties r2 = new R2Properties(
                "https://account.r2.cloudflarestorage.com",
                "public-bucket",
                "private-bucket",
                "access-key",
                "secret-key",
                "https://public.r2.dev"
        );
        presignedUrlService = new PresignedUrlService(s3Presigner, s3Client, r2);
    }

    @Test
    void profile_업로드_URL은_public_버킷으로_발급한다() throws MalformedURLException {
        given(presignedPutObjectRequest.url())
                .willReturn(URI.create("https://upload.example.com/profile.png").toURL());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willReturn(presignedPutObjectRequest);

        ImageUrlResponseDto result = presignedUrlService.generateUploadUrl(
                1L,
                new ImageUploadRequestDto("profile", "image/png")
        );

        ArgumentCaptor<PutObjectPresignRequest> captor =
                ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        assertThat(captor.getValue().putObjectRequest().bucket()).isEqualTo("public-bucket");
        assertThat(result.key()).startsWith("profile/users/1/").endsWith(".png");
    }

    @Test
    void record_업로드_URL은_private_버킷으로_발급한다() throws MalformedURLException {
        given(presignedPutObjectRequest.url())
                .willReturn(URI.create("https://upload.example.com/record.png").toURL());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willReturn(presignedPutObjectRequest);

        presignedUrlService.generateUploadUrl(
                1L,
                new ImageUploadRequestDto("record", "image/png")
        );

        ArgumentCaptor<PutObjectPresignRequest> captor =
                ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        assertThat(captor.getValue().putObjectRequest().bucket()).isEqualTo("private-bucket");
    }

    @Test
    void profile_조회는_public_CDN_URL을_반환한다() {
        String result = presignedUrlService.resolveImageUrl(
                1L,
                "profile/users/1/profile.png"
        );

        assertThat(result).isEqualTo(
                "https://public.r2.dev/profile/users/1/profile.png"
        );
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void record_조회는_private_버킷의_presigned_URL을_반환한다() throws MalformedURLException {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder().contentLength(1024L).build());
        given(presignedGetObjectRequest.url())
                .willReturn(URI.create("https://download.example.com/record.png?signature=test").toURL());
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .willReturn(presignedGetObjectRequest);

        String result = presignedUrlService.resolveImageUrl(
                1L,
                "record/users/1/record.png"
        );

        ArgumentCaptor<GetObjectPresignRequest> captor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("private-bucket");
        assertThat(result).contains("signature=test");
    }
}
