package com.boot.ict05_final_user.domain.user.repository;

import com.boot.ict05_final_user.domain.staff.entity.QStaffProfile;
import com.boot.ict05_final_user.domain.user.entity.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Long findStoreIdByUsername(String username) {
        QStaffProfile staffProfile = QStaffProfile.staffProfile;

        Long storeId = queryFactory
                .select(staffProfile.store.id)
                .from(staffProfile)
                .where(
                        staffProfile.staffEmail.eq(username)
                )
                .fetchOne();

        return storeId;
    }

    @Override
    public Long findMamberId(String username) {
        QMember member = QMember.member;
        Long memberId = queryFactory
                .select(member.id)
                .from(member)
                .where(
                        member.email.eq(username)
                )
                .fetchOne();
        return memberId;
    }

    @Override
    public String findMemberName(String username) {
        QMember member = QMember.member;
        String memberName = queryFactory
                .select(member.name)
                .from(member)
                .where(
                        member.email.eq(username)
                )
                .fetchOne();
        return memberName;
    }
}
