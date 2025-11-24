package com.boot.ict05_final_user.domain.notice.repository;

import com.boot.ict05_final_user.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 공지사항 기본 JPA 리포지토리.
 *
 * <p>Spring Data JPA의 기본 CRUD 메서드를 제공하며,
 * {@link NoticeRepositoryCustom}의 커스텀 쿼리 기능을 함께 확장합니다.</p>
 *
 */
public interface NoticeRepository extends JpaRepository<Notice, Long>, NoticeRepositoryCustom {

}
