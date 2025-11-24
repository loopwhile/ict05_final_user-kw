package com.boot.ict05_final_user.domain.staff.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

import java.time.LocalTime;

/**
 * 직원 근무시간대(시프트 타입) 엔티티.
 *
 * <p>
 * 특정 시간대(예: 오픈, 미들, 마감 등)에 대한 이름, 시작/종료 시간,<br>
 * 설명을 관리한다.
 * </p>
 */
@Entity
@Table(name = "staff_shift_type")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "직원 근무시간대(시프트 타입) 엔티티")
public class StaffShiftType {

    /** 근무시간대 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_shift_type_id", columnDefinition = "INT UNSIGNED")
    @Comment("근무시간대 시퀀스")
    @Schema(description = "근무시간대 ID", example = "1")
    private Long id;

    /** 근무시간대 이름 */
    @Column(name = "staff_shift_type_name", length = 100, nullable = false)
    @Comment("근무시간대 이름")
    @Schema(description = "근무시간대 이름", example = "오픈(오전)")
    private String TypeName;

    /** 근무 시작 시간 */
    @Column(name = "staff_shift_start_time", nullable = false)
    @Comment("시작")
    @Schema(description = "근무 시작 시간", type = "string", format = "time", example = "09:00:00")
    private LocalTime StartTime;

    /** 근무 종료 시간 */
    @Column(name = "staff_shift_end_time", nullable = false)
    @Comment("종료")
    @Schema(description = "근무 종료 시간", type = "string", format = "time", example = "18:00:00")
    private LocalTime EndTime;

    /** 근무시간대 설명 */
    @Column(name = "staff_shift_memo", columnDefinition = "TEXT")
    @Comment("근무시간대 설명")
    @Schema(description = "근무시간대 비고/설명", example = "오픈 준비 및 오전 운영 담당")
    private String ShiftMemo;
}
