package com.boot.ict05_final_user.domain.fcm.repository;

import com.boot.ict05_final_user.domain.fcm.entity.AppType;
import com.boot.ict05_final_user.domain.fcm.entity.FcmPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * FCM 알림 선호도(Preference) 리포지토리.
 *
 * <p>회원별 FCM 카테고리 수신 여부를 관리합니다.
 * HQ 및 STORE 구분(AppType)에 따라 선호도를 조회/저장합니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public interface FcmPreferenceRepository extends JpaRepository<FcmPreference, Long> {

	/**
	 * 특정 앱 유형(AppType)과 회원 ID로 선호도 1건 조회.
	 *
	 * @param appType   앱 구분 (HQ / STORE)
	 * @param memberIdFk 회원 ID
	 * @return FCM 선호도 엔티티(Optional)
	 */
	Optional<FcmPreference> findFirstByAppTypeAndMemberIdFk(AppType appType, Long memberIdFk);
}
