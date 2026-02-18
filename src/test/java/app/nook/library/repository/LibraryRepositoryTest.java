package app.nook.library.repository;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.repository.BookRepository;
import app.nook.global.config.QueryDslConfig;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.user.domain.User;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class LibraryRepositoryTest {

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

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

        Library found = libraryRepository.findByUserAndBook(user, book);

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

        Library found = libraryRepository.findByUserAndBook(user, book);

        assertThat(found).isNull();
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
}
