package com.boot.ict05_final_user.domain.inventory.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 재고 공통 베이스
 *
 * <p>공통 필드와 공통 동작만 제공. 상태 계산은 {@link InventoryStatus}에 위임한다.</p>
 *
 * @author 김주연
 * @since 2025-11-11
 */
@Getter
@MappedSuperclass
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@SuperBuilder
public abstract class InventoryBase {

    /** 현재 수량 */
    @Builder.Default
    @Setter
    @Column(name = "inventory_quantity", precision = 15, scale = 3, nullable = false,
            columnDefinition = "DECIMAL(15,3) DEFAULT 0.000")
    @Comment("현재 수량")
    protected BigDecimal quantity = BigDecimal.ZERO;

    /** 적정 수량 */
    @Setter
    @Column(name = "inventory_optimal_quantity", precision = 15, scale = 3,
            columnDefinition = "DECIMAL(15,3)")
    @Comment("적정 수량")
    protected BigDecimal optimalQuantity;

    /** 재고 상태 */
    @Builder.Default
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false, length = 20)
    @Comment("재고 상태")
    protected InventoryStatus status = InventoryStatus.SUFFICIENT;

    /** 마지막 업데이트 일시 */
    @Setter
    @Column(name = "inventory_update_date", nullable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("재고 수정일")
    protected LocalDateTime updateDate;

    /**
     * 재고 상태를 즉시 재계산해 반영한다.
     *
     * <p>수량 또는 적정 수량이 변경된 직후 호출한다.</p>
     * <ul>
     *   <li>수량 ≤ 0 → {@link InventoryStatus#SHORTAGE}</li>
     *   <li>적정 수량이 null → 수량 &gt; 0 이면 {@link InventoryStatus#SUFFICIENT}</li>
     *   <li>수량 &lt; 적정 수량 → {@link InventoryStatus#LOW}</li>
     *   <li>그 외 → {@link InventoryStatus#SUFFICIENT}</li>
     * </ul>
     */
    public final void updateStatusNow() {
        this.status = InventoryStatus.from(this.quantity, this.optimalQuantity);
    }

    /**
     * 수량 변경 후 상태와 업데이트 시각을 동기화한다.
     *
     * <p>서비스 계층에서 수량을 갱신한 뒤 반드시 호출한다.
     * 내부적으로 {@link #updateStatusNow()}를 호출하고 {@code updateDate}를 현재 시각으로 갱신한다.</p>
     */
    public final void touchAfterQuantityChange() {
        this.updateStatusNow();
        this.updateDate = LocalDateTime.now();
    }

    /**
     * 영속화 직전 훅.
     *
     * <p>{@code updateDate}가 비어 있으면 현재 시각으로 채우고,
     * 상태가 비어 있으면 {@link #updateStatusNow()}로 초기 상태를 확정한다.</p>
     */
    @PrePersist
    protected void onCreate() {
        if (this.updateDate == null) this.updateDate = LocalDateTime.now();
        if (this.status == null) this.updateStatusNow();
    }

    /**
     * 업데이트 직전 훅.
     *
     * <p>{@code updateDate}를 현재 시각으로 갱신하고
     * 상태가 비어 있으면 {@link #updateStatusNow()}로 보정한다.</p>
     */
    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
        if (this.status == null) this.updateStatusNow();
    }
}
