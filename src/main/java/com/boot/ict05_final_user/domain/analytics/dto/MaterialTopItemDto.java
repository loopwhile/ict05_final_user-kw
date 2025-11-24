package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 재료 Top 랭킹 카드에서 사용되는 단일 재료 정보 DTO.
 *
 * <p>사용량 기준 / 원가 기준 Top 리스트에서 공통으로 사용합니다.</p>
 *
 * @param materialId 재료 ID.
 * @param materialName 재료명.
 * @param unitName 단위명 (예: g, kg, ml, 개 등).
 * @param usedQuantity 기간 내 소모량 합계 (기준 단위).
 * @param cost 기간 내 재료 원가 합계(원).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MaterialTopItemDto(
		@Schema(description = "재료 ID.", example = "201")
		Long materialId,

		@Schema(description = "재료명.", example = "닭가슴살")
		String materialName,

		@Schema(description = "단위명 (예: g, kg, ml, 개 등).", example = "kg")
		String unitName,

		@Schema(description = "기간 내 소모량 합계 (기준 단위).", example = "250.0")
		double usedQuantity,

		@Schema(description = "기간 내 재료 원가 합계(원).", example = "900000")
		long cost
) {
}
