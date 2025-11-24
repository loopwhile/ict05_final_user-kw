package com.boot.ict05_final_user.domain.fcm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 가맹점 사용자의 FCM 알림 수신 설정 변경 요청 DTO.
 *
 * <p>사용자는 공지, 재고 부족, 유통기한 임박 등의 알림을
 * 선택적으로 구독/해제할 수 있습니다.</p>
 *
 * <p>본 DTO는 HQ(Admin) 시스템과 동일한 구조를 사용합니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record FcmPreferenceUpdateRequest(
		/** 공지 알림 수신 여부 */
		Boolean catNotice,

		/** 재고 부족 알림 수신 여부 */
		Boolean catStockLow,

		/** 유통기한 임박 알림 수신 여부 */
		Boolean catExpireSoon,

		/** 임박 기준일(1~30일 사이 설정 가능) */
		@Min(1) @Max(30) Integer thresholdDays,

		/** 변경 즉시 구독 반영 여부 (기본 true) */
		Boolean applySubscriptions
) { }
