package com.boot.ict05_final_user.domain.analytics.repository;

import com.boot.ict05_final_user.domain.analytics.dto.*;
import com.boot.ict05_final_user.domain.analytics.dto.AnalyticsSearchDto.ViewBy;
import com.boot.ict05_final_user.domain.inventory.entity.*;
import com.boot.ict05_final_user.domain.menu.entity.QMenu;
import com.boot.ict05_final_user.domain.menu.entity.QMenuCategory;
import com.boot.ict05_final_user.domain.menu.entity.QMenuUsageMaterialLog;
import com.boot.ict05_final_user.domain.order.entity.*;
import com.boot.ict05_final_user.domain.store.entity.QStore;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Analytics 도메인 전용 커스텀 리포지토리 구현체.
 *
 * <p><b>역할</b>:
 * <ul>
 *   <li>KPI, 주문/메뉴/시간·요일/재료 분석에 필요한 집계 쿼리 제공</li>
 *   <li>커서 기반 페이징(문자열 커서 또는 ID 커서)과 PDF 페이로드 전용 조회 지원</li>
 * </ul>
 * </p>
 *
 * <p><b>경계</b>:
 * <ul>
 *   <li>입력: 서비스에서 KST 기준 {@link LocalDate}·{@link LocalDateTime}가 전달된다고 가정</li>
 *   <li>상태 필터: 기본적으로 {@code OrderStatus.COMPLETED}만 집계</li>
 *   <li>점포 스코프: 모든 메서드는 단일 {@code storeId} 기준</li>
 * </ul>
 * </p>
 *
 * <p><b>성능/인덱스</b>:
 * <ul>
 *   <li>핵심 인덱스 권장: {@code customer_order(store_id, status, ordered_at)},
 *       {@code customer_order_detail(order_id)},
 *       {@code menu_usage_material_log(order_id, store_material_id)},
 *       {@code store_inventory_batch(store_id, expiration_date)}</li>
 *   <li>모든 메인 조회는 {@code readOnly}, {@code flushMode=COMMIT}, 타임아웃 힌트를 사용</li>
 *   <li>가능한 한 단일 스캔 + GROUP BY로 계산(파생 KPI는 Java에서)</li>
 * </ul>
 * </p>
 *
 * <p><b>시간대</b>: 날짜 경계는 서비스에서 Asia/Seoul(KST)로 정규화하여 전달하며,
 * 본 구현은 {@code [start 00:00, end+1 00:00)}(닫힌–열린) 규칙을 따른다.</p>
 *
 * <p><b>커서 규칙</b>:
 * <ul>
 *   <li>KPI: {@code "YYYY-MM-DD"} 또는 {@code "YYYY-MM"}</li>
 *   <li>주문 일별: 마지막 주문 ID(Long)</li>
 *   <li>메뉴 일별/월별: {@code "YYYY-MM-DD|menuId"}, {@code "YYYY-MM|menuId"}</li>
 *   <li>시간·요일 일별/월별: {@code "YYYY-MM-DD|HH"}, {@code "YYYY-MM|weekday|hour"}</li>
 * </ul>
 * </p>
 *
 * <p><b>트랜잭션</b>: 모든 조회는 {@code @Transactional(readOnly = true)}. 변경 작업 없음.</p>
 *
 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
 */
@RequiredArgsConstructor
@Repository
public class AnalyticsRepositoryImpl implements AnalyticsRespositoryCustom {

	/** QueryDSL 엔진. 스레드-세이프하게 싱글턴 주입 사용. */
	private final JPAQueryFactory query;

	// =========================
	//         Q-Types
	// =========================
	/** 주문(헤더): 상태/점포/주문시각 필터의 메인 소스. */
	private final QCustomerOrder co = QCustomerOrder.customerOrder;
	/** 주문상세(라인): 수량/라인금액 집계 시 조인. */
	private final QCustomerOrderDetail cod = QCustomerOrderDetail.customerOrderDetail;
	/** 점포: 모든 조회는 단일 store 스코프. */
	private final QStore s = QStore.store;
	/** 메뉴/카테고리: 메뉴/카테고리 단위 집계에 사용. */
	private final QMenu m = QMenu.menu;
	private final QMenuCategory mc = QMenuCategory.menuCategory;
	/** 점포-재료(마스터): 단가/단위/환산비율 기준. */
	private final QStoreMaterial sm = QStoreMaterial.storeMaterial;
	/** 메뉴-재료 사용 로그: 재료 사용량/원가 계산의 메인 소스. */
	private final QMenuUsageMaterialLog log = QMenuUsageMaterialLog.menuUsageMaterialLog;
	/** 공통 재료(옵셔널): 점포-재료명 누락 시 대체 표시용. */
	private final QMaterial material = QMaterial.material;
	/** 점포 재고 배치: 유통기한 임박/최근 입고일 계산. */
	private final QStoreInventoryBatch batch = QStoreInventoryBatch.storeInventoryBatch;
	/** 점포 재고: 재고 부족 상태 계산. */
	private final QStoreInventory inv = QStoreInventory.storeInventory;

	// 유통기한 임박 기준 (일 단위)
	// 실제 FCM 스캐너 설정과 맞추고 싶으면 설정값 주입으로 교체하면 됨.
	private static final int EXPIRE_SOON_DAYS = 3;

	@Override
	@Transactional(readOnly = true)
	/**
	 * KPI 요약 카드(MTD + WoW%)를 집계하여 반환한다.
	 *
	 * <p>
	 * 기준 시각은 KST {@code today 00:00}이며, MTD 구간은
	 * {@code [thisMonth-01 00:00, today 00:00)}로 해석되어 "이번달 1일 ~ 어제"를 포함한다.
	 * WoW% 계산을 위해 최근 7일(L7: {@code [D-6, D]})과 그 이전 7일(P7: {@code [D-13, D-7]})
	 * 구간을 함께 스캔한다. (여기서 D = {@code today-1})
	 * </p>
	 *
	 * <ul>
	 *   <li>Sales_MTD: MTD 매출 합계(₩).</li>
	 *   <li>Tx_MTD: MTD 주문수(건).</li>
	 *   <li>Units_MTD: MTD 판매수량 합계.</li>
	 *   <li>UPT = {@code Units_MTD / Tx_MTD}.</li>
	 *   <li>ADS = {@code Sales_MTD / Tx_MTD} (객단가, 반올림).</li>
	 *   <li>AUR = {@code Sales_MTD / Units_MTD} (단가, 반올림).</li>
	 *   <li>WoW% = {@code (L7 - P7) / P7 * 100} (P7=0이면 null).</li>
	 * </ul>
	 *
	 * @param storeId 대상 점포 ID.
	 * @param today   조회 기준일(KST, {@code LocalDate}).
	 * @return KPI 요약 DTO.
 */
	public KpiSummaryDto fetchKpiSummary(Long storeId, LocalDate today) {

		// 기준 시간 (KST 기준 LocalDate 들어온다고 가정)
		LocalDateTime todayStart = today.atStartOfDay();
		LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

		// 어제 D, 최근7일/이전7일
		LocalDate d = today.minusDays(1);                  // 어제
		LocalDateTime l7Start = d.minusDays(6).atStartOfDay(); // [D-6, D+1)
		LocalDateTime l7EndEx = todayStart;
		LocalDateTime p7Start = d.minusDays(13).atStartOfDay(); // [D-13, D-6)
		LocalDateTime p7EndEx = l7Start;

		// 스캔 범위: MTD와 P7/L7 전체를 모두 포함하도록 min(monthStart, p7Start) ~ todayStart
		LocalDateTime scanStart = monthStart.isBefore(p7Start) ? monthStart : p7Start;

		// 공통 WHERE: 상태 + 점포 + 스캔 범위
		BooleanExpression base = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, scanStart, todayStart));

		// co.totalPrice(BigDecimal) 기반 CASE 합계들
		NumberExpression<BigDecimal> salesMtdExpr = new CaseBuilder()
				.when(betweenClosedOpen(co.orderedAt, monthStart, todayStart))
				.then(co.totalPrice)
				.otherwise(Expressions.constant(BigDecimal.ZERO))
				.sum();

		NumberExpression<Long> txMtdExpr = new CaseBuilder()
				.when(betweenClosedOpen(co.orderedAt, monthStart, todayStart))
				.then(1L).otherwise(0L).sum();

		NumberExpression<BigDecimal> salesL7Expr = new CaseBuilder()
				.when(betweenClosedOpen(co.orderedAt, l7Start, l7EndEx))
				.then(co.totalPrice)
				.otherwise(Expressions.constant(BigDecimal.ZERO))
				.sum();

		NumberExpression<BigDecimal> salesP7Expr = new CaseBuilder()
				.when(betweenClosedOpen(co.orderedAt, p7Start, p7EndEx))
				.then(co.totalPrice)
				.otherwise(Expressions.constant(BigDecimal.ZERO))
				.sum();

		Tuple t = query
				.select(salesMtdExpr, txMtdExpr, salesL7Expr, salesP7Expr)
				.from(co)
				.join(co.store, s)
				.where(base)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetchOne();

		BigDecimal salesMtdBD = nvlBD(t == null ? null : t.get(salesMtdExpr));
		long txMtd            = nvlLong(t == null ? null : t.get(txMtdExpr));
		BigDecimal salesL7BD  = nvlBD(t == null ? null : t.get(salesL7Expr));
		BigDecimal salesP7BD  = nvlBD(t == null ? null : t.get(salesP7Expr));

		// Units_MTD (상세 테이블 cod 기준 별도 스캔)
		Integer unitsMtdInt = query
				.select(cod.quantity.sum())
				.from(cod)
				.join(co).on(cod.order.id.eq(co.id))
				.join(co.store, s)
				.where(
						statusCompleted(),
						eqStore(storeId),
						betweenClosedOpen(co.orderedAt, monthStart, todayStart)
				)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetchOne();

		long unitsMtd = (unitsMtdInt == null) ? 0L : unitsMtdInt.longValue();

		// 파생 계산(Java)
		long salesMtd = salesMtdBD.longValue();
		long salesL7  = salesL7BD.longValue();
		long salesP7  = salesP7BD.longValue();

		double upt = safeDiv(unitsMtd, txMtd);         // UPT = units / tx
		long ads   = Math.round(safeDiv(salesMtd, txMtd));   // ADS(객단가)
		long aur   = Math.round(safeDiv(salesMtd, unitsMtd)); // AUR(단가)

		Double wow = (salesP7 == 0L)
				? null
				: round1(((salesL7 - salesP7) * 100.0) / salesP7);

		return new KpiSummaryDto(salesMtd, txMtd, unitsMtd, upt, ads, aur, wow);
	}

	@Override
	@Transactional(readOnly = true)
	/**
	 * KPI 테이블(일별/월별)을 커서 기반으로 페이지 조회한다.
	 *
	 * <p>
	 * 기간은 {@code [start, end]} 모두 포함으로 해석되며,
	 * 내부적으로 {@code [start 00:00, end+1 00:00)}의 열린-닫힘 구간으로 변환한다.
	 * 라벨은 일별은 {@code YYYY-MM-DD}, 월별은 {@code YYYY-MM}이며
	 * 내림차순(최근 → 과거) 정렬 기준으로 커서 비교에 사용한다.
	 * </p>
	 *
	 * <p><b>커서 규칙</b></p>
	 * <ul>
	 *   <li>요청 커서가 존재하면 {@code label &lt; cursor} 조건으로 이후(과거) 페이지를 조회한다.</li>
	 *   <li>응답의 {@code nextCursor}는 현재 페이지의 마지막 라벨(문자열)이다.</li>
	 *   <li>라벨 포맷 특성상 문자열 비교가 시간 역순과 일치한다.</li>
	 * </ul>
	 *
	 * <p><b>집계 규칙</b></p>
	 * <ul>
	 *   <li>매출/주문수: 주문 헤더(co) 기준 집계(중복 합계 방지).</li>
	 *   <li>판매수량(units): 주문 상세(cod) 기준 집계 후 라벨별 매핑.</li>
	 *   <li>파생지표: {@code UPT=units/tx}, {@code ADS=sales/tx}, {@code AUR=sales/units}.</li>
	 * </ul>
	 *
	 * @param storeId 점포 ID.
	 * @param cond    조회 조건(시작일/종료일, {@code viewBy=DAY|MONTH}, {@code size}, {@code cursor}).
	 * @return 커서 페이지(아이템 리스트와 {@code nextCursor}).
	 */
	public CursorPage<KpiRowDto> fetchKpiRows(Long storeId, AnalyticsSearchDto cond) {
		boolean byMonth = cond.viewBy() == ViewBy.MONTH;
		int size = (cond.size() == null ? 50 : cond.size());

		// 기간 (열림-닫힘) : [start 00:00, end 00:00)
		LocalDateTime start = cond.startDate().atStartOfDay();
		LocalDateTime endEx = cond.endDate().plusDays(1).atStartOfDay();

		// 공통 WHERE
		BooleanExpression filter = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, start, endEx));

		// 라벨 (일별 or 월별)
		StringExpression dayLabel = Expressions.stringTemplate(
				"DATE_FORMAT({0}, {1})", co.orderedAt, ConstantImpl.create("%Y-%m-%d"));
		StringExpression monthLabel = Expressions.stringTemplate(
				"DATE_FORMAT({0}, {1})", co.orderedAt, ConstantImpl.create("%Y-%m"));
		StringExpression labelExpr = byMonth ? monthLabel : dayLabel;

		// 커서(최근순) - label 문자열 비교 (YYYY-MM[-DD] 포맷이므로 문자열 비교 = 날짜 역순)
		if (cond.cursor() != null && !cond.cursor().isBlank()) {
			filter = filter.and(labelExpr.lt(cond.cursor()));
		}

		// 1) 매출/주문수 기본 집계 (co만 스캔 → 중복 합계 방지)
		NumberExpression<BigDecimal> salesSum = co.totalPrice.sum();     // BigDecimal
		NumberExpression<Long> txCount = co.id.countDistinct();          // Long

		List<Tuple> rows = query
				.select(labelExpr, salesSum, txCount)
				.from(co)
				.join(co.store, s)
				.where(filter)
				.groupBy(labelExpr)
				.orderBy(labelExpr.desc())
				.limit(size + 1) // 다음 커서 유무 확인용으로 +1
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		List<KpiRowDto> items = new ArrayList<>();
		if (rows.isEmpty()) {
			return new CursorPage<>(items, null);
		}

		boolean hasNext = rows.size() > size;
		List<Tuple> pageRows = hasNext ? rows.subList(0, size) : rows;

		// 현재 페이지 라벨만 추출
		List<String> labels = new ArrayList<>(pageRows.size());
		for (Tuple t : pageRows) {
			labels.add(t.get(labelExpr));
		}

		// 2) 수량(units) 집계: cod 기준, label 기준으로 SUM(quantity)
		Map<String, Long> unitsMap = new HashMap<>();
		if (!labels.isEmpty()) {
			List<Tuple> unitRows = query
					.select(labelExpr, cod.quantity.sum())
					.from(cod)
					.join(cod.order, co)
					.join(co.store, s)
					.where(
							statusCompleted(),
							eqStore(storeId),
							betweenClosedOpen(co.orderedAt, start, endEx),
							labelExpr.in(labels)
					)
					.groupBy(labelExpr)
					.setHint("org.hibernate.readOnly", true)
					.setHint("org.hibernate.flushMode", "COMMIT")
					.setHint("jakarta.persistence.query.timeout", 3000)
					.fetch();

			for (Tuple t : unitRows) {
				String label = t.get(labelExpr);
				Integer unitsInt = t.get(1, Integer.class);
				long units = (unitsInt == null) ? 0L : unitsInt.longValue();
				unitsMap.put(label, units);
			}
		}

		// 3) DTO 변환 + 파생 KPI 계산
		for (Tuple t : pageRows) {
			String label = t.get(labelExpr);
			BigDecimal salesBD = nvlBD(t.get(salesSum));
			long sales = salesBD.longValue();
			long tx = nvlLong(t.get(txCount));
			long units = unitsMap.getOrDefault(label, 0L);

			double upt = safeDiv(units, tx);
			long ads = Math.round(safeDiv(sales, tx));    // 객단가
			long aur = Math.round(safeDiv(sales, units)); // 단가

			items.add(new KpiRowDto(label, sales, tx, upt, ads, aur));
		}

		String nextCursor = null;
		if (hasNext) {
			Tuple last = pageRows.get(pageRows.size() - 1);
			nextCursor = last.get(labelExpr); // YYYY-MM-DD or YYYY-MM
		}

		return new CursorPage<>(items, nextCursor);
	}


	@Override
	@Transactional(readOnly = true)
	/**
	 * 주문 분석 상단 요약(MTD)을 조회한다.
	 *
	 * <p>KST {@code today 00:00} 기준으로 이번 달 1일 00:00부터 오늘 00:00 직전까지
	 * ({@code [thisMonth-01 00:00, today 00:00)}) 구간의 데이터를 집계한다.
	 * 주문 상태는 COMPLETED만 포함한다.</p>
	 *
	 * <ul>
	 *   <li>배달/포장/매장 매출(₩) 합계</li>
	 *   <li>주문수(건) = 주문 헤더 ID 기준 countDistinct</li>
	 * </ul>
	 *
	 * @param storeId 점포 ID.
	 * @param today   조회 기준일(KST, {@code LocalDate}).
	 * @return 주문 요약 DTO(배달/포장/매장 매출과 주문수).
	 */
	public OrderSummaryDto fetchOrderSummary(Long storeId, LocalDate today) {

		LocalDateTime todayStart = today.atStartOfDay();
		LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

		BooleanExpression base = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, monthStart, todayStart)); // 이번달 1일 ~ 어제까지

		NumberExpression<BigDecimal> deliverySalesExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.DELIVERY))
				.then(co.totalPrice)
				.otherwise(Expressions.constant(BigDecimal.ZERO))
				.sum();

		NumberExpression<BigDecimal> takeoutSalesExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.TAKEOUT))
				.then(co.totalPrice)
				.otherwise(Expressions.constant(BigDecimal.ZERO))
				.sum();

		NumberExpression<BigDecimal> visitSalesExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.VISIT))
				.then(co.totalPrice)
				.otherwise(Expressions.constant(BigDecimal.ZERO))
				.sum();

		NumberExpression<Long> orderCountExpr = co.id.countDistinct();

		Tuple t = query
				.select(deliverySalesExpr, takeoutSalesExpr, visitSalesExpr, orderCountExpr)
				.from(co)
				.join(co.store, s)
				.where(base)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetchOne();

		BigDecimal deliveryBD = nvlBD(t == null ? null : t.get(deliverySalesExpr));
		BigDecimal takeoutBD  = nvlBD(t == null ? null : t.get(takeoutSalesExpr));
		BigDecimal visitBD    = nvlBD(t == null ? null : t.get(visitSalesExpr));
		long orderCount       = nvlLong(t == null ? null : t.get(orderCountExpr));

		return new OrderSummaryDto(
				deliveryBD.longValue(),
				takeoutBD.longValue(),
				visitBD.longValue(),
				orderCount
		);
	}


	@Override
	@Transactional(readOnly = true)
	/**
	 * 주문 분석 일별 테이블(주문 1건 = 1 row)을 커서 기반으로 페이지 조회한다.
	 *
	 * <p>기간은 {@code [start, end]} 모두 포함으로 해석하며 내부적으로
	 * {@code [start 00:00, end+1 00:00)}로 변환한다.
	 * COMPLETED 주문만 대상이며, 메뉴 수량은 주문상세(cod) 합계를 사용한다.</p>
	 *
	 * <p><b>정렬/커서 규칙</b></p>
	 * <ul>
	 *   <li>정렬: {@code orderedAt DESC, id DESC} (최신 주문 우선).</li>
	 *   <li>커서: 마지막 주문 ID(Long) 기반, 요청 시 {@code id &lt; cursorId} 조건으로 다음 페이지 조회.</li>
	 *   <li>{@code nextCursor}: 현재 페이지의 마지막 주문 ID(문자열).</li>
	 * </ul>
	 *
	 * @param storeId 점포 ID.
	 * @param cond    조회 조건(시작일/종료일, size, cursor).
	 * @return 커서 페이지(일별 주문행 리스트와 {@code nextCursor}).
	 */
	public CursorPage<OrderDailyRowDto> fetchOrderDailyRows(Long storeId, AnalyticsSearchDto cond) {
		int size = (cond.size() == null ? 50 : cond.size());

		// [start 00:00, end+1 00:00)
		LocalDateTime start = cond.startDate().atStartOfDay();
		LocalDateTime endEx = cond.endDate().plusDays(1).atStartOfDay();

		BooleanExpression filter = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, start, endEx));

		// 🔹 커서: "마지막 주문 ID" 기준으로만 사용
		if (cond.cursor() != null && !cond.cursor().isBlank()) {
			try {
				Long lastId = Long.valueOf(cond.cursor());
				filter = filter.and(co.id.lt(lastId));
			} catch (NumberFormatException ignore) {
				// 잘못된 커서 값이면 그냥 무시하고 처음 페이지처럼 동작
			}
		}

		// 🔹 메뉴 수량 합계 (상세 테이블 기준)
		NumberExpression<Integer> menuCountExpr = cod.quantity.sum();

		List<Tuple> rows = query
				.select(
						co.orderedAt,
						co.id,
						co.orderCode,
						co.orderType,
						co.totalPrice,
						menuCountExpr,
						co.paymentType,
						co.memo
				)
				.from(co)
				.join(co.store, s)
				// ⭐ 주문 ↔ 주문상세 조인 (LEFT JOIN) 후 groupBy 집계
				.leftJoin(cod).on(cod.order.id.eq(co.id))
				.where(filter)
				.groupBy(
						co.orderedAt,
						co.id,
						co.orderCode,
						co.orderType,
						co.totalPrice,
						co.paymentType,
						co.memo
				)
				// 🔹 화면 정렬: 날짜 내림차순 + 같은 날은 ID 내림차순
				.orderBy(co.orderedAt.desc(), co.id.desc())
				.limit(size + 1)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		List<OrderDailyRowDto> items = new ArrayList<>();
		List<Tuple> pageRows = rows.size() > size ? rows.subList(0, size) : rows;

		for (Tuple t : pageRows) {
			LocalDateTime orderedAt = t.get(co.orderedAt);
			Long orderId            = t.get(co.id);
			String orderCode        = t.get(co.orderCode);
			OrderType orderType     = t.get(co.orderType);
			BigDecimal totalPriceBD = nvlBD(t.get(co.totalPrice));
			Integer menuCountInt    = t.get(menuCountExpr);
			PaymentType payType     = t.get(co.paymentType);
			String memo             = t.get(co.memo);

			String orderDate = orderedAt.toLocalDate().toString();
			long totalPrice  = totalPriceBD.longValue();
			long menuCount   = menuCountInt == null ? 0L : menuCountInt.longValue();

			items.add(new OrderDailyRowDto(
					orderDate,
					orderId,
					orderCode,
					orderType != null ? orderType.name() : null,
					totalPrice,
					menuCount,
					payType != null ? payType.name() : null,
					memo
			));
		}

		String nextCursor = null;
		if (rows.size() > size) {
			Tuple last = rows.get(size - 1);
			Long lastId = last.get(co.id);
			if (lastId != null) {
				nextCursor = String.valueOf(lastId); // 🔹 커서 = 마지막 주문 ID
			}
		}

		return new CursorPage<>(items, nextCursor);
	}


	@Override
	@Transactional(readOnly = true)
	/**
	 * 주문 분석 월별 테이블(월 단위 집계)을 커서 기반으로 페이지 조회한다.
	 *
	 * <p>기간은 {@code [start, end]} 모두 포함으로 해석하며 내부적으로
	 * {@code [start 00:00, end+1 00:00)}로 변환한다.
	 * 라벨은 {@code YYYY-MM}이며 내림차순(최근월 → 과거월)으로 정렬한다.</p>
	 *
	 * <p><b>집계 항목</b></p>
	 * <ul>
	 *   <li>총매출(₩), 주문수(건), 평균주문금액(₩/건)</li>
	 *   <li>주문유형별 매출: 배달/포장/매장</li>
	 * </ul>
	 *
	 * <p><b>커서 규칙</b></p>
	 * <ul>
	 *   <li>요청 커서가 존재하면 {@code monthLabel &lt; cursorYm} 조건으로 이후 페이지 조회.</li>
	 *   <li>{@code nextCursor}: 현재 페이지 마지막 {@code YYYY-MM} 문자열.</li>
	 * </ul>
	 *
	 * @param storeId 점포 ID.
	 * @param cond    조회 조건(시작일/종료일, size, cursor).
	 * @return 커서 페이지(월별 집계 행 리스트와 {@code nextCursor}).
	 */
	public CursorPage<OrderMonthlyRowDto> fetchOrderMonthlyRows(Long storeId, AnalyticsSearchDto cond) {
		int size = (cond.size() == null ? 50 : cond.size());

		LocalDateTime start = cond.startDate().atStartOfDay();
		LocalDateTime endEx = cond.endDate().plusDays(1).atStartOfDay();

		BooleanExpression filter = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, start, endEx));

		StringExpression monthLabel = Expressions.stringTemplate(
				"DATE_FORMAT({0}, {1})", co.orderedAt, ConstantImpl.create("%Y-%m"));

		// 커서: 최근 월 기준 (YYYY-MM) 내려가기
		if (cond.cursor() != null && !cond.cursor().isBlank()) {
			filter = filter.and(monthLabel.lt(cond.cursor()));
		}

		NumberExpression<BigDecimal> totalSalesExpr = co.totalPrice.sum();
		NumberExpression<Long>       orderCountExpr = co.id.countDistinct();

		NumberExpression<BigDecimal> deliverySalesExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.DELIVERY))
				.then(co.totalPrice)
				.otherwise(Expressions.constant(BigDecimal.ZERO))
				.sum();

		NumberExpression<BigDecimal> takeoutSalesExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.TAKEOUT))
				.then(co.totalPrice)
				.otherwise(Expressions.constant(BigDecimal.ZERO))
				.sum();

		NumberExpression<BigDecimal> visitSalesExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.VISIT))
				.then(co.totalPrice)
				.otherwise(Expressions.constant(BigDecimal.ZERO))
				.sum();

		List<Tuple> rows = query
				.select(
						monthLabel,
						totalSalesExpr,
						orderCountExpr,
						deliverySalesExpr,
						takeoutSalesExpr,
						visitSalesExpr
				)
				.from(co)
				.join(co.store, s)
				.where(filter)
				.groupBy(monthLabel)
				.orderBy(monthLabel.desc())
				.limit(size + 1)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		List<OrderMonthlyRowDto> items = new ArrayList<>();
		List<Tuple> pageRows = rows.size() > size ? rows.subList(0, size) : rows;

		for (Tuple t : pageRows) {
			String ym = t.get(monthLabel);

			BigDecimal totalSalesBD = nvlBD(t.get(totalSalesExpr));
			long totalSales         = totalSalesBD.longValue();
			long orderCount         = nvlLong(t.get(orderCountExpr));

			BigDecimal deliveryBD = nvlBD(t.get(deliverySalesExpr));
			BigDecimal takeoutBD  = nvlBD(t.get(takeoutSalesExpr));
			BigDecimal visitBD    = nvlBD(t.get(visitSalesExpr));

			long delivery = deliveryBD.longValue();
			long takeout  = takeoutBD.longValue();
			long visit    = visitBD.longValue();

			long avgOrderAmount = Math.round(safeDiv(totalSales, orderCount));

			items.add(new OrderMonthlyRowDto(
					ym,
					totalSales,
					orderCount,
					avgOrderAmount,
					delivery,
					takeout,
					visit
			));
		}

		String nextCursor = null;
		if (rows.size() > size) {
			Tuple last = rows.get(size - 1);
			String lastYm = last.get(monthLabel);
			nextCursor = lastYm;
		}

		return new CursorPage<>(items, nextCursor);
	}


	@Override
	@Transactional(readOnly = true)
	/**
	 * 메뉴 분석 상단 요약 카드를 조회한다.
	 *
	 * <p>KST {@code today 00:00} 기준으로 이번 달 1일 00:00부터 오늘 00:00 직전까지
	 * ({@code [thisMonth-01 00:00, today 00:00)}) COMPLETED 주문을 대상으로 한다.</p>
	 *
	 * <p><b>집계 항목</b></p>
	 * <ul>
	 *   <li>판매수량 Top3 메뉴: 주문상세 수량 합계 기준 내림차순</li>
	 *   <li>카테고리 매출 Top3: 카테고리별 매출 합계 기준 내림차순</li>
	 *   <li>매출 기여도 Top3 메뉴: (메뉴 매출 / 전체 메뉴 매출) × 100, 소수점 1자리 반올림</li>
	 *   <li>저성과 메뉴(하위 3개): 메뉴 매출 합계 기준 오름차순</li>
	 * </ul>
	 *
	 * <p><b>주의</b></p>
	 * <ul>
	 *   <li>전체 매출 합계가 0일 때 매출 기여도는 0.0으로 처리한다.</li>
	 *   <li>정렬은 Java 측 스트림에서 Comparator로 수행한다.</li>
	 * </ul>
	 *
	 * @param storeId 점포 ID
	 * @param today   조회 기준일(KST, {@code LocalDate})
	 * @return Top/하위 랭킹을 포함한 메뉴 요약 DTO
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	public MenuSummaryDto fetchMenuSummary(Long storeId, LocalDate today) {
		LocalDateTime todayStart = today.atStartOfDay();
		LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

		// MTD: 이번 달 1일 00:00 ~ 오늘 00:00 (어제까지)
		BooleanExpression base = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, monthStart, todayStart));

		// -------- 0) 공통: 메뉴별 수량/매출 집계 --------
		NumberExpression<Integer> qtySumExpr = cod.quantity.sum();
		NumberExpression<BigDecimal> salesSumExpr = cod.lineTotal.sum();

		List<Tuple> menuRows = query
				.select(
						m.menuId,
						m.menuName,
						qtySumExpr,
						salesSumExpr
				)
				.from(cod)
				.join(cod.order, co)
				.join(co.store, s)
				.join(cod.menuIdFk, m)
				.where(base)
				.groupBy(m.menuId, m.menuName)
				.fetch();

		// 전체 메뉴 매출 합계 (매출 기여도 계산용)
		BigDecimal totalSalesBD = BigDecimal.ZERO;
		for (Tuple t : menuRows) {
			totalSalesBD = totalSalesBD.add(nvlBD(t.get(salesSumExpr)));
		}
		long totalSalesAll = totalSalesBD.longValue();

		// 공통 Comparator
		Comparator<Tuple> byQtyDesc = Comparator.comparingLong((Tuple t) -> {
			Integer q = t.get(qtySumExpr);
			return q == null ? 0L : q.longValue();
		}).reversed();

		Comparator<Tuple> bySalesDesc = Comparator.comparingLong((Tuple t) -> {
			BigDecimal s = nvlBD(t.get(salesSumExpr));
			return s.longValue();
		}).reversed();

		Comparator<Tuple> bySalesAsc = Comparator.comparingLong((Tuple t) -> {
			BigDecimal s = nvlBD(t.get(salesSumExpr));
			return s.longValue();
		});

		// -------- 1) 판매수량 Top3 메뉴 --------
		List<MenuTopMenuDto> topMenusByQty = menuRows.stream()
				.sorted(byQtyDesc)
				.limit(3)
				.map(t -> {
					Integer qtyInt = t.get(qtySumExpr);
					long qty = (qtyInt == null) ? 0L : qtyInt.longValue();
					return new MenuTopMenuDto(
							t.get(m.menuId),
							t.get(m.menuName),
							qty
					);
				})
				.toList();

		// -------- 2) 매출 Top3 카테고리 --------
		NumberExpression<BigDecimal> catSalesExpr = cod.lineTotal.sum();

		List<Tuple> catRows = query
				.select(
						mc.menuCategoryId,
						mc.menuCategoryName,
						catSalesExpr
				)
				.from(cod)
				.join(cod.order, co)
				.join(co.store, s)
				.join(cod.menuIdFk, m)
				.join(m.menuCategory, mc)
				.where(base)
				.groupBy(mc.menuCategoryId, mc.menuCategoryName)
				.orderBy(catSalesExpr.desc())
				.limit(3)
				.fetch();

		List<MenuCategoryRankDto> topCategoriesBySales = catRows.stream()
				.map(t -> {
					BigDecimal salesBD = nvlBD(t.get(catSalesExpr));
					return new MenuCategoryRankDto(
							t.get(mc.menuCategoryId),
							t.get(mc.menuCategoryName),
							salesBD.longValue()
					);
				})
				.toList();

		// -------- 3) 매출 기여도 Top3 메뉴 --------
		List<MenuSalesContributionDto> topMenusBySalesContribution = menuRows.stream()
				.sorted(bySalesDesc)
				.limit(3)
				.map(t -> {
					BigDecimal salesBD = nvlBD(t.get(salesSumExpr));
					long sales = salesBD.longValue();
					double share = (totalSalesAll == 0L)
							? 0.0
							: round1((sales * 100.0) / totalSalesAll); // 소수점 1자리

					return new MenuSalesContributionDto(
							t.get(m.menuId),
							t.get(m.menuName),
							sales,
							share
					);
				})
				.toList();

		// -------- 4) 저성과 Top 메뉴 (매출 하위 3개) --------
		List<MenuLowPerformanceDto> lowPerformMenus = menuRows.stream()
				.sorted(bySalesAsc) // 매출 오름차순
				.limit(3)
				.map(t -> {
					Integer qtyInt = t.get(qtySumExpr);
					long qty = (qtyInt == null) ? 0L : qtyInt.longValue();
					BigDecimal salesBD = nvlBD(t.get(salesSumExpr));
					long sales = salesBD.longValue();

					return new MenuLowPerformanceDto(
							t.get(m.menuId),
							t.get(m.menuName),
							qty,
							sales
					);
				})
				.toList();

		return new MenuSummaryDto(
				topMenusByQty,
				topCategoriesBySales,
				topMenusBySalesContribution,
				lowPerformMenus
		);
	}


	@Override
	@Transactional(readOnly = true)
	/**
	 * 메뉴 분석 일별 테이블을 커서 기반으로 페이지 조회한다.
	 *
	 * <p>기간은 {@code [start, end]} 모두 포함으로 해석하며 내부적으로
	 * {@code [start 00:00, end+1 00:00)}로 변환한다. COMPLETED 주문만 대상.</p>
	 *
	 * <p><b>집계 단위</b> : (날짜 YYYY-MM-DD, 메뉴ID) 별</p>
	 * <ul>
	 *   <li>판매수량 합계: 주문상세 수량 합</li>
	 *   <li>매출 합계: 주문상세 lineTotal 합</li>
	 *   <li>주문수: 주문 헤더 ID countDistinct</li>
	 * </ul>
	 *
	 * <p><b>정렬/커서 규칙</b></p>
	 * <ul>
	 *   <li>정렬: {@code orderDate DESC → sales DESC → menuId DESC}</li>
	 *   <li>커서 형식: {@code "YYYY-MM-DD|menuId"}</li>
	 *   <li>다음 페이지 조건: {@code (date &lt; cDate) OR (date = cDate AND menuId &lt; cMenuId)}</li>
	 *   <li>{@code nextCursor}: 현재 페이지 마지막 레코드의 {@code "date|menuId"}</li>
	 * </ul>
	 *
	 * @param storeId 점포 ID
	 * @param cond    조회 조건(시작일/종료일, size, cursor)
	 * @return 커서 페이지(일별 메뉴 집계 행 리스트와 {@code nextCursor})
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	public CursorPage<MenuDailyRowDto> fetchMenuDailyRows(Long storeId, AnalyticsSearchDto cond) {

		LocalDateTime startDT = cond.startDate().atStartOfDay();
		LocalDateTime endExDT = cond.endDate().plusDays(1).atStartOfDay();

		BooleanExpression base = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, startDT, endExDT));

		StringTemplate dayLabel = Expressions.stringTemplate(
				"DATE_FORMAT({0}, '%Y-%m-%d')", co.orderedAt
		);

		NumberExpression<Integer>   qtySumExpr    = cod.quantity.sum();
		NumberExpression<BigDecimal> salesSumExpr = cod.lineTotal.sum();
		NumberExpression<Long>       orderCntExpr = co.id.countDistinct();

		// ----- 커서 처리 -----
		BooleanExpression cursorFilter = null;
		if (cond.cursor() != null && cond.cursor().contains("|")) {
			String[] arr = cond.cursor().split("\\|");
			String cDate = arr[0];
			Long cMenuId = Long.valueOf(arr[1]);

			cursorFilter = dayLabel.lt(cDate)
					.or(dayLabel.eq(cDate).and(m.menuId.lt(cMenuId)));
		}

		// ----- 쿼리 -----
		List<Tuple> rows = query
				.select(
						dayLabel,
						mc.menuCategoryName,
						m.menuName,
						qtySumExpr,
						salesSumExpr,
						orderCntExpr,
						m.menuId
				)
				.from(cod)
				.join(cod.order, co)
				.join(co.store, s)
				.join(cod.menuIdFk, m)
				.join(m.menuCategory, mc)
				.where(base, cursorFilter)
				.groupBy(dayLabel, m.menuId, m.menuName, mc.menuCategoryName)
				.orderBy(
						dayLabel.desc(),
						salesSumExpr.desc(),
						m.menuId.desc()
				)
				.limit(cond.size() + 1)
				.fetch();

		List<MenuDailyRowDto> result = new ArrayList<>();
		String nextCursor = null;

		for (Tuple t : rows) {
			if (result.size() == cond.size()) {
				String d = t.get(dayLabel);
				Long mid = t.get(m.menuId);
				nextCursor = d + "|" + mid;
				break;
			}

			Integer    qtyInt     = t.get(qtySumExpr);
			BigDecimal salesBD    = nvlBD(t.get(salesSumExpr));
			Long       orderCnt   = nvlLong(t.get(orderCntExpr));

			result.add(new MenuDailyRowDto(
					t.get(dayLabel),
					t.get(mc.menuCategoryName),
					t.get(m.menuName),
					qtyInt == null ? 0L : qtyInt.longValue(),
					salesBD.longValue(),
					orderCnt
			));
		}

		return new CursorPage<>(result, nextCursor);
	}


	@Override
	@Transactional(readOnly = true)
	/**
	 * 메뉴 분석 월별 테이블을 커서 기반으로 페이지 조회한다.
	 *
	 * <p>기간은 {@code [start, end]} 모두 포함으로 해석하며 내부적으로
	 * {@code [start 00:00, end+1 00:00)}로 변환한다. COMPLETED 주문만 대상.</p>
	 *
	 * <p><b>집계 단위</b> : (월 YYYY-MM, 메뉴ID) 별</p>
	 * <ul>
	 *   <li>판매수량 합계: 주문상세 수량 합</li>
	 *   <li>매출 합계: 주문상세 lineTotal 합</li>
	 *   <li>주문수: 주문 헤더 ID countDistinct</li>
	 * </ul>
	 *
	 * <p><b>정렬/커서 규칙</b></p>
	 * <ul>
	 *   <li>정렬: {@code yearMonth DESC → sales DESC → menuId DESC}</li>
	 *   <li>커서 형식: {@code "YYYY-MM|menuId"}</li>
	 *   <li>다음 페이지 조건: {@code (ym &lt; cYm) OR (ym = cYm AND menuId &lt; cMenuId)}</li>
	 *   <li>{@code nextCursor}: 현재 페이지 마지막 레코드의 {@code "YYYY-MM|menuId"}</li>
	 * </ul>
	 *
	 * @param storeId 점포 ID
	 * @param cond    조회 조건(시작일/종료일, size, cursor)
	 * @return 커서 페이지(월별 메뉴 집계 행 리스트와 {@code nextCursor})
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	public CursorPage<MenuMonthlyRowDto> fetchMenuMonthlyRows(Long storeId, AnalyticsSearchDto cond) {

		int size = (cond.size() == null ? 50 : cond.size());

		LocalDateTime startDT = cond.startDate().atStartOfDay();
		LocalDateTime endExDT = cond.endDate().plusDays(1).atStartOfDay();

		BooleanExpression base = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, startDT, endExDT));

		// YYYY-MM 라벨
		StringTemplate ymLabel = Expressions.stringTemplate(
				"DATE_FORMAT({0}, '%Y-%m')", co.orderedAt
		);

		// 집계식
		NumberExpression<Integer>    qtySumExpr   = cod.quantity.sum();
		NumberExpression<BigDecimal> salesSumExpr = cod.lineTotal.sum();
		NumberExpression<Long>       orderCntExpr = co.id.countDistinct();

		// ----- 커서 처리: "YYYY-MM|menuId" 형식 -----
		BooleanExpression cursorFilter = null;
		String cursor = cond.cursor();

		if (cursor != null && !cursor.isBlank()) {
			try {
				String[] parts = cursor.split("\\|");
				if (parts.length == 2) {
					String cYm     = parts[0];                 // ex) 2025-09
					long   cMenuId = Long.parseLong(parts[1]); // ex) 144

					// 정렬: ym DESC, sales DESC, menuId DESC
					// WHERE에서는 ym + menuId만으로 "이후 페이지" 판단
					cursorFilter =
							ymLabel.lt(cYm)
									.or(
											ymLabel.eq(cYm)
													.and(m.menuId.lt(cMenuId))
									);
				}
			} catch (Exception ignore) {
				// 잘못된 커서 값이면 무시하고 첫 페이지처럼 동작
				cursorFilter = null;
			}
		}

		// ----- 쿼리 -----
		List<Tuple> rows = query
				.select(
						ymLabel,
						m.menuName,
						mc.menuCategoryName,
						qtySumExpr,
						salesSumExpr,
						orderCntExpr,
						m.menuId
				)
				.from(cod)
				.join(cod.order, co)
				.join(co.store, s)
				.join(cod.menuIdFk, m)
				.join(m.menuCategory, mc)
				.where(base, cursorFilter)
				.groupBy(ymLabel, m.menuId, m.menuName, mc.menuCategoryName)
				.orderBy(
						ymLabel.desc(),
						salesSumExpr.desc(),
						m.menuId.desc()
				)
				.limit(size + 1)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		List<MenuMonthlyRowDto> result = new ArrayList<>();
		String nextCursor = null;

		boolean hasNext = rows.size() > size;
		List<Tuple> pageRows = hasNext ? rows.subList(0, size) : rows;

		for (Tuple t : pageRows) {
			String ym = t.get(ymLabel);

			Integer    qtyInt   = t.get(qtySumExpr);
			BigDecimal salesBD  = nvlBD(t.get(salesSumExpr));
			Long       orders   = nvlLong(t.get(orderCntExpr));

			long qty   = (qtyInt == null) ? 0L : qtyInt.longValue();
			long sales = salesBD.longValue();

			result.add(new MenuMonthlyRowDto(
					ym,
					t.get(m.menuName),
					t.get(mc.menuCategoryName),
					qty,
					sales,
					orders
			));
		}

		// ----- nextCursor 생성 -----
		if (hasNext && !pageRows.isEmpty()) {
			Tuple last      = pageRows.get(pageRows.size() - 1);
			String ymLast   = last.get(ymLabel);
			Long menuIdLast = last.get(m.menuId);

			// "YYYY-MM|menuId"
			nextCursor = ymLast + "|" + menuIdLast;
		}

		return new CursorPage<>(result, nextCursor);
	}


	// ============================================================================
	//                            ★ 재료 분석 Summary ★
	// ============================================================================
	@Override
	@Transactional(readOnly = true)
	public MaterialSummaryDto fetchMaterialSummary(Long storeId, LocalDate today) {
		// 이번달 MTD와 전월 동일기간 계산
		LocalDate thisMonthStart = today.withDayOfMonth(1);
		LocalDate currentEndDate = today.minusDays(1);
		boolean hasMtd = !currentEndDate.isBefore(thisMonthStart);

		LocalDate prevMonthStart = thisMonthStart.minusMonths(1);
		LocalDate prevMonthLast = prevMonthStart.withDayOfMonth(prevMonthStart.lengthOfMonth());
		int mtdDay = hasMtd ? currentEndDate.getDayOfMonth() : 0;
		int prevEndDay = hasMtd ? Math.min(mtdDay, prevMonthLast.getDayOfMonth()) : 0;
		LocalDate prevEndDate = hasMtd && prevEndDay > 0 ? prevMonthStart.withDayOfMonth(prevEndDay) : prevMonthStart.minusDays(1);

		LocalDateTime currentStartDt = thisMonthStart.atStartOfDay();
		LocalDateTime currentEndExDt = today.atStartOfDay();                // [1일 00:00, 오늘 00:00)
		LocalDateTime prevStartDt    = prevMonthStart.atStartOfDay();
		LocalDateTime prevEndExDt    = prevEndDate.plusDays(1).atStartOfDay();

		// Top5(사용량/원가)
		List<MaterialTopItemDto> topByUsage = hasMtd
				? findMaterialTopByUsage(storeId, currentStartDt, currentEndExDt, 5)
				: List.of();
		List<MaterialTopItemDto> topByCost = hasMtd
				? findMaterialTopByCost(storeId, currentStartDt, currentEndExDt, 5)
				: List.of();

		// 원가율(현재/전월동기간)
		double currentCostRate = 0.0, prevCostRate = 0.0, diff = 0.0;
		if (hasMtd) {
			BigDecimal curCost = fetchMaterialCostTotal(storeId, currentStartDt, currentEndExDt);
			long curSales = fetchSalesTotal(storeId, currentStartDt, currentEndExDt);
			if (curSales > 0L) currentCostRate = round1(safeDiv(curCost.longValue(), curSales) * 100.0);

			BigDecimal prvCost = fetchMaterialCostTotal(storeId, prevStartDt, prevEndExDt);
			long prvSales = fetchSalesTotal(storeId, prevStartDt, prevEndExDt);
			if (prvSales > 0L) prevCostRate = round1(safeDiv(prvCost.longValue(), prvSales) * 100.0);

			diff = round1(currentCostRate - prevCostRate);
		}

		// 재고 위험
		long lowStockCount   = fetchLowStockCount(storeId);
		long expireSoonCount = fetchExpireSoonCount(storeId, today);

		return new MaterialSummaryDto(
				topByUsage,
				topByCost,
				currentCostRate,
				prevCostRate,
				diff,
				lowStockCount,
				expireSoonCount
		);
	}

	// ============================================================================
	//                         ★ 재료 분석 일별 테이블 ★
	// ============================================================================
	@Override
	@Transactional(readOnly = true)
	public CursorPage<MaterialDailyRowDto> fetchMaterialDailyRows(Long storeId, AnalyticsSearchDto cond) {
		int size = (cond.size() == null ? 50 : cond.size());

		LocalDateTime startDt = cond.startDate().atStartOfDay();
		LocalDateTime endExDt = cond.endDate().plusDays(1).atStartOfDay();

		// 매출(일자별) 맵 / 최근 입고일 맵
		Map<String, Long> salesByDate = fetchSalesByDayForMaterials(storeId, startDt, endExDt);
		Map<Long, LocalDateTime> lastInboundBySm = fetchLastInboundDateByStoreMaterial(storeId);

		// 커서: "YYYY-MM-DD|storeMaterialId"
		String cursor = cond.cursor();
		String cDate = null; Long cSmId = null;
		if (cursor != null && !cursor.isBlank()) {
			String[] parts = cursor.split("\\|");
			if (parts.length >= 2) {
				cDate = parts[0];
				try { cSmId = Long.valueOf(parts[1]); } catch (NumberFormatException ignore) {}
			}
		}

		// 라벨(일)
		StringExpression dayExpr = Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m-%d')", co.orderedAt);

		// 집계식
		NumberExpression<BigDecimal> usedQtyExpr = log.count.sum();     // baseUnit 수량 합
		NumberExpression<BigDecimal> costExpr    = materialCostSumExpr(); // SUM( (count / conv) * price )

		// 이름/단위
		StringExpression materialNameExpr = Expressions.stringTemplate("IFNULL({0}, {1})", sm.name, material.name);

		// 커서 조건
		BooleanExpression cursorFilter = null;
		if (cDate != null && cSmId != null) {
			cursorFilter = dayExpr.lt(cDate)
					.or(dayExpr.eq(cDate).and(sm.id.lt(cSmId)));
		}

		List<Tuple> tuples = query
				.select(
						dayExpr,                // 0
						sm.id,                  // 1
						materialNameExpr,       // 2 (IFNULL)
						sm.baseUnit,            // 3
						usedQtyExpr,            // 4
						costExpr                // 5
				)
				.from(log)
				.join(log.customerOrderFk, co)
				.join(co.store, s)
				.join(log.storeMaterialFk, sm)
				.leftJoin(sm.material, material) // HQ재료 없을 수도 있으므로 LEFT
				.where(
						statusCompleted(),
						eqStore(storeId),
						betweenClosedOpen(co.orderedAt, startDt, endExDt),
						cursorFilter
				)
				.groupBy(dayExpr, sm.id, sm.name, material.name, sm.baseUnit)
				.orderBy(dayExpr.desc(), sm.id.desc())
				.limit(size + 1) // ← hasNext 판단을 위해 +1
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		boolean hasNext = tuples.size() > size;
		List<Tuple> pageRows = hasNext ? tuples.subList(0, size) : tuples;

		List<MaterialDailyRowDto> items = new ArrayList<>(pageRows.size());
		for (Tuple t : pageRows) {
			String useDate       = t.get(dayExpr);
			Long storeMaterialId = t.get(sm.id);
			String matName       = t.get(materialNameExpr);
			String unitName      = t.get(sm.baseUnit);

			double usedQty = nvlBD(t.get(usedQtyExpr)).doubleValue();
			long cost      = nvlBD(t.get(costExpr)).longValue();

			long daySales = salesByDate.getOrDefault(useDate, 0L);
			double salesShare = (daySales > 0L && cost > 0L) ? round1(safeDiv(cost, daySales) * 100.0) : 0.0;

			LocalDateTime inbound = lastInboundBySm.get(storeMaterialId);
			String inboundStr = (inbound != null ? inbound.toString() : null);

			items.add(new MaterialDailyRowDto(
					useDate,
					matName,
					usedQty,
					unitName,
					cost,
					salesShare,
					inboundStr
			));
		}

		String nextCursor = null;
		if (hasNext && !pageRows.isEmpty()) {
			Tuple last = pageRows.get(pageRows.size() - 1);
			nextCursor = last.get(dayExpr) + "|" + last.get(sm.id);
		}

		return new CursorPage<>(items, nextCursor);
	}

	// ============================================================================
	//                         ★ 재료 분석 월별 테이블 ★
	// ============================================================================
	@Override
	@Transactional(readOnly = true)
	public CursorPage<MaterialMonthlyRowDto> fetchMaterialMonthlyRows(Long storeId, AnalyticsSearchDto cond) {
		int size = (cond.size() == null ? 50 : cond.size());

		LocalDateTime startDt = cond.startDate().atStartOfDay();
		LocalDateTime endExDt = cond.endDate().plusDays(1).atStartOfDay();

		// 매출(월별) 맵 / 최근 입고일 맵
		Map<String, Long> salesByMonth = fetchSalesByMonthForMaterials(storeId, startDt, endExDt);
		Map<Long, LocalDateTime> lastInboundBySm = fetchLastInboundDateByStoreMaterial(storeId);

		// 커서: "YYYY-MM|storeMaterialId"
		String cursor = cond.cursor();
		String cYm = null; Long cSmId = null;
		if (cursor != null && !cursor.isBlank()) {
			String[] parts = cursor.split("\\|");
			if (parts.length >= 2) {
				cYm  = parts[0];
				try { cSmId = Long.valueOf(parts[1]); } catch (NumberFormatException ignore) {}
			}
		}

		StringExpression ymExpr = Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m')", co.orderedAt);
		NumberExpression<BigDecimal> usedQtyExpr = log.count.sum();
		NumberExpression<BigDecimal> costExpr    = materialCostSumExpr();
		StringExpression materialNameExpr = Expressions.stringTemplate("IFNULL({0}, {1})", sm.name, material.name);

		BooleanExpression cursorFilter = null;
		if (cYm != null && cSmId != null) {
			cursorFilter = ymExpr.lt(cYm)
					.or(ymExpr.eq(cYm).and(sm.id.lt(cSmId)));
		}

		List<Tuple> tuples = query
				.select(
						ymExpr,
						sm.id,
						materialNameExpr,
						sm.baseUnit,
						usedQtyExpr,
						costExpr
				)
				.from(log)
				.join(log.customerOrderFk, co)
				.join(co.store, s)
				.join(log.storeMaterialFk, sm)
				.leftJoin(sm.material, material)
				.where(
						statusCompleted(),
						eqStore(storeId),
						betweenClosedOpen(co.orderedAt, startDt, endExDt),
						cursorFilter
				)
				.groupBy(ymExpr, sm.id, sm.name, material.name, sm.baseUnit)
				.orderBy(ymExpr.desc(), sm.id.desc())
				.limit(size + 1) // ← hasNext 판단을 위해 +1
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		boolean hasNext = tuples.size() > size;
		List<Tuple> pageRows = hasNext ? tuples.subList(0, size) : tuples;

		List<MaterialMonthlyRowDto> items = new ArrayList<>(pageRows.size());
		DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

		for (Tuple t : pageRows) {
			String ym       = t.get(ymExpr);
			Long smId       = t.get(sm.id);
			String matName  = t.get(materialNameExpr);
			String unitName = t.get(sm.baseUnit);

			double usedQty  = nvlBD(t.get(usedQtyExpr)).doubleValue();
			long cost       = nvlBD(t.get(costExpr)).longValue();

			long monthSales = salesByMonth.getOrDefault(ym, 0L);
			double costRate = (monthSales > 0L && cost > 0L) ? round1(safeDiv(cost, monthSales) * 100.0) : 0.0;

			LocalDateTime inbound = lastInboundBySm.get(smId);
			String lastInboundMonth = inbound != null ? inbound.format(ymFormatter) : null;

			items.add(new MaterialMonthlyRowDto(
					ym,
					matName,
					usedQty,
					cost,
					costRate,
					lastInboundMonth
			));
		}

		String nextCursor = null;
		if (hasNext && !pageRows.isEmpty()) {
			Tuple last = pageRows.get(pageRows.size() - 1);
			nextCursor = last.get(ymExpr) + "|" + last.get(sm.id);
		}

		return new CursorPage<>(items, nextCursor);
	}



	/**
	 * 재료 사용량 Top 리스트 조회.
	 *
	 * <p><b>대상/기간</b>: 단일 점포({@code storeId}), COMPLETED 주문, {@code [startDt, endExDt)}.</p>
	 * <p><b>집계</b>:
	 * <ul>
	 *   <li>사용량: {@code log.count.sum()}</li>
	 *   <li>원가: {@code materialCostSumExpr()} ( (count / conversionRate) * purchasePrice )</li>
	 *   <li>재료명: {@code IFNULL(sm.name, material.name)}</li>
	 * </ul>
	 * </p>
	 * <p><b>정렬/한도</b>: 사용량 DESC, 동률 시 sm.id ASC, {@code limit} 개.</p>
	 *
	 * @param storeId 점포 ID
	 * @param startDt 조회 시작 (포함)
	 * @param endExDt 조회 종료 (배타)
	 * @param limit   최대 반환 개수
	 * @return 사용량 기준 상위 재료 리스트
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private List<MaterialTopItemDto> findMaterialTopByUsage(
			Long storeId, LocalDateTime startDt, LocalDateTime endExDt, int limit) {

		NumberExpression<BigDecimal> usedQtyExpr = log.count.sum();
		NumberExpression<BigDecimal> costExpr = materialCostSumExpr();
		StringExpression materialNameExpr = Expressions.stringTemplate(
				"IFNULL({0}, {1})", sm.name, material.name
		);

		List<Tuple> tuples = query
				.select(
						sm.id,
						materialNameExpr,
						sm.baseUnit,
						usedQtyExpr,
						costExpr
				)
				.from(log)
				.join(log.customerOrderFk, co)
				.join(co.store, s)
				.join(log.storeMaterialFk, sm)
				.leftJoin(sm.material, material)
				.where(
						statusCompleted(),
						eqStore(storeId),
						betweenClosedOpen(co.orderedAt, startDt, endExDt)
				)
				.groupBy(sm.id, sm.name, material.name, sm.baseUnit)
				.orderBy(usedQtyExpr.desc(), sm.id.asc())
				.limit(limit)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		List<MaterialTopItemDto> result = new ArrayList<>(tuples.size());
		for (Tuple t : tuples) {
			Long smId = t.get(sm.id);
			String matName = t.get(materialNameExpr);
			String unit = t.get(sm.baseUnit);
			double qty = nvlBD(t.get(usedQtyExpr)).doubleValue();
			long costLong = nvlBD(t.get(costExpr)).longValue();

			result.add(new MaterialTopItemDto(smId, matName, unit, qty, costLong));
		}
		return result;
	}

	/**
	 * 재료 원가 Top 리스트 조회.
	 *
	 * <p><b>대상/기간</b>: 단일 점포({@code storeId}), COMPLETED 주문, {@code [startDt, endExDt)}.</p>
	 * <p><b>집계</b>:
	 * <ul>
	 *   <li>사용량: {@code log.count.sum()} (정보 제공용으로 함께 반환)</li>
	 *   <li>원가: {@code materialCostSumExpr()} (정렬 key)</li>
	 *   <li>재료명: {@code IFNULL(sm.name, material.name)}</li>
	 * </ul>
	 * </p>
	 * <p><b>정렬/한도</b>: 원가 DESC, 동률 시 sm.id ASC, {@code limit} 개.</p>
	 *
	 * @param storeId 점포 ID
	 * @param startDt 조회 시작 (포함)
	 * @param endExDt 조회 종료 (배타)
	 * @param limit   최대 반환 개수
	 * @return 원가 기준 상위 재료 리스트
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private List<MaterialTopItemDto> findMaterialTopByCost(
			Long storeId, LocalDateTime startDt, LocalDateTime endExDt, int limit) {

		NumberExpression<BigDecimal> usedQtyExpr = log.count.sum();
		NumberExpression<BigDecimal> costExpr = materialCostSumExpr();
		StringExpression materialNameExpr = Expressions.stringTemplate(
				"IFNULL({0}, {1})", sm.name, material.name
		);

		List<Tuple> tuples = query
				.select(
						sm.id,
						materialNameExpr,
						sm.baseUnit,
						usedQtyExpr,
						costExpr
				)
				.from(log)
				.join(log.customerOrderFk, co)
				.join(co.store, s)
				.join(log.storeMaterialFk, sm)
				.leftJoin(sm.material, material)
				.where(
						statusCompleted(),
						eqStore(storeId),
						betweenClosedOpen(co.orderedAt, startDt, endExDt)
				)
				.groupBy(sm.id, sm.name, material.name, sm.baseUnit)
				.orderBy(costExpr.desc(), sm.id.asc())
				.limit(limit)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		List<MaterialTopItemDto> result = new ArrayList<>(tuples.size());
		for (Tuple t : tuples) {
			Long smId = t.get(sm.id);
			String matName = t.get(materialNameExpr);
			String unit = t.get(sm.baseUnit);
			double qty = nvlBD(t.get(usedQtyExpr)).doubleValue();
			long costLong = nvlBD(t.get(costExpr)).longValue();

			result.add(new MaterialTopItemDto(smId, matName, unit, qty, costLong));
		}
		return result;
	}

	/**
	 * 재료 원가 총합 조회.
	 *
	 * <p>식: {@code SUM( (log.count / conversionRate) * purchasePrice )}.</p>
	 * <p>대상 기간: {@code [startDt, endExDt)}, COMPLETED 주문, 단일 점포.</p>
	 *
	 * @param storeId 점포 ID
	 * @param startDt 조회 시작(포함)
	 * @param endExDt 조회 종료(배타)
	 * @return 원가 총합(BigDecimal, null 안전 처리)
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private BigDecimal fetchMaterialCostTotal(Long storeId, LocalDateTime startDt, LocalDateTime endExDt) {
		NumberExpression<BigDecimal> costExpr = materialCostSumExpr();

		BigDecimal result = query
				.select(costExpr)
				.from(log)
				.join(log.customerOrderFk, co)
				.join(co.store, s)
				.join(log.storeMaterialFk, sm)
				.leftJoin(sm.material, material)
				.where(
						statusCompleted(),
						eqStore(storeId),
						betweenClosedOpen(co.orderedAt, startDt, endExDt)
				)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetchOne();

		return nvlBD(result);
	}

	/**
	 * 매출 총합 조회.
	 *
	 * <p>식: {@code SUM(co.totalPrice)}. 기간은 {@code [startDt, endExDt)}.</p>
	 *
	 * @param storeId 점포 ID
	 * @param startDt 조회 시작(포함)
	 * @param endExDt 조회 종료(배타)
	 * @return 매출 총합(원, long; null이면 0)
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private long fetchSalesTotal(Long storeId, LocalDateTime startDt, LocalDateTime endExDt) {
		BigDecimal salesBD = query
				.select(co.totalPrice.sum())
				.from(co)
				.join(co.store, s)
				.where(
						statusCompleted(),
						eqStore(storeId),
						betweenClosedOpen(co.orderedAt, startDt, endExDt)
				)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetchOne();
		return salesBD == null ? 0L : salesBD.longValue();
	}

	/**
	 * 재료 분석용 일자별 매출 맵 조회.
	 *
	 * <p>키: {@code 'YYYY-MM-DD'}, 값: 해당 일자의 매출 합(원).</p>
	 * <p>대상: COMPLETED 주문, {@code [startDt, endExDt)}, 단일 점포.</p>
	 *
	 * @param storeId 점포 ID
	 * @param startDt 조회 시작(포함)
	 * @param endExDt 조회 종료(배타)
	 * @return {@code Map<날짜문자열, 매출(원)>}
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private Map<String, Long> fetchSalesByDayForMaterials(Long storeId, LocalDateTime startDt, LocalDateTime endExDt) {
		StringExpression dayExpr = Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m-%d')", co.orderedAt);
		NumberExpression<BigDecimal> salesExpr = co.totalPrice.sum();

		List<Tuple> tuples = query
				.select(dayExpr, salesExpr)
				.from(co)
				.join(co.store, s)
				.where(
						statusCompleted(),
						eqStore(storeId),
						betweenClosedOpen(co.orderedAt, startDt, endExDt)
				)
				.groupBy(dayExpr)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		Map<String, Long> map = new HashMap<>(tuples.size());
		for (Tuple t : tuples) {
			map.put(t.get(dayExpr), nvlBD(t.get(salesExpr)).longValue());
		}
		return map;
	}

	/**
	 * 재료 분석용 월별 매출 맵 조회.
	 *
	 * <p>키: {@code 'YYYY-MM'}, 값: 해당 월의 매출 합(원).</p>
	 * <p>대상: COMPLETED 주문, {@code [startDt, endExDt)}, 단일 점포.</p>
	 *
	 * @param storeId 점포 ID
	 * @param startDt 조회 시작(포함)
	 * @param endExDt 조회 종료(배타)
	 * @return {@code Map<연월문자열, 매출(원)>}
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private Map<String, Long> fetchSalesByMonthForMaterials(Long storeId, LocalDateTime startDt, LocalDateTime endExDt) {
		StringExpression ymExpr = Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m')", co.orderedAt);
		NumberExpression<BigDecimal> salesExpr = co.totalPrice.sum();

		List<Tuple> tuples = query
				.select(ymExpr, salesExpr)
				.from(co)
				.join(co.store, s)
				.where(
						statusCompleted(),
						eqStore(storeId),
						betweenClosedOpen(co.orderedAt, startDt, endExDt)
				)
				.groupBy(ymExpr)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		Map<String, Long> map = new HashMap<>(tuples.size());
		for (Tuple t : tuples) {
			map.put(t.get(ymExpr), nvlBD(t.get(salesExpr)).longValue());
		}
		return map;
	}

	/**
	 * 점포-재료별 최근 입고일 조회.
	 *
	 * <p>식: {@code MAX(batch.receivedDate)}.</p>
	 * <p>대상: 단일 점포의 인벤토리 배치 기준으로 StoreMaterial 별 최신 입고일.</p>
	 * <p>반환: {@code Map<storeMaterialId, LocalDate>} (없으면 미포함).</p>
	 *
	 * @param storeId 점포 ID
	 * @return 최근 입고일 맵
	 * 주석변경필요
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private Map<Long, LocalDateTime> fetchLastInboundDateByStoreMaterial(Long storeId) {
		DateTimeExpression<LocalDateTime> lastReceivedExpr = batch.receivedDate.max();

		List<Tuple> tuples = query
				.select(sm.id, lastReceivedExpr)
				.from(batch)
				.join(batch.storeInventory, inv)
				.join(inv.storeMaterial, sm)
				.join(inv.store, s)
				.where(eqStore(storeId))
				.groupBy(sm.id)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		Map<Long, LocalDateTime> map = new HashMap<>(tuples.size());
		for (Tuple t : tuples) {
			Long smId = t.get(sm.id);
			LocalDateTime recv = t.get(lastReceivedExpr);
			if (smId != null && recv != null) map.put(smId, recv);
		}
		return map;
	}


	/**
	 * 재고 부족(LOW/SHORTAGE) 인벤토리 개수 조회.
	 *
	 * <p><b>대상</b>: 단일 점포 {@code storeId}의 StoreInventory.</p>
	 * <p><b>조건</b>: {@code InventoryStatus.LOW} 또는 {@code InventoryStatus.SHORTAGE} 상태.</p>
	 * <p><b>반환</b>: 중복 없는 인벤토리 행 수(Long), null 안전(없으면 0).</p>
	 *
	 * <p><b>성능</b>: readOnly/flushMode/timeout 힌트 설정.</p>
	 *
	 * @param storeId 점포 ID
	 * @return 부족 재고 개수
	 * 주석변경필요
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private long fetchLowStockCount(Long storeId) {
		QStoreInventory inv = QStoreInventory.storeInventory;

		Long result = query
				.select(inv.id.countDistinct())
				.from(inv)
				.where(
						storeId == null ? null : inv.store.id.eq(storeId),
						inv.status.in(InventoryStatus.LOW, InventoryStatus.SHORTAGE)
				)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000) // jakarta → javax 권장
				.fetchOne();

		return result != null ? result : 0L;
	}

	/**
	 * 유통기한 임박 배치 수 조회.
	 *
	 * <p><b>기간</b>: {@code [today, today + EXPIRE_SOON_DAYS]} (양끝 포함).</p>
	 * <p><b>대상</b>: 단일 점포 {@code storeId}의 {@code StoreInventoryBatch} 기준.</p>
	 * <p><b>반환</b>: 임박 구간에 포함되는 배치 기준 중복 없는 인벤토리 수(Long), null 안전(없으면 0).</p>
	 *
	 * <p><b>주의</b>: 임박 기준(EXPIRE_SOON_DAYS)은 FCM 알림 로직과 일관되게 유지.</p>
	 *
	 * @param storeId 점포 ID
	 * @param today   기준일(LocalDate, KST 가정)
	 * @return 유통기한 임박 개수
	 * 주석변경필요
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private long fetchExpireSoonCount(Long storeId, LocalDate today) {
		LocalDate endEx = today.plusDays(EXPIRE_SOON_DAYS + 1);

		Long result = query
				.select(inv.id.countDistinct())
				.from(batch)
				.join(batch.storeInventory, inv)
				.where(
						storeId == null ? null : inv.store.id.eq(storeId),
						batch.expirationDate.goe(today),
						batch.expirationDate.lt(endEx)
				)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetchOne();

		return result != null ? result : 0L;
	}

	/**
	 * 재료 원가 합계 식 생성.
	 *
	 * <p><b>정의</b>: {@code SUM( (log.count / conversionRate) * purchasePrice )}.</p>
	 * <p><b>null/0 보호</b>:
	 * <ul>
	 *   <li>{@code conversionRate}: NULL 또는 0 → 1.0 대체</li>
	 *   <li>{@code purchasePrice}: NULL → 0 대체</li>
	 *   <li>{@code COALESCE / NULLIF}로 SQL 레벨에서 안전성 확보</li>
	 * </ul>
	 * </p>
	 *
	 * <p>재사용 가능한 QueryDSL {@code NumberExpression<BigDecimal>}을 반환하며,
	 * SUM 까지를 포함한 누적 식으로 구성되어 그룹바이 문맥에서도 사용 가능합니다.</p>
	 *
	 * @return 원가 합계 식(BigDecimal)
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private NumberExpression<BigDecimal> materialCostSumExpr() {
		// conversionRate: NULL이면 1, 0이면 1로 대체
		NumberExpression<BigDecimal> safeConvRate = Expressions.numberTemplate(
				BigDecimal.class,
				"COALESCE(NULLIF({0}, 0), 1.0)",
				sm.conversionRate
		);

		// purchasePrice: NULL이면 0으로 대체
		NumberExpression<BigDecimal> safePurchasePrice = Expressions.numberTemplate(
				BigDecimal.class,
				"COALESCE({0}, 0)",
				sm.purchasePrice
		);

		// 최종 계산: SUM( (count / safeConvRate) * safePurchasePrice )
		return Expressions.numberTemplate(
				BigDecimal.class,
				"SUM( ({0} / {1}) * {2} )",
				log.count,
				safeConvRate,
				safePurchasePrice
		);
	}


	/**
	 * 주문 시각의 "시(hour)"를 추출하는 식 생성.
	 *
	 * <p><b>정의</b>: {@code HOUR(co.orderedAt)} → 0~23 범위 정수.</p>
	 * <p><b>용도</b>: 시간대별 집계(예: 07~20시 영업시간 필터)에서 공통으로 사용.</p>
	 * <p><b>주의</b>: DB 함수 {@code HOUR()} 사용(MySQL 호환). 타 DB 사용 시 대응 필요.</p>
	 *
	 * @return 0~23 범위를 갖는 시간(시) NumberExpression
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private NumberExpression<Integer> hourOfDay() {
		return Expressions.numberTemplate(Integer.class, "HOUR({0})", co.orderedAt);
	}

	/**
	 * 한국식 요일 정수(월=1, …, 일=7)로 변환하는 식 생성.
	 *
	 * <p><b>배경</b>: MySQL {@code DAYOFWEEK()}는 1=일, …, 7=토를 반환.
	 * 이를 월=1, …, 일=7 체계로 변환하기 위해 {@code ((DAYOFWEEK(x)+5)%7)+1}을 사용.</p>
	 *
	 * <p><b>검증</b>:
	 * <ul>
	 *   <li>일(1) → ((1+5)%7)+1 = 7 → 일</li>
	 *   <li>월(2) → ((2+5)%7)+1 = 1 → 월</li>
	 *   <li>…</li>
	 *   <li>토(7) → ((7+5)%7)+1 = 6 → 토</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>주의</b>: DB 함수 {@code DAYOFWEEK()} 사용(MySQL 호환). 타 DB 사용 시 변환식 조정 필요.</p>
	 *
	 * @return 1~7 범위를 갖는 요일 NumberExpression (월=1 … 일=7)
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private NumberExpression<Integer> weekDayKorean() {
		return Expressions.numberTemplate(
				Integer.class,
				"((DAYOFWEEK({0}) + 5) % 7) + 1",
				co.orderedAt
		);
	}

	/**
	 * 영업시간(07~20시) 필터식 생성.
	 *
	 * <p><b>정의</b>: {@code 7 <= hour <= 20} 조건을 만족하는 BooleanExpression.</p>
	 * <p><b>용도</b>: 시간대별/요일별 분석에서 영업시간 구간만 집계할 때 사용.</p>
	 * <p><b>주의</b>: 입력 {@code hourExpr}는 {@link #hourOfDay()} 등 0~23 정수 범위를 반환해야 함.</p>
	 *
	 * @param hourExpr 0~23 범위의 시(hour) 표현식
	 * @return 영업시간 구간(07~20시) 여부 BooleanExpression
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private BooleanExpression businessHoursFilter(NumberExpression<Integer> hourExpr) {
		return hourExpr.goe(7).and(hourExpr.loe(20));
	}


	@Override
	@Transactional(readOnly = true)
	/**
	 * 시간/요일 분석 상단 요약 카드를 조회한다.
	 *
	 * <p><b>기간 규칙</b>: today 기준 MTD = {@code [이번달 1일 00:00, 오늘 00:00)} (즉, “이번달 1일 ~ 어제까지”).</p>
	 * <p><b>영업시간 필터</b>: 시간대는 07~20시만 집계한다(브라우저/리포트 표준과 일치).</p>
	 * <p><b>산출 항목</b>:
	 * <ul>
	 *   <li>피크 시간대: 매출 최댓값의 시간(h), 매출액</li>
	 *   <li>비수 시간대: 매출 &gt; 0 인 구간 중 최솟값의 시간(h), 매출액</li>
	 *   <li>최고 매출 요일: 요일 인덱스(1~7, 월=1) 및 해당 매출액</li>
	 *   <li>주중/주말 매출 합계(주중=월~금, 주말=토/일)</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>엣지 케이스</b>:
	 * <ul>
	 *   <li>today가 1일이면 집계 구간이 비어 결과는 모두 0/NULL 로 처리됨</li>
	 *   <li>영업시간 내 데이터가 없으면 피크/비수/최고요일이 NULL 이 될 수 있음</li>
	 * </ul>
	 * </p>
	 *
	 * @param storeId 점포 ID
	 * @param today   기준일(LocalDate, KST 가정)
	 * @return {@link TimeDaySummaryDto} (NULL 허용 필드는 명세 참고)
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	public TimeDaySummaryDto fetchTimeDaySummary(Long storeId, LocalDate today) {

		// 이번달 1일
		LocalDate mtdStart = today.withDayOfMonth(1);
		// 어제
		LocalDate mtdEnd = today.minusDays(1);

		// 만약 오늘이 1일이면 mtdEnd < mtdStart -> where 조건은 그대로지만 결과 0건 → 전부 0/ null 처리
		LocalDateTime startDT = mtdStart.atStartOfDay();
		LocalDateTime endExDT = today.atStartOfDay(); // 어제 24:00 == 오늘 00:00

		BooleanExpression base = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, startDT, endExDT));

		NumberExpression<Integer> hourExpr = hourOfDay();
		NumberExpression<Integer> weekdayExpr = weekDayKorean();
		BooleanExpression bizHours = businessHoursFilter(hourExpr);

		NumberExpression<BigDecimal> salesSumExpr = co.totalPrice.sum();

		// ---- 1) 시간대별 매출 ----
		List<Tuple> hourlyRows = query
				.select(hourExpr, salesSumExpr)
				.from(co)
				.join(co.store, s)
				.where(base, bizHours)
				.groupBy(hourExpr)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		Integer peakHour = null;
		long peakSales = 0L;
		Integer offHour = null;
		long offSales = 0L;

		for (Tuple t : hourlyRows) {
			Integer h = t.get(hourExpr);
			if (h == null) continue;
			BigDecimal salesBD = nvlBD(t.get(salesSumExpr));
			long sales = salesBD.longValue();

			// 피크 (최대 매출)
			if (sales > peakSales) {
				peakSales = sales;
				peakHour = h;
			}
			// 비수 (매출>0 중 최소)
			if (sales > 0L) {
				if (offHour == null || sales < offSales) {
					offSales = sales;
					offHour = h;
				}
			}
		}

		// ---- 2) 요일별 매출 + 주중/주말 ----
		List<Tuple> weekdayRows = query
				.select(weekdayExpr, salesSumExpr)
				.from(co)
				.join(co.store, s)
				.where(base, bizHours)
				.groupBy(weekdayExpr)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		Integer topWeekday = null;
		long topWeekdaySales = 0L;
		long weekdaySales = 0L;
		long weekendSales = 0L;

		for (Tuple t : weekdayRows) {
			Integer wd = t.get(weekdayExpr);
			if (wd == null) continue;
			BigDecimal salesBD = nvlBD(t.get(salesSumExpr));
			long sales = salesBD.longValue();

			// 최고 매출 요일
			if (sales > topWeekdaySales) {
				topWeekdaySales = sales;
				topWeekday = wd;
			}

			// 주중(월~금=1~5) / 주말(토,일=6,7)
			if (wd == 6 || wd == 7) {
				weekendSales += sales;
			} else {
				weekdaySales += sales;
			}
		}

		return new TimeDaySummaryDto(
				peakHour,
				peakSales,
				offHour,
				offSales,
				topWeekday,
				topWeekdaySales,
				weekdaySales,
				weekendSales
		);
	}



	/**
	 * 시간대별(07~20시) 매출/주문수/채널별 주문수를 집계한다.
	 *
	 * <p><b>기간 규칙</b>: {@code [startDate 00:00, endDate+1 00:00)}.</p>
	 * <p><b>영업시간 필터</b>: 07~20시 범위만 집계하며, 누락된 시간대는 0 값으로 보정하여 7~20의 연속 구간을 항상 반환한다.</p>
	 * <p><b>산출 항목</b>:
	 * <ul>
	 *   <li>sales: 총매출(완료 주문 기준)</li>
	 *   <li>orders: 주문수(중복 제거)</li>
	 *   <li>visit/takeout/delivery: 채널별 주문수</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>성능</b>: 단일 GROUP BY(HOUR) 집계 1회. 인덱스: {@code (store_id, status, ordered_at)} 권장.</p>
	 *
	 * @param storeId   점포 ID
	 * @param startDate 조회 시작일(포함)
	 * @param endDate   조회 종료일(포함)
	 * @return 07~20시 구간의 {@link TimeHourlyPointDto} 목록(누락 시간대는 0으로 채움)
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	@Override
	@Transactional(readOnly = true)
	public List<TimeHourlyPointDto> fetchTimeHourlyChart(Long storeId, LocalDate startDate, LocalDate endDate) {

		LocalDateTime startDT = startDate.atStartOfDay();
		LocalDateTime endExDT = endDate.plusDays(1).atStartOfDay();

		BooleanExpression base = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, startDT, endExDT));

		NumberExpression<Integer> hourExpr = hourOfDay();
		BooleanExpression bizHours = businessHoursFilter(hourExpr);

		NumberExpression<BigDecimal> salesSumExpr = co.totalPrice.sum();
		NumberExpression<Long> orderCountExpr = co.id.countDistinct();

		NumberExpression<Long> visitCountExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.VISIT))
				.then(1L).otherwise(0L).sum();

		NumberExpression<Long> takeoutCountExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.TAKEOUT))
				.then(1L).otherwise(0L).sum();

		NumberExpression<Long> deliveryCountExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.DELIVERY))
				.then(1L).otherwise(0L).sum();

		List<Tuple> rows = query
				.select(
						hourExpr,
						salesSumExpr,
						orderCountExpr,
						visitCountExpr,
						takeoutCountExpr,
						deliveryCountExpr
				)
				.from(co)
				.join(co.store, s)
				.where(base, bizHours)
				.groupBy(hourExpr)
				.orderBy(hourExpr.asc())
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		Map<Integer, TimeHourlyPointDto> map = new HashMap<>();
		for (Tuple t : rows) {
			Integer h = t.get(hourExpr);
			if (h == null) continue;

			BigDecimal salesBD = nvlBD(t.get(salesSumExpr));
			long sales = salesBD.longValue();
			long orders = nvlLong(t.get(orderCountExpr));
			long visit = nvlLong(t.get(visitCountExpr));
			long takeout = nvlLong(t.get(takeoutCountExpr));
			long delivery = nvlLong(t.get(deliveryCountExpr));

			map.put(h, new TimeHourlyPointDto(h, sales, orders, visit, takeout, delivery));
		}

		// 07~20 모든 시간대를 채우되, 없는 시간대는 0으로 채움
		List<TimeHourlyPointDto> result = new ArrayList<>();
		for (int h = 7; h <= 20; h++) {
			TimeHourlyPointDto p = map.get(h);
			if (p == null) {
				p = new TimeHourlyPointDto(h, 0L, 0L, 0L, 0L, 0L);
			}
			result.add(p);
		}
		return result;
	}

	/**
	 * 요일별 매출/주문수를 집계한다.
	 *
	 * <p><b>기간 규칙</b>: {@code [startDate 00:00, endDate+1 00:00)}.</p>
	 * <p><b>요일 인덱스</b>: 1~7, 월=1 … 일=7. 내부적으로 {@code DAYOFWEEK()} 보정식을 사용.</p>
	 * <p><b>영업시간 필터</b>: 07~20시만 집계.</p>
	 * <p><b>반환 규칙</b>: 1~7 모든 요일을 반환하며, 데이터가 없는 요일은 매출/주문수가 0인 포인트로 채움.</p>
	 *
	 * <p><b>성능</b>: 단일 GROUP BY(weekday) 집계 1회. 인덱스: {@code (store_id, status, ordered_at)} 권장.</p>
	 *
	 * @param storeId   점포 ID
	 * @param startDate 조회 시작일(포함)
	 * @param endDate   조회 종료일(포함)
	 * @return 요일(1~7)별 {@link WeekdaySalesPointDto} 목록(빈 요일은 0 보정)
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	@Override
	@Transactional(readOnly = true)
	public List<WeekdaySalesPointDto> fetchWeekdayChart(Long storeId, LocalDate startDate, LocalDate endDate) {

		LocalDateTime startDT = startDate.atStartOfDay();
		LocalDateTime endExDT = endDate.plusDays(1).atStartOfDay();

		BooleanExpression base = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, startDT, endExDT));

		NumberExpression<Integer> weekdayExpr = weekDayKorean();
		NumberExpression<Integer> hourExpr = hourOfDay();
		BooleanExpression bizHours = businessHoursFilter(hourExpr);

		NumberExpression<BigDecimal> salesSumExpr = co.totalPrice.sum();
		NumberExpression<Long> orderCountExpr = co.id.countDistinct();

		List<Tuple> rows = query
				.select(weekdayExpr, salesSumExpr, orderCountExpr)
				.from(co)
				.join(co.store, s)
				.where(base, bizHours)
				.groupBy(weekdayExpr)
				.orderBy(weekdayExpr.asc())
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		Map<Integer, WeekdaySalesPointDto> map = new HashMap<>();
		for (Tuple t : rows) {
			Integer wd = t.get(weekdayExpr);
			if (wd == null) continue;
			BigDecimal salesBD = nvlBD(t.get(salesSumExpr));
			long sales = salesBD.longValue();
			long orders = nvlLong(t.get(orderCountExpr));
			map.put(wd, new WeekdaySalesPointDto(wd, sales, orders));
		}

		List<WeekdaySalesPointDto> result = new ArrayList<>();
		for (int wd = 1; wd <= 7; wd++) {
			WeekdaySalesPointDto p = map.get(wd);
			if (p == null) {
				p = new WeekdaySalesPointDto(wd, 0L, 0L);
			}
			result.add(p);
		}
		return result;
	}

	/**
	 * 시간/요일 분석의 일별 테이블(1행 = {@code [날짜, 요일, 시간대]})을 커서 기반으로 조회한다.
	 *
	 * <p><b>기간 규칙</b>: {@code [cond.startDate 00:00, cond.endDate+1 00:00)} (닫힌–열린 구간).</p>
	 * <p><b>영업시간 필터</b>: 07~20시만 집계한다.</p>
	 * <p><b>정렬</b>: 날짜 내림차순, 동일 날짜 내에서는 시간 오름차순.</p>
	 * <p><b>커서</b>: 문자열 {@code "YYYY-MM-DD|HH"} 형식.
	 *   <ul>
	 *     <li>다음 페이지 조건: {@code dayLabel &lt; cDate} OR ({@code dayLabel = cDate} AND {@code hour &gt; cHour})</li>
	 *     <li>{@code nextCursor}는 현재 페이지의 마지막 행 기준으로 동일 형식으로 반환</li>
	 *   </ul>
	 * </p>
	 *
	 * <p><b>집계 항목</b>:
	 * <ul>
	 *   <li>sales: 매출 합계</li>
	 *   <li>orderCount: 주문수(중복 제거)</li>
	 *   <li>visit/takeout/delivery: 채널별 주문수</li>
	 *   <li>visitRate/takeoutRate/deliveryRate: {@code 채널별주문수 / orderCount} (분모 0이면 0.0)</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>엣지 케이스</b>:
	 * <ul>
	 *   <li>집계 구간/영업시간에 데이터가 없으면 빈 페이지 및 {@code nextCursor = null}</li>
	 *   <li>요일 인덱스는 1~7(월=1)로 변환되며, NULL 방어를 위해 0으로 대체될 수 있다</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>성능</b>: GROUP BY(날짜, 요일, 시간) 1회. 권장 인덱스: {@code (store_id, status, ordered_at)}.</p>
	 *
	 * @param storeId 점포 ID
	 * @param cond    조회 조건(기간, 사이즈, 커서)
	 * @return 커서 페이지 {@link CursorPage}&lt;{@link TimeDayDailyRowDto}&gt;
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	@Override
	@Transactional(readOnly = true)
	public CursorPage<TimeDayDailyRowDto> fetchTimeDayDailyRows(Long storeId, AnalyticsSearchDto cond) {

		int size = (cond.size() == null ? 50 : cond.size());

		LocalDateTime startDT = cond.startDate().atStartOfDay();
		LocalDateTime endExDT = cond.endDate().plusDays(1).atStartOfDay();

		BooleanExpression base = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, startDT, endExDT));

		NumberExpression<Integer> hourExpr = hourOfDay();
		NumberExpression<Integer> weekdayExpr = weekDayKorean();
		BooleanExpression bizHours = businessHoursFilter(hourExpr);

		StringTemplate dayLabel = Expressions.stringTemplate(
				"DATE_FORMAT({0}, '%Y-%m-%d')", co.orderedAt
		);

		NumberExpression<BigDecimal> salesSumExpr = co.totalPrice.sum();
		NumberExpression<Long> orderCntExpr = co.id.countDistinct();

		NumberExpression<Long> visitCntExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.VISIT))
				.then(1L).otherwise(0L).sum();

		NumberExpression<Long> takeoutCntExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.TAKEOUT))
				.then(1L).otherwise(0L).sum();

		NumberExpression<Long> deliveryCntExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.DELIVERY))
				.then(1L).otherwise(0L).sum();

		// 커서: "YYYY-MM-DD|HH"
		BooleanExpression cursorFilter = null;
		String cursor = cond.cursor();
		if (cursor != null && cursor.contains("|")) {
			try {
				String[] parts = cursor.split("\\|");
				String cDate = parts[0];
				int cHour = Integer.parseInt(parts[1]);

				cursorFilter = dayLabel.lt(cDate)
						.or(
								dayLabel.eq(cDate)
										.and(hourExpr.gt(cHour))
						);
			} catch (Exception ignore) {
				cursorFilter = null;
			}
		}

		List<Tuple> rows = query
				.select(
						dayLabel,
						weekdayExpr,
						hourExpr,
						salesSumExpr,
						orderCntExpr,
						visitCntExpr,
						takeoutCntExpr,
						deliveryCntExpr
				)
				.from(co)
				.join(co.store, s)
				.where(base, bizHours, cursorFilter)
				.groupBy(dayLabel, weekdayExpr, hourExpr)
				.orderBy(dayLabel.desc(), hourExpr.asc())
				.limit(size + 1)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		List<TimeDayDailyRowDto> result = new ArrayList<>();
		String nextCursor = null;

		for (Tuple t : rows) {
			if (result.size() == size) {
				String d = t.get(dayLabel);
				Integer h = t.get(hourExpr);
				if (d != null && h != null) {
					nextCursor = d + "|" + h;
				}
				break;
			}

			String d = t.get(dayLabel);
			Integer wd = t.get(weekdayExpr);
			Integer h = t.get(hourExpr);

			BigDecimal salesBD = nvlBD(t.get(salesSumExpr));
			long sales = salesBD.longValue();
			long orderCnt = nvlLong(t.get(orderCntExpr));
			long visit = nvlLong(t.get(visitCntExpr));
			long takeout = nvlLong(t.get(takeoutCntExpr));
			long delivery = nvlLong(t.get(deliveryCntExpr));

			double visitRate = safeDiv(visit, orderCnt);
			double takeoutRate = safeDiv(takeout, orderCnt);
			double deliveryRate = safeDiv(delivery, orderCnt);

			result.add(new TimeDayDailyRowDto(
					d,
					wd == null ? 0 : wd,
					h == null ? 0 : h,
					orderCnt,
					sales,
					visit,
					takeout,
					delivery,
					visitRate,
					takeoutRate,
					deliveryRate
			));
		}

		return new CursorPage<>(result, nextCursor);
	}

	/**
	 * 시간/요일 분석의 월별 테이블(1행 = {@code [월, 요일, 시간대]})을 커서 기반으로 조회한다.
	 *
	 * <p><b>기간 규칙</b>: {@code [cond.startDate 00:00, cond.endDate+1 00:00)} (닫힌–열린 구간).</p>
	 * <p><b>영업시간 필터</b>: 07~20시만 집계한다.</p>
	 * <p><b>정렬</b>: 월(YYYY-MM) 내림차순 → 요일 오름차순(1~7, 월=1) → 시간 오름차순.</p>
	 * <p><b>커서</b>: 문자열 {@code "YYYY-MM|weekday|hour"} 형식.
	 *   <ul>
	 *     <li>다음 페이지 조건: {@code ym &lt; cYm} OR ({@code ym = cYm} AND ({@code weekday &gt; cWd} OR ({@code weekday = cWd} AND {@code hour &gt; cHour})))</li>
	 *     <li>{@code nextCursor}는 현재 페이지의 마지막 행 기준으로 동일 형식으로 반환</li>
	 *   </ul>
	 * </p>
	 *
	 * <p><b>집계 항목</b>:
	 * <ul>
	 *   <li>sales: 매출 합계</li>
	 *   <li>orderCount: 주문수(중복 제거)</li>
	 *   <li>visit/takeout/delivery: 채널별 주문수</li>
	 *   <li>visitRate/takeoutRate/deliveryRate: {@code 채널별주문수 / orderCount} (분모 0이면 0.0)</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>엣지 케이스</b>:
	 * <ul>
	 *   <li>집계 구간/영업시간에 데이터가 없으면 빈 페이지 및 {@code nextCursor = null}</li>
	 *   <li>요일/시간이 NULL인 경우 0으로 대체하여 반환</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>성능</b>: GROUP BY(월, 요일, 시간) 1회. 권장 인덱스: {@code (store_id, status, ordered_at)}.</p>
	 *
	 * @param storeId 점포 ID
	 * @param cond    조회 조건(기간, 사이즈, 커서)
	 * @return 커서 페이지 {@link CursorPage}&lt;{@link TimeDayMonthlyRowDto}&gt;
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	@Override
	@Transactional(readOnly = true)
	public CursorPage<TimeDayMonthlyRowDto> fetchTimeDayMonthlyRows(Long storeId, AnalyticsSearchDto cond) {

		int size = (cond.size() == null ? 50 : cond.size());

		LocalDateTime startDT = cond.startDate().atStartOfDay();
		LocalDateTime endExDT = cond.endDate().plusDays(1).atStartOfDay();

		BooleanExpression base = statusCompleted()
				.and(eqStore(storeId))
				.and(betweenClosedOpen(co.orderedAt, startDT, endExDT));

		NumberExpression<Integer> hourExpr = hourOfDay();
		NumberExpression<Integer> weekdayExpr = weekDayKorean();
		BooleanExpression bizHours = businessHoursFilter(hourExpr);

		StringTemplate ymLabel = Expressions.stringTemplate(
				"DATE_FORMAT({0}, '%Y-%m')", co.orderedAt
		);

		NumberExpression<BigDecimal> salesSumExpr = co.totalPrice.sum();
		NumberExpression<Long> orderCntExpr = co.id.countDistinct();

		NumberExpression<Long> visitCntExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.VISIT))
				.then(1L).otherwise(0L).sum();

		NumberExpression<Long> takeoutCntExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.TAKEOUT))
				.then(1L).otherwise(0L).sum();

		NumberExpression<Long> deliveryCntExpr = new CaseBuilder()
				.when(co.orderType.eq(OrderType.DELIVERY))
				.then(1L).otherwise(0L).sum();

		// 커서: "YYYY-MM|weekday|hour"
		BooleanExpression cursorFilter = null;
		String cursor = cond.cursor();
		if (cursor != null && !cursor.isBlank() && cursor.contains("|")) {
			try {
				String[] parts = cursor.split("\\|");
				String cYm = parts[0];
				int cWd = Integer.parseInt(parts[1]);
				int cHour = Integer.parseInt(parts[2]);

				BooleanExpression afterSameYm =
						weekdayExpr.gt(cWd)
								.or(
										weekdayExpr.eq(cWd)
												.and(hourExpr.gt(cHour))
								);

				cursorFilter = ymLabel.lt(cYm)
						.or(
								ymLabel.eq(cYm).and(afterSameYm)
						);
			} catch (Exception ignore) {
				cursorFilter = null;
			}
		}

		List<Tuple> rows = query
				.select(
						ymLabel,
						weekdayExpr,
						hourExpr,
						salesSumExpr,
						orderCntExpr,
						visitCntExpr,
						takeoutCntExpr,
						deliveryCntExpr
				)
				.from(co)
				.join(co.store, s)
				.where(base, bizHours, cursorFilter)
				.groupBy(ymLabel, weekdayExpr, hourExpr)
				.orderBy(
						ymLabel.desc(),
						weekdayExpr.asc(),
						hourExpr.asc()
				)
				.limit(size + 1)
				.setHint("org.hibernate.readOnly", true)
				.setHint("org.hibernate.flushMode", "COMMIT")
				.setHint("jakarta.persistence.query.timeout", 3000)
				.fetch();

		List<TimeDayMonthlyRowDto> result = new ArrayList<>();
		String nextCursor = null;

		for (Tuple t : rows) {
			if (result.size() == size) {
				String ym = t.get(ymLabel);
				Integer wd = t.get(weekdayExpr);
				Integer h = t.get(hourExpr);
				if (ym != null && wd != null && h != null) {
					nextCursor = ym + "|" + wd + "|" + h;
				}
				break;
			}

			String ym = t.get(ymLabel);
			Integer wd = t.get(weekdayExpr);
			Integer h = t.get(hourExpr);

			BigDecimal salesBD = nvlBD(t.get(salesSumExpr));
			long sales = salesBD.longValue();
			long orderCnt = nvlLong(t.get(orderCntExpr));
			long visit = nvlLong(t.get(visitCntExpr));
			long takeout = nvlLong(t.get(takeoutCntExpr));
			long delivery = nvlLong(t.get(deliveryCntExpr));

			double visitRate = safeDiv(visit, orderCnt);
			double takeoutRate = safeDiv(takeout, orderCnt);
			double deliveryRate = safeDiv(delivery, orderCnt);

			result.add(new TimeDayMonthlyRowDto(
					ym,
					wd == null ? 0 : wd,
					h == null ? 0 : h,
					orderCnt,
					sales,
					visit,
					takeout,
					delivery,
					visitRate,
					takeoutRate,
					deliveryRate
			));
		}

		return new CursorPage<>(result, nextCursor);
	}





	// ===== Helpers =====

	/**
	 * BigDecimal null-safe 치환 유틸리티.
	 *
	 * <p><b>정의</b>: 입력이 {@code null}이면 {@link BigDecimal#ZERO} 반환, 그렇지 않으면 원본 값 반환.</p>
	 * <p><b>용도</b>: SUM/AVG 등 집계 결과가 {@code null}일 수 있는 경우의 방어 코드.</p>
	 *
	 * @param v 입력 BigDecimal (null 가능)
	 * @return null이면 0, 아니면 원본 값
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private static BigDecimal nvlBD(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}

	/**
	 * Long null-safe 치환 유틸리티.
	 *
	 * <p><b>정의</b>: 입력이 {@code null}이면 0L 반환.</p>
	 * <p><b>용도</b>: COUNT 결과나 캐스팅 과정에서 {@code null} 가능성이 있는 경우.</p>
	 *
	 * @param v 입력 Long (null 가능)
	 * @return null이면 0L, 아니면 원본 값
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private static long nvlLong(Long v) {
		return v == null ? 0L : v;
	}

	/**
	 * 0으로 나누기 방지용 안전 나눗셈.
	 *
	 * <p><b>정의</b>: {@code den == 0}이면 0.0, 아니면 {@code num / den}의 double 결과.</p>
	 * <p><b>용도</b>: UPT/ADS/AUR 등 파생지표 계산 시 분모 0 방어.</p>
	 *
	 * @param num 분자
	 * @param den 분모
	 * @return 안전한 실수 나눗셈 결과
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private static double safeDiv(long num, long den) {
		return den == 0L ? 0.0 : (double) num / (double) den;
	}

	/**
	 * 소수점 첫째 자리 반올림 유틸리티.
	 *
	 * <p><b>정의</b>: {@code Math.round(v * 10.0) / 10.0}.</p>
	 * <p><b>용도</b>: % 지표(예: WoW%)와 같이 한 자리 소수 표현.</p>
	 *
	 * @param v 입력 값
	 * @return 소수점 1자리로 반올림된 값
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private static double round1(double v) {
		return Math.round(v * 10.0) / 10.0;
	}

	/**
	 * 주문 상태 COMPLETED 필터식.
	 *
	 * <p><b>정의</b>: {@code co.status = COMPLETED}.</p>
	 * <p><b>용도</b>: 모든 분석 쿼리의 기본 WHERE 조건.</p>
	 *
	 * @return COMPLETED 상태 비교 BooleanExpression
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private BooleanExpression statusCompleted() {
		return co.status.eq(OrderStatus.COMPLETED);
	}

	/**
	 * 단일 점포 스코프 필터식.
	 *
	 * <p><b>정의</b>: {@code s.id = :storeId}.</p>
	 * <p><b>용도</b>: 멀티테넌시/매장별 격리를 위한 기본 WHERE 조건.</p>
	 *
	 * @param storeId 점포 ID (null 안정성 보강)
	 * @return 점포 ID 일치 BooleanExpression
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private BooleanExpression eqStore(Long storeId) {
		return (storeId == null) ? null : s.id.eq(storeId);
	}

	/**
	 * 닫힌–열린 구간(Closed-Open) 기간 필터 생성기.
	 *
	 * <p><b>정의</b>: {@code start <= col < endEx}.</p>
	 * <p><b>권장</b>: 일자 구간을 시간 경계(자정)로 다룰 때 중복/누락 없이 안정적.</p>
	 *
	 * @param col   비교 대상 컬럼 (예: {@code co.orderedAt})
	 * @param start 포함 시작시각 (inclusive)
	 * @param endEx 배타 종료시각 (exclusive)
	 * @return 기간 필터 BooleanExpression
	 *
	 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
	 */
	private BooleanExpression betweenClosedOpen(
			DateTimePath<LocalDateTime> col,
			LocalDateTime start,
			LocalDateTime endEx
	) {
		return col.goe(start).and(col.lt(endEx));
	}

}
