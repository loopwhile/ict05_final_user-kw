package com.boot.ict05_final_user.domain.dailyClosing.dto;

import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosingDenom;
import lombok.*;

/**
 * 일일 마감 화면에서 사용하는 권종별 시재 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyClosingDenomDto {

    /** 권종 금액 예: 50000, 10000, 5000, 1000, 500, 100, 50, 10 */
    private Integer denomValue;

    /** 개수 */
    private Integer count;

    /** 금액 합계 */
    private Long amount;

    /**
     * 엔티티에서 DTO 로 변환한다.
     *
     * @param d DailyClosingDenom 엔티티
     * @return 변환된 DTO
     */
    public static DailyClosingDenomDto from(DailyClosingDenom d) {
        return DailyClosingDenomDto.builder()
                .denomValue(d.getDenomValue())
                .count(d.getCount())
                .amount(d.getAmount())
                .build();
    }

}
