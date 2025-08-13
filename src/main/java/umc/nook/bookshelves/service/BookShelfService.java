package umc.nook.bookshelves.service;



import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.nook.book.domain.Book;
import umc.nook.book.repository.BookRepository;
import umc.nook.bookshelves.controller.BookShelfController;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.bookshelves.dto.SortType;
import umc.nook.bookshelves.repository.UserBookshelfRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.records.repository.BookRecordRepository;
import umc.nook.records.repository.ChatRecordRepository;
import umc.nook.users.domain.User;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookShelfService {

    private final BookRepository bookRepository;
    private final UserBookshelfRepository userBookshelfRepository;
    private final ChatRecordRepository chatRecordRepository;
    private final BookRecordRepository bookRecordRepository;

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
        UserBookShelf userBook = userBookshelfRepository.findByUserAndBook(user, thisBook);
        if (userBook == null) throw new CustomException(ErrorCode.BOOK_NOT_EXIST);
        // ChatRecord 삭제
        chatRecordRepository.deleteAllByBookshelf(userBook);
        // BookRecord 삭제
        bookRecordRepository.deleteAllByBookshelf(userBook);
        userBookshelfRepository.delete(userBook);
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
    @Transactional(readOnly = true)
    public BookShelfDTO.BooksInsightDTO viewBooksInsight(User user) {
        return userBookshelfRepository.getBooksInsight(user);
    }

    @Transactional(readOnly = true)
    public BookShelfDTO.RegisteredBookListResponseDTO viewRegisteredDatesInMonth(User user, YearMonth yearMonth) {
        List<LocalDate> dates = userBookshelfRepository.findAllByUser(user).stream()
                .filter(b -> b.getBook() != null)
                .map(b -> b.getCreatedDate().toLocalDate())
                .filter(date -> YearMonth.from(date).equals(yearMonth))
                .sorted()
                .collect(Collectors.toList());
        return new BookShelfDTO.RegisteredBookListResponseDTO(dates);
    }

    @Transactional(readOnly = true)
    public BookShelfDTO.CursorPageDTO<BookShelfDTO.UserBookListResponseDTO> getUserBooks(
            User user,
            ReadingStatus status,
            int page,
            Integer size,
            SortType sort)
    {
        if (page < 0) {
            throw new CustomException(ErrorCode.INVALID_PAGE);
        }
        final int MAX_PAGE_SIZE = 100;
        final int safeSize = (size == null) ? 8 : size;
        if (safeSize <= 0 || safeSize > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_LIMIT);
        }
        int safePage = Math.max(0, page);
        SortType safeSort = (sort == null) ? SortType.RECENT : sort;
        try {
            // 3) 실제 조회
            return userBookshelfRepository.getUserBooks(user, status, page, safeSize, safeSort);

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid argument in getUserBooks: page={}, size={}, sort={}, msg={}",
                    page, safeSize, safeSort, ex.getMessage());
            throw new CustomException(ErrorCode.INVALID_LIMIT);
        }

    }

    public List<BookShelfDTO.DailyBooksResponseDTO> getMonthlyBooks(User user, YearMonth yearMonth) {
        return userBookshelfRepository.getMonthlyBooks(user, yearMonth);
    }


    // 지금 독서중인 책
    @Transactional(readOnly = true)
    public BookShelfDTO.BookThumbnail viewReadingBooks(User user) {
        List<UserBookShelf> userBookShelfList = userBookshelfRepository.findByUserAndReadingStatusOrderByCreatedDateDesc(user,ReadingStatus.READING);
        if (userBookShelfList.isEmpty()) {
            throw new CustomException(ErrorCode.BOOKSHELF_IS_EMPTY);
        }
        return new BookShelfDTO.BookThumbnail(userBookShelfList.get(0).getBook());
    }

    // 이번주 서재에 등록한 책
    @Transactional(readOnly = true)
    public List<BookShelfDTO.WeeklyBooksDTO> viewWeeklyBookShelf(User user) {
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate monday = now.with(DayOfWeek.MONDAY);
        LocalDateTime startOfWeek = monday.atStartOfDay();
        LocalDateTime endOfToday = now.atTime(LocalTime.MAX);
        List<UserBookShelf> weeklyBooks = userBookshelfRepository
                .findByUserAndCreatedDateBetweenOrderByRecordedAtAsc(user, startOfWeek, endOfToday);

        return weeklyBooks.stream()
                .map(ubs -> new BookShelfDTO.WeeklyBooksDTO(
                        ubs.getRecordedAt().getDayOfMonth(),
                        new BookShelfDTO.BookThumbnail(ubs.getBook())
                ))
                .collect(Collectors.toList());
    }


}
