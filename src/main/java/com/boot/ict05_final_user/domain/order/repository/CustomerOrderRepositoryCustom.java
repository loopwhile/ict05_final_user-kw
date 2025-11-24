package com.boot.ict05_final_user.domain.order.repository;

import com.boot.ict05_final_user.domain.order.dto.CustomerOrderSearchDTO;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerOrderRepositoryCustom {

    // 로그인한 가맹점 기준 주문 검색 + 페이징
    Page<CustomerOrder> searchOrders(Long storeId,
                                     CustomerOrderSearchDTO cond,
                                     Pageable pageable);
}
