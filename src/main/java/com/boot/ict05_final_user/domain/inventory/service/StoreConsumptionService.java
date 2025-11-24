package com.boot.ict05_final_user.domain.inventory.service;

import com.boot.ict05_final_user.domain.inventory.dto.StoreConsumeRequestDTO;
import com.boot.ict05_final_user.domain.inventory.entity.InventoryRecordStatus;
import com.boot.ict05_final_user.domain.inventory.entity.StoreInventory;
import com.boot.ict05_final_user.domain.inventory.entity.StoreInventoryOut;
import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import com.boot.ict05_final_user.domain.inventory.repository.StoreInventoryOutRepository;
import com.boot.ict05_final_user.domain.inventory.repository.StoreInventoryRepository;
import com.boot.ict05_final_user.domain.inventory.repository.StoreMaterialRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 가맹점 "판매 소진(Consumption)" 트랜잭션 도메인 서비스.
 *
 * <p>역할</p>
 * <ul>
 *   <li>이미 가맹점 재료(StoreMaterial) 기준으로 정규화된 소진 라인들을 받아 재고를 차감한다.</li>
 *   <li>차감 결과를 {@link StoreInventoryOut} 이력으로 기록한다(단가 미관리).</li>
 *   <li>단위 변환/레시피 조회/본사 재료 매핑은 사전 단계에서 완료되었다고 가정한다.</li>
 * </ul>
 *
 * <p>규칙</p>
 * <ul>
 *   <li>수량 스케일: 소수점 셋째 자리(DECIMAL(15,3)), 반올림 모드: HALF_UP.</li>
 *   <li>음수 재고 허용하지 않음: 처리 전 선검증, 처리 중에도 2차 방어 로직으로 재확인.</li>
 *   <li>소진 이력 단가는 관리하지 않는다(매장 판매가/원가 산정은 별도 모듈).</li>
 *   <li>이력 상태는 {@link InventoryRecordStatus#CONFIRMED}로 저장.</li>
 * </ul>
 *
 * <p>트랜잭션</p>
 * <ul>
 *   <li>{@link #consume(Long, StoreConsumeRequestDTO)}: 쓰기 트랜잭션. 라인별 집계 재고 갱신 + 이력 저장을 단일 트랜잭션으로 커밋.</li>
 * </ul>
 *
 * <p>동시성</p>
 * <ul>
 *   <li>본 구현은 라인별 조회→검증→차감 순서를 따른다. 동시 소진이 빈번한 경우에는 비관/낙관 잠금, 버전 필드, 또는 재시도 정책을 상위 계층에서 고려하라.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class StoreConsumptionService {

    private final StoreInventoryRepository storeInventoryRepository;
    private final StoreInventoryOutRepository storeInventoryOutRepository;
    private final StoreMaterialRepository storeMaterialRepository;

    /**
     * 판매 소진 처리.
     *
     * <p>처리 흐름</p>
     * <ol>
     *   <li>요청 유효성 검사 및 이벤트 시각 확정(미지정 시 now).</li>
     *   <li>선검증: 모든 라인에 대해 음수 재고 발생 가능성 사전 차단.</li>
     *   <li>라인 반복: 집계 재고 차감 → {@link StoreInventoryOut} 이력 생성/저장.</li>
     * </ol>
     *
     * <p>예외</p>
     * <ul>
     *   <li>{@link EntityNotFoundException}: StoreMaterial 또는 StoreInventory 미존재, 혹은 매장 불일치.</li>
     *   <li>{@link IllegalStateException}: 재고 부족(선검증 또는 처리 중 재확인에 의해 발생).</li>
     *   <li>{@link NullPointerException}: storeId 또는 request가 null.</li>
     * </ul>
     *
     * @param storeId 가맹점 ID(소유 검증용)
     * @param request 소진 요청(가맹점 재료 기준으로 정규화된 라인 목록)
     */
    @Transactional
    public void consume(final Long storeId, final StoreConsumeRequestDTO request) {
        Objects.requireNonNull(storeId, "storeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        final LocalDateTime eventAt = Optional.ofNullable(request.getSaleAt()).orElseGet(LocalDateTime::now);

        final List<StoreConsumeRequestDTO.Line> lines = request.getLines();
        if (lines == null || lines.isEmpty()) return;

        // 1) 선검증: 모든 라인에 대해 음수 재고 방지 (현재고 < 요구수량 이면 전체 요청 거절)
        for (StoreConsumeRequestDTO.Line line : lines) {
            final StoreInventory inv = findInventoryByStoreMaterialId(storeId, line.getStoreMaterialId());
            final BigDecimal current = nz(inv.getQuantity());
            if (current.compareTo(line.getQuantity()) < 0) {
                throw new IllegalStateException("재고 부족. storeMaterialId=" + line.getStoreMaterialId()
                        + ", current=" + current + ", required=" + line.getQuantity());
            }
        }

        // 2) 라인별 차감 및 이력 생성
        for (StoreConsumeRequestDTO.Line line : lines) {
            final Long storeMaterialId = line.getStoreMaterialId();
            final BigDecimal outQty = scale3(line.getQuantity());

            final StoreInventory inv = findInventoryByStoreMaterialId(storeId, storeMaterialId);
            final BigDecimal before = nz(inv.getQuantity());
            final BigDecimal after = before.subtract(outQty);
            if (after.compareTo(BigDecimal.ZERO) < 0) {
                // 동시성 경합 등으로 인한 추가 방어
                throw new IllegalStateException("재고 부족. 경합 발생. storeMaterialId=" + storeMaterialId
                        + ", before=" + before + ", out=" + outQty);
            }

            // 집계 재고 갱신
            inv.setQuantity(after);
            storeInventoryRepository.save(inv);

            // 소진 이력 저장 (단가 미사용)
            final StoreInventoryOut out = StoreInventoryOut.builder()
                    .store(inv.getStore())
                    .storeMaterial(inv.getStoreMaterial())
                    .quantity(outQty)
                    .stockAfter(after)
                    .unitPrice(null) // 매장 판매가/원가 이력 비관리
                    .memo(Optional.ofNullable(request.getMemo()).orElse("SALE"))
                    .outDate(eventAt)
                    .status(InventoryRecordStatus.CONFIRMED)
                    .build();
            storeInventoryOutRepository.save(out);
        }
    }

    /**
     * 가맹점 소유 검증 포함: StoreMaterial 존재/소유 확인 후 StoreInventory 조회.
     */
    private StoreInventory findInventoryByStoreMaterialId(final Long storeId, final Long storeMaterialId) {
        final StoreMaterial sm = storeMaterialRepository.findById(storeMaterialId)
                .orElseThrow(() -> new EntityNotFoundException("StoreMaterial not found. id=" + storeMaterialId));
        if (!sm.getStore().getId().equals(storeId)) {
            throw new EntityNotFoundException("StoreMaterial의 매장 불일치. storeId=" + storeId + ", storeMaterialId=" + storeMaterialId);
        }
        return storeInventoryRepository.findByStoreIdAndStoreMaterialId(storeId, storeMaterialId)
                .orElseThrow(() -> new EntityNotFoundException("StoreInventory not found. storeId=" + storeId + ", storeMaterialId=" + storeMaterialId));
    }

    /**
     * null 안전 BigDecimal. null 이면 0 반환.
     */
    private static BigDecimal nz(final BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 수량 스케일을 소수점 셋째 자리로 고정하고 HALF_UP 반올림.
     */
    private static BigDecimal scale3(final BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(3, RoundingMode.HALF_UP);
    }
}
