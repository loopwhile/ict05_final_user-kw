package com.boot.ict05_final_user.domain.fcm.dto;

import lombok.*;

/**
 * 재고 부족 후보 항목 DTO.
 *
 * <p>알림 생성 전에 재고 부족 상태를 감지한 항목을 임시 저장할 때 사용됩니다.
 * 본사 {@code HqStockLowCandidate}와 동일한 구조를 가집니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoreStockLowCandidate {

	/** 자재 ID */
	private Long materialId;

	/** 자재명 */
	private String materialName;

	/** 현재 수량 */
	private Long qty;

	/** 부족 기준 임계값 */
	private Long threshold;
}
