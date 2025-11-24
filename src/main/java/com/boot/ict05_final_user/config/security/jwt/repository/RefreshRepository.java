package com.boot.ict05_final_user.config.security.jwt.repository;


import com.boot.ict05_final_user.config.security.jwt.entity.RefreshEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface RefreshRepository extends JpaRepository<RefreshEntity, Long> {

    // jwt refresh 토큰 기반 존재 확인 메소드
    Boolean existsByRefresh(String refreshToken);

    // jwt refresh 토큰 기반 삭제 메소드
    @Transactional
    void deleteByRefresh(String refresh);

    // jwt 발급 username 기반 삭제 메소드(탈퇴시)
    @Transactional
    void deleteByUsername(String username);

    // 특정일 지난 refresh 토큰 삭제
    @Transactional
    void deleteByCreatedDateBefore(LocalDateTime createdDate);

}