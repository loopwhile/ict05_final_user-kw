package com.boot.ict05_final_user.domain.analytics.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 재료 분석 상단 요약 카드 DTO.
 *
 * <p>기간 정의:
 * <ul>
 *   <li>MTD(이번달): today 기준, {@code thisMonthStart = today.withDayOfMonth(1)}</li>
 *   <li>집계 기간: [thisMonthStart 00:00, today 00:00) → "이번달 1일 ~ 어제까지"</li>
 *   <li>전월 비교: {@code prevMonthStart = thisMonthStart.minusMonths(1)}</li>
 * </ul>
 * </p>
 *
 * <p>원가율 정의:
 * <ul>
 *   <li>원가율 = (재료 원가 합계 ÷ 매출 합계) × 100</li>
 *   <li>{@code currentCostRate}와 {@code prevCostRate}는 0~100 범위의 퍼센트 값(소수점 1자리)</li>
 *   <li>{@code costRateDiff} = currentCostRate - prevCostRate (퍼센트 포인트)</li>
 * </ul>
 * </p>
 *
 * <p>카드 구성(예시):
 * <ul>
 *   <li>TopByUsage, TopByCost: 상위 재료 리스트</li>
 *   <li>원가율 카드: currentCostRate, prevCostRate, costRateDiff</li>
 *   <li>리스크 카드: lowStockCount, expireSoonCount</li>
 * </ul>
 * </p>
 *
 * @param topByUsage 사용량 기준 Top 재료 리스트.
 * @param topByCost 원가 기준 Top 재료 리스트.
 * @param currentCostRate 이번달 원가율(%) 소수점1자리.
 * @param prevCostRate 전월 동일기간 원가율(%) 소수점1자리.
 * @param costRateDiff 원가율 증감 (percentage point).
 * @param lowStockCount 재고 부족 위험 재료 수.
 * @param expireSoonCount 유통기한 임박 재료 수.
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record MaterialSummaryDto(

		@Schema(description = "사용량 기준 Top 재료 리스트.")
		List<MaterialTopItemDto> topByUsage,

		@Schema(description = "원가 기준 Top 재료 리스트.")
		List<MaterialTopItemDto> topByCost,

		@Schema(description = "이번달 원가율(%), 소수점1자리.", example = "3.6")
		double currentCostRate,

		@Schema(description = "전월 동일기간 원가율(%), 소수점1자리.", example = "3.2")
		double prevCostRate,

		@Schema(description = "원가율 증감 (percentage point).", example = "0.4")
		double costRateDiff,

		@Schema(description = "재고 부족 위험 재료 수.", example = "5")
		long lowStockCount,

		@Schema(description = "유통기한 임박 재료 수.", example = "2")
		long expireSoonCount
) {
}
