package umc.nook.records.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * umc.nook.records.dto.QRecordDTO_MonthlyRecordRateResponseDTO_MonthRate is a Querydsl Projection type for MonthRate
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QRecordDTO_MonthlyRecordRateResponseDTO_MonthRate extends ConstructorExpression<RecordDTO.MonthlyRecordRateResponseDTO.MonthRate> {

    private static final long serialVersionUID = 988269691L;

    public QRecordDTO_MonthlyRecordRateResponseDTO_MonthRate(com.querydsl.core.types.Expression<Integer> month, com.querydsl.core.types.Expression<Long> total, com.querydsl.core.types.Expression<Long> recorded) {
        super(RecordDTO.MonthlyRecordRateResponseDTO.MonthRate.class, new Class<?>[]{int.class, long.class, long.class}, month, total, recorded);
    }

}

