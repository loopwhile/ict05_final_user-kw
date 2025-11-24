package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시간/요일 분석 - 일별 테이블 한 행.
 *
 * <p>1 row는 [날짜, 요일, 시간대] 조합 하나를 의미합니다.</p>
 *
 * @param orderDate 날짜 (YYYY-MM-DD).
 * @param weekday 요일 (1~7, 월=1).
 * @param hour 시간대 (7~20, HH).
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
public record TimeDayDailyRowDto(
		@Schema(description = "날짜 (YYYY-MM-DD).", example = "2025-11-05")
		String orderDate,

		@Schema(description = "요일 (1~7, 월=1).", example = "3")
		int weekday,

		@Schema(description = "시간대 (7~20).", example = "12")
		int hour,

		@Schema(description = "주문수(건).", example = "45")
		long orderCount,

		@Schema(description = "매출(원).", example = "1250000")
		long sales,

		@Schema(description = "매장 주문수.", example = "20")
		long visitCount,

		@Schema(description = "포장 주문수.", example = "10")
		long takeoutCount,

		@Schema(description = "배달 주문수.", example = "15")
		long deliveryCount,

		@Schema(description = "매장 주문 비율 (0~1).", example = "0.444")
		double visitRate,

		@Schema(description = "포장 주문 비율 (0~1).", example = "0.222")
		double takeoutRate,

		@Schema(description = "배달 주문 비율 (0~1).", example = "0.333")
		double deliveryRate
) {
}
