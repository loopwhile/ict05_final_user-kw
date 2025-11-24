package com.boot.ict05_final_user.domain.purchaseOrder.dto;

import com.boot.ict05_final_user.domain.purchaseOrder.entity.PurchaseOrderPriority;
import com.boot.ict05_final_user.domain.purchaseOrder.entity.PurchaseOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 가맹점 발주 목록 DTO
 * 목록 화면 및 엑셀 다운로드용 요약 데이터
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "가맹점 발주 목록 DTO")
public class PurchaseOrderListDTO {

    /** 발주 시퀀스 */
    @Schema(description = "발주 ID", example = "1")
    private Long id;

    /** 발주 코드 */
    @Schema(description = "발주 번호", example = "ORD20240115001")
    private String orderCode;

    /** 공급업체명 */
    @Schema(description = "공급업체명", example = "신선마트")
    private String supplier;

    /** 발주 대표 품목명 */
    @Schema(description = "발주 품목명 (대표 1개)", example = "토마토")
    private String mainItemName;

    /** 한 발주 내 품목 수 */
    @Schema(description = "발주 품목 수", example = "3")
    private Integer itemCount;

    /** 발주 총금액 */
    @Schema(description = "발주 총금액", example = "210000")
    private BigDecimal totalPrice;

    /** 발주 주문일 */
    @Schema(description = "발주일", example = "2024-01-15")
    private LocalDate orderDate;

    /** 발주 실제납기일 */
    @Schema(description = "실제 납기일", example = "2024-01-16")
    private LocalDate actualDeliveryDate;

    /** 발주 상태 */
    @Schema(description = "발주 상태", example = "RECEIVED")
    private PurchaseOrderStatus status;

    /** 발주 우선순위 */
    @Schema(description = "우선순위", example = "NORMAL")
    private PurchaseOrderPriority priority;

    /** 발주 비고 */
    @Schema(description = "비고", example = "신선도 확인 필요")
    private String notes;

    /**
     * 발주 상태(Enum)의 한글 설명 반환
     *
     * @return 한글 상태 설명 (예: "주문", "배송중", "검수완료"), 없으면 빈 문자열
     */
    public String getStatusDescription() { return status != null ? status.getDescription() : ""; }

    /**
     * 발주 우선순위(Enum)의 한글 설명 반환
     *
     * @return 한글 우선순위 설명 (예: "일반", "우선"), 없으면 빈 문자열
     */
    public String getPriorityDescription() { return priority != null ? priority.getDescription() : ""; }
}
