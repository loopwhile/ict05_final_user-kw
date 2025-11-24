package com.boot.ict05_final_user.domain.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 단가(UnitPrice) 엔티티 클래스
 *
 * <p>본 클래스는 {@code unit_price} 테이블과 매핑되며,
 * 각 재료별 본사 매입 단가와 가맹점 공급 단가의 이력을 관리한다.</p>
 *
 * <ul>
 *     <li>material: 단가가 적용되는 재료</li>
 *     <li>purchasePrice: 본사의 매입 단가</li>
 *     <li>sellingPrice: 가맹점 공급 단가</li>
 *     <li>validFrom / validTo: 단가 적용 기간</li>
 * </ul>
 *
 * <p>단가 변경 시 새로운 행이 생성되며, 기간 기준으로 단가 이력을 추적할 수 있다.</p>
 *
 * @author 김주연
 * @since 2025-11-05
 */
@Entity
@Table(name = "unit_price")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitPrice {
    /** 단가 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_price_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    @Comment("단가 시퀀스")
    private Long id;

    /** 재료 (FK: material.material_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id_fk", nullable = false,
            foreignKey = @ForeignKey(name = "fk_unit_price_material"))
    @Comment("재료 시퀀스 (FK)")
    private Material material;

    /** 단가 구분 */
    @Enumerated(EnumType.STRING)
    @Comment("단가 구분")
    @jakarta.persistence.Column(name = "unit_price_type", length = 20, nullable = false)
    @Builder.Default
    private UnitPriceType type = UnitPriceType.PURCHASE;

    /** 본사 매입 단가 */
    @Column(name = "unit_price_purchase", precision = 15, scale = 3, nullable = false,
            columnDefinition = "DECIMAL(15,3) DEFAULT 0")
    @Comment("본사 매입 단가")
    @Builder.Default
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    /** 가맹점 판매 단가 */
    @Column(name = "unit_price_selling", precision = 15, scale = 3, nullable = false,
            columnDefinition = "DECIMAL(15,3) DEFAULT 0")
    @Comment("가맹점 판매 단가")
    @Builder.Default
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    /** 단가 적용 시작일 */
    @Column(name = "unit_price_date_from", nullable = false, columnDefinition = "DATETIME")
    @Comment("단가 적용 시작일")
    @Builder.Default
    private LocalDateTime validFrom = LocalDateTime.now();

    /** 단가 적용 종료일 */
    @Column(name = "unit_price_date_to", columnDefinition = "DATETIME")
    @Comment("단가 적용 종료일")
    private LocalDateTime validTo;

    /** 등록일시 (자동 생성) */
    @CreationTimestamp
    @Column(name = "unit_price_created_at",
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("등록일시 (자동 생성)")
    private LocalDateTime createdAt;
}
