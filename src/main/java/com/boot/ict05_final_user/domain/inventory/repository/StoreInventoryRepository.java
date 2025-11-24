package com.boot.ict05_final_user.domain.inventory.repository;

import com.boot.ict05_final_user.domain.inventory.entity.StoreInventory;
import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import com.boot.ict05_final_user.domain.store.entity.Store;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 가맹점 집계 재고(StoreInventory) 리포지토리.
 *
 * <p>역할</p>
 * <ul>
 *   <li>매장/재료 단위 집계 재고 조회 및 존재 여부 확인</li>
 *   <li>경합 방지를 위한 비관적 잠금 조회 제공(소진/조정 등에서 사용 가능)</li>
 * </ul>
 */
public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Long>, StoreInventoryRepositoryCustom {

    /** 매장별 집계 재고 목록 */
    List<StoreInventory> findByStore_Id(Long storeId);

    /** 특정 매장의 모든 집계 재고 */
    List<StoreInventory> findByStore(Store store);

    /** 매장-가맹점재료 기준 존재 여부 */
    boolean existsByStoreAndStoreMaterial(Store store, StoreMaterial storeMaterial);

    /** 매장-가맹점재료로 단건 조회 */
    Optional<StoreInventory> findByStore_IdAndStoreMaterial_Id(Long storeId, Long storeMaterialId);

    /** 서비스 시그니처 맞춤 프록시 */
    default Optional<StoreInventory> findByStoreIdAndStoreMaterialId(Long storeId, Long storeMaterialId) {
        return findByStore_IdAndStoreMaterial_Id(storeId, storeMaterialId);
    }

    /**
     * 경합 방지를 위한 비관적 잠금 조회.
     *
     * <p>용도</p>
     * <ul>
     *   <li>소진/조정 등 수량 갱신 전 행 잠금</li>
     *   <li>트랜잭션 경계 내에서 사용해야 함</li>
     * </ul>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")) // 5초
    @Query("""
       select si
       from StoreInventory si
       where si.store.id = :storeId and si.storeMaterial.id = :storeMaterialId
       """)
    Optional<StoreInventory> findByStoreIdAndStoreMaterialIdForUpdate(@Param("storeId") Long storeId,
                                                                      @Param("storeMaterialId") Long storeMaterialId);
}
