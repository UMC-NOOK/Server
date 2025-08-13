package umc.nook.records.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.bookshelves.domain.QUserBookShelf;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.records.dto.RecordDTO;
import umc.nook.users.domain.User;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RecordCustomRepositoryImpl implements RecordCustomRepository{

    private final JPAQueryFactory queryFactory;

    @Transactional
    public RecordDTO.MonthlyRecordRateResponseDTO viewRecordRate(User user, Year year) {
        QUserBookShelf ub = QUserBookShelf.userBookShelf;

        Map<Integer, List<UserBookShelf>> booksByMonth = queryFactory
                .selectFrom(ub)
                .where(
                        ub.user.eq(user),
                        ub.createdDate.year().eq(year.getValue())
                )
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(b -> b.getCreatedDate().getMonthValue()));

        List<RecordDTO.MonthlyRecordRateResponseDTO.MonthRate> rates = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            List<UserBookShelf> booksInMonth = booksByMonth.getOrDefault(month, List.of());

            if (booksInMonth.isEmpty()) {
                rates.add(new RecordDTO.MonthlyRecordRateResponseDTO.MonthRate(month, 0.0));
                continue;
            }

            long total = booksInMonth.size();
            long recorded = booksInMonth.stream()
                    .filter(b -> b.getRecordedAt() != null)
                    .count();

            double rate = (recorded * 100.0) / total;
            rates.add(new RecordDTO.MonthlyRecordRateResponseDTO.MonthRate(month, rate));
        }

        return new RecordDTO.MonthlyRecordRateResponseDTO(rates);
    }

}
