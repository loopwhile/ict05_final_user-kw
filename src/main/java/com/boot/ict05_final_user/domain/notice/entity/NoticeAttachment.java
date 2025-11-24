package com.boot.ict05_final_user.domain.notice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class NoticeAttachment {

    /**
     * 첨부파일 고유 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 첨부파일이 연결된 공지사항 ID
     */
    private Long noticeId;

    /**
     * 첨부파일 URL
     */
    private String url;
    /**
     * 원본 파일명
     */
    private String originalFilename;
}
