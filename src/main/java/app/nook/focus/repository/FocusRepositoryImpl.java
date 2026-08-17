package app.nook.focus.repository;

import app.nook.book.domain.QBook;
import app.nook.focus.domain.Focus;
import app.nook.focus.domain.QFocus;
import app.nook.focus.repository.dto.FocusRangeStatsDto;
import app.nook.focus.repository.dto.QFocusRangeStatsDto;
import app.nook.library.domain.QLibrary;
import app.nook.user.domain.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FocusRepositoryImpl implements FocusRepositoryCustom {

    private static final QFocus focus = QFocus.focus;
    private static final QLibrary library = QLibrary.library;
    private static final QBook book = QBook.book;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<LocalDate> findDistinctFocusDatesByLibraryAndUser(Long libraryId, Long userId) {
        BooleanBuilder builder = new BooleanBuilder()
                .and(focus.library.id.eq(libraryId))
                .and(focus.library.user.id.eq(userId));

        return queryFactory
                .select(focus.focusDate)
                .distinct()
                .from(focus)
                .where(builder)
                .fetch();
    }

    @Override
    public List<FocusRangeStatsDto> findOverlappingFocusRanges(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        BooleanBuilder builder = new BooleanBuilder()
                .and(library.user.id.eq(userId))
                .and(focus.startedAt.lt(end))
                .and(focus.endedAt.isNull().or(focus.endedAt.gt(start)));

        List<FocusRangeStatsDto> rows = queryFactory
                .select(new QFocusRangeStatsDto(
                        focus.startedAt,
                        focus.endedAt,
                        book.id,
                        book.coverImageKey
                ))
                .from(focus)
                .join(focus.library, library)
                .join(library.book, book)
                .where(builder)
                .orderBy(focus.startedAt.asc(), focus.id.asc())
                .fetch();

        return rows;
    }

    @Override
    public Slice<Focus> findByLibraryWithCursorByDate(
            User user,
            LocalDate focusDate,
            Long cursor,
            Pageable pageable
    ) {
        LocalDateTime dayStart = focusDate.atStartOfDay();
        LocalDateTime nextDayStart = focusDate.plusDays(1).atStartOfDay();
        BooleanBuilder builder = new BooleanBuilder()
                .and(focus.library.user.eq(user))
                .and(focus.startedAt.lt(nextDayStart))
                .and(focus.endedAt.isNull().or(focus.endedAt.gt(dayStart)));

        if (cursor != null) {
            builder.and(focus.id.lt(cursor));
        }

        List<Focus> fetched = queryFactory
                .selectFrom(focus)
                .join(focus.library, library).fetchJoin()
                .join(library.book, book).fetchJoin()
                .where(builder)
                .orderBy(focus.id.desc())
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        boolean hasNext = fetched.size() > pageable.getPageSize();
        List<Focus> content = hasNext
                ? new ArrayList<>(fetched.subList(0, pageable.getPageSize()))
                : fetched;

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public Slice<Focus> findRecentByUserWithCursor(
            User user,
            Long cursor,
            Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder()
                .and(focus.library.user.eq(user))
                .and(focus.endedAt.isNotNull());

        if (cursor != null) {
            builder.and(focus.id.lt(cursor));
        }

        List<Focus> fetched = queryFactory
                .selectFrom(focus)
                .join(focus.library, library).fetchJoin()
                .join(library.book, book).fetchJoin()
                .where(builder)
                .orderBy(focus.id.desc())
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        boolean hasNext = fetched.size() > pageable.getPageSize();
        List<Focus> content = hasNext
                ? new ArrayList<>(fetched.subList(0, pageable.getPageSize()))
                : fetched;

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public List<Focus> findRecentDistinctBooksByUser(User user, Pageable pageable) {
        QFocus newerFocus = new QFocus("newerFocus");
        QLibrary newerLibrary = new QLibrary("newerLibrary");

        return queryFactory
                .selectFrom(focus)
                .join(focus.library, library).fetchJoin()
                .join(library.book, book).fetchJoin()
                .where(
                        library.user.eq(user),
                        JPAExpressions
                                .selectOne()
                                .from(newerFocus)
                                .join(newerFocus.library, newerLibrary)
                                .where(
                                        newerLibrary.user.eq(user),
                                        newerLibrary.book.id.eq(book.id),
                                        newerFocus.startedAt.gt(focus.startedAt)
                                                .or(newerFocus.startedAt.eq(focus.startedAt)
                                                        .and(newerFocus.id.gt(focus.id)))
                                )
                                .notExists()
                )
                .orderBy(focus.startedAt.desc(), focus.id.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
