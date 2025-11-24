package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 메뉴 분석 - 월별 테이블 한 행 DTO.
 *
 * <p>특정 월(YYYY-MM) 기준으로 메뉴/카테고리별 판매 지표와 집계 정보를 담습니다.</p>
 *
 * @param yearMonth 기준 월 (YYYY-MM).
 * @param menuName 메뉴명.
 * @param categoryName 카테고리명.
 * @param quantity 판매수량 합계.
 * @param sales 매출액 합계(원).
 * @param orderCount 해당 메뉴를 포함한 주문 건수.
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MenuMonthlyRowDto(
		@Schema(description = "기준 월 (YYYY-MM).", example = "2025-11")
		String yearMonth,

		@Schema(description = "메뉴명.", example = "치즈버거세트")
		String menuName,

		@Schema(description = "카테고리명.", example = "버거")
		String categoryName,

		@Schema(description = "판매수량 합계.", example = "320")
		long quantity,

		@Schema(description = "매출액 합계(원).", example = "9600000")
		long sales,

		@Schema(description = "주문수 (해당 메뉴 포함 건수).", example = "300")
		long orderCount
) {
}
