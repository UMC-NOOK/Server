package app.nook.book.service;

import app.nook.book.domain.Book;
import app.nook.book.domain.BookViewHistory;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.repository.BookViewHistoryRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookViewHistoryService {

    private static final int MAX_HISTORY_SIZE = 5;

    private final BookViewHistoryRepository bookViewHistoryRepository;
    private final UserRepository userRepository;
    private final PresignedUrlService presignedUrlService;

    public List<BookResponseDto.RecentlyViewedBookDto> getRecentlyViewedBooks(User user) {
        return bookViewHistoryRepository.findAllRecent(user, PageRequest.of(0, MAX_HISTORY_SIZE)).stream()
                .limit(MAX_HISTORY_SIZE)
                .map(history -> {
                    Book book = history.getBook();
                    return new BookResponseDto.RecentlyViewedBookDto(
                            book.getId(),
                            book.getTitle(),
                            book.getAuthor(),
                            presignedUrlService.resolveImageUrl(user.getId(), book.getCoverImageKey())
                    );
                })
                .toList();
    }

    @Transactional
    public void saveBookView(User user, Book book) {
        // 이력 행이 없는 최초 조회도 직렬화하기 위해 사용자 행을 잠근다.
        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        bookViewHistoryRepository.findExisting(lockedUser, book)
                .ifPresent(existingHistory -> {
                    bookViewHistoryRepository.delete(existingHistory);
                    // 동일한 사용자·도서 이력을 재삽입하기 전에 삭제를 반영해 유니크 제약 충돌을 막는다.
                    bookViewHistoryRepository.flush();
                });

        List<BookViewHistory> histories = bookViewHistoryRepository.findAllRecentForUpdate(lockedUser);

        // 새 이력 한 자리를 확보하면서 기존 최대 개수 초과 데이터도 함께 정리한다.
        histories.stream()
                .skip(MAX_HISTORY_SIZE - 1L)
                .forEach(bookViewHistoryRepository::delete);

        bookViewHistoryRepository.save(BookViewHistory.builder()
                .user(lockedUser)
                .book(book)
                .build());
    }
}
