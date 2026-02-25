package app.nook.library.service;

import app.nook.book.domain.Book;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.dto.CursorResponse;
import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
import app.nook.library.converter.LibraryConverter;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.timeline.converter.TimeLineConverter;
import app.nook.timeline.domain.BookTimeLine;
import app.nook.timeline.domain.enums.BookTimeLineType;
import app.nook.timeline.repository.BookTimeLineRepository;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryService {

    private final LibraryRepository libraryRepository;
    private final BookRepository bookRepository;
    private final BookTimeLineRepository bookTimeLineRepository;
    private final FocusRepository focusRepository;
    private final CacheManager cacheManager;


    // 서재 책 개수 조회

    // 서재 책 등록
    @Transactional
    public void save(User user,Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
        // 이미 서재에 등록된 책인지 확인
        if (libraryRepository.findByUserAndBook(user,book) != null)
            throw new CustomException(LibraryErrorCode.BOOK_ALREADY_EXIST);

        // 서재 생성
        Library library = new Library(user,book);
        Library savedLibrary = libraryRepository.save(library);

        // 타임라인 업데이트
        BookTimeLine timeLine = TimeLineConverter.toBookTimeLine(
                savedLibrary,
                BookTimeLineType.REGISTER,
                savedLibrary.getCreatedDate().toString(),
                savedLibrary.getId()
        );
        bookTimeLineRepository.save(timeLine);
        evictStatusBookFirstPageCaches(user.getId());
    }

    // 서재 책 삭제
    @Transactional
    public void deleteById(User user, Long bookId){
        // 책 존재 검증
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
        Library library = libraryRepository.findByUserAndBook(user,book);
        // 서재 책 존재 확인
        if (library == null)
            throw new CustomException(LibraryErrorCode.BOOK_NOT_EXIST);

        // 캐시 무효화에 필요한 데이터 조회
        List<FocusRepository.FocusYearMonthProjection> affectedYearMonths =
                focusRepository.findDistinctFocusYearMonthsByLibraryAndUser(
                        library.getId(),
                        user.getId()
                );

        libraryRepository.delete(library);

        // 캐시 무효화
        evictMonthlyStatsCaches(user.getId(), affectedYearMonths);
        evictStatusBookFirstPageCaches(user.getId());
    }

    // 서재 책 상태변경
    @Transactional
    public void changeStatus(User user, ReadingStatusRequestDto requestDto) {
        Book book = bookRepository.findById(requestDto.bookId())
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
        Library library = libraryRepository.findByUserAndBook(user,book);
        // 책 존재 확인
        if (library == null)
            throw new CustomException(LibraryErrorCode.BOOK_NOT_EXIST);
        // 중복 방지
        if (library.getReadingStatus() == requestDto.readingStatus())
            throw new CustomException(LibraryErrorCode.BOOK_STATUS_INVALID);
        // 상태 변경
        library.updateStatus(requestDto.readingStatus());
        // 타임라인 업데이트
        BookTimeLine timeLine = TimeLineConverter.toBookTimeLine(
                library,
                BookTimeLineType.STATUS,
                library.getReadingStatus().toString(),
                library.getId());
        bookTimeLineRepository.save(timeLine);
        evictStatusBookFirstPageCaches(user.getId());
    }

    // 서재 상태별 책 조회
    @Cacheable(
            value = "libraryStatusFirstPage",
            key = "#user.id + ':' + #status",
            condition = "#cursor == null && #size == 20"
    )
    public LibraryViewDto.StatusBookResponseDto viewBooksByStatus(
            User user,
            ReadingStatus status,
            Long cursor,
            int size
    ) {
        Pageable pageable = PageRequest.of(0, size + 1);

        Slice<Library> libraries =
                libraryRepository.findByStatusWithCursor(
                        user,
                        status,
                        cursor,
                        pageable
                );

        CursorResponse<LibraryViewDto.UserStatusBookItem> cursorResponse =
                LibraryConverter.toCursorResponse(libraries.getContent(), size);

        int totalCount = 0;
        if (cursor == null) {
            totalCount = (int) libraryRepository.countByUserAndReadingStatus(user,status);
        }
        return LibraryConverter.toStatusBookResponse(
                status,
                totalCount,
                cursorResponse
        );
    }

    // 전체 검색 - 서재 보유 ISBN 목록 반환
    public Set<String> findOwnedIsbns(Long userId, List<String> isbns) {
        return libraryRepository.findIsbnsByUserIdAndIsbnIn(userId, isbns);
    }

    // 서재 내 도서 검색
    public Page<Library> searchBooksInLibrary(Long userId, String keyword, int page, int size) {
        String escapedKeyword = keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        Pageable pageable = PageRequest.of(page, size);
        return libraryRepository.searchByUserIdAndKeyword(userId, escapedKeyword, pageable);
    }

    // 캐시 무효화 월별
    private void evictMonthlyStatsCaches(
            Long userId,
            List<FocusRepository.FocusYearMonthProjection> yearMonths
    ) {
        if (yearMonths == null || yearMonths.isEmpty()) {
            return;
        }
        Cache monthlyCache = cacheManager.getCache("libraryMonthlyCurrent");
        Cache focusTimeCache = cacheManager.getCache("focusMonthlyCurrent");

        for (FocusRepository.FocusYearMonthProjection yearMonth : yearMonths) {
            int year = yearMonth.getYearValue();
            int month = yearMonth.getMonthValue();
            String key = userId + ":" + YearMonth.of(year, month);
            if (monthlyCache != null) {
                monthlyCache.evict(key);
            }
            if (focusTimeCache != null) {
                focusTimeCache.evict(key);
            }
        }
    }

    // 기본 상태별 목록 캐시 무효화 - 첫 페이지
    private void evictStatusBookFirstPageCaches(Long userId) {
        Cache cache = cacheManager.getCache("libraryStatusFirstPage");
        if (cache == null) {
            return;
        }
        for (ReadingStatus status : ReadingStatus.values()) {
            cache.evict(userId + ":" + status);
        }
    }
}
