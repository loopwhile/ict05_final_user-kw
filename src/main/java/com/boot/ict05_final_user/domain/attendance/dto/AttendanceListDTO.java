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
 * 근태 목록 조회 DTO.
 *
 * <p>
 * 직원들의 하루 근태 요약 정보를 리스트 형태로 조회할 때 사용된다.
 * 상세 정보는 포함하지 않고, 화면 테이블 목록 구성에 필요한 핵심 데이터만 담는다.
 * </p>
 *
 * <p>주요 데이터:</p>
 * <ul>
 *     <li>근무 일자, 출퇴근 시간, 근태 상태</li>
 *     <li>근무 시간</li>
 *     <li>직원 정보(이름, 근무형태 등)</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceListDTO {

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

    // === StaffProfile JOIN ===

    /** 직원 시퀀스 */
    private Long staffId;

    /** 직원 이름 */
    private String staffName;

    /** 직원 근무형태 (점주/직원/알바 등) */
    private StaffEmploymentType staffEmploymentType;

}
