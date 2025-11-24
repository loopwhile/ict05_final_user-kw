package com.boot.ict05_final_user.domain.menu.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 레시피 수량 단위를 나타내는 Enum.
 *
 * <p>그램(g), 밀리리터(ml), 개수, 장(시트) 등의 단위를 표준화합니다.</p>
 */
@Schema(description = "레시피 단위", allowableValues = {"G", "ML", "EA", "SHEET"})
public enum RecipeUnit {

    G("g"),
    ML("ml"),
    EA("개"),
    SHEET("장");

    /** 화면/문서 표기를 위한 단위 라벨 */
    private final String label;

    RecipeUnit(String label) { this.label = label; }

    /** 단위 라벨을 반환합니다. */
    public String getLabel() { return label; }
}
