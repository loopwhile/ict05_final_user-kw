package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 재료 분석 - 일별 테이블 한 행 DTO.
 *
 * <p>1 row = [날짜, 재료] 조합 하나를 표현합니다.</p>
 *
 * <ul>
 *   <li>useDate: 사용 기준 일자 (YYYY-MM-DD).</li>
 *   <li>materialName: 재료명.</li>
 *   <li>usedQuantity: 해당 일자에 소모된 재료 수량 (기준 단위).</li>
 *   <li>unitName: 단위 (g, kg, ml, 개 등).</li>
 *   <li>cost: 해당 일자 재료 원가 합계(원).</li>
 *   <li>salesShare: 해당 재료 원가가 그날 매출에서 차지하는 비중(%), 0~100, 소수점 1자리.</li>
 *   <li>lastInboundDate: 최근 입고일 (YYYY-MM-DD, 없으면 null).</li>
 * </ul>
 *
 * @param useDate 사용 기준 일자 (YYYY-MM-DD).
 * @param materialName 재료명.
 * @param usedQuantity 사용량 (기준 단위).
 * @param unitName 단위명.
 * @param cost 원가 합계(원).
 * @param salesShare 매출 대비 원가 비중(%).
 * @param lastInboundDate 최근 입고일 (YYYY-MM-DD) 또는 null.
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MaterialDailyRowDto(
		@Schema(description = "사용 기준 일자 (YYYY-MM-DD).", example = "2025-11-05")
		String useDate,

		@Schema(description = "재료명.", example = "닭가슴살")
		String materialName,

		@Schema(description = "사용량 (기준 단위).", example = "12.5")
		double usedQuantity,

		@Schema(description = "단위명 (g, kg, ml, 개 등).", example = "kg")
		String unitName,

		@Schema(description = "원가 합계(원).", example = "45000")
		long cost,

		@Schema(description = "매출 대비 원가 비중(%), 0~100, 소수점 1자리.", example = "2.5")
		double salesShare,

		@Schema(description = "최근 입고일 (YYYY-MM-DD) 또는 null.", example = "2025-10-28")
		String lastInboundDate
) {
}
