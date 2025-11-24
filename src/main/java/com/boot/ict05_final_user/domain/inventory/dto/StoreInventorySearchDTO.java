package com.boot.ict05_final_user.domain.inventory.dto;

import com.boot.ict05_final_user.domain.inventory.entity.InventoryStatus;
import lombok.Data;

/**
 * 가맹점 재고 검색 DTO
 *
 * <p>가맹점 재고 목록 페이지 및 REST API 검색 조건을 전달한다.</p>
 */
@Data
public class StoreInventorySearchDTO {
    private String s;
    private String type;
    private String size = "10";

    /** 가맹점 ID */
    private Long storeId;

    /** 재고 상태 (SUFFICIENT / LOW / SHORTAGE) */
    private InventoryStatus status;
}