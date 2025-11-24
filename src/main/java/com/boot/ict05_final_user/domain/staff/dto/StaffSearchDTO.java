package com.boot.ict05_final_user.domain.staff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 직원 목록 검색 조건 DTO.
 *
 * <p>
 * 직원 리스트 조회 시 검색어, 검색 타입, 페이지 크기, 가맹점 ID를 전달하는
 * 가벼운 파라미터 컨테이너이다.
 * </p>
 */
@Data
@Schema(description = "직원 목록 검색 조건 DTO")
public class StaffSearchDTO {

    /** 검색어 (직원명, 직원 ID 등) */
    @Schema(description = "검색어(직원명, 직원 ID 등)", example = "홍길동")
    private String keyword;

    /** 검색 타입 (name, status, all 등) */
    @Schema(description = "검색 타입(name, status, all 등)", example = "name")
    private String type;

    /** 페이지 사이즈 */
    @Schema(description = "페이지 크기(문자열)", example = "10", defaultValue = "10")
    private String size = "10";

    /** 가맹점 ID */
    @Schema(description = "가맹점 ID", example = "5")
    private Long storeId;
}
