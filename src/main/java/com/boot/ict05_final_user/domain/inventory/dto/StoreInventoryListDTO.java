package com.boot.ict05_final_user.domain.inventory.dto;

import com.boot.ict05_final_user.domain.inventory.entity.InventoryStatus;
import com.boot.ict05_final_user.domain.inventory.entity.StoreInventory;
import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 가맹점 재고 목록 DTO
 * StoreInventory + StoreMaterial 요약본
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreInventoryListDTO {

    /** store_inventory_id */
    private Long id;

    /** store_material_id */
    private Long storeMaterialId;

    /** 품목명 (가맹점 표시명) */
    private String name;

    /** 카테고리 (문자열) */
    private String category;

    /** 현재 수량 */
    private BigDecimal quantity;

    /** 적정 수량 */
    private BigDecimal optimalQuantity;

    /** 재고 상태 */
    private InventoryStatus status;

    /** 소진 단위 (ea, g 등) */
    private String baseUnit;

    /** 공급업체 */
    private String supplier;

    /** 최근 매입단가 */
    private BigDecimal purchasePrice;

    public static StoreInventoryListDTO from(StoreInventory si) {
        StoreMaterial sm = si.getStoreMaterial();

        return StoreInventoryListDTO.builder()
                .id(si.getId())
                .storeMaterialId(sm != null ? sm.getId() : null)
                .name(sm != null ? sm.getName() : null)
                .category(sm != null ? sm.getCategory() : null)
                .quantity(si.getQuantity())
                .optimalQuantity(si.getOptimalQuantity())
                .status(si.getStatus())
                .baseUnit(sm != null ? sm.getBaseUnit() : null)
                .supplier(sm != null ? sm.getSupplier() : null)
                .purchasePrice(sm != null ? sm.getPurchasePrice() : null)
                .build();
    }
}
