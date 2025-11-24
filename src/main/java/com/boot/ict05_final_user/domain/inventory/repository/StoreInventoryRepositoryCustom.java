package com.boot.ict05_final_user.domain.inventory.repository;

/**
 * 가맹점 집계 재고 커스텀 쿼리 확장 포인트.
 *
 * <p>예시</p>
 * <ul>
 *   <li>카테고리/온도대/상태별 집계</li>
 *   <li>예측 수요 기반 가용 재고 산출</li>
 * </ul>
 */
public interface StoreInventoryRepositoryCustom {
    // Optional<BigDecimal> findProjectedQtyByStoreMaterial(Long storeId, Long storeMaterialId);
}
