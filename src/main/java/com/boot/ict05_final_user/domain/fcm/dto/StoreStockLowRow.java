package com.boot.ict05_final_user.domain.fcm.dto;

import java.math.BigDecimal;

/**
 * 재고 부족 항목 조회용 DTO.
 *
 * <p>본 DTO는 재고 부족 상태를 확인하기 위한 읽기 전용 데이터 전송 객체이며,
 * 본사 {@code HqStockLowRow}와 동일한 인터페이스를 제공합니다.</p>
 *
 * <p>수량 차이를 계산하기 위한 {@link #deficit()} 메서드를 제공합니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record StoreStockLowRow(
		/** 자재 ID */
		Long materialId,

		/** 자재명 */
		String materialName,

		/** 현재 재고 수량 */
		BigDecimal quantity,

		/** 적정(목표) 수량 */
		BigDecimal optimal
) {
	/**
	 * 부족 수량을 계산합니다.
	 *
	 * @return {@code optimal - quantity} 값 (음수가 되지 않도록 0 이상 반환)
	 */
	public BigDecimal deficit() {
		if (quantity == null || optimal == null) return BigDecimal.ZERO;
		return optimal.subtract(quantity);
	}
}
