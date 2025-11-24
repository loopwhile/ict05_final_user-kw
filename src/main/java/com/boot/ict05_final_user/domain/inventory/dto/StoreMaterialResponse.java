package com.boot.ict05_final_user.domain.inventory.dto;

import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import com.boot.ict05_final_user.domain.inventory.entity.MaterialStatus;
import com.boot.ict05_final_user.domain.inventory.entity.MaterialTemperature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMaterialResponse {

    private Long id;
    private Long storeId;
    private String code;
    private String name;

    private String category;              // StoreMaterial.category (문자열)
    private String baseUnit;
    private String salesUnit;

    private String supplier;
    private MaterialTemperature temperature;
    private MaterialStatus status;

    private BigDecimal optimalQuantity;
    private BigDecimal purchasePrice;

    private boolean hqMaterial;

    public static StoreMaterialResponse from(StoreMaterial sm) {
        return StoreMaterialResponse.builder()
                .id(sm.getId())
                .storeId(sm.getStore().getId())
                .code(sm.getCode())
                .name(sm.getName())
                .category(sm.getCategory()) // String
                .baseUnit(sm.getBaseUnit())
                .salesUnit(sm.getSalesUnit())
                .supplier(sm.getSupplier())
                .temperature(sm.getTemperature())
                .status(sm.getStatus())
                .optimalQuantity(sm.getOptimalQuantity())
                .purchasePrice(sm.getPurchasePrice())
                .hqMaterial(sm.isHqMaterial())
                .build();
    }
}
