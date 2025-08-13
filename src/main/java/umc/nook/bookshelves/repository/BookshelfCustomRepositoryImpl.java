package umc.nook.bookshelves.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.book.domain.QBook;
import umc.nook.bookshelves.domain.QUserBookShelf;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.bookshelves.dto.SortType;
import umc.nook.bookshelves.repository.BookShelfCustomRepository;
import umc.nook.records.domain.QBookRecord;
import umc.nook.records.domain.QChatRecord;
import umc.nook.records.dto.RecordDTO;
import umc.nook.review.domain.QReview;
import umc.nook.users.domain.User;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static umc.nook.records.domain.QBookRecord.bookRecord;
import static umc.nook.records.domain.QChatRecord.chatRecord;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
class BookShelfCustomRepositoryImpl implements BookShelfCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Transactional
    public List<BookShelfDTO.UserBookListResponseDTO> getUserBooks(User user, ReadingStatus status, int page, int size, SortType sort) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;
        QBook book = QBook.book;
        QReview review = QReview.review;
        QBookRecord bookRecord = QBookRecord.bookRecord;
        QChatRecord chatRecord = QChatRecord.chatRecord;

        BooleanBuilder condition = new BooleanBuilder()
                .and(ub.user.eq(user))
                .and(ub.readingStatus.eq(status));

        List<Tuple> tuples;

        // 내가 준 별점순
        if (sort.equals(SortType.RATING)) {
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
        } else if (sort.equals(SortType.RECENT)) {
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
                    .orderBy(switch (sort) {
                        case TITLE -> book.title.asc();
                        case LATEST -> ub.createdDate.desc();
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
                                        t.get(book.title),
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

        Long totalCount = Optional.ofNullable(
                queryFactory
                        .select(ub.count())
                        .from(ub)
                        .where(
                                ub.user.eq(user)
                                        .and(ub.readingStatus.ne(ReadingStatus.BOOKMARK))
                        )
                        .fetchOne()
        ).orElse(0L);


        Long recordCount = Optional.ofNullable(queryFactory
                .select(record.count())
                .from(record)
                .where(record.bookshelf.user.eq(user))
                .fetchOne()
        ).orElse(0L);

        // 상태별 집계
        List<Tuple> statusCounts = queryFactory
                .select(ub.readingStatus, ub.count())
                .from(ub)
                .where(ub.user.eq(user))
                .groupBy(ub.readingStatus)
                .fetch();

        // Tuple -> Map<ReadingStatus, Long>
        Map<ReadingStatus, Long> countMap = statusCounts.stream()
                .collect(Collectors.toMap(
                        t -> t.get(ub.readingStatus),
                        t -> t.get(ub.count())
                ));

        // 필요한 상태(READING, COMPLETED, BOOKMARK)를 고정 순서로 0 기본값 포함해 생성
        List<BookShelfDTO.BooksInsightTypeDTO> typeDTOs = Stream.of(
                        ReadingStatus.READING,
                        ReadingStatus.FINISHED,
                        ReadingStatus.BOOKMARK
                )
                .map(st -> new BookShelfDTO.BooksInsightTypeDTO(
                        st,
                        countMap.getOrDefault(st, 0L).intValue()
                ))
                .toList();
        return new BookShelfDTO.BooksInsightDTO(totalCount, recordCount, typeDTOs);
    }




}
