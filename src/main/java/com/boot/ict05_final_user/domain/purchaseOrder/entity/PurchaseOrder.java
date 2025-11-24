package com.boot.ict05_final_user.domain.purchaseOrder.entity;

import com.boot.ict05_final_user.domain.store.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 가맹점 발주 엔티티 (Purchase Order)
 * DB 테이블명: purchase_order
 */
@Entity
@Table(name = "purchase_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    /** 발주 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_order_id", columnDefinition = "BIGINT UNSIGNED")
    @Schema(description = "발주 ID")
    private Long id;

    /** 가맹점 */
    @ManyToOne
    @JoinColumn(name = "store_id_fk")
    @Schema(description = "발주를 생성한 가맹점")
    private Store store;

    /** 발주 주문코드 */
    @Column(name = "purchase_order_code", length = 32, nullable = false, unique = true)
    @Schema(description = "발주 코드 (예: ORD20240115001)")
    private String orderCode;

    /** Material에서 가져오는 대표 품목명 */
    @Column(name = "purchase_order_main_item_name", length = 100)
    @Schema(description = "대표 품목명")
    private String mainItemName;

    /** 한 발주 내 품목 수 */
    @Column(name = "purchase_order_item_count")
    @Schema(description = "발주 품목 개수")
    private Integer itemCount;

    /** 발주 주문일 */
    @Column(name = "purchase_order_date", nullable = false)
    @Schema(description = "발주일자")
    private LocalDate orderDate;

    /** 총액 */
    @Column(name = "purchase_order_total_price", precision = 12, scale = 2)
    @Builder.Default
    @Schema(description = "발주 총액")
    private BigDecimal totalPrice = BigDecimal.ZERO;

    /** 발주 비고 */
    @Column(name = "purchase_order_remark", columnDefinition = "TEXT")
    @Schema(description = "발주 비고")
    private String remark;

    /** Material에서 가져오는 공급업체명 */
    @Column(name = "purchase_order_supplier", length = 100, nullable = false)
    @Schema(description = "공급업체명")
    private String supplier;

    /** 발주 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "purchase_order_status")
    @Schema(description = "발주 상태")
    private PurchaseOrderStatus status;

    /** 발주 우선순위 */
    @Enumerated(EnumType.STRING)
    @Column(name = "purchase_order_priority")
    @Schema(description = "발주 우선순위")
    private PurchaseOrderPriority priority;

    /** 발주 완료일 */
    @Column(name = "purchase_order_delivery_date")
    @Schema(description = "배송 완료일")
    private LocalDate deliveryDate;

    /** 발주 실제 납기일 */
    @Column(name = "purchase_order_actual_delivery_date")
    @Schema(description = "실제 납기일")
    private LocalDate actualDeliveryDate;

    /** 발주 상세 항목들 */
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "발주 상세 항목 리스트")
    private List<PurchaseOrderDetail> details;
}
