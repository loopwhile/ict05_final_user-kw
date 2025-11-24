package com.boot.ict05_final_user.domain.dailyClosing.dto;

import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosing;
import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosingDenom;
import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosingExpense;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 일일 마감 상세 조회 응답 DTO.
 * DailyClosing 한 건 + 권종 + 지출 목록을 한 번에 내려준다.
 *
 * 프론트의 DailyClosingDetailResponse 타입과 필드명을 맞춘다.
 */
@Getter
@Builder
public class DailyClosingDetailResponse {

    /** 마감 일자 */
    private LocalDate closingDate;

    /** 현금 방문 매출 */
    private Long cashVisitSales;

    /** 금 포장 매출 */
    private Long cashTakeoutSales;

    /** 현금 배달 매출 */
    private Long cashDeliverySales;

    /** 카드 방문 매출 */
    private Long cardVisitSales;

    /** 카드 포장 매출 */
    private Long cardTakeoutSales;

    /** 카드 배달 매출 */
    private Long cardDeliverySales;

    /** 상품권 매출 합계 */
    private Long voucherSales;

    /** 현금 지출 총액 */
    private Long totalExpense;

    /** 시재 차액 (실제 - 계산) */
    private Long differenceAmount;

    /**
     * 마감 여부
     * <ul>
     *     <li>true  - 마감 완료</li>
     *     <li>false - 아직 마감 전</li>
     * </ul>
     */
    private boolean closed;

    /** 권종별 시재 목록 */
    private List<DailyClosingDenomDto> denoms;

    /** 현금 지출 내역 목록 */
    private List<DailyClosingExpenseDto> expenses;

    /** 차액 사유 메모 */
    private String memo;

    /**
     * 일일 마감 엔티티와 관련 엔티티 목록을 기반으로 상세 응답 DTO 를 생성한다.
     *
     * @param closing         일일 마감 헤더 엔티티
     * @param denomEntities   권종별 시재 엔티티 목록
     * @param expenseEntities 지출 내역 엔티티 목록
     * @return 매핑이 완료된 {@link DailyClosingDetailResponse} 인스턴스
     */
    public static DailyClosingDetailResponse from(
            DailyClosing closing,
            List<DailyClosingDenom> denomEntities,
            List<DailyClosingExpense> expenseEntities
    ) {
        return DailyClosingDetailResponse.builder()
                .closingDate(closing.getClosingDate())

                .cashVisitSales(closing.getCashVisitSales())
                .cashTakeoutSales(closing.getCashTakeoutSales())
                .cashDeliverySales(closing.getCashDeliverySales())

                .cardVisitSales(closing.getCardVisitSales())
                .cardTakeoutSales(closing.getCardTakeoutSales())
                .cardDeliverySales(closing.getCardDeliverySales())

                .voucherSales(closing.getVoucherSales())
                .totalExpense(closing.getTotalExpense())
                .differenceAmount(closing.getDifferenceAmount())
                .closed(closing.isClosed())

                .denoms(
                        denomEntities.stream()
                                .map(DailyClosingDenomDto::from)
                                .toList()
                )
                .expenses(
                        expenseEntities.stream()
                                .map(DailyClosingExpenseDto::from)
                                .toList()
                )
                .memo(closing.getDifferenceMemo())
                .build();
    }
}

