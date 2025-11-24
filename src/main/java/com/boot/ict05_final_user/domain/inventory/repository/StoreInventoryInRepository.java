package com.boot.ict05_final_user.domain.inventory.repository;

import com.boot.ict05_final_user.domain.inventory.entity.StoreInventoryIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 가맹점 입고(StoreInventoryIn) 리포지토리.
 *
 * <p>역할</p>
 * <ul>
 *   <li>입고 이력 저장/조회 등 기본 CRUD</li>
 *   <li>가맹점 재료별 최신(단건) 조회 헬퍼 제공</li>
 * </ul>
 *
 * <p>최신 레코드 판정 기준</p>
 * <ul>
 *   <li><b>권장</b>: {@code createdAt} 내림차순 기준 최상위(시간 정확도가 보장되는 경우)</li>
 *   <li>보조: {@code id} 내림차순 기준 최상위(단조 증가 PK를 최신으로 간주하는 경우)</li>
 * </ul>
 */
public interface StoreInventoryInRepository
        extends JpaRepository<StoreInventoryIn, Long>, StoreInventoryInRepositoryCustom {
    /**
     * 최신 1건(엔티티) – <b>createdAt DESC</b> 기준.
     * <p>권장: 시간 컬럼을 기준으로 명시적으로 최신 레코드를 선택.</p>
     */
    Optional<StoreInventoryIn> findFirstByStoreMaterial_IdOrderByCreatedAtDesc(Long storeMaterialId);

    /**
     * [Deprecated] 최근 입고 <b>단가</b>만 반환.
     * 내부적으로 최신 1건 조회에 위임하여 중복 로직을 제거한다.
     * <p>대체: 서비스단에서 {@link #findFirstByStoreMaterial_IdOrderByCreatedAtDesc(Long)} 호출 후
     * {@code getUnitPrice()}를 직접 참조.</p>
     */
    @Deprecated(forRemoval = false)
    default Optional<BigDecimal> findLatestUnitPriceByStoreMaterialId(Long storeMaterialId) {
        return findFirstByStoreMaterial_IdOrderByCreatedAtDesc(storeMaterialId)
                .map(StoreInventoryIn::getUnitPrice);
    }
}
