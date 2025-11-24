package com.boot.ict05_final_user.domain.fcm.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * FCM 테스트 메시지 발송 요청 DTO.
 *
 * <p>토픽 또는 단일 토큰 대상으로 테스트 메시지를 발송할 때 사용합니다.
 * link 등 부가 정보는 data 맵을 통해 전달됩니다.</p>
 *
 * <p>본 DTO는 HQ(Admin) 시스템과 동일한 구조를 사용합니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record FcmTestSendRequest(
		/** 수신 대상 (토큰 또는 토픽) */
		@NotBlank String tokenOrTopic,

		/** 토픽 발송 여부 */
		boolean topic,

		/** 메시지 제목 */
		@NotBlank String title,

		/** 메시지 내용 */
		@NotBlank String body,

		/** 부가 데이터 (link 등) */
		Map<String, String> data
) { }
