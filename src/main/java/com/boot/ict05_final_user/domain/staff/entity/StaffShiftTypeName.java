package com.boot.ict05_final_user.domain.staff.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 근무시간대 이름(Enum).
 *
 * <p>
 * 직원의 근무시간대(오픈/미들/마감)를 정의하며,<br>
 * 각 항목은 한글 설명(description)을 가진다.
 * </p>
 *
 * <ul>
 *     <li>OPEN   : 오픈조</li>
 *     <li>MIDDLE : 미들조</li>
 *     <li>CLOSE  : 마감조</li>
 * </ul>
 */
@Getter
@Schema(description = "근무시간대 이름 Enum")
public enum StaffShiftTypeName {

    /** 오픈조 */
    @Schema(description = "오픈 근무조")
    OPEN("오픈"),

    /** 미들조 */
    @Schema(description = "미들 근무조")
    MIDDLE("미들"),

    /** 마감조 */
    @Schema(description = "마감 근무조")
    CLOSE("마감");

    /** 한글 설명 */
    private final String description;

    StaffShiftTypeName(String description) {
        this.description = description;
    }
}
