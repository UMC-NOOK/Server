package app.nook.focus.repository.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * app.nook.focus.repository.dto.QFocusRangeStatsDto is a Querydsl Projection type for FocusRangeStatsDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QFocusRangeStatsDto extends ConstructorExpression<FocusRangeStatsDto> {

    private static final long serialVersionUID = 1593018462L;

    public QFocusRangeStatsDto(com.querydsl.core.types.Expression<java.time.LocalDateTime> startedAt, com.querydsl.core.types.Expression<java.time.LocalDateTime> endedAt) {
        super(FocusRangeStatsDto.class, new Class<?>[]{java.time.LocalDateTime.class, java.time.LocalDateTime.class}, startedAt, endedAt);
    }

}

