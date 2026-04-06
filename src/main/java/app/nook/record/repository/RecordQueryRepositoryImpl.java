package app.nook.record.repository;

import app.nook.book.domain.QBook;
import app.nook.library.domain.QLibrary;
import app.nook.record.domain.Record;
import app.nook.record.domain.QRecord;
import app.nook.record.domain.QRecordImage;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.domain.enums.SortType;
import app.nook.record.dto.BookRecordDto;
import app.nook.record.dto.RecordListCursor;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class RecordQueryRepositoryImpl implements RecordQueryRepository{

    private final JPAQueryFactory queryFactory;


    QRecord record = QRecord.record;
    QRecordImage recordImage = QRecordImage.recordImage;


    // 정렬 조건에 따라 책별 독서 기록을 그룹핑해서 반환
    public List<BookRecordDto.BookRecordItemDto> findRecordsByCursor(
            Long userId, RecordListCursor cursor, SortType sortType, int size
    ){
        QBook book = QBook.book;
        QLibrary library = QLibrary.library;
        QRecord subRecord = new QRecord("subRecord");
        NumberExpression<Long> recordCount = record.count();
        DateTimeExpression<LocalDateTime> lastCreatedDate = sortDateExpression(sortType);

        return queryFactory
                .select(Projections.constructor(BookRecordDto.BookRecordItemDto.class,
                        record.library.book.id,
                        record.library.book.title,
                        record.library.book.author,
                        JPAExpressions // 가장 최신의 기록 1개 가져오기
                                .select(subRecord.content)
                                .from(subRecord)
                                .where(
                                        subRecord.library.book.eq(book),
                                        subRecord.library.user.id.eq(userId)
                                )
                                .orderBy(subRecord.createdDate.desc())
                                .limit(1),
                        record.library.book.coverImageUrl,
                        recordCount
                ))
                .from(record)
                .join(record.library, library)
                .join(record.library.book, book)
                .where(
                        library.user.id.eq(userId)
                )
                .groupBy(
                        record.library.book.id,
                        record.library.book.title,
                        record.library.book.author,
                        record.library.book.coverImageUrl
                )
                .having(havingCondition(cursor, sortType, recordCount, book.id, lastCreatedDate))
                .orderBy(orderByConditions(sortType, recordCount, book.id, lastCreatedDate))
                .limit(size + 1)
                .fetch();
    }

    public LocalDateTime findBookBoundaryCreatedDate(Long userId, Long bookId, SortType sortType) {
        QLibrary library = QLibrary.library;

        return queryFactory
                .select(sortDateExpression(sortType))
                .from(record)
                .join(record.library, library)
                .where(
                        library.user.id.eq(userId),
                        library.book.id.eq(bookId)
                )
                .fetchOne();
    }

    // 감정 필터
    private BooleanExpression emotionCondition(EnumPath<Emotion> emotionPath, Emotion emotion) {
        if (emotion == null) {
            return null;
        }
        return emotionPath.eq(emotion);
    }

    // count 순으로 하는 조건은 having 절에, createdDate 순으로 하는 조건은 where 절에 걸리도록
    private BooleanExpression havingCondition(
            RecordListCursor cursor,
            SortType sortType,
            NumberExpression<Long> recordCount,
            NumberExpression<Long> bookId,
            DateTimeExpression<LocalDateTime> lastCreatedDate
    ) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }

        return switch (sortType) {
            case RECENT_RECORDED -> createdDateCursorCondition(
                    cursor.lastCreatedDate(),
                    cursor.bookId(),
                    lastCreatedDate,
                    bookId,
                    false
            );
            case OLDEST_RECORDED -> createdDateCursorCondition(
                    cursor.lastCreatedDate(),
                    cursor.bookId(),
                    lastCreatedDate,
                    bookId,
                    true
            );
            case RECORD_COUNT_DESC -> countCursorCondition(
                    cursor.lastCount(),
                    cursor.bookId(),
                    recordCount,
                    bookId,
                    false
            );
            case RECORD_COUNT_ASC -> countCursorCondition(
                    cursor.lastCount(),
                    cursor.bookId(),
                    recordCount,
                    bookId,
                    true
            );
        };
    }

    // 생성 일자 순으로 커서 설정
    private BooleanExpression createdDateCursorCondition(
            LocalDateTime cursorCreatedDate,
            Long cursorBookId,
            DateTimeExpression<LocalDateTime> lastCreatedDate,
            NumberExpression<Long> bookId,
            boolean ascending
    ) {
        if (cursorCreatedDate == null) {
            return null;
        }

        BooleanExpression byCreatedDate = ascending
                ? lastCreatedDate.gt(cursorCreatedDate)
                : lastCreatedDate.lt(cursorCreatedDate);

        if (cursorBookId == null) {
            return byCreatedDate;
        }

        BooleanExpression tieBreaker = ascending
                ? bookId.gt(cursorBookId)
                : bookId.lt(cursorBookId);

        return byCreatedDate.or(lastCreatedDate.eq(cursorCreatedDate).and(tieBreaker));
    }

    // count 순서로 커서 설정
    private BooleanExpression countCursorCondition(
            Long cursorCount,
            Long cursorBookId,
            NumberExpression<Long> recordCount,
            NumberExpression<Long> bookId,
            boolean ascending
    ) {
        if (cursorCount == null) {
            return null;
        }

        BooleanExpression byCount = ascending
                ? recordCount.gt(cursorCount)
                : recordCount.lt(cursorCount);

        if (cursorBookId == null) {
            return byCount;
        }

        BooleanExpression tieBreaker = ascending
                ? bookId.gt(cursorBookId)
                : bookId.lt(cursorBookId);

        return byCount.or(recordCount.eq(cursorCount).and(tieBreaker));
    }

    // 기록 순서 필터
    private DateTimeExpression<LocalDateTime> sortDateExpression(SortType sortType) {
        return switch (sortType) {
            case RECENT_RECORDED, RECORD_COUNT_ASC, RECORD_COUNT_DESC -> record.createdDate.max();
            case OLDEST_RECORDED -> record.createdDate.min();
        };
    }

    private OrderSpecifier<?>[] orderByConditions(
            SortType sortType,
            NumberExpression<Long> recordCount,
            NumberExpression<Long> bookId,
            DateTimeExpression<LocalDateTime> lastCreatedDate
    ) {
        return switch (sortType) {
            case RECENT_RECORDED -> new OrderSpecifier<?>[]{
                    lastCreatedDate.desc(),
                    bookId.desc()
            };
            case OLDEST_RECORDED -> new OrderSpecifier<?>[]{
                    lastCreatedDate.asc(),
                    bookId.asc()
            };
            case RECORD_COUNT_DESC -> new OrderSpecifier<?>[]{
                    recordCount.desc(),
                    bookId.desc()
            };
            case RECORD_COUNT_ASC -> new OrderSpecifier<?>[]{
                    recordCount.asc(),
                    bookId.asc()
            };
        };
    }

    // 최신날짜 내림차순 커서 조건
    private BooleanExpression bookRecordCursorCondition(Long cursor) {
        if (cursor == null) {
            return null;
        }

        QRecord cursorRecord = new QRecord("cursorRecord");
        return record.createdDate.lt(
                        JPAExpressions
                                .select(cursorRecord.createdDate)
                                .from(cursorRecord)
                                .where(cursorRecord.id.eq(cursor))
                )
                .or(record.createdDate.eq(
                                JPAExpressions
                                        .select(cursorRecord.createdDate)
                                        .from(cursorRecord)
                                        .where(cursorRecord.id.eq(cursor))
                        )
                        .and(record.id.lt(cursor)));
    }

    // 개별 책 기록 조회, 감정별 필터
    public List<Record> findBookRecordsByCursor(
            Long userId, Long bookId, Long cursor, Emotion emotion, int size
    ) {
        QLibrary library = QLibrary.library;
        List<Long> recordIds = queryFactory
                .select(record.id)
                .from(record)
                .join(record.library,library)
                .where(
                        library.user.id.eq(userId),
                        library.book.id.eq(bookId),
                        emotionCondition(record.emotion, emotion),
                        bookRecordCursorCondition(cursor)
                )
                .orderBy(record.createdDate.desc(), record.id.desc())
                .limit(size + 1L)
                .fetch();

        if (recordIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> orderMap = new HashMap<>();
        for (int index = 0; index < recordIds.size(); index++) {
            orderMap.put(recordIds.get(index), index);
        }

        return queryFactory
                .selectDistinct(record)
                .from(record)
                .join(record.library, library).fetchJoin()
                .leftJoin(record.images, recordImage).fetchJoin()
                .where(record.id.in(recordIds))
                .fetch().stream()
                .sorted(Comparator.comparingInt(item -> orderMap.getOrDefault(item.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    // 감상별 독서 기록 개수 조회
    public BookRecordDto.RecordEmotionCountResponse countRecordsByEmotion(Long userId) {
        Long totalCount = Optional.ofNullable(
                queryFactory
                        .select(record.count())
                        .from(record)
                        .where(record.library.user.id.eq(userId))
                        .fetchOne()
        ).orElse(0L);

        List<BookRecordDto.RecordEmotionDto> emotionCounts = queryFactory
                .select(Projections.constructor(BookRecordDto.RecordEmotionDto.class,
                        record.emotion,
                        record.count()
                ))
                .from(record)
                .where(record.library.user.id.eq(userId))
                .groupBy(record.emotion)
                .fetch();

        return new BookRecordDto.RecordEmotionCountResponse(totalCount, emotionCounts);
    }
}
