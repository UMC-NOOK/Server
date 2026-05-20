package app.nook.library.service;

import app.nook.book.domain.Book;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.BookRepository;
import app.nook.book.service.BookAccessService;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.library.domain.Library;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.dto.ReadingStatusRequestDto;
import app.nook.library.event.LibraryCacheInvalidateEvent;
import app.nook.library.exception.LibraryErrorCode;
import app.nook.library.repository.LibraryRepository;
import app.nook.timeline.service.TimelineCommandService;
import app.nook.user.domain.User;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryCommandService {

    private final LibraryRepository libraryRepository;
    private final BookRepository bookRepository;
    private final FocusRepository focusRepository;
    private final TimelineCommandService timelineCommandService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final BookAccessService bookAccessService;

    @Transactional
    public LibraryViewDto.BookStatusResponseDto registerBook(Long userId, Long bookId) {
        // 등록 대상 도서와 사용자 조회
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
        User user = getUser(userId);
        bookAccessService.assertCanAddToLibrary(user, book);

        // 이미 서재에 있는 도서는 중복 등록 차단
        if (libraryRepository.findByUserIdAndBook(userId, book).isPresent()) {
            throw new CustomException(LibraryErrorCode.BOOK_ALREADY_EXIST);
        }

        // 서재 등록 후 타임라인과 캐시를 함께 갱신
        Library library = new Library(user, book);
        Library savedLibrary;
        try {
            savedLibrary = libraryRepository.saveAndFlush(library);
        } catch (DataIntegrityViolationException exception) {
            throw new CustomException(LibraryErrorCode.BOOK_ALREADY_EXIST);
        }

        timelineCommandService.appendRegister(savedLibrary);
        return toBookStatusResponse(savedLibrary);
    }

    @Transactional
    public LibraryViewDto.BookStatusResponseDto deleteByBookId(Long userId, Long bookId) {
        // 삭제 대상 도서와 서재 등록 여부 확인
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
        Library library = libraryRepository.findByUserIdAndBook(userId, book)
                .orElseThrow(() -> new CustomException(LibraryErrorCode.BOOK_NOT_EXIST));

        // 삭제 이후 상태 캐시와 월별 캐시를 함께 무효화
        libraryRepository.delete(library);

        eventPublisher.publishEvent(LibraryCacheInvalidateEvent.monthly(userId));
        return new LibraryViewDto.BookStatusResponseDto(bookId, null, null);
    }

    @Transactional
    public LibraryViewDto.BookStatusResponseDto changeReadingStatus(Long userId, ReadingStatusRequestDto requestDto) {
        // 상태 변경 대상 도서와 서재 엔티티 조회
        Book book = bookRepository.findById(requestDto.bookId())
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
        Library library = libraryRepository.findByUserIdAndBook(userId, book)
                .orElseThrow(() -> new CustomException(LibraryErrorCode.BOOK_NOT_EXIST));

        // 동일 상태 요청은 변경으로 보지 않고 에러 처리
        if (library.getReadingStatus() == requestDto.readingStatus()) {
            throw new CustomException(LibraryErrorCode.BOOK_STATUS_INVALID);
        }

        // 상태 변경 이력과 첫 페이지 캐시를 함께 갱신
        LocalDateTime occurredAt = LocalDateTime.now();
        library.updateStatus(requestDto.readingStatus());
        timelineCommandService.appendStatusChanged(library, occurredAt);
        return toBookStatusResponse(library);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    private LibraryViewDto.BookStatusResponseDto toBookStatusResponse(Library library) {
        return new LibraryViewDto.BookStatusResponseDto(
                library.getBook().getId(),
                library.getId(),
                library.getReadingStatus()
        );
    }
}
