package com.boot.ict05_final_user.domain.store.repository;

import com.boot.ict05_final_user.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Store 엔티티용 Spring Data JPA 리포지토리 인터페이스.
 *
 * <p>특징</p>
 * <ul>
 *   <li>{@link JpaRepository} 상속: 기본 CRUD, 페이징/정렬 메서드 자동 제공</li>
 *   <li>{@code StoreRepositoryCustom} 상속: 복잡한 동적쿼리/커스텀 메서드 구현 분리</li>
 * </ul>
 */
public interface StoreRepository extends JpaRepository<Store, Long>, StoreRepositoryCustom {
}
