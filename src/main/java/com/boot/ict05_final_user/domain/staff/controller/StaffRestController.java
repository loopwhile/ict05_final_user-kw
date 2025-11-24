package com.boot.ict05_final_user.domain.staff.controller;

import com.boot.ict05_final_user.config.security.principal.AppUser;
import com.boot.ict05_final_user.domain.staff.dto.StaffListDTO;
import com.boot.ict05_final_user.domain.staff.dto.StaffModifyFormDTO;
import com.boot.ict05_final_user.domain.staff.dto.StaffWriteFormDTO;
import com.boot.ict05_final_user.domain.staff.entity.StaffProfile;
import com.boot.ict05_final_user.domain.attendance.repository.AttendanceRepository;
import com.boot.ict05_final_user.domain.staff.repository.StaffRepository;
import com.boot.ict05_final_user.domain.staff.service.StaffService;
import com.boot.ict05_final_user.domain.store.entity.Store;
import com.boot.ict05_final_user.domain.store.service.StoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 직원 관련 REST API 컨트롤러.
 *
 * <p>기능:</p>
 * <ul>
 *     <li>직원 목록 조회</li>
 *     <li>직원 등록</li>
 *     <li>직원 수정</li>
 * </ul>
 *
 * <p>검증, 바인딩, 인증 사용자(StoreId) 기반 접근 제어를 수행한다.</p>
 *
 * @author 미리
 * @since 2025.10.21
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@Tag(name = "직원 API", description = "직원 등록/조회/수정 기능 제공")
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class StaffRestController {

    private final StaffService staffService;
    private final StaffRepository staffRepository;
    private final StoreService storeService;
    private final AttendanceRepository attendanceRepository;

    /**
     * 직원 목록 조회 API.
     *
     * @param user 로그인한 사용자 정보(AppUser)
     * @param page 페이지 번호(0부터 시작)
     * @param size 페이지 크기
     * @return 직원 목록 페이지
     */
    @Operation(
            summary = "직원 목록 조회",
            description = "로그인한 사용자의 storeId 기준으로 직원 목록을 조회합니다."
    )
    @GetMapping("/list")
    public Page<StaffListDTO> getStaffList(
            @AuthenticationPrincipal AppUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        log.info("GET /api/staff/list page={}, size={}, storeId={}", page, size, user.getStoreId());

        Long storeId = user.getStoreId();
        Pageable pageable = PageRequest.of(page, size);

        return staffService.selectAllStaff(storeId, pageable);
    }

    /**
     * 직원 등록 API.
     *
     * <p>요청된 직원 정보를 저장하고 생성된 직원 ID를 반환한다.</p>
     *
     * @param dto 등록할 직원 정보
     * @param user 인증 사용자(AppUser)
     * @return 생성된 직원 ID
     */
    @Operation(
            summary = "직원 등록",
            description = "현재 로그인한 매장의 직원 정보를 신규 등록합니다."
    )
    @PostMapping("/add")
    public ResponseEntity<Long> creatStaff(
            @Valid @RequestBody StaffWriteFormDTO dto,
            @AuthenticationPrincipal AppUser user
    ) {
        log.info("POST /api/staff/add dto={}, storeId={}", dto, user.getStoreId());

        Store store = storeService.findById(user.getStoreId());

        StaffProfile staff = StaffProfile.builder()
                .store(store)
                .staffName(dto.getStaffName())
                .staffEmploymentType(dto.getStaffEmploymentType())
                .staffEmail(dto.getStaffEmail())
                .staffPhone(dto.getStaffPhone())
                .staffBirth(dto.getStaffBirth())
                .staffStartDate(dto.getStaffStartDate())
                .build();

        staffRepository.save(staff);

        log.info("직원 등록 완료 id={}", staff.getId());

        return ResponseEntity.ok(staff.getId());
    }

    /**
     * 직원 수정 API.
     *
     * @param id 수정 대상 직원 ID
     * @param dto 수정할 데이터 DTO
     * @param user 인증 사용자(AppUser)
     * @return 204 No Content
     */
    @Operation(
            summary = "직원 정보 수정",
            description = "직원 정보를 수정합니다. 로그인한 사용자의 매장 직원만 수정 가능합니다."
    )
    @PutMapping("/modify/{id}")
    public ResponseEntity<Void> modifyStaff(
            @PathVariable Long id,
            @Valid @RequestBody StaffModifyFormDTO dto,
            @AuthenticationPrincipal AppUser user
    ) {
        log.info("PUT /api/staff/modify/{} dto={}, storeId={}", id, dto, user.getStoreId());

        StaffProfile staff = staffRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("직원이 존재하지 않습니다."));

        // 다른 매장의 직원인지 검증
        if (!staff.getStore().getId().equals(user.getStoreId())) {
            return ResponseEntity.status(403).build();
        }

        // 값 수정
        staff.setStaffName(dto.getStaffName());
        staff.setStaffEmploymentType(dto.getStaffEmploymentType());
        staff.setStaffEmail(dto.getStaffEmail());
        staff.setStaffPhone(dto.getStaffPhone());
        staff.setStaffBirth(dto.getStaffBirth());
        staff.setStaffStartDate(dto.getStaffStartDate());
        staff.setStaffEndDate(dto.getStaffEndDate());

        staffRepository.save(staff);

        return ResponseEntity.noContent().build();
    }

}
