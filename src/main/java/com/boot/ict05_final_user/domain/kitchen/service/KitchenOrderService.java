package com.boot.ict05_final_user.domain.kitchen.service;

import com.boot.ict05_final_user.domain.kitchen.dto.KitchenOrderItemDTO;
import com.boot.ict05_final_user.domain.kitchen.dto.KitchenOrderResponseDTO;
import com.boot.ict05_final_user.domain.kitchen.dto.UpdateKitchenOrderStatusRequestDTO;
import com.boot.ict05_final_user.domain.menu.entity.Menu;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrder;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrderDetail;
import com.boot.ict05_final_user.domain.order.entity.OrderStatus;
import com.boot.ict05_final_user.domain.order.entity.OrderType;
import com.boot.ict05_final_user.domain.order.entity.PaymentType;
import com.boot.ict05_final_user.domain.order.repository.CustomerOrderDetailRepository;
import com.boot.ict05_final_user.domain.order.repository.CustomerOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주방 주문(Kitchen Orders) 도메인 비즈니스 로직을 담당하는 서비스.
 *
 * <p>
 * - 주방 화면(KDS)에 노출할 주문 목록 조회<br>
 * - 특정 주문의 상태 변경(프론트 상태 ⇄ 백엔드 {@link OrderStatus} 매핑)<br>
 * - 엔티티를 주방 응답 DTO로 변환
 * </p>
 *
 * <p><b>Transaction Boundary</b></p>
 * <ul>
 *   <li>{@link #getKitchenOrders(Long)}: 읽기 전용 트랜잭션</li>
 *   <li>{@link #updateStatus(Long, UpdateKitchenOrderStatusRequestDTO)}: 쓰기 트랜잭션</li>
 * </ul>
 *
 * <p><i>Note:</i> Swagger(OpenAPI) 문서는 Controller/DTO에 적용됩니다. Service에는 Javadoc만 추가합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KitchenOrderService {

    private final CustomerOrderRepository orderRepository;
    private final CustomerOrderDetailRepository orderDetailRepository;

    /**
     * 주방 화면 주문 목록을 조회합니다.
     *
     * <p>대상 상태: {@link OrderStatus#PREPARING}, {@link OrderStatus#COOKING}, {@link OrderStatus#READY}</p>
     * <p>정렬: 접수 시각 오름차순(먼저 들어온 주문이 먼저)</p>
     *
     * @param storeId 가맹점(점포) ID
     * @return 주방 주문 응답 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<KitchenOrderResponseDTO> getKitchenOrders(Long storeId) {

        List<OrderStatus> statuses = Arrays.asList(
                OrderStatus.PREPARING,  // 접수/준비 대기
                OrderStatus.COOKING,    // 조리중
                OrderStatus.READY       // 픽업대기
        );

        List<CustomerOrder> orders = orderRepository
                .findByStore_IdAndStatusInOrderByOrderedAtAsc(storeId, statuses);

        return orders.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }


    /**
     * 주문 상태를 변경합니다.
     *
     * <p>프론트 상태 문자열(예: "cooking")을 백엔드 {@link OrderStatus}로 변환한 뒤 저장합니다.</p>
     *
     * @param orderId 상태를 변경할 주문 ID
     * @param req     변경할 상태 요청 DTO(허용값: preparing | cooking | ready | completed)
     * @return 변경 후 주방 주문 응답 DTO
     * @throws IllegalArgumentException 주문을 찾을 수 없거나 상태 문자열이 허용되지 않은 경우
     */
    @Transactional
    public KitchenOrderResponseDTO updateStatus(Long orderId, UpdateKitchenOrderStatusRequestDTO req) {

        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // 프론트 상태를 백엔드 상태로 변환
        OrderStatus newStatus = fromFrontStatus(req.getStatus());

        // 상태 업데이트
        order.setStatus(newStatus);

        return toDto(order);
    }

    /**
     * 주문 엔티티를 주방 응답 DTO로 변환합니다.
     *
     * @param order 주문 엔티티
     * @return 변환된 {@link KitchenOrderResponseDTO}
     */
    private KitchenOrderResponseDTO toDto(CustomerOrder order) {

        List<CustomerOrderDetail> details =
                orderDetailRepository.findByOrder_Id(order.getId());

        List<KitchenOrderItemDTO> items = details.stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());

        return KitchenOrderResponseDTO.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .items(items)
                .total(order.getTotalPrice())
                .originalTotal(order.getTotalPrice())
                .discount(order.getDiscount())
                .status(toFrontStatus(order.getStatus()))
                .orderTime(order.getOrderedAt())
                .customer(order.getMemo())
                .paymentMethod(toKorPayment(order.getPaymentType()))
                .orderType(toKorOrderType(order.getOrderType()))
                .priority("normal")
                .notes(order.getMemo())
                .build();
    }

    /**
     * 주문 상세(품목) 엔티티를 주방 품목 DTO로 변환합니다.
     *
     * @param detail 주문 상세 엔티티
     * @return 변환된 {@link KitchenOrderItemDTO}
     */
    private KitchenOrderItemDTO toItemDto(CustomerOrderDetail detail) {
        Menu menu = detail.getMenuIdFk();

        return KitchenOrderItemDTO.builder()
                .menuId(menu.getMenuId())          // Menu 엔티티의 PK 게터 이름에 맞게 (보통 getMenuId)
                .name(menu.getMenuName())          // 메뉴 이름 필드에 맞게
                .price(detail.getUnitPrice())      // 단가 (BigDecimal)
                .quantity(detail.getQuantity())    // 수량
                .image("🍔")
                .options(null)
                .build();
    }

    /**
     * 백엔드 상태를 프론트 표기 상태 문자열로 변환합니다.
     *
     * @param status 백엔드 상태값
     * @return 프론트 표기 문자열 (preparing | cooking | ready | completed)
     */
    private String toFrontStatus(OrderStatus status) {
        return switch (status) {
            case PREPARING -> "preparing";
            case COOKING   -> "cooking";
            case READY -> "ready";
            case COMPLETED -> "completed";
            default -> "preparing";
        };
    }

    /**
     * 프론트 표기 상태 문자열을 백엔드 {@link OrderStatus}로 변환합니다.
     *
     * @param status 프론트 표기 문자열 (preparing | cooking | ready | completed)
     * @return 매핑된 {@link OrderStatus}
     * @throws IllegalArgumentException 허용되지 않은 문자열인 경우
     */
    private OrderStatus fromFrontStatus(String status) {
        return switch (status) {
            case "preparing" -> OrderStatus.PREPARING;
            case "cooking"   -> OrderStatus.COOKING;
            case "ready"     -> OrderStatus.READY;
            case "completed" -> OrderStatus.COMPLETED;
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }

    /**
     * 주문 유형을 한글 라벨로 변환합니다.
     *
     * @param type 주문 유형(enum)
     * @return 한글 라벨("방문" | "포장" | "배달")
     */
    private String toKorOrderType(OrderType type) {
        if (type == null) return "방문";
        return switch (type) {
            case VISIT   -> "방문";
            case TAKEOUT -> "포장";
            case DELIVERY-> "배달";
        };
    }

    /**
     * 결제 수단을 한글 라벨로 변환합니다.
     *
     * @param type 결제 수단(enum)
     * @return 한글 라벨("카드" | "현금" | "상품권" | "외부결제" | "기타")
     */
    private String toKorPayment(PaymentType type) {
        if (type == null) return "기타";
        return switch (type) {
            case CARD    -> "카드";
            case CASH    -> "현금";
            case VOUCHER -> "상품권";
            case EXTERNAL-> "외부결제";
        };
    }
}
