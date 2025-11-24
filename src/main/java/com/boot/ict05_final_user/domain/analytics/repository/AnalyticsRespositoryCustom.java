package com.boot.ict05_final_user.domain.analytics.repository;

import com.boot.ict05_final_user.domain.analytics.dto.*;
import java.time.LocalDate;

/**
 * Analytics 전용 커스텀 리포지토리 인터페이스.
 *
 * <p>복잡한 집계 쿼리와 커서 기반 페이징, PDF 전달용 데이터 조회 등
 * Analytics 도메인에서 사용하는 원시 조회 메서드를 정의합니다.</p>
 *
 * <ul>
 *   <li>KPI, 주문, 메뉴, 재료, 시간/요일 분석을 위한 커스텀 조회</li>
 *   <li>리포트(PDF) 생성을 위한 전체 데이터 조회 지원</li>
 * </ul>
 *
 * 구현체는 QueryDSL/JPA Native Query 등을 사용하여 성능을 최적화해야 합니다.
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public interface AnalyticsRespositoryCustom {

	// =========================
	// KPI
	// =========================

	/**
	 * KPI 요약 카드용 데이터를 조회합니다.
	 *
	 * <p>today 기준으로 MTD, WoW 등 요약 지표를 계산하여 반환합니다.</p>
	 *
	 * @param storeId 조회 대상 점포 ID.
	 * @param today 조회 기준일 (KST 기준의 LocalDate).
	 * @return KPI 요약 DTO.
	 */
	KpiSummaryDto fetchKpiSummary(Long storeId, LocalDate today);

	/**
	 * KPI 테이블(일별/월별) 데이터의 커서 페이징 결과를 조회합니다.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건(기간, viewBy, size, cursor).
	 * @return 커서 기반의 KPI 행 페이지.
	 */
	CursorPage<KpiRowDto> fetchKpiRows(Long storeId, AnalyticsSearchDto cond);

	// =========================
	// 주문 분석
	// =========================

	/**
	 * 주문 분석 상단 요약(주문/매출 등)을 조회합니다.
	 *
	 * <p>MTD 기준 요약을 반환합니다.</p>
	 *
	 * @param storeId 점포 ID.
	 * @param today 조회 기준일.
	 * @return 주문 요약 DTO.
	 */
	OrderSummaryDto fetchOrderSummary(Long storeId, LocalDate today);

	/**
	 * 주문 분석 - 일별(주문 단위) 행들을 커서 페이징으로 조회합니다.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 주문 일별 커서 페이지.
	 */
	CursorPage<OrderDailyRowDto> fetchOrderDailyRows(Long storeId, AnalyticsSearchDto cond);

	/**
	 * 주문 분석 - 월별 집계 행들을 커서 페이징으로 조회합니다.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 주문 월별 커서 페이지.
	 */
	CursorPage<OrderMonthlyRowDto> fetchOrderMonthlyRows(Long storeId, AnalyticsSearchDto cond);

	// =========================
	// 메뉴 분석
	// =========================

	/**
	 * 메뉴 분석 상단 요약(Top 메뉴/카테고리 등)을 조회합니다.
	 *
	 * <p>MTD 기준의 판매수량 Top, 카테고리 매출 Top 등을 포함합니다.</p>
	 *
	 * @param storeId 점포 ID.
	 * @param today 조회 기준일.
	 * @return 메뉴 요약 DTO.
	 */
	MenuSummaryDto fetchMenuSummary(Long storeId, LocalDate today);

	/**
	 * 메뉴 분석 - 일별 테이블 데이터를 커서 페이징으로 조회합니다.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 메뉴 일별 커서 페이지.
	 */
	CursorPage<MenuDailyRowDto> fetchMenuDailyRows(Long storeId, AnalyticsSearchDto cond);

	/**
	 * 메뉴 분석 - 월별 테이블 데이터를 커서 페이징으로 조회합니다.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 메뉴 월별 커서 페이지.
	 */
	CursorPage<MenuMonthlyRowDto> fetchMenuMonthlyRows(Long storeId, AnalyticsSearchDto cond);

	// =========================
	// 시간/요일 분석
	// =========================

	/**
	 * 시간/요일 분석 상단 요약 카드를 조회합니다.
	 *
	 * <p>피크/비수 시간대, 주중/주말 합계 등 요약 지표를 반환합니다.</p>
	 *
	 * @param storeId 점포 ID.
	 * @param today 조회 기준일 (KST 기준).
	 * @return 시간/요일 요약 DTO.
	 */
	TimeDaySummaryDto fetchTimeDaySummary(Long storeId, LocalDate today);

	/**
	 * 시간대별 매출/주문수 차트 포인트를 조회합니다.
	 *
	 * @param storeId 점포 ID.
	 * @param startDate 조회 시작일 (inclusive).
	 * @param endDate 조회 종료일 (inclusive).
	 * @return 시간대별 포인트 리스트 (hour 7~20).
	 */
	java.util.List<TimeHourlyPointDto> fetchTimeHourlyChart(Long storeId, LocalDate startDate, LocalDate endDate);

	/**
	 * 요일별 매출/주문수 차트 포인트를 조회합니다.
	 *
	 * @param storeId 점포 ID.
	 * @param startDate 조회 시작일 (inclusive).
	 * @param endDate 조회 종료일 (inclusive).
	 * @return 요일별 포인트 리스트 (weekday 1~7, 월=1).
	 */
	java.util.List<WeekdaySalesPointDto> fetchWeekdayChart(Long storeId, LocalDate startDate, LocalDate endDate);

	/**
	 * 시간/요일 분석 - 일별 테이블(커서 기반)을 조회합니다.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 시간/요일 일별 커서 페이지.
	 */
	CursorPage<TimeDayDailyRowDto> fetchTimeDayDailyRows(Long storeId, AnalyticsSearchDto cond);

	/**
	 * 시간/요일 분석 - 월별 테이블(커서 기반)을 조회합니다.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 시간/요일 월별 커서 페이지.
	 */
	CursorPage<TimeDayMonthlyRowDto> fetchTimeDayMonthlyRows(Long storeId, AnalyticsSearchDto cond);

	// =========================
	// 재료 분석
	// =========================

	/**
	 * 재료 분석 상단 카드 요약을 조회합니다.
	 *
	 * <p>기간 규칙: AnalyticsService에서 전달한 today를 기준으로 MTD(이번달 1일 ~ 어제까지)와
	 * 전월 동일기간 비교를 수행하여 결과를 반환해야 합니다.</p>
	 *
	 * @param storeId 점포 ID.
	 * @param today 조회 기준일.
	 * @return 재료 요약 DTO.
	 */
	MaterialSummaryDto fetchMaterialSummary(Long storeId, LocalDate today);

	/**
	 * 재료 분석 - 일별 테이블을 커서 기반으로 조회합니다.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 재료 일별 커서 페이지.
	 */
	CursorPage<MaterialDailyRowDto> fetchMaterialDailyRows(Long storeId, AnalyticsSearchDto cond);

	/**
	 * 재료 분석 - 월별 테이블을 커서 기반으로 조회합니다.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 재료 월별 커서 페이지.
	 */
	CursorPage<MaterialMonthlyRowDto> fetchMaterialMonthlyRows(Long storeId, AnalyticsSearchDto cond);

}
