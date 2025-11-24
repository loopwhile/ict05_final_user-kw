package com.boot.ict05_final_user.domain.home.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiCardDTO {

    /** 카드 식별키: sales_today, orders_today, visitors_today, top_menu 등 */
    private String key;

    /** 카드 메인 값: 예) "₩542만", "138건", "치킨버거" */
    private String value;

    /** 보조 설명: 예) "어제 대비 +8.2%", "28개 판매" */
    private String change;

    /** increase | decrease | neutral */
    private ChangeType changeType;

}
