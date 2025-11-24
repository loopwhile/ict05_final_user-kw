package com.boot.ict05_final_user.domain.menu.repository;

import com.boot.ict05_final_user.domain.menu.entity.StoreMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreMenuRepository extends JpaRepository<StoreMenu, Long> {

    // storeId + menuId 로 한 건 조회
    @Query("""
        select sm
        from StoreMenu sm
        where sm.store.id = :storeId
          and sm.menu.menuId   = :menuId
    """)
    Optional<StoreMenu> findByStoreIdAndMenuId(
            @Param("storeId") Long storeId,
            @Param("menuId") Long menuId
    );

    // 특정 가맹점의 전체 메뉴
    @Query("""
        select sm
        from StoreMenu sm
        where sm.store.id = :storeId
    """)
    List<StoreMenu> findByStoreId(@Param("storeId") Long storeId);
}
