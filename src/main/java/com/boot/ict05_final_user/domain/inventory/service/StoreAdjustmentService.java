package com.boot.ict05_final_user.domain.inventory.service;

import com.boot.ict05_final_user.domain.inventory.dto.StoreInventoryAdjustmentWriteDTO;
import com.boot.ict05_final_user.domain.inventory.entity.AdjustmentReason;
import com.boot.ict05_final_user.domain.inventory.entity.InventoryRecordStatus;
import com.boot.ict05_final_user.domain.inventory.entity.StoreInventory;
import com.boot.ict05_final_user.domain.inventory.entity.StoreInventoryAdjustment;
import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import com.boot.ict05_final_user.domain.inventory.repository.StoreInventoryAdjustmentRepository;
import com.boot.ict05_final_user.domain.inventory.repository.StoreInventoryRepository;
import com.boot.ict05_final_user.domain.inventory.repository.StoreMaterialRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StoreAdjustmentService {

    private final StoreInventoryRepository storeInventoryRepository;
    private final StoreInventoryAdjustmentRepository storeInventoryAdjustmentRepository;
    private final StoreMaterialRepository storeMaterialRepository;

    @Transactional
    public Long adjust(final Long storeId, final StoreInventoryAdjustmentWriteDTO req) {
        Objects.requireNonNull(storeId, "storeId must not be null");
        Objects.requireNonNull(req, "request must not be null");

        final Long storeMaterialId = req.getStoreMaterialId();
        final BigDecimal newQty = scale3(BigDecimal.valueOf(req.getNewQuantity()));
        final AdjustmentReason reason = AdjustmentReason.valueOf(req.getReason().trim().toUpperCase());
        final LocalDateTime at = LocalDateTime.now();

        // 대상 검증
        final StoreMaterial sm = storeMaterialRepository.findById(storeMaterialId)
                .orElseThrow(() -> new EntityNotFoundException("StoreMaterial not found. id=" + storeMaterialId));
        if (!sm.getStore().getId().equals(storeId)) {
            throw new EntityNotFoundException("StoreMaterial store mismatch. storeId=" + storeId + ", storeMaterialId=" + storeMaterialId);
        }

        final StoreInventory inv = storeInventoryRepository
                .findByStoreIdAndStoreMaterialId(storeId, storeMaterialId)
                .orElseThrow(() -> new EntityNotFoundException("StoreInventory not found. storeId=" + storeId + ", storeMaterialId=" + storeMaterialId));

        final BigDecimal before = nz(inv.getQuantity());
        final BigDecimal after = newQty;
        final BigDecimal diff = after.subtract(before).setScale(3, RoundingMode.HALF_UP);

        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("조정 후 수량이 0 미만이다. after=" + after);
        }

        // 조정 이력 생성
        StoreInventoryAdjustment adj = StoreInventoryAdjustment.builder()
                // 엔티티 필드명이 storeInventory 라고 가정
                .storeInventory(inv)
                .quantityBefore(before)
                .quantityAfter(after)
                .difference(diff)
                // unitPrice 필드 사용 안 함
                .memo(req.getMemo())
                .reason(reason)
                .createdAt(at)
                .status(InventoryRecordStatus.CONFIRMED)
                .build();
        storeInventoryAdjustmentRepository.save(adj);

        // 집계 재고 갱신
        inv.setQuantity(after);
        storeInventoryRepository.save(inv);

        return adj.getId();
    }

    private static BigDecimal nz(final BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal scale3(final BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(3, RoundingMode.HALF_UP);
    }
}
