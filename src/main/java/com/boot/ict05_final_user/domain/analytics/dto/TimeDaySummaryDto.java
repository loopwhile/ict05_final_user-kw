package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시간/요일 분석 상단 요약 카드 DTO.
 *
 * <p>- 분석 대상: 단일 매장(storeId).<br>
 * - 기간: [startDate, endDate] (영업시간 07~20시만 집계).<br>
 * - peakHour / offpeakHour / topWeekday가 null이면 해당 구간에 데이터 없음.</p>
 *
 * @param peakHour 피크 시간대 (7~20), null 가능.
 * @param peakHourSales 피크 시간대 매출(원).
 * @param offpeakHour 비수 시간대 (7~20, 매출>0 중 최소), null 가능.
 * @param offpeakHourSales 비수 시간대 매출(원).
 * @param topWeekday 최고 매출 요일 (1~7, 월=1), null 가능.
 * @param topWeekdaySales 최고 매출 요일 매출(원).
 * @param weekdaySales 주중(월~금) 매출 합(원).
 * @param weekendSales 주말(토~일) 매출 합(원).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record TimeDaySummaryDto(
		@Schema(description = "피크 시간대 (7~20). Null 가능.", example = "12")
		Integer peakHour,

		@Schema(description = "피크 시간대 매출(원).", example = "450000")
		long peakHourSales,

		@Schema(description = "비수 시간대 (7~20). Null 가능.", example = "8")
		Integer offpeakHour,

		@Schema(description = "비수 시간대 매출(원).", example = "120000")
		long offpeakHourSales,

		@Schema(description = "최고 매출 요일 (1~7, 월=1). Null 가능.", example = "5")
		Integer topWeekday,

		@Schema(description = "최고 매출 요일 매출(원).", example = "2300000")
		long topWeekdaySales,

		@Schema(description = "주중(월~금) 매출 합(원).", example = "7200000")
		long weekdaySales,

		@Schema(description = "주말(토~일) 매출 합(원).", example = "3200000")
		long weekendSales
) {
}
