package com.boot.ict05_final_user.domain.kitchen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * 주방 주문 항목(Kitchen Order Item)을 표현하는 DTO입니다.
 *
 * <p>주문 상세 화면 및 주방표시(KDS)에서 각 메뉴 라인을 나타내며,
 * 메뉴 식별자, 이름, 단가, 수량, 대표 이미지 경로/URL, 선택 옵션 문자열 등을 포함합니다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "주방 주문 품목 DTO")
public class KitchenOrderItemDTO {

    /** 메뉴 ID */
    @Schema(description = "메뉴 ID", nullable = false)
    private Long menuId;

    /** 메뉴 이름(표시명) */
    @Schema(description = "메뉴 이름", nullable = false)
    private String name;

    /** 품목 단가 */
    @Schema(description = "단가(BigDecimal)", nullable = false)
    private BigDecimal price;

    /** 주문 수량 */
    @Schema(description = "수량", minimum = "1", nullable = false)
    private int quantity;

    /** 대표 이미지 경로 또는 URL */
    @Schema(description = "대표 이미지 경로/URL")
    private String image;

    /**
     * 선택 옵션 문자열(프론트 직렬화 포맷 그대로 전달)
     * <p>예: JSON 직렬화 문자열 또는 콤마 구분 문자열 등</p>
     */
    @Schema(
            description = "선택 옵션(문자열/직렬화 데이터). 예: JSON 문자열",
            example = "{\"extraCheese\":true,\"noOnion\":true}"
    )
    private String options;
}
