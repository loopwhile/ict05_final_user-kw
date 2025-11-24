package com.boot.ict05_final_user.domain.inventory.repository;

import com.boot.ict05_final_user.domain.inventory.entity.Material;
import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import com.boot.ict05_final_user.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 가맹점 재료(StoreMaterial) 리포지토리.
 *
 * <p>정책</p>
 * <ul>
 *   <li>파생 메서드 네이밍은 엔티티 필드명과 동일하게 유지한다.</li>
 *   <li>소유 매장 검증이 필요한 조회는 <b>store.id</b> 조건을 함께 사용한다.</li>
 * </ul>
 *
 * <p>주요 메서드</p>
 * <ul>
 *   <li>소유 매장 검증 포함 단건 조회 → {@link #findByIdAndStore_Id(Long, Long)}</li>
 *   <li>HQ 재료 매핑 중복 체크 → {@link #existsByStoreAndMaterial(Store, Material)}</li>
 *   <li>가맹점 자체 코드 중복 체크 → {@link #existsByStoreAndCode(Store, String)}</li>
 *   <li>매장 전체 목록 → {@link #findByStore(Store)}</li>
 *   <li>매장+HQ재료 단건 조회 → {@link #findByStore_IdAndMaterial_Id(Long, Long)}</li>
 * </ul>
 */
@Repository
public interface StoreMaterialRepository extends JpaRepository<StoreMaterial, Long> {

    /** 가맹점 자체 코드 중복 체크 */
    boolean existsByStoreAndCode(Store store, String code);

    /** HQ 재료 매핑 중복 체크 */
    boolean existsByStoreAndMaterial(Store store, Material material);

    /** 매장 기준 전체 목록 */
    List<StoreMaterial> findByStore(Store store);

    /**
     * 소유 매장 검증 포함 단건 조회.
     *
     * <p>전제: {@code Store}의 식별자 필드명은 {@code id}.</p>
     *
     * @param id      store_material PK
     * @param storeId store PK
     */
    Optional<StoreMaterial> findByIdAndStore_Id(Long id, Long storeId);

    /**
     * 매장 + HQ 재료 기준 단건 조회.
     *
     * @param storeId    store PK
     * @param materialId hq material PK
     */
    Optional<StoreMaterial> findByStore_IdAndMaterial_Id(Long storeId, Long materialId);
}
