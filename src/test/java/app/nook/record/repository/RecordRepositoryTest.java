package app.nook.record.repository;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.QueryDslConfig;
import app.nook.library.domain.Library;
import app.nook.record.domain.Record;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.domain.enums.SortType;
import app.nook.record.dto.BookRecordDto;
import app.nook.record.dto.RecordListCursor;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class RecordRepositoryTest extends AbstractPostgresContainerTests {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private TestEntityManager em;

    @Nested
    @DisplayName("기본 조회")
    class BasicQuery {

        @Test
        @DisplayName("서재와 사용자 기준으로 기록 개수를 센다")
        void countByLibraryIdAndUserId_개수조회() {
            // given
            User user = persistUser("count-library@test.com", "count-library-provider");
            Book book = persistBook("개수 조회 테스트", "작가");
            Library library = persistLibrary(user, book);
            persistRecord(library, Emotion.FUN, "기록1",
                    LocalDateTime.of(2026, 4, 1, 10, 0));
            persistRecord(library, Emotion.SAD, "기록2",
                    LocalDateTime.of(2026, 4, 2, 10, 0));
            em.flush();
            em.clear();

            // when
            long result = recordRepository.countByLibraryIdAndUserId(library.getId(), user.getId());

            // then
            assertThat(result).isEqualTo(2L);
        }

        @Test
        @DisplayName("사용자 기준으로 전체 기록 개수를 센다")
        void countByUserId_개수조회() {
            // given
            User user = persistUser("count-user@test.com", "count-user-provider");
            User otherUser = persistUser("count-other@test.com", "count-other-provider");
            Book firstBook = persistBook("첫 번째 책", "작가A");
            Book secondBook = persistBook("두 번째 책", "작가B");
            Library firstLibrary = persistLibrary(user, firstBook);
            Library secondLibrary = persistLibrary(user, secondBook);
            Library otherLibrary = persistLibrary(otherUser, firstBook);

            persistRecord(firstLibrary, Emotion.FUN, "기록1",
                    LocalDateTime.of(2026, 4, 1, 10, 0));
            persistRecord(secondLibrary, Emotion.SAD, "기록2",
                    LocalDateTime.of(2026, 4, 2, 10, 0));
            persistRecord(otherLibrary, Emotion.USEFUL, "다른 사용자 기록",
                    LocalDateTime.of(2026, 4, 3, 10, 0));
            em.flush();
            em.clear();

            // when
            long result = recordRepository.countByUserId(user.getId());

            // then
            assertThat(result).isEqualTo(2L);
        }

        @Test
        @DisplayName("서재 ID 기준으로 최신 기록을 조회한다")
        void findRecentByLibraryId_최신순조회() {
            // given
            User user = persistUser("recent@test.com", "recent-provider");
            Book book = persistBook("최신 기록 테스트", "작가");
            Library library = persistLibrary(user, book);
            Record oldest = persistRecord(library, Emotion.FUN, "오래된 기록",
                    LocalDateTime.of(2026, 4, 1, 10, 0));
            Record newest = persistRecord(library, Emotion.SAD, "최신 기록",
                    LocalDateTime.of(2026, 4, 3, 10, 0));
            em.flush();
            em.clear();

            // when
            List<Record> result = recordRepository.findRecentByLibraryId(
                    library.getId(),
                    PageRequest.of(0, 1)
            );

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(newest.getId());
            assertThat(result.get(0).getId()).isNotEqualTo(oldest.getId());
        }
    }

    @Nested
    @DisplayName("QueryDSL 조회")
    class QueryDslQuery {

        @Test
        @DisplayName("사용자 기록을 감정별로 집계한다")
        void countRecordsByEmotion_감정별집계() {
            // given
            User user = persistUser("emotion@test.com", "emotion-provider");
            User otherUser = persistUser("other-emotion@test.com", "other-emotion-provider");
            Book book = persistBook("감정 집계 테스트", "작가");
            Library library = persistLibrary(user, book);
            Library otherLibrary = persistLibrary(otherUser, book);

            persistRecord(library, Emotion.FUN, "재미1", LocalDateTime.of(2026, 4, 1, 10, 0));
            persistRecord(library, Emotion.FUN, "재미2", LocalDateTime.of(2026, 4, 2, 10, 0));
            persistRecord(library, Emotion.SAD, "슬픔", LocalDateTime.of(2026, 4, 3, 10, 0));
            persistRecord(library, Emotion.EMPTY, "감정 미입력", LocalDateTime.of(2026, 4, 4, 10, 0));
            Book otherBook = persistBook("다른 책", "다른 작가");
            Library otherBookLibrary = persistLibrary(user, otherBook);
            persistRecord(otherBookLibrary, Emotion.FUN, "다른 책 기록", LocalDateTime.of(2026, 4, 3, 11, 0));
            persistRecord(otherLibrary, Emotion.USEFUL, "다른 사용자", LocalDateTime.of(2026, 4, 4, 10, 0));
            em.flush();
            em.clear();

            // when
            BookRecordDto.RecordEmotionCountResponse result = recordRepository.countRecordsByEmotion(user.getId(), book.getId());

            // then
            assertThat(result.totalCount()).isEqualTo(4L);
            assertThat(result.emotionCounts()).hasSize(3);
            assertThat(result.emotionCounts())
                    .anySatisfy(item -> {
                        assertThat(item.emotion()).isEqualTo("FUN");
                        assertThat(item.recordCount()).isEqualTo(2L);
                    })
                    .anySatisfy(item -> {
                        assertThat(item.emotion()).isEqualTo("SAD");
                        assertThat(item.recordCount()).isEqualTo(1L);
                    })
                    .anySatisfy(item -> {
                        assertThat(item.emotion()).isEqualTo("EMPTY");
                        assertThat(item.recordCount()).isEqualTo(1L);
                    });
        }

        @Test
        @DisplayName("한 책에 기록이 2개 이상이어도 책별로 1건씩, 최신 기록 content로 그룹핑한다")
        void findRecordsByCursor_책당_기록_다수() {
            // given - 한 책에 기록 3개 (기존에는 content 서브쿼리가 여러 row를 반환해 예외 발생)
            User user = persistUser("cursor@test.com", "cursor-provider");
            Book book = persistBook("커서 그룹핑 테스트", "작가");
            Library library = persistLibrary(user, book);
            persistRecord(library, Emotion.FUN, "오래된 기록",
                    LocalDateTime.of(2026, 4, 1, 10, 0));
            persistRecord(library, Emotion.SAD, "중간 기록",
                    LocalDateTime.of(2026, 4, 2, 10, 0));
            Record latestRecord = persistRecord(library, Emotion.USEFUL, "최신 기록",
                    LocalDateTime.of(2026, 4, 3, 10, 0));
            em.flush();
            em.clear();

            // when
            List<BookRecordDto.BookRecordItemDto> result = recordRepository.findRecordsByCursor(
                    user.getId(),
                    new RecordListCursor(null, null, null),
                    SortType.RECENT_RECORDED,
                    10
            );

            // then - 책 1권으로 묶이고 count=3, content는 가장 최신 기록
            assertThat(result).hasSize(1);
            assertThat(result.get(0).bookId()).isEqualTo(book.getId());
            assertThat(result.get(0).recordId()).isEqualTo(latestRecord.getId());
            assertThat(result.get(0).recordCount()).isEqualTo(3L);
            assertThat(result.get(0).recordContent()).isEqualTo("최신 기록");
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("기록을 삭제해도 소속 서재(Library)는 삭제되지 않는다")
        void deleteRecord_서재는_보존() {
            // given - 한 서재에 기록 2개
            User user = persistUser("delete@test.com", "delete-provider");
            Book book = persistBook("삭제 테스트", "작가");
            Library library = persistLibrary(user, book);
            Record target = persistRecord(library, Emotion.FUN, "삭제될 기록",
                    LocalDateTime.of(2026, 4, 1, 10, 0));
            Record survivor = persistRecord(library, Emotion.SAD, "남을 기록",
                    LocalDateTime.of(2026, 4, 2, 10, 0));
            em.flush();

            // when - 기록 1건 삭제
            recordRepository.delete(target);
            em.flush();
            em.clear();

            // then - 서재와 다른 기록은 그대로 남는다 (자식→부모 삭제 전파 없음)
            assertThat(em.find(Library.class, library.getId())).isNotNull();
            assertThat(recordRepository.findById(target.getId())).isEmpty();
            assertThat(recordRepository.findById(survivor.getId())).isPresent();
        }
    }

    private User persistUser(String email, String providerId) {
        User user = User.builder()
                .email(email)
                .nickName(email)
                .provider("GOOGLE")
                .providerId(providerId)
                .role(UserRole.USER)
                .build();
        em.persist(user);
        return user;
    }

    private Book persistBook(String title, String author) {
        String isbn13 = String.format("%013d", Math.floorMod(System.nanoTime(), 1_000_000_000_000L));
        Book book = Book.builder()
                .isbn13(isbn13)
                .title(title)
                .author(author)
                .coverImageKey("book/cover.png")
                .sourceType(SourceType.ALADIN)
                .build();
        em.persist(book);
        return book;
    }

    private Library persistLibrary(User user, Book book) {
        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        em.persist(library);
        return library;
    }

    private Record persistRecord(
            Library library,
            Emotion emotion,
            String content,
            LocalDateTime createdDate
    ) {
        Record record = Record.create(library, emotion, content);
        ReflectionTestUtils.setField(record, "createdDate", createdDate);
        ReflectionTestUtils.setField(record, "modifiedDate", createdDate);
        em.persist(record);
        return record;
    }
}
