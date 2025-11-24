package com.boot.ict05_final_user.domain.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 재고 조정(절대 수량 설정) 요청 DTO.
 *
 * <p>컨트롤러: POST /API/store/inventory/adjust</p>
 * <ul>
 *   <li>storeInventoryId: 대상 집계 재고 PK (필수)</li>
 *   <li>storeMaterialId: 가맹점 재료 PK (선택, 소유권 교차검증용)</li>
 *   <li>newQuantity: 조정 후 최종 수량(절대값, 0 이상)</li>
 *   <li>reason: 조정 사유(필수) — 예: MANUAL, DAMAGE, LOSS, ERROR, REAL_AUDIT 등</li>
 *   <li>memo: 선택 메모</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class StoreInventoryAdjustmentWriteDTO {

    @NotNull(message = "storeInventoryId는 필수입니다.")
    private Long storeInventoryId;

    /** 선택: 소유권/추적 보조. null 허용 */
    private Long storeMaterialId;

    @NotNull(message = "newQuantity는 필수입니다.")
    @PositiveOrZero(message = "newQuantity는 0 이상이어야 합니다.")
    private Double newQuantity;

    @NotBlank(message = "reason은(는) 필수입니다.")
    private String reason;

    /** 선택 메모 */
    private String memo;
}
