package com.boot.ict05_final_user.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 주문 상태 변경 요청 DTO.
 *
 * <p>주문 ID 경로변수와 함께 사용되어 주문 상태를 변경합니다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "주문 상태 변경 요청 DTO")
public class UpdateStatusRequestDTO {

    /**
     * 변경할 주문 상태.
     *
     * <p>지원 값: PREPARING, COOKING, READY, COMPLETED, CANCELLED/CANCELED, PAID 등</p>
     */
    @Schema(
            description = "변경할 주문 상태",
            allowableValues = {
                    "PREPARING", "COOKING", "READY", "COMPLETED", "CANCELLED", "CANCELED", "PAID"
            },
            nullable = false
    )
    private String status;
}
