package com.boot.ict05_final_user.domain.fcm.controller;

import com.boot.ict05_final_user.domain.fcm.dto.FcmPreferenceUpdateRequest;
import com.boot.ict05_final_user.domain.fcm.dto.StoreTopic;
import com.boot.ict05_final_user.domain.fcm.entity.FcmPreference;
import com.boot.ict05_final_user.domain.fcm.entity.FcmDeviceToken;
import com.boot.ict05_final_user.domain.fcm.repository.FcmDeviceTokenRepository;
import com.boot.ict05_final_user.domain.fcm.service.FcmPreferenceService;
import com.boot.ict05_final_user.domain.fcm.service.FcmService;
import com.google.firebase.messaging.FirebaseMessagingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FCM 알림 설정(Preference) 관련 REST API 컨트롤러.
 *
 * <p>가맹점 사용자가 자신의 FCM 알림 구독 설정(공지, 재고부족, 유통기한 임박)을
 * 조회 및 수정할 수 있도록 지원합니다. 수정 시, 관련 토픽 구독 상태가 즉시 반영됩니다.</p>
 *
 * <ul>
 *     <li>내 알림 설정 조회 (GET /fcm/pref/me)</li>
 *     <li>내 알림 설정 수정 (PUT /fcm/pref/me)</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@RestController
@RequestMapping("/fcm/pref")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('STORE','OWNER','STAFF','USER')")
@Tag(name = "FCM Preference", description = "FCM 알림 구독 설정 API")
public class FcmPreferenceController {

	private final FcmPreferenceService prefService;
	private final FcmDeviceTokenRepository tokenRepo;
	private final FcmService fcmService;

	/**
	 * 인증 주체 또는 JWT에서 storeId와 memberId를 추출하기 위한 내부 클래스.
	 */
	private static class Ids {
		Long storeId;
		Long memberId;
	}

	/**
	 * Principal 또는 Authorization 헤더로부터 storeId와 memberId를 추출합니다.
	 *
	 * @param principal Spring Security의 Principal 객체
	 * @param authHeader Authorization 헤더
	 * @return 추출된 식별자 정보
	 */
	private Ids resolveIds(Object principal, String authHeader) {
		Ids ids = new Ids();
		try {
			if (principal != null) {
				var cls = principal.getClass();
				var mStore = cls.getMethod("getStoreId");
				var mMember = cls.getMethod("getMemberId");
				Object sid = mStore.invoke(principal);
				Object mid = mMember.invoke(principal);
				if (sid instanceof Number) ids.storeId = ((Number) sid).longValue();
				if (mid instanceof Number) ids.memberId = ((Number) mid).longValue();
			}
		} catch (Exception ignore) {}

		if ((ids.storeId == null || ids.memberId == null)
				&& authHeader != null && authHeader.startsWith("Bearer ")) {
			try {
				String token = authHeader.substring(7);
				Long sid = com.boot.ict05_final_user.config.security.jwt.JWTUtil.getStoreId(token);
				Long mid = com.boot.ict05_final_user.config.security.jwt.JWTUtil.getMemberId(token);
				if (ids.storeId == null) ids.storeId = sid;
				if (ids.memberId == null) ids.memberId = mid;
			} catch (Exception e) {
				log.warn("[FCM] JWT parse failed: {}", e.getMessage());
			}
		}
		return ids;
	}

	/**
	 * 현재 로그인 사용자의 FCM 알림 설정 정보를 조회합니다.
	 *
	 * @param me 인증된 사용자
	 * @param auth Authorization 헤더
	 * @return 사용자의 FCM 알림 설정 정보
	 */
	@Operation(
			summary = "내 FCM 알림 설정 조회",
			description = "현재 로그인한 사용자의 FCM 알림 구독 설정을 반환합니다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "정상 조회 성공"),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
	})
	@GetMapping("/me")
	public Map<String, Object> getMyPref(
			@Parameter(hidden = true) @org.springframework.security.core.annotation.AuthenticationPrincipal Object me,
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth
	) {
		Ids ids = resolveIds(me, auth);
		if (ids.memberId == null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "NO_AUTH");

		FcmPreference p = prefService.getForStoreMember(ids.memberId);
		Map<String, Object> res = new LinkedHashMap<>();
		res.put("memberId", ids.memberId);
		res.put("storeId", ids.storeId);
		res.put("catNotice", p != null ? p.getCatNotice() : true);
		res.put("catStockLow", p != null ? p.getCatStockLow() : true);
		res.put("catExpireSoon", p != null ? p.getCatExpireSoon() : true);
		res.put("thresholdDays", p != null ? p.getThresholdDays() : 3);
		return res;
	}

	/**
	 * 현재 로그인 사용자의 FCM 알림 설정을 수정합니다.
	 *
	 * <p>변경 후, 해당 사용자의 모든 활성 토큰에 대해 즉시 구독 상태가 갱신됩니다.
	 * 예를 들어, 재고 부족 알림을 해제하면 해당 토픽에서 즉시 구독이 해제됩니다.</p>
	 *
	 * @param me 인증된 사용자
	 * @param auth Authorization 헤더
	 * @param req 설정 변경 요청 DTO
	 * @return 업데이트된 설정의 ID
	 */
	@Operation(
			summary = "내 FCM 알림 설정 수정",
			description = "현재 로그인한 사용자의 FCM 알림 설정을 수정하며, 구독 상태를 즉시 반영합니다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "정상 수정 완료"),
			@ApiResponse(responseCode = "401", description = "인증 실패"),
			@ApiResponse(responseCode = "500", description = "구독 적용 중 오류 발생")
	})
	@PutMapping("/me")
	public Map<String, Object> updateMyPref(
			@Parameter(hidden = true) @org.springframework.security.core.annotation.AuthenticationPrincipal Object me,
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
			@RequestBody FcmPreferenceUpdateRequest req
	) {
		Ids ids = resolveIds(me, auth);
		if (ids.memberId == null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "NO_AUTH");

		FcmPreference saved = prefService.upsertForStoreMember(
				ids.memberId, ids.storeId,
				req.catNotice(), req.catStockLow(), req.catExpireSoon(), req.thresholdDays()
		);

		boolean apply = (req.applySubscriptions() == null) || req.applySubscriptions();
		if (apply && ids.storeId != null) {
			List<FcmDeviceToken> tokens = tokenRepo.findByMemberIdFkAndIsActiveTrue(ids.memberId);
			try {
				// 재고 부족 알림
				if (req.catStockLow() != null) {
					String topic = StoreTopic.invLow(ids.storeId);
					for (FcmDeviceToken t : tokens) {
						if (req.catStockLow()) fcmService.subscribe(t.getToken(), topic);
						else fcmService.unsubscribe(t.getToken(), topic);
					}
				}
				// 유통기한 임박 알림
				if (req.catExpireSoon() != null) {
					String topic = StoreTopic.expireSoon(ids.storeId);
					for (FcmDeviceToken t : tokens) {
						if (req.catExpireSoon()) fcmService.subscribe(t.getToken(), topic);
						else fcmService.unsubscribe(t.getToken(), topic);
					}
				}
				// 공지 구독(store-{id})
				if (req.catNotice() != null) {
					String topic = StoreTopic.store(ids.storeId);
					for (FcmDeviceToken t : tokens) {
						if (req.catNotice()) fcmService.subscribe(t.getToken(), topic);
						else fcmService.unsubscribe(t.getToken(), topic);
					}
				}
			} catch (FirebaseMessagingException e) {
				log.warn("[FCM] applySubscriptions failed", e);
			}
		}
		return Map.of("status", "ok", "prefId", saved.getFcmPreferenceId());
	}
}
