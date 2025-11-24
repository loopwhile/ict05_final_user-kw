package com.boot.ict05_final_user.domain.analytics.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Python FastAPI PDF 서비스에 전달할 시간/요일 분석 보고서 페이로드.
 *
 * <p>Analytics DTO들을 포함하여 JSON으로 직렬화하여 전송합니다.</p>
 *
 * @param storeId 매장 ID.
 * @param storeName 매장명.
 * @param periodLabel 기간 라벨 예: "2025-11-01 ~ 2025-11-17".
 * @param summary 상단 요약 카드 정보.
 * @param hourlyPoints 시간대별 차트 포인트 리스트.
 * @param weekdayPoints 요일별 차트 포인트 리스트.
 * @param viewBy "DAY" 또는 "MONTH".
 * @param dailyRows 일별 테이블 데이터.
 * @param monthlyRows 월별 테이블 데이터.
 * @param generatedAt 보고서 생성 시각 (KST 기준 문자열).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record TimeDayReportPayload(
		@Schema(description = "매장 ID.", example = "10")
		Long storeId,

		@Schema(description = "매장명.", example = "토스트랩 강남점")
		String storeName,

		@Schema(description = "기간 라벨.", example = "2025-11-01 ~ 2025-11-17")
		String periodLabel,

		@Schema(description = "상단 요약 카드 데이터.")
		TimeDaySummaryDto summary,

		@Schema(description = "시간대별 차트 포인트 목록.")
		List<TimeHourlyPointDto> hourlyPoints,

		@Schema(description = "요일별 차트 포인트 목록.")
		List<WeekdaySalesPointDto> weekdayPoints,

		@Schema(description = "조회 단위 (\"DAY\" or \"MONTH\").", example = "DAY")
		String viewBy,

		@Schema(description = "일별 테이블 데이터.")
		List<TimeDayDailyRowDto> dailyRows,

		@Schema(description = "월별 테이블 데이터.")
		List<TimeDayMonthlyRowDto> monthlyRows,

		@Schema(description = "생성 시각 (KST 기준 문자열).", example = "2025-11-17T14:30:00+09:00")
		String generatedAt
) { }
