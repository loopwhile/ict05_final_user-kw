package com.boot.ict05_final_user.domain.fcm.dto;

import com.boot.ict05_final_user.domain.fcm.entity.PlatformType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 가맹점 단말의 FCM 토큰 등록(업서트) 요청 DTO.
 *
 * <p>앱에서 FCM 토큰을 업서트할 때 사용되며,
 * 서버에서 {@code appType=STORE}로 강제 설정합니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record TokenUpsertRequest(
		/** 디바이스의 FCM 토큰 */
		@NotBlank String token,

		/** 플랫폼 타입 (ANDROID, IOS, WEB 등) */
		@NotNull PlatformType platform,

		/** 디바이스 식별자 (선택적) */
		String deviceId
) { }
