package com.boot.ict05_final_user.domain.fcm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 유통기한 임박 항목 조회용 DTO.
 *
 * <p>임박 항목 조회 시 사용되며, 본사 HqExpireSoonRow와 동일한 필드 구성을 가집니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record StoreExpireSoonRow(
		/** 자재 ID */
		Long materialId,

		/** 자재명 */
		String materialName,

		/** 재고 배치 ID */
		Long batchId,

		/** 유통기한 */
		LocalDate expireDate,

		/** 남은 일수 */
		Integer daysLeft,

		/** 수량 */
		BigDecimal quantity
) { }
