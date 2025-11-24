package com.boot.ict05_final_user.domain.attendance.repository;

import com.boot.ict05_final_user.domain.attendance.dto.AttendanceDetailDTO;
import com.boot.ict05_final_user.domain.attendance.dto.AttendanceListDTO;
import com.boot.ict05_final_user.domain.attendance.dto.AttendanceSearchDTO;
import com.boot.ict05_final_user.domain.staff.entity.AttendanceStatus;
import com.boot.ict05_final_user.domain.staff.entity.QAttendance;
import com.boot.ict05_final_user.domain.staff.entity.QStaffProfile;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * AttendanceRepositoryCustom 구현체.
 *
 * <p>
 * QueryDSL을 활용한 복잡한 조회 및 삭제 로직을 담당한다.
 * </p>
 *
 * <h3>주요 특징</h3>
 * <ul>
 *     <li>직원 프로필 JOIN 기반 조회 수행</li>
 *     <li>조건을 BooleanBuilder로 동적으로 구성</li>
 *     <li>Projections를 사용해 DTO로 직접 매핑(엔티티 노출 방지)</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class AttendanceRepositoryImpl implements AttendanceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 날짜의 가맹점 직원 근태 목록 조회(QueryDSL).
     *
     * <p>검색조건 포함 (직원명, 직원ID, 근태상태)</p>
     *
     * @param storeId 가맹점 ID
     * @param workDate 조회 날짜
     * @param pageable 페이징 정보
     * @param dto 검색 조건 DTO
     * @return 페이징된 근태 리스트
     */
    @Override
    public Page<AttendanceListDTO> findDailyAttendanceByStore(
            Long storeId,
            LocalDate workDate,
            Pageable pageable,
            AttendanceSearchDTO dto
    ) {
        QAttendance attendance = QAttendance.attendance;
        QStaffProfile staff = QStaffProfile.staffProfile;

        BooleanBuilder condition = new BooleanBuilder();

        // 기본 조건 (날짜 + 점포)
        condition.and(attendance.workDate.eq(workDate));
        if (storeId != null) {
            condition.and(staff.store.id.eq(storeId));
        }

        // 검색 조건
        if (dto != null) {
            String keyword = dto.getKeyword();
            String type = dto.getType();
            AttendanceStatus statusFilter = dto.getAttendanceStatus();

            // 검색어 조건
            if (keyword != null && !keyword.isBlank()) {
                if ("name".equalsIgnoreCase(type)) {
                    condition.and(staff.staffName.containsIgnoreCase(keyword));
                } else if ("id".equalsIgnoreCase(type)) {
                    condition.and(staff.id.stringValue().containsIgnoreCase(keyword));
                } else { // 전체 검색
                    condition.and(
                            staff.staffName.containsIgnoreCase(keyword)
                                    .or(staff.id.stringValue().containsIgnoreCase(keyword))
                    );
                }
            }

            // 근태 상태 필터
            if (statusFilter != null) {
                condition.and(attendance.status.eq(statusFilter));
            }
        }

        // ==================
        // 1) 데이터 조회 쿼리
        // ==================
        JPAQuery<AttendanceListDTO> contentQuery = queryFactory
                .select(Projections.constructor(
                        AttendanceListDTO.class,
                        attendance.id,
                        attendance.workDate,
                        attendance.checkIn,
                        attendance.checkOut,
                        attendance.status,
                        attendance.workHours,
                        staff.id,
                        staff.staffName,
                        staff.staffEmploymentType
                ))
                .from(attendance)
                .join(attendance.staffProfile, staff)
                .where(condition)
                .orderBy(
                        staff.staffName.asc(),
                        attendance.checkIn.asc()
                );

        if (pageable.isPaged()) {
            contentQuery.offset(pageable.getOffset()).limit(pageable.getPageSize());
        }

        List<AttendanceListDTO> content = contentQuery.fetch();

        // ==================
        // 2) 카운트 쿼리
        // ==================
        Long total = queryFactory
                .select(attendance.count())
                .from(attendance)
                .join(attendance.staffProfile, staff)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /**
     * 근태 상세 조회(QueryDSL).
     *
     * <p>
     * store 검증 포함(점주가 본인 매장의 근태만 볼 수 있도록)
     * </p>
     */
    @Override
    public Optional<AttendanceDetailDTO> findAttendanceDetailByIdAndStore(Long attendanceId, Long storeId) {

        QAttendance attendance = QAttendance.attendance;
        QStaffProfile staff = QStaffProfile.staffProfile;

        BooleanBuilder condition = new BooleanBuilder();
        condition.and(attendance.id.eq(attendanceId));

        // 점주 본인 매장만 조회
        if (storeId != null) {
            condition.and(staff.store.id.eq(storeId));
        }

        AttendanceDetailDTO result = queryFactory
                .select(Projections.constructor(
                        AttendanceDetailDTO.class,
                        attendance.id,
                        attendance.workDate,
                        attendance.checkIn,
                        attendance.checkOut,
                        attendance.status,
                        attendance.workHours,
                        attendance.memo,
                        staff.id,
                        staff.staffName,
                        staff.staffEmploymentType
                ))
                .from(attendance)
                .join(attendance.staffProfile, staff)
                .where(condition)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 직원 + 날짜 근태 일괄 삭제.
     *
     * @return 삭제된 row 수
     */
    @Override
    public long deleteByStoreAndStaffAndWorkDate(Long storeId, Long staffId, LocalDate workDate) {
        QAttendance attendance = QAttendance.attendance;
        return queryFactory
                .delete(attendance)
                .where(
                        attendance.store.id.eq(storeId)
                                .and(attendance.staffProfile.id.eq(staffId))
                                .and(attendance.workDate.eq(workDate))
                )
                .execute();
    }

    /**
     * 단건 근태 삭제 (가맹점 검증 포함).
     *
     * @return 삭제된 row 수
     */
    @Override
    public long deleteByIdAndStore(Long attendanceId, Long storeId) {
        QAttendance attendance = QAttendance.attendance;

        return queryFactory
                .delete(attendance)
                .where(
                        attendance.id.eq(attendanceId)
                                .and(attendance.store.id.eq(storeId))
                )
                .execute();
    }
}
