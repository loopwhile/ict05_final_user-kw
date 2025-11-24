package com.boot.ict05_final_user.domain.store.repository;

import com.boot.ict05_final_user.domain.store.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Store 리포지토리의 커스텀(동적 쿼리) 인터페이스.
 *
 * <p>기본 CRUD를 제공하는 {@code JpaRepository}로 처리하기 어려운
 * 복합 검색/집계 로직을 정의한다. 구현체는 관례상
 * {@code StoreRepositoryCustomImpl}로 작성한다.</p>
 *
 * <h2>역할</h2>
 * <ul>
 *   <li>검색 조건 DTO({@link StoreSearchDTO})를 받아 동적 WHERE 구성</li>
 *   <li>목록 조회는 DTO 프로젝션({@link StoreListDTO})으로 바로 반환</li>
 *   <li>카운트 쿼리는 페이징 total 계산용으로 분리</li>
 * </ul>
 */

public interface StoreRepositoryCustom {
    Page<StoreListDTO> listStore (StoreSearchDTO storeSearchDTO, Pageable pageable);
    /**
     * 검색 조건에 해당하는 전체 건수를 반환한다.
     * <p>페이징 total 계산에 사용되며, 목록 쿼리와 동일한 WHERE 조건을 적용해야 한다.</p>
     *
     * @param storeSearchDTO 검색 조건
     * @return               총 레코드 수
     */
    long countStore(StoreSearchDTO storeSearchDTO);

    List<FindStoreDTO> findStoreName ();

    StoreDetailDTO findByStoreDetail(Long id);

    List<StaffNameDTO> ownerStaffOptions();    // OWNER
    List<StaffNameDTO> hqWorkerStaffOptions(); // WORKER + OFFICE

    // 전체 가맹점 수
    long countStoreAll();
    // 운영 가맹점 수
    long countActiveStore();
    // 평균 월매출
    BigDecimal avgMonthlySales();
    // 총 직원수
    long totalEmployees();

    FindMemberEmailtoIdDTO findMemberByEmail(String email);
}


