package com.boot.ict05_final_user.domain.notice.service;

import com.boot.ict05_final_user.domain.notice.entity.NoticeAttachment;
import com.boot.ict05_final_user.domain.notice.repository.NoticeAttachmentRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 공지사항 첨부파일 서비스 클래스.
 *
 * <p>공지사항과 연결된 첨부파일의 조회 기능을 제공합니다.</p>
 *
 * <ul>
 *   <li>공지 ID 기반 첨부파일 목록 조회</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Service
@Transactional
@RequiredArgsConstructor
@Schema(description = "공지사항 첨부파일 서비스 로직")
public class NoticeAttachmentService {

    private final NoticeAttachmentRepository noticeAttachmentRepository;

    /**
     * 공지사항 ID를 기준으로 첨부파일 목록을 조회한다.
     *
     * @param noticeId 공지사항 ID
     * @return 해당 공지사항에 첨부된 파일 목록
     */
    public List<NoticeAttachment> findByNoticeId(Long noticeId) {
        return noticeAttachmentRepository.findByNoticeId(noticeId);
    }
}
