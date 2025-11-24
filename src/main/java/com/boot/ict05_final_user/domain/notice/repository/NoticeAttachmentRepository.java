package com.boot.ict05_final_user.domain.notice.repository;

import com.boot.ict05_final_user.domain.notice.entity.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 공지사항 첨부파일 리포지토리.
 *
 * <p>공지사항 ID를 기준으로 첨부파일 엔티티를 조회합니다.</p>
 *
 * <ul>
 *   <li>공지사항 ID별 첨부파일 목록 조회</li>
 * </ul>
 *
 */
public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    /**
     * 공지사항 ID를 기준으로 첨부파일 목록을 조회한다.
     *
     * @param noticeId 공지사항 ID
     * @return 첨부파일 리스트
     */
    List<NoticeAttachment> findByNoticeId(Long noticeId);
}
