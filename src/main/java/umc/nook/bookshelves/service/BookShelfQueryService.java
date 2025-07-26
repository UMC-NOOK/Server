package umc.nook.bookshelves.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.nook.book.domain.QBook;
import umc.nook.bookshelves.domain.QUserBookShelf;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.records.domain.QBookRecord;
import umc.nook.records.dto.RecordDTO;
import umc.nook.review.domain.QReview;
import umc.nook.users.domain.User;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static umc.nook.records.domain.QBookRecord.bookRecord;
import static umc.nook.records.domain.QChatRecord.chatRecord;

@Service
@RequiredArgsConstructor
public class BookShelfQueryService {

    private final JPAQueryFactory queryFactory;

    @Transactional
    public List<BookShelfDTO.UserBookListResponseDTO> getUserBooks(User user, ReadingStatus status, Long cursorBookId, int size, String sort) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;
        QBook book = QBook.book;
        QReview review = QReview.review;
        QBookRecord record = bookRecord;

        BooleanBuilder condition = new BooleanBuilder()
                .and(ub.user.eq(user))
                .and(ub.readingStatus.eq(status));

        if (cursorBookId != null) {
            condition.and(book.bookId.lt(cursorBookId));
        }

        List<Tuple> tuples;

        if ("rating".equalsIgnoreCase(sort)) {
            tuples = queryFactory
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
        } else if ("recent".equalsIgnoreCase(sort)) {
            tuples = queryFactory
                    .select(
                            book.bookId,
                            book.title,
                            book.author,
                            book.publisher,
                            book.coverImageUrl,
                            ub.readingStatus.stringValue(),
                            review.rating,
                            Expressions.stringTemplate(
                                    "COALESCE(GREATEST({0}, {1}), {2})",
                                    JPAExpressions
                                            .select(chatRecord.createdDate.max())
                                            .from(chatRecord)
                                            .where(chatRecord.bookshelf.eq(ub)),
                                    JPAExpressions
                                            .select(bookRecord.createdDate.max())
                                            .from(bookRecord)
                                            .where(bookRecord.bookshelf.eq(ub)),
                                    ub.recordedAt  // fallback
                            ).as("latestRecordDate")
                    )
                    .from(ub)
                    .join(ub.book, book)
                    .leftJoin(review).on(
                            review.book.eq(book),
                            review.user.eq(user)
                    )
                    .where(condition)
                    .orderBy(Expressions.stringPath("latestRecordDate").desc().nullsLast())
                    .limit(size + 1)
                    .fetch();
        } else {
            tuples = queryFactory
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
                    .orderBy(switch (sort.toLowerCase()) {
                        case "title" -> book.title.asc();
                        case "latest" -> ub.createdDate.desc();
                        default -> ub.recordedAt.desc();
                    })
                    .limit(size + 1)
                    .fetch();
        }

        return tuples.stream()
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
    }

    @Transactional
    public List<BookShelfDTO.DailyBooksResponseDTO> getMonthlyBooks(User user, YearMonth yearMonth) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;
        QBook book = QBook.book;

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Tuple> result = queryFactory
                .select(ub.recordedAt, book.bookId, book.coverImageUrl)
                .from(ub)
                .join(ub.book, book)
                .where(
                        ub.user.eq(user),
                        ub.recordedAt.isNotNull(),
                        ub.recordedAt.between(startDate, endDate)
                )
                .orderBy(ub.recordedAt.asc())
                .fetch();

        Map<LocalDate, List<BookShelfDTO.BookThumbnail>> grouped = result.stream()
                .filter(t -> t.get(ub.recordedAt) != null)
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

        return grouped.entrySet().stream()
                .map(entry -> new BookShelfDTO.DailyBooksResponseDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional
    public BookShelfDTO.BooksInsightDTO getBooksInsight(User user) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;
        QBookRecord record = bookRecord;

        Long totalCount = queryFactory
                .select(ub.count())
                .from(ub)
                .where(ub.user.eq(user))
                .fetchOne();

        Long recordCount = queryFactory
                .select(record.count())
                .from(record)
                .where(record.bookshelf.user.eq(user))
                .fetchOne();

        List<Tuple> statusCounts = queryFactory
                .select(ub.readingStatus, ub.count())
                .from(ub)
                .where(ub.user.eq(user))
                .groupBy(ub.readingStatus)
                .fetch();

        List<BookShelfDTO.BooksInsightTypeDTO> typeDTOs = statusCounts.stream()
                .map(t -> new BookShelfDTO.BooksInsightTypeDTO(
                        t.get(ub.readingStatus),
                        t.get(ub.count()).intValue()
                ))
                .toList();

        return new BookShelfDTO.BooksInsightDTO(totalCount, recordCount, typeDTOs);
    }

    @Transactional
    public RecordDTO.MonthlyRecordRateResponseDTO viewRecordRate(User user, Year year) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;

        Map<Integer, List<UserBookShelf>> booksByMonth = queryFactory
                .selectFrom(ub)
                .where(
                        ub.user.eq(user),
                        ub.createdDate.year().eq(year.getValue())
                )
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(b -> b.getCreatedDate().getMonthValue()));

        List<RecordDTO.MonthlyRecordRateResponseDTO.MonthRate> rates = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            List<UserBookShelf> booksInMonth = booksByMonth.getOrDefault(month, List.of());

            if (booksInMonth.isEmpty()) {
                rates.add(new RecordDTO.MonthlyRecordRateResponseDTO.MonthRate(month, 0.0));
                continue;
            }

            long total = booksInMonth.size();
            long recorded = booksInMonth.stream()
                    .filter(b -> b.getRecordedAt() != null)
                    .count();

            double rate = (recorded * 100.0) / total;
            rates.add(new RecordDTO.MonthlyRecordRateResponseDTO.MonthRate(month, rate));
        }

        return new RecordDTO.MonthlyRecordRateResponseDTO(rates);
    }

}
