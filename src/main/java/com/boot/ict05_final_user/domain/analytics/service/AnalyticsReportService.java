package com.boot.ict05_final_user.domain.analytics.service;

import com.boot.ict05_final_user.config.PythonPdfClient;
import com.boot.ict05_final_user.domain.analytics.dto.*;
import com.boot.ict05_final_user.domain.store.entity.Store;
import com.boot.ict05_final_user.domain.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 분석 리포트(PDF) 생성 서비스.
 *
 * <p>AnalyticsService로부터 데이터를 수집하여 Python FastAPI 기반 PDF 서비스에 페이로드를 전송하고,
 * 생성된 PDF 바이트를 반환합니다.</p>
 *
 * <ul>
 *   <li>KPI / Orders / Menu / TimeDay / Material 리포트 생성</li>
 *   <li>PythonPdfClient를 통해 외부 PDF 생성 서비스와 통신</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsReportService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AnalyticsService analyticsService;
    private final PythonPdfClient pythonPdfClient;
    private final StoreService storeService;

    // =========================================
    // KPI 리포트
    // =========================================

    /**
     * KPI 리포트(PDF) 생성 요청을 수행합니다.
     *
     * <p>내부적으로 AnalyticsService에서 KPI 행을 모두 조회한 뒤 PythonPdfClient로 전송합니다.</p>
     *
     * @param storeId 점포 ID.
     * @param startDate 조회 시작일 (YYYY-MM-DD).
     * @param endDate 조회 종료일 (YYYY-MM-DD).
     * @param viewBy 집계 단위 (DAY / MONTH).
     * @return 생성된 PDF 바이트 배열.
     * @throws RuntimeException PDF 생성 실패 또는 외부 서비스 통신 실패 시 발생할 수 있습니다.
     */
    public byte[] generateKpiReport(Long storeId,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    AnalyticsSearchDto.ViewBy viewBy) {

        // 1) 점포명 (StoreService로 실제 점포명 조회)
        String storeName = resolveStoreName(storeId);

        // 2) KPI 테이블 데이터 전체 가져오기 (리포트용이니 사이즈 넉넉하게)
        AnalyticsSearchDto cond = new AnalyticsSearchDto(
                startDate,
                endDate,
                viewBy,
                500,
                null
        );
        CursorPage<KpiRowDto> page = analyticsService.getKpiRows(storeId, cond);
        List<KpiRowDto> rows = page.items();

        // 3) criteria 구성
        Map<String, Object> criteria = new LinkedHashMap<>();
        criteria.put("storeId", storeId);
        criteria.put("storeName", storeName);
        criteria.put("startDate", startDate.toString());
        criteria.put("endDate", endDate.toString());
        criteria.put("viewBy", viewBy.name());

        // 4) data 리스트 (Python KpiRow에 맞게 key 채우기)
        List<Map<String, Object>> data = rows.stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();

                    // ✅ KpiRowDto 필드에 맞게 매핑
                    m.put("storeName", storeName);

                    // KpiRowDto: long / double 타입 → Number로 auto boxing
                    m.put("sales", safeNumber(r.sales()));   // long → Double
                    m.put("transaction", safeInt(r.tx()));   // long → Integer
                    m.put("upt", safeNumber(r.upt()));       // double → Double
                    m.put("ads", safeNumber(r.ads()));       // long → Double
                    m.put("aur", safeNumber(r.aur()));       // long → Double

                    // 아직 안 쓰는 필드는 일단 null로 채우기 (Optional 이라 상관 없음)
                    m.put("compMoM", null);
                    m.put("compYoY", null);

                    // 날짜 라벨 ("YYYY-MM-DD" 또는 "YYYY-MM")
                    m.put("date", r.label());

                    // 채널 비율도 아직 계산 안 하니 null
                    m.put("ratioVisit", null);
                    m.put("ratioTakeout", null);
                    m.put("ratioDelivery", null);

                    return m;
                })
                .toList();

        KpiPdfPayload payload = new KpiPdfPayload(criteria, data);

        log.info("[KPI-Report] criteria={}, rows={}", criteria, data.size());
        return pythonPdfClient.requestKpiReport(payload);
    }

    // =========================================
    // 주문 분석 리포트
    // =========================================

    /**
     * 주문 분석 리포트(PDF) 생성.
     *
     * <p>viewBy에 따라 일단위(주문 단위) 또는 월단위 집계 데이터를 Python PDF 서비스로 전송합니다.</p>
     *
     * @param storeId 점포 ID.
     * @param startDate 조회 시작일.
     * @param endDate 조회 종료일.
     * @param viewBy DAY 또는 MONTH.
     * @return 생성된 PDF 바이트 배열.
     * @throws RuntimeException PDF 생성 또는 외부 통신 실패 시 발생할 수 있습니다.
     */
    public byte[] generateOrdersReport(Long storeId,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       AnalyticsSearchDto.ViewBy viewBy) {

        String storeName = resolveStoreName(storeId);   // 아까 만든 메서드 써도 됨

        AnalyticsSearchDto cond = new AnalyticsSearchDto(
                startDate,
                endDate,
                viewBy,
                500,
                null
        );

        if (viewBy == AnalyticsSearchDto.ViewBy.DAY) {
            // ===== 일별(주문 단위) =====
            CursorPage<OrderDailyRowDto> page = analyticsService.getOrderDailyRows(storeId, cond);
            List<OrderDailyRowDto> rows = page.items();

            Map<String, Object> criteria = new LinkedHashMap<>();
            criteria.put("storeId", storeId);
            criteria.put("storeName", storeName);
            criteria.put("startDate", startDate.toString());
            criteria.put("endDate", endDate.toString());
            criteria.put("viewBy", "DAY");

            List<Map<String, Object>> data = rows.stream()
                    .map(r -> {
                        Map<String, Object> m = new LinkedHashMap<>();

                        m.put("date", r.orderDate());          // YYYY-MM-DD
                        m.put("orderId", r.orderId());         // 주문 ID
                        m.put("orderType", r.orderType());     // VISIT/TAKEOUT/DELIVERY
                        m.put("orderCount", 1);                // 주문 1건 단위
                        m.put("totalPrice", safeNumber(r.totalPrice()));
                        m.put("menuCount", safeInt(r.menuCount()));
                        m.put("paymentType", r.paymentType()); // CARD/CASH/...
                        m.put("channelMemo", r.channelMemo()); // 메모

                        return m;
                    })
                    .toList();

            OrdersPdfPayload payload = new OrdersPdfPayload(criteria, data);
            log.info("[Orders-Report-DAY] criteria={}, rows={}", criteria, data.size());
            return pythonPdfClient.requestOrdersReport(payload);

        } else {
            // ===== 월별 집계 =====
            CursorPage<OrderMonthlyRowDto> page = analyticsService.getOrderMonthlyRows(storeId, cond);
            List<OrderMonthlyRowDto> rows = page.items();

            Map<String, Object> criteria = new LinkedHashMap<>();
            criteria.put("storeId", storeId);
            criteria.put("storeName", storeName);
            criteria.put("startDate", startDate.toString());
            criteria.put("endDate", endDate.toString());
            criteria.put("viewBy", "MONTH");

            List<Map<String, Object>> data = rows.stream()
                    .map(r -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("date", r.yearMonth());                        // "YYYY-MM"
                        m.put("orderCount", safeInt(r.orderCount()));        // 주문수
                        m.put("totalSales", safeNumber(r.totalSales()));     // 총매출
                        m.put("avgOrderAmount", safeNumber(r.avgOrderAmount())); // 평균주문금액
                        m.put("deliverySales", safeNumber(r.deliverySales()));   // 배달매출
                        m.put("takeoutSales", safeNumber(r.takeoutSales()));     // 포장매출
                        m.put("visitSales", safeNumber(r.visitSales()));         // 매장매출

                        return m;
                    })
                    .toList();

            OrdersPdfPayload payload = new OrdersPdfPayload(criteria, data);
            log.info("[Orders-Report-MONTH] criteria={}, rows={}", criteria, data.size());
            return pythonPdfClient.requestOrdersReport(payload);
        }
    }

    // =========================================
    // 메뉴 분석 리포트
    // =========================================

    /**
     * 메뉴 분석 리포트(PDF) 생성.
     *
     * @param storeId 점포 ID.
     * @param startDate 조회 시작일.
     * @param endDate 조회 종료일.
     * @param viewBy DAY 또는 MONTH.
     * @return 생성된 PDF 바이트 배열.
     * @throws RuntimeException 외부 서비스 통신 실패 시 발생할 수 있습니다.
     */
    public byte[] generateMenuReport(Long storeId,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     AnalyticsSearchDto.ViewBy viewBy) {

        String storeName = resolveStoreName(storeId);

        AnalyticsSearchDto cond = new AnalyticsSearchDto(
                startDate,
                endDate,
                viewBy,
                500,
                null
        );

        if (viewBy == AnalyticsSearchDto.ViewBy.DAY) {
            CursorPage<MenuDailyRowDto> page = analyticsService.getMenuDailyRows(storeId, cond);
            List<MenuDailyRowDto> rows = page.items();

            Map<String, Object> criteria = new LinkedHashMap<>();
            criteria.put("storeId", storeId);
            criteria.put("storeName", storeName);
            criteria.put("startDate", startDate.toString());
            criteria.put("endDate", endDate.toString());
            criteria.put("viewBy", "DAY");

            List<Map<String, Object>> data = rows.stream()
                    .map(r -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        // Python MenuRow 기준
                        m.put("date", r.orderDate().toString());
                        m.put("storeName", storeName);
                        m.put("category", r.categoryName());
                        m.put("menu", r.menuName());
                        m.put("quantity", safeInt(r.quantity()));
                        m.put("sales", safeNumber(r.sales()));
                        m.put("orderCount", safeInt(r.orderCount()));
                        return m;
                    })
                    .toList();

            MenuPdfPayload payload = new MenuPdfPayload(criteria, data);
            log.info("[Menu-Report-DAY] criteria={}, rows={}", criteria, data.size());
            return pythonPdfClient.requestMenusReport(payload);

        } else {
            CursorPage<MenuMonthlyRowDto> page = analyticsService.getMenuMonthlyRows(storeId, cond);
            List<MenuMonthlyRowDto> rows = page.items();

            Map<String, Object> criteria = new LinkedHashMap<>();
            criteria.put("storeId", storeId);
            criteria.put("storeName", storeName);
            criteria.put("startDate", startDate.toString());
            criteria.put("endDate", endDate.toString());
            criteria.put("viewBy", "MONTH");

            List<Map<String, Object>> data = rows.stream()
                    .map(r -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("date", r.yearMonth()); // "YYYY-MM"
                        m.put("storeName", storeName);
                        m.put("category", r.categoryName());
                        m.put("menu", r.menuName());
                        m.put("quantity", safeInt(r.quantity()));
                        m.put("sales", safeNumber(r.sales()));
                        m.put("orderCount", safeInt(r.orderCount()));
                        return m;
                    })
                    .toList();

            MenuPdfPayload payload = new MenuPdfPayload(criteria, data);
            log.info("[Menu-Report-MONTH] criteria={}, rows={}", criteria, data.size());
            return pythonPdfClient.requestMenusReport(payload);
        }
    }

    /**
     * 시간/요일 분석 리포트(PDF) 생성.
     *
     * <p>상단 요약, 차트, 테이블(일/월)을 조합하여 TimeDayReportPayload를 구성하고 Python 서비스로 전송합니다.</p>
     *
     * @param storeId 점포 ID.
     * @param startDate 조회 시작일.
     * @param endDate 조회 종료일.
     * @param viewBy DAY 또는 MONTH.
     * @return 생성된 PDF 바이트 배열.
     * @throws RuntimeException 외부 서비스 예외 발생 시 전파됩니다.
     */
    public byte[] generateTimeDayReport(
            Long storeId,
            LocalDate startDate,
            LocalDate endDate,
            AnalyticsSearchDto.ViewBy viewBy
    ) {
        // 1) 상단 요약 / 차트
        TimeDaySummaryDto summary = analyticsService.getTimeDaySummary(storeId);

        List<TimeHourlyPointDto> hourlyPoints =
                analyticsService.getTimeDayHourlyChart(storeId, startDate, endDate);
        List<WeekdaySalesPointDto> weekdayPoints =
                analyticsService.getWeekdayChart(storeId, startDate, endDate);

        // 2) 테이블 데이터 (일별 or 월별)
        List<TimeDayDailyRowDto> dailyRows = List.of();
        List<TimeDayMonthlyRowDto> monthlyRows = List.of();

        if (viewBy == AnalyticsSearchDto.ViewBy.DAY) {
            AnalyticsSearchDto cond = new AnalyticsSearchDto(
                    startDate, endDate,
                    AnalyticsSearchDto.ViewBy.DAY,
                    500,
                    null
            );
            CursorPage<TimeDayDailyRowDto> page = analyticsService.getTimeDayDailyRows(storeId, cond);
            dailyRows = page.items();
        } else {
            AnalyticsSearchDto cond = new AnalyticsSearchDto(
                    startDate, endDate,
                    AnalyticsSearchDto.ViewBy.MONTH,
                    500,
                    null
            );
            CursorPage<TimeDayMonthlyRowDto> page = analyticsService.getTimeDayMonthlyRows(storeId, cond);
            monthlyRows = page.items();
        }

        // ✅ StoreService로 점포명 조회
        String storeName = resolveStoreName(storeId);
        String periodLabel = startDate + " ~ " + endDate;
        String generatedAt = LocalDateTime.now(KST).toString();

        TimeDayReportPayload payload = new TimeDayReportPayload(
                storeId,
                storeName,
                periodLabel,
                summary,
                hourlyPoints,
                weekdayPoints,
                viewBy.name(),   // "DAY" / "MONTH"
                dailyRows,
                monthlyRows,
                generatedAt
        );

        log.info("[TimeDay-Report] storeId={}, viewBy={}, rowsDaily={}, rowsMonthly={}",
                storeId, viewBy, dailyRows.size(), monthlyRows.size());

        return pythonPdfClient.requestTimeDayReport(payload);
    }

    /**
     * 재료 분석 리포트(PDF) 생성.
     *
     * @param storeId 점포 ID.
     * @param startDate 조회 시작일.
     * @param endDate 조회 종료일.
     * @param viewBy DAY 또는 MONTH.
     * @return 생성된 PDF 바이트 배열.
     * @throws RuntimeException 외부 서비스 예외 발생 시 전파됩니다.
     */
    public byte[] generateMaterialReport(
            Long storeId,
            LocalDate startDate,
            LocalDate endDate,
            AnalyticsSearchDto.ViewBy viewBy
    ) {
        // 1) 상단 요약
        MaterialSummaryDto summary = analyticsService.getMaterialSummary(storeId);

        // 2) 테이블 데이터 (일별 or 월별)
        List<MaterialDailyRowDto> dailyRows = List.of();
        List<MaterialMonthlyRowDto> monthlyRows = List.of();

        if (viewBy == AnalyticsSearchDto.ViewBy.DAY) {
            AnalyticsSearchDto cond = new AnalyticsSearchDto(
                    startDate, endDate,
                    AnalyticsSearchDto.ViewBy.DAY,
                    500,
                    null
            );
            CursorPage<MaterialDailyRowDto> page = analyticsService.getMaterialDailyRows(storeId, cond);
            dailyRows = page.items();
        } else {
            AnalyticsSearchDto cond = new AnalyticsSearchDto(
                    startDate, endDate,
                    AnalyticsSearchDto.ViewBy.MONTH,
                    500,
                    null
            );
            CursorPage<MaterialMonthlyRowDto> page = analyticsService.getMaterialMonthlyRows(storeId, cond);
            monthlyRows = page.items();
        }

        // 3) 점포명 조회
        String storeName = resolveStoreName(storeId);
        String periodLabel = startDate + " ~ " + endDate;
        String generatedAt = LocalDateTime.now(KST).toString();

        // 4) Payload 구성
        MaterialReportPayload payload = new MaterialReportPayload(
                storeId,
                storeName,
                periodLabel,
                summary,
                viewBy.name(),   // "DAY" / "MONTH"
                dailyRows,
                monthlyRows,
                generatedAt
        );

        log.info("[Material-Report] storeId={}, viewBy={}, rowsDaily={}, rowsMonthly={}",
                storeId, viewBy, dailyRows.size(), monthlyRows.size());

        return pythonPdfClient.requestMaterialReport(payload);
    }

    // =========================================
    // null 방어용 helper
    // =========================================

    /**
     * Number를 Double로 안전하게 변환합니다. null이면 0.0 반환.
     *
     * @param n 변환 대상 Number.
     * @return Double 값 또는 0.0.
     */
    private Double safeNumber(Number n) {
        return n == null ? 0.0 : n.doubleValue();
    }

    /**
     * Number를 Integer로 안전하게 변환합니다. null이면 0 반환.
     *
     * @param n 변환 대상 Number.
     * @return Integer 값 또는 0.
     */
    private Integer safeInt(Number n) {
        return n == null ? 0 : n.intValue();
    }

    /**
     * Store ID로 점포명을 조회하고, 없으면 fallback 문자열을 반환합니다.
     *
     * @param storeId 점포 ID.
     * @return 점포명 또는 "Store-{id}" 형태의 fallback.
     */
    private String resolveStoreName(Long storeId) {
        Store store = storeService.findById(storeId);
        if (store == null) {
            // 혹시 잘못된 ID로 들어온 경우 대비
            log.warn("Store not found for id={}, fallback name 사용", storeId);
            return "Store-" + storeId;
        }
        return store.getName();   // Store 엔티티 필드명이 name 이라서 이렇게
    }

}
