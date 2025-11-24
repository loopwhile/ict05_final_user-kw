package com.boot.ict05_final_user.domain.menu.dto;

import com.boot.ict05_final_user.domain.menu.entity.MenuShow;
import com.boot.ict05_final_user.domain.menu.entity.StoreMenuSoldout;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 메뉴 목록 조회 시 사용하는 검색/필터 파라미터 DTO.
 *
 * <p>검색어/검색타입과 판매/품절 상태, 카테고리 필터를 포함합니다.
 * Controller에서는 {@code @ParameterObject}로 바인딩하여 사용합니다.</p>
 */
@Data
@Schema(description = "메뉴 검색/필터 파라미터 DTO")
public class MenuSearchDTO {

    /** 검색어 */
    @Schema(description = "검색어(부분 일치)")
    private String s;

    /** 검색 타입 (name/info/all) */
    @Schema(
            description = "검색 대상 필드",
            example = "all",
            allowableValues = {"name", "info", "all"}
    )
    private String type;

    /** 재고상태 */
    @Schema(
            description = "가맹점 단위 품절 상태 필터",
            implementation = StoreMenuSoldout.class,
            example = "SOLD_OUT",
            nullable = true
    )
    private StoreMenuSoldout storeMenuSoldout;

    /** 판매상태(true: 판매중 / false: 중단) */
    @Schema(
            description = "판매 표시 상태 필터",
            implementation = MenuShow.class,
            example = "SHOW",
            nullable = true
    )
    private MenuShow menuShow;

    /** 필터 키 (안정성) */
    @Schema(description = "메뉴 카테고리 ID(필터)")
    private Long menuCategoryId;

    /** 선택된 카테고리 이름 (표시용; 서버에서 채워서 내려줌) */
    @Schema(description = "선택된 카테고리 이름(표시용 파생값)")
    private String categoryName;

}
