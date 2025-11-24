package com.boot.ict05_final_user.domain.staff.entity;

import com.boot.ict05_final_user.domain.store.entity.Store;
import com.boot.ict05_final_user.domain.user.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 직원 프로필 엔티티.
 *
 * <p>
 * 매장에 소속된 직원(점주/직원/알바)의 기본 정보를 나타내며,<br>
 * 매장(Store), 회원(Member)와의 관계, 인적사항, 입·퇴사일 등을 포함한다.
 * </p>
 */
@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "직원 프로필 엔티티")
public class StaffProfile {

    /** 직원 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    @Schema(description = "직원 ID", example = "1")
    private Long id;

    /** 소속 매장 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id_fk", nullable = false)
    @Schema(description = "직원의 근무 매장")
    private Store store;

    /**
     * 회원 계정과의 1:1 매핑.
     *
     * <p>회원(Member)와 연결된 직원인 경우에만 값이 존재한다.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "member_id_fk",
            referencedColumnName = "member_id",
            foreignKey = @ForeignKey(name = "fk_staff_profile__member"),
            nullable = true,
            unique = true
    )
    @Schema(description = "연결된 회원 계정(선택)")
    private Member member;

    /** 직원 이름 */
    @Column(name = "staff_name", length = 100)
    @Schema(description = "직원 이름", example = "홍길동")
    private String staffName;

    /** 직원 근무형태 (점주/직원/알바) */
    @Enumerated(EnumType.STRING)
    @Column(name = "staff_employment_type")
    @Schema(description = "근무 형태(점주/직원/알바)", example = "WORKER")
    private StaffEmploymentType staffEmploymentType;

    /** 직원 부서 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "직원 부서", example = "STORE")
    private StaffDepartment staffDepartment = StaffDepartment.STORE; // 기본값 지정

    /** 직원 이메일 */
    @Column(name = "staff_email")
    @Schema(description = "직원 이메일", example = "staff@example.com")
    private String staffEmail;

    /** 직원 전화번호 */
    @Column(name = "staff_phone", length = 50)
    @Schema(description = "직원 전화번호", example = "010-1234-5678")
    private String staffPhone;

    /** 직원 주소 */
    @Column(name = "staff_address")
    @Schema(description = "직원 주소", example = "서울시 강남구 테헤란로 123, 3층")
    private String staffAddress;

    /** 직원 생년월일 */
    @Schema(type = "string", format = "date-time", description = "직원 생년월일")
    @Column(name = "staff_birth")
    private LocalDateTime staffBirth;

    /** 직원 입사일자 (혹은 매장 근무 시작일) */
    @Schema(type = "string", format = "date-time", description = "입사일자(근무 시작일)")
    @Column(name = "staff_start_date")
    private LocalDateTime staffStartDate;

    /** 직원 퇴사일자 */
    @Schema(type = "string", format = "date-time", description = "퇴사일자(재직 중이면 null)")
    @Column(name = "staff_end_date")
    private LocalDateTime staffEndDate;

}
