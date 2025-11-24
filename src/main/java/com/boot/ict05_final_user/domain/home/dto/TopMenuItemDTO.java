package com.boot.ict05_final_user.domain.home.dto;

import lombok.*;

/**
 * 대시보드 TOP 메뉴 단건 DTO
 *
 * 순위, 메뉴명, 판매수량, 매출 금액, 표시용 이미지(이모지 또는 이미지 URL)를 포함한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopMenuItemDTO {

    /** 메뉴 식별자(선택) */
    private Long menuId;

    /** 메뉴 이름 */
    private String name;

    /** 판매 수량 */
    private Integer quantity;

    /** 매출 금액 원 단위 */
    private Long sales;

    /**
     * 표시용 이미지
     * 이모지나 간단한 텍스트 아이콘을 내려줄 때 사용. 선택
     * 예: "🍔"
     */
    private String image;

    /**
     * 이미지 URL
     * 이미지 파일이나 CDN 경로가 있을 때 사용. 선택
     * 예: "https://cdn.example.com/menu/123.png"
     */
    private String imageUrl;
}
