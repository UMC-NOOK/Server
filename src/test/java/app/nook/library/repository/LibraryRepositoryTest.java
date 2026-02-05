package app.nook.library.repository;

import app.nook.book.domain.Book;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.repository.BookRepository;
import app.nook.library.domain.Library;
import app.nook.user.domain.User;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
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
}
