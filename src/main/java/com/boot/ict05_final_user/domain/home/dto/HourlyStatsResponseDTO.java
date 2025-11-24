// domain/home/dto/HourlyStatsResponseDTO.java
package com.boot.ict05_final_user.domain.home.dto;


import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HourlyStatsResponseDTO {
    private LocalDate date;            // 기준일 (오늘)
    private Long storeId;              // 선택
    private List<HourlyStatDTO> items; // 시간대별 포인트
}
