package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 주문 분석 월별 테이블 한 행 (월 단위 집계).
 *
 * <p>월 단위 집계 결과를 한 행으로 표현합니다.</p>
 *
 * @param yearMonth 기준 월 (YYYY-MM).
 * @param totalSales 총매출(원).
 * @param orderCount 주문수(건).
 * @param avgOrderAmount 평균주문금액(원).
 * @param deliverySales 배달 매출(원).
 * @param takeoutSales 포장 매출(원).
 * @param visitSales 매장 매출(원).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record OrderMonthlyRowDto(
		@Schema(description = "기준 월 (YYYY-MM).", example = "2025-11")
		String yearMonth,

		@Schema(description = "총매출(원).", example = "12500000")
		long totalSales,

		@Schema(description = "주문수(건).", example = "4200")
		long orderCount,

		@Schema(description = "평균주문금액 = totalSales / orderCount (원).", example = "2976")
		long avgOrderAmount,

		@Schema(description = "배달 매출(원).", example = "5500000")
		long deliverySales,

		@Schema(description = "포장 매출(원).", example = "3000000")
		long takeoutSales,

		@Schema(description = "매장 매출(원).", example = "4000000")
		long visitSales
) {
}
