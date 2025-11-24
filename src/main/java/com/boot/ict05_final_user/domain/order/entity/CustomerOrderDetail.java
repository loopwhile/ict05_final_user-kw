package com.boot.ict05_final_user.domain.order.entity;

import com.boot.ict05_final_user.domain.menu.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

/**
 * 주문 상세(CustomerOrderDetail) 엔티티.
 *
 * <p>주문에 포함된 개별 메뉴/수량/단가/금액 정보를 보관합니다.
 * 단가와 수량을 기반으로 라인 합계(lineTotal)를 자동 계산합니다.</p>
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "customer_order_detail")
@Schema(description = "주문 상세 엔티티")
public class CustomerOrderDetail {

    /** 주문 상세 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_order_detail_id")
    @Schema(description = "주문 상세 ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    /** 주문(FK) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_order_id_fk", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Setter
    @Schema(description = "상위 주문", implementation = CustomerOrder.class, nullable = false)
    private CustomerOrder order;

    /** 메뉴 시퀀스(FK) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id_fk", nullable = false)
    @Schema(description = "해당 라인의 메뉴", implementation = Menu.class, nullable = false)
    private Menu menuIdFk;

    /** 주문 수량 */
    @Column(name = "customer_order_detail_quantity", nullable = false)
    @Schema(description = "주문 수량", nullable = false)
    private Integer quantity;

    /** 단가 (당시 메뉴 가격 스냅샷) */
    @Column(name = "customer_order_detail_unit_price", precision = 15, scale = 2, nullable = false)
    @Schema(description = "단가(BigDecimal)", nullable = false)
    private BigDecimal unitPrice;

    /** 주문 금액(해당 라인 총액 = 단가 × 수량) */
    @Column(name = "customer_order_detail_total", precision = 15, scale = 2, nullable = false)
    @Schema(description = "라인 합계(단가×수량)", nullable = false)
    private BigDecimal lineTotal;

    /**
     * 영속/수정 직전 훅에서 라인 합계를 계산합니다.
     *
     * <p>수량이 없으면 1, 단가가 없으면 0으로 간주하여 계산합니다.</p>
     */
    @PrePersist
    @PreUpdate
    void calculateLineTotal() {
        if (quantity == null) quantity = 1;
        if (unitPrice == null) unitPrice = BigDecimal.ZERO;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
