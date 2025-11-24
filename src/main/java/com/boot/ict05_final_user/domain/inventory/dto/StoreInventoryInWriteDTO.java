package com.boot.ict05_final_user.domain.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 입고(Inbound) 요청 DTO.
 *
 * <p>컨트롤러: POST /API/store/inventory/in</p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class StoreInventoryInWriteDTO {

    @NotNull(message = "storeInventoryId는 필수입니다.")
    private Long storeInventoryId;

    /** 소유권 교차검증/보조 식별자(선택). 지정 시 서비스에서 매칭 검증 수행 */
    @NotNull(message = "storeMaterialId는 필수입니다.")
    private Long storeMaterialId;

    @NotNull(message = "quantity는 필수입니다.")
    @PositiveOrZero(message = "quantity는 0 이상이어야 합니다.")
    private Double quantity;

    /** 선택 메모 */
    private String memo;

    /** 선택: HQ 재료는 미입력 가능(서비스가 HQ SELLING 또는 최근 입고가로 해석). 자체 재료는 미입력 시 거절. */
    @PositiveOrZero(message = "unitPrice는 0 이상이어야 합니다.")
    private Double unitPrice;
}
