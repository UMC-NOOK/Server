package umc.nook.bookshelves.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.book.domain.QBook;
import umc.nook.bookshelves.domain.QUserBookShelf;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.bookshelves.dto.SortType;
import umc.nook.records.domain.QBookRecord;
import umc.nook.review.domain.QReview;
import umc.nook.users.domain.User;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static umc.nook.records.domain.QBookRecord.bookRecord;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
class BookShelfCustomRepositoryImpl implements BookShelfCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Transactional(readOnly = true)
    public BookShelfDTO.PageDTO<BookShelfDTO.UserBookListResponseDTO> getUserBooks(
            User user,
            ReadingStatus status,
            int page,
            int size,
            SortType sort
    ) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;
        QBook book = QBook.book;
        QReview review = QReview.review;
        QBookRecord bookRecord = QBookRecord.bookRecord;

        BooleanBuilder condition = new BooleanBuilder()
                .and(ub.user.eq(user))
                .and(ub.readingStatus.eq(status));

        // 정렬 조건
        OrderSpecifier<?> primaryOrder;
        OrderSpecifier<?> secondaryOrder = book.bookId.desc();

        switch (sort) {
            case RATING -> primaryOrder = review.rating.desc().nullsLast();
            case RECENT -> primaryOrder = Expressions.stringPath("latestRecordDate").desc().nullsLast();
            case TITLE -> {
                primaryOrder = book.title.asc();
                secondaryOrder = book.bookId.asc();
            }
            case LATEST -> primaryOrder = ub.createdDate.desc();
            default -> primaryOrder = ub.recordedAt.desc();
        }

        // ✅ 전체 개수 (totalPage 계산용)
        long totalCount = queryFactory
                .select(ub.count())
                .from(ub)
                .where(condition)
                .fetchOne();

        long totalPage = (long) Math.ceil((double) totalCount / size);

        // ✅ 데이터 조회 (size+1로 hasNext 확인)
        List<Tuple> tuples = queryFactory
                .select(
                        book.bookId,
                        book.title,
                        book.author,
                        book.publisher,
                        book.coverImageUrl,
                        ub.readingStatus.stringValue(),
                        book.isbn13,
                        review.rating,
                        book.publicationDate,
                        Expressions.stringTemplate(
                                "COALESCE({0}, {1})",
                                JPAExpressions.select(bookRecord.createdDate.max())
                                        .from(bookRecord)
                                        .where(bookRecord.bookshelf.eq(ub)),
                                ub.recordedAt
                        ).as("latestRecordDate")
                )
                .from(ub)
                .join(ub.book, book)
                .leftJoin(review).on(
                        review.book.eq(book),
                        review.user.eq(user)
                )
                .where(condition)
                .orderBy(primaryOrder, secondaryOrder)
                .offset(page * size)
                .limit(size + 1)
                .fetch();

        boolean hasNext = tuples.size() > size;
        if (hasNext) {
            tuples = tuples.subList(0, size);
        }

        List<BookShelfDTO.UserBookListResponseDTO> content = tuples.stream()
                .map(t -> new BookShelfDTO.UserBookListResponseDTO(
                        t.get(book.bookId),
                        t.get(book.title),
                        t.get(book.author),
                        t.get(book.publisher),
                        t.get(book.coverImageUrl),
                        t.get(ub.readingStatus.stringValue()),
                        t.get(book.isbn13),
                        t.get(review.rating) != null ? t.get(review.rating).intValue() : 0,
                        t.get(book.publicationDate)
                ))
                .toList();

        return new BookShelfDTO.PageDTO<>(
                content,
                page,
                size,
                hasNext,
                totalPage
        );
    }
    // 월별 책 조회
    @Transactional(readOnly = true)
    public List<BookShelfDTO.DailyBooksResponseDTO> getMonthlyBooks(User user, YearMonth yearMonth) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;
        QBook book = QBook.book;

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Tuple> result = queryFactory
                .select(ub.recordedAt, book.bookId, book.title, book.coverImageUrl, book.author)
                .from(ub)
                .join(ub.book, book)
                .where(
                        ub.user.eq(user),
                        ub.recordedAt.isNotNull(),
                        ub.recordedAt.between(startDate, endDate),
                        ub.readingStatus.ne(ReadingStatus.BOOKMARK)
                )
                .orderBy(ub.recordedAt.asc())
                .fetch();

        return result.stream()
                .filter(t -> t.get(ub.recordedAt) != null)
                .map(t -> new BookShelfDTO.DailyBooksResponseDTO(
                        t.get(ub.recordedAt),
                        new BookShelfDTO.MonthlyBookThumbnail(
                                t.get(book.bookId),
                                t.get(book.title),
                                t.get(book.coverImageUrl),
                                t.get(book.author)
                        )
                ))
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
