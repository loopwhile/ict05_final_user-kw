package com.boot.ict05_final_user.domain.analytics.dto;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * KPI PDF 생성을 위해 Python 서버로 전달하는 페이로드입니다.
 *
 * <p>FastAPI 측 KpiPayload 구조(criteria + data 목록)에 맞춰 전송합니다.</p>
 *
 * @param criteria PDF 생성에 사용되는 조회 조건 및 메타 정보.
 * @param data PDF에 포함할 행 데이터 목록 (Map 형태).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record KpiPdfPayload(
		@Schema(description = "PDF 생성용 기준(criteria) 맵.", example = "{\"start\":\"2025-11-01\",\"end\":\"2025-11-17\"}")
		Map<String, Object> criteria,

		@Schema(description = "PDF에 포함될 데이터 리스트. 각 요소는 컬럼명-값 맵 형태입니다.")
		List<Map<String, Object>> data
) { }
