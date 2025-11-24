package com.boot.ict05_final_user.domain.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 가맹점 재고 수량 조정 이력 (StoreInventoryAdjustment)
 *
 * <p>실사 차이, 파손, 오입력 정정 등 비정상 사유로 인한 재고 수정 이벤트를 기록한다.
 * 매장 단은 LOT를 관리하지 않으므로 총량 기준으로만 반영한다.</p>
 *
 * <ul>
 *   <li>storeInventory: 가맹점 집계 재고 FK</li>
 *   <li>quantityBefore/After: 조정 전·후 수량</li>
 *   <li>difference: 후 - 전 (양수=증가, 음수=감소)</li>
 *   <li>unitPrice: 조정 단가(선택), 단가 이력 생성 시 참고</li>
 *   <li>reason: MANUAL/DAMAGE/LOSS/ERROR</li>
 * </ul>
 */
@Entity
@Table(name = "store_inventory_adjustment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreInventoryAdjustment {

    /** 가맹점 재고 조정 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_adjustment_id", columnDefinition = "BIGINT UNSIGNED")
    @Comment("가맹점 재고 조정 시퀀스")
    private Long id;

    /** 가맹점 재고 FK */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_inventory_id_fk")
    @Comment("가맹점 재고 FK")
    private StoreInventory storeInventory;

    /** 조정 전 수량 */
    @Column(name = "store_inventory_adjustment_quantity_before", precision = 15, scale = 3, nullable = false)
    @Comment("조정 전 수량")
    private BigDecimal quantityBefore;

    /** 조정 후 수량 */
    @Column(name = "store_inventory_adjustment_quantity_after", precision = 15, scale = 3, nullable = false)
    @Comment("조정 후 수량")
    private BigDecimal quantityAfter;

    /** 증감 수량 (후 - 전) */
    @Column(name = "store_inventory_adjustment_difference", precision = 15, scale = 3, nullable = false)
    @Comment("증감 수량")
    private BigDecimal difference;

    /** 조정 단가(선택) */
    @Column(name = "store_inventory_adjustment_unit_price", columnDefinition = "BIGINT")
    @Comment("조정 단가")
    private BigDecimal unitPrice;

    /** 비고 / 사유 */
    @Column(name = "store_inventory_adjustment_memo", columnDefinition = "VARCHAR(255)")
    @Comment("비고 / 사유")
    private String memo;

    /** 조정일시 */
    @CreationTimestamp
    @Column(name = "store_inventory_adjustment_created_at", nullable = false, updatable = false,
            columnDefinition = "DATETIME")
    @Comment("조정일시")
    private LocalDateTime createdAt;

    /** 조정 사유(본사 Enum 재사용: MANUAL, DAMAGE, LOSS, ERROR) */
    @Enumerated(EnumType.STRING)
    @Column(name = "store_inventory_adjustment_reason", length = 20)
    @Comment("조정 사유 (MANUAL, DAMAGE, LOSS, ERROR)")
    private AdjustmentReason reason;

    /** 트랜잭션 상태(DRAFT/CONFIRMED/CANCELLED/REVERSED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_record_status", nullable = false, length = 20)
    @Builder.Default
    @Comment("트랜잭션 상태")
    private InventoryRecordStatus status = InventoryRecordStatus.CONFIRMED;

    /** 증감 계산 헬퍼 */
    public void calculateDifference() {
        if (quantityBefore != null && quantityAfter != null) {
            this.difference = quantityAfter.subtract(quantityBefore);
        }
    }
}
