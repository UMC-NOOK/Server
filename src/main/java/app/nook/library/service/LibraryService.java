package app.nook.library.service;

import app.nook.book.domain.Book;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.focus.domain.Focus;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.dto.CursorResponse;
import app.nook.global.exception.CustomException;
import app.nook.library.converter.LibraryConverter;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.event.LibraryCacheInvalidateEvent;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.timeline.converter.TimeLineConverter;
import app.nook.timeline.domain.BookTimeLine;
import app.nook.timeline.domain.enums.BookTimeLineType;
import app.nook.timeline.repository.BookTimeLineRepository;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryService {

    private final LibraryRepository libraryRepository;
    private final BookRepository bookRepository;
    private final BookTimeLineRepository bookTimeLineRepository;
    private final FocusRepository focusRepository;
    private final ApplicationEventPublisher eventPublisher;


    // 서재 책 개수 조회
    // 서재 책은 최대 100000권이므로 int로 반환 타입 설정
    public LibraryViewDto.BookCountResponseDto countBooks(User user) {
        return new LibraryViewDto.BookCountResponseDto(libraryRepository.countByUser(user));
    }

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
        eventPublisher.publishEvent(LibraryCacheInvalidateEvent.statusOnly(user.getId()));
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
        List<YearMonth> affectedYearMonths =
                focusRepository.findDistinctFocusYearMonthsByLibraryAndUser(
                        library.getId(),
                        user.getId()
                ).stream()
                .map(ym -> YearMonth.of(ym.getYearValue(), ym.getMonthValue()))
                .collect(Collectors.toList());

        libraryRepository.delete(library);

        eventPublisher.publishEvent(
                LibraryCacheInvalidateEvent.statusAndMonthly(user.getId(), affectedYearMonths)
        );
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
        eventPublisher.publishEvent(LibraryCacheInvalidateEvent.statusOnly(user.getId()));
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


    // 날짜별 포커스 기록 조회 커서 페이징
    public CursorResponse<LibraryViewDto.UserBookResponseDto> viewFocusRecordByDate(
            User user,
            LocalDate date,
            Long cursor,
            int size
    ) {
            Pageable pageable = PageRequest.of(0, size + 1);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            Slice<Focus> focuses = focusRepository.findByLibraryWithCursorByDate(
                    user,
                    start,
                    end,
                    cursor,
                    pageable
            );

            List<Focus> content = focuses.getContent();
            boolean hasNext = content.size() > size;
            List<Focus> pageContent = hasNext ? content.subList(0, size) : content;

            List<LibraryViewDto.UserBookResponseDto> bookItems = pageContent.stream()
                    .map(focus -> new LibraryViewDto.UserBookResponseDto(
                            focus.getLibrary().getBook().getId(),
                            focus.getLibrary().getBook().getTitle(),
                            focus.getLibrary().getBook().getAuthor(),
                            focus.getDurationSec() == null ? 0 : focus.getDurationSec(),
                            focus.getLibrary().getBook().getCoverImageUrl()
                    ))
                    .toList();

            Long nextCursor = hasNext ? pageContent.get(pageContent.size() - 1).getId() : null;

            return CursorResponse.of(bookItems, nextCursor, hasNext);
    }

}
