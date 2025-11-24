package com.boot.ict05_final_user.domain.staff.repository;

import com.boot.ict05_final_user.domain.staff.entity.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 직원(StaffProfile) 엔티티용 Spring Data JPA 리포지토리.
 *
 * <p>기능 요약</p>
 * <ul>
 *     <li>기본 CRUD 기능(JpaRepository)</li>
 *     <li>직원 커스텀 쿼리(StaffRepositoryCustom) 포함</li>
 *     <li>마이페이지용 member_id 기반 직원 조회</li>
 *     <li>로그인 후 지점명 조회(지연 로딩 이슈 해결용 JPQL)</li>
 * </ul>
 */
public interface StaffRepository extends JpaRepository<StaffProfile, Long>, StaffRepositoryCustom {

    /**
     * member_id(FK)로 직원 프로필을 조회한다.
     *
     * @param memberId 연결된 Member ID
     * @return StaffProfile Optional
     */
    Optional<StaffProfile> findByMember_Id(Long memberId);

    /**
     * 로그인 후 상단 메뉴에서 지점명 표시할 때 사용.
     * <p>LAZY 로딩으로 store.name 호출 시 터지는 문제 방지용.</p>
     *
     * @param email 로그인한 직원의 이메일
     * @return 지점명 Optional
     */
    @Query("select s.store.name from StaffProfile s " +
            "join s.store " +
            "where s.member.email = :email")
    Optional<String> findStoreNameByMemberEmail(@Param("email") String email);
}
