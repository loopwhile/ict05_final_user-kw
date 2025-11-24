package com.boot.ict05_final_user.domain.notice.dto;

import com.boot.ict05_final_user.domain.notice.entity.NoticeCategory;
import com.boot.ict05_final_user.domain.notice.entity.NoticePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 공지사항 상세 DTO.
 *
 * <p>공지 상세 화면에 필요한 모든 필드를 포함합니다.</p>
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "공지사항 상세 DTO")
public class NoticeDetailDTO {

    /** 공지사항 고유 ID */
    @Schema(description = "공지사항 ID")
    private Long id;

    /** 작성자(회원) FK */
    @Schema(description = "작성자 회원 ID")
    private Long memberIdFk;

    /** 공지사항 카테고리 */
    @Schema(description = "공지사항 카테고리")
    private NoticeCategory noticeCategory;

    /** 공지사항 우선순위 */
    @Schema(description = "공지사항 우선순위")
    private NoticePriority noticePriority;

    /** 공지사항 공개 여부 */
    @Schema(description = "공지사항 공개 여부")
    private boolean isShow;

    /** 공지사항 제목 */
    @Schema(description = "공지사항 제목")
    private String title;

    /** 공지사항 내용 */
    @Schema(description = "공지사항 본문 내용")
    private String body;

    /** 작성자 이름 */
    @Schema(description = "작성자 이름")
    private String writer;

    /** 작성일시 */
    @Schema(description = "공지 작성일시", type = "string", format = "date-time")
    private java.time.LocalDateTime registeredAt;
}
