package com.boot.ict05_final_user.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 가맹점 주문 목록 조회용 검색/필터 DTO.
 *
 * <p>키워드, 상태, 결제수단, 주문유형, 기간 프리셋을 통해 목록 검색을 수행합니다.</p>
 */
@Data
@Schema(description = "주문 목록 검색/필터 DTO")
public class CustomerOrderSearchDTO {

    /** 검색어 */
    @Schema(description = "검색어(주문번호/고객명/전화번호/메뉴명 등)")
    private String keyword;

    /** 상태 */
    @Schema(
            description = "주문 상태",
            allowableValues = {"PENDING", "PREPARING", "COOKING", "READY", "COMPLETED", "CANCELLED"}
    )
    private String status;

    /** 결제 */
    @Schema(
            description = "결제 수단(영문 코드 또는 한글 라벨)",
            allowableValues = {"CARD", "CASH", "VOUCHER", "EXTERNAL"}
    )
    private String paymentType;

    /** 주문유형 */
    @Schema(
            description = "주문 유형",
            allowableValues = {"VISIT", "TAKEOUT", "DELIVERY"}
    )
    private String orderType;

    /** 기간 */
    @Schema(
            description = "조회 기간 프리셋",
            allowableValues = {"all", "today", "week", "month"}
    )
    private String period = "all";
}
