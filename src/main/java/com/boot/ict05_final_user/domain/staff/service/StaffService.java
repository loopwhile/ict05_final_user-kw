package com.boot.ict05_final_user.domain.staff.service;

import com.boot.ict05_final_user.config.security.auth.CustomUserDetails;
import com.boot.ict05_final_user.domain.staff.dto.StaffListDTO;
import com.boot.ict05_final_user.domain.staff.dto.StaffSearchDTO;
import com.boot.ict05_final_user.domain.attendance.repository.AttendanceRepository;
import com.boot.ict05_final_user.domain.staff.repository.StaffRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직원 관련 비즈니스 로직을 처리하는 서비스 클래스.
 *
 * <p>주요 역할</p>
 * <ul>
 *     <li>현재 로그인한 점주의 매장(storeId)에 따른 직원 목록 조회</li>
 *     <li>검색 DTO 구성 후 Repository(QueryDSL) 호출</li>
 *     <li>인증 정보에서 storeId 추출</li>
 * </ul>
 */
@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class StaffService {

    private final StaffRepository staffRepository;
    private final AttendanceRepository attendanceRepository;

    /** EntityManager (필요 시 근태 최신값 서브쿼리 등 직접 조회 가능) */
    @PersistenceContext
    private EntityManager em;

    /**
     * 로그인한 가맹점(storeId)의 직원 목록을 조회한다.
     *
     * <p>특징</p>
     * <ul>
     *     <li>storeId가 null이면 전체 조회(관리자 모드)</li>
     *     <li>storeId가 존재하면 해당 매장의 직원만 조회</li>
     *     <li>QueryDSL 기반 페이징된 DTO 리스트 반환</li>
     * </ul>
     *
     * @param storeId 로그인 사용자 소속 매장 ID
     * @param pageable 페이징 정보(Page 번호·개수)
     * @return 직원 목록 페이지
     */
    public Page<StaffListDTO> selectAllStaff(Long storeId, Pageable pageable) {

        StaffSearchDTO searchDTO = new StaffSearchDTO();
        searchDTO.setStoreId(storeId);

        log.info("직원 목록 조회 요청 - storeId: {}, page: {}, size: {}",
                storeId != null ? storeId : "전체조회",
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return staffRepository.listStaff(searchDTO, pageable);
    }

    /**
     * Spring Security 인증 정보에서 현재 로그인한 사용자의 storeId를 가져온다.
     *
     * <p>리턴 규칙</p>
     * <ul>
     *     <li>로그인 상태(CustomUserDetails) → storeId 반환</li>
     *     <li>anonymousUser(비로그인) → null</li>
     *     <li>인증 정보 없음 → null</li>
     * </ul>
     *
     * @return 로그인한 사용자의 storeId 또는 null
     */
    private Long getCurrentStoreId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            log.warn("인증 정보 없음 → 전체 직원 조회 (관리자용 혹은 비로그인)");
            return null;
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof CustomUserDetails user) {
            Long storeId = user.getStoreId();
            log.debug("현재 로그인 사용자 storeId: {}", storeId);
            return storeId;
        }

        if (principal instanceof String s && "anonymousUser".equals(s)) {
            log.warn("anonymousUser → 전체 조회 허용 (임시)");
            return null;
        }

        log.warn("예상치 못한 principal 타입: {}", principal.getClass());
        return null;
    }

}
