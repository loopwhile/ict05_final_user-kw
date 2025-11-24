package com.boot.ict05_final_user.domain.store.dto;

import com.boot.ict05_final_user.domain.store.entity.StoreStatus;
import lombok.Data;

/** 매장 목록 검색 조건 DTO.
 *
 * <p>목록 조회 시 전달되는 가벼운 파라미터 컨테이너로,
 * 키워드/검색유형/페이지 크기(문자열)를 담는다.</p>
 *
 * <ul>
 *     <li>{@code keyword} : 검색어(매장명/점주명 등)</li>
 *     <li>{@code type}    : 검색 유형(예: "name", "owner" 등 프런트·컨트롤러 약속값)</li>
 *     <li>{@code size}    : 페이지 크기(문자열, 기본 "10")</li>
 * </ul>
 *
 * */
@Data
public class StoreSearchDTO {
    private String keyword;        // 검색어(매장명, 점주명 등).
    private String type;           // 검색 유형 키.
    private String size = "10";    // 페이지 크기(문자열).

    /** 가맹점 상태 필터 (USE / STOP) */
    private StoreStatus status;
}
