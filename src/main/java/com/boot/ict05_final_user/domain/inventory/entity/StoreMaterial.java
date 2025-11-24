package com.boot.ict05_final_user.domain.inventory.entity;

import com.boot.ict05_final_user.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 가맹점 재료(StoreMaterial) 엔티티
 *
 * <p>
 * - 재고 수량/유통기한/입출고 이력은 {@link StoreInventory} 및 이후
 *   StoreInventoryBatch/Log 에서만 관리한다.<br>
 * - 이 엔티티는 “가맹점 기준 재료 마스터 + 적정 재고 + 최근 매입 정보”까지만 가진다.
 * </p>
 *
 * <ul>
 *   <li>본사 공급 재료(hqMaterial = true) → material FK 존재</li>
 *   <li>가맹점 자체 등록 재료(hqMaterial = false) → material FK = NULL</li>
 * </ul>
 */
@Entity
@Table(
        name = "store_material",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_store_material_code",
                columnNames = {"store_id_fk", "store_material_code"}
        )
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreMaterial {

    /** 가맹점 재료 고유 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_material_id", columnDefinition = "BIGINT UNSIGNED")
    @Comment("가맹점 재료 시퀀스")
    private Long id;

    /** 가맹점 (FK: store.store_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "store_id_fk",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sm_store"),
            columnDefinition = "BIGINT UNSIGNED"
    )
    @Comment("매장 시퀀스 (FK)")
    private Store store;

    /** 본사 재료 (FK: material.material_id) – 본사 재료 사용 시에만 설정 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "material_id_fk",
            foreignKey = @ForeignKey(name = "fk_sm_material"),
            columnDefinition = "BIGINT UNSIGNED"
    )
    @Comment("본사 재료 (FK)")
    private Material material;

    /** 가맹점 재료 코드 (점포별 고유) */
    @Column(name = "store_material_code", length = 30, nullable = false,
            columnDefinition = "VARCHAR(30)")
    @Comment("가맹점 재료 코드(점포별 고유)")
    private String code;

    /** 가맹점 재료명 (표시명) */
    @Column(name = "store_material_name", length = 100, nullable = false,
            columnDefinition = "VARCHAR(100)")
    @Comment("가맹점 재료명")
    private String name;

    /** 카테고리 (본사의 MaterialCategory Enum 값을 문자열로 저장) */
    @Column(name = "store_material_category", length = 50,
            columnDefinition = "VARCHAR(50)")
    @Comment("가맹점 재료 카테고리")
    private String category;

    /** 소진 단위 (가맹점 기준 – 예: 개, 샷, g) */
    @Column(name = "store_material_base_unit", length = 20,
            columnDefinition = "VARCHAR(20)")
    @Comment("소진 단위(가맹점 기준)")
    private String baseUnit;

    /** 입고 단위 (본사 판매단위/발주단위 – 예: 박스, 통, kg) */
    @Column(name = "store_material_sales_unit", length = 20,
            columnDefinition = "VARCHAR(20)")
    @Comment("입고 단위(본사 기준 단위)")
    private String salesUnit;

    /** 변환비율(입고단위 → 소진단위, 예: 1박스=1000g) */
    @Column(name = "material_conversion_rate", nullable = false,
            columnDefinition = "INT DEFAULT 1")
    @Comment("변환비율(입고단위 → 소진단위)")
    private Integer conversionRate;

    /** 공급업체명 (최근/대표 공급처) */
    @Column(name = "store_material_supplier", length = 100,
            columnDefinition = "VARCHAR(100)")
    @Comment("가맹점 재료 공급업체명(대표/최근)")
    private String supplier;

    /** 보관온도 */
    @Enumerated(EnumType.STRING)
    @Column(name = "store_material_temperature",
            columnDefinition = "ENUM('TEMPERATURE','REFRIGERATE','FREEZE')")
    @Comment("보관온도")
    private MaterialTemperature temperature;

    /** 재료 상태 (USE=사용, STOP=미사용) */
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "store_material_status", nullable = false,
            columnDefinition = "ENUM('USE','STOP') DEFAULT 'USE'")
    @Comment("재료 상태")
    private MaterialStatus status;

    /** 적정 수량 (가맹점 기준 – 보통 소진단위 기준으로 운용) */
    @Setter
    @Column(name = "store_material_optimal_quantity", precision = 15, scale = 3,
            columnDefinition = "DECIMAL(15,3)")
    @Comment("적정 수량(가맹점 기준)")
    private BigDecimal optimalQuantity;

    /** 최근 매입 단가 (입고단위 기준 금액) */
    @Column(name = "store_material_purchase_price",
            columnDefinition = "DECIMAL(15,2)")
    @Comment("최근 매입 단가(입고단위 기준)")
    private BigDecimal purchasePrice;

    /** 본사 재료 여부 (1=본사, 0=가맹점 자체 등록) */
    @Column(name = "store_material_is_hq_material", nullable = false,
            columnDefinition = "TINYINT(1) DEFAULT 0")
    @Comment("본사 재료 여부")
    private boolean isHqMaterial;

    /** 등록일 */
    @CreationTimestamp
    @Column(name = "store_material_reg_date", nullable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("등록일")
    private LocalDateTime regDate;

    /** 수정일 */
    @UpdateTimestamp
    @Column(name = "store_material_modify_date",
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Comment("수정일")
    private LocalDateTime modifyDate;
}
