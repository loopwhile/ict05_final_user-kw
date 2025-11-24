package com.boot.ict05_final_user.domain.order.repository;

import com.boot.ict05_final_user.domain.order.entity.CustomerOrder;
import com.boot.ict05_final_user.domain.order.entity.OrderStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long>, CustomerOrderRepositoryCustom  {

    // 최신 주문 하나
    Optional<CustomerOrder> findTopByOrderByIdDesc();

    // 주방에서 사용할 주문 목록 조회 (가맹점 기준 + 상태 in)
    List<CustomerOrder> findByStore_IdAndStatusInOrderByOrderedAtAsc(
            Long storeId,
            List<OrderStatus> statuses
    );

    // 주문 + 매장 + 디테일 + 디테일의 메뉴를 한 번에 로딩
    @EntityGraph(attributePaths = {"store", "details", "details.menuIdFk"})
    Optional<CustomerOrder> findById(Long id);
}

