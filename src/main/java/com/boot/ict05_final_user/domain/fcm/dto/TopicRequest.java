package com.boot.ict05_final_user.domain.fcm.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * FCM 토픽 구독/해제 요청 DTO.
 *
 * <p>단말이 특정 토픽에 대해 구독하거나 구독 해제할 때 사용합니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record TopicRequest(

		/** 토픽명 (예: store-1, inv-low-3 등) */
		@NotBlank String topic
) { }
