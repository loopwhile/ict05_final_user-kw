package com.boot.ict05_final_user.domain.inventory.entity;

/**
 * 재료의 상태 Enum
 *
 * <p>재료의 상태을 정의하며, 각 항목은 한글 설명(description)을 가진다.</p>
 *
 * <p>재료의 상태:</p>
 * <ul>
 *     <li>USE: 사용중</li>
 *     <li>STOP : 사용중단</li>
 * </ul>
 */
public enum MaterialStatus {
    /** 사용중 */
    USE("사용중"),

    /** 사용중단 */
    STOP("사용중단");

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자
     *
     * @param description 각 재료의 상태의 한글 설명
     */
    MaterialStatus(String description) {
        this.description = description;
    }

    /**
     * 재료의 상태 한글 설명을 반환한다.
     *
     * @return 재료의 상태 설명
     */
    public String getDescription() {
        return description;
    }
}
