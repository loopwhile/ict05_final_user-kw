package com.boot.ict05_final_user.domain.notice.repository;

import com.boot.ict05_final_user.domain.notice.dto.NoticeListDTO;
import com.boot.ict05_final_user.domain.notice.dto.NoticeSearchDTO;
import com.boot.ict05_final_user.domain.notice.entity.NoticePriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 공지사항 커스텀 리포지토리 인터페이스.
 *
 * <p>QueryDSL을 기반으로 하는 동적 검색, 중요도 카운트 등의 고급 조회 기능을 정의합니다.</p>
 *
 * <ul>
 *     <li>{@link #listNotice(NoticeSearchDTO, Pageable)} : 검색 조건 기반 공지사항 목록 조회</li>
 *     <li>{@link #countNotice(NoticeSearchDTO)} : 조건 기반 총 개수 조회</li>
 *     <li>{@link #countByPriority(NoticePriority)} : 중요도별 개수 조회</li>
 * </ul>
 *
 */
public interface NoticeRepositoryCustom {

    /**
     * 검색 조건 및 페이지 정보를 기반으로 공지사항 목록을 조회한다.
     *
     * @param noticeSearchDTO 검색 조건 DTO
     * @param pageable 페이지 요청 정보
     * @return 공지사항 목록 DTO 페이지 객체
     */
    Page<NoticeListDTO> listNotice(NoticeSearchDTO noticeSearchDTO, Pageable pageable);

    /**
     * 검색 조건 기반의 전체 공지 개수를 조회한다.
     *
     * @param noticeSearchDTO 검색 조건 DTO
     * @return 조건에 해당하는 전체 공지 개수
     */
    long countNotice(NoticeSearchDTO noticeSearchDTO);

    /**
     * 공지사항의 우선순위 기준으로 개수를 조회한다.
     *
     * @param priority 공지 우선순위
     * @return 해당 우선순위의 공지 개수
     */
    long countByPriority(NoticePriority priority);
}
