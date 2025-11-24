package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * KPI 요약 카드용 DTO입니다.
 *
 * <p>MTD(이번달 1일 ~ 어제) 기준의 주요 KPI를 담습니다.</p>
 *
 * @param salesMtd 이번달 매출 합계(원).
 * @param txMtd 이번달 주문 건수(건).
 * @param unitsMtd 이번달 판매 수량 합계(단위).
 * @param uptMtd UPT(Units_MTD / Tx_MTD).
 * @param adsMtd ADS(매출Mtd / Tx_Mtd), 반올림.
 * @param aurMtd AUR(매출Mtd / Units_Mtd), 반올림.
 * @param wowPercent WoW% (최근 7일 대비 그 이전 7일 증감률). 분모 0일 경우 null.
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record KpiSummaryDto(
		@Schema(description = "이번달 매출 합계(원).", example = "1250000")
		long salesMtd,

		@Schema(description = "이번달 주문 건수(건).", example = "320")
		long txMtd,

		@Schema(description = "이번달 판매 수량 합계(단위).", example = "480")
		long unitsMtd,

		@Schema(description = "UPT = Units_MTD / Tx_MTD.", example = "1.5")
		double uptMtd,

		@Schema(description = "ADS = Sales_MTD / Tx_MTD (반올림).", example = "3906")
		long adsMtd,

		@Schema(description = "AUR = Sales_MTD / Units_MTD (반올림).", example = "2600")
		long aurMtd,

		@Schema(description = "WoW%: 최근 7일 대비 이전 7일 증감률. 분모가 0이면 null.", example = "12.5")
		Double wowPercent
) {}
