package com.boot.ict05_final_user.domain.menu.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 메뉴 마스터를 표현하는 JPA 엔티티.
 *
 * <p>카테고리, 가격/칼로리, 판매 표기 상태, 코드/이름/설명 등
 * 메뉴 기본 속성과 레시피(연결 엔티티) 연관을 보유합니다.</p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "메뉴 엔티티")
public class Menu {

    /** 메뉴 고유 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    @Schema(description = "메뉴 ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long menuId;

    /** 메뉴명 */
    @Column(name = "menu_name")
    @Schema(description = "메뉴 이름(한글)", nullable = false)
    private String menuName;

    /** 메뉴코드 */
    @Column(name = "menu_code")
    @Schema(description = "메뉴 코드")
    private String menuCode;

    /** 메뉴 설명 */
    @Schema(description = "메뉴 설명")
    private String menuInformation;

    /** 메뉴 영문명 */
    @Schema(description = "메뉴 이름(영문)")
    private String menuNameEnglish;

    /** 메뉴 칼로리 */
    @Column(name = "menu_kcal")
    @Schema(description = "메뉴 칼로리(kcal)")
    private Integer menuKcal;

    /** 판매상태(0:중지, 1:판매중) */
    @Enumerated(EnumType.STRING)
    @Column(name = "menu_show")
    @Schema(description = "판매 표시 상태", implementation = MenuShow.class)
    private MenuShow menuShow;

    /** 가격 */
    @Column(name = "menu_price")
    @Schema(description = "가격(BigDecimal)", nullable = false)
    private BigDecimal menuPrice;

    /** 레시피(연결엔티티) */
    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Schema(description = "연결된 레시피 목록", accessMode = Schema.AccessMode.READ_ONLY)
    private List<MenuRecipe> recipe = new ArrayList<>();

    /** menuCategory 참조 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name= "menu_category_id_fk")
    @Schema(description = "메뉴 카테고리", implementation = MenuCategory.class, nullable = false)
    private MenuCategory menuCategory;

    /**
     * 메뉴의 카테고리를 교체합니다.
     *
     * @param category 새로운 카테고리
     */
    public void changeCategory(MenuCategory category) { this.menuCategory = category; }

}
