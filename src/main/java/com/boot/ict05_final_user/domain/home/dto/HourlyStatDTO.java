package com.boot.ict05_final_user.domain.home.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HourlyStatDTO {
    private String time;          // "09:00"
    private Long   sales;         // 매출(원)
    private Integer orders;       // 총 주문수
    private Integer visitOrders;  // 방문주문
    private Integer takeoutOrders;// 포장주문
    private Integer deliveryOrders;// 배달주문
}
