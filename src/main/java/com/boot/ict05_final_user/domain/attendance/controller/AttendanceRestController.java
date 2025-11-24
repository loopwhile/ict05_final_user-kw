package com.boot.ict05_final_user.domain.attendance.controller;

import com.boot.ict05_final_user.config.security.principal.AppUser;
import com.boot.ict05_final_user.domain.attendance.dto.*;
import com.boot.ict05_final_user.domain.attendance.service.AttendanceService;
import com.boot.ict05_final_user.domain.staff.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 직원 근태 관련 REST API 컨트롤러.
 *
 * <p>주요 기능:</p>
 * <ul>
 *     <li>하루 근태 목록 조회(검색/필터/페이징)</li>
 *     <li>근태 등록</li>
 *     <li>근태 상세 조회</li>
 *     <li>근태 수정 폼 조회 및 수정</li>
 *     <li>특정 직원의 특정 날짜 근태 삭제</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "직원근태 API", description = "직원근태 조회/등록/수정/삭제 기능 제공")
public class AttendanceRestController {

    private final AttendanceService attendanceService;

    /**
     * 로그인한 가맹점의 특정 날짜 하루 근태 목록을 조회한다.
     *
     * <p>
     * - 날짜(date) 기준으로 필터링<br>
     * - 검색어/검색 타입/근태 상태 필터 제공<br>
     * - 페이징(Page, Size) 지원
     * </p>
     *
     * 예시: {@code GET /api/attendance/daily?date=2025-11-25&page=0&size=10}
     *
     * @param date 조회할 근무 일자(yyyy-MM-dd)
     * @param page 페이지 번호(0부터 시작)
     * @param size 페이지 크기
     * @param keyword 검색어(선택)
     * @param type 검색 타입(선택)
     * @param attendanceStatus 근태 상태 필터(선택)
     * @return 근태 목록 페이지
     */
    @Operation(
            summary = "하루 근태 목록 조회",
            description = "로그인한 가맹점의 특정 날짜에 대한 직원 근태 목록을 페이징하여 조회합니다."
    )
    @GetMapping("/daily")
    public Page<AttendanceListDTO> getDailyAttendance(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) AttendanceStatus attendanceStatus
    ) {
        Pageable pageable = PageRequest.of(page, size);

        AttendanceSearchDTO searchDto = new AttendanceSearchDTO();
        searchDto.setKeyword(keyword);
        searchDto.setType(type);
        searchDto.setAttendanceStatus(attendanceStatus);

        log.info("📌 AttendanceRestController - 하루 근태 조회: date={}, page={}, size={}, keyword={}, type={}, status={}",
                date, page, size, keyword, type, attendanceStatus);

        // Service에서 자동으로 로그인한 사용자의 storeId 가져감
        return attendanceService.getDailyAttendance(date, pageable, searchDto);
    }

    /**
     * 직원 근태 등록 API.
     *
     * <p>근태 데이터를 저장하고 생성된 근태 ID를 반환한다.</p>
     *
     * @param dto 등록할 근태 정보 DTO
     * @param user 로그인한 사용자(AppUser) – 소속 매장(storeId) 사용
     * @return 생성된 근태 ID
     */
    @Operation(
            summary = "근태 등록",
            description = "직원 근태 정보를 등록하고 생성된 근태 ID를 반환합니다."
    )
    @PostMapping("/add")
    public ResponseEntity<Long> addAttendance(
            @Valid @RequestBody AttendanceWriteFormDTO dto,
            @AuthenticationPrincipal AppUser user
    ) {
        log.info("POST /api/attendance/add dto={}, storeId={}", dto, user.getStoreId());

        Long attendanceId = attendanceService.createAttendance(dto, user.getStoreId());

        log.info("근태 등록 완료 id={}", attendanceId);
        return ResponseEntity.ok(attendanceId);
    }

    /**
     * 근태 상세 조회.
     *
     * <p>근태 ID를 기준으로 단건 상세 정보를 조회한다.</p>
     *
     * 예시: {@code GET /api/attendance/detail/10}
     *
     * @param attendanceId 근태 ID
     * @return 근태 상세 정보 DTO
     */
    @Operation(
            summary = "근태 상세 조회",
            description = "근태 ID를 이용하여 단건 근태 상세 정보를 조회합니다."
    )
    @GetMapping("/detail/{attendanceId}")
    public ResponseEntity<AttendanceDetailDTO> getAttendanceDetail(
            @PathVariable Long attendanceId
    ) {
        log.info("📌 AttendanceRestController - 근태 상세 조회: id={}", attendanceId);
        AttendanceDetailDTO detail = attendanceService.getAttendanceDetail(attendanceId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 근태 수정 폼 조회.
     *
     * <p>수정 화면에 표시할 기존 근태 정보를 조회한다.</p>
     *
     * 예시: {@code GET /api/attendance/modify/10}
     *
     * @param attendanceId 근태 ID
     * @param user 로그인한 사용자(AppUser)
     * @return 수정 폼용 근태 DTO
     */
    @Operation(
            summary = "근태 수정 폼 조회",
            description = "근태 수정 화면에 사용할 기존 근태 정보를 조회합니다."
    )
    @GetMapping("/modify/{attendanceId}")
    public ResponseEntity<AttendanceModifyFormDTO> getAttendanceModifyForm(
            @PathVariable Long attendanceId,
            @AuthenticationPrincipal AppUser user
    ) {
        log.info("📌 AttendanceRestController - 근태 수정 폼 조회: id={}, storeId={}", attendanceId, user.getStoreId());
        AttendanceModifyFormDTO dto = attendanceService.getAttendanceModifyForm(attendanceId);
        return ResponseEntity.ok(dto);
    }

    /**
     * 근태 수정 저장.
     *
     * <p>경로 변수의 근태 ID와 요청 본문의 수정 데이터를 이용해 근태 정보를 수정한다.</p>
     *
     * 예시: {@code PUT /api/attendance/modify/10}
     *
     * @param attendanceId 수정 대상 근태 ID(path variable)
     * @param dto 수정 데이터 DTO
     * @param user 로그인한 사용자(AppUser)
     * @return 200 OK (Body 없음)
     */
    @Operation(
            summary = "근태 수정",
            description = "근태 ID와 수정 데이터를 이용해 기존 근태 정보를 수정합니다."
    )
    @PutMapping("/modify/{attendanceId}")
    public ResponseEntity<Void> updateAttendance(
            @PathVariable Long attendanceId,
            @Valid @RequestBody AttendanceModifyFormDTO dto,
            @AuthenticationPrincipal AppUser user
    ) {
        log.info("📌 AttendanceRestController - 근태 수정 요청: pathId={}, dto={}, storeId={}",
                attendanceId, dto, user.getStoreId());

        dto.setAttendanceId(attendanceId);  // path 변수를 DTO에 세팅
        attendanceService.modifyAttendance(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 직원의 특정 날짜 근태 전체 삭제.
     *
     * <p>
     * - 해당 직원의 지정된 날짜에 기록된 근태를 모두 삭제<br>
     * - 주로 수정 실수/재등록 시 사용하는 용도
     * </p>
     *
     * 예시: {@code DELETE /api/attendance/daily/staff?date=2025-11-25&staffId=123}
     *
     * @param date 삭제 대상 날짜
     * @param staffId 직원 ID
     * @param user 로그인한 사용자(AppUser)
     * @return 204 No Content
     */
    @Operation(
            summary = "특정 직원 하루 근태 삭제",
            description = "특정 직원의 지정된 날짜의 근태 기록을 모두 삭제합니다."
    )
    @DeleteMapping("/daily/staff")
    public ResponseEntity<Void> deleteDailyAttendanceForStaff(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("staffId") Long staffId,
            @AuthenticationPrincipal AppUser user
    ) {
        log.info("🗑️ DELETE /api/attendance/daily/staff?date={}&staffId={} (storeId={})",
                date, staffId, user != null ? user.getStoreId() : null);

        attendanceService.deleteDailyAttendanceForStaff(staffId, date);
        return ResponseEntity.noContent().build();
    }
}
