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
 * 근태 등록 DTO.
 *
 * <p>
 * 새로운 근태를 생성할 때 클라이언트로부터 전달되는 데이터 모델이다.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceWriteFormDTO {

    /** 근무 시퀀스 (등록 시에는 보통 null) */
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

    // === 직원 정보 ===

    /** 직원 시퀀스 */
    private Long staffId;

    /** 직원 이름 */
    private String staffName;

    /** 직원 근무형태 */
    private StaffEmploymentType staffEmploymentType;

}
