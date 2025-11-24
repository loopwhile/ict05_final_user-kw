package com.boot.ict05_final_user.domain.purchaseOrder.dto;

import com.boot.ict05_final_user.domain.purchaseOrder.entity.PurchaseOrderPriority;
import lombok.*;

import java.util.List;

/**
 * 발주 등록 및 수정 공용 DTO
 * 등록/수정 시 모두 사용 가능
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequestsDTO {

    /** 발주 비고 */
    private String notes;

    /** 발주 우선순위 */
    private PurchaseOrderPriority priority;

    /** 발주 품록 리스트 */
    private List<PurchaseOrderItemDTO> items;

}
