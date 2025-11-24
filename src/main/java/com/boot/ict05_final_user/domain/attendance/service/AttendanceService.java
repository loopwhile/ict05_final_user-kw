package com.boot.ict05_final_user.domain.attendance.service;

import com.boot.ict05_final_user.domain.staff.entity.StaffProfile;
import com.boot.ict05_final_user.config.security.auth.CustomUserDetails;
import com.boot.ict05_final_user.config.security.principal.AppUser;
import com.boot.ict05_final_user.domain.attendance.dto.*;
import com.boot.ict05_final_user.domain.attendance.repository.AttendanceRepository;
import com.boot.ict05_final_user.domain.staff.entity.Attendance;
import com.boot.ict05_final_user.domain.staff.entity.AttendanceStatus;
import com.boot.ict05_final_user.domain.store.entity.Store;
import com.boot.ict05_final_user.domain.staff.repository.StaffRepository;
import com.boot.ict05_final_user.domain.store.repository.StoreRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 근태(Attendance) 도메인 서비스.
 *
 * <p>주요 역할:</p>
 * <ul>
 *     <li>가맹점(storeId) 기준으로 하루 근태 목록 조회</li>
 *     <li>직원 근태 등록/수정/삭제 비즈니스 로직 처리</li>
 *     <li>현재 로그인한 사용자의 storeId 기반 보안 검증</li>
 *     <li>근무 시간 계산 등 공통 유틸 기능 제공</li>
 * </ul>
 */
@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StaffRepository staffRepository;
    private final StoreRepository storeRepository;

    @PersistenceContext
    private EntityManager em;

    /* ================== 하루 리스트 조회 ================== */

    /**
     * 검색 조건 없이, 지정된 날짜의 근태 리스트를 조회하는 기본 버전.
     *
     * <p>내부적으로 {@link #getDailyAttendance(LocalDate, Pageable, AttendanceSearchDTO)} 를
     * 검색 조건 null로 호출한다.</p>
     *
     * @param workDate 조회할 근무 일자
     * @param pageable 페이징 정보
     * @return 페이징된 근태 리스트
     */
    public Page<AttendanceListDTO> getDailyAttendance(LocalDate workDate, Pageable pageable) {
        return getDailyAttendance(workDate, pageable, null);
    }

    /**
     * 검색/필터가 적용된 하루 근태 목록 조회.
     *
     * <p>현재 로그인한 사용자의 storeId를 기준으로 해당 매장의 근태만 조회하며,
     * 검색 DTO에 포함된 keyword/type/status를 조건으로 사용한다.</p>
     *
     * @param workDate   조회할 근무 일자
     * @param pageable   페이징 정보
     * @param searchDto  검색/필터 조건 (null 가능)
     * @return 페이징된 근태 리스트
     */
    @Transactional(readOnly = true)
    public Page<AttendanceListDTO> getDailyAttendance(LocalDate workDate,
                                                      Pageable pageable,
                                                      AttendanceSearchDTO searchDto) {

        Long storeId = getCurrentStoreId();

        if (storeId == null) {
            log.warn("storeId 없음 → 가맹점주가 아닌 사용자 or 비로그인. 근태 조회 불가.");
            return Page.empty(pageable);
        }

        log.info("하루 근태 조회 요청 - storeId: {}, date: {}, page: {}, size: {}, keyword={}, type={}, status={}",
                storeId,
                workDate,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                searchDto != null ? searchDto.getKeyword() : null,
                searchDto != null ? searchDto.getType() : null,
                (searchDto != null && searchDto.getAttendanceStatus() != null)
                        ? searchDto.getAttendanceStatus().name()
                        : null
        );

        return attendanceRepository.findDailyAttendanceByStore(storeId, workDate, pageable, searchDto);
    }

    /* ================== 근태 등록 ================== */

    /**
     * 직원 근태 등록.
     *
     * <p>로직 요약:</p>
     * <ol>
     *     <li>직원 조회 및 매장(storeId) 일치 여부 검증</li>
     *     <li>출근/퇴근 시간 유효성 검증 (null/역전 여부)</li>
     *     <li>이미 해당 날짜에 근태가 존재하는지 중복 체크</li>
     *     <li>근무 시간 계산(분 → 시간 단위 BigDecimal)</li>
     *     <li>근태 상태 기본값 NORMAL 처리</li>
     *     <li>Attendance 엔티티 생성 및 저장</li>
     * </ol>
     *
     * @param dto     근태 등록 DTO
     * @param storeId 로그인한 가맹점주의 storeId
     * @return 생성된 근태 ID
     */
    public Long createAttendance(AttendanceWriteFormDTO dto, Long storeId) {

        // 1) 직원 조회
        StaffProfile staff = staffRepository.findById(dto.getStaffId())
                .orElseThrow(() -> new IllegalArgumentException("직원이 존재하지 않습니다."));

        // 2) 매장 검증
        if (storeId == null || staff.getStore() == null
                || !staff.getStore().getId().equals(storeId)) {
            throw new IllegalArgumentException("해당 매장의 직원이 아니므로 근태를 등록할 수 없습니다.");
        }

        // ⭐ storeId로 Store 조회
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("잘못된 매장(storeId)입니다."));

        // 3) 출퇴근 시간 검증
        LocalDateTime checkIn = dto.getAttendanceCheckIn();
        LocalDateTime checkOut = dto.getAttendanceCheckOut();

        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("출근/퇴근 시간은 필수입니다.");
        }

        if (checkIn.isAfter(checkOut)) {
            throw new IllegalArgumentException("출근 시간이 퇴근 시간보다 늦을 수 없습니다.");
        }

        // 4) 중복 근태 체크
        boolean exists = attendanceRepository.existsByStaffProfileIdAndWorkDate(
                staff.getId(), dto.getAttendanceWorkDate());

        if (exists) {
            throw new IllegalStateException("이미 해당 날짜에 등록된 근태가 있습니다.");
        }

        // 5) 근무 시간 계산
        BigDecimal workHours = calculateWorkHours(checkIn, checkOut);

        // 6) 근태 상태 (입력 없으면 NORMAL)
        AttendanceStatus status = dto.getAttendanceStatus() != null
                ? dto.getAttendanceStatus()
                : AttendanceStatus.NORMAL;

        // 7) 엔티티 생성
        Attendance attendance = Attendance.builder()
                .staffProfile(staff)
                .store(store)             // ⭐ store 추가
                .workDate(dto.getAttendanceWorkDate())
                .checkIn(checkIn)
                .checkOut(checkOut)
                .status(status)
                .workHours(workHours)
                .memo(dto.getAttendanceMemo())
                .build();

        // 8) 저장
        attendanceRepository.save(attendance);

        return attendance.getId();
    }

    /**
     * 근태 상세 조회.
     *
     * <p>
     * 현재 로그인한 점주의 storeId 기준으로,
     * 해당 근태(attendanceId)가 본인 매장의 기록인지 검증한 뒤 상세 정보를 반환한다.
     * </p>
     *
     * @param attendanceId 조회할 근태 ID
     * @return 근태 상세 DTO
     */
    @Transactional(readOnly = true)
    public AttendanceDetailDTO getAttendanceDetail(Long attendanceId) {
        Long storeId = getCurrentStoreId();

        if (storeId == null) {
            throw new IllegalStateException("가맹점 정보가 없어 근태 상세 조회를 할 수 없습니다.");
        }

        return attendanceRepository
                .findAttendanceDetailByIdAndStore(attendanceId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 근태 기록을 찾을 수 없습니다."));
    }

    /* ================== 근태 수정 폼 조회 ================== */

    /**
     * 근태 수정 화면에서 사용할 기존 데이터 조회.
     *
     * <p>
     * - 현재 로그인한 가맹점주의 storeId 기준으로
     *   본인 매장에 속한 근태만 조회한다.<br>
     * - 조회된 엔티티를 {@link AttendanceModifyFormDTO}로 변환하여 반환한다.
     * </p>
     *
     * @param attendanceId 수정 대상 근태 ID
     * @return 수정 폼용 근태 DTO
     */
    @Transactional(readOnly = true)
    public AttendanceModifyFormDTO getAttendanceModifyForm(Long attendanceId) {

        Long storeId = getCurrentStoreId();
        if (storeId == null) {
            throw new IllegalStateException("가맹점 정보가 없어 근태 수정 폼을 조회할 수 없습니다.");
        }

        com.boot.ict05_final_user.domain.staff.entity.Attendance attendance =
                attendanceRepository.findById(attendanceId)
                        .orElseThrow(() -> new IllegalArgumentException("근태 정보를 찾을 수 없습니다. id=" + attendanceId));

        // 내 매장 데이터인지 검증
        if (attendance.getStore() == null
                || attendance.getStore().getId() == null
                || !attendance.getStore().getId().equals(storeId)) {
            throw new IllegalArgumentException("현재 로그인한 매장의 근태 정보가 아닙니다.");
        }

        // === Entity -> DTO 매핑 ===
        AttendanceModifyFormDTO dto = new AttendanceModifyFormDTO();
        dto.setAttendanceId(attendance.getId());
        dto.setAttendanceWorkDate(attendance.getWorkDate());
        dto.setAttendanceCheckIn(attendance.getCheckIn());
        dto.setAttendanceCheckOut(attendance.getCheckOut());
        dto.setAttendanceStatus(attendance.getStatus());
        dto.setAttendanceWorkHours(attendance.getWorkHours());
        dto.setAttendanceMemo(attendance.getMemo());

        StaffProfile staff = attendance.getStaffProfile();
        if (staff != null) {
            dto.setStaffId(staff.getId());
            dto.setStaffName(staff.getStaffName());
            dto.setStaffEmploymentType(staff.getStaffEmploymentType());
        }

        return dto;
    }

    /* ================== 근태 수정 저장 ================== */

    /**
     * 근태 수정.
     *
     * <p>수정 내용:</p>
     * <ul>
     *     <li>직원(staff) 변경(같은 매장 소속일 때만 허용)</li>
     *     <li>근무 일자, 출퇴근 시간, 근태 상태, 메모</li>
     *     <li>근무 시간(프론트에서 전달되면 사용, 아니면 서버에서 재계산)</li>
     * </ul>
     *
     * @param dto 수정할 근태 데이터 DTO
     */
    public void modifyAttendance(AttendanceModifyFormDTO dto) {

        Long storeId = getCurrentStoreId();
        if (storeId == null) {
            throw new IllegalStateException("가맹점 정보가 없어 근태 수정을 할 수 없습니다.");
        }

        com.boot.ict05_final_user.domain.staff.entity.Attendance attendance =
                attendanceRepository.findById(dto.getAttendanceId())
                        .orElseThrow(() -> new IllegalArgumentException("근태 정보를 찾을 수 없습니다. id=" + dto.getAttendanceId()));

        // 내 매장 데이터인지 검증
        if (attendance.getStore() == null
                || attendance.getStore().getId() == null
                || !attendance.getStore().getId().equals(storeId)) {
            throw new IllegalArgumentException("현재 로그인한 매장의 근태 정보가 아닙니다.");
        }

        // ===== 직원 변경 허용 (옵션) =====
        if (dto.getStaffId() != null
                && (attendance.getStaffProfile() == null
                || !dto.getStaffId().equals(attendance.getStaffProfile().getId()))) {

            StaffProfile newStaff = staffRepository.findById(dto.getStaffId())
                    .orElseThrow(() -> new IllegalArgumentException("직원 정보를 찾을 수 없습니다. id=" + dto.getStaffId()));

            // 새 직원도 같은 매장인지 검증
            if (newStaff.getStore() == null
                    || newStaff.getStore().getId() == null
                    || !newStaff.getStore().getId().equals(storeId)) {
                throw new IllegalArgumentException("해당 매장의 직원이 아니라 근태를 변경할 수 없습니다.");
            }

            attendance.setStaffProfile(newStaff);
        }

        // ===== 출퇴근 시간 / 근무 시간 / 상태 / 메모 수정 =====

        LocalDateTime checkIn = dto.getAttendanceCheckIn();
        LocalDateTime checkOut = dto.getAttendanceCheckOut();

        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("출근/퇴근 시간은 필수입니다.");
        }
        if (checkIn.isAfter(checkOut)) {
            throw new IllegalArgumentException("출근 시간이 퇴근 시간보다 늦을 수 없습니다.");
        }

        attendance.setWorkDate(dto.getAttendanceWorkDate());
        attendance.setCheckIn(checkIn);
        attendance.setCheckOut(checkOut);
        attendance.setStatus(dto.getAttendanceStatus() != null
                ? dto.getAttendanceStatus()
                : AttendanceStatus.NORMAL);
        attendance.setMemo(dto.getAttendanceMemo());

        // 근무 시간: 프론트에서 직접 보낸 값이 있으면 우선 사용, 아니면 다시 계산
        if (dto.getAttendanceWorkHours() != null) {
            attendance.setWorkHours(dto.getAttendanceWorkHours());
        } else {
            attendance.setWorkHours(calculateWorkHours(checkIn, checkOut));
        }

        // 클래스 전체가 @Transactional 이라 별도 save() 없이 dirty checking으로 업데이트됨
        log.info("📌 근태 수정 완료: attendanceId={}, storeId={}", attendance.getId(), storeId);
    }

    /**
     * 특정 직원의 특정 날짜 근태 전체 삭제.
     *
     * <p>
     * - 현재 로그인한 가맹점의 storeId를 기준으로 권한 검증 후<br>
     *   해당 storeId + staffId + workDate 조합의 근태를 일괄 삭제한다.
     * </p>
     *
     * @param staffId 삭제 대상 직원 ID
     * @param workDate 삭제할 날짜
     */
    public void deleteDailyAttendanceForStaff(Long staffId, LocalDate workDate) {
        Long storeId = getCurrentStoreId();
        if (storeId == null) {
            throw new IllegalStateException("가맹점 정보가 없어 근태 삭제를 할 수 없습니다.");
        }

        long deleted = attendanceRepository.deleteByStoreAndStaffAndWorkDate(storeId, staffId, workDate);
        if (deleted == 0) {
            throw new IllegalArgumentException("해당 날짜의 근태를 찾을 수 없거나 삭제 권한이 없습니다.");
        }

        log.info("✅ 직원 하루 근태 일괄 삭제 완료: storeId={}, staffId={}, date={}, deletedRows={}",
                storeId, staffId, workDate, deleted);
    }

    /* ================== 공통 유틸 ================== */

    /**
     * 현재 로그인한 사용자(SecurityContext)의 storeId를 추출한다.
     *
     * <p>지원 principal 타입:</p>
     * <ul>
     *     <li>{@link AppUser}</li>
     *     <li>{@link CustomUserDetails}</li>
     * </ul>
     *
     * @return storeId, 없으면 null
     */
    private Long getCurrentStoreId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            log.warn("인증 정보 없음 → storeId 조회 불가");
            return null;
        }

        Object principal = auth.getPrincipal();
        log.debug("근태 principal 타입: {}", principal.getClass());

        if (principal instanceof AppUser appUser) {
            return appUser.getStoreId();
        }
        if (principal instanceof CustomUserDetails user) {
            return user.getStoreId();
        }
        if (principal instanceof String s && "anonymousUser".equals(s)) {
            log.warn("anonymousUser → storeId 없음");
            return null;
        }

        log.warn("예상치 못한 principal 타입: {}", principal.getClass());
        return null;
    }

    /**
     * 출근/퇴근 시간으로 실제 근무시간(시간 단위, 소수 둘째 자리)을 계산한다.
     *
     * @param checkIn  출근 시각
     * @param checkOut 퇴근 시각
     * @return 근무 시간(시간 단위, 소수점 둘째 자리 반올림)
     */
    private BigDecimal calculateWorkHours(LocalDateTime checkIn, LocalDateTime checkOut) {
        long minutes = Duration.between(checkIn, checkOut).toMinutes();

        if (minutes < 0) {
            minutes = 0;
        }

        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

}
