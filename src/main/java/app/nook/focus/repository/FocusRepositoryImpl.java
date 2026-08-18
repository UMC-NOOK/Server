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
    public List<FocusRangeStatsDto> findOverlappingFocusRanges(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        // 조회 범위와 겹치는 포커스만 반개구간 [start, end) 기준으로 조회
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
            LocalDateTime serverNow,
            Long cursor,
            Pageable pageable
    ) {
        LocalDateTime dayStart = focusDate.atStartOfDay();
        LocalDateTime nextDayStart = focusDate.plusDays(1).atStartOfDay();
        // 오늘은 현재 시각까지만 조회하고 미래 날짜는 빈 범위로 처리
        LocalDateTime effectiveEnd = nextDayStart.isBefore(serverNow) ? nextDayStart : serverNow;
        if (!dayStart.isBefore(effectiveEnd)) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        BooleanBuilder builder = new BooleanBuilder()
                .and(focus.library.user.eq(user))
                .and(focus.startedAt.lt(effectiveEnd))
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
