package com.boot.ict05_final_user.domain.purchaseOrder.entity;

public enum PurchaseOrderPriority {

    /** 일반 */
    NORMAL("일반"),

    /** 우선 */
    URGENT("우선");

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자
     *
     * @param description 각 카테고리의 한글 설명
     */
    PurchaseOrderPriority(String description) { this.description = description; }

    /**
     * 카테고리 한글 설명을 반환한다.
     *
     * @return 카테고리 설명
     */
    public String getDescription() { return description; }
}
