package com.boot.ict05_final_user.domain.order.entity;

import com.boot.ict05_final_user.domain.store.entity.Store;
import jakarta.persistence.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문(CustomerOrder) 엔티티.
 *
 * <p>주문 기본정보(매장, 상태, 결제유형, 주문일시, 총금액 등)를 담습니다.</p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "customer_order")
@Schema(description = "주문 엔티티")
public class CustomerOrder {

    /** 주문 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_order_id")
    @Schema(description = "주문 ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    /** 매장 시퀀스(FK) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id_fk", nullable = false)
    @Schema(description = "주문 소속 매장", implementation = Store.class, nullable = false)
    private Store store;

    /** 주문 코드(예: YYYYMMDD-XXXX 등) */
    @Column(name = "customer_order_code", unique = true)
    @Schema(description = "주문 코드")
    private String orderCode;

    /** 주문 상태 (대기/준비중/완료/취소) */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_order_status", nullable = false)
    @Schema(description = "주문 상태", implementation = OrderStatus.class, nullable = false)
    private OrderStatus status;

    /** 주문 총금액 */
    @Column(name = "customer_order_total_price", precision = 15, scale = 2, nullable = false)
    @Schema(description = "총 결제 금액", nullable = false)
    private BigDecimal totalPrice;

    /** 주문 일시 */
    @Schema(type = "string", format = "date-time", description = "주문(접수) 시각", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "customer_order_date", nullable = false)
    private LocalDateTime orderedAt;

    /** 주문 형태 (visit/takeout/delivery) */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_order_type", nullable = false)
    @Schema(description = "주문 유형", implementation = OrderType.class, nullable = false)
    private OrderType orderType;

    /** 결제 방식 (card/cash/voucher/external) */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_order_payment_type", nullable = false)
    @Schema(description = "결제 수단", implementation = PaymentType.class, nullable = false)
    private PaymentType paymentType;

    /** 할인 금액(없으면 0.00) */
    @Builder.Default
    @Column(name = "customer_order_discount", precision = 15, scale = 2, nullable = false)
    @Schema(description = "할인 금액", nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    /** 비고 */
    @Column(name = "customer_order_memo")
    @Schema(description = "비고/메모")
    private String memo;

    /** 고객 전화번호 */
    @Column(name = "customer_phone")
    @Schema(description = "고객 전화번호")
    private String customerPhone;

    /** 배달 주소 */
    @Schema(description = "배달 주소")
    private String deliveryAddress;

    /** 주문 상세 목록 */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @Schema(description = "주문 품목 목록", implementation = CustomerOrderDetail.class)
    private List<CustomerOrderDetail> details;

    /** 기본값 설정 훅 */
    @PrePersist
    void prePersist() {
        if (discount == null) discount = BigDecimal.ZERO;
        if (orderedAt == null) orderedAt = LocalDateTime.now();
        if (status == null) status = OrderStatus.PENDING;
    }
}
