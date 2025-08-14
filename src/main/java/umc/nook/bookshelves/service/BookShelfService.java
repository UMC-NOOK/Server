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

    private Book getBookOrThrow(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
    }

    private UserBookShelf getUserBookShelfOrThrow(User user, Book book) {
        UserBookShelf userBook = userBookshelfRepository.findByUserAndBook(user, book);
        if (userBook == null) {
            throw new CustomException(ErrorCode.BOOK_NOT_EXIST);
        }
        return userBook;
    }

    // 서재에 책 등록
    @Transactional
    public String registerBook(BookShelfDTO.RegisterBookDTO registerBookDTO, User user) {
        Book thisBook = getBookOrThrow(registerBookDTO.getBookId());
        // 해당 날짜에 등록 가능한지 확인
        boolean alreadyRegisteredToday = userBookshelfRepository.existsByUserAndRecordedAt(
                user,
                registerBookDTO.getDate()
        );
        if (alreadyRegisteredToday) {
            throw new CustomException(ErrorCode.ALREADY_REGISTERED_TODAY);
        }
        // 이미 등록된 책인지 확인
        boolean alreadyRegisteredBook = userBookshelfRepository.existsByUserAndBook(
                user,
                thisBook
        );
        if (alreadyRegisteredBook) {
            throw new CustomException(ErrorCode.DUPLICATE_BOOK_IN_SHELF);
        }

        // 책 등록
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
        Book thisBook = getBookOrThrow(bookId);
        UserBookShelf userBook = getUserBookShelfOrThrow(user,thisBook);
        // ChatRecord 삭제
        chatRecordRepository.deleteAllByBookshelf(userBook);
        // BookRecord 삭제
        bookRecordRepository.deleteAllByBookshelf(userBook);
        userBookshelfRepository.delete(userBook);
        return "책이 성공적으로 서재에서 삭제되었습니다.";
    }

    // 독서중으로 상태 변경
    @Transactional
    public String changeBookState(Long bookId, User user){
        Book book = getBookOrThrow(bookId);
        UserBookShelf userBook = getUserBookShelfOrThrow(user,book);
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

    // 월별 책 등록된 날짜 조회
    @Transactional(readOnly = true)
    public BookShelfDTO.RegisteredBookListResponseDTO viewRegisteredDatesInMonth(User user, YearMonth yearMonth) {
        List<LocalDate> dates = userBookshelfRepository.findAllByUser(user).stream()
                .filter(b -> b.getBook() != null)
                .map(b -> b.getRecordedAt())
                .filter(date -> YearMonth.from(date).equals(yearMonth))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return new BookShelfDTO.RegisteredBookListResponseDTO(dates);
    }

    // 서재 책 조회 - 정렬 조건, offset 기반 페이징
    @Transactional(readOnly = true)
    public BookShelfDTO.PageDTO<BookShelfDTO.UserBookListResponseDTO> getUserBooks(
            User user,
            ReadingStatus status,
            int page,
            Integer size,
            SortType sort
    ) {
        int safePage = Math.max(0, page);
        int safeSize = (size == null || size <= 0) ? 8 : size;
        SortType safeSort = (sort == null) ? SortType.RECENT : sort;

        // size + 1로 조회
        List<BookShelfDTO.UserBookListResponseDTO> content =
                userBookshelfRepository.getUserBooks(user, status, safePage, safeSize + 1, safeSort);

        // hasNext 계산
        boolean hasNext = content.size() > safeSize;
        if (hasNext) {
            content = content.subList(0, safeSize);
        }

        return new BookShelfDTO.PageDTO<>(
                content,
                safePage,
                safeSize,
                hasNext
        );
    }

    // 월별 책 조회
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
