package com.boot.ict05_final_user.domain.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * 가맹점 재고 입고 요청 DTO
 *
 * <p>단일 품목에 대해 수량을 추가(입고)한다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreInventoryRestockRequest {

    /** 가맹점 재고 PK (store_inventory_id) */
    @NotNull(message = "재고 ID는 필수입니다.")
    private Long storeInventoryId;

    /** 재입고 수량 */
    @NotNull(message = "재입고 수량은 필수입니다.")
    @DecimalMin(value = "0.001", message = "재입고 수량은 0보다 커야 합니다.")
    private BigDecimal quantity;

    /** 메모 (선택) */
    private String memo;
    // 메모, 유통기한 등은 나중에 로그/배치 화면 붙일 때 확장
}
