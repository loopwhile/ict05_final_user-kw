package com.boot.ict05_final_user.domain.inventory.service;

import com.boot.ict05_final_user.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;

@Disabled("1회 시딩 완료")
@SpringBootTest
class StoreMaterialServiceSeedAllStoresTest {

    @Autowired private StoreMaterialService storeMaterialService;
    @Autowired private StoreRepository storeRepository;

    /**
     * 실행 1회용 시딩: 본사 재료 → 전 가맹점 매핑 + 재고 0 초기화
     * - 실행 후 데이터가 남도록 @Commit 적용
     * - 완료 후 테스트에 @Disabled 붙여 비활성화 권장
     */
    @Test
    @Commit
    @DisplayName("전 가맹점 시딩: HQ→Store 매핑 + 재고 0 생성")
    // @Disabled("1회 시딩 완료 후 비활성화")
    void seed_allStores_mapAndInit() {
        // 가맹점 ID 1..34 대상. 존재 확인 후만 실행.
        for (long storeId = 1; storeId <= 34; storeId++) {
            if (!storeRepository.existsById(storeId)) continue;

            // 1) 본사 재료 일괄 매핑 (USE 상태 HQ 재료만)
            storeMaterialService.mapAllHqMaterialsToStore(storeId);

            // 2) 누락 재고 0 보정 (store_material 대비 store_inventory 1:1 보장)
            storeMaterialService.initStoreInventoryForStore(storeId);
        }
    }
}
