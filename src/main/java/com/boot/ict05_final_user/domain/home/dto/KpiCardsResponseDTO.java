package com.boot.ict05_final_user.domain.home.dto;


import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiCardsResponseDTO {
    private LocalDateTime date;          // 기준일 (예: 오늘)
    private Long storeId;            // 가맹점 ID (선택)
    private List<KpiCardDTO> cards;  // KPI 카드 4개
}
