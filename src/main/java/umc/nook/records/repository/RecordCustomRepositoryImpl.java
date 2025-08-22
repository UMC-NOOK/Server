package umc.nook.records.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.book.domain.QBook;
import umc.nook.bookshelves.domain.QUserBookShelf;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.records.domain.QBookRecord;
import umc.nook.records.dto.QRecordDTO_MonthlyRecordRateResponseDTO_MonthRate;
import umc.nook.records.dto.RecordDTO;
import umc.nook.users.domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RecordCustomRepositoryImpl implements RecordCustomRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    @Transactional(readOnly = true)
    public RecordDTO.MonthlyRecordRateResponseDTO viewRecordRate(User user, Year year) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;
        QBookRecord record = QBookRecord.bookRecord;

        LocalDateTime start = LocalDate.of(year.getValue(), 1, 1).atStartOfDay();
        LocalDateTime end   = LocalDate.of(year.getValue() + 1, 1, 1).atStartOfDay();

        BooleanExpression sameMonthJoin =
                Expressions.booleanTemplate(
                        "MONTH({0}) = MONTH({1}) AND YEAR({0}) = YEAR({1})",
                        record.createdDate, ub.createdDate
                );


        List<RecordDTO.MonthlyRecordRateResponseDTO.MonthRate> rows = queryFactory
                .select(new QRecordDTO_MonthlyRecordRateResponseDTO_MonthRate(
                        ub.createdDate.month(),
                        ub.book.bookId.countDistinct(),
                        record.bookshelf.book.bookId.countDistinct()
                ))
                .from(ub)
                .leftJoin(record).on(record.bookshelf.eq(ub).and(sameMonthJoin))
                .where(
                        ub.user.eq(user),
                        ub.createdDate.goe(start),
                        ub.createdDate.lt(end)
                )
                .groupBy(ub.createdDate.month())
                .orderBy(ub.createdDate.month().asc())
                .fetch();

        // 월→DTO 맵으로 변환 (중복 탐색 방지)
        Map<Integer, RecordDTO.MonthlyRecordRateResponseDTO.MonthRate> byMonth = rows.stream()
                .collect(Collectors.toMap(
                        RecordDTO.MonthlyRecordRateResponseDTO.MonthRate::getMonth,
                        r -> r
                ));

        // 1~12월 누락분 0으로 채우기 (QueryProjection 생성자 이용: total/recorded=0 -> rate=0)
        List<RecordDTO.MonthlyRecordRateResponseDTO.MonthRate> filled = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            filled.add(byMonth.getOrDefault(
                    m,
                    new RecordDTO.MonthlyRecordRateResponseDTO.MonthRate(m, 0L, 0L)
            ));
        }

        return new RecordDTO.MonthlyRecordRateResponseDTO(filled);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<BookShelfDTO.RecentBookDTO> viewRecentRecordedBook(User user) {
        QBookRecord record = QBookRecord.bookRecord;
        QBook book = QBook.book;

        return Optional.ofNullable(
                queryFactory
                        .select(
                                book.bookId,
                                book.title,
                                book.author
                        )
                        .from(record)
                        .join(record.bookshelf.book, book)
                        .where(record.bookshelf.user.eq(user))
                        .orderBy(record.createdDate.desc())
                        .fetchFirst()
        ).map(tuple -> new BookShelfDTO.RecentBookDTO(
                tuple.get(book.bookId),
                tuple.get(book.title),
                tuple.get(book.author)
        ));
    }


}
