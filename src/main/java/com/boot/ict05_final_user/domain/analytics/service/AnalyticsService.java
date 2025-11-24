package com.boot.ict05_final_user.domain.analytics.service;

import com.boot.ict05_final_user.domain.analytics.dto.*;
import com.boot.ict05_final_user.domain.analytics.repository.AnalyticsRespositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Analytics 비즈니스 서비스.
 *
 * <p>Repository 계층(AnalyticsRespositoryCustom)을 통해 분석용 데이터를 조회하고,
 * 컨트롤러에서 호출할 수 있도록 도메인 단위 메서드를 제공합니다.</p>
 *
 * <ul>
 *   <li>KPI / 주문 / 메뉴 / 재료 / 시간-요일 분석 관련 조회 메서드 제공</li>
 *   <li>시간대는 KST(Asia/Seoul) 기준으로 계산</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

	private final AnalyticsRespositoryCustom repo;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/**
	 * KPI 요약 카드 조회.
	 *
	 * <p>KST 오늘 날짜를 기준으로 MTD 등의 요약 지표를 repo에서 조회하여 반환합니다.</p>
	 *
	 * @param storeId 조회 대상 점포 ID.
	 * @return KPI 요약 DTO.
	 */
	public KpiSummaryDto getKpiSummary(Long storeId) {
		LocalDate today = LocalDate.now(KST);
		return repo.fetchKpiSummary(storeId, today);
	}

	/**
	 * KPI 테이블(일별/월별) 커서 페이징 조회.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건 (startDate, endDate, viewBy, size, cursor).
	 * @return 커서 기반 KPI 행 페이지.
	 */
	public CursorPage<KpiRowDto> getKpiRows(Long storeId, AnalyticsSearchDto cond) {
		return repo.fetchKpiRows(storeId, cond);
	}

	// ===== 주문 분석 =====

	/**
	 * 주문 분석 상단 요약 카드 조회.
	 *
	 * <p>KST 오늘 기준으로 MTD(이번달 1일 ~ 어제까지) 집계를 리턴합니다.</p>
	 *
	 * @param storeId 점포 ID.
	 * @return 주문 요약 DTO.
	 */
	public OrderSummaryDto getOrderSummary(Long storeId) {
		LocalDate today = LocalDate.now(KST);
		return repo.fetchOrderSummary(storeId, today);
	}

	/**
	 * 주문 분석 - 일별(주문 단위) 커서 페이징 조회.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 주문 일별 행들의 커서 페이지.
	 */
	public CursorPage<OrderDailyRowDto> getOrderDailyRows(Long storeId, AnalyticsSearchDto cond) {
		return repo.fetchOrderDailyRows(storeId, cond);
	}

	/**
	 * 주문 분석 - 월별(월 단위 집계) 커서 페이징 조회.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 주문 월별 행들의 커서 페이지.
	 */
	public CursorPage<OrderMonthlyRowDto> getOrderMonthlyRows(Long storeId, AnalyticsSearchDto cond) {
		return repo.fetchOrderMonthlyRows(storeId, cond);
	}

	// ===== 메뉴 분석 =====

	/**
	 * 메뉴 분석 상단 카드 요약을 조회합니다.
	 *
	 * <p>기준: KST 오늘 날짜 기준 "이번달 1일 ~ 어제까지". 판매수량 TOP3, 카테고리 매출 TOP3 등을 포함합니다.</p>
	 *
	 * @param storeId 점포 ID.
	 * @return 메뉴 요약 DTO.
	 */
	public MenuSummaryDto getMenuSummary(Long storeId) {
		LocalDate today = LocalDate.now(KST);
		return repo.fetchMenuSummary(storeId, today);
	}

	/**
	 * 메뉴 분석 일별 테이블 조회.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 메뉴 일별 행들의 커서 페이지.
	 */
	public CursorPage<MenuDailyRowDto> getMenuDailyRows(Long storeId, AnalyticsSearchDto cond) {
		return repo.fetchMenuDailyRows(storeId, cond);
	}

	/**
	 * 메뉴 분석 월별 테이블 조회.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 메뉴 월별 행들의 커서 페이지.
	 */
	public CursorPage<MenuMonthlyRowDto> getMenuMonthlyRows(Long storeId, AnalyticsSearchDto cond) {
		return repo.fetchMenuMonthlyRows(storeId, cond);
	}

	// ===== 시간/요일 분석 =====

	/**
	 * 시간/요일 분석 상단 요약 카드 조회.
	 *
	 * <p>피크/비수 시간대, 주중/주말 집계 등을 포함합니다.</p>
	 *
	 * @param storeId 점포 ID.
	 * @return 시간/요일 요약 DTO.
	 */
	public TimeDaySummaryDto getTimeDaySummary(Long storeId) {
		LocalDate todayKst = LocalDate.now(KST);
		return repo.fetchTimeDaySummary(storeId, todayKst);
	}

	/**
	 * 시간대별 매출/주문수 차트 데이터 조회.
	 *
	 * @param storeId 점포 ID.
	 * @param startDate 조회 시작일 (YYYY-MM-DD).
	 * @param endDate 조회 종료일 (YYYY-MM-DD).
	 * @return 시간대별 포인트 리스트.
	 */
	public List<TimeHourlyPointDto> getTimeDayHourlyChart(Long storeId, LocalDate startDate, LocalDate endDate) {
		return repo.fetchTimeHourlyChart(storeId, startDate, endDate);
	}

	/**
	 * 요일별 매출/주문수 차트 데이터 조회.
	 *
	 * @param storeId 점포 ID.
	 * @param startDate 조회 시작일 (YYYY-MM-DD).
	 * @param endDate 조회 종료일 (YYYY-MM-DD).
	 * @return 요일별 포인트 리스트.
	 */
	public List<WeekdaySalesPointDto> getWeekdayChart(Long storeId, LocalDate startDate, LocalDate endDate) {
		return repo.fetchWeekdayChart(storeId, startDate, endDate);
	}

	/**
	 * 시간/요일 분석 - 일별 테이블 조회.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 시간/요일 일별 행들의 커서 페이지.
	 */
	public CursorPage<TimeDayDailyRowDto> getTimeDayDailyRows(Long storeId, AnalyticsSearchDto cond) {
		return repo.fetchTimeDayDailyRows(storeId, cond);
	}

	/**
	 * 시간/요일 분석 - 월별 테이블 조회.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 시간/요일 월별 행들의 커서 페이지.
	 */
	public CursorPage<TimeDayMonthlyRowDto> getTimeDayMonthlyRows(Long storeId, AnalyticsSearchDto cond) {
		return repo.fetchTimeDayMonthlyRows(storeId, cond);
	}

	/**
	 * 재료 분석 상단 요약 카드 조회.
	 *
	 * <p>MTD 및 전월 비교 등의 재료 KPI를 반환합니다.</p>
	 *
	 * @param storeId 점포 ID.
	 * @return 재료 요약 DTO.
	 */
	public MaterialSummaryDto getMaterialSummary(Long storeId) {
		LocalDate today = LocalDate.now(KST);
		System.out.println("재료 카드 쿼리 나오나?" + repo.fetchMaterialSummary(storeId, today));

		return repo.fetchMaterialSummary(storeId, today);
	}

	/**
	 * 재료 분석 일별 테이블 조회.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 재료 일별 행들의 커서 페이지.
	 */
	public CursorPage<MaterialDailyRowDto> getMaterialDailyRows(Long storeId, AnalyticsSearchDto cond) {
		System.out.println("재료 일별 쿼리 나오나?" + repo.fetchMaterialDailyRows(storeId, cond));
		return repo.fetchMaterialDailyRows(storeId, cond);
	}

	/**
	 * 재료 분석 월별 테이블 조회.
	 *
	 * @param storeId 점포 ID.
	 * @param cond 조회 조건.
	 * @return 재료 월별 행들의 커서 페이지.
	 */
	public CursorPage<MaterialMonthlyRowDto> getMaterialMonthlyRows(Long storeId, AnalyticsSearchDto cond) {
		System.out.println("재료 월별 쿼리 나오나?" + repo.fetchMaterialMonthlyRows(storeId, cond));
		return repo.fetchMaterialMonthlyRows(storeId, cond);
	}

}
