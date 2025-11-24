package com.boot.ict05_final_user.domain.store.service;

import com.boot.ict05_final_user.domain.staff.entity.StaffProfile;
import com.boot.ict05_final_user.domain.staff.repository.StaffRepository;
import com.boot.ict05_final_user.domain.store.dto.*;
import com.boot.ict05_final_user.domain.store.entity.Store;
import com.boot.ict05_final_user.domain.store.repository.StoreRepository;
import com.boot.ict05_final_user.domain.user.entity.Member;
import com.boot.ict05_final_user.domain.user.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 가맹점 관련 비즈니스 로직을 처리하는 서비스 클래스
 *
 * <p>특징</p>
 * <ul>
 *   <li>컨트롤러와 리포지토리 사이에서 트랜잭션과 도메인 규칙을 담당</li>
 *   <li>목록/단건 조회 등 읽기 기능 제공(추후 등록/수정/삭제 확장)</li>
 * </ul>
 *
 */
@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository; // 데이터 접근(기본 CRUD + 커스텀 쿼리) 의존성
    private final StaffRepository staffRepository;
    private final MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager em;


    /**
     * 가맹점 이름으로 필터링하여 공지사항 목록을 페이지 단위로 조회한다.
     *
     * @param storeSearchDTO 작성자 이름 (선택, null 가능)
     * @param pageable       페이지 정보 (페이지 번호, 크기, 정렬)
     * @return 페이징 처리된 공지사항 리스트 DTO
     */
    public Page<StoreListDTO> selectAllOfficeStore(StoreSearchDTO storeSearchDTO, Pageable pageable) {
        return storeRepository.listStore(storeSearchDTO, pageable);   // Querydsl 커스텀 리포지토리 호출
    }

    /**
     * 새로운 가맹점을 등록한다.
     *
     * @param dto 가맹점 등록 정보
     * @return 저장된 가맹점 ID
     */
    public Long insertOfficeStore(StoreWriteFormDTO dto) {
        // ✅ 1. 주소 결합
        String address1 = dto.getUserAddress1();
        String address2 = dto.getUserAddress2();
        String address = (address1 == null ? "" : address1) + "," + (address2 == null ? "" : address2);
        dto.setStoreLocation(address);

        // ✅ 2. 본사 담당자 필수 검증
        if (dto.getHqWorkerStaffId() == null) {
            throw new IllegalArgumentException("본사 담당자는 반드시 선택해야 합니다.");
        }

        // ✅ 3. 본사 담당자 → Member 매핑
        StaffProfile hqWorker = staffRepository.findById(dto.getHqWorkerStaffId())
                .orElseThrow(() -> new IllegalArgumentException("선택한 본사 담당자 정보를 찾을 수 없습니다."));

        if (hqWorker.getStaffEmail() == null || hqWorker.getStaffEmail().isBlank()) {
            throw new IllegalArgumentException("본사 담당자의 이메일 정보가 존재하지 않습니다.");
        }

        FindMemberEmailtoIdDTO mDto = storeRepository.findMemberByEmail(hqWorker.getStaffEmail());
        if (mDto == null || mDto.getId() == null) {
            throw new IllegalArgumentException("본사 담당자 이메일에 해당하는 Member 계정을 찾을 수 없습니다.");
        }

        Member member = em.getReference(Member.class, mDto.getId());

        // ✅ 4. Store 엔티티 생성
        Store store = Store.builder()
                .name(dto.getStoreName())
                .member(member)
                .businessRegistrationNumber(dto.getBusinessRegistrationNumber())
                .phone(dto.getStorePhone())
                .status(dto.getStoreStatus())
                .type(dto.getStoreType())
                .totalEmployees(dto.getStoreTotalEmployees())
                .location(dto.getStoreLocation())
                .contractStartDate(dto.getStoreContractStartDate())
                .contractAffiliateDate(dto.getStoreContractAffiliateDate())
                .contractTerm(dto.getStoreContractTerm())
                .affiliatePrice(dto.getStoreAffiliatePrice())
                .monthlySales(dto.getStoreMonthlySales())
                .royalty(dto.getRoyalty())
                .comment(dto.getComment())
                .build();

        // ✅ 5. 저장
        Store saved = storeRepository.save(store);

        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<StaffNameDTO> ownerOptions() {
        return storeRepository.ownerStaffOptions();
    }

    @Transactional(readOnly = true)
    public List<StaffNameDTO> hqWorkerOptions() {
        return storeRepository.hqWorkerStaffOptions();
    }


    /**
     * 가맹점의 이름(및 필요 시 식별자 등 최소 필드)을 DTO로 조회한다.
     *
     * @return 가맹점 표시용 DTO 리스트. 데이터가 없으면 일반적으로 빈 리스트를 반환.
     */
    public List<FindStoreDTO> findStoreName() {
        return storeRepository.findStoreName();
    }

    /**
     * 가맹점 상세 정보를 조회한다.
     *
     * @param id 재료 ID
     * @return 가맹점 엔티티, 존재하지 않으면 null
     */
    public StoreDetailDTO detailOfficeStore(Long id) {
        return storeRepository.findByStoreDetail(id);
    }

    /**
     * ID를 기준으로 가맹점을 조회한다.
     *
     * @param id 가맹점 ID
     * @return 가맹점 엔티티, 존재하지 않으면 null
     */
    @Transactional(readOnly = true)
    public Store findById(Long id) {
        return storeRepository.findById(id).orElse(null);
    }


    @Transactional(readOnly = true)
    public Map<String, Object> listHeaderStats() {
        long total       = storeRepository.countStoreAll();     // ✅ 인스턴스 호출
        long active      = storeRepository.countActiveStore();  // ✅
        BigDecimal avg   = storeRepository.avgMonthlySales();   // ✅ BigDecimal
        long totalStaff  = storeRepository.totalEmployees();    // ✅ long

        return Map.of(
                "totalStore",       total,
                "activeStore",      active,
                "avgMonthlySales",  avg,        // 키도 의미 맞게
                "totalStaff",       totalStaff
        );
    }

}





