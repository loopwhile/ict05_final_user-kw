package com.boot.ict05_final_user.domain.fcm.dto;

import com.boot.ict05_final_user.domain.fcm.entity.AppType;
import com.boot.ict05_final_user.domain.fcm.entity.PlatformType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * FCM 디바이스 토큰 등록(업서트) 요청 DTO.
 *
 * <p>가맹점 앱에서 디바이스 토큰을 서버에 등록할 때 사용합니다.
 * appType은 서버에서 STORE로 강제 오버라이드될 수 있습니다.</p>
 *
 * <p>본 DTO는 HQ(Admin) 시스템과 동일한 구조를 사용합니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record FcmRegisterTokenRequest(
		/** 앱 타입 (STORE, HQ 등) */
		@NotNull AppType appType,

		/** 플랫폼 타입 (ANDROID, IOS, WEB 등) */
		@NotNull PlatformType platform,

		/** 디바이스 FCM 토큰 */
		@NotBlank String token,

		/** 디바이스 고유 식별자 (선택적) */
		String deviceId,

		/** 회원 ID 외래키 (내부 참조용) */
		Long memberIdFk
) { }
