package com.boot.ict05_final_user.domain.dailyClosing.repository;

import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosing;
import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosingDenom;
import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosingExpense;

import java.time.LocalDate;
import java.util.List;

/**
 * DailyClosing 에서 사용하는 커스텀 쿼리 인터페이스.
 */
public interface DailyClosingRepositoryCustom {

    /**
     * 주문 테이블을 기준으로 일일 매출 합계를 조회한다.
     * (현금, 카드, 상품권, 할인, 환불)
     *
     * @param storeId 가맹점 아이디
     * @param date    기준 일자
     * @return 일일 매출 합계 정보
     */
    OrderDailySummary getOrderDailySummary(Long storeId, LocalDate date);

    /**
     * 특정 일일 마감 건에 연결된 지출 목록을 조회한다.
     *
     * @param closing DailyClosing 엔티티
     * @return 지출 엔티티 목록
     */
    List<DailyClosingExpense> findExpensesByClosing(DailyClosing closing);

    /**
     * 특정 일일 마감 건에 연결된 권종별 시재 목록을 조회한다.
     *
     * @param closing DailyClosing 엔티티
     * @return 권종 엔티티 목록
     */
    List<DailyClosingDenom> findDenomsByClosing(DailyClosing closing);

    /**
     * 일일 매출 합계를 담는 값 객체.
     */
    class OrderDailySummary {

        public long cashVisit;
        public long cashTakeout;
        public long cashDelivery;

        public long cardVisit;
        public long cardTakeout;
        public long cardDelivery;

        public long voucherTotal;
        public long totalDiscount;
        public long totalRefund;

        public OrderDailySummary(long cashVisit, long cashTakeout, long cashDelivery,
                                 long cardVisit, long cardTakeout, long cardDelivery,
                                 long voucherTotal, long totalDiscount, long totalRefund) {
            this.cashVisit = cashVisit;
            this.cashTakeout = cashTakeout;
            this.cashDelivery = cashDelivery;
            this.cardVisit = cardVisit;
            this.cardTakeout = cardTakeout;
            this.cardDelivery = cardDelivery;
            this.voucherTotal = voucherTotal;
            this.totalDiscount = totalDiscount;
            this.totalRefund = totalRefund;
        }

        /**
         * 모든 값이 0인 기본 객체를 반환한다.
         */
        public static OrderDailySummary empty() {
            return new OrderDailySummary(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    List<DailyClosing> findDailyClosingHistory(Long storeId, LocalDate from, LocalDate to);
}
