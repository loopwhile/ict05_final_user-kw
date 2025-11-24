package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 재료 분석 - 월별 테이블 한 행 DTO.
 *
 * <p>1 row = [월, 재료] 조합 하나를 표현합니다.</p>
 *
 * <ul>
 *   <li>yearMonth: 기준 월 (YYYY-MM).</li>
 *   <li>materialName: 재료명.</li>
 *   <li>usedQuantity: 해당 월 동안의 소모량 합계.</li>
 *   <li>cost: 해당 월 동안의 원가 합계(원).</li>
 *   <li>costRate: 해당 재료 원가 ÷ 전체 매출 × 100 (%, 소수점 1자리).</li>
 *   <li>lastInboundMonth: 최근 입고가 있었던 월 (YYYY-MM, 없으면 null).</li>
 * </ul>
 *
 * @param yearMonth 기준 월 (YYYY-MM).
 * @param materialName 재료명.
 * @param usedQuantity 사용량 합계.
 * @param cost 원가 합계(원).
 * @param costRate 원가율(%).
 * @param lastInboundMonth 최근 입고월 (YYYY-MM) 또는 null.
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MaterialMonthlyRowDto(
		@Schema(description = "기준 월 (YYYY-MM).", example = "2025-11")
		String yearMonth,

		@Schema(description = "재료명.", example = "닭가슴살")
		String materialName,

		@Schema(description = "사용량 합계 (기준 단위).", example = "250.0")
		double usedQuantity,

		@Schema(description = "원가 합계(원).", example = "900000")
		long cost,

		@Schema(description = "원가율(%), 0~100, 소수점 1자리.", example = "3.6")
		double costRate,

		@Schema(description = "최근 입고월 (YYYY-MM) 또는 null.", example = "2025-10")
		String lastInboundMonth
) {
}
