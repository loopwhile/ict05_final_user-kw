package com.boot.ict05_final_user.domain.inventory.dto;

import com.boot.ict05_final_user.domain.inventory.entity.InventoryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 재입고 처리 결과 DTO
 */
@Getter
@AllArgsConstructor
public class StoreInventoryRestockResponse {

    private Long storeInventoryId;
    private Long storeId;
    private Long storeMaterialId;
    private BigDecimal quantityAfter;      // 재입고 후 수량
    private InventoryStatus statusAfter;   // 재입고 후 상태
}
