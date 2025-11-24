package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 요일별 매출/주문수 차트 포인트.
 *
 * <p>weekday: 1~7, 월=1, 화=2, …, 일=7.</p>
 *
 * @param weekday 요일(1~7, 월=1).
 * @param sales 해당 요일 총 매출(원).
 * @param orders 해당 요일 총 주문수(건).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record WeekdaySalesPointDto(
		@Schema(description = "요일 (1~7, 월=1).", example = "1")
		int weekday,

		@Schema(description = "해당 요일 총 매출(원).", example = "1250000")
		long sales,

		@Schema(description = "해당 요일 총 주문수(건).", example = "320")
		long orders
) {
}
