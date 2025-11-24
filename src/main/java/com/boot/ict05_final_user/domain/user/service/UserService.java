package com.boot.ict05_final_user.domain.user.service;

import com.boot.ict05_final_user.config.security.jwt.service.JwtService;
import com.boot.ict05_final_user.domain.user.dto.UserRequestDTO;
import com.boot.ict05_final_user.domain.user.dto.UserResponseDTO;
import com.boot.ict05_final_user.domain.user.entity.Member;
import com.boot.ict05_final_user.domain.user.entity.MemberStatus;
import com.boot.ict05_final_user.domain.user.repository.UserRepository; // ← Member를 다루는 Repo
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository, JwtService jwtService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /** 존재 여부 (이메일 기준) */
    @Transactional(readOnly = true)
    public Boolean existUser(UserRequestDTO dto) {
        String email = dto.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email 은 필수입니다.");
        }
        return userRepository.existsByEmail(email);
    }

    /** 회원 가입 (이메일 + 비밀번호 + 이름) */
    @Transactional
    public Long addUser(UserRequestDTO dto) {
        String email = dto.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email 은 필수입니다.");
        }

        // 1) 이메일 기준 기존 회원 조회
        Member existing = userRepository.findByEmail(email).orElse(null);

        // 1-1) 이미 ACTIVE 회원이면 가입 불가
        if (existing != null && existing.getStatus() == MemberStatus.ACTIVE) {
            throw new IllegalArgumentException("이미 유저가 존재합니다.");
        }

        // ✅ name 필수 검증 (기존 그대로)
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("name 은 필수입니다.");
        }

        // 2) 기존 WITHDRAW 회원이면 재활성화
        if (existing != null && existing.getStatus() == MemberStatus.WITHDRAW) {
            existing.setName(dto.getName());
            existing.setPhone(dto.getPhone());
            existing.setPassword(passwordEncoder.encode(dto.getPassword()));
            existing.setStatus(MemberStatus.ACTIVE);  // 탈퇴 → 활성 전환

            return existing.getId();
        }

        // 3) 그 외에는 신규 회원 생성
        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .phone(dto.getPhone())
                .status(MemberStatus.ACTIVE)   // 신규는 항상 ACTIVE
                .build();

        return userRepository.save(member).getId();
    }


    /** 로그인용(스프링 시큐리티) — 파라미터 username에 email이 들어옵니다 */
    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member m = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        // 역할 컬럼이 별도로 없다면 ROLE_USER 고정
        return User.builder()
                .username(m.getEmail())
                .password(m.getPassword())
                .roles("USER")
                .build();
    }

    /** 회원 정보 수정 (이메일 기준 본인 확인) */
    @Transactional
    public Long updateUser(UserRequestDTO dto) throws AccessDeniedException {
        String sessionEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!sessionEmail.equals(dto.getEmail())) {
            throw new AccessDeniedException("본인 계정만 수정 가능");
        }

        Member m = userRepository.findByEmail(sessionEmail)
                .orElseThrow(() -> new UsernameNotFoundException(sessionEmail));

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            m.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getName() != null && !dto.getName().isBlank()) {
            m.setName(dto.getName());
        }
        // ✅ 전화번호도 업데이트 허용(선택)
        if (dto.getPhone() != null) {
            m.setPhone(dto.getPhone());
        }

        return userRepository.save(m).getId();
    }


    @Transactional
    public void logout() {
        // 1. SecurityContext 에서 현재 인증된 사용자 이메일 가져오기
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        // 2. 해당 이메일 기준으로 DB에 저장된 refresh 토큰 삭제
        jwtService.removeRefreshUser(email);
    }



    /** 회원 탈퇴 (이메일 기준) */
    @Transactional
    public void deleteUser(UserRequestDTO dto) throws AccessDeniedException {
        String sessionEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        String targetEmail = (dto.getEmail() != null && !dto.getEmail().isBlank())
                ? dto.getEmail()
                : sessionEmail;

        if (!sessionEmail.equals(targetEmail)) {
            throw new AccessDeniedException("본인만 삭제할 수 있습니다.");
        }

        userRepository.deleteByEmail(targetEmail);
        // refresh 토큰도 email 기준으로 제거
        jwtService.removeRefreshUser(targetEmail);
    }

    /** 내 정보 조회 */
    @Transactional(readOnly = true)
    public UserResponseDTO readUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member m = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + email));

        // 기존 UserResponseDTO(username, social, nickname, email) 구조를 유지한다면:
        return new UserResponseDTO(
                m.getEmail(),   // username 자리에 email 주입
                false,          // social 사용 안함
                m.getName(),    // nickname 자리에 name 주입
                m.getEmail()
        );
    }

}
