package com.boot.ict05_final_user.domain.analytics.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 커서 페이징 응답 레코드.
 *
 * <p>일반적인 커서 기반 페이지 응답을 표현합니다.</p>
 *
 * @param items 현재 페이지 항목 목록.
 * @param nextCursor 다음 페이지로 이동할 커서. 없으면 null.
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record CursorPage<T>(
		@Schema(description = "현재 페이지 항목 목록.")
		List<T> items,

		@Schema(description = "다음 페이지 커서. 없으면 null.", example = "2025-11-08")
		String nextCursor
) {}
