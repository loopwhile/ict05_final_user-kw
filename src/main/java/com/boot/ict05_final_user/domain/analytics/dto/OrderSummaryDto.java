package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 주문 분석 상단 카드 요약 DTO.
 *
 * <p>기간: 이번달 1일 00:00 ~ 어제 24:00 (MTD).</p>
 * <p>기준: 단일 storeId, 상태 COMPLETED, KST 기준.</p>
 *
 * @param deliverySalesMtd 배달 매출 (MTD, 원).
 * @param takeoutSalesMtd 포장 매출 (MTD, 원).
 * @param visitSalesMtd 매장 매출 (MTD, 원).
 * @param orderCountMtd 주문수 (MTD, 건).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record OrderSummaryDto(
		@Schema(description = "배달 매출 MTD(원).", example = "550000")
		long deliverySalesMtd,

		@Schema(description = "포장 매출 MTD(원).", example = "300000")
		long takeoutSalesMtd,

		@Schema(description = "매장 매출 MTD(원).", example = "450000")
		long visitSalesMtd,

		@Schema(description = "주문수 MTD(건).", example = "780")
		long orderCountMtd
) {
}
