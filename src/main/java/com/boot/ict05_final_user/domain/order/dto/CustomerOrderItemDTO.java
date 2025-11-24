package com.boot.ict05_final_user.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 주문 상세 내 단일 품목을 표현하는 DTO.
 *
 * <p>메뉴 식별자/이름, 수량, 단가 정보를 포함합니다.</p>
 */
@Getter
@Builder
@Schema(description = "주문 상세 품목 DTO")
public class CustomerOrderItemDTO {

    @Schema(description = "메뉴 ID", nullable = false)
    private Long menuId;

    @Schema(description = "메뉴 이름", nullable = false)
    private String menuName;

    @Schema(description = "수량", nullable = false)
    private Integer quantity;

    @Schema(description = "단가(BigDecimal)", nullable = false)
    private BigDecimal unitPrice;
}
