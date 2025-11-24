package com.boot.ict05_final_user.domain.purchaseOrder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderSummaryDTO {

    /** 총 발주 건수 */
    private Long totalCount;

    /** 대기중인 발주 건수 */
    private Long receiveCount;

    /** 주문된 발주 건수 */
    private Long shippingCount;

    /** 완료된 발주 건수 */
    private Long deliveredCount;

}
