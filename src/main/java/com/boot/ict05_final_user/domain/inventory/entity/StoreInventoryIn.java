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
 * 가맹점 입고 이력 (StoreInventoryIn)
 *
 * <p>본사 → 가맹점 출고 수령 등 매장 측 입고 이벤트를 기록한다.
 * 매장 단에서는 LOT(배치)를 관리하지 않으며 총량만 반영한다.</p>
 *
 * <ul>
 *   <li>store: 가맹점 FK</li>
 *   <li>storeMaterial: 가맹점 재료 FK</li>
 *   <li>quantity: 입고 수량</li>
 *   <li>stockAfter: 입고 반영 후 매장 재고 수량(집계 결과)</li>
 *   <li>unitPrice/sellingPrice: 정책상 단가 이력 생성에 사용</li>
 *   <li>refHqOutId: 본사 출고 헤더 참조(추적용, 선택)</li>
 * </ul>
 *
 * <p>트랜잭션 상태는 본사와 동일한 {@code InventoryRecordStatus}를 사용한다.</p>
 */
@Entity
@Table(name = "store_inventory_in")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreInventoryIn {

    /** 가맹점 입고 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_inventory_in_id", columnDefinition = "BIGINT UNSIGNED")
    @Comment("가맹점 입고 시퀀스")
    private Long id;

    /** 가맹점 (FK) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id_fk",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_inventory_in_store"))
    @Comment("가맹점 코드 (FK)")
    private Store store;

    /** 가맹점 재료 (FK: store_material.store_material_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_material_id_fk",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_inventory_in_store_material"))
    @Comment("가맹점 재료 코드 (FK)")
    private StoreMaterial storeMaterial;

    /** 입고 수량 */
    @Column(name = "store_inventory_in_quantity", precision = 15, scale = 3, nullable = false,
            columnDefinition = "DECIMAL(15,3) DEFAULT 0")
    @Comment("입고 수량")
    private BigDecimal quantity;

    /** 입고 후 재고량(집계 테이블 반영 결과) */
    @Setter
    @Column(name = "store_inventory_in_stock_after", precision = 15, scale = 3)
    @Comment("입고 반영 후 재고 수량")
    private BigDecimal stockAfter;

    /** 입고 단가(매장 기준) — 단가 이력 생성에 사용 */
    @Column(name = "store_inventory_in_unit_price", precision = 15, scale = 2, nullable = false,
            columnDefinition = "BIGINT")
    @Comment("입고 단가(매장 기준)")
    private BigDecimal unitPrice;

    /** 판매가(매장 기준, 선택) */
    @Column(name = "store_inventory_in_selling_price", precision = 15, scale = 2,
            columnDefinition = "BIGINT")
    @Comment("판매가(매장 기준)")
    private BigDecimal sellingPrice;

    /** 본사 출고 헤더 참조(추적용, 선택) */
    @Column(name = "ref_hq_out_id_fk", columnDefinition = "BIGINT UNSIGNED")
    @Comment("본사 출고 헤더 참조(추적용)")
    private Long refHqOutId;

    /** 입고일시(실제) */
    @Column(name = "store_inventory_in_date", nullable = false, columnDefinition = "DATETIME")
    @Comment("입고일시")
    private LocalDateTime inDate;

    /** 비고 */
    @Column(name = "store_inventory_in_memo", columnDefinition = "VARCHAR(255)")
    @Comment("비고")
    private String memo;

    /** 등록일시 (자동 생성) */
    @CreationTimestamp
    @Column(name = "store_inventory_in_created_at",
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("등록일시 (자동 생성)")
    private LocalDateTime createdAt;

    /** 트랜잭션 상태(DRAFT/CONFIRMED/CANCELLED/REVERSED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_record_status", nullable = false, length = 20)
    @Builder.Default
    @Comment("트랜잭션 상태")
    private InventoryRecordStatus status = InventoryRecordStatus.CONFIRMED;



    @PrePersist
    void prePersist() {
        if (this.inDate == null) this.inDate = LocalDateTime.now();   // ★ 입고일 기본값
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
