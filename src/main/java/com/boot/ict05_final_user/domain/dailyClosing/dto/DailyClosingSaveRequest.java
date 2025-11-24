package com.boot.ict05_final_user.domain.dailyClosing.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 일일 시재 마감 저장 요청 DTO.
 *
 * 화면에서 입력한 시재 금액, 지출, 권종 정보를 서버로 전달한다.
 */
@Data
public class DailyClosingSaveRequest {

    /** 마감 대상 일자 (필수) */
    private LocalDate closingDate;

    /** 전일 이월 현금 */
    private Long startingCash;

    /** 지출 합계 */
    private Long totalExpense;

    /** 입금 예정 금액 */
    private Long depositAmount;

    /** 계산상 현금 잔액(전일이월 + 매출 - 지출 - 입금 등) */
    private Long calculatedCash;

    /** 실제 카운트한 현금 */
    private Long actualCash;

    /** 이월할 현금 */
    private Long carryoverCash;

    /** 차이 금액 */
    private Long differenceAmount;

    /** 차이 사유 메모 */
    private String differenceMemo;

    /** 지출 상세 목록 */
    private List<DailyClosingExpenseDto> expenses;

    /** 권종별 시재 목록 */
    private List<DailyClosingDenomDto> denoms;

}
