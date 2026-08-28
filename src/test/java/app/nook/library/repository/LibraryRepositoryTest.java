package app.nook.library.repository;

import app.nook.book.domain.Book;
import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.repository.BookRepository;
import app.nook.book.repository.CategoryRepository;
import app.nook.focus.domain.Focus;
import app.nook.focus.domain.Theme;
import app.nook.focus.domain.enums.ThemeName;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.QueryDslConfig;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.LibrarySortType;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryBookCursor;
import app.nook.library.repository.dto.LibraryBookQueryResult;
import app.nook.library.repository.dto.LibraryStatusCount;
import app.nook.record.domain.Record;
import app.nook.record.domain.enums.Emotion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import app.nook.user.domain.User;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class LibraryRepositoryTest extends AbstractPostgresContainerTests {

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void findByUserAndBook_존재() {
        User user = User.builder()
                .email("repo@library.com")
                .nickName("repo")
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();
        userRepository.save(user);

        Book book = Book.builder()
                .isbn13("1234567890123")
                .title("테스트 도서")
                .author("테스트 작가")
                .sourceType(SourceType.ALADIN)
                .build();
        bookRepository.save(book);

        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        libraryRepository.save(library);

        Library found = libraryRepository.findByUserAndBook(user, book).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getBook().getId()).isEqualTo(book.getId());
    }

    @Test
    void findByUserAndBook_없음() {
        User user = User.builder()
                .email("missing@library.com")
                .nickName("missing")
                .provider("KAKAO")
                .providerId("provider-2")
                .build();
        userRepository.save(user);

        Book book = Book.builder()
                .isbn13("9999999999999")
                .title("없는 도서")
                .author("작가")
                .sourceType(SourceType.USER)
                .build();
        bookRepository.save(book);

        Optional<Library> found = libraryRepository.findByUserAndBook(user, book);

        assertThat(found).isEmpty();
    }

    @Test
    void findByStatusWithCursor_커서및정렬검증() {
        User user = User.builder()
                .email("cursor@library.com")
                .nickName("cursor")
                .provider("GOOGLE")
                .providerId("provider-3")
                .build();
        userRepository.save(user);

        Book book1 = Book.builder()
                .isbn13("1111111111111")
                .title("도서1")
                .author("작가1")
                .sourceType(SourceType.ALADIN)
                .build();
        bookRepository.save(book1);

        Book book2 = Book.builder()
                .isbn13("2222222222222")
                .title("도서2")
                .author("작가2")
                .sourceType(SourceType.ALADIN)
                .build();
        bookRepository.save(book2);

        Book book3 = Book.builder()
                .isbn13("3333333333333")
                .title("도서3")
                .author("작가3")
                .sourceType(SourceType.ALADIN)
                .build();
        bookRepository.save(book3);

        Library library1 = Library.builder().user(user).book(book1).build();
        ReflectionTestUtils.setField(library1, "readingStatus", ReadingStatus.READING);
        libraryRepository.save(library1);

        Library library2 = Library.builder().user(user).book(book2).build();
        ReflectionTestUtils.setField(library2, "readingStatus", ReadingStatus.READING);
        libraryRepository.save(library2);

        Library library3 = Library.builder().user(user).book(book3).build();
        ReflectionTestUtils.setField(library3, "readingStatus", ReadingStatus.READING);
        libraryRepository.save(library3);

        Long newestId = library3.getId();
        Long middleId = library2.getId();
        Long oldestId = library1.getId();

        Slice<Library> firstPage = libraryRepository.findByStatusWithCursor(
                user,
                ReadingStatus.READING,
                null,
                PageRequest.of(0, 2)
        );

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getContent().get(0).getId()).isEqualTo(newestId);
        assertThat(firstPage.getContent().get(1).getId()).isEqualTo(middleId);

        Slice<Library> nextPage = libraryRepository.findByStatusWithCursor(
                user,
                ReadingStatus.READING,
                middleId,
                PageRequest.of(0, 2)
        );

        assertThat(nextPage.getContent()).hasSize(1);
        assertThat(nextPage.getContent().get(0).getId()).isEqualTo(oldestId);
    }

    @Test
    void findByIdAndUserIdForUpdate_존재하면조회된다() {
        User user = User.builder()
                .email("lock@library.com")
                .nickName("lock")
                .provider("GOOGLE")
                .providerId("provider-lock")
                .build();
        userRepository.save(user);

        Book book = Book.builder()
                .isbn13("4444444444444")
                .title("락 테스트 도서")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .build();
        bookRepository.save(book);

        Library library = libraryRepository.save(Library.builder().user(user).book(book).build());

        var found = libraryRepository.findByIdAndUserIdForUpdate(library.getId(), user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(library.getId());
    }

    @Test
    void findByIdAndUserId_존재하면조회된다() {
        User user = User.builder()
                .email("findbyid@library.com")
                .nickName("findbyid")
                .provider("GOOGLE")
                .providerId("provider-find")
                .build();
        userRepository.save(user);

        Book book = Book.builder()
                .isbn13("5555555555555")
                .title("일반 조회 도서")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .build();
        bookRepository.save(book);

        Library saved = libraryRepository.save(Library.builder().user(user).book(book).build());

        var found = libraryRepository.findByIdAndUserId(saved.getId(), user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void countByUserIdGroupByReadingStatus_상태별개수를한번에반환한다() {
        User user = User.builder()
                .email("count-status@library.com")
                .nickName("count-status")
                .provider("GOOGLE")
                .providerId("provider-count-status")
                .build();
        userRepository.save(user);

        Book beforeBook = Book.builder()
                .isbn13("6666666666661")
                .title("before")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .build();
        Book readingBook = Book.builder()
                .isbn13("6666666666662")
                .title("reading")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .build();
        Book finishedBook = Book.builder()
                .isbn13("6666666666663")
                .title("finished")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .build();
        bookRepository.save(beforeBook);
        bookRepository.save(readingBook);
        bookRepository.save(finishedBook);

        Library beforeLibrary = Library.builder().user(user).book(beforeBook).build();
        ReflectionTestUtils.setField(beforeLibrary, "readingStatus", ReadingStatus.BEFORE);
        libraryRepository.save(beforeLibrary);

        Library readingLibrary = Library.builder().user(user).book(readingBook).build();
        ReflectionTestUtils.setField(readingLibrary, "readingStatus", ReadingStatus.READING);
        libraryRepository.save(readingLibrary);

        Library finishedLibrary = Library.builder().user(user).book(finishedBook).build();
        ReflectionTestUtils.setField(finishedLibrary, "readingStatus", ReadingStatus.FINISHED);
        libraryRepository.save(finishedLibrary);

        List<LibraryStatusCount> counts = libraryRepository.countByUserIdGroupByReadingStatus(user.getId());

        assertThat(counts).containsExactlyInAnyOrder(
                new LibraryStatusCount(ReadingStatus.BEFORE, 1L),
                new LibraryStatusCount(ReadingStatus.READING, 1L),
                new LibraryStatusCount(ReadingStatus.FINISHED, 1L)
        );
    }

    @Test
    void findAladinIsbnsByUserIdAndIsbnIn_서재보유ISBN만반환한다() {
        User user = User.builder()
                .email("isbn@library.com")
                .nickName("isbn")
                .provider("GOOGLE")
                .providerId("provider-isbn")
                .build();
        userRepository.save(user);

        Book ownedBook = Book.builder()
                .isbn13("7777777777777")
                .title("보유 도서")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .build();
        bookRepository.save(ownedBook);
        libraryRepository.save(Library.builder().user(user).book(ownedBook).build());
        Book userBook = Book.builder()
                .isbn13("8888888888888")
                .title("사용자 등록 도서")
                .author("작가")
                .sourceType(SourceType.USER)
                .createdByUserId(user.getId())
                .build();
        bookRepository.save(userBook);
        libraryRepository.save(Library.builder().user(user).book(userBook).build());

        var result = libraryRepository.findAladinIsbnsByUserIdAndIsbnIn(
                user.getId(),
                java.util.List.of("7777777777777", "8888888888888")
        );

        assertThat(result).containsExactly("7777777777777");
    }

    @Test
    void searchByUserIdAndKeyword_제목저자ISBN으로검색된다() {
        User user = User.builder()
                .email("search@library.com")
                .nickName("search")
                .provider("GOOGLE")
                .providerId("provider-search")
                .build();
        userRepository.save(user);

        Category category = categoryRepository.save(
                Category.of(MallType.BOOK, "소설", 100)
        );

        Book titleBook = Book.builder()
                .isbn13("9999999999991")
                .title("자바의 정석")
                .author("남궁성")
                .sourceType(SourceType.ALADIN)
                .category(category)
                .build();
        Book authorBook = Book.builder()
                .isbn13("9999999999992")
                .title("스프링 입문")
                .author("김영한")
                .sourceType(SourceType.ALADIN)
                .category(category)
                .build();
        Book isbnBook = Book.builder()
                .isbn13("1231231231231")
                .title("테스트")
                .author("익명")
                .sourceType(SourceType.ALADIN)
                .category(category)
                .build();
        bookRepository.save(titleBook);
        bookRepository.save(authorBook);
        bookRepository.save(isbnBook);
        libraryRepository.save(Library.builder().user(user).book(titleBook).build());
        libraryRepository.save(Library.builder().user(user).book(authorBook).build());
        libraryRepository.save(Library.builder().user(user).book(isbnBook).build());

        var titleResult = libraryRepository.searchByUserIdAndKeyword(user.getId(), "자바", PageRequest.of(0, 10));
        var authorResult = libraryRepository.searchByUserIdAndKeyword(user.getId(), "김영한", PageRequest.of(0, 10));
        var isbnResult = libraryRepository.searchByUserIdAndKeyword(user.getId(), "1231231231231", PageRequest.of(0, 10));

        assertThat(titleResult.getContent()).hasSize(1);
        assertThat(titleResult.getContent().get(0).getBook().getTitle()).isEqualTo("자바의 정석");
        assertThat(authorResult.getContent()).hasSize(1);
        assertThat(authorResult.getContent().get(0).getBook().getAuthor()).isEqualTo("김영한");
        assertThat(isbnResult.getContent()).hasSize(1);
        assertThat(isbnResult.getContent().get(0).getBook().getIsbn13()).isEqualTo("1231231231231");
    }

    @Test
    void countByUser_사용자서재총개수를반환한다() {
        User user = User.builder()
                .email("count@library.com")
                .nickName("count")
                .provider("GOOGLE")
                .providerId("provider-count")
                .build();
        userRepository.save(user);

        Book firstBook = Book.builder()
                .isbn13("1010101010101")
                .title("첫 번째")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .build();
        Book secondBook = Book.builder()
                .isbn13("1010101010102")
                .title("두 번째")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .build();
        bookRepository.save(firstBook);
        bookRepository.save(secondBook);
        libraryRepository.save(Library.builder().user(user).book(firstBook).build());
        libraryRepository.save(Library.builder().user(user).book(secondBook).build());

        int count = libraryRepository.countByUser(user);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findByUserIdAndReadingStatusOrderByIdDesc_최신순으로반환한다() {
        User user = User.builder()
                .email("before@library.com")
                .nickName("before")
                .provider("GOOGLE")
                .providerId("provider-before")
                .build();
        userRepository.save(user);

        Book oldBook = Book.builder()
                .isbn13("2020202020201")
                .title("오래된 도서")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .build();
        Book newBook = Book.builder()
                .isbn13("2020202020202")
                .title("최신 도서")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .build();
        bookRepository.save(oldBook);
        bookRepository.save(newBook);

        Library oldLibrary = Library.builder().user(user).book(oldBook).build();
        ReflectionTestUtils.setField(oldLibrary, "readingStatus", ReadingStatus.BEFORE);
        libraryRepository.save(oldLibrary);

        Library newLibrary = Library.builder().user(user).book(newBook).build();
        ReflectionTestUtils.setField(newLibrary, "readingStatus", ReadingStatus.BEFORE);
        libraryRepository.save(newLibrary);

        var result = libraryRepository.findByUserIdAndReadingStatusOrderByIdDesc(
                user.getId(),
                ReadingStatus.BEFORE,
                PageRequest.of(0, 5)
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(newLibrary.getId());
        assertThat(result.get(1).getId()).isEqualTo(oldLibrary.getId());
    }

    @Test
    void findTopAladinCategoryIdsByUserId_카테고리별집계내림차순반환() {
        User user = User.builder()
                .email("topcat@library.com")
                .nickName("topcat")
                .provider("GOOGLE")
                .providerId("provider-topcat")
                .build();
        userRepository.save(user);

        Category novel = categoryRepository.save(Category.of(MallType.BOOK, "테스트 소설", 1001));
        Category essay = categoryRepository.save(Category.of(MallType.BOOK, "테스트 에세이", 1002));

        Book novelBook1 = Book.builder()
                .isbn13("3030303030301")
                .title("소설1")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .category(novel)
                .build();
        Book novelBook2 = Book.builder()
                .isbn13("3030303030302")
                .title("소설2")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .category(novel)
                .build();
        Book essayBook = Book.builder()
                .isbn13("3030303030303")
                .title("에세이")
                .author("작가")
                .sourceType(SourceType.ALADIN)
                .category(essay)
                .build();
        bookRepository.save(novelBook1);
        bookRepository.save(novelBook2);
        bookRepository.save(essayBook);

        libraryRepository.save(Library.builder().user(user).book(novelBook1).build());
        libraryRepository.save(Library.builder().user(user).book(novelBook2).build());
        libraryRepository.save(Library.builder().user(user).book(essayBook).build());

        var result = libraryRepository.findTopAladinCategoryIdsByUserId(
                user.getId(),
                MallType.BOOK,
                PageRequest.of(0, 3)
        );

        assertThat(result).containsExactly(1001, 1002);
    }

    @Test
    void findAllBooksByCursor_최근포커스순_정렬과커서조건을검증한다() {
        User user = saveUser("recent-focus@library.com", "provider-recent-focus");
        User otherUser = saveUser("recent-focus-other@library.com", "provider-recent-focus-other");
        Theme theme = saveTheme();

        Library oldFocused = saveLibrary(user, "최근 포커스 오래됨");
        Library recentFocused = saveLibrary(user, "최근 포커스 최신");
        Library noFocused = saveLibrary(user, "포커스 없음");
        Library sameTimeFocused = saveLibrary(user, "최근 포커스 동일시각");
        Library otherUserLibrary = saveLibrary(otherUser, "다른 사용자 포커스");

        LocalDateTime oldEndedAt = LocalDateTime.of(2026, 5, 1, 21, 0);
        LocalDateTime recentEndedAt = LocalDateTime.of(2026, 5, 3, 21, 0);
        saveFocus(oldFocused, theme, oldEndedAt);
        saveFocus(recentFocused, theme, recentEndedAt);
        saveFocus(sameTimeFocused, theme, recentEndedAt);
        saveFocus(otherUserLibrary, theme, LocalDateTime.of(2026, 5, 4, 21, 0));
        em.flush();
        em.clear();

        List<LibraryBookQueryResult> firstPage = libraryRepository.findAllBooksByCursor(
                user.getId(),
                null,
                LibrarySortType.RECENT_FOCUSED,
                10
        );

        assertThat(firstPage)
                .extracting(LibraryBookQueryResult::libraryId)
                .containsExactly(
                        sameTimeFocused.getId(),
                        recentFocused.getId(),
                        oldFocused.getId(),
                        noFocused.getId()
                );

        LibraryBookCursor cursor = new LibraryBookCursor(recentFocused.getId(), recentEndedAt, null, null);
        List<LibraryBookQueryResult> nextPage = libraryRepository.findAllBooksByCursor(
                user.getId(),
                cursor,
                LibrarySortType.RECENT_FOCUSED,
                10
        );

        assertThat(nextPage)
                .extracting(LibraryBookQueryResult::libraryId)
                .containsExactly(oldFocused.getId(), noFocused.getId());
    }

    @Test
    void findAllBooksByCursor_기록많은순_정렬과커서조건을검증한다() {
        User user = saveUser("record-desc@library.com", "provider-record-desc");
        User otherUser = saveUser("record-desc-other@library.com", "provider-record-desc-other");

        Library twoRecordsLowId = saveLibrary(user, "기록 두 개 낮은 ID");
        Library twoRecordsHighId = saveLibrary(user, "기록 두 개 높은 ID");
        Library threeRecords = saveLibrary(user, "기록 세 개");
        Library noRecords = saveLibrary(user, "기록 없음");
        Library otherUserLibrary = saveLibrary(otherUser, "다른 사용자 기록 많음");

        saveRecords(twoRecordsLowId, 2);
        saveRecords(twoRecordsHighId, 2);
        saveRecords(threeRecords, 3);
        saveRecords(otherUserLibrary, 4);
        em.flush();
        em.clear();

        List<LibraryBookQueryResult> firstPage = libraryRepository.findAllBooksByCursor(
                user.getId(),
                null,
                LibrarySortType.RECORD_COUNT_DESC,
                10
        );

        assertThat(firstPage)
                .extracting(LibraryBookQueryResult::libraryId)
                .containsExactly(
                        threeRecords.getId(),
                        twoRecordsHighId.getId(),
                        twoRecordsLowId.getId(),
                        noRecords.getId()
                );
        assertThat(firstPage)
                .extracting(LibraryBookQueryResult::recordCount)
                .containsExactly(3L, 2L, 2L, 0L);

        LibraryBookCursor cursor = new LibraryBookCursor(twoRecordsHighId.getId(), null, 2L, null);
        List<LibraryBookQueryResult> nextPage = libraryRepository.findAllBooksByCursor(
                user.getId(),
                cursor,
                LibrarySortType.RECORD_COUNT_DESC,
                10
        );

        assertThat(nextPage)
                .extracting(LibraryBookQueryResult::libraryId)
                .containsExactly(twoRecordsLowId.getId(), noRecords.getId());
    }

    @Test
    void findAllBooksByCursor_기록적은순_정렬과커서조건을검증한다() {
        User user = saveUser("record-asc@library.com", "provider-record-asc");

        Library noRecords = saveLibrary(user, "기록 없음");
        Library oneRecord = saveLibrary(user, "기록 한 개");
        Library twoRecordsLowId = saveLibrary(user, "기록 두 개 낮은 ID");
        Library twoRecordsHighId = saveLibrary(user, "기록 두 개 높은 ID");
        Library threeRecords = saveLibrary(user, "기록 세 개");

        saveRecords(oneRecord, 1);
        saveRecords(twoRecordsLowId, 2);
        saveRecords(twoRecordsHighId, 2);
        saveRecords(threeRecords, 3);
        em.flush();
        em.clear();

        List<LibraryBookQueryResult> firstPage = libraryRepository.findAllBooksByCursor(
                user.getId(),
                null,
                LibrarySortType.RECORD_COUNT_ASC,
                10
        );

        assertThat(firstPage)
                .extracting(LibraryBookQueryResult::libraryId)
                .containsExactly(
                        noRecords.getId(),
                        oneRecord.getId(),
                        twoRecordsLowId.getId(),
                        twoRecordsHighId.getId(),
                        threeRecords.getId()
                );

        LibraryBookCursor cursor = new LibraryBookCursor(twoRecordsLowId.getId(), null, 2L, null);
        List<LibraryBookQueryResult> nextPage = libraryRepository.findAllBooksByCursor(
                user.getId(),
                cursor,
                LibrarySortType.RECORD_COUNT_ASC,
                10
        );

        assertThat(nextPage)
                .extracting(LibraryBookQueryResult::libraryId)
                .containsExactly(twoRecordsHighId.getId(), threeRecords.getId());
    }

    @Test
    void findAllBooksByCursor_가나다순_정렬과커서조건을검증한다() {
        User user = saveUser("alphabetical@library.com", "provider-alphabetical");
        User otherUser = saveUser("alphabetical-other@library.com", "provider-alphabetical-other");

        Library firstSameTitle = saveLibrary(user, "가나다");
        Library secondSameTitle = saveLibrary(user, "가나다");
        Library nextTitle = saveLibrary(user, "나나");
        Library lastTitle = saveLibrary(user, "다다");
        saveLibrary(otherUser, "가가");
        em.flush();
        em.clear();

        List<LibraryBookQueryResult> firstPage = libraryRepository.findAllBooksByCursor(
                user.getId(),
                null,
                LibrarySortType.ALPHABETICAL,
                10
        );

        assertThat(firstPage)
                .extracting(LibraryBookQueryResult::libraryId)
                .containsExactly(
                        firstSameTitle.getId(),
                        secondSameTitle.getId(),
                        nextTitle.getId(),
                        lastTitle.getId()
                );

        LibraryBookCursor cursor = new LibraryBookCursor(firstSameTitle.getId(), null, null, "가나다");
        List<LibraryBookQueryResult> nextPage = libraryRepository.findAllBooksByCursor(
                user.getId(),
                cursor,
                LibrarySortType.ALPHABETICAL,
                10
        );

        assertThat(nextPage)
                .extracting(LibraryBookQueryResult::libraryId)
                .containsExactly(secondSameTitle.getId(), nextTitle.getId(), lastTitle.getId());
    }

    private User saveUser(String email, String providerId) {
        User user = User.builder()
                .email(email)
                .nickName(email)
                .provider("GOOGLE")
                .providerId(providerId)
                .build();
        return userRepository.save(user);
    }

    private Library saveLibrary(User user, String title) {
        Book book = Book.builder()
                .isbn13(String.format("%013d", Math.floorMod(title.hashCode() + System.nanoTime(), 1_000_000_000_000L)))
                .title(title)
                .author("작가")
                .coverImageKey("library/cover.png")
                .sourceType(SourceType.ALADIN)
                .build();
        bookRepository.save(book);

        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        return libraryRepository.save(library);
    }

    private Theme saveTheme() {
        Theme theme = Theme.builder()
                .name(ThemeName.THEME1)
                .imageUrl("https://cdn.nook.com/themes/theme1.png")
                .build();
        em.persist(theme);
        return theme;
    }

    private void saveFocus(Library library, Theme theme, LocalDateTime endedAt) {
        Focus focus = Focus.builder()
                .library(library)
                .theme(theme)
                .startedAt(endedAt.minusMinutes(30))
                .endedAt(endedAt)
                .durationSec(1800)
                .build();
        em.persist(focus);
    }

    private void saveRecords(Library library, int count) {
        for (int i = 0; i < count; i++) {
            Record record = Record.create(library, Emotion.FUN, "기록 " + i);
            em.persist(record);
        }
    }
}
