package com.boot.ict05_final_user.domain.staff.dto;

import com.boot.ict05_final_user.domain.staff.entity.StaffEmploymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 직원 프로필 수정 폼 DTO.
 *
 * <p>
 * 직원 정보 수정 시 클라이언트에서 전달되는 데이터를 담는다.<br>
 * 이름, 근무 형태, 이메일, 연락처, 생년월일, 입사일자, 퇴사일자 등을 포함한다.
 * </p>
 *
 * <p>
 * 검증 규칙 요약:
 * <ul>
 *     <li>staffName, staffEmploymentType, staffEmail, staffPhone, staffBirth 는 필수</li>
 *     <li>staffEmail 은 이메일 형식</li>
 *     <li>staffPhone 은 숫자+하이픈만 허용</li>
 *     <li>staffBirth 는 과거, staffStartDate/staffEndDate 는 과거 또는 오늘</li>
 * </ul>
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "직원 정보 수정 폼 DTO")
public class StaffModifyFormDTO {

    /** 수정할 사원의 고유 ID */
    @Schema(description = "수정 대상 사원 ID", example = "1")
    private Long id;

    /** 사원 이름 */
    @Schema(description = "사원 이름", example = "홍길동")
    @NotNull(message = "직원 이름을 입력해주세요")
    private String staffName;

    /** 직무 형태 (점주, 직원, 알바) */
    @Schema(description = "근무 형태(점주/직원/알바)", example = "WORKER")
    @NotNull(message = "근무 형태를 선택해주세요")
    private StaffEmploymentType staffEmploymentType;

    /** 사원 이메일 */
    @Schema(description = "사원 이메일", example = "staff@example.com")
    @NotNull(message = "이메일을 입력해주세요")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String staffEmail;

    /** 사원 연락처 */
    @Schema(description = "사원 연락처 (숫자+하이픈)", example = "010-1234-5678")
    @NotNull(message = "연락처를 입력해주세요")
    @Pattern(regexp = "^[0-9\\-]{9,13}$", message = "연락처는 숫자와 하이픈만 입력해주세요")
    private String staffPhone;

    /** 생년월일 (과거) */
    @Schema(type = "string", format = "date-time", description = "생년월일(과거 날짜)")
    @NotNull(message = "생년월일을 입력해주세요")
    @Past(message = "생년월일은 과거여야 합니다")
    private LocalDateTime staffBirth;

    /** 입사일자 (선택, 과거 또는 현재) */
    @Schema(type = "string", format = "date-time", description = "입사일자(과거 또는 오늘)")
    @PastOrPresent(message = "입사일자는 과거 또는 오늘이어야 합니다")
    private LocalDateTime staffStartDate;

    /** 퇴사일자 (선택, 과거 또는 현재, 재직 중이면 null) */
    @Schema(type = "string", format = "date-time", description = "퇴사일자(재직 중이면 null)")
    @PastOrPresent(message = "퇴사일자는 과거 또는 오늘이어야 합니다")
    private LocalDateTime staffEndDate;

}
