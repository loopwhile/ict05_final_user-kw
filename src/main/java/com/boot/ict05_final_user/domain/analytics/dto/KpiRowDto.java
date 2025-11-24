package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * KPI 테이블 한 행 (일별/월별 공용)입니다.
 *
 * <p>label 필드는 YYYY-MM-DD(일별) 또는 YYYY-MM(월별) 형태를 사용합니다.</p>
 *
 * @param label 날짜 또는 연월(label). (YYYY-MM-DD 또는 YYYY-MM).
 * @param sales 매출 합계(원).
 * @param tx 주문 수(건).
 * @param upt 판매수량 / 주문수로 계산된 UPT(단위/건).
 * @param ads 객단가(매출/주문수, 반올림).
 * @param aur 판매단가(매출/판매수량, 반올림).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record KpiRowDto(
		@Schema(description = "라벨(YYYY-MM-DD 또는 YYYY-MM).", example = "2025-11-01")
		String label,

		@Schema(description = "매출 합계(원).", example = "1250000")
		long sales,

		@Schema(description = "주문 수(건).", example = "320")
		long tx,

		@Schema(description = "UPT = 판매수량 / 주문수.", example = "1.5")
		double upt,

		@Schema(description = "ADS = 매출 / 주문수 (반올림).", example = "3906")
		long ads,

		@Schema(description = "AUR = 매출 / 판매수량 (반올림).", example = "2600")
		long aur
) {}
