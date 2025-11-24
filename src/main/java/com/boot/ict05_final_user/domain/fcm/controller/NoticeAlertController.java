package com.boot.ict05_final_user.domain.fcm.controller;

import com.boot.ict05_final_user.domain.fcm.service.NoticeAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 본사(HQ) 공지사항 관련 FCM 알림 트리거 컨트롤러.
 *
 * <p>이 컨트롤러는 HQ 서버에서 USER 서버로 내부 호출되어,
 * 공지 등록 또는 수정 시 가맹점 대상으로 FCM 공지 알림을 발송합니다.
 * 내부 서비스 간 호출을 위한 용도로 사용되며, 인증/인가를 요구하지 않습니다.</p>
 *
 * <ul>
 *     <li>공지 등록 시 알림 발송 (POST /fcm/notice/created/{noticeId})</li>
 *     <li>공지 수정 시 알림 발송 (POST /fcm/notice/updated/{noticeId})</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@RestController
@RequestMapping("/fcm/notice")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FCM Notice Alert", description = "HQ 공지사항 등록/수정 시 FCM 알림 트리거 API")
public class NoticeAlertController {

    private final NoticeAlertService noticeAlertService;

    /**
     * HQ 서버에서 공지 등록 시 호출되는 내부 트리거.
     *
     * <p>가맹점 사용자들에게 신규 공지 등록 FCM 알림을 발송합니다.</p>
     * <p>서버 간 내부 호출로 사용되며, 인증/인가 절차는 적용되지 않습니다.</p>
     *
     * @param noticeId 공지사항 ID
     * @return 발송된 알림 수 및 상태 정보
     */
    @Operation(
            summary = "공지 등록 시 FCM 알림 발송",
            description = "HQ 서버에서 신규 공지가 등록될 때, 모든 가맹점 사용자에게 FCM 공지 알림을 발송합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공"),
            @ApiResponse(responseCode = "500", description = "FCM 발송 중 오류", content = @Content)
    })
    @PostMapping("/created/{noticeId}")
    public Map<String, Object> created(
            @Parameter(description = "공지사항 ID", example = "101") @PathVariable Long noticeId
    ) {
        int sent = noticeAlertService.sendNoticeCreatedBroadcast(noticeId);
        return Map.of(
                "status", "ok",
                "mode", "created",
                "noticeId", noticeId,
                "sent", sent
        );
    }

    /**
     * HQ 서버에서 공지 수정 시 호출되는 내부 트리거.
     *
     * <p>가맹점 사용자들에게 공지 수정 FCM 알림을 발송합니다.</p>
     * <p>서버 간 내부 호출로 사용되며, 인증/인가 절차는 적용되지 않습니다.</p>
     *
     * @param noticeId 공지사항 ID
     * @return 발송된 알림 수 및 상태 정보
     */
    @Operation(
            summary = "공지 수정 시 FCM 알림 발송",
            description = "HQ 서버에서 공지사항이 수정될 때, 모든 가맹점 사용자에게 수정 알림을 발송합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공"),
            @ApiResponse(responseCode = "500", description = "FCM 발송 중 오류", content = @Content)
    })
    @PostMapping("/updated/{noticeId}")
    public Map<String, Object> updated(
            @Parameter(description = "공지사항 ID", example = "101") @PathVariable Long noticeId
    ) {
        int sent = noticeAlertService.sendNoticeUpdatedBroadcast(noticeId);
        return Map.of(
                "status", "ok",
                "mode", "updated",
                "noticeId", noticeId,
                "sent", sent
        );
    }
}
