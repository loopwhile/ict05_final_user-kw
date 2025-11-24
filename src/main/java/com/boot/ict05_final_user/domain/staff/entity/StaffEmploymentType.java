package com.boot.ict05_final_user.domain.staff.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 직원 근무형태 Enum.
 *
 * <p>근무 형태의 종류를 정의하며, 각 항목은 한글 설명(description)을 가진다.</p>
 *
 * <p>주요 근무형태:</p>
 * <ul>
 *     <li>OWNER: 점주</li>
 *     <li>WORKER: 직원</li>
 *     <li>PART_TIMER: 알바</li>
 * </ul>
 */
@Schema(description = "직원 근무 형태 Enum")
public enum StaffEmploymentType {

    @Schema(description = "점주")
    OWNER("점주"),

    @Schema(description = "정규/직원")
    WORKER("직원"),

    @Schema(description = "파트타이머/알바")
    PART_TIMER("알바");

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자.
     *
     * @param description 각 카테고리의 한글 설명
     */
    StaffEmploymentType(String description) {
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
