package app.nook.book.service;

import app.nook.book.domain.Book;
import app.nook.book.domain.BookViewHistory;
import app.nook.book.repository.BookViewHistoryRepository;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookViewHistoryService {

    private static final int MAX_HISTORY_SIZE = 10;

    private final BookViewHistoryRepository bookViewHistoryRepository;

    @Transactional
    public void saveBookView(User user, Book book) {
        bookViewHistoryRepository.findExisting(user, book)
                .ifPresent(existingHistory -> {
                    bookViewHistoryRepository.delete(existingHistory);
                    bookViewHistoryRepository.flush();
                });

        List<BookViewHistory> histories = bookViewHistoryRepository.findAllRecentForUpdate(user);

        if (histories.size() >= MAX_HISTORY_SIZE) {
            BookViewHistory oldest = histories.get(histories.size() - 1);
            bookViewHistoryRepository.delete(oldest);
        }

        bookViewHistoryRepository.save(BookViewHistory.builder()
                .user(user)
                .book(book)
                .build());
    }
}
