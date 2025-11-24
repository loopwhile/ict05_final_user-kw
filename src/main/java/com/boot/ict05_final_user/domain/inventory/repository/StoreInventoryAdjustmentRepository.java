package com.boot.ict05_final_user.domain.inventory.repository;

import com.boot.ict05_final_user.domain.inventory.entity.StoreInventoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 가맹점 재고 조정(StoreInventoryAdjustment) 리포지토리.
 *
 * <p>용도</p>
 * <ul>
 *   <li>조정 이력 저장/조회 등 기본 CRUD 제공.</li>
 * </ul>
 *
 * <p>모형</p>
 * <ul>
 *   <li>조정 이력은 절대 수량 재설정의 전/후/차이를 기록한다.</li>
 *   <li>일반적으로 {@code status=CONFIRMED} 로 저장되며 트랜잭션은 서비스 계층에서 관리한다.</li>
 * </ul>
 */
public interface StoreInventoryAdjustmentRepository extends JpaRepository<StoreInventoryAdjustment, Long> {
    // 필요 시 도메인 파생 쿼리 또는 Custom 확장을 추가한다.
}
