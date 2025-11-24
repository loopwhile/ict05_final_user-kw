package com.boot.ict05_final_user.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 주문 생성 응답 DTO.
 *
 * <p>주문 생성 결과로 반환되는 식별자 및 코드 정보를 포함합니다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "주문 생성 응답 DTO")
public class CreateOrderResponseDTO {

    /** 주문 ID */
    @Schema(description = "주문 ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long orderId;

    /** 주문 코드 */
    @Schema(description = "주문 코드", accessMode = Schema.AccessMode.READ_ONLY)
    private String orderCode;
}
