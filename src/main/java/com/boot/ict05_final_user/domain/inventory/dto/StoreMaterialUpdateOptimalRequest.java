package com.boot.ict05_final_user.domain.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.*;

/**
 * 가맹점 재료 적정재고(최소 재고) 업데이트 요청 DTO
 *
 * <p>소진 단위 기준의 적정재고 수량을 설정한다. null일 경우 미설정으로 간주한다.</p>
 *
 * @author …
 * @since 2025-11-20
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreMaterialUpdateOptimalRequest {

    /**
     * 적정 재고 수량(소진 단위)
     *
     * <p>null 허용(미설정). 값이 있을 경우 0 이상.</p>
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "적정 재고는 0 이상이어야 합니다.")
    private Double optimalQuantity; // 프런트 number 매핑 일관성 유지(필요시 BigDecimal로 교체)
}
