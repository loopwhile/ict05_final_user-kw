package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 매출이 낮은(저성과) 메뉴 정보를 표현하는 DTO.
 *
 * <p>기간 내 판매량 및 매출이 낮은 메뉴를 표시할 때 사용합니다.</p>
 *
 * @param menuId 메뉴 ID.
 * @param menuName 메뉴명.
 * @param quantity 기간 내 판매 수량 합계.
 * @param sales 기간 내 매출 합계(원).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MenuLowPerformanceDto(
		@Schema(description = "메뉴 ID", example = "101")
		Long menuId,

		@Schema(description = "메뉴명", example = "구운감자")
		String menuName,

		@Schema(description = "기간 내 판매 수량 합계.", example = "3")
		long quantity,

		@Schema(description = "기간 내 매출 합계(원).", example = "9000")
		long sales
) {}
