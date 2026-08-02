package app.nook.admin.service;

import app.nook.book.repository.BookRepository;
import app.nook.global.config.R2Properties;
import app.nook.record.repository.RecordImageRepository;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrphanImageServiceTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private R2Properties r2;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RecordImageRepository recordImageRepository;
    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private OrphanImageService orphanImageService;

    @Test
    void DB에없고_24시간_지난_객체만_고아로_판정한다() {
        // known: DB가 참조 중인 key
        given(userRepository.findAllProfileImageKeys()).willReturn(List.of("profile/users/1/known.jpg"));
        given(recordImageRepository.findAllKeys()).willReturn(List.of("record/users/1/known-record.jpg"));
        given(bookRepository.findAllCoverImageKeys()).willReturn(List.of());

        Instant old = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant recent = Instant.now().minus(1, ChronoUnit.HOURS);

        S3Object known = S3Object.builder()
                .key("profile/users/1/known.jpg").lastModified(old).build();
        S3Object orphanOld = S3Object.builder()
                .key("record/users/1/orphan-old.jpg").lastModified(old).build();
        S3Object orphanRecent = S3Object.builder()
                .key("record/users/1/orphan-recent.jpg").lastModified(recent).build();

        List<String> orphans = orphanImageService.findOrphanKeys(
                List.of(known, orphanOld, orphanRecent));

        assertThat(orphans).containsExactly("record/users/1/orphan-old.jpg");
        // known(참조중)은 제외, 최근 업로드(24h 이내)는 커밋 대기 보호로 제외
        assertThat(orphans).doesNotContain(
                "profile/users/1/known.jpg", "record/users/1/orphan-recent.jpg");
    }
}
