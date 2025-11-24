package com.boot.ict05_final_user.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 주문 분석 일별 테이블 한 행 (주문 1건 기준).
 *
 * <p>주문 단위의 상세 행 데이터를 프론트/리포트에서 사용하기 위한 DTO입니다.</p>
 *
 * @param orderDate 주문일자 (YYYY-MM-DD).
 * @param orderId 주문 고유 ID.
 * @param orderCode 주문 코드(외부 연동 코드 등).
 * @param orderType 주문 유형 (VISIT / TAKEOUT / DELIVERY).
 * @param totalPrice 주문 총금액(원).
 * @param menuCount 메뉴 수(수량 합).
 * @param paymentType 결제수단 (CARD / CASH / VOUCHER / EXTERNAL).
 * @param channelMemo 채널 메모(배달사 메모 등).
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public record OrderDailyRowDto(
		@Schema(description = "주문 일자 (YYYY-MM-DD).", example = "2025-11-05")
		String orderDate,

		@Schema(description = "주문 ID.", example = "123456789")
		Long orderId,

		@Schema(description = "주문 코드.", example = "ORD-20251105-0001")
		String orderCode,

		@Schema(description = "주문 유형. VISIT / TAKEOUT / DELIVERY.", example = "DELIVERY")
		String orderType,

		@Schema(description = "총금액(원).", example = "45000")
		long totalPrice,

		@Schema(description = "메뉴 수(수량 합).", example = "3")
		long menuCount,

		@Schema(description = "결제 유형. CARD / CASH / VOUCHER / EXTERNAL.", example = "CARD")
		String paymentType,

		@Schema(description = "채널 메모(주문 채널 관련 메모).", example = "문 앞에 놔주세요.")
		String channelMemo
) {
}
