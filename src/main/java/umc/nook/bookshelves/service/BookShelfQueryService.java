package umc.nook.records.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.nook.book.domain.QBook;
import umc.nook.bookshelves.domain.QUserBookShelf;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.records.domain.QBookRecord;
import umc.nook.review.domain.QReview;
import umc.nook.users.domain.User;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookShelfQueryService {

    private final JPAQueryFactory queryFactory;

    public List<Tuple> findUserBooksSorted(
            User user, ReadingStatus status, Long cursorBookId, int size, String sort
    ) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;
        QRecord record = QRecord.record;
        QBook book = QBook.book;
        QReview review = QReview.review;

        QBookRecord record = QBookRecord.bookRecord;

        BooleanBuilder condition = new BooleanBuilder()
                .and(ub.user.eq(user))
                .and(ub.readingStatus.eq(status));

        if (cursorBookId != null) {
            condition.and(book.bookId.lt(cursorBookId));
        }

        if ("rating".equalsIgnoreCase(sort)) {
            return queryFactory
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
            return queryFactory
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

        // 기본 정렬
        OrderSpecifier<?> orderSpecifier = switch (sort.toLowerCase()) {
            case "title" -> book.title.asc();
            case "latest" -> ub.createdDate.desc();
            default -> ub.recordedAt.desc();
        };

        return queryFactory
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
                .orderBy(orderSpecifier)
                .limit(size + 1)
                .fetch();
    }
}
