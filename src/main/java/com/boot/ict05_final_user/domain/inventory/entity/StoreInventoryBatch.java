package com.boot.ict05_final_user.domain.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 가맹점 재고 배치(LOT) 엔티티
 *
 * <p>
 * - 각 가맹점 재료별 입고분(LOT)을 관리한다.<br>
 * - 잔량(quantity), 입고일, 유통기한, LOT 번호 기준으로
 *   유통기한 임박/소진 등의 알림 및 FIFO 출고에 사용된다.
 * </p>
 *
 * <p>
 * 수량/잔량은 StoreInventoryOut / Adjustment 를 통해서만 변경하고,
 * 직접 UPDATE 하지 않는다.
 * </p>
 */
@Entity
@Table(
        name = "store_inventory_batch",
        indexes = {
                @Index(
                        name = "idx_sib_store_expire",
                        columnList = "store_inventory_id_fk, store_inventory_batch_expiration_date"
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Comment("가맹점 재고 배치(LOT)")
public class StoreInventoryBatch {

    /** 가맹점 재고 배치 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_inventory_batch_id", columnDefinition = "BIGINT UNSIGNED")
    @Comment("가맹점 재고 배치 시퀀스")
    private Long id;

    /** 가맹점 재고 (FK: store_inventory.store_inventory_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "store_inventory_id_fk",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sib_store_inventory")
    )
    @Comment("가맹점 재고(FK)")
    private StoreInventory storeInventory;

    /** 가맹점 재료 (FK: store_material.store_material_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "store_material_id_fk",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sib_store_material")
    )
    @Comment("가맹점 재료(FK)")
    private StoreMaterial storeMaterial;

    /** 배치 잔량 (소진 단위 기준 or 내부 기준 단위) */
    @Column(
            name = "store_inventory_batch_quantity",
            nullable = false,
            precision = 15,
            scale = 3,
            columnDefinition = "DECIMAL(15,3) DEFAULT 0"
    )
    @Comment("배치 잔량")
    private BigDecimal quantity;

    /** 입고일시 */
    @Column(
            name = "store_inventory_batch_received_date",
            nullable = false,
            columnDefinition = "DATETIME"
    )
    @Comment("입고일시")
    private LocalDateTime receivedDate;

    /** 유통기한 */
    @Column(
            name = "store_inventory_batch_expiration_date",
            columnDefinition = "DATE"
    )
    @Comment("유통기한")
    private LocalDate expirationDate;

    /** LOT 번호 (가맹점 기준, 본사 LOT와 별도 관리 가능) */
    @Column(
            name = "store_inventory_batch_lot_no",
            length = 50,
            columnDefinition = "VARCHAR(50)"
    )
    @Comment("LOT 번호(가맹점 기준)")
    private String lotNo;

    /** 등록일 */
    @CreationTimestamp
    @Column(
            name = "store_inventory_batch_reg_date",
            nullable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    @Comment("등록일")
    private LocalDateTime regDate;
}
