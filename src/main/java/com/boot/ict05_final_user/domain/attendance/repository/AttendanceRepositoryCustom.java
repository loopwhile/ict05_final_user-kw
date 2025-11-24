package com.boot.ict05_final_user.domain.attendance.repository;

import com.boot.ict05_final_user.domain.attendance.dto.AttendanceDetailDTO;
import com.boot.ict05_final_user.domain.attendance.dto.AttendanceListDTO;
import com.boot.ict05_final_user.domain.attendance.dto.AttendanceSearchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Attendance 리포지토리 커스텀 인터페이스.
 *
 * <p>QueryDSL 기반 복합 조회 기능을 담당한다.</p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *     <li>가맹점별 하루 근태 목록 조회</li>
 *     <li>근태 상세 조회(직원 JOIN 포함)</li>
 *     <li>근태 단건 삭제</li>
 *     <li>직원 + 날짜 단위 일괄 근태 삭제</li>
 * </ul>
 */
public interface AttendanceRepositoryCustom {

    /**
     * 로그인한 가맹점의 특정 날짜 근태 목록 조회.
     *
     * @param storeId 가맹점 ID
     * @param workDate 조회할 근무 날짜
     * @param pageable 페이징 정보
     * @param searchDto 검색 조건 (직원명/ID/근태 상태 등)
     * @return 페이징된 근태 리스트
     */
    Page<AttendanceListDTO> findDailyAttendanceByStore(
            Long storeId, LocalDate workDate, Pageable pageable, AttendanceSearchDTO searchDto
    );

    /**
     * 근태 상세 조회 (가맹점 검증 포함).
     *
     * @param attendanceId 근태 ID
     * @param storeId 로그인 사용자의 가맹점 ID(보안용)
     * @return 근태 상세 DTO Optional
     */
    Optional<AttendanceDetailDTO> findAttendanceDetailByIdAndStore(Long attendanceId, Long storeId);

    /**
     * 근태 단건 삭제.
     *
     * @param attendanceId 삭제할 근태 ID
     * @param storeId 가맹점 ID
     * @return 삭제된 row 수
     */
    long deleteByIdAndStore(Long attendanceId, Long storeId);

    /**
     * 특정 직원의 특정 날짜 근태 전체 삭제.
     *
     * @param storeId 가맹점 ID
     * @param staffId 직원 ID
     * @param workDate 삭제할 날짜
     * @return 삭제된 row 수
     */
    long deleteByStoreAndStaffAndWorkDate(Long storeId, Long staffId, LocalDate workDate);
}
