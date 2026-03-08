package app.nook.focus.repository.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * app.nook.focus.repository.dto.QFocusTimeStatsDto is a Querydsl Projection type for FocusTimeStatsDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QFocusTimeStatsDto extends ConstructorExpression<FocusTimeStatsDto> {

    private static final long serialVersionUID = 297916236L;

    public QFocusTimeStatsDto(com.querydsl.core.types.Expression<java.time.LocalDate> focusDate, com.querydsl.core.types.Expression<Long> totalSec) {
        super(FocusTimeStatsDto.class, new Class<?>[]{java.time.LocalDate.class, long.class}, focusDate, totalSec);
    }

}

