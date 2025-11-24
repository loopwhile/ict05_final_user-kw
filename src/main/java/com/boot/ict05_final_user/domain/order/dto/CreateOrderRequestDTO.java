package com.boot.ict05_final_user.domain.order.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문 생성 요청 DTO.
 *
 * <p>
 * 가맹점 주문 생성 시 필요한 기본 정보를 담습니다.
 * 주문 유형/결제 수단 규격, 총액/할인, 품목 목록 등을 포함합니다.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "주문 생성 요청 DTO")
public class CreateOrderRequestDTO {

    /** 가맹점 ID (인증 정보로 대체 가능) */
    @Schema(description = "가맹점 ID", nullable = true)
    private Long storeId;

    /** 외부/프론트 생성 가능 주문 코드 */
    @Schema(description = "주문 코드", nullable = true)
    private String orderCode;

    /** 주문 유형 (대문자 규격) */
    @Schema(description = "주문 유형", allowableValues = {"VISIT", "TAKEOUT", "DELIVERY"}, nullable = false)
    private String orderType;

    /** 결제 수단 (소문자 규격) */
    @Schema(description = "결제 수단", allowableValues = {"card", "cash", "voucher", "external"}, nullable = false)
    private String paymentType;

    /** 총 결제 금액 */
    @Schema(description = "총 결제 금액", nullable = false)
    private BigDecimal totalPrice;

    /** 할인 금액 */
    @Schema(description = "할인 금액", nullable = true)
    private BigDecimal discount;

    /** 고객 식별/표시명(메모 용도 포함) */
    @Schema(description = "고객 이름/표시명", nullable = true)
    private String customerName;

    /** 주문 품목 목록 */
    @ArraySchema(arraySchema = @Schema(description = "주문 품목 목록", nullable = false),
            schema = @Schema(implementation = OrderItemRequest.class))
    private List<OrderItemRequest> items;

    /**
     * 주문 품목 요청 DTO.
     *
     * <p>메뉴 ID, 수량, 단가로 구성됩니다.</p>
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "주문 품목 요청 DTO")
    public static class OrderItemRequest {

        /** 메뉴 ID */
        @Schema(description = "메뉴 ID", nullable = false)
        private Long menuId;

        /** 주문 수량 */
        @Schema(description = "수량", nullable = false)
        private Integer quantity;

        /** 품목 단가 */
        @Schema(description = "단가", nullable = false)
        private BigDecimal unitPrice;
    }
}
