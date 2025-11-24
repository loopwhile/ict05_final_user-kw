package com.boot.ict05_final_user.domain.attendance.dto;

import com.boot.ict05_final_user.domain.staff.entity.AttendanceStatus;
import lombok.Data;

/**
 * 근태 조회 검색 조건 DTO.
 *
 * <p>
 * 검색어, 상태, 페이지 크기 등
 * 근태 리스트 조회 시 필요한 조건을 저장한다.
 * </p>
 */
@Data
public class AttendanceSearchDTO {

    /** 검색어 (직원명, 직원 ID 등) */
    private String keyword;

    /** 검색 타입 (name, status, all 등) */
    private String type;

    /** 페이지 사이즈 */
    private String size = "10";

    /** 가맹점 ID */
    private Long storeId;

    /** 근태 상태 필터 (NORMAL, LATE, ABSENT 등) */
    private AttendanceStatus attendanceStatus;
}
