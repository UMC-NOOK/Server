package app.nook.global.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class CursorResponse<T, C> {

    private final List<T> items;
    private final C nextCursor;
    private final boolean hasNext;

    public CursorResponse(
            List<T> items,
            C nextCursor,
            boolean hasNext
    ) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static <T, C> CursorResponse<T, C> of(
            List<T> items,
            C nextCursor,
            boolean hasNext
    ) {
        return new CursorResponse<>(items, nextCursor, hasNext);
    }
}
