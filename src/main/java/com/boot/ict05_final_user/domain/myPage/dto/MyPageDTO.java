package com.boot.ict05_final_user.domain.myPage.dto;

import com.boot.ict05_final_user.domain.user.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 마이페이지에서 사용하는 회원 프로필 정보를 전달하기 위한 DTO.
 *
 * <p>
 * - 기본 회원 정보(id, 이름, 이메일, 전화번호)<br>
 * - 프로필 이미지 경로<br>
 * - 가맹점(지점) 이름<br>
 * 등을 포함한다.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPageDTO {

    /** 회원 ID */
    @Schema(description = "회원 ID")
    private Long id;

    /** 회원 이름 */
    @Schema(description = "회원 이름")
    private String name;

    /** 회원 이메일 */
    @Schema(description = "회원 이메일")
    private String email;

    /** 회원 전화번호 */
    @Schema(description = "회원 전화번호")
    private String phone;

    /** 회원 프로필 이미지 경로 */
    @Schema(description = "회원 프로필 이미지 경로(URL 또는 상대 경로)")
    private String memberImagePath;

    /** 지점명 */
    @Schema(description = "회원이 속한 지점(가맹점) 이름")
    private String storeName;

    /**
     * Member 엔티티를 MyPageDTO 로 변환한다.
     *
     * @param member 회원 엔티티
     * @return 엔티티 값이 매핑된 MyPageDTO
     */
    public static MyPageDTO fromEntity(Member member) {
        return MyPageDTO.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .memberImagePath(member.getMemberImagePath())
                .build();
    }
}
