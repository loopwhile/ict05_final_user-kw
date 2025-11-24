package com.boot.ict05_final_user.domain.order.service;

import com.boot.ict05_final_user.domain.menu.entity.Menu;
import com.boot.ict05_final_user.domain.menu.repository.MenuRepository;
import com.boot.ict05_final_user.domain.order.dto.*;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrder;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrderDetail;
import com.boot.ict05_final_user.domain.order.entity.OrderStatus;
import com.boot.ict05_final_user.domain.order.entity.OrderType;
import com.boot.ict05_final_user.domain.order.entity.PaymentType;
import com.boot.ict05_final_user.domain.order.repository.CustomerOrderDetailRepository;
import com.boot.ict05_final_user.domain.order.repository.CustomerOrderRepository;
import com.boot.ict05_final_user.domain.store.entity.Store;
import com.boot.ict05_final_user.domain.store.repository.StoreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 도메인의 핵심 비즈니스 로직을 제공하는 서비스.
 *
 * <p>
 * - 주문 생성<br>
 * - 주문 상태 변경<br>
 * - 주문 상세 조회(가맹점 기준 권한 확인)<br>
 * - 주문 목록 페이지 변환
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepository;
    private final CustomerOrderDetailRepository detailRepository;
    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;

    /**
     * 주문을 생성합니다.
     *
     * <p>인증된 가맹점 ID를 기준으로 주문을 저장하고, 품목 상세를 함께 영속화합니다.</p>
     *
     * @param req     주문 생성 요청 DTO
     * @param storeId 인증된 가맹점 ID
     * @return 생성된 주문의 식별자/코드를 담은 응답 DTO
     * @throws IllegalArgumentException 가맹점 또는 메뉴가 존재하지 않을 때
     */
    @Transactional
    public CreateOrderResponseDTO create(CreateOrderRequestDTO req, Long storeId) {

        log.info("▶ create order storeId(from login user) = {}", storeId);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        String orderCode = generateOrderCode();

        CustomerOrder order = CustomerOrder.builder()
                .store(store)
                .orderCode(orderCode)
                .orderType(OrderType.from(req.getOrderType()))
                .paymentType(resolvePaymentType(req.getPaymentType()))
                .totalPrice(req.getTotalPrice())
                .discount(req.getDiscount())
                .status(OrderStatus.PREPARING)
                .memo(req.getCustomerName())
                .build();

        order = orderRepository.save(order);

        for (CreateOrderRequestDTO.OrderItemRequest i : req.getItems()) {
            Menu menu = menuRepository.findById(i.getMenuId())
                    .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + i.getMenuId()));

            CustomerOrderDetail d = CustomerOrderDetail.builder()
                    .order(order)
                    .menuIdFk(menu)
                    .quantity(i.getQuantity())
                    .unitPrice(i.getUnitPrice())
                    .build();
            detailRepository.save(d);
        }

        return CreateOrderResponseDTO.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .build();
    }

    /**
     * 주문 상태를 변경합니다.
     *
     * <p>영문 상수 또는 DB 라벨 문자열을 입력받아 {@link OrderStatus}로 변환 후 반영합니다.</p>
     *
     * @param orderId    주문 ID
     * @param statusText 상태 문자열
     * @throws IllegalArgumentException 주문이 없거나 상태 문자열이 유효하지 않을 때
     */
    @Transactional
    public void updateStatus(Long orderId, String statusText) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(statusText.toUpperCase());
        } catch (Exception ignore) {
            newStatus = OrderStatus.from(statusText);
        }
        order.setStatus(newStatus);
    }

    /**
     * 주문 상세를 조회합니다(가맹점 기준 접근 제어).
     *
     * @param storeId 로그인 가맹점 ID
     * @param orderId 주문 ID
     * @return 주문 상세 DTO
     * @throws IllegalArgumentException 주문이 존재하지 않을 때
     * @throws IllegalStateException    다른 가맹점의 주문일 때
     */
    public CustomerOrderDetailDTO getOrderDetail(Long storeId, Long orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getStore().getId().equals(storeId)) {
            throw new IllegalStateException("다른 매장의 주문에 접근할 수 없습니다.");
        }

        List<CustomerOrderDetail> details = detailRepository.findByOrder_Id(orderId);

        return CustomerOrderDetailDTO.from(order, details);
    }

    /**
     * 주문 목록을 페이지 단위로 조회하고 목록용 DTO로 변환합니다.
     *
     * @param storeId  가맹점 ID
     * @param cond     검색/필터 조건
     * @param pageable 페이징/정렬 정보
     * @return 주문 목록 페이지 DTO
     */
    public Page<CustomerOrderListDTO> searchOrderListPage(
            Long storeId,
            CustomerOrderSearchDTO cond,
            Pageable pageable
    ) {
        var page = orderRepository.searchOrders(storeId, cond, pageable);
        return page.map(CustomerOrderListDTO::from);
    }

    /**
     * 주문 코드를 생성합니다.
     *
     * <p>최근 주문 ID를 기반으로 증가값을 생성하여 코드로 만듭니다.</p>
     *
     * @return 생성된 주문 코드
     */
    private String generateOrderCode() {
        Long lastId = orderRepository.findTopByOrderByIdDesc()
                .map(CustomerOrder::getId)
                .orElse(0L);

        long next = lastId + 1;
        return String.format("#%04d", next);
    }

    /**
     * 결제수단 입력 문자열을 {@link PaymentType}으로 변환합니다.
     *
     * <p>영문 상수/코드 또는 라벨 문자열을 허용합니다.</p>
     *
     * @param value 입력 문자열
     * @return 매핑된 결제수단
     * @throws IllegalArgumentException 매핑 실패 시
     */
    private PaymentType resolvePaymentType(String value) {
        if (value == null) {
            throw new IllegalArgumentException("paymentType is null");
        }

        String v = value.trim();

        if (v.endsWith("결제")) {
            v = v.substring(0, v.length() - 2);
        }

        try {
            return PaymentType.valueOf(v.toUpperCase());
        } catch (Exception ignore) { }

        for (PaymentType type : PaymentType.values()) {
            if (type.getLabel().equals(v)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown paymentType: " + value);
    }

    /** null 안전 소문자 변환 유틸. */
    private String safeLower(String s) {
        return s == null ? null : s.toLowerCase();
    }
}
