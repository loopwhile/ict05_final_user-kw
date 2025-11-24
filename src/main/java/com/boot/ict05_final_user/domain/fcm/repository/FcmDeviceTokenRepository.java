package com.boot.ict05_final_user.domain.fcm.repository;

import com.boot.ict05_final_user.domain.fcm.entity.AppType;
import com.boot.ict05_final_user.domain.fcm.entity.FcmDeviceToken;
import com.boot.ict05_final_user.domain.fcm.entity.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * FCM 기기 토큰 기본 CRUD 및 커스텀 쿼리 리포지토리.
 *
 * <p>{@link JpaRepository} 기반 CRUD 메서드 외에,
 * {@link FcmDeviceTokenQueryRepository}를 확장하여 Upsert 및
 * 배치 비활성화 로직을 제공합니다.</p>
 *
 * <ul>
 *   <li>토큰별 조회 및 멤버별 활성 토큰 목록 조회</li>
 *   <li>앱/플랫폼/디바이스별 필터링</li>
 *   <li>cleanup 스케줄러 연동 (updatedAt, lastSeenAt 기반)</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public interface FcmDeviceTokenRepository
        extends JpaRepository<FcmDeviceToken, Long>, FcmDeviceTokenQueryRepository {

    /** 단일 토큰으로 토큰 엔티티 조회 */
    Optional<FcmDeviceToken> findByToken(String token);

    /** 특정 회원의 활성 토큰 전체 조회 */
    List<FcmDeviceToken> findByMemberIdFkAndIsActiveTrue(Long memberIdFk);

    /** 앱/플랫폼/회원/디바이스 조합으로 활성 토큰 조회 */
    List<FcmDeviceToken> findByAppTypeAndPlatformAndMemberIdFkAndDeviceIdAndIsActiveTrue(
            AppType appType, PlatformType platform, Long memberIdFk, String deviceId
    );

    /**
     * 멤버의 활성 토큰 문자열만 반환 (토큰 문자열만 필요한 경우)
     *
     * @param memberIdFk 회원 ID
     * @return 활성 토큰 문자열 목록
     */
    default List<String> findActiveTokensOfMember(Long memberIdFk) {
        return findByMemberIdFkAndIsActiveTrue(memberIdFk).stream()
                .map(FcmDeviceToken::getToken)
                .toList();
    }

    /** 앱 유형별 활성 토큰 조회 */
    List<FcmDeviceToken> findByAppTypeAndIsActiveTrue(AppType appType);

    /** 앱 + 멤버 기준 활성 토큰 조회 */
    List<FcmDeviceToken> findByAppTypeAndMemberIdFkAndIsActiveTrue(AppType appType, Long memberIdFk);

    /** updatedAt 기준 cleanup 후보 조회 */
    List<FcmDeviceToken> findByIsActiveTrueAndUpdatedAtBefore(LocalDateTime cutoff);

    /** lastSeenAt 기준 cleanup 후보 조회 */
    List<FcmDeviceToken> findByIsActiveTrueAndLastSeenAtBefore(LocalDateTime cutoff);
}
