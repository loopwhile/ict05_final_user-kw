package com.boot.ict05_final_user.domain.menu.dto;

import com.boot.ict05_final_user.domain.menu.entity.MenuShow;
import com.boot.ict05_final_user.domain.menu.entity.StoreMenuSoldout;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 메뉴 상세 정보를 표현하는 DTO.
 *
 * <p>가맹점 사용자 앱/웹의 메뉴 상세 화면에 사용됩니다.
 * 카테고리, 가격/칼로리, 판매/품절 상태, 설명, 재료 등 표시용 데이터를 제공합니다.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "메뉴 상세 DTO")
public class MenuDetailDTO {

    /** 메뉴 고유 ID */
    @Schema(description = "메뉴 ID", nullable = false)
    private Long menuId;

    /** 메뉴 카테고리 ID */
    @Schema(description = "메뉴 카테고리 ID")
    private Long menuCategoryId;

    /** 메뉴 카테고리 */
    @Schema(description = "메뉴 카테고리명")
    private String menuCategoryName;

    /** 판매 상태 */
    @Schema(description = "판매 표시 상태", implementation = MenuShow.class)
    private MenuShow menuShow;

    /** 메뉴코드 */
    @Schema(description = "메뉴 코드")
    private String menuCode;

    /** 메뉴명 */
    @Schema(description = "메뉴 이름(한글)", nullable = false)
    private String menuName;

    /** 영문명 */
    @Schema(description = "메뉴 이름(영문)")
    private String menuNameEnglish;

    /** 가격 */
    @Schema(description = "메뉴 가격(BigDecimal)", nullable = false)
    private BigDecimal menuPrice;

    /** 칼로리 */
    @Schema(description = "메뉴 칼로리(kcal)")
    private Integer menuKcal;

    /** 판매중 / 품절 */
    @Schema(description = "가맹점 단위 품절 상태", implementation = StoreMenuSoldout.class)
    private StoreMenuSoldout storeMenuSoldout;

    /** 설명 */
    @Schema(description = "메뉴 설명")
    private String menuInformation;

    /** 재료 */
    @ArraySchema(
            arraySchema = @Schema(description = "재료(표기명) 목록"),
            schema = @Schema(example = "패티")
    )
    private List<String> ingredientNames;

}
