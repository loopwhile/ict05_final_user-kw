package com.boot.ict05_final_user.domain.staff.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;

/**
 * 직원 근태 상태 Enum.
 *
 * <p>
 * DB에는 영어 코드(code)가 저장되며,<br>
 * 화면(UI)에는 한글 라벨(label)을 사용한다.
 * </p>
 *
 * <ul>
 *     <li>NORMAL(정상)</li>
 *     <li>LATE(지각)</li>
 *     <li>EARLY_LEAVE(조퇴)</li>
 *     <li>ABSENT(결근)</li>
 *     <li>VACATION(휴가)</li>
 *     <li>HOLIDAY(휴일)</li>
 *     <li>RESIGN(퇴사)</li>
 * </ul>
 */
@Schema(description = "근태 상태 Enum")
public enum AttendanceStatus {

    @Schema(description = "정상 출근")
    NORMAL("normal", "정상"),

    @Schema(description = "지각")
    LATE("late", "지각"),

    @Schema(description = "조퇴")
    EARLY_LEAVE("early_leave", "조퇴"),

    @Schema(description = "결근")
    ABSENT("absent", "결근"),

    @Schema(description = "휴가")
    VACATION("vacation", "휴가"),

    @Schema(description = "휴일")
    HOLIDAY("holiday", "휴일"),

    @Schema(description = "퇴사")
    RESIGN("resign", "퇴사");

    /** DB 저장 코드 */
    private final String code;

    /** 화면 표기용 라벨 */
    private final String label;

    AttendanceStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /** DB 저장 코드 반환 */
    public String getCode() {
        return code;
    }

    /** 화면 한글 라벨 반환 */
    public String getLabel() {
        return label;
    }

    /** code → enum 변환 */
    public static AttendanceStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AttendanceStatus code: " + code));
    }

    /** label → enum 변환 */
    public static AttendanceStatus fromLabel(String label) {
        return Arrays.stream(values())
                .filter(v -> v.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AttendanceStatus label: " + label));
    }

}
