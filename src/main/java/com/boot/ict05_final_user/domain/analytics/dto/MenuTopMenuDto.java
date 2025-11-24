package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 메뉴 분석 - 판매수량 TOP3 메뉴 정보를 담는 DTO.
 *
 * <p>기간 내 가장 많이 팔린 메뉴 3개(수량 기준)를 반환할 때 사용합니다.</p>
 *
 * @param menuId 메뉴 ID.
 * @param menuName 메뉴명.
 * @param quantity 판매 수량 합계.
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MenuTopMenuDto(
		@Schema(description = "메뉴 ID", example = "101")
		Long menuId,

		@Schema(description = "메뉴명", example = "치즈버거세트")
		String menuName,

		@Schema(description = "판매 수량 합계.", example = "480")
		long quantity
) {
}
