package com.boot.ict05_final_user.domain.inventory.entity;

import com.boot.ict05_final_user.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

/**
 * 가맹점 재고(StoreInventory) 엔티티
 *
 * <p>
 * 각 매장의 현재 재고와 재고 상태를 관리한다.<br>
 * 적정 수량(Optimal Quantity)은 {@link StoreMaterial} 에서 관리하고,
 * 본 엔티티는 StoreMaterial 별 현재 수량/상태/최근 갱신 정보에 집중한다.
 * </p>
 */
@Entity
@Table(name = "store_inventory",
        uniqueConstraints = @UniqueConstraint(name = "uq_store_inv",
                columnNames = {"store_id_fk", "store_material_id_fk"}))
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@SuperBuilder
@Getter
@Comment("가맹점 재고")
public class StoreInventory extends InventoryBase {

    /** 가맹점 재고 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_inventory_id", columnDefinition = "BIGINT UNSIGNED")
    @Comment("가맹점 재고 시퀀스")
    private Long id;

    /** 가맹점 (FK: store.store_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id_fk", nullable = false,
            foreignKey = @ForeignKey(name = "fk_si_store"))
    private Store store;

    /** 가맹점 재료 (FK: store_material.store_material_id) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_material_id_fk", nullable = false,
            foreignKey = @ForeignKey(name = "fk_si_store_material"))
    private StoreMaterial storeMaterial;



    /**
     * 수량 증가(Null-Safe)
     * @param delta 증가분(0 이상)
     */
    public void increase(BigDecimal delta) {
        if (delta == null) throw new IllegalArgumentException("증가 수량이 필요합니다.");
        if (delta.signum() < 0) throw new IllegalArgumentException("증가 수량은 0 이상이어야 합니다.");
        BigDecimal cur = this.quantity != null ? this.quantity : BigDecimal.ZERO;
        this.quantity = cur.add(delta); // 필요 시 .setScale(3, RoundingMode.HALF_UP)
    }

    /**
     * 수량 감소(Null-Safe)
     * @param delta 감소분(0 이상)
     */
    public void decrease(BigDecimal delta) {
        if (delta == null) throw new IllegalArgumentException("감소 수량이 필요합니다.");
        if (delta.signum() < 0) throw new IllegalArgumentException("감소 수량은 0 이상이어야 합니다.");
        BigDecimal cur = this.quantity != null ? this.quantity : BigDecimal.ZERO;
        BigDecimal next = cur.subtract(delta);
        if (next.signum() < 0) {
            throw new IllegalStateException("재고 수량은 음수가 될 수 없습니다.");
        }
        this.quantity = next; // 필요 시 .setScale(3, RoundingMode.HALF_UP)
    }
}
