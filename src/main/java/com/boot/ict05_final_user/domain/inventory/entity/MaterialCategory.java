package com.boot.ict05_final_user.domain.inventory.entity;

/**
 * 재료 카테고리 Enum
 *
 * <p>재료의 카테고리를 정의하며, 각 항목은 한글 설명(description)을 가진다.</p>
 *
 * <p>주요 카테고리:</p>
 * <ul>
 *     <li>BASE: 기본재료</li>
 *     <li>SIDE: 사이드</li>
 *     <li>SAUCE: 소스</li>
 *     <li>TOPPING: 토핑</li>
 *     <li>BEVERAGE: 음료</li>
 *     <li>PACKAGE: 패키지</li>
 *     <li>ETC: 기타</li>
 * </ul>
 */
public enum MaterialCategory {
    /** 기본재료 */
    BASE("기본재료", "BAS"),

    /** 사이드 */
    SIDE("사이드", "SID"),

    /** 소스 */
    SAUCE("소스", "SAU"),
    
    /** 토핑 */
    TOPPING("토핑", "TOP"),
    
    /** 음료 */
    BEVERAGE("음료", "BEV"),
    
    /** 패키지 */
    PACKAGE("패키지", "PAC"),

    /** 기타 */
    ETC("기타", "ETC");

    /** 코드 접두어 (영문 3자리) */
    private final String codePrefix;

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자
     *
     * @param description 각 카테고리의 한글 설명
     * @param codePrefix  각 카테고리의 코드 접두어
     */
    MaterialCategory(String description, String codePrefix) {
        this.description = description;
        this.codePrefix = codePrefix;
    }

    /**
     * 카테고리 한글 설명을 반환한다.
     *
     * @return 카테고리 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * 코드의 접두어를 반환한다.
     *
     * @return 코드접두어
     */
    public String getCodePrefix() {return codePrefix;}
}
