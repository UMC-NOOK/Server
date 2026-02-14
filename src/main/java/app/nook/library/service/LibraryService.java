package app.nook.library.service;

import app.nook.book.domain.Book;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryService {

    private final LibraryRepository libraryRepository;
    private final BookRepository bookRepository;
    private final BookTimeLineRepository bookTimeLineRepository;

    // TODO: Book 도메인 에러코드로 추후 수정

    // 서재 책 등록
    @Transactional
    public void save(User user,Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
        // 이미 서재에 등록된 책인지 확인
        if (libraryRepository.findByUserAndBook(user,book) != null)
            throw new CustomException(LibraryErrorCode.BOOK_ALREADY_EXIST);

        // 서재 생성
        Library library = Library.builder()
                .user(user)
                .book(book)
                .build();
        Library savedLibrary = libraryRepository.save(library);

        // 타임라인 업데이트
        BookTimeLine timeLine = TimeLineConverter.toBookTimeLine(
                savedLibrary,
                BookTimeLineType.REGISTER,
                savedLibrary.getCreatedDate().toString(),
                savedLibrary.getId()
        );
        bookTimeLineRepository.save(timeLine);
    }

    // 서재 책 삭제
    @Transactional
    public void deleteById(User user, Long bookId){
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
        Library library = libraryRepository.findByUserAndBook(user,book);
        // 책 존재 확인
        if (library == null)
            throw new CustomException(LibraryErrorCode.BOOK_NOT_EXIST);
        libraryRepository.delete(library);
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
    }

    // 서재 월별 책 조회
//    public LibraryViewDto.MonthlyBookResponseDto viewMonthly(User user, YearMonth yearMonth){
//        libraryRepository.findByUserAndYearMonth(user,yearMonth)
//    }

    // 서재 포커스 시간별 책 조회

    // 서재 상태별 책 조회
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
}
