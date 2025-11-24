package com.boot.ict05_final_user.domain.kitchen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 주방 주문 상태 변경 요청 DTO.
 *
 * <p>주문의 진행 상태를 변경할 때 사용합니다.
 * 프론트 규격에 맞춰 소문자 상태값을 사용합니다.</p>
 */
@Getter
@Setter
@Schema(description = "주방 주문 상태 변경 요청 DTO")
public class UpdateKitchenOrderStatusRequestDTO {

    /**
     * 변경할 주문 상태
     * <p>허용 값: preparing | cooking | ready | completed</p>
     */
    @Schema(
            description = "변경할 주문 상태",
            example = "cooking",
            allowableValues = {"preparing", "cooking", "ready", "completed"},
            nullable = false
    )
    private String status;
}
