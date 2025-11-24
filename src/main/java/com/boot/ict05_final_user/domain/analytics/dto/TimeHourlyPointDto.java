package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시간대별 매출/주문수 차트용 포인트.
 *
 * <p>- hour: 7~20 (07:00~07:59 → 7).</p>
 *
 * @param hour 시간대 (7~20).
 * @param sales 해당 시간대 총 매출(원).
 * @param orders 해당 시간대 총 주문수(건).
 * @param visitOrders 해당 시간대 매장 주문수(건).
 * @param takeoutOrders 해당 시간대 포장 주문수(건).
 * @param deliveryOrders 해당 시간대 배달 주문수(건).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record TimeHourlyPointDto(
		@Schema(description = "시간대 (7~20).", example = "12")
		int hour,

		@Schema(description = "해당 시간대 총 매출(원).", example = "125000")
		long sales,

		@Schema(description = "해당 시간대 총 주문수(건).", example = "45")
		long orders,

		@Schema(description = "해당 시간대 매장 주문수(건).", example = "20")
		long visitOrders,

		@Schema(description = "해당 시간대 포장 주문수(건).", example = "10")
		long takeoutOrders,

		@Schema(description = "해당 시간대 배달 주문수(건).", example = "15")
		long deliveryOrders
) {
}
