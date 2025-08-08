package umc.nook.bookshelves.service;



import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import umc.nook.book.domain.Book;
import umc.nook.book.domain.CategoryCount;
import umc.nook.book.domain.QBook;
import umc.nook.book.repository.BookRepository;
import umc.nook.bookshelves.domain.QUserBookShelf;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.bookshelves.repository.UserBookshelfRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.records.domain.BookRecord;
import umc.nook.records.domain.ChatRecord;
import umc.nook.records.domain.QBookRecord;
import umc.nook.records.domain.QChatRecord;
import umc.nook.review.domain.QReview;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookShelfService {

    private final BookRepository bookRepository;
    private final UserBookshelfRepository userBookshelfRepository;
    private final BookShelfQueryService bookShelfQueryService;

    // 서재에 책 등록
    @Transactional
    public String registerBook(BookShelfDTO.RegisterBookDTO registerBookDTO, User user) {
        Book thisBook = bookRepository.findByBookId(registerBookDTO.getBookId());
        if (thisBook == null) {
            throw new CustomException(ErrorCode.BOOK_NOT_FOUND);
        }
        boolean alreadyRegistered = userBookshelfRepository.existsByUserAndBook(user, thisBook);
        if (alreadyRegistered) {
            throw new CustomException(ErrorCode.DUPLICATE_BOOK_IN_SHELF);
        }
        UserBookShelf userBook = UserBookShelf.builder()
                .book(thisBook)
                .recordedAt(registerBookDTO.getDate())
                .readingStatus(registerBookDTO.getReadingStatus())
                .user(user)
                .build();

        userBookshelfRepository.save(userBook);
        return  "서재에 책이 성공적으로 등록되었습니다.";
    }

    // 책 삭제
    @Transactional
    public String deleteBook(Long bookId, User user) {
        Book thisBook = bookRepository.findByBookId(bookId);
        if (thisBook == null) {
            throw new CustomException(ErrorCode.BOOK_NOT_FOUND);
        }
        boolean isRegistered = userBookshelfRepository.existsByUserAndBook(user, thisBook);
        if (!isRegistered) {
            throw new CustomException(ErrorCode.BOOK_NOT_EXIST);
        }
        userBookshelfRepository.deleteByUserAndBook(user,thisBook);
        return "책이 성공적으로 서재에서 삭제되었습니다.";
    }

    @Transactional
    public String changeBookState(Long bookId, User user){
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        UserBookShelf userBook = userBookshelfRepository.findByUserAndBook(user, book);
        if (userBook == null)
            throw new CustomException(ErrorCode.BOOK_NOT_EXIST);

        ReadingStatus currentStatus = userBook.getReadingStatus();
        if (currentStatus == ReadingStatus.READING) {
            throw new CustomException(ErrorCode.ALREADY_READING);
        }

        if (currentStatus == ReadingStatus.FINISHED) {
            throw new CustomException(ErrorCode.ALREADY_FINISHED);
        }

        userBook.updateReadingStatus(ReadingStatus.READING);
        userBookshelfRepository.save(userBook);

        return "해당 책이 '독서중' 상태로 변경되었습니다.";
    }


    // 서재 통계 조회
    public BookShelfDTO.BooksInsightDTO viewBooksInsight(User user) {
        return bookShelfQueryService.getBooksInsight(user);
    }

    public BookShelfDTO.RegisteredBookListResponseDTO viewRegisteredDatesInMonth(User user, YearMonth yearMonth) {
        List<LocalDate> dates = userBookshelfRepository.findAllByUser(user).stream()
                .filter(b -> b.getBook() != null)
                .map(b -> b.getCreatedDate().toLocalDate())
                .filter(date -> YearMonth.from(date).equals(yearMonth))
                .sorted()
                .collect(Collectors.toList());
        return new BookShelfDTO.RegisteredBookListResponseDTO(dates);
    }

    @Transactional
    public BookShelfDTO.CursorPageDTO<BookShelfDTO.UserBookListResponseDTO> getUserBooks(
            User user, ReadingStatus statusStr, Long cursorBookId, int size, String sort) {

        List<BookShelfDTO.UserBookListResponseDTO> dtoList = bookShelfQueryService.getUserBooks(user, statusStr, cursorBookId, size, sort);

        boolean hasNext = dtoList.size() > size;
        Long nextCursor = null;

        if (hasNext) {
            BookShelfDTO.UserBookListResponseDTO last = dtoList.remove(size); // size+1 번째 요소 제거
            nextCursor = last.getBookId();
        }

        return new BookShelfDTO.CursorPageDTO<>(dtoList, nextCursor, hasNext);
    }

    public List<BookShelfDTO.DailyBooksResponseDTO> getMonthlyBooks(User user, YearMonth yearMonth) {
        return bookShelfQueryService.getMonthlyBooks(user, yearMonth);
    }


    // 지금 독서중인 책
    public BookShelfDTO.BookThumbnail viewReadingBooks(User user) {
        List<UserBookShelf> userBookShelfList = userBookshelfRepository.findByUserAndReadingStatusOrderByCreatedDateDesc(user,ReadingStatus.READING);
        if (userBookShelfList.isEmpty()) {
            throw new CustomException(ErrorCode.BOOKSHELF_IS_EMPTY);
        }
        return new BookShelfDTO.BookThumbnail(userBookShelfList.get(0).getBook());
    }

    // 이번주 서재에 등록한 책
    public List<BookShelfDTO.BookThumbnail> viewWeeklyBookShelf(User user) {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(DayOfWeek.MONDAY);
        // 시작일 00:00:00 ~ 오늘 23:59:59
        LocalDateTime startOfWeek = monday.atStartOfDay();
        LocalDateTime endOfToday = now.atTime(LocalTime.MAX);

        List<UserBookShelf> weeklyBooks = userBookshelfRepository
                .findByUserAndCreatedDateBetweenOrderByCreatedDateDesc(user, startOfWeek, endOfToday);

        return weeklyBooks.stream()
                .map(ubs -> new BookShelfDTO.BookThumbnail(ubs.getBook()))
                .collect(Collectors.toList());
    }

}
