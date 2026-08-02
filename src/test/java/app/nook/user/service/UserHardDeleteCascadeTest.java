package app.nook.user.service;

import app.nook.book.domain.Book;
import app.nook.book.domain.BookViewHistory;
import app.nook.book.domain.SearchHistory;
import app.nook.book.domain.enums.SearchType;
import app.nook.book.domain.enums.SourceType;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.QueryDslConfig;
import app.nook.library.domain.Library;
import app.nook.record.domain.Record;
import app.nook.record.domain.RecordImage;
import app.nook.record.domain.enums.Emotion;
import app.nook.timeline.domain.Timeline;
import app.nook.timeline.domain.enums.TimelineType;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회원 hard delete cascade 통합 테스트
 * <p>
 * WithdrawService.hardDelete 는 userRepository.delete 후 연관 데이터 삭제를 DB FK ON DELETE CASCADE 에 의존
 * → User row 삭제만으로 서재/기록/이미지/타임라인/검색·조회 기록이 전부 삭제되는지 실DB(Testcontainers)로 검증
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class UserHardDeleteCascadeTest extends AbstractPostgresContainerTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("회원 삭제 시 DB ON DELETE CASCADE 로 연관 데이터가 전부 삭제되고 책은 보존된다")
    void deleteUser_연관데이터_전부삭제() {
        // given - 유저 1명과 그에 딸린 모든 연관 데이터
        User user = persistUser("cascade@test.com", "cascade-provider");
        Book book = persistBook("캐스케이드 테스트", "작가");
        Library library = persistLibrary(user, book);
        Record record = persistRecord(library, "기록");
        persistRecordImage(record, "record/users/1/img.png");
        persistTimeline(library);
        persistSearchHistory(user);
        persistBookViewHistory(user, book);
        em.flush();
        em.clear();

        // sanity - 실제로 저장됐는지 확인
        assertThat(count("Library")).isEqualTo(1L);
        assertThat(count("Record")).isEqualTo(1L);
        assertThat(count("RecordImage")).isEqualTo(1L);
        assertThat(count("Timeline")).isEqualTo(1L);
        assertThat(count("SearchHistory")).isEqualTo(1L);
        assertThat(count("BookViewHistory")).isEqualTo(1L);

        // when - WithdrawService.hardDelete 와 동일하게 userRepository.delete
        userRepository.delete(userRepository.findById(user.getId()).orElseThrow());
        em.flush();
        em.clear();

        // then - user 및 모든 연관 데이터 삭제
        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(count("Library")).isZero();
        assertThat(count("Record")).isZero();
        assertThat(count("RecordImage")).isZero();
        assertThat(count("Timeline")).isZero();
        assertThat(count("SearchHistory")).isZero();
        assertThat(count("BookViewHistory")).isZero();

        // 책(Book)은 회원 삭제와 무관하게 보존
        assertThat(count("Book")).isEqualTo(1L);
    }

    private long count(String entity) {
        return em.getEntityManager()
                .createQuery("select count(e) from " + entity + " e", Long.class)
                .getSingleResult();
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

    private Record persistRecord(Library library, String content) {
        Record record = Record.create(library, Emotion.FUN, content);
        em.persist(record);
        return record;
    }

    private void persistRecordImage(Record record, String key) {
        RecordImage image = RecordImage.builder()
                .record(record)
                .key(key)
                .orderIndex(0)
                .build();
        em.persist(image);
    }

    private void persistTimeline(Library library) {
        Timeline timeline = Timeline.builder()
                .library(library)
                .type(TimelineType.RECORD)
                .targetId(1L)
                .occurredAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .previewText("미리보기")
                .build();
        em.persist(timeline);
    }

    private void persistSearchHistory(User user) {
        SearchHistory history = SearchHistory.builder()
                .keyword("검색어")
                .searchType(SearchType.GLOBAL)
                .user(user)
                .build();
        em.persist(history);
    }

    private void persistBookViewHistory(User user, Book book) {
        BookViewHistory history = BookViewHistory.builder()
                .user(user)
                .book(book)
                .build();
        em.persist(history);
    }
}
