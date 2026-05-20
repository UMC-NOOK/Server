package app.nook.library.event;

/**
 * 캐시 무효화 이벤트를 처리하는 레코드
 */
public record LibraryCacheInvalidateEvent(
        Long userId
) {
    public static LibraryCacheInvalidateEvent monthly(Long userId) {
        return new LibraryCacheInvalidateEvent(userId);
    }
}
