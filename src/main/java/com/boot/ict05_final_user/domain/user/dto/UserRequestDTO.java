package com.boot.ict05_final_user.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDTO {

    public interface existGroup {}    // 존재 확인
    public interface addGroup {}      // 회원가입
    public interface passwordGroup {} // 비번 변경
    public interface updateGroup {}   // 수정
    public interface deleteGroup {}   // 삭제

    // === 하위호환 필드(선택) ===
//    @Size(min = 4)                // 더이상 필수 아님
//    private String username;      // 예전 프론트가 email을 여기 넣어 보낼 수도 있음
//    private String nickname;      // 예전 '닉네임' 대용

    // === 신규/권장 필드 ===
    @NotBlank(groups = {addGroup.class, updateGroup.class})
    @Email(groups = {addGroup.class, updateGroup.class, existGroup.class})
    private String email;         // 로그인/가입의 주 식별자

    @NotBlank(groups = {addGroup.class, passwordGroup.class})
    @Size(min = 4, groups = {addGroup.class, passwordGroup.class})
    private String password;

    @NotBlank(groups = {addGroup.class, updateGroup.class})
    private String name;          // 프론트에서 입력하는 '이름'

    private String phone;

}
