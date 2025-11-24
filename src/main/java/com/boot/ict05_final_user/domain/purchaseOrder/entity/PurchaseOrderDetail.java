package com.boot.ict05_final_user.domain.purchaseOrder.entity;

import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 가맹점 발주 상세 엔티티 (Purchase Order Detail)
 * DB 테이블명: purchase_order_detail
 */
@Entity
@Table(name = "purchase_order_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDetail {

    /** 발주 상세 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_order_detail_id")
    @Schema(description = "발주 상세 ID")
    private Long id;

    /** 발주 헤더 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id_fk", nullable = false)
    @Schema(description = "상위 발주 헤더")
    private PurchaseOrder purchaseOrder;

    /** 발주 품목 Material */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id_fk", nullable = false)
    @Schema(description = "발주 품목(가맹점 재고 재료)")
    private StoreMaterial material;

    /** 단가 : 등록 시 Material.unitPrice 사용 */
    @Column(name = "purchase_order_detail_unit_price", precision = 12, scale = 2, nullable = false)
    @Schema(description = "단가 (Material.unitPrice 기준)")
    private BigDecimal unitPrice;

    /** 단위 : Material.unit 사용 (DTO에서만 필요할 수 있음) */
    @Transient
    @Schema(description = "단위 (Material.unit, DB 비저장 필드)")
    private String unit;

    /** 수량 : 등록 시 입력값 */
    @Column(name = "purchase_order_detail_count", nullable = false)
    @Schema(description = "발주 수량")
    private Integer count;

    /** 총액 = 단가 * 수량 */
    @Column(name = "purchase_order_detail_total_price", precision = 12, scale = 2, nullable = false)
    @Schema(description = "총액 (단가 × 수량)")
    private BigDecimal totalPrice;
}

