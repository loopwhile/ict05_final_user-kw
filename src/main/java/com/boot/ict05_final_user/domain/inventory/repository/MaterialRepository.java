package com.boot.ict05_final_user.domain.inventory.repository;

import com.boot.ict05_final_user.domain.inventory.entity.Material;
import com.boot.ict05_final_user.domain.inventory.entity.MaterialStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 본사 재료(Material) 표준 리포지토리.
 *
 * <p>역할</p>
 * <ul>
 *   <li>기본 CRUD: {@link JpaRepository}</li>
 *   <li>상태 기반 조회: {@link #findByMaterialStatus(MaterialStatus)}</li>
 *   <li>확장 쿼리: {@link MaterialRepositoryCustom} (QueryDSL 등 구현체에서 처리)</li>
 * </ul>
 *
 * <p>주의</p>
 * <ul>
 *   <li>본사 재료는 가맹점 재료(StoreMaterial) 매핑의 소스가 된다. 운영 상태가 {@link MaterialStatus#USE} 인 항목만
 *   가맹점으로 동기화하는 정책을 기본으로 가정한다.</li>
 * </ul>
 */
public interface MaterialRepository extends JpaRepository<Material, Long> {

    /**
     * 재료 운영 상태로 필터링하여 조회한다.
     *
     * @param status {@link MaterialStatus} (예: USE)
     * @return 상태 일치하는 재료 리스트
     */
    List<Material> findByMaterialStatus(MaterialStatus status);
}
