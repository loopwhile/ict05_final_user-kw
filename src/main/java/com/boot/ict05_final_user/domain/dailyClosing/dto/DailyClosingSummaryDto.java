package com.boot.ict05_final_user.domain.dailyClosing.dto;

import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 일일 마감 내역 리스트 화면에서 사용하는 요약 DTO.
 *
 * DailyClosing 엔티티에서 날짜별 핵심 합계 정보만 추려서 전달한다.
 * 프론트 DailyClosingList 테이블의 한 행(row)에 대응한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyClosingSummaryDto {

    /** 일일 마감 PK */
    private Long id;

    /** 마감 기준 일자 */
    private LocalDate closingDate;

    /** 현금 방문 매출 합계 */
    private long cashVisitSales;

    /** 현금 포장 매출 합계 */
    private long cashTakeoutSales;

    /** 현금 배달 매출 합계 */
    private long cashDeliverySales;

    /** 카드 방문 매출 합계 */
    private long cardVisitSales;

    /** 카드 포장 매출 합계 */
    private long cardTakeoutSales;

    /** 카드 배달 매출 합계 */
    private long cardDeliverySales;

    /** 상품권 매출 합계 */
    private long voucherSales;

    /** 현금 지출 총합 */
    private long totalExpense;

    /** 시재 차액 (실제 시재 - 이론상 시재) */
    private long differenceAmount;

    /** 마감 완료 여부 */
    private boolean closed;

    /**
     * DailyClosing 엔티티에서 요약 정보를 추출해 DailyClosingSummaryDto 로 변환한다.
     *
     * 엔티티의 Long 필드가 null 인 경우 0 으로 치환해서 전달한다.
     *
     * @param entity 변환할 일일 마감 엔티티
     * @return 일일 마감 요약 DTO
     */
    public static DailyClosingSummaryDto from(DailyClosing entity) {
        if (entity == null) {
            return null;
        }

        return DailyClosingSummaryDto.builder()
                .id(entity.getId())
                .closingDate(entity.getClosingDate())
                .cashVisitSales(nvl(entity.getCashVisitSales()))
                .cashTakeoutSales(nvl(entity.getCashTakeoutSales()))
                .cashDeliverySales(nvl(entity.getCashDeliverySales()))
                .cardVisitSales(nvl(entity.getCardVisitSales()))
                .cardTakeoutSales(nvl(entity.getCardTakeoutSales()))
                .cardDeliverySales(nvl(entity.getCardDeliverySales()))
                .voucherSales(nvl(entity.getVoucherSales()))
                .totalExpense(nvl(entity.getTotalExpense()))
                .differenceAmount(nvl(entity.getDifferenceAmount()))
                .closed(entity.isClosed())
                .build();
    }

    /**
     * Long 값을 primitive long 으로 변환한다.
     * null 이면 0 을 반환한다.
     *
     * @param value 변환할 Long 값
     * @return null 이 아닌 long 값
     */
    private static long nvl(Long value) {
        return value != null ? value : 0L;
    }
}
