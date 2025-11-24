package com.boot.ict05_final_user.domain.analytics.controller;

import com.boot.ict05_final_user.config.security.principal.AppUser;
import com.boot.ict05_final_user.domain.analytics.dto.*;
import com.boot.ict05_final_user.domain.analytics.service.AnalyticsReportService;
import com.boot.ict05_final_user.domain.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 가맹점 통계 분석 REST 컨트롤러.
 *
 * <p>KPI, 주문, 메뉴, 재료, 시간/요일 분석 및 각종 PDF 리포트를 제공합니다.</p>
 *
 * <ul>
 *   <li>KPI 요약 및 테이블 조회</li>
 *   <li>주문(일/월), 메뉴(일/월), 재료(일/월), 시간/요일 분석 API</li>
 *   <li>PDF 리포트 다운로드 (KPI / Orders / Menus / Materials / Time-Day)</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "가맹점 통계 분석 API")
public class AnalyticsRestController {

	private final AnalyticsService service;
	private final AnalyticsReportService analyticsReportService;


	/**
	 * KPI 요약 카드 조회.
	 *
	 * <p>로그인된 가맹점 기준으로 이번 달 KPI 요약(MTD + WoW%) 데이터를 반환합니다.</p>
	 *
	 * @param appUser 인증 사용자 정보 (JWT)
	 * @return KPI 요약 DTO
	 */
	@Operation(summary = "KPI 요약 카드 조회", description = "로그인된 가맹점의 MTD + WoW% KPI 요약 데이터를 반환합니다.")
	@GetMapping("/api/analytics/kpi/summary")
	public ResponseEntity<KpiSummaryDto> getKpiSummary(
			@AuthenticationPrincipal AppUser appUser
	) {
		Long storeId = appUser.getStoreId();
		return ResponseEntity.ok(service.getKpiSummary(storeId));
	}


	/**
	 * KPI 테이블(일별/월별) 커서 페이징 조회.
	 *
	 * <p>일별 또는 월별 단위로 KPI 데이터를 커서 기반 페이징 형태로 반환합니다.</p>
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일 (YYYY-MM-DD, inclusive)
	 * @param end 조회 종료일 (YYYY-MM-DD, inclusive)
	 * @param viewBy DAY 또는 MONTH
	 * @param size 페이지 크기 (예: 50)
	 * @param cursor 커서 (이전 응답의 nextCursor, 없으면 첫 페이지)
	 * @return 커서 페이징된 KPI 행 목록
	 */
	@Operation(summary = "KPI 테이블 조회", description = "일별 또는 월별 단위 KPI 데이터를 커서 기반 페이징 형태로 반환합니다.")
	@GetMapping("/api/analytics/kpi/rows")
	public ResponseEntity<CursorPage<KpiRowDto>> getKpiRows(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "DAY") AnalyticsSearchDto.ViewBy viewBy,
			@RequestParam(defaultValue = "50") Integer size,
			@RequestParam(required = false) String cursor
	) {
		Long storeId = appUser.getStoreId();
		AnalyticsSearchDto cond = new AnalyticsSearchDto(
				LocalDate.parse(start),
				LocalDate.parse(end),
				viewBy, size, cursor
		);
		return ResponseEntity.ok(service.getKpiRows(storeId, cond));
	}


	/**
	 * 주문 분석 Summary (상단 카드) 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @return 주문 요약 DTO
	 */
	@Operation(summary = "주문 분석 요약 카드 조회", description = "이번 달 주문수 및 매출 요약을 반환합니다.")
	@GetMapping("/api/analytics/orders/summary")
	public ResponseEntity<OrderSummaryDto> getOrderSummary(
			@AuthenticationPrincipal AppUser appUser
	) {
		Long storeId = appUser.getStoreId();
		return ResponseEntity.ok(service.getOrderSummary(storeId));
	}


	/**
	 * 주문 분석 테이블 - 일별(주문 단위) 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일 (YYYY-MM-DD)
	 * @param end 조회 종료일 (YYYY-MM-DD)
	 * @param size 페이지 크기
	 * @param cursor 커서
	 * @return 주문 일별 행 목록 DTO (커서 페이징)
	 */
	@Operation(summary = "주문 분석 일별 테이블", description = "일별 주문 단위로 판매/매출 데이터를 커서 페이징 형태로 반환합니다.")
	@GetMapping("/api/analytics/orders/day-rows")
	public ResponseEntity<CursorPage<OrderDailyRowDto>> getOrderDailyRows(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "50") Integer size,
			@RequestParam(required = false) String cursor
	) {
		Long storeId = appUser.getStoreId();
		AnalyticsSearchDto cond = new AnalyticsSearchDto(
				LocalDate.parse(start),
				LocalDate.parse(end),
				AnalyticsSearchDto.ViewBy.DAY,
				size,
				cursor
		);
		return ResponseEntity.ok(service.getOrderDailyRows(storeId, cond));
	}


	/**
	 * 주문 분석 테이블 - 월별(월 단위 집계) 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작월 (YYYY-MM-DD 형식의 월 시작일 사용 가능)
	 * @param end 조회 종료월 (YYYY-MM-DD 형식의 월 종료일 사용 가능)
	 * @param size 페이지 크기
	 * @param cursor 커서
	 * @return 주문 월별 집계 DTO (커서 페이징)
	 */
	@Operation(summary = "주문 분석 월별 테이블", description = "월 단위로 주문 및 매출 집계 데이터를 커서 페이징 형태로 반환합니다.")
	@GetMapping("/api/analytics/orders/month-rows")
	public ResponseEntity<CursorPage<OrderMonthlyRowDto>> getOrderMonthlyRows(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "50") Integer size,
			@RequestParam(required = false) String cursor
	) {
		Long storeId = appUser.getStoreId();
		AnalyticsSearchDto cond = new AnalyticsSearchDto(
				LocalDate.parse(start),
				LocalDate.parse(end),
				AnalyticsSearchDto.ViewBy.MONTH,
				size,
				cursor
		);
		return ResponseEntity.ok(service.getOrderMonthlyRows(storeId, cond));
	}


	/**
	 * 메뉴 분석 Summary (상단 카드) 조회.
	 *
	 * <p>판매수량 TOP3, 카테고리 매출 TOP3, 평균 판매가, 재고 소진률 TOP3 등을 반환합니다.</p>
	 *
	 * @param appUser 인증 사용자 정보
	 * @return 메뉴 요약 DTO
	 */
	@Operation(
			summary = "메뉴 분석 요약 카드",
			description = "판매수량 TOP3, 카테고리 매출 TOP3, 평균 판매가, 재고 소진률 TOP3를 반환합니다. 기간은 MTD 기준입니다."
	)
	@GetMapping("/api/analytics/menus/summary")
	public ResponseEntity<MenuSummaryDto> getMenuSummary(
			@AuthenticationPrincipal AppUser appUser
	) {
		Long storeId = appUser.getStoreId();
		return ResponseEntity.ok(service.getMenuSummary(storeId));
	}


	/**
	 * 메뉴 분석 테이블 - 일별 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일
	 * @param end 조회 종료일
	 * @param size 페이지 크기
	 * @param cursor 커서
	 * @return 메뉴 일별 행 목록 DTO (커서 페이징)
	 */
	@Operation(summary = "메뉴 분석 일별 테이블", description = "일별 메뉴 판매/매출/주문수 집계 테이블을 커서 페이징 형태로 반환합니다.")
	@GetMapping("/api/analytics/menus/day-rows")
	public ResponseEntity<CursorPage<MenuDailyRowDto>> getMenuDailyRows(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "50") Integer size,
			@RequestParam(required = false) String cursor
	) {
		Long storeId = appUser.getStoreId();
		AnalyticsSearchDto cond = new AnalyticsSearchDto(
				LocalDate.parse(start),
				LocalDate.parse(end),
				AnalyticsSearchDto.ViewBy.DAY,
				size,
				cursor
		);
		return ResponseEntity.ok(service.getMenuDailyRows(storeId, cond));
	}


	/**
	 * 메뉴 분석 테이블 - 월별 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작월
	 * @param end 조회 종료월
	 * @param size 페이지 크기
	 * @param cursor 커서
	 * @return 메뉴 월별 집계 DTO (커서 페이징)
	 */
	@Operation(summary = "메뉴 분석 월별 테이블", description = "월별 메뉴 판매/매출/주문수 집계 테이블을 커서 페이징 형태로 반환합니다.")
	@GetMapping("/api/analytics/menus/month-rows")
	public ResponseEntity<CursorPage<MenuMonthlyRowDto>> getMenuMonthlyRows(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "50") Integer size,
			@RequestParam(required = false) String cursor
	) {
		Long storeId = appUser.getStoreId();
		AnalyticsSearchDto cond = new AnalyticsSearchDto(
				LocalDate.parse(start),
				LocalDate.parse(end),
				AnalyticsSearchDto.ViewBy.MONTH,
				size,
				cursor
		);
		return ResponseEntity.ok(service.getMenuMonthlyRows(storeId, cond));
	}


	/**
	 * 재료 분석 Summary (상단 카드) 조회.
	 *
	 * <p>이번 달 1일 ~ 어제까지 재료 사용량/원가/원가율 및 재고 위험/유통기한 임박 재료 수를 반환합니다.</p>
	 *
	 * @param appUser 인증 사용자 정보
	 * @return 재료 요약 DTO
	 */
	@Operation(summary = "재료 분석 상단 요약 카드", description = "이번달 1일 ~ 어제까지 재료 사용량/원가/원가율 및 재고 위험/유통기한 임박 재료 수를 반환합니다.")
	@GetMapping("/api/analytics/materials/summary")
	public ResponseEntity<MaterialSummaryDto> getMaterialSummary(
			@AuthenticationPrincipal AppUser appUser
	) {
		Long storeId = appUser.getStoreId();
		return ResponseEntity.ok(service.getMaterialSummary(storeId));
	}


	/**
	 * 재료 분석 테이블 - 일별 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일
	 * @param end 조회 종료일
	 * @param size 페이지 크기
	 * @param cursor 커서
	 * @return 재료 일별 행 목록 DTO (커서 페이징)
	 */
	@Operation(summary = "재료 분석 일별 테이블", description = "일 단위 재료 사용량/원가/매출 대비 비중 등을 커서 기반 페이징으로 반환합니다.")
	@GetMapping("/api/analytics/materials/day-rows")
	public ResponseEntity<CursorPage<MaterialDailyRowDto>> getMaterialDailyRows(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "50") Integer size,
			@RequestParam(required = false) String cursor
	) {
		Long storeId = appUser.getStoreId();
		LocalDate startDate = LocalDate.parse(start);
		LocalDate endDate = LocalDate.parse(end);

		AnalyticsSearchDto cond = new AnalyticsSearchDto(
				startDate,
				endDate,
				AnalyticsSearchDto.ViewBy.DAY,
				size,
				cursor
		);
		return ResponseEntity.ok(service.getMaterialDailyRows(storeId, cond));
	}


	/**
	 * 재료 분석 테이블 - 월별 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작월
	 * @param end 조회 종료월
	 * @param size 페이지 크기
	 * @param cursor 커서
	 * @return 재료 월별 집계 DTO (커서 페이징)
	 */
	@Operation(summary = "재료 분석 월별 테이블", description = "월 단위 재료 사용량/원가/원가율 등을 커서 기반 페이징으로 반환합니다.")
	@GetMapping("/api/analytics/materials/month-rows")
	public ResponseEntity<CursorPage<MaterialMonthlyRowDto>> getMaterialMonthlyRows(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "50") Integer size,
			@RequestParam(required = false) String cursor
	) {
		Long storeId = appUser.getStoreId();
		LocalDate startDate = LocalDate.parse(start);
		LocalDate endDate = LocalDate.parse(end);

		AnalyticsSearchDto cond = new AnalyticsSearchDto(
				startDate,
				endDate,
				AnalyticsSearchDto.ViewBy.MONTH,
				size,
				cursor
		);
		return ResponseEntity.ok(service.getMaterialMonthlyRows(storeId, cond));
	}


	// ==================================================
	//               ★ 시간/요일 분석 (신규) ★
	// ==================================================


	/**
	 * 시간/요일 분석 상단 요약 카드 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @return 시간/요일 요약 DTO
	 */
	@Operation(summary = "시간/요일 분석 요약 카드 조회", description = "시간대 및 요일별 매출/주문수 요약 데이터를 반환합니다.")
	@GetMapping("/api/analytics/time-day/summary")
	public ResponseEntity<TimeDaySummaryDto> getTimeDaySummary(
			@AuthenticationPrincipal AppUser appUser
	) {
		Long storeId = appUser.getStoreId();
		return ResponseEntity.ok(service.getTimeDaySummary(storeId));
	}


	/**
	 * 시간대별 매출/주문수 차트 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일
	 * @param end 조회 종료일
	 * @return 시간대별 포인트 목록
	 */
	@Operation(summary = "시간대별 차트 조회", description = "지정 기간의 시간대별 매출 및 주문수 차트를 반환합니다.")
	@GetMapping("/api/analytics/time-day/hourly-chart")
	public ResponseEntity<java.util.List<TimeHourlyPointDto>> getTimeDayHourlyChart(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end
	) {
		Long storeId = appUser.getStoreId();
		LocalDate startDate = LocalDate.parse(start);
		LocalDate endDate = LocalDate.parse(end);
		return ResponseEntity.ok(service.getTimeDayHourlyChart(storeId, startDate, endDate));
	}


	/**
	 * 요일별 매출/주문수 차트 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일
	 * @param end 조회 종료일
	 * @return 요일별 포인트 목록
	 */
	@Operation(summary = "요일별 차트 조회", description = "지정 기간의 요일별 매출 및 주문수 차트를 반환합니다.")
	@GetMapping("/api/analytics/time-day/weekday-chart")
	public ResponseEntity<java.util.List<WeekdaySalesPointDto>> getWeekdayChart(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end
	) {
		Long storeId = appUser.getStoreId();
		LocalDate startDate = LocalDate.parse(start);
		LocalDate endDate = LocalDate.parse(end);
		return ResponseEntity.ok(service.getWeekdayChart(storeId, startDate, endDate));
	}


	/**
	 * 시간/요일 분석 - 일별 테이블 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일
	 * @param end 조회 종료일
	 * @param size 페이지 크기
	 * @param cursor 커서
	 * @return 시간/요일 일별 행 목록 DTO (커서 페이징)
	 */
	@Operation(summary = "시간/요일 분석 일별 테이블", description = "일 단위로 시간/요일별 매출 및 주문 데이터를 반환합니다.")
	@GetMapping("/api/analytics/time-day/day-rows")
	public ResponseEntity<CursorPage<TimeDayDailyRowDto>> getTimeDayDailyRows(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "50") Integer size,
			@RequestParam(required = false) String cursor
	) {
		Long storeId = appUser.getStoreId();
		AnalyticsSearchDto cond = new AnalyticsSearchDto(
				LocalDate.parse(start),
				LocalDate.parse(end),
				AnalyticsSearchDto.ViewBy.DAY,
				size,
				cursor
		);
		return ResponseEntity.ok(service.getTimeDayDailyRows(storeId, cond));
	}


	/**
	 * 시간/요일 분석 - 월별 테이블 조회.
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작월
	 * @param end 조회 종료월
	 * @param size 페이지 크기
	 * @param cursor 커서
	 * @return 시간/요일 월별 집계 DTO (커서 페이징)
	 */
	@Operation(summary = "시간/요일 분석 월별 테이블", description = "월 단위로 시간/요일별 매출 및 주문 데이터를 반환합니다.")
	@GetMapping("/api/analytics/time-day/month-rows")
	public ResponseEntity<CursorPage<TimeDayMonthlyRowDto>> getTimeDayMonthlyRows(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "50") Integer size,
			@RequestParam(required = false) String cursor
	) {
		Long storeId = appUser.getStoreId();
		AnalyticsSearchDto cond = new AnalyticsSearchDto(
				LocalDate.parse(start),
				LocalDate.parse(end),
				AnalyticsSearchDto.ViewBy.MONTH,
				size,
				cursor
		);
		return ResponseEntity.ok(service.getTimeDayMonthlyRows(storeId, cond));
	}


	/**
	 * KPI 분석 PDF 다운로드.
	 *
	 * <p>KPI 요약 및 테이블 데이터를 PDF로 생성하여 반환합니다.</p>
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일
	 * @param end 조회 종료일
	 * @param viewBy 조회 단위 (DAY or MONTH)
	 * @return PDF 파일 바이트 응답
	 */
	@Operation(summary = "KPI 분석 PDF 다운로드", description = "KPI 요약 및 테이블 데이터를 PDF로 다운로드합니다.")
	@GetMapping("/api/analytics/kpi/report")
	public ResponseEntity<byte[]> downloadKpiReport(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "DAY") AnalyticsSearchDto.ViewBy viewBy
	) {
		Long storeId = appUser.getStoreId();
		LocalDate startDate = LocalDate.parse(start);
		LocalDate endDate = LocalDate.parse(end);

		byte[] pdfBytes = analyticsReportService.generateKpiReport(
				storeId, startDate, endDate, viewBy
		);

		String filename = "kpi-report_" + viewBy.name().toLowerCase() + "_" + start + "_" + end + ".pdf";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(
				ContentDisposition.attachment()
						.filename(filename, StandardCharsets.UTF_8)
						.build()
		);

		return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
	}


	/**
	 * 주문 분석 PDF 다운로드.
	 *
	 * <p>주문 분석 상단 요약 및 일/월별 데이터를 PDF로 생성하여 반환합니다.</p>
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일
	 * @param end 조회 종료일
	 * @param viewBy 조회 단위 (DAY or MONTH)
	 * @return PDF 파일 바이트 응답
	 */
	@Operation(summary = "주문 분석 PDF 다운로드", description = "주문 분석 상단 요약 및 일/월별 데이터를 PDF로 생성하여 다운로드합니다.")
	@GetMapping("/api/analytics/orders/report")
	public ResponseEntity<byte[]> downloadOrdersReport(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "DAY") AnalyticsSearchDto.ViewBy viewBy
	) {
		Long storeId = appUser.getStoreId();
		LocalDate startDate = LocalDate.parse(start);
		LocalDate endDate = LocalDate.parse(end);

		byte[] pdfBytes = analyticsReportService.generateOrdersReport(
				storeId, startDate, endDate, viewBy
		);

		String filename = "orders-report_" + viewBy.name().toLowerCase() + "_" + start + "_" + end + ".pdf";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(
				ContentDisposition.attachment()
						.filename(filename, StandardCharsets.UTF_8)
						.build()
		);

		return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
	}


	/**
	 * 메뉴 분석 PDF 다운로드.
	 *
	 * <p>메뉴 분석 상단 요약 및 일/월별 데이터를 PDF로 생성하여 반환합니다.</p>
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일
	 * @param end 조회 종료일
	 * @param viewBy 조회 단위 (DAY or MONTH)
	 * @return PDF 파일 바이트 응답
	 */
	@Operation(summary = "메뉴 분석 PDF 다운로드", description = "메뉴 분석 상단 요약 및 일/월별 데이터를 PDF로 생성하여 다운로드합니다.")
	@GetMapping("/api/analytics/menus/report")
	public ResponseEntity<byte[]> downloadMenuReport(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "DAY") AnalyticsSearchDto.ViewBy viewBy
	) {
		Long storeId = appUser.getStoreId();
		LocalDate startDate = LocalDate.parse(start);
		LocalDate endDate = LocalDate.parse(end);

		byte[] pdfBytes = analyticsReportService.generateMenuReport(
				storeId, startDate, endDate, viewBy
		);

		String filename = "menu-report_" + viewBy.name().toLowerCase() + "_" + start + "_" + end + ".pdf";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(
				ContentDisposition.attachment()
						.filename(filename, StandardCharsets.UTF_8)
						.build()
		);

		return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
	}


	/**
	 * 시간/요일 분석 PDF 리포트 다운로드.
	 *
	 * <p>시간/요일 분석 상단 요약 + 일/월별 테이블 데이터를 PDF로 생성하여 반환합니다.</p>
	 *
	 * 예:
	 * GET /api/analytics/time-day/report?start=2025-11-01&end=2025-11-17&viewBy=DAY
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일
	 * @param end 조회 종료일
	 * @param viewBy 조회 단위 (DAY or MONTH)
	 * @return PDF 파일 바이트 응답
	 */
	@Operation(summary = "시간/요일 분석 PDF 리포트 다운로드", description = "시간/요일 분석 상단 요약 + 일/월별 테이블 데이터를 PDF로 생성하여 다운로드합니다.")
	@GetMapping("/api/analytics/time-day/report")
	public ResponseEntity<byte[]> downloadTimeDayReport(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "DAY") AnalyticsSearchDto.ViewBy viewBy
	) {
		Long storeId = appUser.getStoreId();
		LocalDate startDate = LocalDate.parse(start);
		LocalDate endDate = LocalDate.parse(end);

		byte[] pdfBytes = analyticsReportService.generateTimeDayReport(
				storeId, startDate, endDate, viewBy
		);

		String filename = "time-day-report_" + viewBy.name().toLowerCase() + "_" + start + "_" + end + ".pdf";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(
				ContentDisposition.attachment()
						.filename(filename, StandardCharsets.UTF_8)
						.build()
		);

		return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
	}


	/**
	 * 재료 분석 PDF 리포트 다운로드.
	 *
	 * <p>재료 분석 상단 요약 + 일/월별 테이블 데이터를 PDF로 생성하여 반환합니다.</p>
	 *
	 * 예:
	 * GET /api/analytics/materials/report?start=2025-11-01&end=2025-11-17&viewBy=DAY
	 *
	 * @param appUser 인증 사용자 정보
	 * @param start 조회 시작일
	 * @param end 조회 종료일
	 * @param viewBy 조회 단위 (DAY or MONTH)
	 * @return PDF 파일 바이트 응답
	 */
	@Operation(summary = "재료 분석 PDF 리포트 다운로드", description = "재료 분석 상단 요약 + 일/월별 테이블 데이터를 PDF로 생성하여 다운로드합니다.")
	@GetMapping("/api/analytics/materials/report")
	public ResponseEntity<byte[]> downloadMaterialReport(
			@AuthenticationPrincipal AppUser appUser,
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam(defaultValue = "DAY") AnalyticsSearchDto.ViewBy viewBy
	) {
		Long storeId = appUser.getStoreId();
		LocalDate startDate = LocalDate.parse(start);
		LocalDate endDate = LocalDate.parse(end);

		byte[] pdfBytes = analyticsReportService.generateMaterialReport(
				storeId, startDate, endDate, viewBy
		);

		String filename = "material-report_" + viewBy.name().toLowerCase() + "_" + start + "_" + end + ".pdf";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(
				ContentDisposition.attachment()
						.filename(filename, StandardCharsets.UTF_8)
						.build()
		);

		return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
	}

}
