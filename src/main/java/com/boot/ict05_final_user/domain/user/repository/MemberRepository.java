// src/main/java/com/boot/ict05_final_user/domain/user/repository/MemberRepository.java
package com.boot.ict05_final_user.domain.user.repository;

import com.boot.ict05_final_user.domain.user.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<Member> findByEmail(String email);
}
