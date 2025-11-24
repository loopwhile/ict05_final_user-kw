package com.boot.ict05_final_user.domain.purchaseOrder.dto;

import com.boot.ict05_final_user.domain.purchaseOrder.entity.PurchaseOrderStatus;
import lombok.Data;

@Data
public class PurchaseOrderSearchDTO {
    private String s;
    private String type;
    private String size = "10";

    /** 발주 상태 필터 (RECEIVED/SHIPPING/DELIVERED) */
    private PurchaseOrderStatus purchaseOrderStatus;

    private Long storeId;
}
