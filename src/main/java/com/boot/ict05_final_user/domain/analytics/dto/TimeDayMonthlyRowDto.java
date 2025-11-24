package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시간/요일 분석 - 월별 테이블 한 행.
 *
 * <p>1 row는 [월(yearMonth), 요일, 시간대] 조합 하나를 의미합니다.</p>
 *
 * @param yearMonth 기준 월 (YYYY-MM).
 * @param weekday 요일 (1~7, 월=1).
 * @param hour 시간대 (7~20).
 * @param orderCount 주문수(건).
 * @param sales 매출(원).
 * @param visitCount 매장 주문수.
 * @param takeoutCount 포장 주문수.
 * @param deliveryCount 배달 주문수.
 * @param visitRate 매장 주문 비율 (0~1).
 * @param takeoutRate 포장 주문 비율 (0~1).
 * @param deliveryRate 배달 주문 비율 (0~1).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record TimeDayMonthlyRowDto(
		@Schema(description = "기준 월 (YYYY-MM).", example = "2025-11")
		String yearMonth,

		@Schema(description = "요일 (1~7, 월=1).", example = "2")
		int weekday,

		@Schema(description = "시간대 (7~20).", example = "18")
		int hour,

		@Schema(description = "주문수(건).", example = "320")
		long orderCount,

		@Schema(description = "매출(원).", example = "12500000")
		long sales,

		@Schema(description = "매장 주문수.", example = "120")
		long visitCount,

		@Schema(description = "포장 주문수.", example = "100")
		long takeoutCount,

		@Schema(description = "배달 주문수.", example = "100")
		long deliveryCount,

		@Schema(description = "매장 주문 비율 (0~1).", example = "0.375")
		double visitRate,

		@Schema(description = "포장 주문 비율 (0~1).", example = "0.312")
		double takeoutRate,

		@Schema(description = "배달 주문 비율 (0~1).", example = "0.312")
		double deliveryRate
) {
}
