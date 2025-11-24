package com.boot.ict05_final_user.domain.inventory.entity;

import com.boot.ict05_final_user.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 가맹점 단가(StoreUnitPrice)
 *
 * <p>본사 정책과 동일하게, 매장 입고/출고 발생 시마다 단가 이력 행을 생성한다.</p>
 *
 * <ul>
 *   <li>store: 가맹점 FK</li>
 *   <li>storeMaterial: 가맹점 재료 FK</li>
 *   <li>type: 단가 구분(본사와 동일 Enum 재사용: PURCHASE/SELLING)</li>
 *   <li>purchasePrice/sellingPrice: 매장 기준 단가(소수 3자리, 기본 0)</li>
 *   <li>validFrom/validTo: 단가 적용 기간</li>
 *   <li>eventTable/eventId: 생성 근거(선택, IN/OUT/ADJUST)</li>
 * </ul>
 *
 * <p>주의: 이력형 테이블이므로 UPDATE 대신 신규 행 추가를 권장한다.</p>
 */
@Entity
@Table(name = "store_unit_price")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreUnitPrice {

    /** 단가 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_unit_price_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    @Comment("가맹점 단가 시퀀스")
    private Long id;

    /** 가맹점 (FK) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id_fk", nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_unit_price_store"))
    @Comment("가맹점 시퀀스 (FK)")
    private Store store;

    /** 가맹점 재료 (FK) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_material_id_fk", nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_unit_price_store_material"))
    @Comment("가맹점 재료 시퀀스 (FK)")
    private StoreMaterial storeMaterial;

    /** 단가 구분 — 본사 Enum(UnitPriceType: PURCHASE/SELLING) 재사용 */
    @Enumerated(EnumType.STRING)
    @Column(name = "unit_price_type", length = 20, nullable = false)
    @Comment("단가 구분 (PURCHASE/SELLING)")
    @Builder.Default
    private UnitPriceType type = UnitPriceType.PURCHASE;

    /** 매장 매입 단가(=입고 기준가) */
    @Column(name = "unit_price_purchase", precision = 15, scale = 3, nullable = false,
            columnDefinition = "DECIMAL(15,3) DEFAULT 0")
    @Comment("매장 매입 단가")
    @Builder.Default
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    /** 매장 판매 단가(=출고 기준가) */
    @Column(name = "unit_price_selling", precision = 15, scale = 3, nullable = false,
            columnDefinition = "DECIMAL(15,3) DEFAULT 0")
    @Comment("매장 판매 단가")
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

    /** 생성 근거(선택) — IN/OUT/ADJUST */
    @Column(name = "event_table", length = 16)
    @Comment("단가 생성 근거 테이블(IN/OUT/ADJUST)")
    private String eventTable;

    /** 생성 근거 PK(선택) */
    @Column(name = "event_id", columnDefinition = "BIGINT UNSIGNED")
    @Comment("단가 생성 근거 PK")
    private Long eventId;

    /** 등록일시 (자동 생성) */
    @CreationTimestamp
    @Column(name = "unit_price_created_at",
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("등록일시 (자동 생성)")
    private LocalDateTime createdAt;
}
