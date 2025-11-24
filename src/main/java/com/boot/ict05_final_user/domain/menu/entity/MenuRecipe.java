package com.boot.ict05_final_user.domain.menu.entity;

import com.boot.ict05_final_user.domain.inventory.entity.Material;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 메뉴 레시피 항목 엔티티.
 *
 * <p>메뉴별 구성 재료/용량/역할(메인/소스 등)과 표시 순서를 관리합니다.</p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "메뉴 레시피 항목 엔티티")
public class MenuRecipe {

    /** 레시피 항목 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_recipe_id")
    @Schema(description = "레시피 항목 ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long menuRecipeId;

    /** 표기용 항목명 (재료명이 없을 때 사용) */
    @Column(name = "recipe_item_name", length = 100, nullable = false)
    @Schema(description = "레시피 항목명(표기명)", nullable = false)
    private String recipeItemName;

    /** 대상 메뉴 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id_fk", nullable = false)
    @Schema(description = "대상 메뉴", implementation = Menu.class, nullable = false)
    private Menu menu;

    /** 연결 재료 */
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id_fk", nullable = true)
    @Schema(description = "연결 재료", implementation = Material.class, nullable = true)
    private Material material;

    /** 사용 수량 */
    @Column(name = "recipe_qty", precision = 12, scale = 2, nullable = false)
    @Schema(description = "레시피 사용 수량", nullable = false)
    private BigDecimal recipeQty;

    /** 수량 단위 */
    @Column(name = "recipe_unit", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "레시피 단위", implementation = RecipeUnit.class, nullable = false)
    private RecipeUnit recipeUnit;

    /** 역할(메인/소스 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "recipe_role", nullable = false)
    @Schema(description = "레시피 역할", implementation = MenuRecipe.RecipeRole.class, allowableValues = {"MAIN", "SAUCE"}, nullable = false)
    private RecipeRole recipeRole;

    /** 표시/처리 순서 */
    @Column(name = "recipe_sort", nullable = false)
    @Schema(description = "레시피 정렬 순서", nullable = false)
    private Integer recipeSort;

    /** 레시피 역할 Enum */
    @Schema(description = "레시피 역할 값")
    public enum RecipeRole { MAIN, SAUCE }
}
