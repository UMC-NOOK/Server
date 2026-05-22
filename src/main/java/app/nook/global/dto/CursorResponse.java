package app.nook.global.dto;

import java.util.List;

public record CursorResponse<T, C>(
        List<T> items,
        C nextCursor,
        boolean hasNext
) {

    public static <T, C> CursorResponse<T, C> of(
            List<T> items,
            C nextCursor,
            boolean hasNext
    ) {
        return new CursorResponse<>(items, nextCursor, hasNext);
    }
}
