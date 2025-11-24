package com.boot.ict05_final_user.domain.fcm.controller;

import com.boot.ict05_final_user.domain.fcm.service.InventoryAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * FCM 재고 관련 수동 알림 트리거 컨트롤러.
 *
 * <p>본 컨트롤러는 운영자가 수동으로 재고 부족 및 유통기한 임박 알림 스캔을 수행할 수 있도록 합니다.
 * 주로 테스트 및 긴급 대응 상황에서 사용됩니다.</p>
 *
 * <ul>
 *     <li>재고 부족 스캔 (POST /fcm/inventory/scan/low)</li>
 *     <li>유통기한 임박 스캔 (POST /fcm/inventory/scan/expire)</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@RestController
@RequestMapping("/fcm/inventory")
@RequiredArgsConstructor
@Tag(name = "FCM Inventory Alert", description = "FCM 재고 알림 수동 트리거 API")
public class InventoryAlertController {

    private final InventoryAlertService invService;

    /**
     * 재고 부족 항목을 수동으로 스캔하고, 부족 상태인 항목에 대해 FCM 알림을 발송합니다.
     *
     * @param threshold 재고 부족 임계값 (기본값: 3)
     * @return 발송된 알림 수 및 기준 임계값
     */
    @Operation(
            summary = "재고 부족 항목 수동 스캔",
            description = "지정된 임계값 이하의 재고를 가진 항목을 스캔하고 FCM 알림을 발송합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정상 처리 완료"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
    })
    @PostMapping("/scan/low")
    @PreAuthorize("hasAnyRole('HQ','ADMIN')")
    public Map<String, Object> scanLow(
            @Parameter(description = "재고 부족 임계값", example = "3")
            @RequestParam(defaultValue = "3") int threshold
    ) {
        int sent = invService.scanAndNotifyLowStock(threshold);
        return Map.of("status", "ok", "threshold", threshold, "sent", sent);
    }

    /**
     * 유통기한 임박 항목을 수동으로 스캔하고, 해당 항목에 대해 FCM 알림을 발송합니다.
     *
     * @param base 기준 날짜 (기준일을 포함해 지정된 일수 이내 만료 항목을 스캔)
     * @param days 임박 판단 기준 일수 (기본값: 3일)
     * @return 발송된 알림 수 및 기준 정보
     */
    @Operation(
            summary = "유통기한 임박 항목 수동 스캔",
            description = "기준일을 기준으로 지정된 일수 이내에 만료되는 재고 항목을 스캔하고 FCM 알림을 발송합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정상 처리 완료"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
    })
    @PostMapping("/scan/expire")
    @PreAuthorize("hasAnyRole('HQ','ADMIN')")
    public Map<String, Object> scanExpire(
            @Parameter(description = "기준 날짜 (yyyy-MM-dd 형식)", example = "2025-11-20")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate base,
            @Parameter(description = "임박 판단 기준 일수", example = "3")
            @RequestParam(defaultValue = "3") int days
    ) {
        int sent = invService.scanAndNotifyExpireSoon(base, days);
        return Map.of("status", "ok", "base", base.toString(), "days", days, "sent", sent);
    }
}
