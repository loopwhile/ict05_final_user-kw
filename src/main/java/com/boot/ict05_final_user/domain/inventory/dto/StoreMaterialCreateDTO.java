package com.boot.ict05_final_user.domain.inventory.dto;

import com.boot.ict05_final_user.domain.inventory.entity.MaterialStatus;
import com.boot.ict05_final_user.domain.inventory.entity.MaterialTemperature;
import com.boot.ict05_final_user.domain.inventory.entity.MaterialCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * 가맹점 재료 등록 DTO
 *
 * <p>
 * - 재료 마스터 + 적정 재고 + 최근 매입 정보만 포함<br>
 * - 실제 재고 수량/유통기한은 StoreInventory/Batch에서 관리
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreMaterialCreateDTO {

    /** 가맹점 ID */
    @Schema(hidden = true)
    private Long storeId;

    /** 가맹점 재료 코드 (미지정 시 서버에서 자동 생성 가능) */
    private String code;

    /** 가맹점 재료명 */
    @NotBlank(message = "재료명은 필수입니다.")
    private String name;

    /** 재료 카테고리 (본사 MaterialCategory Enum 사용) */
    @NotNull(message = "재료 카테고리는 필수입니다.")
    private MaterialCategory category;

    /** 소진 단위 (예: 개, 샷, g) */
    @NotBlank(message = "소진 단위는 필수입니다.")
    private String baseUnit;

    /** 입고 단위 (예: 박스, 통, kg) */
    @NotBlank(message = "입고 단위는 필수입니다.")
    private String salesUnit;

    /** 변환비율(입고단위 → 소진단위, 1 이상) (null이면 100으로 처리) */
    @Min(value = 1, message = "단위 변환값은 1 이상이어야 합니다.")
    private Integer conversionRate;

    /** 공급업체명 */
    private String supplier;

    /** 보관온도 */
    private MaterialTemperature temperature;

    /** 적정 수량 (가맹점 기준, 보통 소진 단위 기준 수량) */
    @NotNull(message = "적정 재고 수량은 필수입니다.")
    private BigDecimal optimalQuantity;

    /** 최근 매입 단가 (입고 단위 기준) */
    private BigDecimal purchasePrice;
}
