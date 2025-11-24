package com.boot.ict05_final_user.domain.attendance.repository;

import com.boot.ict05_final_user.domain.staff.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

/**
 * Attendance 엔티티용 Spring Data JPA 리포지토리 인터페이스.
 *
 * <p>특징</p>
 * <ul>
 *     <li>{@link JpaRepository} 상속으로 기본 CRUD 및 페이징/정렬 기능 제공</li>
 *     <li>{@link AttendanceRepositoryCustom} 상속으로 QueryDSL 기반 커스텀 쿼리 기능 확장</li>
 * </ul>
 *
 * <p>추가 기능:</p>
 * <ul>
 *     <li>{@code existsByStaffProfileIdAndWorkDate} : 특정 직원이 특정 날짜에 이미 근태 기록이 있는지 검증</li>
 * </ul>
 */
public interface AttendanceRepository
        extends JpaRepository<Attendance, Long>, AttendanceRepositoryCustom {

    /**
     * 특정 직원의 특정 날짜 근태 기록 존재 여부 확인.
     *
     * @param staffId 직원 ID
     * @param workDate 근태 일자
     * @return true = 이미 기록이 존재함
     */
    boolean existsByStaffProfileIdAndWorkDate(Long staffId, LocalDate workDate);
}
