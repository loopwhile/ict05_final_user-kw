package com.boot.ict05_final_user.domain.fcm.dto;

import java.time.LocalDateTime;

/**
 * FCM 전송 로그 목록 조회용 읽기 전용 DTO.
 *
 * <p>본 DTO는 FCM 발송 이력 데이터를 조회할 때 사용되며,
 * 본사(Admin) 시스템과 동일한 필드 구성을 유지합니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record FcmLogRowDto(
		Long id,
		String topic,
		String token,
		String title,
		String body,
		String resultMessageId,
		String resultError,
		LocalDateTime sentAt
) { }
