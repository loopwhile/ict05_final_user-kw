package com.boot.ict05_final_user.domain.order.dto;

import com.boot.ict05_final_user.domain.order.entity.CustomerOrder;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrderDetail;
import com.boot.ict05_final_user.domain.order.entity.OrderStatus;
import com.boot.ict05_final_user.domain.order.entity.OrderType;
import com.boot.ict05_final_user.domain.order.entity.PaymentType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 상세 응답 DTO.
 *
 * <p>주문 기본정보, 결제/상태, 품목 목록 및 금액 정보를 포함합니다.</p>
 */
@Data
@Schema(description = "주문 상세 응답 DTO")
public class CustomerOrderDetailDTO {

    @Schema(description = "주문 ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long orderId;

    @Schema(description = "주문 코드", accessMode = Schema.AccessMode.READ_ONLY)
    private String orderCode;

    @Schema(description = "주문(접수) 시각", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime orderedAt;

    @Schema(description = "고객 이름/표시명")
    private String customerName;

    @Schema(description = "고객 연락처")
    private String customerPhone;

    @Schema(description = "주문 상태", implementation = OrderStatus.class)
    private OrderStatus status;

    @Schema(description = "주문 유형", implementation = OrderType.class)
    private OrderType orderType;

    @Schema(description = "결제 수단", implementation = PaymentType.class)
    private PaymentType paymentType;

    @Schema(description = "결제 총액")
    private int totalPrice;

    @Schema(description = "할인 금액")
    private int discount;

    @Schema(description = "할인 전 금액(총액+할인)")
    private int originalTotal;

    @ArraySchema(arraySchema = @Schema(description = "주문 품목 목록"),
            schema = @Schema(implementation = ItemDTO.class))
    private List<ItemDTO> items;

    /**
     * 주문 상세 품목 DTO.
     *
     * <p>메뉴 ID/이름, 단가, 수량, 라인 합계를 포함합니다.</p>
     */
    @Data
    @Schema(description = "주문 상세 품목 DTO")
    public static class ItemDTO {

        @Schema(description = "메뉴 ID")
        private Long menuId;

        @Schema(description = "메뉴 이름")
        private String menuName;

        @Schema(description = "단가")
        private int unitPrice;

        @Schema(description = "수량")
        private int quantity;

        @Schema(description = "라인 합계(단가×수량)")
        private int lineTotal;

        public static ItemDTO from(CustomerOrderDetail d) {
            ItemDTO dto = new ItemDTO();
            dto.setMenuId(d.getMenuIdFk().getMenuId());
            dto.setMenuName(d.getMenuIdFk().getMenuName());
            dto.setUnitPrice(d.getUnitPrice().intValue());
            dto.setQuantity(d.getQuantity());
            dto.setLineTotal(d.getUnitPrice().intValue() * d.getQuantity());
            return dto;
        }
    }

    /**
     * 엔티티에서 주문 상세 DTO로 변환합니다.
     *
     * @param order   주문 엔티티
     * @param details 주문 품목 엔티티 리스트
     * @return 변환된 DTO
     */
    public static CustomerOrderDetailDTO from(CustomerOrder order, List<CustomerOrderDetail> details) {
        CustomerOrderDetailDTO dto = new CustomerOrderDetailDTO();
        dto.setOrderId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setOrderedAt(order.getOrderedAt());
        dto.setCustomerName(order.getMemo());
        dto.setCustomerPhone(order.getCustomerPhone());

        dto.setStatus(order.getStatus());
        dto.setOrderType(order.getOrderType());
        dto.setPaymentType(order.getPaymentType());

        dto.setTotalPrice(order.getTotalPrice().intValue());
        dto.setDiscount(order.getDiscount().intValue());
        dto.setOriginalTotal(order.getTotalPrice().intValue() + order.getDiscount().intValue());

        dto.setItems(details.stream()
                .map(ItemDTO::from)
                .collect(Collectors.toList()));

        return dto;
    }
}
