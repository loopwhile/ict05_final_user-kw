package com.boot.ict05_final_user.domain.menu.service;

import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import com.boot.ict05_final_user.domain.inventory.repository.StoreMaterialRepository;
import com.boot.ict05_final_user.domain.menu.entity.MenuUsageMaterialLog;
import com.boot.ict05_final_user.domain.menu.repository.MenuUsageMaterialLogRepository;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 메뉴 제작 시 실제 소진된 매장 재료 사용량을 로그로 적재하는 서비스.
 *
 * <p>
 * 입력으로 받은 재료별 필요 수량을 기준으로 매장-재료 매핑을 확인하고,
 * {@link MenuUsageMaterialLog} 엔티티를 생성하여 저장합니다.
 * 단위 문자열은 호출 측 정책에 맞게 보강 가능합니다.
 * </p>
 *
 * <p><b>에러 처리</b>: 매장-재료 매핑이 존재하지 않을 경우 {@link IllegalArgumentException} 발생.</p>
 */
@Service
@RequiredArgsConstructor
public class MenuUsageMaterialLogService {

    private final MenuUsageMaterialLogRepository logRepo;
    private final StoreMaterialRepository storeMaterialRepo; // 재료 FK를 채우기 위해 필요

    /**
     * 주문에 따른 재료 소진 로그를 적재합니다.
     *
     * <p>
     * 처리 절차:
     * <ol>
     *   <li>주문의 매장 식별자 확보</li>
     *   <li>재료 ID별 필요 수량을 순회</li>
     *   <li>매장-재료 매핑 조회(없으면 예외)</li>
     *   <li>단위 문자열 설정 후 로그 엔티티 생성 및 저장</li>
     * </ol>
     * </p>
     *
     * @param order              대상 주문
     * @param needByMaterialId   재료 ID → 필요 수량 매핑
     * @param correlationId      상관키(추적용 식별자). 로그 memo 필드에 기록
     * @throws IllegalArgumentException 매장-재료 매핑이 없을 때
     */
    @Transactional
    public void logDeduct(CustomerOrder order,
                          Map<Long, BigDecimal> needByMaterialId,
                          String correlationId) {

        Long storeId = order.getStore().getId(); // 매장 id

        for (Map.Entry<Long, BigDecimal> e : needByMaterialId.entrySet()) {
            Long materialId = e.getKey();
            BigDecimal qty = e.getValue();         // 사용 수량

            // 1) 매장-재료 FK 찾아오기
            StoreMaterial sm = storeMaterialRepo
                    .findByStore_IdAndMaterial_Id(storeId, materialId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "StoreMaterial not found. storeId=" + storeId + ", materialId=" + materialId));

            // 2) 단위 채우기
            String unit = "BASE";

            // 3) 로그 엔티티 구성 (엔티티 구조에 맞게 FK 레퍼런스 주입)
            MenuUsageMaterialLog log = MenuUsageMaterialLog.builder()
                    .customerOrderFk(order)          // 주문 엔티티
                    .menuFk(null)                    // 메뉴 특정이 필요 없을 경우 null
                    .storeMaterialFk(sm)             // 매장-재료 FK
                    .count(qty)                      // 수량
                    .unit(unit)                      // 단위 문자열
                    .memo(correlationId)             // 상관키를 메모에 기록
                    .build();

            // 4) 저장
            logRepo.save(log);
        }
    }
}
