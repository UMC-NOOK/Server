package app.nook.book.repository;

import app.nook.book.domain.Book;
import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.domain.enums.SourceType;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.QueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
public class BookRepositoryTest extends AbstractPostgresContainerTests {
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        category = categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, "소설/시/희곡")
                .orElseThrow();
    }

    @Test
    @DisplayName("ISBN으로 도서 조회 성공")
    void findByIsbn13_존재() {
        // given
        String isbn13 = "9788936434267";
        String title = "채식주의자";
        String author = "한강";

        bookRepository.save(createBook(category, isbn13, title, author));

        // when
        Optional<Book> foundBook = bookRepository.findByIsbn13("9788936434267");

        // then
        assertThat(foundBook).isPresent();
        assertThat(foundBook.get().getIsbn13()).isEqualTo(isbn13);
        assertThat(foundBook.get().getTitle()).isEqualTo(title);
        assertThat(foundBook.get().getAuthor()).isEqualTo(author);
        assertThat(foundBook.get().getCategory().getCategoryName()).isEqualTo(category.getCategoryName());
    }

    @Test
    @DisplayName("존재하지 않는 ISBN으로 조회 시 빈 Optional 반환")
    void findByIsbn13_존재하지않음() {
        //when
        Optional<Book> foundBook = bookRepository.findByIsbn13("9999999999999");

        //then
        assertThat(foundBook).isEmpty();
    }

    @Test
    @DisplayName("도서 저장 성공 확인")
    void 도서저장_성공() {
        // given
        String isbn13 = "9788936434267";
        String title = "채식주의자";
        String author = "한강";

        Book book = createBook(category, isbn13, title, author);

        // when
        Book savedBook = bookRepository.save(book);

        // then
        assertThat(savedBook.getId()).isNotNull();
        assertThat(savedBook.getIsbn13()).isEqualTo(isbn13);
        assertThat(savedBook.getTitle()).isEqualTo(title);
        assertThat(savedBook.getCategory().getCategoryName()).isEqualTo(category.getCategoryName());
    }

    private Book createBook(Category category, String isbn, String title, String author) {
        return Book.builder()
                .isbn13(isbn)
                .title(title)
                .author(author)
                .publisher("창비")
                .category(category)
                .sourceType(SourceType.ALADIN)
                .build();
    }
}
