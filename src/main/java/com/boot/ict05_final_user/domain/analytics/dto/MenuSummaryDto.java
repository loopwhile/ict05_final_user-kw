package com.boot.ict05_final_user.domain.analytics.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 메뉴 분석 요약 DTO.
 *
 * <p>상단 카드에 표시할 주요 메뉴 분석 요약 정보를 묶어서 전달합니다.</p>
 *
 * @param topMenusByQty 판매수량 Top3 메뉴 목록.
 * @param topCategoriesBySales 매출 Top3 카테고리 목록.
 * @param topMenusBySalesContribution 매출 기여도 Top3 메뉴 목록.
 * @param lowPerformMenus 저성과 Top 메뉴(하위 3개) 목록.
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MenuSummaryDto(
		@Schema(description = "판매수량 Top3 메뉴 목록.")
		List<MenuTopMenuDto> topMenusByQty,

		@Schema(description = "매출 Top3 카테고리 목록.")
		List<MenuCategoryRankDto> topCategoriesBySales,

		@Schema(description = "매출 기여도 Top3 메뉴 목록.")
		List<MenuSalesContributionDto> topMenusBySalesContribution,

		@Schema(description = "저성과 Top 메뉴(하위 3개) 목록.")
		List<MenuLowPerformanceDto> lowPerformMenus
) {}
