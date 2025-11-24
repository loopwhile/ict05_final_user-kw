package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 메뉴 분석 - 일별 테이블 한 행 DTO.
 *
 * <p>특정 일(YYYY-MM-DD) 기준으로 메뉴/카테고리별 판매 지표를 집계한 결과를 담습니다.</p>
 *
 * @param orderDate 날짜 (YYYY-MM-DD).
 * @param categoryName 메뉴 카테고리명.
 * @param menuName 메뉴명.
 * @param quantity 판매수량 합계.
 * @param sales 매출액 합계(원).
 * @param orderCount 해당 메뉴를 포함한 주문 건수.
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MenuDailyRowDto(
		@Schema(description = "날짜 (YYYY-MM-DD).", example = "2025-11-01")
		String orderDate,

		@Schema(description = "메뉴 카테고리명.", example = "버거")
		String categoryName,

		@Schema(description = "메뉴명.", example = "치즈버거세트")
		String menuName,

		@Schema(description = "판매수량 합계.", example = "120")
		long quantity,

		@Schema(description = "매출액 합계(원).", example = "3600000")
		long sales,

		@Schema(description = "주문수 (해당 메뉴를 포함한 주문 건수).", example = "110")
		long orderCount
) {
}
