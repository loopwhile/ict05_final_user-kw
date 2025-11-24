package com.boot.ict05_final_user.domain.inventory.entity;

import com.boot.ict05_final_user.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 가맹점 출고/소진 이력 (StoreInventoryOut)
 *
 * <p>매장 측 재고 감소 이벤트(판매, 폐기, 본사 반품 등)를 기록한다.
 * 매장 단에서는 LOT(배치)를 관리하지 않으므로 총량 기준만 기록한다.</p>
 *
 * <ul>
 *   <li>store: 가맹점 FK</li>
 *   <li>storeMaterial: 가맹점 재료 FK</li>
 *   <li>quantity: 출고 수량(양수로 저장; 의미상 재고 감소)</li>
 *   <li>stockAfter: 출고 반영 후 매장 재고 수량(집계 결과)</li>
 *   <li>unitPrice: 출고 단가(정책상 단가 이력 생성에 사용)</li>
 *   <li>reversalForId: 리버설 대상 원본 출고 헤더 ID(선택)</li>
 * </ul>
 *
 * <p>트랜잭션 상태는 본사와 동일한 {@code InventoryRecordStatus}를 사용한다.</p>
 */
@Entity
@Table(name = "store_inventory_out")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreInventoryOut {

    /** 가맹점 출고 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_inventory_out_id", columnDefinition = "BIGINT UNSIGNED")
    @Comment("가맹점 출고 시퀀스")
    private Long id;

    /** 가맹점 (FK) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id_fk",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_inventory_out_store"))
    @Comment("가맹점 코드 (FK)")
    private Store store;

    /** 가맹점 재료 (FK: store_material.store_material_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_material_id_fk",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_inventory_out_store_material"))
    @Comment("가맹점 재료 코드 (FK)")
    private StoreMaterial storeMaterial;

    /** 출고 수량(양수로 저장; 의미상 감소) */
    @Column(name = "store_inventory_out_quantity", precision = 15, scale = 3, nullable = false,
            columnDefinition = "DECIMAL(15,3) DEFAULT 0")
    @Comment("출고 수량")
    private BigDecimal quantity;

    /** 출고 후 재고량(집계 테이블 반영 결과) */
    @Setter
    @Column(name = "store_inventory_out_stock_after", precision = 15, scale = 3)
    @Comment("출고 반영 후 재고 수량")
    private BigDecimal stockAfter;

    /** 출고 단가(매장 기준) — 단가 이력 생성에 사용 */
    @Column(name = "store_inventory_out_unit_price", precision = 15, scale = 2, nullable = false,
            columnDefinition = "BIGINT")
    @Comment("출고 단가(매장 기준)")
    private BigDecimal unitPrice;

    /** 비고 */
    @Column(name = "store_inventory_out_memo", columnDefinition = "VARCHAR(255)")
    @Comment("비고")
    private String memo;

    /** 출고일시(실제) */
    @Column(name = "store_inventory_out_date", nullable = false, columnDefinition = "DATETIME")
    @Comment("출고일시")
    private LocalDateTime outDate;

    /** 등록일시 (자동 생성) */
    @CreationTimestamp
    @Column(name = "store_inventory_out_created_at",
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("등록일시 (자동 생성)")
    private LocalDateTime createdAt;

    /** 트랜잭션 상태(DRAFT/CONFIRMED/CANCELLED/REVERSED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_record_status", nullable = false, length = 20)
    @Builder.Default
    @Comment("트랜잭션 상태")
    private InventoryRecordStatus status = InventoryRecordStatus.CONFIRMED;

    /** 리버설 대상 원본 출고 헤더 ID(선택) */
    @Column(name = "reversal_for_id", columnDefinition = "BIGINT UNSIGNED")
    @Comment("리버설 대상 출고 헤더 ID(원본)")
    private Long reversalForId;
}
