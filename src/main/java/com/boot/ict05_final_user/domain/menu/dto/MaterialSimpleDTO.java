package com.boot.ict05_final_user.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 재료(원부자재) 간단 정보 DTO.
 *
 * <p>메뉴 상세/레시피/재고 화면 등에서 재료를 간단 표기로 낼 때 사용합니다.
 * 식별자와 표기명을 최소 필드로 제공합니다.</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "재료 간단 정보 DTO")
public class MaterialSimpleDTO {

    /** 재료 ID(내부 식별자) */
    @Schema(description = "재료 ID", example = "501", nullable = false)
    private Long materialId;

    /** 재료 이름(표기명) */
    @Schema(description = "재료 이름", example = "양파", nullable = false)
    private String materialName;
}
