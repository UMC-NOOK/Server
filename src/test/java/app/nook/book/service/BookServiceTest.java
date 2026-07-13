package app.nook.book.service;

import app.nook.aladin.service.AladinService;
import app.nook.book.domain.Book;
import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.dto.BookRequestDto;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.book.repository.CategoryRepository;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.ReadingStatusResponse;
import app.nook.library.repository.LibraryRepository;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
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

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonalizedBestsellerCacheService personalizedBestsellerCacheService;

    @Mock
    private PresignedUrlService presignedUrlService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BookViewHistoryService bookViewHistoryService;

    @Spy
    private BookAccessService bookAccessService = new BookAccessService();

    @InjectMocks
    private BookService bookService;

    @Captor // 저장되는 객체를 가로채기 위한 캡처 도구
    private ArgumentCaptor<Book> bookCaptor;

    private Category category;

    private User testUser;

    @BeforeEach
    void setUp() {
        category = Category.of(MallType.BOOK, TEST_CATEGORY_NAME, 1);
        ReflectionTestUtils.setField(category, "id", 1L);

        testUser = User.builder()
                .email("test@example.com")
                .nickName("테스터")
                .provider("google")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(testUser, "id", TEST_USER_ID);

        lenient().when(presignedUrlService.resolveImageUrl(anyLong(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    @DisplayName("DB에 존재하는 도서 조회 - Repository 조회 결과 반환 검증")
    void getBookDetailByIsbn_DB_존재() {
        // given
        Book book = createBook(TEST_ISBN_1, "채식주의자", 184);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(book, "modifiedDate", LocalDateTime.now());

        given(bookRepository.findByIsbn13AndSourceType(TEST_ISBN_1, SourceType.ALADIN))
                .willReturn(Optional.of(book));
        given(bookRepository.findById(1L))
                .willReturn(Optional.of(book));
        given(libraryRepository.findByUserAndBook(testUser, book))
                .willReturn(Optional.empty()); // 서재에 없는 경우

        // when
        BookResponseDto.BookDetailDto result = bookService.getBookDetailByIsbn(testUser, TEST_ISBN_1);

        // then
        assertThat(result.getIsbn13()).isEqualTo(TEST_ISBN_1);
        assertThat(result.getTitle()).isEqualTo("채식주의자");
        assertThat(result.getAuthor()).isEqualTo(TEST_AUTHOR);
        assertThat(result.getPublisher()).isEqualTo(TEST_PUBLISHER);
        assertThat(result.getPages()).isEqualTo(184);
        assertThat(result.getBookShelfId()).isNull();
        assertThat(result.getLibraryId()).isNull();
        assertThat(result.getReadingStatus()).isEqualTo(ReadingStatusResponse.UNREGISTERED);

        // DB에 있었으므로 알라딘 API는 호출되지 않아야 함
        verify(bookRepository, times(1)).findByIsbn13AndSourceType(TEST_ISBN_1, SourceType.ALADIN);
        verify(aladinService, never()).lookupItem(any());
        verify(bookViewHistoryService).saveBookView(testUser, book);
    }

    @Test
    @DisplayName("DB에 존재하고 서재에 등록된 도서 조회 - 서재 ID와 독서 상태 반환")
    void getBookDetailByIsbn_서재등록_상태반환() {
        // given
        Book book = createBook(TEST_ISBN_1, "채식주의자", 184);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(book, "modifiedDate", LocalDateTime.now());

        Library library = Library.builder()
                .user(testUser)
                .book(book)
                .build();
        ReflectionTestUtils.setField(library, "id", 10L);
        library.updateStatus(ReadingStatus.READING);

        given(bookRepository.findByIsbn13AndSourceType(TEST_ISBN_1, SourceType.ALADIN))
                .willReturn(Optional.of(book));
        given(libraryRepository.findByUserAndBook(testUser, book))
                .willReturn(Optional.of(library));

        // when
        BookResponseDto.BookDetailDto result = bookService.getBookDetailByIsbn(testUser, TEST_ISBN_1);

        // then
        assertThat(result.getBookShelfId()).isEqualTo(10L);
        assertThat(result.getLibraryId()).isEqualTo(10L);
        assertThat(result.getReadingStatus()).isEqualTo(ReadingStatusResponse.READING);
        verify(bookViewHistoryService).saveBookView(testUser, book);
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
        given(bookRepository.findByIsbn13AndSourceType(TEST_ISBN_1, SourceType.ALADIN)).willReturn(Optional.empty()); // DB 없음
        given(aladinService.lookupItem(TEST_ISBN_1)).willReturn(apiResponse); // API 호출
        given(categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, TEST_CATEGORY_NAME))
                .willReturn(Optional.of(category)); // 카테고리 조회
        given(bookRepository.save(any(Book.class))).willReturn(savedBookWithId); // 저장

        // when
        BookResponseDto.BookDetailDto result = bookService.getBookDetailByIsbn(testUser, TEST_ISBN_1);

        // then 1. 반환값 검증 (Controller로 나가는 데이터)
        assertThat(result.getIsbn13()).isEqualTo(TEST_ISBN_1);
        assertThat(result.getTitle()).isEqualTo("채식주의자");
        assertThat(result.getBookShelfId()).isNull();
        assertThat(result.getLibraryId()).isNull();
        assertThat(result.getReadingStatus()).isEqualTo(ReadingStatusResponse.UNREGISTERED);

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
        assertThat(capturedBook.getCoverImageKey()).isEqualTo(apiResponse.getCoverImageUrl());
        assertThat(capturedBook.getAladinLink()).isEqualTo(apiResponse.getAladinLink());

        // SourceType이 ALADIN으로 잘 들어갔는지, 카테고리 연관관계가 잘 맺어졌는지 확인
        assertThat(capturedBook.getSourceType()).isEqualTo(SourceType.ALADIN);
        assertThat(capturedBook.getCategory()).isEqualTo(category);
        verify(bookViewHistoryService).saveBookView(testUser, savedBookWithId);
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

        given(bookRepository.findByIsbn13AndSourceType(TEST_ISBN_1, SourceType.ALADIN)).willReturn(Optional.empty());
        given(aladinService.lookupItem(TEST_ISBN_1)).willReturn(apiResponse);
        // 카테고리 조회 실패 시뮬레이션
        given(categoryRepository.findByMallTypeAndCategoryName(any(), any()))
                .willReturn(Optional.empty());

        // when & then
        CustomException ex = assertThrows(
                CustomException.class,
                () -> bookService.getBookDetailByIsbn(testUser, TEST_ISBN_1)
        );

        assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_ALLOWED);

        // 예외 발생 시 저장이 수행되면 안 됨 (데이터 오염 방지)
        verify(bookRepository, never()).save(any());
        verifyNoInteractions(bookViewHistoryService);
    }

    @Test
    @DisplayName("bookId 기반 상세 조회 성공")
    void getBookDetailById_success() {
        Book book = createBook(TEST_ISBN_1, "제목", 184);
        ReflectionTestUtils.setField(book, "id", 20L);
        ReflectionTestUtils.setField(book, "sourceType", SourceType.USER);
        ReflectionTestUtils.setField(book, "createdByUserId", TEST_USER_ID);

        given(bookRepository.findById(20L)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserAndBook(testUser, book)).willReturn(Optional.empty());

        BookResponseDto.BookDetailDto result = bookService.getBookDetailById(testUser, 20L);

        assertThat(result.getBookId()).isEqualTo(20L);
        assertThat(result.getTitle()).isEqualTo("제목");
        assertThat(result.getBookShelfId()).isNull();
        assertThat(result.getLibraryId()).isNull();
        assertThat(result.getReadingStatus()).isEqualTo(ReadingStatusResponse.UNREGISTERED);
        verify(bookViewHistoryService).saveBookView(testUser, book);
    }

    @Test
    @DisplayName("bookId 기반 상세 조회 - 서재 ID와 독서 상태 반환")
    void getBookDetailById_서재등록_상태반환() {
        Book book = createBook(TEST_ISBN_1, "제목", 184);
        ReflectionTestUtils.setField(book, "id", 20L);
        ReflectionTestUtils.setField(book, "sourceType", SourceType.USER);
        ReflectionTestUtils.setField(book, "createdByUserId", TEST_USER_ID);

        Library library = Library.builder()
                .user(testUser)
                .book(book)
                .build();
        ReflectionTestUtils.setField(library, "id", 10L);
        library.updateStatus(ReadingStatus.READING);

        given(bookRepository.findById(20L)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserAndBook(testUser, book)).willReturn(Optional.of(library));

        BookResponseDto.BookDetailDto result = bookService.getBookDetailById(testUser, 20L);

        assertThat(result.getBookId()).isEqualTo(20L);
        assertThat(result.getBookShelfId()).isEqualTo(10L);
        assertThat(result.getLibraryId()).isEqualTo(10L);
        assertThat(result.getReadingStatus()).isEqualTo(ReadingStatusResponse.READING);
        verify(bookViewHistoryService).saveBookView(testUser, book);
    }

    @Test
    @DisplayName("사용자 도서 생성/수정 응답용 상세 조회는 이력을 저장하지 않음")
    void getBookDetailByIdForUserBookResponse_이력저장없음() {
        Book book = createBook(TEST_ISBN_1, "제목", 184);
        ReflectionTestUtils.setField(book, "id", 20L);
        ReflectionTestUtils.setField(book, "sourceType", SourceType.USER);
        ReflectionTestUtils.setField(book, "createdByUserId", TEST_USER_ID);

        given(bookRepository.findById(20L)).willReturn(Optional.of(book));
        given(libraryRepository.findByUserAndBook(testUser, book)).willReturn(Optional.empty());

        BookResponseDto.BookDetailDto result = bookService.getBookDetailByIdForUserBookResponse(testUser, 20L);

        assertThat(result.getBookId()).isEqualTo(20L);
        assertThat(result.getTitle()).isEqualTo("제목");
        verifyNoInteractions(bookViewHistoryService);
    }

    @Test
    @DisplayName("bookId 기반 상세 조회 - 존재하지 않는 도서면 이력 저장 없이 실패")
    void getBookDetailById_notFound_fail() {
        given(bookRepository.findById(999L)).willReturn(Optional.empty());

        CustomException ex = assertThrows(
                CustomException.class,
                () -> bookService.getBookDetailById(testUser, 999L)
        );

        assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_FOUND);
        verifyNoInteractions(bookViewHistoryService);
    }

    @Test
    @DisplayName("bookId 기반 상세 조회 - 다른 사용자가 생성한 USER 도서면 실패")
    void getBookDetailById_otherUserBook_fail() {
        Book book = createBook(TEST_ISBN_1, "제목", 184);
        ReflectionTestUtils.setField(book, "id", 20L);
        ReflectionTestUtils.setField(book, "sourceType", SourceType.USER);
        ReflectionTestUtils.setField(book, "createdByUserId", 999L);
        ReflectionTestUtils.setField(book, "coverImageKey", "book/users/999/cover.png");

        given(bookRepository.findById(20L)).willReturn(Optional.of(book));

        CustomException ex = assertThrows(
                CustomException.class,
                () -> bookService.getBookDetailById(testUser, 20L)
        );

        assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_ACCESS_DENIED);
        verify(presignedUrlService, never()).resolveImageUrl(anyLong(), any());
        verifyNoInteractions(bookViewHistoryService);
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
        assertThat(result.get(0).title()).isEqualTo("채식주의자");
        assertThat(result.get(0).rank()).isEqualTo(1);
        assertThat(result.get(1).title()).isEqualTo("소년이 온다");

        verify(aladinService, times(1)).fetchItemList("Bestseller", "BOOK", 10, null);
    }

    @Test
    @DisplayName("추천 카테고리: 서재 최다 카테고리 우선")
    void getPersonalizedBestsellers_서재우선() {
        // given
        List<BookResponseDto.BookPreviewDto> mockBestsellers = Arrays.asList(
                createBookPreviewDto(TEST_ISBN_1, "채식주의자", 1)
        );

        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(libraryRepository.findTopAladinCategoryIdsByUserId(TEST_USER_ID, MallType.BOOK, PageRequest.of(0, 1)))
                .willReturn(List.of(42));
        given(personalizedBestsellerCacheService.getByCategoryId(42))
                .willReturn(mockBestsellers);

        // when
        List<BookResponseDto.BookPreviewDto> result = bookService.getPersonalizedBestsellers(testUser);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("채식주의자");

        verify(personalizedBestsellerCacheService, times(1)).getByCategoryId(42);
    }

    @Test
    @DisplayName("추천 카테고리: 서재가 없으면 온보딩 preferred_category 사용")
    void getPersonalizedBestsellers_온보딩fallback() {
        Category preferred = Category.of(MallType.BOOK, "에세이", 77);
        ReflectionTestUtils.setField(preferred, "id", 99L);
        ReflectionTestUtils.setField(testUser, "preferredCategory", preferred);

        List<BookResponseDto.BookPreviewDto> mockBestsellers = List.of(
                createBookPreviewDto(TEST_ISBN_2, "소년이 온다", 1)
        );

        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(libraryRepository.findTopAladinCategoryIdsByUserId(TEST_USER_ID, MallType.BOOK, PageRequest.of(0, 1)))
                .willReturn(List.of());
        given(personalizedBestsellerCacheService.getByCategoryId(77))
                .willReturn(mockBestsellers);

        List<BookResponseDto.BookPreviewDto> result = bookService.getPersonalizedBestsellers(testUser);

        assertThat(result).hasSize(1);
        verify(personalizedBestsellerCacheService, times(1)).getByCategoryId(77);
    }

    @Test
    @DisplayName("추천 카테고리: 서재/온보딩 모두 없으면 default 1")
    void getPersonalizedBestsellers_defaultFallback() {
        List<BookResponseDto.BookPreviewDto> mockBestsellers = List.of(
                createBookPreviewDto(TEST_ISBN_1, "채식주의자", 1)
        );

        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(libraryRepository.findTopAladinCategoryIdsByUserId(TEST_USER_ID, MallType.BOOK, PageRequest.of(0, 1)))
                .willReturn(List.of());
        given(personalizedBestsellerCacheService.getByCategoryId(1))
                .willReturn(mockBestsellers);

        List<BookResponseDto.BookPreviewDto> result = bookService.getPersonalizedBestsellers(testUser);

        assertThat(result).hasSize(1);
        verify(personalizedBestsellerCacheService, times(1)).getByCategoryId(1);
    }

    @Test
    @DisplayName("사용자 도서 생성 성공")
    void createUserBook_success() {
        BookRequestDto.CreateUserBookRequest request = new BookRequestDto.CreateUserBookRequest(
                "혼모노", "성해은", TEST_CATEGORY_NAME, "소개", 348, TEST_PUBLISHER,
                "2023-06-05", "9788936439743", null
        );

        given(categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, TEST_CATEGORY_NAME))
                .willReturn(Optional.of(category));
        given(bookRepository.save(any(Book.class))).willAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        Book saved = bookService.createUserBook(testUser, request, "/uploads/covers/a.png");

        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getSourceType()).isEqualTo(SourceType.USER);
        assertThat(saved.getCreatedByUserId()).isEqualTo(TEST_USER_ID);
        assertThat(saved.getCategory()).isEqualTo(category);
        assertThat(saved.getIsbn13()).isEqualTo("9788936439743");
    }

    @Test
    @DisplayName("사용자 도서 수정 - 본인 소유가 아니면 실패")
    void updateUserBook_notOwned_fail() {
        BookRequestDto.UpdateUserBookRequest request = new BookRequestDto.UpdateUserBookRequest(
                "수정제목", "수정저자", TEST_CATEGORY_NAME, "수정소개", 200, TEST_PUBLISHER,
                "2024-01-01", "1234567890123", null
        );

        Book book = createBook("1234567890123", "원제목", 120);
        ReflectionTestUtils.setField(book, "id", 10L);
        ReflectionTestUtils.setField(book, "sourceType", SourceType.USER);
        ReflectionTestUtils.setField(book, "createdByUserId", 999L);

        given(bookRepository.findById(10L)).willReturn(Optional.of(book));

        CustomException ex = assertThrows(
                CustomException.class,
                () -> bookService.updateUserBook(testUser, 10L, request, null)
        );

        assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_OWNED);
    }

    @Test
    @DisplayName("사용자 도서 수정 - 카테고리 없음 실패")
    void updateUserBook_categoryNotFound_fail() {
        BookRequestDto.UpdateUserBookRequest request = new BookRequestDto.UpdateUserBookRequest(
                "수정제목", "수정저자", "없는카테고리", "수정소개", 200, TEST_PUBLISHER,
                "2024-01-01", "1234567890123", null
        );

        Book book = createBook("1234567890123", "원제목", 120);
        ReflectionTestUtils.setField(book, "id", 11L);
        ReflectionTestUtils.setField(book, "sourceType", SourceType.USER);
        ReflectionTestUtils.setField(book, "createdByUserId", TEST_USER_ID);

        given(bookRepository.findById(11L)).willReturn(Optional.of(book));
        given(categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, "없는카테고리"))
                .willReturn(Optional.empty());

        CustomException ex = assertThrows(
                CustomException.class,
                () -> bookService.updateUserBook(testUser, 11L, request, null)
        );

        assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.CATEGORY_NOT_FOUND);
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
                .coverImageKey("http://example.com/cover.jpg")
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
        return new BookResponseDto.BookPreviewDto(
                isbn13,
                title,
                TEST_AUTHOR,
                "http://example.com/cover.jpg",
                TEST_PUBLISHER,
                rank
        );
    }
}
