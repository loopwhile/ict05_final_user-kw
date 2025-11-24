package com.boot.ict05_final_user.domain.dailyClosing.dto;

import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosing;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 일일 시재 마감 화면 진입 시 내려주는 초기 데이터 응답 DTO.
 * 이미 마감된 날이면 저장된 값을 내려주고,
 * 아직 마감되지 않았다면 주문 집계 데이터만 채워서 내려준다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyClosingInitResponse {

    // 주문 집계 정보

    /** 현금 방문 매출 */
    private long cashVisit;
    /** 현금 포장 매출 */
    private long cashTakeout;
    /** 현금 배달 매출 */
    private long cashDelivery;

    /** 카드 방문 매출 */
    private long cardVisit;
    /** 카드 포장 매출 */
    private long cardTakeout;
    /** 카드 배달 매출 */
    private long cardDelivery;

    /** 상품권 매출 합계 */
    private long voucherTotal;
    /** 할인 금액 합계 */
    private long totalDiscount;
    /** 환불 금액 합계 */
    private long totalRefund;

    // 마감 저장 정보

    /** 시작 시재 금액 */
    private Long startingCash;
    /** 현금 지출 합계 */
    private Long totalExpense;
    /** 입금액 */
    private Long depositAmount;
    /** 이론 시재 금액 */
    private Long calculatedCash;
    /** 실제 시재 금액 */
    private Long actualCash;
    /** 입금 후 이월 시재 금액 */
    private Long carryoverCash;
    /** 차액 금액 */
    private Long differenceAmount;
    /** 차액 사유 메모 */
    private String differenceMemo;

    /** 이미 마감된 날인지 여부 */
    private boolean closed;

    /** 지출 내역 목록 */
    @Builder.Default
    private List<DailyClosingExpenseDto> expenses = new ArrayList<>();

    /** 권종별 시재 목록 */
    @Builder.Default
    private List<DailyClosingDenomDto> denoms = new ArrayList<>();

    /**
     * 마감 엔티티에서 응답 DTO 로 변환한다.
     * 주문 집계 값도 엔티티에 저장된 스냅샷을 사용한다.
     *
     * @param c DailyClosing 엔티티
     * @return 변환된 응답 DTO
     */
    public static DailyClosingInitResponse fromClosing(DailyClosing c) {
        return DailyClosingInitResponse.builder()
                .cashVisit(c.getCashVisitSales())
                .cashTakeout(c.getCashTakeoutSales())
                .cashDelivery(c.getCashDeliverySales())
                .cardVisit(c.getCardVisitSales())
                .cardTakeout(c.getCardTakeoutSales())
                .cardDelivery(c.getCardDeliverySales())
                .voucherTotal(c.getVoucherSales())
                .totalDiscount(c.getTotalDiscount())
                .totalRefund(c.getTotalRefund())
                .startingCash(c.getStartingCash())
                .totalExpense(c.getTotalExpense())
                .depositAmount(c.getDepositAmount())
                .calculatedCash(c.getCalculatedCash())
                .actualCash(c.getActualCash())
                .carryoverCash(c.getCarryoverCash())
                .differenceAmount(c.getDifferenceAmount())
                .differenceMemo(c.getDifferenceMemo())
                .closed(true)   // 마감 상태
                .build();
    }
}
