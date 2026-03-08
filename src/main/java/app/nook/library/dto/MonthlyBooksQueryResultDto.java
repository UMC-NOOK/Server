package app.nook.library.dto;

import app.nook.redis.dto.MonthlyBookCacheRow;

import java.util.List;

public record MonthlyBooksQueryResultDto(
        int totalBookCount,
        FocusRankDto.MonthlyBooksResponseDto response,
        List<MonthlyBookCacheRow> cacheRows
) {
}
