package com.boot.ict05_final_user.domain.fcm.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 테스트 FCM 메시지 발송 요청 DTO.
 *
 * <p>테스트용으로 FCM 토픽 또는 개별 토큰에 메시지를 발송할 때 사용합니다.
 * {@code link} 필드는 {@code data.link} 값보다 우선 적용됩니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record TestSendRequest(
		/** 수신 대상 (토큰 또는 토픽) */
		@NotBlank String tokenOrTopic,

		/** 토픽 여부 (true 시 topic 대상 발송) */
		boolean topic,

		/** 메시지 제목 */
		@NotBlank String title,

		/** 메시지 본문 */
		@NotBlank String body,

		/** 부가 데이터 (link, meta 등) */
		Map<String, String> data,

		/** 링크 (data.link보다 우선 적용) */
		String link
) { }
