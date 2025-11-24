package com.boot.ict05_final_user.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공지사항 집계 데이터 DTO.
 *
 * <p>공지사항 전체 개수, 중요/긴급 공지 개수 등을 포함합니다.
 * 현재 unreadCount 필드는 향후 알림 연동을 위한 예약 필드입니다.</p>
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공지사항 집계 데이터 DTO")
public class NoticeCountDTO {

    /** 전체 공지사항 수 */
    @Schema(description = "전체 공지사항 수")
    private long totalCount;

    /** 긴급 공지 수 */
    @Schema(description = "긴급 공지 수")
    private long urgentCount;

    /** 중요 공지 수 */
    @Schema(description = "중요 공지 수")
    private long importantCount;

    /** 미열람 공지 수 (현재 로직상 0으로 유지) */
    @Schema(description = "미열람 공지 수 (현재는 항상 0)")
    private long unreadCount;
}
