package com.boot.ict05_final_user.domain.inventory.entity;

/**
 * 단가 구분 Enum
 *
 * <p>재료의 단가 구분을 정의하며, 각 항목은 한글 설명(description)을 가진다.</p>
 *
 * <p>주요 카테고리:</p>
 * <ul>
 *     <li>PURCHASE: 매입가</li>
 *     <li>SELLING: 판매가</li>
 * </ul>
 */
public enum UnitPriceType {
    /** 매입가 */
    PURCHASE("매입가"),

    /** 판매가 */
    SELLING("판매가");

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자
     *
     * @param description 각 단가 구분의 한글 설명
     */
    UnitPriceType(String description) { this.description = description; }

    /**
     * 단가 구분 한글 설명을 반환한다.
     *
     * @return 단가 구분 설명
     */
    public String getDescription() {
        return description;
    }
}
