package app.nook.book.service;

import app.nook.aladin.service.AladinService;
import app.nook.book.domain.Book;
import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.repository.BookRepository;
import app.nook.book.repository.CategoryRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookServiceTest {

    private static final String TEST_ISBN_1 = "9788936434267";
    private static final String TEST_ISBN_2 = "9788936433598";
    private static final String TEST_CATEGORY_NAME = "소설/시/희곡";
    private static final String TEST_AUTHOR = "한강";
    private static final String TEST_PUBLISHER = "창비";
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AladinService aladinService;

    @InjectMocks
    private BookService bookService;

    @Captor // 저장되는 객체를 가로채기 위한 캡처 도구
    private ArgumentCaptor<Book> bookCaptor;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.of(MallType.BOOK, TEST_CATEGORY_NAME, 1);
        ReflectionTestUtils.setField(category, "id", 1L);
    }

    @Test
    @DisplayName("DB에 존재하는 도서 조회 - Repository 조회 결과 반환 검증")
    void getBookDetailByIsbn_DB_존재() {
        // given
        Book book = createBook(TEST_ISBN_1, "채식주의자", 184);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(book, "modifiedDate", LocalDateTime.now());

        given(bookRepository.findByIsbn13(TEST_ISBN_1))
                .willReturn(Optional.of(book));

        // when
        BookResponseDto.BookDetailDto result = bookService.getBookDetailByIsbn(TEST_USER_ID, TEST_ISBN_1);

        // then
        assertThat(result.getIsbn13()).isEqualTo(TEST_ISBN_1);
        assertThat(result.getTitle()).isEqualTo("채식주의자");
        assertThat(result.getAuthor()).isEqualTo(TEST_AUTHOR);
        assertThat(result.getPublisher()).isEqualTo(TEST_PUBLISHER);
        assertThat(result.getPages()).isEqualTo(184);

        // DB에 있었으므로 알라딘 API는 호출되지 않아야 함
        verify(bookRepository, times(1)).findByIsbn13(TEST_ISBN_1);
        verify(aladinService, never()).lookupItem(any());
    }

    @Test
    @DisplayName("DB에 없는 도서 - API 호출 후 데이터 매핑 및 저장 검증")
    void getBookDetailByIsbn_DB에없어서API호출() {
        // given
        BookResponseDto.BookDetailDto apiResponse = createBookDetailDto(
                TEST_ISBN_1, "채식주의자", 184);

        Book savedBookWithId = createBook(TEST_ISBN_1, "채식주의자", 184);
        ReflectionTestUtils.setField(savedBookWithId, "id", 100L);

        // Mocking 시나리오
        given(bookRepository.findByIsbn13(TEST_ISBN_1)).willReturn(Optional.empty()); // DB 없음
        given(aladinService.lookupItem(TEST_ISBN_1)).willReturn(apiResponse); // API 호출
        given(categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, TEST_CATEGORY_NAME))
                .willReturn(Optional.of(category)); // 카테고리 조회
        given(bookRepository.save(any(Book.class))).willReturn(savedBookWithId); // 저장

        // when
        BookResponseDto.BookDetailDto result = bookService.getBookDetailByIsbn(TEST_USER_ID, TEST_ISBN_1);

        // then 1. 반환값 검증 (Controller로 나가는 데이터)
        assertThat(result.getIsbn13()).isEqualTo(TEST_ISBN_1);
        assertThat(result.getTitle()).isEqualTo("채식주의자");

        // then 2. 저장 메서드에 넘겨진 실제 엔티티 '나포'
        verify(bookRepository).save(bookCaptor.capture());
        Book capturedBook = bookCaptor.getValue();

        // then 3. [데이터 정합성 검증] DTO -> Entity 변환이 완벽한지 확인
        assertThat(capturedBook.getIsbn13()).isEqualTo(apiResponse.getIsbn13());
        assertThat(capturedBook.getTitle()).isEqualTo(apiResponse.getTitle());
        assertThat(capturedBook.getAuthor()).isEqualTo(apiResponse.getAuthor());
        assertThat(capturedBook.getPublisher()).isEqualTo(apiResponse.getPublisher());
        assertThat(capturedBook.getPages()).isEqualTo(apiResponse.getPages());
        assertThat(capturedBook.getDescription()).isEqualTo(apiResponse.getDescription());
        assertThat(capturedBook.getCoverImageUrl()).isEqualTo(apiResponse.getCoverImageUrl());
        assertThat(capturedBook.getAladinLink()).isEqualTo(apiResponse.getAladinLink());

        // SourceType이 ALADIN으로 잘 들어갔는지, 카테고리 연관관계가 잘 맺어졌는지 확인
        assertThat(capturedBook.getSourceType()).isEqualTo(SourceType.ALADIN);
        assertThat(capturedBook.getCategory()).isEqualTo(category);
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 - 예외 발생 및 저장 로직 미실행 검증")
    void getBookDetailByIsbn_카테고리없음_예외발생() {
        // given
        BookResponseDto.BookDetailDto apiResponse = BookResponseDto.BookDetailDto.builder()
                .isbn13(TEST_ISBN_1)
                .mallTypeCode(MallType.BOOK)
                .category("존재하지않는카테고리")
                .build();

        given(bookRepository.findByIsbn13(TEST_ISBN_1)).willReturn(Optional.empty());
        given(aladinService.lookupItem(TEST_ISBN_1)).willReturn(apiResponse);
        // 카테고리 조회 실패 시뮬레이션
        given(categoryRepository.findByMallTypeAndCategoryName(any(), any()))
                .willReturn(Optional.empty());

        // when & then
        CustomException ex = assertThrows(
                CustomException.class,
                () -> bookService.getBookDetailByIsbn(TEST_USER_ID, TEST_ISBN_1)
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BOOK_NOT_ALLOWED);

        // 예외 발생 시 저장이 수행되면 안 됨 (데이터 오염 방지)
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("주간 베스트셀러 목록 조회 성공")
    void getWeeklyBestsellers_성공() {
        // given
        List<BookResponseDto.BookPreviewDto> mockBestsellers = Arrays.asList(
                createBookPreviewDto(TEST_ISBN_1, "채식주의자", 1),
                createBookPreviewDto(TEST_ISBN_2, "소년이 온다", 2)
        );

        given(aladinService.fetchItemList("Bestseller", "BOOK", 10, null))
                .willReturn(mockBestsellers);

        // when
        List<BookResponseDto.BookPreviewDto> result = bookService.getWeeklyBestsellers();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("채식주의자");
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(1).getTitle()).isEqualTo("소년이 온다");

        verify(aladinService, times(1)).fetchItemList("Bestseller", "BOOK", 10, null);
    }

    @Test
    @DisplayName("사용자 맞춤 추천 베스트셀러 조회 성공")
    void getPersonalizedBestsellers_성공() {
        // given
        List<BookResponseDto.BookPreviewDto> mockBestsellers = Arrays.asList(
                createBookPreviewDto(TEST_ISBN_1, "채식주의자", 1)
        );

        given(aladinService.fetchItemList("Bestseller", "BOOK", 5, "1"))
                .willReturn(mockBestsellers);

        // when
        List<BookResponseDto.BookPreviewDto> result = bookService.getPersonalizedBestsellers(TEST_USER_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("채식주의자");

        verify(aladinService, times(1)).fetchItemList("Bestseller", "BOOK", 5, "1");
    }

    // === 헬퍼 메서드 ===

    private Book createBook(String isbn13, String title, Integer pages) {
        return Book.builder()
                .isbn13(isbn13)
                .title(title)
                .author(TEST_AUTHOR)
                .publisher(TEST_PUBLISHER)
                .publicationDate("2007-10-30")
                .pages(pages)
                .description(TEST_AUTHOR + "의 소설")
                .coverImageUrl("http://example.com/cover.jpg")
                .aladinLink("http://aladin.com/book")
                .sourceType(SourceType.ALADIN)
                .category(category)
                .build();
    }

    private BookResponseDto.BookDetailDto createBookDetailDto(String isbn13, String title, Integer pages) {
        return BookResponseDto.BookDetailDto.builder()
                .isbn13(isbn13)
                .title(title)
                .author(TEST_AUTHOR)
                .publisher(TEST_PUBLISHER)
                .publicationDate("2007-10-30")
                .mallType("도서")
                .mallTypeCode(MallType.BOOK)
                .category(TEST_CATEGORY_NAME)
                .pages(pages)
                .description(TEST_AUTHOR + "의 소설")
                .coverImageUrl("http://example.com/cover.jpg")
                .aladinLink("http://aladin.com/book")
                .sourceType(SourceType.ALADIN)
                .build();
    }

    private BookResponseDto.BookPreviewDto createBookPreviewDto(String isbn13, String title, Integer rank) {
        return BookResponseDto.BookPreviewDto.builder()
                .isbn13(isbn13)
                .title(title)
                .author(TEST_AUTHOR)
                .coverImageUrl("http://example.com/cover.jpg")
                .publisher(TEST_PUBLISHER)
                .rank(rank)
                .build();
    }
}