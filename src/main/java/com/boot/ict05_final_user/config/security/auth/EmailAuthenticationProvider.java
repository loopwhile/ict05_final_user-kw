// src/main/java/com/boot/ict05_final_user/security/EmailAuthenticationProvider.java
package com.boot.ict05_final_user.config.security.auth;

import com.boot.ict05_final_user.domain.user.entity.Member;
import com.boot.ict05_final_user.domain.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EmailAuthenticationProvider implements AuthenticationProvider {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();               // LoginFilter가 setName(email)
        String rawPw  = (String) authentication.getCredentials();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("계정이 존재하지 않습니다."));

        if (!passwordEncoder.matches(rawPw, member.getPassword())) {
            throw new BadCredentialsException("비밀번호가 올바르지 않습니다.");
        }

        // 필요 시 권한 전략 변경
        List<SimpleGrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // principal에 email 또는 memberId/이름 등 원하는 값을 넣으세요.
        return new UsernamePasswordAuthenticationToken(member.getEmail(), null, auths);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
