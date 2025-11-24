package com.boot.ict05_final_user.domain.fcm.repository;

import com.boot.ict05_final_user.domain.fcm.entity.AppType;
import com.boot.ict05_final_user.domain.fcm.entity.FcmDeviceToken;
import com.boot.ict05_final_user.domain.fcm.entity.PlatformType;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * FCM 기기 토큰 관련 사용자 정의 쿼리 리포지토리.
 *
 * <p>Spring Data JPA의 QueryDSL 또는 Native SQL을 통해
 * 고급 업서트 및 배치 비활성화 쿼리를 수행합니다.</p>
 *
 * <ul>
 *   <li>토큰 중복 방지를 위한 Upsert 구현</li>
 *   <li>비활성화(cleanup) 배치 처리</li>
 *   <li>토큰 기준 조회</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public interface FcmDeviceTokenQueryRepository {

    /**
     * (appType, platform, memberId, deviceId) 조합 또는 token을 기준으로 Upsert 수행.
     *
     * @param appType   앱 유형 (HQ / STORE)
     * @param platform  플랫폼 유형 (WEB / ANDROID / IOS)
     * @param storeId   매장 ID (선택)
     * @param memberId  회원 ID
     * @param deviceId  디바이스 식별자
     * @param token     FCM 토큰
     * @param seenAt    최근 접속 시각
     * @return Upsert 완료된 {@link FcmDeviceToken} 엔티티
     */
    FcmDeviceToken upsert(AppType appType, PlatformType platform,
                          Long storeId, Long memberId,
                          String deviceId, String token,
                          LocalDateTime seenAt);

    /**
     * 활성 상태의 특정 토큰 조회.
     *
     * @param token FCM 토큰 문자열
     * @return 활성 토큰 엔티티
     */
    Optional<FcmDeviceToken> findActiveByToken(String token);

    /**
     * updatedAt 기준으로 오래된 토큰 일괄 비활성화.
     *
     * @param cutoff 기준 시각
     * @return 비활성화된 행 수
     */
    int deactivateAllByUpdatedAtBefore(LocalDateTime cutoff);

    /**
     * lastSeenAt 기준으로 오래된 토큰 일괄 비활성화.
     *
     * @param cutoff 기준 시각
     * @return 비활성화된 행 수
     */
    int deactivateAllByLastSeenAtBefore(LocalDateTime cutoff);
}
