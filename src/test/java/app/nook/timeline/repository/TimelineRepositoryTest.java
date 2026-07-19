package app.nook.timeline.repository;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.QueryDslConfig;
import app.nook.library.domain.Library;
import app.nook.library.repository.LibraryRepository;
import app.nook.timeline.domain.Timeline;
import app.nook.timeline.domain.enums.TimelineType;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.repository.UserRepository;
import app.nook.book.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class TimelineRepositoryTest extends AbstractPostgresContainerTests {

    @Autowired
    private TimelineRepository timelineRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    private User saveUser(String email, String providerId) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .nickName(email)
                        .provider("GOOGLE")
                        .providerId(providerId)
                        .role(UserRole.USER)
                        .build()
        );
    }

    private Book saveBook(String isbn13, String title) {
        return bookRepository.save(
                Book.builder()
                        .isbn13(isbn13)
                        .title(title)
                        .author("작가")
                        .sourceType(SourceType.ALADIN)
                        .build()
        );
    }

    private Library saveLibrary(User user, Book book) {
        return libraryRepository.save(
                Library.builder()
                        .user(user)
                        .book(book)
                        .build()
        );
    }

    private Timeline saveTimeline(
            Library library,
            TimelineType type,
            Long targetId,
            LocalDateTime occurredAt,
            String previewText
    ) {
        Timeline timeline = Timeline.builder()
                .library(library)
                .type(type)
                .targetId(targetId)
                .occurredAt(occurredAt)
                .previewText(previewText)
                .build();
        return timelineRepository.save(timeline);
    }

    @Test
    void findByLibraryOrderByOccurredAtDescIdDesc_최신순으로_조회한다() {
        User user = saveUser("timeline-repo@test.com", "provider-1");
        Book book = saveBook("1111111111111", "도서1");
        Library library = saveLibrary(user, book);

        Timeline oldest = saveTimeline(
                library,
                TimelineType.REGISTER,
                library.getId(),
                LocalDateTime.of(2025, 12, 30, 10, 0),
                "서재에 등록했어요"
        );
        saveTimeline(
                library,
                TimelineType.STATUS,
                library.getId(),
                LocalDateTime.of(2025, 12, 31, 10, 0),
                "독서 상태 변경: READING"
        );
        Timeline newest = saveTimeline(
                library,
                TimelineType.RECORD,
                9001L,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                "기록 preview"
        );

        List<Timeline> result = timelineRepository.findByLibraryOrderByOccurredAtDescIdDesc(library);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(newest.getId());
        assertThat(result.get(1).getType()).isEqualTo(TimelineType.STATUS);
        assertThat(result.get(2).getId()).isEqualTo(oldest.getId());
        assertThat(result)
                .extracting(Timeline::getType)
                .contains(TimelineType.STATUS);
    }

    @Test
    void findTop5ByLibraryOrderByOccurredAtDescIdDesc_최신_5건만_조회한다() {
        User user = saveUser("timeline-top5@test.com", "provider-top5");
        Book book = saveBook("2222222222222", "도서2");
        Library library = saveLibrary(user, book);

        for (int i = 0; i < 6; i++) {
            saveTimeline(
                    library,
                    TimelineType.RECORD,
                    9000L + i,
                    LocalDateTime.of(2026, 1, 1 + i, 10, 0),
                    "preview-" + i
            );
        }

        List<Timeline> result = timelineRepository.findTop5ByLibraryOrderByOccurredAtDescIdDesc(library);

        assertThat(result).hasSize(5);
        assertThat(result)
                .extracting(Timeline::getOccurredAt)
                .isSortedAccordingTo((left, right) -> right.compareTo(left));
    }

    @Test
    void findByIdAndLibrary_같은_서재면_조회된다() {
        User user = saveUser("timeline-find@test.com", "provider-3");
        Book book = saveBook("3333333333333", "도서3");
        Library library = saveLibrary(user, book);

        Timeline timeline = saveTimeline(
                library,
                TimelineType.REGISTER,
                library.getId(),
                LocalDateTime.of(2025, 12, 30, 10, 0),
                "서재에 등록했어요"
        );

        Optional<Timeline> found = timelineRepository.findByIdAndLibrary(timeline.getId(), library);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(timeline.getId());
    }

    @Test
    void findByIdAndLibrary_다른_서재면_조회되지_않는다() {
        User user = saveUser("timeline-other@test.com", "provider-4");
        Book book1 = saveBook("4444444444444", "도서4");
        Book book2 = saveBook("5555555555555", "도서5");
        Library library1 = saveLibrary(user, book1);
        Library library2 = saveLibrary(user, book2);

        Timeline timeline = saveTimeline(
                library1,
                TimelineType.REGISTER,
                library1.getId(),
                LocalDateTime.of(2025, 12, 30, 10, 0),
                "서재에 등록했어요"
        );

        Optional<Timeline> found = timelineRepository.findByIdAndLibrary(timeline.getId(), library2);

        assertThat(found).isEmpty();
    }
}
