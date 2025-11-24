package com.boot.ict05_final_user.domain.analytics.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 재료 분석 PDF 페이로드.
 *
 * <p>상단 요약 카드 + 일/월별 테이블 데이터를 포함하여 Python FastAPI PDF 서비스에 전달하는 구조입니다.</p>
 *
 * @param storeId 점포 ID.
 * @param storeName 점포명 (리포트 상단 타이틀).
 * @param periodLabel 기간 라벨 예: "2025-11-01 ~ 2025-11-17".
 * @param summary 상단 요약 카드 데이터.
 * @param viewBy "DAY" 또는 "MONTH".
 * @param dailyRows 일별 테이블 데이터.
 * @param monthlyRows 월별 테이블 데이터.
 * @param generatedAt 리포트 생성 시각 (KST 기준 ISO-8601 문자열).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MaterialReportPayload(

		@Schema(description = "점포 ID.", example = "10")
		Long storeId,

		@Schema(description = "점포명.", example = "토스트랩 강남점")
		String storeName,

		@Schema(description = "기간 라벨.", example = "2025-11-01 ~ 2025-11-17")
		String periodLabel,

		@Schema(description = "상단 요약 카드 데이터.")
		MaterialSummaryDto summary,

		@Schema(description = "조회 단위 (\"DAY\" 또는 \"MONTH\").", example = "DAY")
		String viewBy,

		@Schema(description = "일별 테이블 데이터.")
		List<MaterialDailyRowDto> dailyRows,

		@Schema(description = "월별 테이블 데이터.")
		List<MaterialMonthlyRowDto> monthlyRows,

		@Schema(description = "리포트 생성 시각 (KST 기준 ISO-8601 문자열).", example = "2025-11-17T14:30:00+09:00")
		String generatedAt
) {
}
