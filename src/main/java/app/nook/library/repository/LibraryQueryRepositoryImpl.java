package app.nook.library.repository;

import app.nook.book.domain.QBook;
import app.nook.focus.domain.QFocus;
import app.nook.library.domain.QLibrary;
import app.nook.library.domain.enums.LibrarySortType;
import app.nook.library.dto.LibraryBookCursor;
import app.nook.library.repository.dto.LibraryBookQueryResult;
import app.nook.record.domain.QRecord;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class LibraryQueryRepositoryImpl implements LibraryQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<LibraryBookQueryResult> findAllBooksByCursor(
            Long userId, LibraryBookCursor cursor, LibrarySortType sortType, int size
    ) {
        return switch (sortType) {
            case RECENT_FOCUSED -> findByRecentFocused(userId, cursor, size);
            case RECORD_COUNT_DESC -> findByRecordCount(userId, cursor, size, false);
            case RECORD_COUNT_ASC -> findByRecordCount(userId, cursor, size, true);
            case ALPHABETICAL -> findByAlphabetical(userId, cursor, size);
        };
    }

    // 최근 포커스한 순 — LEFT JOIN focus, GROUP BY, MAX(endedAt) DESC NULLS LAST
    private List<LibraryBookQueryResult> findByRecentFocused(Long userId, LibraryBookCursor cursor, int size) {
        QLibrary library = QLibrary.library;
        QBook book = QBook.book;
        QFocus focus = QFocus.focus;
        DateTimeExpression<LocalDateTime> maxEndedAt = focus.endedAt.max();

        return queryFactory
                .select(
                        library.id,
                        book.id,
                        book.title,
                        book.author,
                        book.coverImageKey,
                        library.readingStatus,
                        maxEndedAt
                )
                .from(library)
                .join(library.book, book)
                .leftJoin(library.focuses, focus)
                .where(library.user.id.eq(userId))
                .groupBy(
                        library.id,
                        book.id,
                        book.title,
                        book.author,
                        book.coverImageKey,
                        library.readingStatus
                )
                .having(recentFocusedHavingCondition(cursor, maxEndedAt, library.id))
                .orderBy(maxEndedAt.desc().nullsLast(), library.id.desc())
                .limit(size + 1L)
                .fetch()
                .stream()
                .map(tuple -> new LibraryBookQueryResult(
                        tuple.get(library.id),
                        tuple.get(book.id),
                        tuple.get(book.title),
                        tuple.get(book.author),
                        tuple.get(book.coverImageKey),
                        tuple.get(library.readingStatus),
                        tuple.get(maxEndedAt),
                        0L
                ))
                .toList();
    }

    // 기록 많은/적은 순 — LEFT JOIN record, GROUP BY, COUNT ASC/DESC
    private List<LibraryBookQueryResult> findByRecordCount(
            Long userId, LibraryBookCursor cursor, int size, boolean ascending
    ) {
        QLibrary library = QLibrary.library;
        QBook book = QBook.book;
        QRecord qRecord = new QRecord("r");
        NumberExpression<Long> cnt = qRecord.id.count();

        OrderSpecifier<?>[] order = ascending
                ? new OrderSpecifier<?>[]{cnt.asc(), library.id.asc()}
                : new OrderSpecifier<?>[]{cnt.desc(), library.id.desc()};

        return queryFactory
                .select(
                        library.id,
                        book.id,
                        book.title,
                        book.author,
                        book.coverImageKey,
                        library.readingStatus,
                        cnt
                )
                .from(library)
                .join(library.book, book)
                .leftJoin(qRecord).on(qRecord.library.id.eq(library.id))
                .where(library.user.id.eq(userId))
                .groupBy(
                        library.id,
                        book.id,
                        book.title,
                        book.author,
                        book.coverImageKey,
                        library.readingStatus
                )
                .having(recordCountHavingCondition(cursor, cnt, library.id, ascending))
                .orderBy(order)
                .limit(size + 1L)
                .fetch()
                .stream()
                .map(tuple -> new LibraryBookQueryResult(
                        tuple.get(library.id),
                        tuple.get(book.id),
                        tuple.get(book.title),
                        tuple.get(book.author),
                        tuple.get(book.coverImageKey),
                        tuple.get(library.readingStatus),
                        null,
                        tuple.get(cnt)
                ))
                .toList();
    }

    // 가나다순 — 집계 없이 WHERE 커서 조건만
    private List<LibraryBookQueryResult> findByAlphabetical(Long userId, LibraryBookCursor cursor, int size) {
        QLibrary library = QLibrary.library;
        QBook book = QBook.book;

        return queryFactory
                .select(
                        library.id,
                        book.id,
                        book.title,
                        book.author,
                        book.coverImageKey,
                        library.readingStatus
                )
                .from(library)
                .join(library.book, book)
                .where(
                        library.user.id.eq(userId),
                        alphabeticalCursorCondition(cursor, book.title, library.id)
                )
                .orderBy(book.title.asc(), library.id.asc())
                .limit(size + 1L)
                .fetch()
                .stream()
                .map(tuple -> new LibraryBookQueryResult(
                        tuple.get(library.id),
                        tuple.get(book.id),
                        tuple.get(book.title),
                        tuple.get(book.author),
                        tuple.get(book.coverImageKey),
                        tuple.get(library.readingStatus),
                        null,
                        0L
                ))
                .toList();
    }

    // RECENT_FOCUSED HAVING 커서 조건
    // 포커스 없는 책(null)은 항상 마지막 — 커서가 null 구간에 있으면 libraryId만 비교
    private BooleanExpression recentFocusedHavingCondition(
            LibraryBookCursor cursor,
            DateTimeExpression<LocalDateTime> maxEndedAt,
            com.querydsl.core.types.dsl.NumberPath<Long> libraryId
    ) {
        if (cursor == null || cursor.isEmpty()) return null;

        if (cursor.lastFocusedAt() != null) {
            return maxEndedAt.lt(cursor.lastFocusedAt())
                    .or(maxEndedAt.eq(cursor.lastFocusedAt()).and(libraryId.lt(cursor.libraryId())))
                    .or(maxEndedAt.isNull());
        }
        return maxEndedAt.isNull().and(libraryId.lt(cursor.libraryId()));
    }

    // RECORD_COUNT HAVING 커서 조건
    private BooleanExpression recordCountHavingCondition(
            LibraryBookCursor cursor,
            NumberExpression<Long> cnt,
            com.querydsl.core.types.dsl.NumberPath<Long> libraryId,
            boolean ascending
    ) {
        if (cursor == null || cursor.isEmpty() || cursor.recordCount() == null) return null;

        BooleanExpression byCount = ascending
                ? cnt.gt(cursor.recordCount())
                : cnt.lt(cursor.recordCount());
        BooleanExpression tieBreaker = ascending
                ? libraryId.gt(cursor.libraryId())
                : libraryId.lt(cursor.libraryId());

        return byCount.or(cnt.eq(cursor.recordCount()).and(tieBreaker));
    }

    // ALPHABETICAL WHERE 커서 조건
    private BooleanExpression alphabeticalCursorCondition(
            LibraryBookCursor cursor,
            StringPath title,
            com.querydsl.core.types.dsl.NumberPath<Long> libraryId
    ) {
        if (cursor == null || cursor.isEmpty() || cursor.title() == null) return null;

        return title.gt(cursor.title())
                .or(title.eq(cursor.title()).and(libraryId.gt(cursor.libraryId())));
    }
}
