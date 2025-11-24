package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 메뉴별 매출 및 매출 기여도 정보를 표현하는 DTO.
 *
 * <p>기간 내 각 메뉴의 매출과 전체 메뉴 매출 대비 기여도를 표현합니다.</p>
 *
 * @param menuId 메뉴 ID.
 * @param menuName 메뉴명.
 * @param sales 기간 내 메뉴 매출 합계(원).
 * @param salesShare 전체 메뉴 매출 대비 비율(0~100, 소수점 1자리).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MenuSalesContributionDto(
		@Schema(description = "메뉴 ID", example = "101")
		Long menuId,

		@Schema(description = "메뉴명", example = "치즈버거세트")
		String menuName,

		@Schema(description = "기간 내 메뉴 매출 합계(원).", example = "9600000")
		long sales,

		@Schema(description = "전체 메뉴 매출 대비 비율 (0~100, 소수점 1자리).", example = "12.5")
		double salesShare
) {}
