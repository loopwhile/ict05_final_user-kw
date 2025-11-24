package com.boot.ict05_final_user.config.security.auth;

import com.boot.ict05_final_user.domain.user.entity.UserRoleType;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * JWT 기반 로그인 시 SecurityContext에 저장되는 사용자 정보 클래스
 * → 로그인한 가맹점주의 storeId를 담을 수 있음
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;        // 유저 ID
    private final Long storeId;   // ✅ 가맹점 ID
    private final String email;   // 로그인 이메일
    private final String password;
    private final UserRoleType role;

    public CustomUserDetails(Long id, Long storeId, String email, String password, UserRoleType role) {
        this.id = id;
        this.storeId = storeId;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    /** ✅ 가맹점 ID 반환 */
    public Long getStoreId() {
        return storeId;
    }

    /** ✅ 스프링 시큐리티 권한 반환 */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    // ✅ 계정 상태 관련 설정 (일단 전부 true)
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
