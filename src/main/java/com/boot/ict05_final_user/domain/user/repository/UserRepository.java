package com.boot.ict05_final_user.domain.user.repository;

import com.boot.ict05_final_user.domain.user.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Member, Long>, UserRepositoryCustom {

    // username 대신 email을 아이디로 사용
    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    @Transactional
    void deleteByEmail(String email);
}
