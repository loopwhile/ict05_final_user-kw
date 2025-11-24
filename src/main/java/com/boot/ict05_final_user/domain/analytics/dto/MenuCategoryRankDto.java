package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 메뉴 분석 - 카테고리 매출 TOP3 정보를 담는 DTO.
 *
 * <p>기간 내 카테고리별 매출 합계를 기준으로 상위 3개 카테고리를 반환할 때 사용합니다.</p>
 *
 * @param categoryId 카테고리 ID.
 * @param categoryName 카테고리명.
 * @param sales 기간 내 카테고리 매출 합계(원).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MenuCategoryRankDto(
		@Schema(description = "카테고리 ID", example = "10")
		Long categoryId,

		@Schema(description = "카테고리명", example = "세트메뉴")
		String categoryName,

		@Schema(description = "기간 내 카테고리 매출 합계(원).", example = "1250000")
		long sales
) {
}
