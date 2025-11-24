package com.boot.ict05_final_user.domain.home.repository;

import com.boot.ict05_final_user.domain.menu.entity.QMenu;
import com.boot.ict05_final_user.domain.menu.entity.QMenuCategory;
import com.boot.ict05_final_user.domain.order.entity.OrderStatus;
import com.boot.ict05_final_user.domain.order.entity.OrderType;
import com.boot.ict05_final_user.domain.order.entity.QCustomerOrder;
import com.boot.ict05_final_user.domain.order.entity.QCustomerOrderDetail;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class HomeRepositoryCustomImpl implements HomeRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public BigDecimal sumSales(LocalDateTime start, LocalDateTime end, Long storeId, List<OrderStatus> statuses) {
        QCustomerOrder customerOrder = QCustomerOrder.customerOrder;
        BigDecimal ret = queryFactory
                .select(customerOrder.totalPrice.sum())
                .from(customerOrder)
                .where(
                        customerOrder.orderedAt.goe(start),
                        customerOrder.orderedAt.lt(end),
                        storeId == null ? null : customerOrder.store.id.eq(storeId),
                        customerOrder.status.in(statuses)
                )
                .fetchOne();
        return ret == null ? BigDecimal.ZERO : ret;
    }

    @Override
    public long countOrders(LocalDateTime start, LocalDateTime end, Long storeId, List<OrderStatus> statuses) {
        QCustomerOrder customerOrder = QCustomerOrder.customerOrder;
        Long ret = queryFactory
                .select(customerOrder.count())
                .from(customerOrder)
                .where(
                        customerOrder.orderedAt.goe(start),
                        customerOrder.orderedAt.lt(end),
                        storeId == null ? null : customerOrder.store.id.eq(storeId),
                        customerOrder.status.in(statuses)
                )
                .fetchOne();
        return ret == null ? 0L : ret;
    }

    @Override
    public long countVisitOrders(LocalDateTime start, LocalDateTime end, Long storeId, List<OrderStatus> statuses) {
        QCustomerOrder customerOrder = QCustomerOrder.customerOrder;
        Long ret = queryFactory
                .select(customerOrder.count())
                .from(customerOrder)
                .where(
                        customerOrder.orderedAt.goe(start),
                        customerOrder.orderedAt.lt(end),
                        storeId == null ? null : customerOrder.store.id.eq(storeId),
                        customerOrder.status.in(statuses),
                        customerOrder.orderType.eq(OrderType.VISIT)
                )
                .fetchOne();
        return ret == null ? 0L : ret;
    }

    private BooleanExpression storeEq(Long storeId) {
        if (storeId == null) return null;

        return QCustomerOrder.customerOrder.store.id.eq(storeId);
    }

    @Override
    public List<TopMenuAgg> findTopMenus(LocalDateTime start, LocalDateTime end, Long storeId, int limit, List<OrderStatus> statuses) {
        QCustomerOrder customerOrder = QCustomerOrder.customerOrder;
        QCustomerOrderDetail customerOrderDetail = QCustomerOrderDetail.customerOrderDetail;
        QMenuCategory menuCategory = QMenuCategory.menuCategory;
        QMenu menu = QMenu.menu;

        var rows = queryFactory
                .select(
                        customerOrderDetail.menuIdFk.menuId,
                        customerOrderDetail.menuIdFk.menuName,
                        menu.menuCategory.menuCategoryName,        // ← 카테고리명
                        customerOrderDetail.quantity.sum(),
                        customerOrderDetail.lineTotal.sum()
                )
                .from(customerOrderDetail)
                .join(customerOrderDetail.order, customerOrder)
                .join(customerOrderDetail.menuIdFk, menu)
                .join(menu.menuCategory, menuCategory)         // ← 조인
                .where(
                        customerOrder.orderedAt.goe(start),
                        customerOrder.orderedAt.lt(end),
                        storeEq(storeId),
                        customerOrder.status.in(statuses)
                )
                .groupBy(
                        customerOrderDetail.menuIdFk.menuId,
                        customerOrderDetail.menuIdFk.menuName,
                        menu.menuCategory.menuCategoryName         // ← group by에도 포함
                )
                .orderBy(
                        customerOrderDetail.quantity.sum().desc(),
                        customerOrderDetail.lineTotal.sum().desc()
                )
                .limit(limit)
                .fetch();

        return rows.stream().map(t -> new TopMenuAgg(
                t.get(customerOrderDetail.menuIdFk.menuId),
                t.get(customerOrderDetail.menuIdFk.menuName),
                t.get(menu.menuCategory.menuCategoryName),
                nzl(t.get(customerOrderDetail.quantity.sum())),
                toLongSafe(t.get(customerOrderDetail.lineTotal.sum()))
        )).toList();
    }

    @Override
    public List<HourlyAgg> aggregateHourly(LocalDateTime start, LocalDateTime end, Long storeId, List<OrderStatus> statuses) {
        QCustomerOrder customerOrder = QCustomerOrder.customerOrder;
        QCustomerOrderDetail customerOrderDetail = QCustomerOrderDetail.customerOrderDetail;
        // HOUR(o.orderedAt)
        NumberExpression<Integer> hh = Expressions.numberTemplate(Integer.class, "HOUR({0})", customerOrder.orderedAt);

        // CASE WHEN … THEN 1 ELSE 0 END 의 합계는 when().then().otherwise()로
        NumberExpression<Integer> visitSum =
                customerOrder.orderType.when(OrderType.VISIT).then(1).otherwise(0).sum();
        NumberExpression<Integer> takeoutSum =
                customerOrder.orderType.when(OrderType.TAKEOUT).then(1).otherwise(0).sum();
        NumberExpression<Integer> deliverySum =
                customerOrder.orderType.when(OrderType.DELIVERY).then(1).otherwise(0).sum();

        List<Tuple> rows = queryFactory
                .select(
                        hh,                                      // 0
                        customerOrder.totalPrice.sum(),          // 1 BigDecimal
                        customerOrder.count(),                   // 2 Long
                        visitSum,                                // 3 Integer
                        takeoutSum,                              // 4 Integer
                        deliverySum                              // 5 Integer
                )
                .from(customerOrder)
                .where(
                        customerOrder.orderedAt.goe(start),
                        customerOrder.orderedAt.lt(end),
                        storeId == null ? null : customerOrder.store.id.eq(storeId),
                        customerOrder.status.in(statuses)
                )
                .groupBy(hh)
                .orderBy(hh.asc())
                .fetch();

        return rows.stream().map(t -> new HourlyAgg(
                nni(t.get(hh)),                                              // hour
                toLongSafe(t.get(customerOrder.totalPrice.sum())),           // sales
                nzl(t.get(customerOrder.count())),                           // orders
                nzl(t.get(visitSum)),                                        // visitOrders
                nzl(t.get(takeoutSum)),                                      // takeoutOrders
                nzl(t.get(deliverySum))                                      // deliveryOrders
        )).toList();
    }

    // ===== helpers =====
    private static long toLongSafe(Number n) {
        if (n == null) return 0L;
        if (n instanceof BigDecimal bd) return bd.longValue();
        return n.longValue();
    }
    private static int nni(Number n) { return n == null ? 0 : n.intValue(); }
    private static long nzl(Number n) { return n == null ? 0L : n.longValue(); }
}