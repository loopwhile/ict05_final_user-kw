package com.boot.ict05_final_user.domain.purchaseOrder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;


/**
 * 발주 상세 품목 정보를 전달하기 위한 DTO.
 *
 * <p>
 *  클라이언트가 선택한 가맹점 재료와 발주 수량을 전달하고,<br>
 *  서버는 본사 재료 정보와 단가, 합계 금액 등을 채워 응답용으로 사용한다.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "발주 상세 품목 DTO")
public class PurchaseOrderItemDTO {

    /** 발주 상세 시퀀스 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "발주 상세 ID (PurchaseOrderDetail.id)", readOnly = true)
    private Long id;

    /** 가맹점 재료 시퀀스 */
    @Schema(description = "가맹점 재료 ID (StoreMaterial.id)")
    private Long storeMaterialId;

    /** 본사 재료 시퀀스 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "본사 재료 ID (Material.id)", readOnly = true)
    private Long materialId;

    /** 상세 품목 수량 */
    @Schema(description = "발주 수량")
    private Integer count;

    /** 상세 품목 재료명 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "재료명 (표시용)", readOnly = true)
    private String materialName;

    /** 상세 품목 단가 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "단가", readOnly = true)
    private BigDecimal unitPrice;

    /** 상세 품목 합계 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "합계 금액 (단가 × 수량)", readOnly = true)
    private BigDecimal totalPrice;

}
