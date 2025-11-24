package com.boot.ict05_final_user.domain.home.dto;

/**
 * KPI 증감 Enum
 *
 * <p>대시보드 KPI부분의 증감을 담당하며, 각 항목은 한글 설명(description)을 가진다.</p>
 *
 * <p>주요 카테고리:</p>
 * <ul>
 *     <li>INCREASE: 증가</li>
 *     <li>DECREASE: 감소</li>
 *     <li>NEUTRAL: 증감없음</li>
 * </ul>
 */
public enum ChangeType {

    INCREASE("증가"),

    DECREASE("감소"),

    NEUTRAL("증감없음");

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자
     *
     * @param description 각 카테고리의 한글 설명
     */
    ChangeType(String description) {
        this.description = description;
    }

    /**
     * 카테고리 한글 설명을 반환한다.
     *
     * @return 카테고리 설명
     */
    public String getDescription() {
        return description;
    }


}
