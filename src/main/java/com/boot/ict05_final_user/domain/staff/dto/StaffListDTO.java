package com.boot.ict05_final_user.domain.staff.dto;

import com.boot.ict05_final_user.domain.staff.entity.AttendanceStatus;
import com.boot.ict05_final_user.domain.staff.entity.StaffEmploymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 직원 목록 조회용 DTO.
 *
 * <p>
 * 직원 리스트 화면 및 직원 목록 API에서 사용되는 가벼운 프로젝션 DTO로,<br>
 * 기본 인적사항과 근무형태, 근태 상태를 포함한다.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "직원 목록 조회 DTO")
public class StaffListDTO {

    /** 사원 시퀀스 */
    @Schema(description = "사원 ID", example = "1")
    private Long id;

    /** 사원 이름 */
    @Schema(description = "사원 이름", example = "홍길동")
    private String staffName;

    /** 사원 생년월일 */
    @Schema(type = "string", format = "date-time", description = "사원 생년월일")
    private LocalDateTime staffBirth;

    /** 사원 전화번호 */
    @Schema(type = "string", example = "010-1234-5678", description = "사원 연락처 (하이픈 포함)")
    private String staffPhone;

    /** 사원 이메일 */
    @Schema(description = "사원 이메일", example = "staff@example.com")
    private String staffEmail;

    /** 사원 근무형태 (점주/직원/알바) */
    @Schema(description = "근무 형태(점주/직원/알바)", example = "WORKER")
    private StaffEmploymentType staffEmploymentType;

    /** 사원 입사일자 (혹은 매장 근무 시작일) */
    @Schema(type = "string", format = "date-time", description = "입사일자(매장 근무 시작일)")
    private LocalDateTime staffStartDate;

    /** 사원 퇴사일자 */
    @Schema(type = "string", format = "date-time", description = "퇴사일자(재직 중일 경우 null)")
    private LocalDateTime staffEndDate;

    /** 근태 상태 */
    @Schema(description = "현재 근태 상태", example = "WORKING")
    private AttendanceStatus attendanceStatus;

}
