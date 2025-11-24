package com.boot.ict05_final_user.domain.inventory.dto;

import com.boot.ict05_final_user.domain.inventory.entity.MaterialCategory;
import com.boot.ict05_final_user.domain.inventory.entity.MaterialStatus;
import com.boot.ict05_final_user.domain.inventory.entity.MaterialTemperature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 재료 수정 폼 DTO
 *
 * <p>재료 수정 시 클라이언트에서 전달되는 데이터 전송 객체(DTO)이다.</p>
 */
@Data
public class MaterialModifyFormDTO {

    /** 작성자(회원) FK */
    private Long memberIdFk;

    /** 수정할 재료의 고유 ID */
    private Long id;

    /** 재료명 */
    @NotBlank(message = "재료명을 입력해주세요")
    private String name;

    /** 재료 카테고리 */
    @NotNull(message = "카테고리를 선택해주세요")
    private MaterialCategory materialCategory;

    /** 기본 단위 */
    @NotBlank(message = "기본 단위를 입력해주세요")
    private String baseUnit;

    /** 판매 단위 */
    @NotBlank(message = "판매 단위를 입력해주세요")
    private String salesUnit;

    /** 판매단위 → 기본단위 변환비율 */
    @NotNull(message = "변환비율을 입력해주세요")
    private Integer conversionRate;

    /** 공급업체명 */
    private String supplier;

    /** 보관 온도 */
    private MaterialTemperature materialTemperature;

    /** 재료 상태 */
    @NotNull(message = "상태를 선택해주세요")
    private MaterialStatus materialStatus;

    /** 본사 기준 적정 재고 수량 */
    @NotNull(message = "적정 수량을 입력해주세요")
    private BigDecimal optimalQuantity;
}
