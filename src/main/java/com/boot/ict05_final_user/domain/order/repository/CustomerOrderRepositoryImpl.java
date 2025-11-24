package com.boot.ict05_final_user.domain.order.repository;

import com.boot.ict05_final_user.domain.order.dto.CustomerOrderSearchDTO;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrder;
import com.boot.ict05_final_user.domain.order.entity.QCustomerOrder;
import com.boot.ict05_final_user.domain.order.entity.QCustomerOrderDetail;
import com.boot.ict05_final_user.domain.menu.entity.QMenu;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link CustomerOrderRepositoryCustom} 구현체.
 *
 * <p>QueryDSL을 사용하여 가맹점 기준 주문 목록을 검색/필터/정렬/페이징 처리합니다.</p>
 *
 * <ul>
 *   <li>가맹점 필터(storeId) 강제</li>
 *   <li>기간 프리셋(today/week/month/all) 필터</li>
 *   <li>상태/결제수단/주문유형 필터</li>
 *   <li>키워드: 주문코드/메모/전화번호 또는 주문 상세의 메뉴명 검색</li>
 *   <li>정렬: 기본 id DESC, 요청 Sort 반영</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class CustomerOrderRepositoryImpl implements CustomerOrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 가맹점(storeId) 기준으로 주문을 검색합니다.
     *
     * <p>검색 조건이 null이면 기본값으로 처리하며, 기간/상태/결제수단/유형/키워드 필터를 조합합니다.
     * 키워드 검색 시 주문코드/메모/전화번호 또는 주문 상세의 메뉴명을 대상으로 OR 검색합니다.</p>
     *
     * @param storeId  가맹점 ID(필수)
     * @param cond     검색/필터 조건 DTO(Null 허용)
     * @param pageable 페이징/정렬 정보
     * @return 주문 페이지 결과
     * @throws IllegalArgumentException storeId가 null인 경우
     */
    @Override
    public Page<CustomerOrder> searchOrders(Long storeId, CustomerOrderSearchDTO cond, Pageable pageable) {
        if (storeId == null) throw new IllegalArgumentException("storeId is required");

        QCustomerOrder order = QCustomerOrder.customerOrder;
        QCustomerOrderDetail detail = QCustomerOrderDetail.customerOrderDetail;
        QMenu menu = QMenu.menu;

        if (cond == null) cond = new CustomerOrderSearchDTO();

        BooleanBuilder where = new BooleanBuilder();
        where.and(order.store.id.eq(storeId));

        BooleanExpression periodExpr = buildPeriodExpr(order, cond.getPeriod());
        if (periodExpr != null) where.and(periodExpr);

        if (StringUtils.hasText(cond.getStatus()) && !"all".equalsIgnoreCase(cond.getStatus())) {
            where.and(order.status.stringValue().eq(cond.getStatus().toUpperCase()));
        }
        if (StringUtils.hasText(cond.getPaymentType()) && !"all".equalsIgnoreCase(cond.getPaymentType())) {
            where.and(order.paymentType.stringValue().eq(cond.getPaymentType().toUpperCase()));
        }
        if (StringUtils.hasText(cond.getOrderType()) && !"all".equalsIgnoreCase(cond.getOrderType())) {
            where.and(order.orderType.stringValue().eq(cond.getOrderType().toUpperCase()));
        }

        if (StringUtils.hasText(cond.getKeyword())) {
            String kw = cond.getKeyword().trim();
            BooleanExpression inOrder =
                    order.orderCode.containsIgnoreCase(kw)
                            .or(order.memo.containsIgnoreCase(kw))
                            .or(order.customerPhone.containsIgnoreCase(kw));
            BooleanExpression inMenu = menu.menuName.containsIgnoreCase(kw);

            where.and(
                    inOrder.or(
                            order.id.in(
                                    queryFactory.select(detail.order.id)
                                            .from(detail)
                                            .leftJoin(detail.menuIdFk, menu)
                                            .where(inMenu)
                            )
                    )
            );
        }

        Sort sort = pageable.getSort().isUnsorted()
                ? Sort.by(Sort.Direction.DESC, "id")
                : pageable.getSort();

        List<CustomerOrder> content = queryFactory
                .selectFrom(order)
                .where(where)
                .orderBy(sort.getOrderFor("id").isAscending() ? order.id.asc() : order.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(order.id.count())
                .from(order)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 기간 프리셋(today/week/month/all)에 따른 주문일시 필터식을 생성합니다.
     *
     * <p>종료 시각은 익일 00:00으로 설정하여 당일 포함 범위를 닫힌 구간처럼 처리합니다.</p>
     *
     * @param order  QCustomerOrder
     * @param period 기간 프리셋 문자열
     * @return 기간 필터 식 또는 null(all/비어있음)
     */
    private BooleanExpression buildPeriodExpr(QCustomerOrder order, String period) {
        LocalDate today = LocalDate.now();

        if (!StringUtils.hasText(period) || "all".equalsIgnoreCase(period)) {
            return null;
        }

        LocalDateTime start;
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        switch (period) {
            case "today" -> start = today.atStartOfDay();
            case "week"  -> start = today.minusDays(6).atStartOfDay();
            case "month" -> start = today.withDayOfMonth(1).atStartOfDay();
            default -> { return null; }
        }

        return order.orderedAt.between(start, end);
        // between은 시작/끝 모두 포함(스펙상)이나, end를 익일 00:00으로 설정해 당일 범위를 자연스럽게 커버
    }
}
