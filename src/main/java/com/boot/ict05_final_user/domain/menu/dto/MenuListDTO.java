package com.boot.ict05_final_user.domain.menu.dto;

import com.boot.ict05_final_user.domain.menu.entity.MenuShow;
import com.boot.ict05_final_user.domain.menu.entity.StoreMenuSoldout;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 메뉴 목록 행(리스트 아이템)을 표현하는 DTO.
 *
 * <p>가맹점 사용자 앱/웹의 메뉴 목록 화면에 렌더링되는 최소/핵심 정보를 담습니다.
 * 판매 표시 상태, 품절 상태, 카테고리/가격/칼로리/설명 등을 포함합니다.</p>
 */
@Data       // getter, setter, toString 등 기본 메서드
@AllArgsConstructor     // 매개변수 생성자
@NoArgsConstructor      // 디폴트 생성자
@Schema(description = "메뉴 목록 DTO")
public class MenuListDTO {

    /** 메뉴 시퀀스 */
    @Schema(description = "메뉴 ID(시퀀스)", nullable = false)
    private Long menuId;

    /** 판매상태(true: 판매중 / false: 중단) */
    @Schema(description = "판매 표시 상태", implementation = MenuShow.class)
    private MenuShow menuShow;

    /** 메뉴명 */
    @Schema(description = "메뉴 이름(한글)", nullable = false)
    private String menuName;

    /** 메뉴 코드 */
    @Schema(description = "메뉴 코드")
    private String menuCode;

    /** 메뉴 카테고리 ID */
    @Schema(description = "메뉴 카테고리 ID")
    private Long menuCategoryId;

    /** 카테고리명 */
    @Schema(description = "카테고리명")
    private String menuCategoryName;

    /** 메뉴 이름(영문) */
    @Schema(description = "메뉴 이름(영문)")
    private String menuNameEnglish;

    /** 가격 */
    @Schema(description = "메뉴 가격(BigDecimal)", nullable = false)
    private BigDecimal menuPrice;

    /** 메뉴 칼로리 */
    @Schema(description = "메뉴 칼로리(kcal)")
    private Integer menuKcal;

    /** 메뉴 설명 */
    @Schema(description = "메뉴 설명")
    private String menuInformation;

    /** 재고상태 */
    @Schema(description = "가맹점 단위 품절 상태", implementation = StoreMenuSoldout.class)
    private StoreMenuSoldout storeMenuSoldout;

    /** 한글 라벨 : 카테고리 */
    @Schema(description = "카테고리 한글 라벨(파생값)")
    public String getMenuCategoryLabel() {
        return menuCategoryName != null ? menuCategoryName : "";
    }

}
