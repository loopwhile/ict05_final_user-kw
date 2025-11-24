package com.boot.ict05_final_user.domain.notice.dto;

import com.boot.ict05_final_user.domain.notice.entity.NoticeCategory;
import com.boot.ict05_final_user.domain.notice.entity.NoticePriority;
import com.boot.ict05_final_user.domain.notice.entity.NoticeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 공지사항 목록 조회용 DTO.
 *
 * <p>공지 목록 테이블 표시 및 리스트 카드용 정보를 제공합니다.</p>
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "공지사항 목록 DTO")
public class NoticeListDTO {

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

    /** 공지사항 상태 */
    @Schema(description = "공지사항 상태")
    private NoticeStatus noticeStatus;

    /** 공지사항 공개 여부 */
    @Schema(description = "공지사항 공개 여부")
    private boolean isShow;

    /** 공지사항 제목 */
    @Schema(description = "공지사항 제목")
    private String title;

    /** 공지사항 내용 */
    @Schema(description = "공지사항 본문 (요약용)")
    private String body;

    /** 작성자 이름 */
    @Schema(description = "작성자 이름")
    private String writer;

    /** 조회수 */
    @Schema(description = "조회수")
    private Integer noticeCount;

    /** 작성일시 */
    @Schema(description = "작성일시", type = "string", format = "date-time")
    private LocalDateTime registeredAt;

    /** 첨부파일 존재 여부 */
    @Schema(description = "첨부파일 존재 여부")
    private boolean hasAttachment;

    /** 첫 번째 첨부파일 URL */
    @Schema(description = "첫 번째 첨부파일 URL (목록에서 다운로드용)")
    private String firstAttachmentUrl;

    /**
     * 작성일자를 "yyyy.MM.dd" 형식의 문자열로 반환한다.
     * 작성일자가 null이면 빈 문자열을 반환한다.
     *
     * @return 형식화된 작성일자 문자열
     */
    public String getWriteDate() {
        if (registeredAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        return registeredAt.format(formatter);
    }

    /** 한글 라벨: 상태 */
    public String getNoticeStatusLabel() {
        return noticeStatus != null ? noticeStatus.getDescription() : "";
    }

    /** 한글 라벨: 카테고리 */
    public String getNoticeCategoryLabel() {
        return noticeCategory != null ? noticeCategory.getDescription() : "";
    }

    /** 한글 라벨: 우선순위 */
    public String getNoticePriorityLabel() {
        return noticePriority != null ? noticePriority.getDescription() : "";
    }
}
