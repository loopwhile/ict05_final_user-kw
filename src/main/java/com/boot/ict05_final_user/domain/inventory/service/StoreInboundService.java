package com.boot.ict05_final_user.domain.inventory.service;

import com.boot.ict05_final_user.domain.inventory.dto.StoreInventoryInWriteDTO;
import com.boot.ict05_final_user.domain.inventory.entity.StoreInventory;
import com.boot.ict05_final_user.domain.inventory.entity.StoreInventoryIn;
import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import com.boot.ict05_final_user.domain.inventory.repository.StoreInventoryInRepository;
import com.boot.ict05_final_user.domain.inventory.repository.StoreInventoryRepository;
import com.boot.ict05_final_user.domain.inventory.repository.StoreMaterialRepository;
import com.boot.ict05_final_user.domain.inventory.repository.UnitPriceJdbcRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * 가맹점 <b>입고(Inbound)</b> 도메인 서비스.
 *
 * <p>역할</p>
 * <ul>
 *   <li>집계 재고(StoreInventory) 수량 증가 및 입고 이력(StoreInventoryIn) 생성</li>
 *   <li>입고 단가 결정 규칙 적용: <i>요청 단가 → HQ 판매가 → 매장 최근 입고가</i></li>
 * </ul>
 *
 * <p>수량/스케일</p>
 * <ul>
 *   <li>수량/단가는 0 이상</li>
 *   <li>수량 스케일: DECIMAL(15,3), 반올림: HALF_UP</li>
 *   <li><code>stockAfter</code>는 집계 반영 이후 수량</li>
 * </ul>
 *
 * <p>동시성</p>
 * <ul>
 *   <li>집계 갱신 전 <b>비관적 잠금</b>으로 대상 행을 확보:
 *       <code>StoreInventoryRepository.findByStoreIdAndStoreMaterialIdForUpdate(...)</code></li>
 *   <li>본 메서드는 @Transactional 경계 내에서 동작해야 함</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class StoreInboundService {

    private final StoreInventoryRepository storeInventoryRepository;
    private final StoreMaterialRepository storeMaterialRepository;
    private final StoreInventoryInRepository storeInventoryInRepository;
    private final UnitPriceJdbcRepository unitPriceJdbcRepository;

    /**
     * 입고 처리.
     *
     * <p>흐름</p>
     * <ol>
     *   <li>대상 StoreInventory/StoreMaterial 검증(매장 소유 포함)</li>
     *   <li>입고 단가 결정(요청값 → HQ 판매가 → 매장 최근 입고가)</li>
     *   <li>집계 재고 수량 증가 및 입고 이력 저장</li>
     * </ol>
     *
     * <p>예외</p>
     * <ul>
     *   <li>{@link IllegalArgumentException}: 대상 미존재, 권한 불일치, 음수 입력 등</li>
     *   <li>{@link EntityNotFoundException}: 내부 조회 실패(상황에 따라)</li>
     * </ul>
     *
     * @param storeId 가맹점 ID(인증 컨텍스트)
     * @param dto     입고 요청 DTO(재고/재료/수량/단가/메모)
     * @return 생성된 입고 이력 PK
     */
    @Transactional
    public Long inbound(final Long storeId, final StoreInventoryInWriteDTO dto) {
        Objects.requireNonNull(storeId, "storeId must not be null");
        Objects.requireNonNull(dto, "dto must not be null");

        // 1) 검증/로딩 — PESSIMISTIC_WRITE (매장+가맹점재료 기준 단건 고정)
        final StoreInventory inv = storeInventoryRepository
                .findByStoreIdAndStoreMaterialIdForUpdate(storeId, dto.getStoreMaterialId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "재고가 존재하지 않습니다. (storeId=%d, storeMaterialId=%d)".formatted(storeId, dto.getStoreMaterialId())
                ));

        // 방어적 소유권 점검(쿼리로 이미 제한되지만 이중 방어)
        if (!inv.getStore().getId().equals(storeId)) {
            throw new IllegalArgumentException("해당 재고에 대한 권한이 없습니다. storeId=" + storeId);
        }

        // 가맹점 재료(소유 매장 일치 확인)
        final StoreMaterial sm = storeMaterialRepository
                .findByIdAndStore_Id(dto.getStoreMaterialId(), storeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "재료가 존재하지 않거나 권한이 없습니다. storeMaterialId=" + dto.getStoreMaterialId()
                ));

        // 2) 단가 결정
        final BigDecimal resolvedUnitPrice = resolveUnitPrice(sm, dto);

        // 3) 수량 계산 및 집계 반영
        final BigDecimal inQty = scale3(BigDecimal.valueOf(dto.getQuantity()));
        if (inQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("입고 수량은 0 이상이어야 합니다. qty=" + inQty);
        }

        final BigDecimal before = nz(inv.getQuantity());
        final BigDecimal after  = before.add(inQty).setScale(3, RoundingMode.HALF_UP);

        inv.setQuantity(after);
        storeInventoryRepository.save(inv);

        // 4) 입고 이력 저장 (증가 이후의 수량 기록)
        final StoreInventoryIn in = StoreInventoryIn.builder()
                .store(inv.getStore())
                .storeMaterial(sm)
                .quantity(inQty)
                .memo(dto.getMemo())
                .unitPrice(resolvedUnitPrice)
                .stockAfter(after)
                .inDate(LocalDateTime.now()) // DTO에 날짜가 있다면 교체 가능
                .build();
        storeInventoryInRepository.save(in);

        // 5) (선택) 단가 이력 정책에 따라 별도 테이블 적재 필요 시 별도 리포지토리 활용
        // ex) storeUnitPriceRepository.save(...);

        return in.getId();
    }

    /**
     * 입고 단가 결정 규칙:
     * <ol>
     *   <li>요청 단가가 존재하면 최우선(0 이상)</li>
     *   <li>HQ 재료: 요청 단가가 있더라도 HQ SELLING <b>미만이면 거절</b>
     *       (정책: HQ 판매가 미만 매입 불가)</li>
     *   <li>요청 단가가 없을 때는 HQ SELLING → 매장 최근 입고가</li>
     *   <li>자체 재료: 요청 단가(0 이상) 없으면 매장 최근 입고가, 둘 다 없으면 입력 요구</li>
     * </ol>
     */
    private BigDecimal resolveUnitPrice(final StoreMaterial sm, final StoreInventoryInWriteDTO dto) {
        // 공통: 음수 거절 + 스케일 정규화 헬퍼
        final var toScaled = (java.util.function.Function<BigDecimal, BigDecimal>)
                v -> v == null ? BigDecimal.ZERO : v.setScale(3, RoundingMode.HALF_UP);

        final boolean isHqMaterial = (sm.getMaterial() != null);

        // 0) 요청 단가가 온 경우
        if (dto.getUnitPrice() != null) {
            if (dto.getUnitPrice() < 0) {
                throw new IllegalArgumentException("입고 단가는 0 이상이어야 합니다. unitPrice=" + dto.getUnitPrice());
            }
            final BigDecimal req = toScaled.apply(BigDecimal.valueOf(dto.getUnitPrice()));

            // HQ 재료 정책: 요청 단가 < HQ SELLING 이면 거절
            if (isHqMaterial) {
                final Optional<BigDecimal> sellingOpt =
                        unitPriceJdbcRepository.findLatestSellingPriceByMaterialId(sm.getMaterial().getId());
                if (sellingOpt.isPresent()) {
                    final BigDecimal selling = toScaled.apply(sellingOpt.get());
                    if (req.compareTo(selling) < 0) {
                        throw new IllegalArgumentException(
                                "HQ 재료의 입고 단가는 HQ 판매가 미만으로 설정할 수 없습니다. "
                                        + "(요청=" + req + ", HQ_Selling=" + selling + ")"
                        );
                    }
                } else {
                    // HQ 재료이지만 판매가 미존재: 정책상 입력 허용(요청 단가 사용)
                    // 필요 시 여기서도 거절하도록 강화 가능
                }
            }

            return req;
        }

        // 1) 요청 단가가 없고, HQ 재료인 경우: HQ SELLING → 매장 최근 입고가
        if (isHqMaterial) {
            final Optional<BigDecimal> selling = unitPriceJdbcRepository
                    .findLatestSellingPriceByMaterialId(sm.getMaterial().getId());
            if (selling.isPresent()) {
                return toScaled.apply(selling.get());
            }
            final Optional<BigDecimal> lastStoreInPrice = storeInventoryInRepository
                    .findFirstByStoreMaterial_IdOrderByCreatedAtDesc(sm.getId())
                    .map(StoreInventoryIn::getUnitPrice);
            if (lastStoreInPrice.isPresent()) {
                return toScaled.apply(lastStoreInPrice.get());
            }
            throw new IllegalArgumentException("본사 판매가가 설정되지 않았습니다. 입고 단가를 입력하세요.");
        }

        // 2) 자체 재료: 최근 입고가, 없으면 입력 요구
        return toScaled.apply(
                storeInventoryInRepository
                        .findFirstByStoreMaterial_IdOrderByCreatedAtDesc(sm.getId())
                        .map(StoreInventoryIn::getUnitPrice)
                        .orElseThrow(() -> new IllegalArgumentException("입고 단가를 입력하세요. (자체 재료)"))
        );
    }

    /* ===== 공통 유틸 ===== */

    private static BigDecimal nz(final BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal scale3(final BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(3, RoundingMode.HALF_UP);
    }
}
