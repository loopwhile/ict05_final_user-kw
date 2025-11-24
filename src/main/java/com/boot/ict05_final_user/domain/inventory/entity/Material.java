package com.boot.ict05_final_user.domain.inventory.entity;

import com.boot.ict05_final_user.domain.inventory.dto.MaterialModifyFormDTO;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 재료(Material) 엔티티 클래스
 *
 * <p>본 클래스는 재료 테이블과 매핑되며,
 * 재료의 재료코드, 재료명, 카테고리, 단위, 공급업체명, 재료보관온도,  재료상태 등의 정보를 포함합니다.</p>
 *
 * <p>엔티티는 생성, 조회, 수정 기능을 지원하며,
 * {@link #updateMaterial(MaterialModifyFormDTO)} 메서드를 통해 상태를 변경할 수 있습니다.</p>
 *
 * @author 김주연
 * @since 2025-10-15
 */

@Entity
@Table(name = "material")
@DynamicUpdate
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    /** 재료 고유 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    @Comment("재료 시퀀스")
    private Long id;

    /** 재료 코드 */
    @Column(name = "material_code", length = 30, nullable = false, unique = true,
            columnDefinition = "VARCHAR(30)")
    @Comment("재료 코드 - 등록시 카테고리 기준으로 생성")
    private String code;

    /** 재료명 */
    @Column(name = "material_name", length = 100, nullable = false,
            columnDefinition = "VARCHAR(100)")
    @Comment("재료명")
    private String name;

    /** 재료 카테고리 */
    @Enumerated(EnumType.STRING)
    @Column(name = "material_category", length = 50, nullable = false,
            columnDefinition = "ENUM('BASE','SIDE','SAUCE','TOPPING','BEVERAGE','PACKAGE','ETC')")
    @Comment("재료 카테고리")
    private MaterialCategory materialCategory;

    /** 기본 단위 (소진 단위) */
    @Column(name = "material_base_unit", length = 20, nullable = false,
            columnDefinition = "VARCHAR(20)")
    @Comment("기본 단위(소진 단위)")
    private String baseUnit;

    /** 판매 단위 */
    @Column(name = "material_sales_unit", length = 20, nullable = false,
            columnDefinition = "VARCHAR(20)")
    @Comment("판매 단위")
    private String salesUnit;

    /** 변환비율(판매단위 → 기본단위) */
    @Column(name = "material_conversion_rate", nullable = false,
            columnDefinition = "INT default 1000")
    @Comment("변환비율(판매단위/기본단위)")
    private Integer conversionRate;

    /** 공급업체명 */
    @Column(name = "material_supplier", length = 100,
            columnDefinition = "VARCHAR(100)")
    @Comment("재료 공급업체명")
    private String supplier;

    /** 재료 보관온도 */
    @Enumerated(EnumType.STRING)
    @Column(name = "material_temperature")
    @Comment("재료 보관온도")
    private MaterialTemperature materialTemperature;

    /** 재료 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "material_status", nullable = false)
    @Comment("재료 상태 (USE/STOP)")
    private MaterialStatus materialStatus;

    /** 등록일시 */
    @CreationTimestamp
    @Column(name = "material_reg_date",
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("등록일시")
    private LocalDateTime regDate;

    /** 수정일시 */
    @UpdateTimestamp
    @Column(name = "material_modify_date",
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Comment("수정일시")
    private LocalDateTime modifyDate;

    /** 본사 기준 적정 재고 수량 */
    @Setter
    @Builder.Default
    @Column(name = "material_optimal_quantity", precision = 15, scale = 3,
            columnDefinition = "DECIMAL(15,3) DEFAULT 0")
    @Comment("본사 기준 적정 재고 수량")
    private BigDecimal optimalQuantity = BigDecimal.ZERO;

    /**
     * 재료 정보를 수정하는 메서드
     *
     * <p>입력된 {@link MaterialModifyFormDTO} 객체의 데이터를 기준으로
     * 공지사항 엔티티의 상태를 변경합니다. 수정 시 작성일자는 현재 시간으로 갱신됩니다.</p>
     *
     * @param dto 수정할 공지사항 정보를 담고 있는 DTO 객체
     */
    public void updateMaterial(MaterialModifyFormDTO dto) {
        this.name                   = dto.getName();
        this.materialCategory       = dto.getMaterialCategory();
        this.baseUnit               = dto.getBaseUnit();
        this.salesUnit              = dto.getSalesUnit();
        this.conversionRate         = dto.getConversionRate();
        this.supplier               = dto.getSupplier();
        this.materialTemperature    = dto.getMaterialTemperature();
        this.materialStatus         = dto.getMaterialStatus();
        this.optimalQuantity        = dto.getOptimalQuantity();
        this.modifyDate             = LocalDateTime.now();
    }
}
