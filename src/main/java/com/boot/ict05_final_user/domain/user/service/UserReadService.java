package com.boot.ict05_final_user.domain.user.service;

import com.boot.ict05_final_user.domain.staff.repository.StaffRepository;
import com.boot.ict05_final_user.domain.user.entity.Member;
import com.boot.ict05_final_user.domain.user.repository.MemberRepository;
import com.boot.ict05_final_user.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserReadService {
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public Long findStoreIdByUsername(String username) {
        return userRepository.findStoreIdByUsername(username);
    }

    @Transactional(readOnly = true)
    public Long findMemberIdByUsername(String username) {
        return userRepository.findMamberId(username);
    }

    @Transactional(readOnly = true)
    public String findMemberNameByUsername(String username) {
        return userRepository.findMemberName(username);
    }

    @Transactional(readOnly = true)
    public String findStoreNameByUsername(String username) {
        // username 이 이메일이면 그대로 사용
        return staffRepository.findStoreNameByMemberEmail(username)
                .orElse(null);
    }
}
