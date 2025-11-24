package com.boot.ict05_final_user.domain.inventory.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 재고 입·출고/조정 헤더 상태
 *
 * <p>DRAFT: 재고 반영 전(임시)</p>
 * <p>CONFIRMED: 재고 반영 완료</p>
 * <p>CANCELLED: 재고 미반영 상태에서의 취소</p>
 * <p>REVERSED: 재고 반영 후, 리버설(역분개)로 상쇄된 상태</p>
 */
@Getter
@Schema(description = "재고 트랜잭션 상태")
public enum InventoryRecordStatus {

    @Schema(description = "임시(재고 미반영)")
    DRAFT,

    @Schema(description = "확정(재고 반영 완료)")
    CONFIRMED,

    @Schema(description = "재고 미반영 상태에서 취소")
    CANCELLED,

    @Schema(description = "재고 반영 후 리버설(역분개)로 상쇄됨")
    REVERSED
}
