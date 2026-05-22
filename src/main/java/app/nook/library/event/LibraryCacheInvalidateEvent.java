package app.nook.library.event;

import java.time.YearMonth;
import java.util.Set;

/**
 * 캐시 무효화 이벤트를 처리하는 레코드
 */
public record LibraryCacheInvalidateEvent(
        Long userId,
        Set<YearMonth> affectedYearMonths
) {
    public static LibraryCacheInvalidateEvent monthly(Long userId, Set<YearMonth> affectedYearMonths) {
        return new LibraryCacheInvalidateEvent(userId, Set.copyOf(affectedYearMonths));
    }
}
