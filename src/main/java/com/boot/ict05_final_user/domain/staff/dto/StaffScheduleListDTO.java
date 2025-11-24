package com.boot.ict05_final_user.domain.staff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 직원 근무 배정(스케줄) 목록 DTO.
 *
 * <p>
 * 직원 스케줄 리스트 조회 시 사용되며,<br>
 * 근무 배정 ID, 근무 예정일, 비고를 포함한다.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "직원 근무 스케줄 목록 DTO")
public class StaffScheduleListDTO {

    /** 근무 배정 시퀀스 */
    @Schema(description = "근무 배정 ID", example = "10")
    private Long id;

    /** 근무 (예정) 일자 */
    @Schema(type = "string", format = "date", description = "근무 예정 일자", example = "2025-11-20")
    private LocalDate staffScheduleWorkDate;

    /** 근무 배정 비고 */
    @Schema(description = "근무 배정 비고", example = "야간 근무, 매장 마감 담당")
    private String staffScheduleMemo;

}
