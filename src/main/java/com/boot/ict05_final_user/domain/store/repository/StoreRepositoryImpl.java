package com.boot.ict05_final_user.domain.store.repository;

import com.boot.ict05_final_user.domain.staff.entity.QStaffProfile;
import com.boot.ict05_final_user.domain.staff.entity.StaffDepartment;
import com.boot.ict05_final_user.domain.staff.entity.StaffEmploymentType;
import com.boot.ict05_final_user.domain.store.dto.*;
import com.boot.ict05_final_user.domain.store.entity.QStore;
import com.boot.ict05_final_user.domain.store.entity.StoreStatus;
import com.boot.ict05_final_user.domain.user.entity.QMember;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StoreRepositoryImpl implements StoreRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<StoreListDTO> listStore(StoreSearchDTO storeSearchDTO, Pageable pageable) {
        QStore store = QStore.store;
        QStaffProfile staffProfile = QStaffProfile.staffProfile;

        List<StoreListDTO> content = queryFactory
                .select(Projections.fields(StoreListDTO.class,
                        store.id.as("storeId"),
                        store.name.as("storeName"),
                        store.status.as("storeStatus"),
                        ExpressionUtils.as(
                                JPAExpressions.select(staffProfile.staffName)
                                        .from(staffProfile)
                                        .where(
                                                staffProfile.store.id.eq(store.id),
                                                staffProfile.staffEmploymentType.eq(StaffEmploymentType.OWNER)
                                        )
                                        .limit(1),
                                "staffName"),
                        store.phone.as("storePhone"),
                        store.monthlySales.as("storeMonthlySales"),
                        store.totalEmployees.as("storeTotalEmployees")
                ))
                .from(store)
                .where(
                        eqSearchStore(storeSearchDTO, store),
                        eqStatus(storeSearchDTO, store)
                )
                .orderBy(store.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(store.count())
                .from(store)
                .where(
                        eqSearchStore(storeSearchDTO, store),
                        eqStatus(storeSearchDTO, store) // 목록과 동일 WHERE
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    /** 키워드: id or name 부분일치(대소문자 무시), 공백/널 방지 */
    private BooleanExpression eqSearchStore(StoreSearchDTO dto, QStore store) {
        String keyword = dto.getKeyword();
        if (keyword == null || keyword.isBlank()) return null;
        keyword = keyword.trim();
        return store.id.stringValue().containsIgnoreCase(keyword)
                .or(store.name.containsIgnoreCase(keyword));
    }

    /** 상태 필터 */
    private BooleanExpression eqStatus(StoreSearchDTO dto, QStore store) {
        return dto.getStatus() != null ? store.status.eq(dto.getStatus()) : null;
    }

    @Override
    public long countStore(StoreSearchDTO storeSearchDTO) {
        QStore store = QStore.store;
        Long total = queryFactory
                .select(store.count())
                .from(store)
                .where(
                        eqSearchStore(storeSearchDTO, store),
                        eqStatus(storeSearchDTO, store) // ✅ 동일 WHERE 적용
                )
                .fetchOne();
        return total == null ? 0L : total;
    }

    @Override
    public List<FindStoreDTO> findStoreName() {
        QStore store = QStore.store;
        return queryFactory
                .select(Projections.fields(FindStoreDTO.class,
                        store.id.as("storeId"),
                        store.name.as("storeName")
                ))
                .from(store)
                .orderBy(store.id.desc())
                .fetch();
    }

    @Override
    public StoreDetailDTO findByStoreDetail(Long id) {
        QStore store = QStore.store;
        QStaffProfile staffProfile = QStaffProfile.staffProfile;
        QMember member = QMember.member;

        return queryFactory
                .select(Projections.fields(StoreDetailDTO.class,
                        store.id.as("storeId"),
                        store.name.as("storeName"),
                        store.status.as("storeStatus"),
                        store.businessRegistrationNumber.as("businessRegistrationNumber"),
                        store.phone.as("storePhone"),
                        store.location.as("storeLocation"),
                        store.type.as("storeType"),
                        store.contractStartDate.as("storeContractStartDate"),
                        store.contractAffiliateDate.as("storeContractAffiliateDate"),
                        store.contractTerm.as("storeContractTerm"),
                        store.affiliatePrice.as("storeAffiliatePrice"),
                        store.monthlySales.as("storeMonthlySales"),
                        store.royalty.as("royalty"),
                        store.comment.as("comment"),

                        member.name.as("memberName"),
                        member.email.as("memberEmail"),

                        ExpressionUtils.as(
                                JPAExpressions.select(staffProfile.staffName)
                                        .from(staffProfile)
                                        .where(
                                                staffProfile.store.id.eq(store.id),
                                                staffProfile.staffEmploymentType.eq(StaffEmploymentType.OWNER)
                                        )
                                        .orderBy(staffProfile.id.desc()) // 최근 1명
                                        .limit(1),
                                "staffName"
                        ),
                        store.totalEmployees.as("storeTotalEmployees")
                ))
                .from(store)
                .leftJoin(store.member, member)
                .where(store.id.eq(id))
                .fetchOne();
    }
    @Override
    public List<StaffNameDTO> ownerStaffOptions() {
        QStaffProfile staffProfile = QStaffProfile.staffProfile;

        return queryFactory
                .select(Projections.fields(StaffNameDTO.class,
                        staffProfile.id.as("staffId"),
                        staffProfile.staffName.as("staffName")
                ))
                .from(staffProfile)
                .where(staffProfile.staffEmploymentType.eq(StaffEmploymentType.OWNER))
                .orderBy(staffProfile.id.desc())
                .fetch();
    }

    @Override
    public List<StaffNameDTO> hqWorkerStaffOptions() {
        QStaffProfile staffProfile = QStaffProfile.staffProfile;

        return queryFactory
                .select(Projections.fields(StaffNameDTO.class,
                        staffProfile.id.as("staffId"),
                        staffProfile.staffName.as("staffName")
                ))
                .from(staffProfile)
                .where(staffProfile.staffEmploymentType.eq(StaffEmploymentType.WORKER)
                        , staffProfile.staffDepartment.eq(StaffDepartment.OFFICE))
                .orderBy(staffProfile.id.desc())
                .fetch();
    }

    // =========================
    // 요약 카드용 집계 4종
    // =========================

    @Override
    public long countStoreAll() {
        QStore store = QStore.store;
        Long v = queryFactory
                .select(store.count())
                .from(store)
                .fetchOne();
        return v == null ? 0L : v;
    }

    @Override
    public long countActiveStore() {
        QStore store = QStore.store;
        Long v = queryFactory
                .select(store.count())
                .from(store)
                .where(store.status.eq(StoreStatus.OPERATING))
                .fetchOne();
        return v == null ? 0L : v;
    }

    @Override
    public BigDecimal avgMonthlySales() {
        QStore store = QStore.store;
        BigDecimal v = queryFactory
                .select(Expressions.numberTemplate(
                        BigDecimal.class,
                        "coalesce(avg({0}), 0)", store.monthlySales))
                .from(store)
                .fetchOne();
        if (v == null) return BigDecimal.ZERO;
        // 소수 없이 반올림 (원하면 scale 조절)
        return v.setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public long totalEmployees() {
        QStaffProfile sp = QStaffProfile.staffProfile;

        Long v = queryFactory
                .select(sp.id.count())
                .from(sp)
                .where(sp.staffEndDate.isNull())   // 재직자만
                .fetchOne();

        return v == null ? 0L : v;
    }

    @Override
    public FindMemberEmailtoIdDTO findMemberByEmail(String email) {
        QMember m = QMember.member;
        return queryFactory
                .select(Projections.fields(FindMemberEmailtoIdDTO.class,
                        m.email.as("email"),
                        m.id.as("id")
                ))
                .from(m)
                .where(m.email.eq(email))
                .fetchOne();
    }
}
