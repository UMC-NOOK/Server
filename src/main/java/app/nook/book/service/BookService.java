package app.nook.book.service;

import app.nook.aladin.service.AladinService;
import app.nook.book.converter.BookConverter;
import app.nook.book.domain.Book;
import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.domain.enums.SourceType;
import app.nook.book.dto.BookRequestDto;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.event.BookCoverImageCleanupEvent;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.book.repository.CategoryRepository;
import app.nook.book.utils.BookUtils;
import app.nook.global.config.CacheConfig;
import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.library.domain.Library;
import app.nook.library.repository.LibraryRepository;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
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
    private final UserRepository userRepository;
    private final AladinService aladinService;
    private final PersonalizedBestsellerCacheService personalizedBestsellerCacheService;
    private final PresignedUrlService presignedUrlService;
    private final ApplicationEventPublisher eventPublisher;

    // ISBN 기반 상세조회: ALADIN 소스만 조회/저장 대상으로 사용
    @Transactional
    public BookResponseDto.BookDetailDto getBookDetailByIsbn(User user, String isbn13) {

        Optional<Book> existingBook = bookRepository.findByIsbn13AndSourceType(isbn13, SourceType.ALADIN);
        if (existingBook.isPresent()) {
            Book book = existingBook.get();

            if (book.getSourceType() == SourceType.ALADIN
                    && book.getIsbn13() != null
                    && isOutdated(book.getModifiedDate())) {
                log.info("[BOOK_UPDATE] isbn={}, title={}", isbn13, book.getTitle());
                updateBookInfo(book, isbn13);
            }
            log.info("[DB_HIT] isbn={}, title='{}'", isbn13, book.getTitle());
            return withResolvedCoverImage(resolveUserId(user), BookConverter.toBookDetailDto(book, findLibraryId(user, book)));
        }

        log.info("[API_FETCH] isbn={}, status='Not found in DB(ALADIN)'", isbn13);
        BookResponseDto.BookDetailDto bookDetailDto = aladinService.lookupItem(isbn13);
        Category category = findCategory(bookDetailDto);
        Book newBook = bookRepository.save(BookConverter.toBook(bookDetailDto, category, SourceType.ALADIN));
        log.info("[BOOK_SAVE] isbn={}, title='{}'", isbn13, bookDetailDto.getTitle());
        return withResolvedCoverImage(resolveUserId(user), BookConverter.toBookDetailDto(newBook, null));
    }

    // bookId 기반 상세조회: USER/ALADIN 공통 상세 진입점
    @Transactional
    public BookResponseDto.BookDetailDto getBookDetailById(User user, Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));

        if (book.getSourceType() == SourceType.ALADIN
                && book.getIsbn13() != null
                && isOutdated(book.getModifiedDate())) {
            updateBookInfo(book, book.getIsbn13());
        }

        return withResolvedCoverImage(resolveUserId(user), BookConverter.toBookDetailDto(book, findLibraryId(user, book)));
    }

    @Transactional
    public Book createUserBook(User user, BookRequestDto.CreateUserBookRequest request, String coverImageKey) {
        // 프론트 categoryName을 DB Category(FK)로 매핑
        Category category = findUserBookCategory(request.categoryName());
        Book book = Book.builder()
                .isbn13(BookUtils.normalizeIsbn(request.isbn13()))
                .title(request.title())
                .author(request.author())
                .publisher(request.publisher())
                .publicationDate(request.publicationDate())
                .pages(request.pages())
                .description(request.description())
                .coverImageKey(coverImageKey)
                .aladinLink(null)
                .sourceType(SourceType.USER)
                .createdByUserId(user.getId())
                .category(category)
                .build();

        Book saved = bookRepository.save(book);
        log.info("[USER_BOOK_CREATE_SAVED] userId={}, bookId={}, sourceType={}",
                user.getId(), saved.getId(), saved.getSourceType());
        return saved;
    }

    @Transactional
    public void updateUserBook(
            User user, Long bookId, BookRequestDto.UpdateUserBookRequest request, String newCoverImageUrl) {
        // 소유권 검증 후에만 USER 도서 수정 허용
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
        validateUserBookOwnership(user, book);
        String oldCoverImageKey = book.getCoverImageKey();
        String coverImageKeyToUse = (newCoverImageUrl == null || newCoverImageUrl.isBlank())
                ? book.getCoverImageKey() : newCoverImageUrl;

        book.updateUserBookInfo(request, coverImageKeyToUse, findUserBookCategory(request.categoryName()));
        cleanupOldCoverImage(oldCoverImageKey, coverImageKeyToUse);
        log.info("[USER_BOOK_UPDATE_DONE] userId={}, bookId={}", user.getId(), bookId);
    }

    // 주간 베스트셀러 조회
    @Cacheable(
            cacheNames = CacheConfig.WEEKLY_BESTSELLERS_CACHE,
            key = "'weekly'",
            sync = true
    )
    public List<BookResponseDto.BookPreviewDto> getWeeklyBestsellers() {
        log.info("[FETCH_WEEKLY_BEST]");
        List<BookResponseDto.BookPreviewDto> bookPreviewDtos = aladinService.fetchItemList(
                "Bestseller", "BOOK", 10, null);

        log.info("[FETCH_WEEKLY_BEST_SUCCESS] count={}", bookPreviewDtos.size());
        return bookPreviewDtos;
    }

    // 사용자 맞춤 추천 베스트셀러 조회
    public List<BookResponseDto.BookPreviewDto> getPersonalizedBestsellers(User user) {
        int categoryId = resolveRecommendationCategoryId(user.getId());

        log.info("[FETCH_PERSONAL_BEST] categoryId={}", categoryId);
        List<BookResponseDto.BookPreviewDto> bookPreviewDtos =
                personalizedBestsellerCacheService.getByCategoryId(categoryId);

        log.info("[FETCH_PERSONAL_BEST_SUCCESS] count={}", bookPreviewDtos.size());
        return bookPreviewDtos;
    }

    // TODO: 응답 DTO 후처리 setter 의존을 제거하고, 최종 coverImageUrl이 계산된 뒤 DTO를 생성하도록 리팩터링
    private BookResponseDto.BookDetailDto withResolvedCoverImage(Long userId, BookResponseDto.BookDetailDto dto) {
        if (userId == null) {
            return dto;
        }
        dto.setCoverImageUrl(presignedUrlService.resolveImageUrl(userId, dto.getCoverImageUrl()));
        return dto;
    }

    private void cleanupOldCoverImage(String oldCoverImageKey, String newCoverImageKey) {
        if (oldCoverImageKey == null || oldCoverImageKey.isBlank() || oldCoverImageKey.equals(newCoverImageKey)) {
            return;
        }
        eventPublisher.publishEvent(new BookCoverImageCleanupEvent(oldCoverImageKey));
    }

    private Long resolveUserId(User user) {
        return user == null ? null : user.getId();
    }

    private int resolveRecommendationCategoryId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        List<Integer> topFromLibrary = libraryRepository.findTopAladinCategoryIdsByUserId(
                userId, MallType.BOOK, PageRequest.of(0, 1)
        );

        if (!topFromLibrary.isEmpty()) {
            int categoryId = topFromLibrary.get(0);
            log.info("[RECOMMEND_CATEGORY_SOURCE] userId={}, source=LIBRARY, categoryId={}", userId, categoryId);
            return categoryId;
        }

        if (user.getPreferredCategory() != null) {
            int categoryId = user.getPreferredCategory().getAladinCategoryId();
            log.info("[RECOMMEND_CATEGORY_SOURCE] userId={}, source=ONBOARDING, categoryId={}", userId, categoryId);
            return categoryId;
        }

        log.info("[RECOMMEND_CATEGORY_SOURCE] userId={}, source=DEFAULT, categoryId=1", userId);
        return 1;
    }

    private void validateUserBookOwnership(User user, Book book) {
        if (book.getSourceType() != SourceType.USER) {
            log.warn("[USER_BOOK_UPDATE_FORBIDDEN] requestUserId={}, bookId={}, reason=NOT_USER_SOURCE, sourceType={}",
                    user.getId(), book.getId(), book.getSourceType());
            throw new CustomException(BookErrorCode.BOOK_NOT_OWNED);
        }
        if (book.getCreatedByUserId() == null || !book.getCreatedByUserId().equals(user.getId())) {
            log.warn("[USER_BOOK_UPDATE_FORBIDDEN] requestUserId={}, bookId={}, reason=NOT_OWNER, createdByUserId={}",
                    user.getId(), book.getId(), book.getCreatedByUserId());
            throw new CustomException(BookErrorCode.BOOK_NOT_OWNED);
        }
    }

    // USER 도서는 BOOK mallType 기준 카테고리만 허용
    private Category findUserBookCategory(String categoryName) {
        return categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, categoryName)
                .orElseThrow(() -> {
                    log.warn("[USER_BOOK_CATEGORY_NOT_FOUND] categoryName={}", categoryName);
                    return new CustomException(BookErrorCode.CATEGORY_NOT_FOUND);
                });
    }

    private Category findCategory(BookResponseDto.BookDetailDto bookDetailDto) {
        MallType mallType = bookDetailDto.getMallTypeCode();
        String categoryName = bookDetailDto.getCategory();

        return categoryRepository.findByMallTypeAndCategoryName(mallType, categoryName)
                .orElseGet(() -> {
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

    private Long findLibraryId(User user, Book book) {
        if (user == null) {
            return null;
        }
        return libraryRepository.findByUserAndBook(user, book)
                .map(Library::getId)
                .orElse(null);
    }
}
