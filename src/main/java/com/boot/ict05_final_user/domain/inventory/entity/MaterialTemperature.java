package com.boot.ict05_final_user.domain.inventory.entity;

/**
 * 재료 보관방법 Enum
 *
 * <p>재료의 보관방법을 정의하며, 각 항목은 한글 설명(description)을 가진다.</p>
 *
 * <p>재료 보관방법:</p>
 * <ul>
 *     <li>TEMPERATURE: 상온보관</li>
 *     <li>REFRIGERATE : 냉장보관</li>
 *     <li>FREEZE : 냉동보관</li>
 * </ul>
 */
public enum MaterialTemperature {

    /** 상온보관 */
    TEMPERATURE("상온"),

    /** 냉장보관 */
    REFRIGERATE("냉장"),

    /** 냉동보관 */
    FREEZE("냉동");

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자
     *
     * @param description 각 재료 보관방법의 한글 설명
     */
    MaterialTemperature(String description) {
        this.description = description;
    }

    /**
     * 재료 보관방법 한글 설명을 반환한다.
     *
     * @return 재료 보관방법 설명
     */
    public String getDescription() {
        return description;
    }
}
