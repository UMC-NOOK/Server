package app.nook.library.event;

import java.time.YearMonth;
import java.util.List;

/**
 * 캐시 무효화 이벤트를 처리하는 레코드
 */
public record LibraryCacheInvalidateEvent(
        Long userId,
        List<YearMonth> affectedYearMonths,
        boolean evictStatusFirstPage
) {
    // 상태 캐시 무효화
    public static LibraryCacheInvalidateEvent statusOnly(Long userId) {
        return new LibraryCacheInvalidateEvent(userId, List.of(), true);
    }

    public static LibraryCacheInvalidateEvent statusAndMonthly(Long userId, List<YearMonth> affectedYearMonths) {
        return new LibraryCacheInvalidateEvent(userId, affectedYearMonths, true);
    }
}
