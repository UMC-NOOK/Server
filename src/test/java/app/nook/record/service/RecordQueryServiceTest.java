package app.nook.record.service;

import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.global.dto.CursorResponse;
import app.nook.global.exception.CustomException;
import app.nook.global.fixture.UserFixture;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.r2.service.PresignedUrlService;
import app.nook.record.converter.RecordConverter;
import app.nook.record.domain.Record;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.domain.enums.SortType;
import app.nook.record.dto.BookRecordDto;
import app.nook.record.dto.RecordListCursor;
import app.nook.record.repository.RecordRepository;
import app.nook.record.util.RecordListCursorCodec;
import app.nook.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordQueryService 테스트")
class RecordQueryServiceTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private RecordConverter recordConverter;

    @Mock
    private PresignedUrlService presignedUrlService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LibraryRepository libraryRepository;

    @InjectMocks
    private RecordQueryService recordQueryService;

    @Nested
    @DisplayName("기록 목록 조회")
    class GetUserRecords {

        @Test
        @DisplayName("성공 - 도서 표지 key를 응답용 URL로 변환한다")
        void 기록목록_조회_key를_응답용_URL로_반환() {
            // given
            User user = UserFixture.user();

            BookRecordDto.BookRecordItemDto item = createMockBookRecordItem(
                    10L,
                    "테스트 도서",
                    3L,
                    LocalDateTime.of(2026, 4, 13, 10, 0)
            );

            given(recordRepository.findRecordsByCursor(1L, null, SortType.RECENT_RECORDED, 10))
                    .willReturn(List.of(item));
            given(presignedUrlService.resolveImageUrl(1L, "cover.png"))
                    .willReturn("https://r2.example.com/cover.png");

            // when
            CursorResponse<BookRecordDto.BookRecordItemDto, String> result =
                    recordQueryService.getUserRecords(user, 10, null, SortType.RECENT_RECORDED);

            // then
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).coverImageUrl())
                    .isEqualTo("https://r2.example.com/cover.png");
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            verify(presignedUrlService).resolveImageUrl(1L, "cover.png");
        }

        @Test
        @DisplayName("성공 - RECENT_RECORDED 정렬의 다음 커서는 bookId와 lastCreatedDate로 구성된다")
        void 최신순_정렬_커서구성() {
            // given
            User user = UserFixture.user();
            int size = 1;
            LocalDateTime lastDate = LocalDateTime.of(2024, 1, 15, 12, 0);

            List<BookRecordDto.BookRecordItemDto> mockItems = List.of(
                    createMockBookRecordItem(10L, "책", 5L, lastDate),
                    createMockBookRecordItem(20L, "책2", 3L, lastDate.minusDays(1))
            );

            given(recordRepository.findRecordsByCursor(
                    eq(1L), any(RecordListCursor.class), eq(SortType.RECENT_RECORDED), eq(size)))
                    .willReturn(mockItems);
            given(presignedUrlService.resolveImageUrl(1L, "cover.png"))
                    .willReturn("https://r2.example.com/cover.png");

            // when
            CursorResponse<BookRecordDto.BookRecordItemDto, String> result =
                    recordQueryService.getUserRecords(
                            user,
                            size,
                            new RecordListCursor(null, null, null),
                            SortType.RECENT_RECORDED
                    );

            // then
            RecordListCursor nextCursor = RecordListCursorCodec.decode(result.nextCursor());
            assertThat(nextCursor.bookId()).isEqualTo(10L);
            assertThat(nextCursor.lastCreatedDate()).isEqualTo(lastDate);
            assertThat(nextCursor.lastCount()).isNull();
        }

        @Test
        @DisplayName("성공 - RECORD_COUNT_DESC 정렬의 다음 커서는 recordCount와 bookId로 구성된다")
        void 기록많은순_정렬_커서구성() {
            // given
            User user = UserFixture.user();
            int size = 1;
            LocalDateTime baseTime = LocalDateTime.of(2026, 4, 24, 10, 0);

            List<BookRecordDto.BookRecordItemDto> mockItems = List.of(
                    createMockBookRecordItem(10L, "책", 5L, baseTime),
                    createMockBookRecordItem(20L, "책2", 3L, baseTime.minusDays(1))
            );

            given(recordRepository.findRecordsByCursor(
                    eq(1L), any(RecordListCursor.class), eq(SortType.RECORD_COUNT_DESC), eq(size)))
                    .willReturn(mockItems);
            given(presignedUrlService.resolveImageUrl(1L, "cover.png"))
                    .willReturn("https://r2.example.com/cover.png");

            // when
            CursorResponse<BookRecordDto.BookRecordItemDto, String> result =
                    recordQueryService.getUserRecords(
                            user,
                            size,
                            new RecordListCursor(null, null, null),
                            SortType.RECORD_COUNT_DESC
                    );

            // then
            RecordListCursor nextCursor = RecordListCursorCodec.decode(result.nextCursor());
            assertThat(nextCursor.lastCount()).isEqualTo(5L);
            assertThat(nextCursor.bookId()).isEqualTo(10L);
            assertThat(nextCursor.lastCreatedDate()).isNull();
        }
    }

    @Nested
    @DisplayName("RecordListCursor 인코딩")
    class RecordListCursorCodecTest {

        @Test
        @DisplayName("성공 - encode 후 decode 하면 동일한 커서를 얻는다")
        void 인코딩_디코딩_왕복() {
            // given
            RecordListCursor original = new RecordListCursor(
                    null,
                    10L,
                    LocalDateTime.of(2024, 1, 1, 0, 0)
            );

            // when
            String encoded = RecordListCursorCodec.encode(original);
            RecordListCursor decoded = RecordListCursorCodec.decode(encoded);

            // then
            assertThat(decoded.bookId()).isEqualTo(original.bookId());
            assertThat(decoded.lastCreatedDate()).isEqualTo(original.lastCreatedDate());
        }

        @Test
        @DisplayName("성공 - null 커서를 encode하면 null을 반환한다")
        void null_커서_인코딩() {
            assertThat(RecordListCursorCodec.encode(null)).isNull();
        }

        @Test
        @DisplayName("실패 - 잘못된 커서 문자열이면 예외를 던진다")
        void 잘못된_커서_디코딩() {
            assertThrows(CustomException.class,
                    () -> RecordListCursorCodec.decode("invalid!!"));
        }
    }

    @Nested
    @DisplayName("기록 감상별 개수 조회")
    class GetRecordEmotionCounts {

        @Test
        @DisplayName("성공 - 감상별 개수를 반환한다")
        void 감상별_개수_조회() {
            // given
            User user = UserFixture.user();
            Long bookId = 10L;
            List<BookRecordDto.RecordEmotionDto> emotionCounts = List.of(
                    new BookRecordDto.RecordEmotionDto("FUN", 5L),
                    new BookRecordDto.RecordEmotionDto("SAD", 3L),
                    new BookRecordDto.RecordEmotionDto(null, 2L)
            );

            given(bookRepository.existsById(bookId)).willReturn(true);
            given(libraryRepository.existsByUserIdAndBookId(user.getId(), bookId)).willReturn(true);
            given(recordRepository.countRecordsByEmotion(1L, bookId))
                    .willReturn(new BookRecordDto.RecordEmotionCountResponse(10L, emotionCounts));

            // when
            BookRecordDto.RecordEmotionCountResponse result = recordQueryService.getRecordEmotionCounts(user, bookId);

            // then
            assertThat(result.totalCount()).isEqualTo(10L);
            assertThat(result.emotionCounts()).hasSize(Emotion.values().length + 1);
            assertThat(result.emotionCounts().get(0).emotion()).isEqualTo("ALL");
            assertThat(result.emotionCounts().get(0).recordCount()).isEqualTo(10L);
            assertThat(result.emotionCounts().get(1).emotion()).isEqualTo("FUN");
            assertThat(result.emotionCounts().get(1).recordCount()).isEqualTo(5L);
            assertThat(result.emotionCounts().get(2).emotion()).isEqualTo("EMPATHIZING");
            assertThat(result.emotionCounts().get(2).recordCount()).isZero();
            assertThat(result.emotionCounts().get(5).emotion()).isEqualTo("SAD");
            assertThat(result.emotionCounts().get(5).recordCount()).isEqualTo(3L);
            assertThat(result.emotionCounts().get(6).emotion()).isEqualTo("UNCOMFORTABLE");
            assertThat(result.emotionCounts().get(6).recordCount()).isZero();
            assertThat(result.emotionCounts().get(7).emotion()).isEqualTo("EMPTY");
            assertThat(result.emotionCounts().get(7).recordCount()).isEqualTo(2L);
            verify(recordRepository).countRecordsByEmotion(1L, bookId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 책이면 예외를 던진다")
        void 감상별_개수_조회_실패_책없음() {
            // given
            User user = UserFixture.user();
            given(bookRepository.existsById(10L)).willReturn(false);

            // when
            CustomException exception = assertThrows(
                    CustomException.class,
                    () -> recordQueryService.getRecordEmotionCounts(user, 10L)
            );

            // then
            assertThat(exception.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 서재에 없는 책이면 예외를 던진다")
        void 감상별_개수_조회_실패_서재없음() {
            // given
            User user = UserFixture.user();
            given(bookRepository.existsById(10L)).willReturn(true);
            given(libraryRepository.existsByUserIdAndBookId(user.getId(), 10L)).willReturn(false);

            // when
            CustomException exception = assertThrows(
                    CustomException.class,
                    () -> recordQueryService.getRecordEmotionCounts(user, 10L)
            );

            // then
            assertThat(exception.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
        }
    }

    @Nested
    @DisplayName("도서별 기록 조회")
    class GetBookRecords {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("성공 - 감정 필터를 적용한 도서별 기록을 반환한다")
            void 도서별_기록조회_감정필터적용() {
                // given
                User user = UserFixture.user();
                List<Record> records = createMockRecords(Emotion.FUN, Emotion.SAD);

                given(bookRepository.existsById(10L)).willReturn(true);
                given(libraryRepository.existsByUserIdAndBookId(user.getId(), 10L)).willReturn(true);
                given(recordRepository.findBookRecordsByCursor(1L, 10L, null, Emotion.FUN, 10))
                        .willReturn(List.of(records.get(0)));
                given(recordConverter.toRecordItemDto(1L, records.get(0)))
                        .willReturn(new BookRecordDto.RecordItemDto(
                                1L,
                                "내용 0",
                                List.of(),
                                List.of(),
                                Emotion.FUN,
                                LocalDate.of(2026, 4, 24)
                        ));

                // when
                CursorResponse<BookRecordDto.RecordItemDto, Long> result =
                        recordQueryService.getBookRecords(user, 10L, 10, null, "FUN");

                // then
                assertThat(result.items()).hasSize(1);
                assertThat(result.items().get(0).emotion()).isEqualTo(Emotion.FUN);
                assertThat(result.nextCursor()).isNull();
                assertThat(result.hasNext()).isFalse();
            }

            @Test
            @DisplayName("성공 - ALL 감정 필터는 모든 기록을 반환한다")
            void 도서별_기록조회_ALL감정필터() {
                // given
                User user = UserFixture.user();
                List<Record> records = createMockRecords(Emotion.FUN, Emotion.SAD);
                given(bookRepository.existsById(10L)).willReturn(true);
                given(libraryRepository.existsByUserIdAndBookId(user.getId(), 10L)).willReturn(true);
                given(recordRepository.findBookRecordsByCursor(1L, 10L, null, null, 10))
                        .willReturn(records);
                given(recordConverter.toRecordItemDto(1L, records.get(0)))
                        .willReturn(new BookRecordDto.RecordItemDto(
                                1L,
                                "내용 0",
                                List.of(),
                                List.of(),
                                Emotion.FUN,
                                LocalDate.of(2026, 4, 24)
                        ));
                given(recordConverter.toRecordItemDto(1L, records.get(1)))
                        .willReturn(new BookRecordDto.RecordItemDto(
                                2L,
                                "내용 1",
                                List.of(),
                                List.of(),
                                Emotion.SAD,
                                LocalDate.of(2026, 4, 25)
                        ));

                // when
                CursorResponse<BookRecordDto.RecordItemDto, Long> result =
                        recordQueryService.getBookRecords(user, 10L, 10, null, "ALL");

                // then
                assertThat(result.items()).hasSize(2);
                assertThat(result.items().get(0).emotion()).isEqualTo(Emotion.FUN);
                assertThat(result.items().get(1).emotion()).isEqualTo(Emotion.SAD);
                assertThat(result.nextCursor()).isNull();
                assertThat(result.hasNext()).isFalse();
            }

            @Test
            @DisplayName("성공 - 다음 페이지가 있으면 hasNext=true와 nextCursor를 반환한다")
            void 도서별_기록조회_다음페이지존재() {
                // given
                User user = UserFixture.user();
                List<Record> records = createMockRecords(Emotion.FUN, Emotion.SAD);

                given(bookRepository.existsById(10L)).willReturn(true);
                given(libraryRepository.existsByUserIdAndBookId(user.getId(), 10L)).willReturn(true);
                given(recordRepository.findBookRecordsByCursor(1L, 10L, null, null, 1))
                        .willReturn(List.of(records.get(0), records.get(1)));
                given(recordConverter.toRecordItemDto(1L, records.get(0)))
                        .willReturn(new BookRecordDto.RecordItemDto(
                                1L,
                                "내용 0",
                                List.of(),
                                List.of(),
                                Emotion.FUN,
                                LocalDate.of(2026, 4, 24)
                        ));

                // when
                CursorResponse<BookRecordDto.RecordItemDto, Long> result =
                        recordQueryService.getBookRecords(user, 10L, 1, null, "ALL");

                // then
                assertThat(result.items()).hasSize(1);
                assertThat(result.hasNext()).isTrue();
                assertThat(result.nextCursor()).isEqualTo(1L);
            }

            @Test
            @DisplayName("성공 - 감정 필터가 null이면 전체 기록을 반환한다")
            void 도서별_기록조회_감정필터_null() {
                // given
                User user = UserFixture.user();
                List<Record> records = createMockRecords(Emotion.FUN, Emotion.SAD);

                given(bookRepository.existsById(10L)).willReturn(true);
                given(libraryRepository.existsByUserIdAndBookId(user.getId(), 10L)).willReturn(true);
                given(recordRepository.findBookRecordsByCursor(1L, 10L, null, null, 10))
                        .willReturn(records);
                given(recordConverter.toRecordItemDto(1L, records.get(0)))
                        .willReturn(new BookRecordDto.RecordItemDto(
                                1L,
                                "내용 0",
                                List.of(),
                                List.of(),
                                Emotion.FUN,
                                LocalDate.of(2026, 4, 24)
                        ));
                given(recordConverter.toRecordItemDto(1L, records.get(1)))
                        .willReturn(new BookRecordDto.RecordItemDto(
                                2L,
                                "내용 1",
                                List.of(),
                                List.of(),
                                Emotion.SAD,
                                LocalDate.of(2026, 4, 25)
                        ));

                // when
                CursorResponse<BookRecordDto.RecordItemDto, Long> result =
                        recordQueryService.getBookRecords(user, 10L, 10, null, null);

                // then
                assertThat(result.items()).hasSize(2);
                assertThat(result.items().get(0).emotion()).isEqualTo(Emotion.FUN);
                assertThat(result.items().get(1).emotion()).isEqualTo(Emotion.SAD);
                assertThat(result.nextCursor()).isNull();
                assertThat(result.hasNext()).isFalse();
            }

            @Test
            @DisplayName("성공 - 감정 필터가 blank면 전체 기록을 반환한다")
            void 도서별_기록조회_감정필터_blank() {
                // given
                User user = UserFixture.user();
                List<Record> records = createMockRecords(Emotion.FUN, Emotion.SAD);

                given(bookRepository.existsById(10L)).willReturn(true);
                given(libraryRepository.existsByUserIdAndBookId(user.getId(), 10L)).willReturn(true);
                given(recordRepository.findBookRecordsByCursor(1L, 10L, null, null, 10))
                        .willReturn(records);
                given(recordConverter.toRecordItemDto(1L, records.get(0)))
                        .willReturn(new BookRecordDto.RecordItemDto(
                                1L,
                                "내용 0",
                                List.of(),
                                List.of(),
                                Emotion.FUN,
                                LocalDate.of(2026, 4, 24)
                        ));
                given(recordConverter.toRecordItemDto(1L, records.get(1)))
                        .willReturn(new BookRecordDto.RecordItemDto(
                                2L,
                                "내용 1",
                                List.of(),
                                List.of(),
                                Emotion.SAD,
                                LocalDate.of(2026, 4, 25)
                        ));

                // when
                CursorResponse<BookRecordDto.RecordItemDto, Long> result =
                        recordQueryService.getBookRecords(user, 10L, 10, null, " ");

                // then
                assertThat(result.items()).hasSize(2);
                assertThat(result.items().get(0).emotion()).isEqualTo(Emotion.FUN);
                assertThat(result.items().get(1).emotion()).isEqualTo(Emotion.SAD);
                assertThat(result.nextCursor()).isNull();
                assertThat(result.hasNext()).isFalse();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("실패 - 존재하지 않는 책이면 예외를 던진다")
            void 도서별_기록조회_실패_책없음() {
                // given
                User user = UserFixture.user();
                given(bookRepository.existsById(10L)).willReturn(false);

                // when
                CustomException exception = assertThrows(
                        CustomException.class,
                        () -> recordQueryService.getBookRecords(user, 10L, 10, null, "ALL")
                );

                // then
                assertThat(exception.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
            }

            @Test
            @DisplayName("실패 - 서재에 없는 책이면 예외를 던진다")
            void 도서별_기록조회_실패_서재없음() {
                // given
                User user = UserFixture.user();
                given(bookRepository.existsById(10L)).willReturn(true);
                given(libraryRepository.existsByUserIdAndBookId(user.getId(), 10L)).willReturn(false);

                // when
                CustomException exception = assertThrows(
                        CustomException.class,
                        () -> recordQueryService.getBookRecords(user, 10L, 10, null, "ALL")
                );

                // then
                assertThat(exception.getErrorCode()).isEqualTo(LibraryErrorCode.BOOK_NOT_EXIST);
            }

            @Test
            @DisplayName("실패 - 잘못된 감정 필터면 예외를 던진다")
            void 도서별_기록조회_실패_잘못된감정필터() {
                // given
                User user = UserFixture.user();
                given(bookRepository.existsById(10L)).willReturn(true);
                given(libraryRepository.existsByUserIdAndBookId(user.getId(), 10L)).willReturn(true);

                // when
                CustomException exception = assertThrows(
                        CustomException.class,
                        () -> recordQueryService.getBookRecords(user, 10L, 10, null, "invalid-emotion")
                );

                // then
                assertThat(exception.getErrorCode().getCode()).isEqualTo("COMMON-002");
            }

            @Test
            @DisplayName("성공 - EMPTY 감정으로 필터링한다")
            void 도서별_기록조회_성공_empty감정필터() {
                // given
                User user = UserFixture.user();
                given(bookRepository.existsById(10L)).willReturn(true);
                given(libraryRepository.existsByUserIdAndBookId(user.getId(), 10L)).willReturn(true);

                // when
                recordQueryService.getBookRecords(user, 10L, 10, null, "EMPTY");

                // then
                verify(recordRepository).findBookRecordsByCursor(1L, 10L, null, Emotion.EMPTY, 10);
            }
        }
    }


    // 기록 생성용 Fixture 클래스
    private List<Record> createMockRecords(Emotion... emotions) {
        return IntStream.range(0, emotions.length)
                .mapToObj(index -> {
                    Record record = Record.create(null, emotions[index], "내용 " + index);
                    ReflectionTestUtils.setField(record, "id", (long) (index + 1));
                    return record;
                })
                .toList();
    }

    private BookRecordDto.BookRecordItemDto createMockBookRecordItem(
            Long bookId,
            String title,
            Long recordCount,
            LocalDateTime lastCreatedDate
    ) {
        return new BookRecordDto.BookRecordItemDto(
                bookId,
                bookId * 10,
                title,
                "저자",
                "내용",
                "cover.png",
                recordCount,
                lastCreatedDate
        );
    }
}
