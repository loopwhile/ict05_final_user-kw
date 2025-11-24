package com.boot.ict05_final_user.domain.home.service;

import com.boot.ict05_final_user.domain.home.dto.*;
import com.boot.ict05_final_user.domain.home.repository.HomeRepositoryCustom;
import com.boot.ict05_final_user.domain.order.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 대시보드(Home) 화면용 통계 집계 서비스.
 *
 * 역할
 * - 오늘 기준 KPI 카드(매출/주문/방문)와 전일 대비 증감률 계산
 * - 오늘의 TOP 메뉴 목록 (수량/매출 정렬)
 * - 오늘 시간대별 매출/주문/채널별 주문수 집계
 *
 * 설계 포인트
 * - 모든 질의는 리포지토리 커스텀(HomeRepositoryCustom)으로 위임하여
 *   서비스 계층은 기간 계산, 포맷팅, UI 친화적 문구 조립에 집중한다.
 * - 기간 범위는 [startOfDay, nextStartOfDay) 형태의 반개구간을 사용한다.
 * - 완료 주문 상태 집합(DONE)을 상수로 유지하여 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private final HomeRepositoryCustom homeRepositoryCustom;

    /** 일자의 시작 시각(00:00:00) */
    private static LocalDateTime s(LocalDate d) { return d.atStartOfDay(); }

    /** 다음 날의 시작 시각(해당 일자 범위의 배타적 상한) */
    private static LocalDateTime e(LocalDate d) { return d.plusDays(1).atStartOfDay(); }

    /** "완료"로 간주하는 주문 상태 모음 (분석 집계 대상 필터) */
    private static final List<OrderStatus> DONE = List.of(OrderStatus.PAID, OrderStatus.COMPLETED);

    /**
     * 오늘 KPI 카드(매출/주문/방문, TOP 메뉴) 조회.
     *
     * 범위
     * - 오늘 00:00:00 ~ 내일 00:00:00 미만
     * - 전일 대비 증감률 계산을 위해 어제 동일 범위도 함께 질의
     *
     * 포맷팅
     * - 매출 표시는 한국 원화 포맷, 1만원 이상은 "₩n만" 축약
     * - 증감률은 "어제 대비 ±x.x%" 형태 문자열
     */
    public KpiCardsResponseDTO getTodayKpis(Long storeId) {
        LocalDate today = LocalDate.now();
        LocalDateTime ts = s(today), te = e(today);
        LocalDateTime ys = s(today.minusDays(1)), ye = e(today.minusDays(1));

        // 오늘 집계
        BigDecimal sales = homeRepositoryCustom.sumSales(ts, te, storeId, DONE);
        long orders = homeRepositoryCustom.countOrders(ts, te, storeId, DONE);
        long visitors = homeRepositoryCustom.countVisitOrders(ts, te, storeId, DONE);

        // 전일 집계 (증감률 비교용)
        BigDecimal salesPrev = homeRepositoryCustom.sumSales(ys, ye, storeId, DONE);
        long ordersPrev = homeRepositoryCustom.countOrders(ys, ye, storeId, DONE);
        long visitorsPrev = homeRepositoryCustom.countVisitOrders(ys, ye, storeId, DONE);

        // 카드 조립
        var cards = List.of(
                KpiCardDTO.builder()
                        .key("sales_today")
                        .value(formatWon(sales))
                        .change(diffPctStr(sales, salesPrev))
                        // 주의: salesPrev가 null일 수 있으면 changeType에서 NPE가 날 수 있다.
                        // 리포지토리에서 0 반환을 보장하지 않으면 아래 유틸에 null 가드 추가 권장.
                        .changeType(changeType(sales, salesPrev))
                        .build(),
                KpiCardDTO.builder()
                        .key("orders_today")
                        .value(orders + "건")
                        .change(diffPctStr(orders, ordersPrev))
                        .changeType(changeType(orders, ordersPrev))
                        .build(),
                KpiCardDTO.builder()
                        .key("visitors_today")
                        .value(visitors + "건")
                        .change(diffPctStr(visitors, visitorsPrev))
                        .changeType(changeType(visitors, visitorsPrev))
                        .build(),
                KpiCardDTO.builder()
                        .key("top_menu")
                        .value(findTopMenuName(ts, te, storeId)) // 상단 텍스트: 메뉴명 또는 "데이터 없음"
                        .change(findTopMenuQty(ts, te, storeId)) // 보조 텍스트: "n개 판매"
                        .changeType(ChangeType.NEUTRAL)          // 의미상 증감 아님
                        .build()
        );

        return KpiCardsResponseDTO.builder()
                .date(LocalDateTime.now())
                .storeId(storeId)
                .cards(cards)
                .build();
    }

    /**
     * 오늘의 TOP 메뉴 목록 조회.
     *
     * 정렬/한도
     * - 수량/매출 기준으로 커스텀 리포지토리에서 상위 N개를 가져온다.
     * - 카테고리/이름 기반 이모지 매핑으로 간단한 썸네일 대용을 제공한다.
     */
    public TopMenusResponseDTO getTopMenus(Long storeId, int limit) {
        LocalDate today = LocalDate.now();
        LocalDateTime ts = s(today), te = e(today);

        var rows = homeRepositoryCustom.findTopMenus(ts, te, storeId, limit, DONE);
        var items = rows.stream()
                .map(r -> TopMenuItemDTO.builder()
                        .menuId(r.menuId())
                        .name(r.name())
                        .quantity((int) r.qty())
                        .sales(r.sales())
                        .image(pickEmojiByCategoryThenName(r.categoryName(), r.name()))
                        .build())
                .toList();

        return TopMenusResponseDTO.builder()
                .date(LocalDateTime.now())
                .periodStart(ts)
                .periodEnd(te.minusSeconds(1))
                .storeId(storeId)
                .limit(limit)
                .items(items)
                .build();
    }

    /** 카테고리 우선, 없으면 이름 규칙으로 이모지 선택 */
    private static String pickEmojiByCategoryThenName(String categoryName, String menuName) {
        String e = mapCategoryEmoji(categoryName);
        return e != null ? e : mapNameEmoji(menuName);
    }

    /** 카테고리명 → 대표 이모지 매핑 (데이터베이스 내 카테고리 스냅샷 기준) */
    private static String mapCategoryEmoji(String categoryName) {
        if (categoryName == null) return null;
        String n = categoryName.trim();
        // 카테고리 예시: 메뉴, 세트메뉴, 단품메뉴, 토스트, 사이드, 음료, 토스트세트, 커피, 시즌한정
        return switch (n) {
            case "토스트", "토스트세트" -> "🍞";
            case "사이드" -> "🍟";
            case "음료" -> "🥤";
            case "커피" -> "☕";
            case "세트메뉴" -> "🍱";
            case "단품메뉴" -> "🍽️";
            case "시즌한정" -> "✨";
            // 상위/루트 등 애매하면 매핑하지 않음
            case "메뉴" -> null;
            default -> null;
        };
    }

    /** 메뉴명 키워드 기반 이모지 매핑(보조 규칙) */
    private static String mapNameEmoji(String name) {
        if (name == null || name.isBlank()) return null;
        String n = name.toLowerCase();
        if (n.contains("토스트") || n.contains("toast")) return "🍞";
        if (n.contains("버거") || n.contains("burger")) return "🍔";
        if (n.contains("치킨") || n.contains("chicken")) return "🍗";
        if (n.contains("감자튀김") || n.contains("감튀") || n.contains("fries")) return "🍟";
        if (n.contains("피자") || n.contains("pizza")) return "🍕";
        if (n.contains("핫도그") || n.contains("hot dog")) return "🌭";
        if (n.contains("샌드") || n.contains("sandwich") || n.contains("파니니") || n.contains("panini")) return "🥪";
        if (n.contains("치즈")) return "🧀";
        if (n.contains("콜라") || n.contains("coke") || n.contains("사이다") || n.contains("sprite") || n.contains("soda")) return "🥤";
        if (n.contains("커피") || n.contains("라떼") || n.contains("latte") || n.contains("espresso") || n.contains("americano")) return "☕";
        return null;
    }

    /**
     * 오늘 시간대별 통계(매출/주문/채널별 주문수) 조회.
     *
     * 반환
     * - 00시부터 23시까지 존재하는 시간 슬롯만 반환(데이터 없는 시간은 리포지토리 구현에 따름)
     * - UI 차트용 문자열 포맷("HH:00")로 시간 라벨을 구성한다.
     */
    public HourlyStatsResponseDTO getTodayHourly(Long storeId) {
        LocalDate today = LocalDate.now();
        LocalDateTime ts = s(today), te = e(today);

        var rows = homeRepositoryCustom.aggregateHourly(ts, te, storeId, DONE);
        var items = rows.stream()
                .map(r -> HourlyStatDTO.builder()
                        .time(String.format("%02d:00", r.hour()))
                        .sales(r.sales())
                        .orders((int) r.orders())
                        .visitOrders((int) r.visitOrders())
                        .takeoutOrders((int) r.takeoutOrders())
                        .deliveryOrders((int) r.deliveryOrders())
                        .build())
                .toList();

        return HourlyStatsResponseDTO.builder()
                .date(today)
                .storeId(storeId)
                .items(items)
                .build();
    }

    // ===== 포맷/증감률/변화유형 유틸 =====

    /** 원화 포맷. 1만원 이상은 "₩n만" 축약 표시 */
    private static String formatWon(BigDecimal n) {
        long v = n.longValue();
        if (v >= 10000) { // 만원 단위 축약
            return "₩" + (v / 10000) + "만";
        }
        return NumberFormat.getCurrencyInstance(Locale.KOREA).format(v);
    }

    /** 전일 대비 증감률 문자열(정수 비교). 분모 0 이하면 +100%로 간주 */
    private static String diffPctStr(long a, long b) {
        if (b <= 0) return "어제 대비 +100%";
        double pct = (a - b) * 100.0 / b;
        return "어제 대비 " + (pct >= 0 ? "+" : "") + String.format(Locale.KOREA, "%.1f", pct) + "%";
    }

    /** 전일 대비 증감률 문자열(금액 비교). 분모 null 또는 0 이하면 +100%로 간주 */
    private static String diffPctStr(BigDecimal a, BigDecimal b) {
        if (b == null || b.signum() <= 0) return "어제 대비 +100%";
        double pct = a.subtract(b).doubleValue() * 100.0 / b.doubleValue();
        return "어제 대비 " + (pct >= 0 ? "+" : "") + String.format(Locale.KOREA, "%.1f", pct) + "%";
    }

    /** 변화 유형(증가/감소/보합) 판정: 정수 비교 */
    private static ChangeType changeType(long a, long b) {
        if (a > b) return ChangeType.INCREASE;
        if (a < b) return ChangeType.DECREASE;
        return ChangeType.NEUTRAL;
    }

    /**
     * 변화 유형(증가/감소/보합) 판정: 금액 비교.
     * 주의: b가 null이면 NPE가 발생하므로, 호출 전 0 보정 또는 null 가드 필요.
     * (리포지토리에서 0 반환 보장 또는 여기서 Optional 처리로 보강 가능)
     */
    private static ChangeType changeType(BigDecimal a, BigDecimal b) {
        int cmp = a.compareTo(b);
        if (cmp > 0) return ChangeType.INCREASE;
        if (cmp < 0) return ChangeType.DECREASE;
        return ChangeType.NEUTRAL;
    }

    // ===== 내부 조회 헬퍼 =====

    /** 오늘 TOP 1 메뉴명 조회. 없으면 "데이터 없음" */
    private String findTopMenuName(LocalDateTime ts, LocalDateTime te, Long storeId) {
        var rows = homeRepositoryCustom.findTopMenus(ts, te, storeId, 1, DONE);
        return rows.isEmpty() ? "데이터 없음" : rows.get(0).name();
        // 필요 시 이미지/URL 확장 가능
    }

    /** 오늘 TOP 1 메뉴 수량 문자열("{n}개 판매") */
    private String findTopMenuQty(LocalDateTime ts, LocalDateTime te, Long storeId) {
        var rows = homeRepositoryCustom.findTopMenus(ts, te, storeId, 1, DONE);
        if (rows.isEmpty()) return null;
        return rows.get(0).qty() + "개 판매";
    }
}
