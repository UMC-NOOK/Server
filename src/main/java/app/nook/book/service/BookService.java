package app.nook.book.service;

import app.nook.aladin.service.AladinService;
import app.nook.book.converter.BookConverter;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.domain.Book;
import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.book.repository.CategoryRepository;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.Library;
import app.nook.library.repository.LibraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final LibraryRepository libraryRepository;
    private final AladinService aladinService;

    // 도서 상세 조회
    // TODO: 사용자가 추가한 서재인 경우 필터링 로직 추가 예정
    @Transactional
    public BookResponseDto.BookDetailDto getBookDetailByIsbn(Long userId, String isbn13) {

        Optional<Book> existingBook = bookRepository.findByIsbn13(isbn13);
        if (existingBook.isPresent()) {
            Book book = existingBook.get();

            if (isOutdated(book.getModifiedDate()) && book.getSourceType()== SourceType.ALADIN) {
                log.info("[BOOK_UPDATE] isbn={}, title={}", isbn13, book.getTitle());
                updateBookInfo(book, isbn13);
            }
            log.info("[DB_HIT] isbn={}, title='{}'", isbn13, book.getTitle());
            return BookConverter.toBookDetailDto(book, findLibraryId(userId, book.getId()));
        }

        log.info("[API_FETCH] isbn={}, status='Not found in DB'", isbn13);
        BookResponseDto.BookDetailDto bookDetailDto = aladinService.lookupItem(isbn13);
        Category category = findCategory(bookDetailDto);
        Book newBook = bookRepository.save(BookConverter.toBook(bookDetailDto, category, SourceType.ALADIN));
        log.info("[BOOK_SAVE] isbn={}, title='{}'", isbn13, bookDetailDto.getTitle());
        return BookConverter.toBookDetailDto(newBook, null);
    }

    // 주간 베스트셀러
    // TODO: redis 도입 예정
    public List<BookResponseDto.BookPreviewDto> getWeeklyBestsellers() {
        log.info("[FETCH_WEEKLY_BEST]");
        List<BookResponseDto.BookPreviewDto> bookPreviewDtos = aladinService.fetchItemList(
                "Bestseller", "BOOK", 10, null);

        log.info("[FETCH_WEEKLY_BEST_SUCCESS] count={}", bookPreviewDtos.size());
        return bookPreviewDtos;
    }

    // 사용자 맞춤 추천 베스트셀러
    // TODO: 카테고리 추출 및 redis는 이후 구현 예정
    public List<BookResponseDto.BookPreviewDto> getPersonalizedBestsellers(Long userId) {
        String categoryId = "1"; // 예시 카테고리 ID

        log.info("[FETCH_PERSONAL_BEST] categoryId={}", categoryId);
        List<BookResponseDto.BookPreviewDto> bookPreviewDtos = aladinService.fetchItemList(
                "Bestseller", "BOOK", 5, categoryId);

        log.info("[FETCH_PERSONAL_BEST_SUCCESS] count={}", bookPreviewDtos.size());
        return bookPreviewDtos;
    }

    private Category findCategory(BookResponseDto.BookDetailDto bookDetailDto) {
        MallType mallType = bookDetailDto.getMallTypeCode(); // 예: BOOK
        String categoryName = bookDetailDto.getCategory(); // 예: 소설/시/희곡

        return categoryRepository.findByMallTypeAndCategoryName(mallType, categoryName)
                .orElseGet(() -> {
                    // 예외 처리: 알라딘이 보낸 1 Depth 이름이 우리 DB 초기화 리스트에 없는 경우
                    log.warn("[CATEGORY_MAPPING_FAIL] mallType={}, category='{}'", mallType, categoryName);
                    throw new CustomException(BookErrorCode.BOOK_NOT_ALLOWED);
                });
    }

    private boolean isOutdated(LocalDateTime modifiedDate) {
        return modifiedDate.isBefore(LocalDateTime.now().minusDays(30));
    }

    private void updateBookInfo(Book book, String isbn13) {
        BookResponseDto.BookDetailDto latestInfo = aladinService.lookupItem(isbn13);
        Category latestCategory = findCategory(latestInfo);
        book.updateInfo(latestInfo, latestCategory);
        log.info("[BookService] Book info updated - ISBN: {}, title: {}", isbn13, book.getTitle());
    }

    private Long findLibraryId(Long userId, Long bookId) {
        return libraryRepository.findByUserIdAndBookId(userId, bookId)
                .map(Library::getId)
                .orElse(null);
    }
}
