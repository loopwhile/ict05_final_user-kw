package com.boot.ict05_final_user.domain.inventory.repository;

import com.boot.ict05_final_user.domain.inventory.entity.StoreInventoryOut;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 가맹점 출고(StoreInventoryOut) 리포지토리.
 * 현재 사용 범위: 기본 CRUD.
 */
public interface StoreInventoryOutRepository extends JpaRepository<StoreInventoryOut, Long> {
    // 동시성 제어가 필요해지면 이후에 @Lock 메서드를 추가한다.
}
