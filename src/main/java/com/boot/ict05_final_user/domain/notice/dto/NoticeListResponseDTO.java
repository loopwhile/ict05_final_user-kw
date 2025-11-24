package com.boot.ict05_final_user.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * 공지사항 목록 응답 DTO.
 *
 * <p>공지 목록과 함께 카운트 정보({@link NoticeCountDTO})를 포함하여 반환합니다.</p>
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공지사항 목록 응답 DTO")
public class NoticeListResponseDTO {

    /** 공지사항 페이지 데이터 */
    @Schema(description = "공지사항 페이지 데이터 (페이징 포함)")
    private Page<NoticeListDTO> pageData;

    /** 공지사항 집계 데이터 */
    @Schema(description = "공지사항 요약 카운트 데이터")
    private NoticeCountDTO countData;
}
