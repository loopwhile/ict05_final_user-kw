package com.boot.ict05_final_user.domain.purchaseOrder.dto;

import com.boot.ict05_final_user.domain.purchaseOrder.entity.PurchaseOrderPriority;
import com.boot.ict05_final_user.domain.purchaseOrder.entity.PurchaseOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 발주 상세 조회 DTO
 * React 모달에서 표시되는 발주 상세 정보를 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "가맹점 발주 상세 DTO")
public class PurchaseOrderDetailDTO {

    /** 발주 상세 시퀀스 */
    @Schema(description = "발주 ID")
    private Long id;

    /** 발주 코드 */
    @Schema(description = "발주번호 (ORD20240115001 형식)")
    private String orderCode;

    /** 발주 공급업체명 */
    @Schema(description = "공급업체명")
    private String supplier;

    /** 발주 주문일 */
    @Schema(description = "발주일자")
    private LocalDate orderDate;

    /** 발주 실제납기일 */
    @Schema(description = "실제 납기일자")
    private LocalDate actualDate;

    /** 발주 우선순위 */
    @Schema(description = "우선순위")
    private PurchaseOrderPriority priority;

    /** 발주 상태 */
    @Schema(description = "발주 상태")
    private PurchaseOrderStatus status;

    /** 발주 총 금액 */
    @Schema(description = "총 발주 금액")
    private BigDecimal totalPrice;

    /** 발주 비고 */
    @Schema(description = "비고 (특이사항)")
    private String notes;

    /** 발주 총 개수 */
    private Integer itemCount;

    /** 발주 품목 목록 리스트 */
    @Schema(description = "발주 품목 목록")
    private List<PurchaseOrderItemDTO> items;

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
