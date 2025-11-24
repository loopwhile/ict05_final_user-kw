package com.boot.ict05_final_user.domain.analytics.dto;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 주문 분석 PDF 페이로드.
 *
 * <p>FastAPI OrdersPayload(criteria + data[OrdersRow]) 와 호환되는 구조로 Python PDF 서비스에 전송합니다.</p>
 *
 * @param criteria PDF 생성에 필요한 조회 조건 및 메타 정보.
 * @param data PDF에 포함할 행 데이터 리스트 (각 요소는 컬럼명-값 맵).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record OrdersPdfPayload(
		@Schema(description = "PDF 생성 기준(criteria) 맵.", example = "{\"start\":\"2025-11-01\",\"end\":\"2025-11-17\"}")
		Map<String, Object> criteria,

		@Schema(description = "PDF에 포함될 데이터 리스트. 각 항목은 컬럼명-값 맵입니다.")
		List<Map<String, Object>> data
) { }
