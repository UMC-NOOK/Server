package app.nook.focus.repository.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * app.nook.focus.repository.dto.QMonthlyFocusStatsDto is a Querydsl Projection type for MonthlyFocusStatsDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QMonthlyFocusStatsDto extends ConstructorExpression<MonthlyFocusStatsDto> {

    private static final long serialVersionUID = 1701630372L;

    public QMonthlyFocusStatsDto(com.querydsl.core.types.Expression<java.time.LocalDate> focusDate, com.querydsl.core.types.Expression<Long> bookId, com.querydsl.core.types.Expression<String> coverImageUrl, com.querydsl.core.types.Expression<Long> totalSec) {
        super(MonthlyFocusStatsDto.class, new Class<?>[]{java.time.LocalDate.class, long.class, String.class, long.class}, focusDate, bookId, coverImageUrl, totalSec);
    }

}

