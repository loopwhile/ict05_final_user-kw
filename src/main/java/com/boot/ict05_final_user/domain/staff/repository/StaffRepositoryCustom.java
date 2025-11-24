package com.boot.ict05_final_user.domain.staff.repository;

import com.boot.ict05_final_user.domain.staff.dto.StaffListDTO;
import com.boot.ict05_final_user.domain.staff.dto.StaffSearchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 직원 커스텀 리포지토리(동적 검색, 페이징 등).
 *
 * <p>QueryDSL 기반으로 직원 목록을 DTO 형태로 조회한다.</p>
 */
public interface StaffRepositoryCustom {

    /**
     * 직원 목록 + 근태 최신 상태 포함 조회.
     *
     * @param staffSearchDTO 검색 조건
     * @param pageable 페이징 정보
     * @return 직원 리스트 페이징 결과
     */
    Page<StaffListDTO> listStaff(StaffSearchDTO staffSearchDTO, Pageable pageable);
}
