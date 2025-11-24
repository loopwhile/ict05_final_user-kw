package com.boot.ict05_final_user.domain.order.dto;

import com.boot.ict05_final_user.domain.order.entity.CustomerOrder;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 목록 화면에 표시되는 주문 요약 DTO.
 *
 * <p>주문 기본정보, 상태, 결제/유형, 품목 요약을 포함합니다.</p>
 */
@Getter
@Builder
@Schema(description = "주문 목록 요약 DTO")
public class CustomerOrderListDTO {

    @Schema(description = "주문 ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "주문 코드", accessMode = Schema.AccessMode.READ_ONLY)
    private String orderCode;

    @Schema(description = "주문 유형", allowableValues = {"VISIT", "TAKEOUT", "DELIVERY"})
    private String orderType;

    @Schema(description = "결제 수단", allowableValues = {"CARD", "CASH", "VOUCHER", "EXTERNAL"})
    private String paymentType;

    @Schema(description = "총 결제 금액")
    private BigDecimal totalPrice;

    @Schema(description = "주문 상태")
    private String status;

    @Schema(description = "주문(접수) 시각")
    private LocalDateTime orderDate;

    @Schema(description = "고객 이름/표시명")
    private String customerName;

    @Schema(description = "고객 연락처")
    private String customerPhone;

    @Schema(description = "배달 주소")
    private String deliveryAddress;

    @ArraySchema(arraySchema = @Schema(description = "주문 품목 목록"),
            schema = @Schema(implementation = CustomerOrderItemDTO.class))
    private List<CustomerOrderItemDTO> items;

    /**
     * 엔티티로부터 목록용 DTO를 생성합니다.
     *
     * @param order 주문 엔티티
     * @return 변환된 DTO
     */
    public static CustomerOrderListDTO from(CustomerOrder order) {

        // 주문 상세 → 메뉴 리스트 변환
        List<CustomerOrderItemDTO> items = order.getDetails().stream()
                .map(d -> CustomerOrderItemDTO.builder()
                        .menuId(d.getMenuIdFk().getMenuId())
                        .menuName(d.getMenuIdFk().getMenuName())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .build()
                )
                .toList();

        return CustomerOrderListDTO.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .orderType(order.getOrderType().name())
                .paymentType(order.getPaymentType().name())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .orderDate(order.getOrderedAt())
                .customerName(order.getMemo())
                .customerPhone(order.getCustomerPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .items(items)
                .build();
    }
}
