package com.boot.ict05_final_user.domain.notice.service;

import com.boot.ict05_final_user.domain.notice.dto.NoticeCountDTO;
import com.boot.ict05_final_user.domain.notice.dto.NoticeListDTO;
import com.boot.ict05_final_user.domain.notice.dto.NoticeListResponseDTO;
import com.boot.ict05_final_user.domain.notice.dto.NoticeSearchDTO;
import com.boot.ict05_final_user.domain.notice.entity.Notice;
import com.boot.ict05_final_user.domain.notice.entity.NoticePriority;
import com.boot.ict05_final_user.domain.notice.repository.NoticeRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공지사항 서비스 클래스.
 *
 * <p>공지사항 목록 및 상세 조회를 처리하며, 조회 시 통계 데이터(중요·긴급·전체 개수)를 함께 제공합니다.</p>
 *
 * <ul>
 *   <li>공지 목록 페이징 조회</li>
 *   <li>공지 상세 조회 및 조회수 증가</li>
 * </ul>
 *
 * <p>트랜잭션 범위는 클래스 레벨에서 선언되어 있으며,
 * 읽기 작업에 대해서도 데이터 정합성을 위해 관리됩니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
@Schema(description = "공지사항 서비스 로직")
public class NoticeService {

    private final NoticeRepository noticeRepository;

    /**
     * 공지사항 목록을 페이지 단위로 조회하고, 중요도별 카운트 정보를 함께 반환한다.
     *
     * @param noticeSearchDTO 검색 및 필터 조건 DTO
     * @param pageable        페이지 정보(번호·크기·정렬)
     * @return 공지사항 목록 및 카운트 정보가 포함된 응답 DTO
     */
    public NoticeListResponseDTO selectAllOfficeNotice(NoticeSearchDTO noticeSearchDTO, Pageable pageable) {
        Page<NoticeListDTO> pageData = noticeRepository.listNotice(noticeSearchDTO, pageable);

        long totalCount = noticeRepository.count();
        long urgentCount = noticeRepository.countByPriority(NoticePriority.EMERGENCY);
        long importantCount = noticeRepository.countByPriority(NoticePriority.IMPORTANT);
        long unreadCount = 0; // 현재 로직상 미사용

        NoticeCountDTO countData = new NoticeCountDTO(totalCount, urgentCount, importantCount, unreadCount);

        return new NoticeListResponseDTO(pageData, countData);
    }

    /**
     * 공지사항 상세 정보를 조회하고 조회수를 1 증가시킨다.
     *
     * @param id 공지사항 ID
     * @return 공지사항 엔티티(존재하지 않으면 {@code null})
     */
    public Notice detailNotice(Long id) {
        Notice notice = noticeRepository.findById(id).orElse(null);
        if (notice != null) {
            notice.incrementNoticeCount();
            noticeRepository.save(notice);
            log.debug("[NoticeService] Notice ID={} 조회수 증가", id);
        }
        return notice;
    }
}
