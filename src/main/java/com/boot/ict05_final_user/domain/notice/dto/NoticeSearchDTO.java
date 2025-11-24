package com.boot.ict05_final_user.domain.notice.dto;

import com.boot.ict05_final_user.domain.notice.entity.NoticePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 공지사항 검색 조건 DTO.
 *
 * <p>검색 키워드, 타입, 페이지 크기, 우선순위 조건 등을 포함합니다.</p>
 *
 */
@Data
@Schema(description = "공지사항 검색 조건 DTO")
public class NoticeSearchDTO {

    /** 검색어 */
    @Schema(description = "검색 키워드")
    private String s;

    /** 검색 타입 (예: title, body 등) */
    @Schema(description = "검색 타입 (예: title, body)")
    private String type;

    /** 페이지당 표시할 건수 (문자열형, 기본값 10) */
    @Schema(description = "페이지당 표시 건수 (기본값 10)")
    private String size = "10";

    /** 공지사항 우선순위 필터 */
    @Schema(description = "공지사항 우선순위 필터")
    private NoticePriority priority;
}
