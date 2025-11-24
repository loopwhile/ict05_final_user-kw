package com.boot.ict05_final_user.domain.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 정규화된 소진 요청 DTO
 * 프런트가 메뉴를 레시피로 풀어 가맹점 재료 기준으로 전송한다
 * 수량은 재고 기본 단위로 보낸다
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreConsumeRequestDTO {

    /** 소진 시각 미전달 시 서버가 now 적용 */
    private LocalDateTime saleAt;

    /** 소진 라인 목록 비어 있을 수 없음 */
    @NotEmpty
    @Valid
    private List<Line> lines;

    /** 공통 메모 선택 */
    private String memo;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Line {
        /** 가맹점 재료 PK */
        @NotNull
        private Long storeMaterialId;

        /** 소진 수량 소수 셋째 자리까지 */
        @NotNull
        @DecimalMin(value = "0.001")
        @Digits(integer = 12, fraction = 3)
        private BigDecimal quantity;
    }
}
