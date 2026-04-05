package app.nook.record.repository;

import app.nook.book.domain.QBook;
import app.nook.library.domain.QLibrary;
import app.nook.record.domain.Record;
import app.nook.record.domain.QRecord;
import app.nook.record.domain.QRecordImage;
import app.nook.record.domain.enums.Emotion;
import app.nook.record.domain.enums.SortType;
import app.nook.record.dto.BookRecordDto;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

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
            Long userId, Long cursor, SortType sortType, Emotion emotion, int size
    ){
        QBook book = QBook.book;
        QLibrary library = QLibrary.library;
        QRecord subRecord = new QRecord("subRecord");

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
                                        subRecord.library.user.id.eq(userId),
                                        emotionCondition(subRecord.emotion, emotion)
                                )
                                .orderBy(subRecord.createdDate.desc())
                                .limit(1),
                        record.library.book.coverImageUrl,
                        record.count()
                ))
                .from(record)
                .join(record.library, library)
                .join(record.library.book, book)
                .where(
                        library.user.id.eq(userId),
                        emotionCondition(record.emotion, emotion)
                )
                .groupBy(
                        record.library.book.id,
                        record.library.book.title,
                        record.library.book.author,
                        record.library.book.coverImageUrl
                )
                .orderBy(orderByCondition(sortType))
                .where(
                        cursorCondition(cursor, sortType),
                        emotionCondition(record.emotion, emotion),
                        library.user.id.eq(userId)
                )
                .having(havingCondition(cursor, sortType))
                .limit(size + 1)
                .fetch();
    }

    private BooleanExpression emotionCondition(com.querydsl.core.types.dsl.EnumPath<Emotion> emotionPath, Emotion emotion) {
        if (emotion == null) {
            return null;
        }
        return emotionPath.eq(emotion);
    }

    private BooleanExpression cursorCondition(Long cursor, SortType sortType) {
        if (cursor == null) return null;

        return switch (sortType) {
            case RECENT_RECORDED -> record.createdDate.lt(
                    JPAExpressions
                            .select(record.createdDate)
                            .from(record)
                            .where(record.id.eq(cursor))
            );
            case OLDEST_RECORDED -> record.createdDate.gt(
                    JPAExpressions
                            .select(record.createdDate)
                            .from(record)
                            .where(record.id.eq(cursor))
            );
            case RECORD_COUNT_DESC, RECORD_COUNT_ASC -> null;
        };
    }

    private BooleanExpression havingCondition(Long cursor, SortType sortType) {
        if (cursor == null) return null;

        return switch (sortType) {
            case RECORD_COUNT_DESC -> record.count().lt(cursor);
            case RECORD_COUNT_ASC -> record.count().gt(cursor);
            case RECENT_RECORDED, OLDEST_RECORDED -> null;
        };
    }

    private OrderSpecifier<?> orderByCondition(SortType sortType) {
        return switch (sortType) {
            case RECENT_RECORDED -> record.createdDate.desc();
            case OLDEST_RECORDED -> record.createdDate.asc();
            case RECORD_COUNT_DESC -> record.count().desc();
            case RECORD_COUNT_ASC -> record.count().asc();
        };
    }

    private BooleanExpression bookRecordCursorCondition(Long cursor) {
        if (cursor == null) {
            return null;
        }
        return record.id.lt(cursor);
    }

    public List<Record> findBookRecordsByCursor(
            Long userId, Long bookId, Long cursor, Emotion emotion, int size
    ) {
        List<Long> recordIds = queryFactory
                .select(record.id)
                .from(record)
                .join(record.library, QLibrary.library)
                .where(
                        QLibrary.library.user.id.eq(userId),
                        QLibrary.library.book.id.eq(bookId),
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
                .join(record.library, QLibrary.library).fetchJoin()
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
