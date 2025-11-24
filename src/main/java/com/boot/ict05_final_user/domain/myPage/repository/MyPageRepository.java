package com.boot.ict05_final_user.domain.myPage.repository;

import com.boot.ict05_final_user.domain.user.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyPageRepository extends JpaRepository<Member, Long> {
}
