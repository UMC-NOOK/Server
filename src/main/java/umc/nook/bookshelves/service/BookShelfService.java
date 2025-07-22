package umc.nook.bookshelves.service;



import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.nook.book.domain.Book;
import umc.nook.book.domain.QBook;
import umc.nook.book.repository.BookRepository;
import umc.nook.bookshelves.domain.QUserBookShelf;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.bookshelves.repository.UserBookshelfRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.records.domain.QBookRecord;
import umc.nook.review.domain.QReview;
import umc.nook.users.domain.User;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookShelfService {

    private final BookRepository bookRepository;
    private final UserBookshelfRepository userBookshelfRepository;
    private final JPAQueryFactory queryFactory;

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

    public List<BookShelfDTO.DailyBooksResponseDTO> getMonthlyBooks(User user, YearMonth yearMonth) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;
        QBook book = QBook.book;

        // 월 시작일과 종료일 계산
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // recordedAt이 null이 아닌 월 내 책들 조회
        List<Tuple> result = queryFactory
                .select(ub.recordedAt, book.bookId, book.coverImageUrl)
                .from(ub)
                .join(ub.book,book)
                .where(
                        ub.user.eq(user),
                        ub.recordedAt.isNotNull(),
                        ub.recordedAt.between(startDate, endDate)
                )
                .orderBy(ub.recordedAt.asc())
                .fetch();

        // 날짜별로 책 썸네일들을 그룹화
        Map<LocalDate, List<BookShelfDTO.BookThumbnail>> groupedBooks = result.stream()
                .filter(t -> t.get(ub.recordedAt) != null) // null safety
                .collect(Collectors.groupingBy(
                        t -> t.get(ub.recordedAt),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                t -> new BookShelfDTO.BookThumbnail(
                                        t.get(book.bookId),
                                        t.get(book.coverImageUrl)
                                ),
                                Collectors.toList()
                        )
                ));

        // DTO로 변환
        return groupedBooks.entrySet().stream()
                .map(entry -> new BookShelfDTO.DailyBooksResponseDTO(entry.getKey(), entry.getValue()))
                .toList();
    }


    @Transactional
    public BookShelfDTO.CursorPageDTO<BookShelfDTO.UserBookListResponseDTO> getUserBooks(
            User user, String statusStr, Long cursorBookId, int size, String sort) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;
        QBook book = QBook.book;
        QReview review = QReview.review;
        QBookRecord record = QBookRecord.bookRecord;

        ReadingStatus status = ReadingStatus.valueOf(statusStr.toUpperCase());

        BooleanBuilder condition = new BooleanBuilder()
                .and(ub.user.eq(user))
                .and(ub.readingStatus.eq(status));

        if (cursorBookId != null) {
            condition.and(book.bookId.lt(cursorBookId));
        }

        List<Tuple> result;

        // 별점 많은 순으로 정렬
        if ("rating".equalsIgnoreCase(sort)) {
            result = queryFactory
                    .select(
                            book.bookId,
                            book.title,
                            book.author,
                            book.publisher,
                            book.coverImageUrl,
                            ub.readingStatus.stringValue(),
                            review.rating
                    )
                    .from(ub)
                    .join(ub.book, book)
                    .leftJoin(review).on(
                            review.book.eq(book),
                            review.user.eq(user)
                    )
                    .where(condition)
                    .orderBy(review.rating.desc().nullsLast())
                    .limit(size + 1)
                    .fetch();
        }
        else if ("recent".equalsIgnoreCase(sort)) {
            result = queryFactory
                    .select(
                            book.bookId,
                            book.title,
                            book.author,
                            book.publisher,
                            book.coverImageUrl,
                            ub.readingStatus.stringValue()
                    )
                    .from(ub)
                    .join(ub.book, book)
                    .leftJoin(record).on(record.bookshelf.eq(ub))
                    .where(condition)
                    .orderBy(record.createdDate.desc().nullsLast())
                    .limit(size + 1)
                    .fetch();
        }
        else {
            OrderSpecifier<?> orderSpecifier = switch (sort.toLowerCase()) {
                case "title" -> book.title.asc();
                case "latest" -> ub.createdDate.desc();
                default -> ub.recordedAt.desc();
            };

            result = queryFactory
                    .select(
                            book.bookId,
                            book.title,
                            book.author,
                            book.publisher,
                            book.coverImageUrl,
                            ub.readingStatus.stringValue(),
                            review.rating // leftJoin 안 하더라도 컬럼 접근 가능해야 하므로 유지
                    )
                    .from(ub)
                    .join(ub.book, book)
                    .leftJoin(review).on(
                            review.book.eq(book),
                            review.user.eq(user)
                    )
                    .where(condition)
                    .orderBy(orderSpecifier)
                    .limit(size + 1)
                    .fetch();
        }

        List<BookShelfDTO.UserBookListResponseDTO> content = result.stream()
                .limit(size)
                .map(t -> new BookShelfDTO.UserBookListResponseDTO(
                        t.get(book.bookId),
                        t.get(book.title),
                        t.get(book.author),
                        t.get(book.publisher),
                        t.get(book.coverImageUrl),
                        t.get(ub.readingStatus.stringValue()),
                        t.get(review.rating) != null ? t.get(review.rating).intValue() : 0
                ))
                .toList();

        Long nextCursor = result.size() > size ? result.get(size).get(book.bookId) : null;
        boolean hasNext = result.size() > size;

        return new BookShelfDTO.CursorPageDTO<>(content, nextCursor, hasNext);
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


}
