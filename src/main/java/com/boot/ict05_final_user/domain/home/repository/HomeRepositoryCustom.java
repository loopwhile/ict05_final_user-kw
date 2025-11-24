package com.boot.ict05_final_user.domain.home.repository;


import com.boot.ict05_final_user.domain.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface HomeRepositoryCustom {

    BigDecimal sumSales(LocalDateTime start, LocalDateTime end, Long storeId, List<OrderStatus> statuses);

    long countOrders(LocalDateTime start, LocalDateTime end, Long storeId, List<OrderStatus> statuses);

    long countVisitOrders(LocalDateTime start, LocalDateTime end, Long storeId, List<OrderStatus> statuses);

    List<TopMenuAgg> findTopMenus(LocalDateTime start, LocalDateTime end, Long storeId, int limit, List<OrderStatus> statuses);

    List<HourlyAgg> aggregateHourly(LocalDateTime start, LocalDateTime end, Long storeId, List<OrderStatus> statuses);

    // ===== Projections =====
    record TopMenuAgg(Long menuId, String name, String categoryName, long qty, long sales) {}
    record HourlyAgg(Integer hour, long sales, long orders, long visitOrders, long takeoutOrders, long deliveryOrders) {}

}
