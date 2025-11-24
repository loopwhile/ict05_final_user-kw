package com.boot.ict05_final_user.domain.menu.entity;

import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 메뉴 재료 소진 기록(menu_usage_material_log) 엔티티.
 *
 * <p>주문 발생 시, 어떤 메뉴로 인해 특정 매장 재료가 얼마나 소진되었는지 추적하기 위한 로그입니다.</p>
 */
@Entity
@Table(name = "menu_usage_material_log")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "메뉴 재료 소진 기록 엔티티")
public class MenuUsageMaterialLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "menu_usage_material_log_id",
            columnDefinition = "BIGINT UNSIGNED COMMENT '재료 소진 기록 시퀀스'"
    )
    @Schema(description = "로그 ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    /** 주문 시퀀스 (FK: customer_order.customer_order_id) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_order_id_fk",
            nullable = false,
            columnDefinition = "BIGINT UNSIGNED COMMENT '주문 시퀀스 (FK)'"
    )
    @Schema(description = "연관 주문", implementation = CustomerOrder.class, nullable = false)
    private CustomerOrder customerOrderFk;

    /** 메뉴 시퀀스 (FK: menu.menu_id) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "menu_id_fk",
            nullable = false,
            columnDefinition = "BIGINT UNSIGNED COMMENT '메뉴 시퀀스 (FK)'"
    )
    @Schema(description = "연관 메뉴", implementation = Menu.class, nullable = false)
    private Menu menuFk;

    /** 매장 재료 시퀀스 (FK: store_material.store_material_id) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_material_id_fk",
            nullable = false,
            columnDefinition = "BIGINT UNSIGNED COMMENT '매장 재료 시퀀스 (FK)'"
    )
    @Schema(description = "연관 매장 재료", implementation = StoreMaterial.class, nullable = false)
    private StoreMaterial storeMaterialFk;

    @Column(
            name = "menu_usage_material_log_count",
            nullable = false,
            precision = 15,
            scale = 3,
            columnDefinition = "DECIMAL(15,3) COMMENT '재료 소진 수량'"
    )
    @Schema(description = "재료 소진 수량", nullable = false)
    private BigDecimal count;

    @Column(
            name = "menu_usage_material_log_unit",
            length = 20,
            nullable = false,
            columnDefinition = "VARCHAR(20) COMMENT '재료 소진 단위'"
    )
    @Schema(description = "재료 소진 단위", nullable = false)
    private String unit;

    @CreationTimestamp
    @Column(
            name = "menu_usage_material_log_date",
            nullable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '재료 소진 기록 일자'"
    )
    @Schema(description = "기록 일시", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime logDate;

    @Column(
            name = "menu_usage_material_log_memo",
            columnDefinition = "VARCHAR(255) COMMENT '비고'"
    )
    @Schema(description = "비고/메모")
    private String memo;
}
