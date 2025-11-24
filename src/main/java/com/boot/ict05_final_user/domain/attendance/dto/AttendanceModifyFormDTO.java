package com.boot.ict05_final_user.domain.attendance.dto;

import com.boot.ict05_final_user.domain.staff.entity.AttendanceStatus;
import com.boot.ict05_final_user.domain.staff.entity.StaffEmploymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 근태 수정 폼 DTO.
 *
 * <p>
 * 근태 수정 화면에 기존 데이터를 표시하거나,
 * 수정된 데이터를 서버에 전송할 때 사용되는 DTO이다.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceModifyFormDTO {

    /** 근무 시퀀스 */
    private Long attendanceId;

    /** 근무 일자 */
    private LocalDate attendanceWorkDate;

    /** 출근 시간 */
    private LocalDateTime attendanceCheckIn;

    /** 퇴근 시간 */
    private LocalDateTime attendanceCheckOut;

    /** 근태 상태 */
    private AttendanceStatus attendanceStatus;

    /** 실제 근무 시간 */
    private BigDecimal attendanceWorkHours;

    /** 근태 비고/사유 */
    private String attendanceMemo;

    // === Staff JOIN ===

    /** 직원 시퀀스 */
    private Long staffId;

    /** 직원 이름 */
    private String staffName;

    /** 직원 근무형태 */
    private StaffEmploymentType staffEmploymentType;

}
