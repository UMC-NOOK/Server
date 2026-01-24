package app.nook.book.service;

import app.nook.aladin.dto.AladinResponseDto;
import app.nook.aladin.service.AladinService;
import app.nook.book.converter.BookConverter;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.entity.Book;
import app.nook.book.entity.Category;
import app.nook.book.entity.MallType;
import app.nook.book.entity.SourceType;
import app.nook.book.repository.BookRepository;
import app.nook.book.repository.CategoryRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
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
    private final AladinService aladinService;

    // 도서 상세 조회
    // TODO: 서재 관련 기능 추후 개발 예정
    // TODO: 인증 로직 구현 시 실제 사용자 ID로 대체
    // TODO: 사용자가 추가한 서재인 경우 필터링 로직 추가 예정
    @Transactional
    public BookResponseDto.BookDetailDto getBookDetailByIsbn(String isbn13) {
        Long userId = 1L; // 추후 인증 로직 구현 시 실제 사용자 ID로 대체

        Optional<Book> existingBook = bookRepository.findByIsbn13(isbn13);
        if (existingBook.isPresent()) {
            Book book = existingBook.get();

            if (isOutdated(book.getModifiedDate()) && book.getSourceType()== SourceType.ALADIN) {
                log.info("[BookService] 도서 정보가 오래되어 업데이트 진행 for isbn13={}", isbn13);
                updateBookInfo(book, isbn13);
            }
            log.info("[BookService] 도서 상세 조회 완료(DB) for isbn13={}, title={}", isbn13, book.getTitle());
            return BookConverter.toBookDetailDto(book, null); // TODO: 서재 ID 추가 예정
        }

        log.info("[BookService] 도서 정보가 없어 알라딘 API 조회 for isbn13={}", isbn13);
        BookResponseDto.BookDetailDto bookDetailDto = aladinService.lookupItem(isbn13);
        Category category = findCategory(bookDetailDto);
        Book newBook = bookRepository.save(BookConverter.toBook(bookDetailDto, category, SourceType.ALADIN));
        log.info("[BookService] 도서 상세 조회 완료 for isbn13={}, title={}", isbn13, bookDetailDto.getTitle());
        return BookConverter.toBookDetailDto(newBook, null); // TODO: 서재 ID 추가 예정
    }

    // 주간 베스트셀러
    // TODO: redis 도입 예정
    public List<BookResponseDto.BookPreviewDto> getWeeklyBestsellers() {
        log.info("[BookService] 주간 베스트셀러 조회 요청");
        List<BookResponseDto.BookPreviewDto> bookPreviewDtos = aladinService.fetchItemList(
                "Bestseller", "BOOK", 10, null);

        log.info("[BookService] 주간 베스트셀러 조회 완료: {}권", bookPreviewDtos.size());
        return bookPreviewDtos;
    }

    // 사용자 맞춤 추천 베스트셀러
    // TODO: 유저 + 카테고리 추출 및 redis는 이후 구현 예정
    public List<BookResponseDto.BookPreviewDto> getPersonalizedBestsellers() {
        String categoryId = "1"; // 예시 카테고리 ID

        log.info("[BookService] 맞춤 추천 베스트셀러 조회 요청 for categoryId={}", categoryId);
        List<BookResponseDto.BookPreviewDto> bookPreviewDtos = aladinService.fetchItemList(
                "Bestseller", "BOOK", 5, categoryId);

        log.info("[BookService] 맞춤 추천 베스트셀러 조회 완료: {}권", bookPreviewDtos.size());
        return bookPreviewDtos;
    }

    private Category findCategory(BookResponseDto.BookDetailDto bookDetailDto) {
        MallType mallType = bookDetailDto.getMallTypeCode(); // 예: BOOK
        String categoryName = bookDetailDto.getCategory(); // 예: 소설/시/희곡

        return categoryRepository.findByMallTypeAndCategoryName(mallType, categoryName)
                .orElseGet(() -> {
                    // 예외 처리: 알라딘이 보낸 1 Depth 이름이 우리 DB 초기화 리스트에 없는 경우
                    log.warn("매핑된 카테고리가 없습니다. [{} - {}]", mallType, categoryName);
                    throw new CustomException(ErrorCode.BOOK_NOT_ALLOWED);
                });
    }

    private boolean isOutdated(LocalDateTime modifiedDate) {
        return modifiedDate.isBefore(LocalDateTime.now().minusDays(30));
    }

    private void updateBookInfo(Book book, String isbn13) {
        BookResponseDto.BookDetailDto latestInfo = aladinService.lookupItem(isbn13);
        Category latestCategory = findCategory(latestInfo);
        book.updateInfo(latestInfo, latestCategory);
        log.info("[BookService] 도서 정보 업데이트 완료 for isbn13={}", isbn13);
    }
}
