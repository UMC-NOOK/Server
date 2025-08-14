package umc.nook.bookshelves.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "서재 정렬 기준")
public enum SortType {
    @Schema(description = "최근 기록순") RECENT,
    @Schema(description = "최근 등록순") LATEST,
    @Schema(description = "제목순") TITLE,
    @Schema(description = "별점순") RATING
}