package app.nook.library.repository;


import app.nook.library.domain.Library;
import app.nook.library.domain.QLibrary;
import app.nook.user.domain.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RequiredArgsConstructor
public class LibraryRepositoryImpl implements LibraryRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    // TODO : focus시간 기반으로 수정
    @Override
    public List<Library> findByUserAndYearMonth(User user, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        QLibrary library = QLibrary.library;

        return queryFactory
                .selectFrom(library)
                .where(
                        library.user.eq(user),
                        library.createdDate.between(
                                startDate.atStartOfDay(),
                                endDate.atTime(23, 59, 59)
                        )
                )
                .orderBy(library.createdDate.desc())
                .fetch();
    }

}
