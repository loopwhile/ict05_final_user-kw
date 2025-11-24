package com.boot.ict05_final_user.domain.notice.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    /** 공지사항 고유 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    /** 작성자(회원) FK */
    @Column(name = "member_id_fk")
    private Long memberIdFk;

    /** 공지사항 카테고리 */
    @Enumerated(EnumType.STRING)
    @Column(name = "notice_category")
    private NoticeCategory noticeCategory;

    /** 공지사항 우선순위 */
    @Enumerated(EnumType.STRING)
    @Column(name = "notice_priority")
    private NoticePriority noticePriority;

    /** 공지사항 상태  */
    @Enumerated(EnumType.STRING)
    @Column(name = "notice_status")
    private NoticeStatus noticeStatus;

    /** 공지사항 노출 여부 */
    @Column(name = "is_show")
    private Boolean isShow;

    /** 작성자 이름 */
    @Column(name = "writer")
    private String writer;

    /** 공지사항 제목 */
    @Column(name = "notice_title")
    private String title;

    /** 공지사항 본문 내용 */
    @Column(name = "notice_content")
    private String body;

    /** 작성일자 */
    @Schema(type="string", format="date-time")
    @Column(name = "notice_reg_date")
    private LocalDateTime registeredAt;

    /** 조회수 */
    @Column(name = "notice_count")
    private Integer noticeCount;

    /** 확인 여부 (TINYINT(1) → Boolean) */
    @Column(name = "notice_confirmed")
    private Boolean noticeConfirmed;

    /**
     * 조회수를 1 증가시키는 메서드
     */
    public void incrementNoticeCount() {
        if (this.noticeCount == null) {
            this.noticeCount = 0;
        }
        this.noticeCount++;
    }
}