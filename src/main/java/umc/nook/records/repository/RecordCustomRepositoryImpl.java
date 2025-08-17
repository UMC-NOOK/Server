package umc.nook.records.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.book.domain.QBook;
import umc.nook.bookshelves.domain.QUserBookShelf;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.records.domain.QBookRecord;
import umc.nook.records.dto.RecordDTO;
import umc.nook.users.domain.User;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        // 월별 책 등록 수
        Map<Integer, Long> totalBooksByMonth = queryFactory
                .select(ub.createdDate.month(), ub.count())
                .from(ub)
                .where(
                        ub.user.eq(user),
                        ub.createdDate.year().eq(year.getValue())
                )
                .groupBy(ub.createdDate.month())
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(0, Integer.class),
                        t -> t.get(1, Long.class)
                ));

        // 월별 기록 수
        Map<Integer, Long> recordedBooksByMonth = queryFactory
                .select(record.createdDate.month(), record.countDistinct())
                .from(record)
                .where(
                        record.bookshelf.user.eq(user),
                        record.createdDate.year().eq(year.getValue())
                )
                .groupBy(record.createdDate.month())
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(0, Integer.class),
                        t -> t.get(1, Long.class)
                ));

        // 1~12월 기록률 계산
        List<RecordDTO.MonthlyRecordRateResponseDTO.MonthRate> rates = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            long total = totalBooksByMonth.getOrDefault(month, 0L);
            long recorded = recordedBooksByMonth.getOrDefault(month, 0L);

            double rate = (total == 0) ? 0.0 : (recorded * 100.0) / total;
            rates.add(new RecordDTO.MonthlyRecordRateResponseDTO.MonthRate(month, rate));
        }

        return new RecordDTO.MonthlyRecordRateResponseDTO(rates);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<BookShelfDTO.BookThumbnail> viewRecentRecordedBook(User user) {
        QBookRecord record = QBookRecord.bookRecord;
        QBook book = QBook.book;

        return Optional.ofNullable(
                queryFactory
                        .select(
                                book.bookId,
                                book.title,
                                book.coverImageUrl
                        )
                        .from(record)
                        .join(record.bookshelf.book, book)
                        .where(record.bookshelf.user.eq(user))
                        .orderBy(record.createdDate.desc())
                        .fetchFirst()
        ).map(tuple -> new BookShelfDTO.BookThumbnail(
                tuple.get(book.bookId),
                tuple.get(book.title),
                tuple.get(book.coverImageUrl)
        ));
    }


}
