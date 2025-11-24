package com.boot.ict05_final_user.domain.dailyClosing.dto;

import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosingExpense;
import lombok.*;

/**
 * 일일 마감 화면에서 사용하는 현금 지출 내역 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyClosingExpenseDto {

    /** 지출 내역 아이디 */
    private Long id;

    /** 지출 설명 */
    private String description;

    /** 지출 금액 */
    private Long amount;

    /**
     * 엔티티에서 DTO 로 변환한다.
     *
     * @param e DailyClosingExpense 엔티티
     * @return 변환된 DTO
     */
    public static DailyClosingExpenseDto from(DailyClosingExpense e) {
        return DailyClosingExpenseDto.builder()
                .id(e.getId())
                .description(e.getDescription())
                .amount(e.getAmount())
                .build();
    }

}
