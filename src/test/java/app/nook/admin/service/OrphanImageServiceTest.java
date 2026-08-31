package app.nook.admin.service;

import app.nook.admin.dto.OrphanScanResult;
import app.nook.book.repository.BookRepository;
import app.nook.global.config.R2Properties;
import app.nook.record.repository.RecordImageRepository;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @Test
    void 이미지_타입별_버킷과_prefix를_분리해서_스캔한다() {
        given(r2.publicBucketName()).willReturn("public-bucket");
        given(r2.privateBucketName()).willReturn("private-bucket");
        given(userRepository.findAllProfileImageKeys()).willReturn(List.of());
        given(recordImageRepository.findAllKeys()).willReturn(List.of());
        given(bookRepository.findAllCoverImageKeys()).willReturn(List.of());

        ListObjectsV2Iterable paginator = org.mockito.Mockito.mock(ListObjectsV2Iterable.class);
        SdkIterable<S3Object> emptyObjects = Collections::emptyIterator;
        given(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .willReturn(paginator);
        given(paginator.contents()).willReturn(emptyObjects);

        orphanImageService.scan();

        ArgumentCaptor<ListObjectsV2Request> captor =
                ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3Client, times(3)).listObjectsV2Paginator(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ListObjectsV2Request::bucket, ListObjectsV2Request::prefix)
                .containsExactlyInAnyOrder(
                        tuple("public-bucket", "book/"),
                        tuple("public-bucket", "profile/"),
                        tuple("private-bucket", "record/")
                );
    }

    @Test
    void 고아_이미지를_버킷별로_그룹화해서_삭제한다() {
        givenStorageConfiguration();
        givenEmptyReferencedKeys();

        S3Object book = oldObject("book/users/1/book.jpg");
        S3Object profile = oldObject("profile/users/1/profile.jpg");
        S3Object record = oldObject("record/users/1/record.jpg");
        givenObjectsByPrefix(List.of(book), List.of(profile), List.of(record));
        given(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .willReturn(DeleteObjectsResponse.builder().build());

        OrphanScanResult result = orphanImageService.deleteOrphans();

        ArgumentCaptor<DeleteObjectsRequest> captor =
                ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client, times(2)).deleteObjects(captor.capture());
        assertThat(captor.getAllValues()).anySatisfy(request -> {
            assertThat(request.bucket()).isEqualTo("public-bucket");
            assertThat(request.delete().objects())
                    .extracting(object -> object.key())
                    .containsExactlyInAnyOrder(book.key(), profile.key());
        });
        assertThat(captor.getAllValues()).anySatisfy(request -> {
            assertThat(request.bucket()).isEqualTo("private-bucket");
            assertThat(request.delete().objects())
                    .extracting(object -> object.key())
                    .containsExactly(record.key());
        });
        assertThat(result.scanned()).isEqualTo(3);
        assertThat(result.orphanCount()).isEqualTo(3);
        assertThat(result.orphanKeys()).isEmpty();
        assertThat(result.deleted()).isTrue();
    }

    @Test
    void R2_삭제_요청이_실패하면_삭제_성공_건수를_0으로_반환한다() {
        givenStorageConfiguration();
        givenEmptyReferencedKeys();

        S3Object record = oldObject("record/users/1/record.jpg");
        givenObjectsByPrefix(List.of(), List.of(), List.of(record));
        given(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .willThrow(S3Exception.builder().statusCode(500).message("R2 unavailable").build());

        OrphanScanResult result = orphanImageService.deleteOrphans();

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.orphanCount()).isZero();
        assertThat(result.orphanKeys()).isEmpty();
        assertThat(result.deleted()).isTrue();
    }

    private void givenStorageConfiguration() {
        given(r2.publicBucketName()).willReturn("public-bucket");
        given(r2.privateBucketName()).willReturn("private-bucket");
    }

    private void givenEmptyReferencedKeys() {
        given(userRepository.findAllProfileImageKeys()).willReturn(List.of());
        given(recordImageRepository.findAllKeys()).willReturn(List.of());
        given(bookRepository.findAllCoverImageKeys()).willReturn(List.of());
    }

    private void givenObjectsByPrefix(
            List<S3Object> bookObjects,
            List<S3Object> profileObjects,
            List<S3Object> recordObjects
    ) {
        ListObjectsV2Iterable bookPaginator = paginatorWith(bookObjects);
        ListObjectsV2Iterable profilePaginator = paginatorWith(profileObjects);
        ListObjectsV2Iterable recordPaginator = paginatorWith(recordObjects);
        given(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .willAnswer(invocation -> {
                    ListObjectsV2Request request = invocation.getArgument(0);
                    return switch (request.prefix()) {
                        case "book/" -> bookPaginator;
                        case "profile/" -> profilePaginator;
                        case "record/" -> recordPaginator;
                        default -> throw new IllegalArgumentException("Unexpected prefix: " + request.prefix());
                    };
                });
    }

    private ListObjectsV2Iterable paginatorWith(List<S3Object> objects) {
        ListObjectsV2Iterable paginator = mock(ListObjectsV2Iterable.class);
        SdkIterable<S3Object> contents = objects::iterator;
        given(paginator.contents()).willReturn(contents);
        return paginator;
    }

    private S3Object oldObject(String key) {
        return S3Object.builder()
                .key(key)
                .lastModified(Instant.EPOCH)
                .build();
    }
}
