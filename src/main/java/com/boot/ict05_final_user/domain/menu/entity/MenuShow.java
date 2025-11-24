package com.boot.ict05_final_user.domain.menu.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 메뉴 상태 Enum.
 *
 * <p>메뉴의 판매 표시 상태를 정의합니다.</p>
 *
 * <p>주요 상태:</p>
 * <ul>
 *     <li>SHOW: 판매중</li>
 *     <li>HIDE: 판매중지</li>
 * </ul>
 */
@Schema(description = "메뉴 판매 표시 상태", allowableValues = {"SHOW", "HIDE"})
public enum MenuShow {

    SHOW("판매중"),
    HIDE("판매중지");

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자
     *
     * @param description 각 상태의 한글 설명
     */
    MenuShow(String description) { this.description = description; }

    /**
     * 메뉴 상태의 한글 설명을 반환합니다.
     *
     * @return 한글 설명
     */
    public String getDescription() {
        return description;
    }
}
