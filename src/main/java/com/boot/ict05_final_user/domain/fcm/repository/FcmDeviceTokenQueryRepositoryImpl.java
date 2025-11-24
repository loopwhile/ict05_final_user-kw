package com.boot.ict05_final_user.domain.fcm.repository;

import com.boot.ict05_final_user.domain.fcm.entity.AppType;
import com.boot.ict05_final_user.domain.fcm.entity.FcmDeviceToken;
import com.boot.ict05_final_user.domain.fcm.entity.PlatformType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAUpdateClause;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.boot.ict05_final_user.domain.fcm.entity.QFcmDeviceToken.fcmDeviceToken;

/**
 * {@link FcmDeviceTokenQueryRepository} 구현체.
 *
 * <p>FCM 기기 토큰의 Upsert 및 배치 비활성화 로직을 QueryDSL 기반으로 구현합니다.</p>
 *
 * <ul>
 *   <li>Upsert: (appType, platform, memberId, deviceId) 조합 또는 token 기준으로 신규/갱신 처리</li>
 *   <li>조회: 활성 토큰 단건 조회</li>
 *   <li>정리: updatedAt / lastSeenAt 기준 대량 비활성화</li>
 * </ul>
 *
 * <p>읽기 전용 조회 시 성능 최적화를 위해 Hibernate Hint를 사용합니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Repository
@RequiredArgsConstructor
public class FcmDeviceTokenQueryRepositoryImpl implements FcmDeviceTokenQueryRepository {

    private final JPAQueryFactory query;
    private final EntityManager em;

    /**
     * 토큰 또는 (appType, platform, memberId, deviceId) 기준으로 Upsert 수행.
     *
     * <p>1) 토큰 존재 시: 기존 행 재활성화 및 정보 갱신<br>
     * 2) 토큰 미존재 시: 동일 디바이스 존재 여부 확인 후 삽입 또는 갱신</p>
     *
     * @param appType   앱 구분 (HQ / STORE)
     * @param platform  플랫폼 (WEB / ANDROID / IOS)
     * @param storeIdFk 매장 ID
     * @param memberIdFk 회원 ID
     * @param deviceId  디바이스 식별자
     * @param token     FCM 등록 토큰
     * @param seenAt    최근 접속 시각
     * @return 저장 또는 갱신된 {@link FcmDeviceToken}
     */
    @Override
    public FcmDeviceToken upsert(AppType appType, PlatformType platform,
                                 Long storeIdFk, Long memberIdFk,
                                 String deviceId, String token,
                                 LocalDateTime seenAt) {

        FcmDeviceToken found = query
                .selectFrom(fcmDeviceToken)
                .where(fcmDeviceToken.token.eq(token))
                .setHint("org.hibernate.readOnly", true)
                .setHint("org.hibernate.flushMode", "COMMIT")
                .setHint("jakarta.persistence.query.timeout", 3000)
                .fetchFirst();

        if (found == null) {
            FcmDeviceToken byDevice = query
                    .selectFrom(fcmDeviceToken)
                    .where(allOf(
                            fcmDeviceToken.appType.eq(appType),
                            fcmDeviceToken.platform.eq(platform),
                            eqOrNull(fcmDeviceToken.memberIdFk, memberIdFk),
                            eqOrNull(fcmDeviceToken.deviceId, deviceId)
                    ))
                    .setHint("org.hibernate.readOnly", true)
                    .setHint("org.hibernate.flushMode", "COMMIT")
                    .setHint("jakarta.persistence.query.timeout", 3000)
                    .fetchFirst();

            if (byDevice == null) {
                FcmDeviceToken row = FcmDeviceToken.builder()
                        .appType(appType)
                        .platform(platform)
                        .token(token)
                        .deviceId(deviceId)
                        .memberIdFk(memberIdFk)
                        .storeIdFk(storeIdFk)
                        .isActive(true)
                        .lastSeenAt(seenAt)
                        .build();
                em.persist(row);
                return row;
            } else {
                byDevice.setToken(token);
                byDevice.setStoreIdFk(storeIdFk);
                byDevice.setMemberIdFk(memberIdFk);
                byDevice.setIsActive(true);
                byDevice.setLastSeenAt(seenAt);
                return byDevice;
            }
        } else {
            found.setAppType(appType);
            found.setPlatform(platform);
            found.setStoreIdFk(storeIdFk);
            found.setMemberIdFk(memberIdFk);
            found.setDeviceId(deviceId);
            found.setIsActive(true);
            found.setLastSeenAt(seenAt);
            return found;
        }
    }

    /**
     * 활성 상태의 토큰 단건 조회.
     *
     * @param token FCM 토큰 문자열
     * @return 활성 토큰 Optional
     */
    @Override
    public Optional<FcmDeviceToken> findActiveByToken(String token) {
        FcmDeviceToken row = query.selectFrom(fcmDeviceToken)
                .where(fcmDeviceToken.token.eq(token), fcmDeviceToken.isActive.isTrue())
                .setHint("org.hibernate.readOnly", true)
                .setHint("org.hibernate.flushMode", "COMMIT")
                .setHint("jakarta.persistence.query.timeout", 3000)
                .fetchFirst();
        return Optional.ofNullable(row);
    }

    /**
     * updatedAt 기준으로 오래된 토큰 일괄 비활성화.
     *
     * @param cutoff 기준 시각
     * @return 비활성화된 행 수
     */
    @Override
    public int deactivateAllByUpdatedAtBefore(LocalDateTime cutoff) {
        long affected = new JPAUpdateClause(em, fcmDeviceToken)
                .where(fcmDeviceToken.isActive.isTrue()
                        .and(fcmDeviceToken.updatedAt.before(cutoff)))
                .set(fcmDeviceToken.isActive, false)
                .execute();
        return (int) affected;
    }

    /**
     * lastSeenAt 기준으로 오래된 토큰 일괄 비활성화.
     *
     * @param cutoff 기준 시각
     * @return 비활성화된 행 수
     */
    @Override
    public int deactivateAllByLastSeenAtBefore(LocalDateTime cutoff) {
        long affected = new JPAUpdateClause(em, fcmDeviceToken)
                .where(fcmDeviceToken.isActive.isTrue()
                        .and(fcmDeviceToken.lastSeenAt.isNotNull())
                        .and(fcmDeviceToken.lastSeenAt.before(cutoff)))
                .set(fcmDeviceToken.isActive, false)
                .execute();
        return (int) affected;
    }

    /** 다중 BooleanExpression 조합 유틸리티 */
    private static BooleanExpression allOf(BooleanExpression... exps) {
        BooleanExpression acc = null;
        for (BooleanExpression e : exps) {
            if (e == null) continue;
            acc = (acc == null) ? e : acc.and(e);
        }
        return acc;
    }

    /** 값이 null이 아닐 경우 eq 조건 생성 */
    private static <T> BooleanExpression eqOrNull(com.querydsl.core.types.dsl.SimpleExpression<T> col, T v) {
        return (v == null) ? null : col.eq(v);
    }
}
