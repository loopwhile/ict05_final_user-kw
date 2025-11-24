package com.boot.ict05_final_user.domain.staff.repository;

import com.boot.ict05_final_user.domain.staff.dto.StaffListDTO;
import com.boot.ict05_final_user.domain.staff.dto.StaffSearchDTO;
import com.boot.ict05_final_user.domain.staff.entity.QAttendance;
import com.boot.ict05_final_user.domain.staff.entity.QStaffProfile;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 직원 커스텀 리포지토리 QueryDSL 구현 클래스.
 *
 * <p>기능 요약</p>
 * <ul>
 *     <li>가맹점 직원 목록 조회</li>
 *     <li>직원 기본 정보 + 최신 근태 상태 조회</li>
 *     <li>직원명/ID/부서 검색 가능</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class StaffRepositoryImpl implements StaffRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 직원 목록 조회 + 최신 근태 JOIN 포함.
     *
     * <p>특징</p>
     * <ul>
     *     <li>가장 최근 attendance.id 최대값만 JOIN (최신 근태 상태)</li>
     *     <li>동적 검색 조건 적용</li>
     *     <li>DTO 프로젝션으로 성능 최적화</li>
     * </ul>
     */
    @Override
    public Page<StaffListDTO> listStaff(StaffSearchDTO dto, Pageable pageable) {

        QStaffProfile staff = QStaffProfile.staffProfile;
        QAttendance attendance = QAttendance.attendance;
        QAttendance a2 = new QAttendance("a2"); // 서브쿼리 alias

        BooleanBuilder condition = new BooleanBuilder();
        condition.and(eqStore(dto, staff));
        condition.and(eqTitleOrBody(dto, staff));

        // =============================
        //   1) 직원 목록 + 최신 근태 상태
        // =============================
        JPAQuery<StaffListDTO> contentQuery = queryFactory
                .select(Projections.fields(
                        StaffListDTO.class,
                        staff.id,
                        staff.staffName,
                        staff.staffBirth,
                        staff.staffPhone,
                        staff.staffEmail,
                        staff.staffEmploymentType,
                        staff.staffStartDate,
                        staff.staffEndDate,
                        attendance.status.as("attendanceStatus")
                ))
                .from(staff)
                .leftJoin(attendance).on(
                        attendance.staffProfile.eq(staff)
                                .and(attendance.id.eq(
                                        com.querydsl.jpa.JPAExpressions
                                                .select(a2.id.max())
                                                .from(a2)
                                                .where(a2.staffProfile.eq(staff))
                                ))
                )
                .where(condition)
                .orderBy(staff.id.desc())
                .distinct();

        // 페이징 처리
        if (pageable.isPaged()) {
            contentQuery
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize());
        }

        List<StaffListDTO> content = contentQuery.fetch();

        // =============================
        //   2) 총 개수 조회
        // =============================
        Long total = queryFactory
                .select(staff.count())
                .from(staff)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /** 가맹점 필터 */
    private BooleanExpression eqStore(StaffSearchDTO dto, QStaffProfile staff) {
        if (dto.getStoreId() == null) return null;
        return staff.store.id.eq(dto.getStoreId());
    }

    /** 직원명/ID/부서 검색 */
    private BooleanExpression eqTitleOrBody(StaffSearchDTO dto, QStaffProfile staff) {
        if (dto.getKeyword() == null || dto.getKeyword().isBlank()) return null;

        String keyword = dto.getKeyword();

        return staff.id.stringValue().containsIgnoreCase(keyword)
                .or(staff.staffName.containsIgnoreCase(keyword))
                .or(staff.staffDepartment.stringValue().containsIgnoreCase(keyword));
    }
}
