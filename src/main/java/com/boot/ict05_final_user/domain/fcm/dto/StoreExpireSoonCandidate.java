package com.boot.ict05_final_user.domain.fcm.dto;

import lombok.*;

/**
 * 유통기한 임박 후보 항목 DTO.
 *
 * <p>FCM 알림 전송 전에 임박 대상 재료를 선별할 때 사용됩니다.
 * 본사 HqExpireSoonCandidate와 동일한 필드 구성을 가집니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoreExpireSoonCandidate {

	/** 자재 ID */
	private Long materialId;

	/** 자재명 */
	private String materialName;

	/** LOT 번호 */
	private String lot;

	/** 유통기한 */
	private java.time.LocalDate expireDate;

	/** 남은 일수 */
	private Integer daysLeft;
}
