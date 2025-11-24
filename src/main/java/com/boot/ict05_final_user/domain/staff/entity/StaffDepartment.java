package com.boot.ict05_final_user.domain.staff.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 직원 부서 Enum.
 *
 * <p>부서의 종류를 정의하며, 각 항목은 한글 설명(description)을 가진다.</p>
 *
 * <p>주요 부서:</p>
 * <ul>
 *     <li>OFFICE: 본사팀</li>
 *     <li>STORE: 판매팀</li>
 *     <li>FRANCHISE: 가맹관리팀</li>
 *     <li>OPS: 운영지원팀</li>
 *     <li>HR: 인사팀</li>
 *     <li>ANALYTICS: 데이터분석팀</li>
 *     <li>ADMIN: 관리팀</li>
 * </ul>
 */
@Schema(description = "직원 부서 Enum")
public enum StaffDepartment {

    @Schema(description = "본사팀")
    OFFICE("본사팀"),

    @Schema(description = "판매팀(매장)")
    STORE("판매팀"),

    @Schema(description = "가맹관리팀")
    FRANCHISE("가맹관리팀"),

    @Schema(description = "운영지원팀")
    OPS("운영지원팀"),

    @Schema(description = "인사팀")
    HR("인사팀"),

    @Schema(description = "데이터분석팀")
    ANALYTICS("데이터분석팀"),

    @Schema(description = "관리팀")
    ADMIN("관리팀");

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자.
     *
     * @param description 각 카테고리의 한글 설명
     */
    StaffDepartment(String description) {
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
