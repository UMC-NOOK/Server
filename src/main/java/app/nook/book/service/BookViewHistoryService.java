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
        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        bookViewHistoryRepository.findExisting(lockedUser, book)
                .ifPresent(existingHistory -> {
                    bookViewHistoryRepository.delete(existingHistory);
                    bookViewHistoryRepository.flush();
                });

        List<BookViewHistory> histories = bookViewHistoryRepository.findAllRecentForUpdate(lockedUser);

        histories.stream()
                .skip(MAX_HISTORY_SIZE - 1L)
                .forEach(bookViewHistoryRepository::delete);

        bookViewHistoryRepository.save(BookViewHistory.builder()
                .user(lockedUser)
                .book(book)
                .build());
    }
}
