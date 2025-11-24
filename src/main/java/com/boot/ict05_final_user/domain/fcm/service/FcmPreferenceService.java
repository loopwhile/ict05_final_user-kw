package com.boot.ict05_final_user.domain.fcm.service;

import com.boot.ict05_final_user.domain.fcm.entity.AppType;
import com.boot.ict05_final_user.domain.fcm.entity.FcmDeviceToken;
import com.boot.ict05_final_user.domain.fcm.entity.FcmPreference;
import com.boot.ict05_final_user.domain.fcm.repository.FcmDeviceTokenRepository;
import com.boot.ict05_final_user.domain.fcm.repository.FcmPreferenceRepository;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FCM 알림 수신 설정(Preference) 관리 서비스.
 *
 * <p>사용자의 FCM 구독 선호도를 저장하고, 해당 선호도에 따라
 * 관련 FCM 토픽 구독 상태를 자동으로 동기화합니다.</p>
 *
 * <ul>
 *     <li>가맹점 사용자의 알림 선호도 저장(업서트)</li>
 *     <li>멤버별 현재 선호도 조회</li>
 *     <li>선호도 기반 토픽 구독/해제 일괄 동기화</li>
 * </ul>
 *
 * <p>FCM 기능이 비활성화되어 있을 경우({@code fcm.enabled=false}) 서비스는 로드되지 않습니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
public class FcmPreferenceService {

	private final FcmPreferenceRepository repo;
	private final FcmDeviceTokenRepository tokenRepo;
	private final FcmService fcmService;

	/**
	 * 가맹점 사용자의 FCM 알림 선호도를 저장(업서트)합니다.
	 *
	 * <p>이미 존재하는 설정이 있으면 갱신하며, 저장 직후 {@link #syncTopicsForMember(Long, Long, FcmPreference)}를 통해
	 * 실제 FCM 토픽 구독 상태를 선호도에 맞게 동기화합니다.</p>
	 *
	 * @param memberId       회원 ID
	 * @param storeId        매장 ID
	 * @param catNotice      공지 알림 수신 여부
	 * @param catStockLow    재고 부족 알림 수신 여부
	 * @param catExpireSoon  유통기한 임박 알림 수신 여부
	 * @param thresholdDays  임박 기준일(일 단위)
	 * @return 저장된 {@link FcmPreference} 객체
	 */
	@Transactional
	public FcmPreference upsertForStoreMember(
			Long memberId,
			Long storeId,
			Boolean catNotice,
			Boolean catStockLow,
			Boolean catExpireSoon,
			Integer thresholdDays
	) {
		FcmPreference row = repo.findFirstByAppTypeAndMemberIdFk(AppType.STORE, memberId)
				.orElseGet(() -> FcmPreference.builder()
						.appType(AppType.STORE)
						.memberIdFk(memberId)
						.storeIdFk(storeId)
						.build());

		if (catNotice != null) row.setCatNotice(catNotice);
		if (catStockLow != null) row.setCatStockLow(catStockLow);
		if (catExpireSoon != null) row.setCatExpireSoon(catExpireSoon);
		if (thresholdDays != null) row.setThresholdDays(Math.max(0, thresholdDays));
		if (row.getStoreIdFk() == null && storeId != null) row.setStoreIdFk(storeId);

		FcmPreference saved = repo.save(row);

		try {
			syncTopicsForMember(memberId, saved.getStoreIdFk(), saved);
		} catch (Exception e) {
			log.warn("[FCM][PrefSync] sync failed memberId={} storeId={} err={}",
					memberId, saved.getStoreIdFk(), e.getMessage());
		}
		return saved;
	}

	/**
	 * 가맹점 사용자의 FCM 알림 선호도 정보를 조회합니다.
	 *
	 * <p>해당 사용자가 아직 설정을 저장하지 않았다면 {@code null}을 반환합니다.</p>
	 *
	 * @param memberId 회원 ID
	 * @return {@link FcmPreference} 또는 null
	 */
	@Transactional(readOnly = true)
	public FcmPreference getForStoreMember(Long memberId) {
		return repo.findFirstByAppTypeAndMemberIdFk(AppType.STORE, memberId).orElse(null);
	}

	/**
	 * 사용자의 활성화된 FCM 토큰들을 기반으로, 매장 관련 토픽을 일괄 구독 또는 해제합니다.
	 *
	 * <p>해당 사용자가 구독 설정을 변경하면 본 메서드가 호출되어
	 * 관련 FCM 토픽({@code store-}, {@code inv-low-}, {@code expire-soon-})에 대해
	 * 구독 상태를 즉시 반영합니다.</p>
	 *
	 * @param memberId 회원 ID
	 * @param storeId  매장 ID
	 * @param pref     사용자의 알림 선호도 객체
	 * @throws FirebaseMessagingException FCM 구독/해제 요청 실패 시 발생
	 */
	@Transactional(readOnly = true)
	public void syncTopicsForMember(Long memberId, Long storeId, FcmPreference pref)
			throws FirebaseMessagingException {

		if (memberId == null || storeId == null || pref == null) {
			log.debug("[FCM][PrefSync] skip: invalid args memberId={} storeId={} prefNull={}",
					memberId, storeId, (pref == null));
			return;
		}

		List<FcmDeviceToken> actives =
				tokenRepo.findByAppTypeAndMemberIdFkAndIsActiveTrue(AppType.STORE, memberId);
		if (actives.isEmpty()) {
			log.debug("[FCM][PrefSync] no active tokens for memberId={}, storeId={}", memberId, storeId);
			return;
		}

		List<String> tokens = actives.stream().map(FcmDeviceToken::getToken).toList();

		final String topicNotice = "store-" + storeId;
		final String topicInvLow = "inv-low-" + storeId;
		final String topicExpire = "expire-soon-" + storeId;

		// 매장 공지 구독 상태 반영
		if (Boolean.TRUE.equals(pref.getCatNotice())) {
			fcmService.subscribeAll(tokens, topicNotice);
		} else {
			fcmService.unsubscribeAll(tokens, topicNotice);
		}

		// 재고 부족 구독 상태 반영
		if (Boolean.TRUE.equals(pref.getCatStockLow())) {
			fcmService.subscribeAll(tokens, topicInvLow);
		} else {
			fcmService.unsubscribeAll(tokens, topicInvLow);
		}

		// 유통기한 임박 구독 상태 반영
		if (Boolean.TRUE.equals(pref.getCatExpireSoon())) {
			fcmService.subscribeAll(tokens, topicExpire);
		} else {
			fcmService.unsubscribeAll(tokens, topicExpire);
		}

		log.info("[FCM][PrefSync] memberId={} storeId={} tokens={} notice={} invLow={} expire={}",
				memberId, storeId, tokens.size(),
				pref.getCatNotice(), pref.getCatStockLow(), pref.getCatExpireSoon());
	}
}
