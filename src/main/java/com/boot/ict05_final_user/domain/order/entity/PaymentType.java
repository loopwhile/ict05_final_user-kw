package com.boot.ict05_final_user.domain.order.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 결제 수단 유형 Enum.
 *
 * <p>주문 결제 시 사용되는 결제 수단을 카드/현금/상품권/외부 결제로 구분합니다.</p>
 */
@Schema(description = "결제 수단 유형", allowableValues = {"CARD", "CASH", "VOUCHER", "EXTERNAL"})
public enum PaymentType {

    /** 카드 결제 */
    CARD("카드"),

    /** 현금 결제 */
    CASH("현금"),

    /** 상품권 결제 */
    VOUCHER("상품권"),

    /** 외부(타 PG/제휴사 등) 결제 */
    EXTERNAL("외부 결제");

    /** 화면 및 응답 DTO 등에 노출할 한글 라벨 */
    private final String label;

    PaymentType(String label) {
        this.label = label;
    }

    /** 한글 라벨(표시용)을 반환합니다. */
    @Schema(description = "표시용 한글 라벨")
    public String getLabel() {
        return label;
    }
}
