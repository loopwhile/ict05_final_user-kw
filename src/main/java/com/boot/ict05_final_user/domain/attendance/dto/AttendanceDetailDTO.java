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
 * 근태 상세 조회 DTO.
 *
 * <p>특정 근태 기록을 상세 조회할 때 사용되는 DTO이며,
 * 리스트 DTO보다 더 많은 정보(근태 사유, 직원 정보 등)를 포함한다.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceDetailDTO {

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

    // === StaffProfile JOIN ===

    /** 직원 시퀀스 */
    private Long staffId;

    /** 직원 이름 */
    private String staffName;

    /** 직원 근무형태 */
    private StaffEmploymentType staffEmploymentType;

}
