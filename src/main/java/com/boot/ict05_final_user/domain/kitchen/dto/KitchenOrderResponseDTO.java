package com.boot.ict05_final_user.domain.kitchen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주방 주문 응답(Kitchen Order Response) DTO.
 *
 * <p>주방 화면(KDS)과 가맹점 주문 모니터에 표시할 주문 단위를 표현합니다.
 * 주문 기본정보, 품목 리스트, 금액 정보(총액), 상태/우선순위, 결제수단 등을 포함합니다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "주방 주문 응답 DTO")
public class KitchenOrderResponseDTO {

    /** 주문 ID(내부 식별자) */
    @Schema(description = "주문 ID", nullable = false)
    private Long id;

    /** 외부/프론트/백엔드에서 부여된 주문 코드 */
    @Schema(description = "주문 코드")
    private String orderCode;

    /** 주문 품목 목록 */
    @Schema(description = "주문 품목 리스트", implementation = KitchenOrderItemDTO.class)
    private List<KitchenOrderItemDTO> items;

    /** 주문 총 금액 */
    @Schema(description = "총 결제 금액(할인 적용 후)",nullable = false)
    private BigDecimal total;

    /** 할인 전 금액(정가 총합) */
    @Schema(description = "할인 전 금액")
    private BigDecimal originalTotal;

    /** 할인 금액 */
    @Schema(description = "할인 금액")
    private BigDecimal discount;

    /**
     * 주문 진행 상태
     * <p>프론트 요구사항에 따라 소문자 표기 가능: preparing | cooking | ready | completed</p>
     */
    @Schema(
            description = "주문 상태(프론트 표기 규격). 예: preparing, cooking, ready, completed",
            example = "preparing"
    )
    private String status;

    /** 주문 생성(접수) 시각 */
    @Schema(description = "주문 시각(ISO-8601)")
    private LocalDateTime orderTime;

    /** 고객 식별/표시명 또는 메모에 표시할 이름 */
    @Schema(description = "고객 이름/표시명")
    private String customer;

    /**
     * 결제 수단
     * <p>시스템 상위 Enum과 매핑: CARD, CASH, VOUCHER, EXTERNAL</p>
     */
    @Schema(description = "결제 수단")
    private String paymentMethod;

    /**
     * 주문 유형
     * <p>시스템 상위 Enum과 매핑: VISIT, TAKEOUT, DELIVERY</p>
     */
    @Schema(description = "주문 유형")
    private String orderType;

    /** 우선순위(예: NORMAL, URGENT 등 비즈니스 정책에 따름) */
    @Schema(description = "우선순위")
    private String priority;

    /** 가게/주방/고객 요청 메모 */
    @Schema(description = "비고/메모")
    private String notes;
}
