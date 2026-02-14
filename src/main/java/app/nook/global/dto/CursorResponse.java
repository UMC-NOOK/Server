package app.nook.global.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class CursorResponse<T> {

    private final List<T> items;
    private final Long nextCursor;
    private final boolean hasNext;

    public CursorResponse(
            List<T> items,
            Long nextCursor,
            boolean hasNext
    ) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static <T> CursorResponse<T> of(
            List<T> items,
            Long nextCursor,
            boolean hasNext
    ) {
        return new CursorResponse<>(items, nextCursor, hasNext);
    }
}