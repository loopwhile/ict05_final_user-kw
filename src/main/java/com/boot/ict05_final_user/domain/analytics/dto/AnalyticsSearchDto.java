package com.boot.ict05_final_user.domain.analytics.dto;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 분석 조회 조건 DTO.
 *
 * <p>API에서 조회 기간, 집계 단위, 페이지 크기 및 커서를 전달할 때 사용합니다.</p>
 *
 * @param startDate 조회 시작일 (inclusive).
 * @param endDate 조회 종료일 (inclusive, YYYY-MM-DD 그대로).
 * @param viewBy 조회 단위 (DAY 또는 MONTH).
 * @param size 페이지 크기 (예: 50).
 * @param cursor 커서 (null 또는 "YYYY-MM-DD" / "YYYY-MM").
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record AnalyticsSearchDto(
		@Schema(description = "조회 시작일 (inclusive).", type = "string", format = "date", example = "2025-11-01")
		LocalDate startDate,

		@Schema(description = "조회 종료일 (inclusive).", type = "string", format = "date", example = "2025-11-17")
		LocalDate endDate,

		@Schema(description = "조회 단위. DAY 또는 MONTH.", example = "DAY")
		ViewBy viewBy,

		@Schema(description = "페이지 크기. (50/100/150/200/300)", example = "50")
		Integer size,

		@Schema(description = "커서. DAY 단위이면 YYYY-MM-DD, MONTH 단위이면 YYYY-MM. 첫 페이지는 null.", example = "2025-11-01")
		String cursor
) {
	/**
	 * 조회 단위.
	 */
	public enum ViewBy { DAY, MONTH }
}
