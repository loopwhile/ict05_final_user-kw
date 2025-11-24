package com.boot.ict05_final_user.domain.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * v_store_inventory_log 뷰 매핑 (읽기 전용)
 *
 * <p>가맹점 입고/출고/조정 이벤트를 단일 타임라인으로 통합 제공하는 View 엔티티.
 * INSERT/UPDATE 금지.</p>
 *
 * <ul>
 *   <li>event_type: INCOME / OUTGO / ADJUST</li>
 *   <li>event_qty: +입고, -출고, ±조정</li>
 *   <li>stock_after: 이벤트 반영 후 재고</li>
 * </ul>
 */
@Entity
@Table(name = "v_store_inventory_log")
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreInventoryLogView {

    /** 뷰 전용 PK (ROW_NUMBER() 등으로 생성) */
    @Id
    @Column(name = "row_id")
    private Long rowId;

    /** 업무용 로그 ID (입고/출고/조정 원본 PK) */
    @Column(name = "log_id")
    private Long logId;

    /** 로그 일시 */
    @Column(name = "log_date")
    private LocalDateTime date;

    /** 가맹점 재료 ID */
    @Column(name = "store_material_id")
    private Long storeMaterialId;

    /** 가맹점 ID */
    @Column(name = "store_id")
    private Long storeId;

    /** 이벤트 타입: INCOME / OUTGO / ADJUST */
    @Column(name = "log_type")
    private String type;

    /** 수량(+입고, -출고, ±조정) */
    @Column(name = "quantity")
    private BigDecimal quantity;

    /** 이벤트 반영 후 재고 */
    @Column(name = "stock_after")
    private BigDecimal stockAfter;

    /** 단가(옵션) */
    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    /** 메모 */
    @Column(name = "memo")
    private String memo;

    /** 가맹점명(옵션) */
    @Column(name = "store_name")
    private String storeName;
}
