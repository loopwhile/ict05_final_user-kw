package com.boot.ict05_final_user.domain.staff.entity;

import com.boot.ict05_final_user.domain.store.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 근태(Attendance) 엔티티.
 *
 * <p>
 * 직원의 일일 근태 기록을 저장하며,<br>
 * 출근/퇴근 시간, 근태 상태, 근무 시간 등을 포함한다.
 * </p>
 *
 * <ul>
 *     <li>직원(StaffProfile) FK</li>
 *     <li>가맹점(Store) FK</li>
 *     <li>근무 일자(LocalDate)</li>
 *     <li>출근/퇴근 시간(LocalDateTime)</li>
 *     <li>근태 상태(AttendanceStatus)</li>
 *     <li>실근무시간(BigDecimal)</li>
 * </ul>
 */
@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "근태(Attendance) 엔티티")
public class Attendance {

    /** 근무 시퀀스 (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id", nullable = false, updatable = false)
    @Schema(description = "근태 ID", example = "101")
    private Long id;

    /** 직원 프로필 (근태는 직원에 종속됨) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id_fk", nullable = false)
    @Schema(description = "근태 대상 직원 프로필")
    private StaffProfile staffProfile;

    /** 가맹점 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id_fk", nullable = false)
    @Schema(description = "근무 매장 정보")
    private Store store;

    /** 근무 일자 */
    @Column(name = "attendance_work_date", nullable = false)
    @Schema(description = "근무 일자", example = "2025-11-20")
    private LocalDate workDate;

    /** 출근 시간 */
    @Column(name = "attendance_check_in")
    @Schema(description = "출근 시간", type = "string", format = "date-time")
    private LocalDateTime checkIn;

    /** 퇴근 시간 */
    @Column(name = "attendance_check_out")
    @Schema(description = "퇴근 시간", type = "string", format = "date-time")
    private LocalDateTime checkOut;

    /** 근태 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    @Schema(description = "근태 상태", example = "LATE")
    private AttendanceStatus status;

    /** 실제 근무 시간 */
    @Column(name = "attendance_work_hours", nullable = false, precision = 6, scale = 2)
    @Schema(description = "실제 근무 시간(시간 단위, 소수점 2자리)", example = "7.50")
    private BigDecimal workHours;

    /** 비고/사유 */
    @Column(name = "attendance_memo", length = 255)
    @Schema(description = "근태 비고 또는 사유", example = "지각 - 교통사고")
    private String memo;

    /** 처음 생성된 시각 */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "레코드 생성 시각", type = "string", format = "date-time")
    private LocalDateTime createdAt;

    /** 마지막으로 수정된 시각 */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "레코드 수정 시각", type = "string", format = "date-time")
    private LocalDateTime updatedAt;

}
