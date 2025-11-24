package com.boot.ict05_final_user.domain.order.service;

import com.boot.ict05_final_user.domain.inventory.dto.StoreConsumeRequestDTO;
import com.boot.ict05_final_user.domain.inventory.repository.StoreMaterialRepository;
import com.boot.ict05_final_user.domain.inventory.service.StoreConsumptionService;
import com.boot.ict05_final_user.domain.menu.service.MenuUsageCalculator;
import com.boot.ict05_final_user.domain.menu.service.MenuUsageMaterialLogService;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrder;
import com.boot.ict05_final_user.domain.order.entity.OrderStatus;
import com.boot.ict05_final_user.domain.order.repository.CustomerOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 주문 상태 전이와 그에 따른 재고 차감/사용 로그 기록을 담당하는 서비스.
 *
 * <p><b>전이 규칙</b></p>
 * <ul>
 *   <li>{@link OrderStatus#PREPARING} → {@link OrderStatus#COOKING} 전이 시 재고 차감 수행</li>
 *   <li>그 외 전이는 상태만 갱신</li>
 * </ul>
 *
 * <p><b>재고 차감 흐름</b></p>
 * <ol>
 *   <li>레시피 기반 필요 수량 집계({@link MenuUsageCalculator})</li>
 *   <li>소비 요청 DTO 변환 후 판매 소진 처리({@link StoreConsumptionService})</li>
 *   <li>사용 로그 기록({@link MenuUsageMaterialLogService})</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private final CustomerOrderRepository orderRepo;                 // 주문 저장소
    private final MenuUsageCalculator usageCalculator;               // 레시피 기반 필요 수량 계산기
    private final StoreConsumptionService storeConsumptionService;   // 재고 차감(판매 소진)
    private final StoreMaterialRepository storeMaterialRepository;   // materialId -> storeMaterialId 매핑
    private final MenuUsageMaterialLogService usageLogService;       // 사용 로그 기록

    /**
     * 주문 상태를 갱신한다. 필요 시 재고 차감을 수행한다.
     *
     * <p>규칙: {@code PREPARING → COOKING} 전이에서만 재고 차감/로그 기록을 트리거한다.</p>
     *
     * @param orderId 주문 ID
     * @param next    다음 상태
     * @throws IllegalArgumentException 주문이 존재하지 않을 때
     */
    @Transactional
    public void updateStatus(Long orderId, OrderStatus next) {
        CustomerOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));

        OrderStatus prev = order.getStatus();
        order.setStatus(next);

        if (prev == OrderStatus.PREPARING && next == OrderStatus.COOKING) {
            applyUsage(order); // 조리 시작 시 재고 차감
        }
    }

    /**
     * 레시피에 따른 필요 수량을 집계하고, 판매 소진 처리 및 사용 로그를 기록한다.
     *
     * <p>처리 순서:</p>
     * <ol>
     *   <li>필요 수량 집계: materialId → 총 필요 수량</li>
     *   <li>상관키 생성</li>
     *   <li>소비 서비스 호출</li>
     *   <li>사용 로그 기록</li>
     * </ol>
     *
     * @param order 대상 주문
     */
    private void applyUsage(CustomerOrder order) {
        // 1) 주문 전체 필요 재료 합계 (materialId -> 총필요수량)
        Map<Long, BigDecimal> need = usageCalculator.calcMaterialsForOrder(order);

        // 2) 상관키 (멱등/추적용)
        String corr = "ORDER-" + order.getId();

        // 3) StoreConsumptionService 가 요구하는 DTO로 변환해서 호출
        Long storeId = order.getStore().getId();
        StoreConsumeRequestDTO req = toConsumeRequest(storeId, need, corr);
        storeConsumptionService.consume(storeId, req);

        // 4) 사용 로그 기록
        usageLogService.logDeduct(order, need, corr);
    }

    /**
     * 필요 수량 맵(materialId → qty)을 재고 소비 서비스가 요구하는
     * DTO(storeMaterialId → qty 라인 리스트)로 변환한다.
     *
     * @param storeId        매장 ID
     * @param need           재료별 필요 수량 맵
     * @param correlationId  상관키
     * @return 판매 소진 요청 DTO
     * @throws IllegalArgumentException 매장-재료 매핑이 없을 때
     */
    private StoreConsumeRequestDTO toConsumeRequest(Long storeId,
                                                    Map<Long, BigDecimal> need,
                                                    String correlationId) {

        StoreConsumeRequestDTO dto = new StoreConsumeRequestDTO();
        dto.setSaleAt(LocalDateTime.now());                // 이벤트 시각
        dto.setMemo("ORDER " + correlationId);             // 메모로 상관키 남김

        List<StoreConsumeRequestDTO.Line> lines = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> e : need.entrySet()) {
            Long materialId = e.getKey();                  // 본사 재료 PK
            BigDecimal qty = e.getValue();                 // 필요 수량

            // storeId + materialId -> storeMaterialId 조회
            Long storeMaterialId = storeMaterialRepository
                    .findByStore_IdAndMaterial_Id(storeId, materialId)
                    .orElseThrow(() ->
                            new IllegalArgumentException("StoreMaterial not found. storeId="
                                    + storeId + ", materialId=" + materialId))
                    .getId();

            // DTO 라인 구성
            StoreConsumeRequestDTO.Line line = new StoreConsumeRequestDTO.Line();
            line.setStoreMaterialId(storeMaterialId);
            line.setQuantity(qty);

            lines.add(line);
        }

        dto.setLines(lines);
        return dto;
    }
}
