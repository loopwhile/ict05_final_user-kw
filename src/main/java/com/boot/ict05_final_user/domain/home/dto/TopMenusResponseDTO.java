package com.boot.ict05_final_user.domain.home.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대시보드 TOP 메뉴 응답 DTO
 *
 * 기준일과 집계 구간, 매장 필터, 항목 리스트를 포함한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopMenusResponseDTO {

    /** 기준일 예 오늘 날짜와 시간 */
    private LocalDateTime date;

    /** 집계 시작 시각 선택 */
    private LocalDateTime periodStart;

    /** 집계 종료 시각 선택 */
    private LocalDateTime periodEnd;

    /** 가맹점 ID 선택 null이면 전체 */
    private Long storeId;

    /** 요청한 Top N 기본 5 */
    private Integer limit;

    /** Top N 메뉴 목록 */
    private List<TopMenuItemDTO> items;
}
