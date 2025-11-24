package com.boot.ict05_final_user.domain.order.repository;

import com.boot.ict05_final_user.domain.order.entity.CustomerOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderDetailRepository
        extends JpaRepository<CustomerOrderDetail, Long> {

    List<CustomerOrderDetail> findByOrder_Id(Long orderId);
}
