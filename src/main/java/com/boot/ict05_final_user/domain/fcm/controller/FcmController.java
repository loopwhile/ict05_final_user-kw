package com.boot.ict05_final_user.domain.fcm.controller;

import com.boot.ict05_final_user.config.security.jwt.service.JwtService;
import com.boot.ict05_final_user.domain.fcm.dto.FcmRegisterTokenRequest;
import com.boot.ict05_final_user.domain.fcm.dto.FcmTestSendRequest;
import com.boot.ict05_final_user.domain.fcm.dto.StoreTopic;
import com.boot.ict05_final_user.domain.fcm.entity.AppType;
import com.boot.ict05_final_user.domain.fcm.entity.PlatformType;
import com.boot.ict05_final_user.domain.fcm.repository.FcmDeviceTokenRepository;
import com.boot.ict05_final_user.domain.fcm.service.FcmService;
import com.google.firebase.messaging.FirebaseMessagingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;

/**
 * FCM 관련 REST API 컨트롤러.
 *
 * <p>가맹점 앱에서 사용하는 FCM 토큰 등록, 해제, 토픽 구독/해제 및 테스트 발송 기능을 제공합니다.
 * 토큰은 회원 및 기기 단위로 관리되며, Firebase Admin SDK를 통해 실제 발송이 수행됩니다.</p>
 *
 * <ul>
 *     <li>토큰 등록/업서트 (POST /fcm/token)</li>
 *     <li>토큰 해제 (POST /fcm/token/revoke)</li>
 *     <li>토픽 구독/해제 (POST /fcm/topic/subscribe, /fcm/topic/unsubscribe)</li>
 *     <li>테스트 발송 (POST /fcm/send/test)</li>
 *     <li>HQ 공지 발송 (POST /fcm/send/hq-notice)</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@RestController
@RequestMapping("/fcm")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FCM", description = "Firebase Cloud Messaging 관련 API")
public class FcmController {

    private final FcmService fcmService;
    private final JwtService jwtService;
    private final FcmDeviceTokenRepository tokenRepo;

    @Value("${fcm.test.admin-only:false}")
    private boolean testAdminOnly;

    /**
     * JWT 또는 Principal에서 storeId, memberId를 추출하기 위한 내부 클래스.
     */
    private static class Ids {
        Long storeId;
        Long memberId;
    }

    /**
     * AuthenticationPrincipal 또는 JWT 토큰에서 storeId/memberId를 추출합니다.
     *
     * @param principal 인증 주체 객체
     * @param authHeader Authorization 헤더 (Bearer 토큰)
     * @return 추출된 {@link Ids} 객체
     */
    private Ids resolveIds(Object principal, String authHeader) {
        Ids ids = new Ids();
        try {
            if (principal != null) {
                var cls = principal.getClass();
                var mStore = cls.getMethod("getStoreId");
                var mMember = cls.getMethod("getMemberId");
                Object sid = mStore.invoke(principal);
                Object mid = mMember.invoke(principal);
                if (sid instanceof Number) ids.storeId = ((Number) sid).longValue();
                if (mid instanceof Number) ids.memberId = ((Number) mid).longValue();
            }
        } catch (Exception ignore) { }

        if ((ids.storeId == null || ids.memberId == null) && authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Long sid = com.boot.ict05_final_user.config.security.jwt.JWTUtil.getStoreId(token);
                Long mid = com.boot.ict05_final_user.config.security.jwt.JWTUtil.getMemberId(token);
                if (ids.storeId == null) ids.storeId = sid;
                if (ids.memberId == null) ids.memberId = mid;
            } catch (Exception e) {
                log.warn("[FCM] JWT parse failed: {}", e.getMessage());
            }
        }
        return ids;
    }

    /**
     * FCM 디바이스 토큰을 등록 또는 업데이트합니다.
     *
     * @param me 인증 사용자 정보
     * @param auth Authorization 헤더
     * @param req 토큰 등록 요청 DTO
     * @return 등록된 토큰의 ID 및 앱 타입
     */
    @Operation(summary = "FCM 토큰 업서트", description = "가맹점 사용자 단말기의 FCM 토큰을 등록하거나 갱신합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정상 등록 완료"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PostMapping("/token")
    @PreAuthorize("hasAnyRole('STORE','OWNER','STAFF','USER')")
    public Map<String, Object> upsertToken(
            @AuthenticationPrincipal Object me,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody FcmRegisterTokenRequest req
    ) {
        log.info("[FCM] upsert req token={}, platform={}, deviceId={}", req.token(), req.platform(), req.deviceId());
        Ids ids = resolveIds(me, auth);
        if (ids.storeId == null || ids.memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "NO_AUTH");
        }

        var row = fcmService.upsertStoreToken(ids.storeId, ids.memberId, req);
        return Map.of("status", "ok", "id", row.getFcmDeviceTokenId(), "appType", AppType.STORE);
    }

    /**
     * 등록된 FCM 토큰을 비활성화(revoke)합니다.
     *
     * <p>요청 본문에 token이 존재하면 해당 토큰을 비활성화하고,
     * 없을 경우 platform + deviceId 조합으로 비활성화합니다.</p>
     *
     * @param me 인증 사용자 정보
     * @param auth Authorization 헤더
     * @param req 토큰 정보
     * @return 비활성화된 레코드 개수
     */
    @Operation(summary = "FCM 토큰 해제", description = "등록된 FCM 토큰을 비활성화(revoke)합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정상 해제 완료"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/token/revoke")
    @PreAuthorize("hasAnyRole('STORE','OWNER','STAFF','USER')")
    public Map<String, Object> revokeToken(
            @AuthenticationPrincipal Object me,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody FcmRegisterTokenRequest req
    ) {
        Ids ids = resolveIds(me, auth);
        if (ids.memberId == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "NO_AUTH");

        int affected = 0;
        if (req.token() != null && !req.token().isBlank()) {
            tokenRepo.findByToken(req.token()).ifPresent(row -> {
                if (Objects.equals(row.getMemberIdFk(), ids.memberId)) {
                    row.setIsActive(false);
                }
            });
            affected = 1;
        } else {
            PlatformType platform = req.platform();
            String deviceId = req.deviceId();
            if (platform == null || deviceId == null || deviceId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "platform/deviceId required if token absent");
            }
            var rows = tokenRepo.findByAppTypeAndPlatformAndMemberIdFkAndDeviceIdAndIsActiveTrue(
                    AppType.STORE, platform, ids.memberId, deviceId
            );
            rows.forEach(r -> r.setIsActive(false));
            affected = rows.size();
        }
        return Map.of("status", "ok", "revoked", affected);
    }

    /**
     * 특정 FCM 토픽에 단말기를 구독시킵니다.
     *
     * @param token FCM 디바이스 토큰
     * @param topic 구독할 토픽명
     * @return 성공 여부
     * @throws FirebaseMessagingException Firebase 전송 예외
     */
    @Operation(summary = "토픽 구독", description = "지정된 FCM 토픽에 단말기를 구독시킵니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구독 성공"),
            @ApiResponse(responseCode = "400", description = "허용되지 않은 토픽명")
    })
    @PostMapping("/topic/subscribe")
    @PreAuthorize("hasAnyRole('STORE','OWNER','STAFF','USER')")
    public Map<String, Object> subscribe(
            @Parameter(description = "FCM 디바이스 토큰", example = "fcm_token_string") @RequestParam String token,
            @Parameter(description = "구독할 토픽명", example = "store-1") @RequestParam String topic
    ) throws FirebaseMessagingException {
        if (!StoreTopic.isAllowed(topic))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOPIC_NOT_ALLOWED");
        fcmService.subscribe(token, topic);
        return Map.of("status", "ok");
    }

    /**
     * 특정 FCM 토픽에서 단말기를 구독 해제합니다.
     *
     * @param token FCM 디바이스 토큰
     * @param topic 해제할 토픽명
     * @return 성공 여부
     * @throws FirebaseMessagingException Firebase 전송 예외
     */
    @Operation(summary = "토픽 구독 해제", description = "지정된 FCM 토픽에서 단말기의 구독을 해제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "해제 성공"),
            @ApiResponse(responseCode = "400", description = "허용되지 않은 토픽명")
    })
    @PostMapping("/topic/unsubscribe")
    @PreAuthorize("hasAnyRole('STORE','OWNER','STAFF','USER')")
    public Map<String, Object> unsubscribe(
            @Parameter(description = "FCM 디바이스 토큰") @RequestParam String token,
            @Parameter(description = "해제할 토픽명") @RequestParam String topic
    ) throws FirebaseMessagingException {
        if (!StoreTopic.isAllowed(topic))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOPIC_NOT_ALLOWED");
        fcmService.unsubscribe(token, topic);
        return Map.of("status", "ok");
    }

    /**
     * 테스트용 FCM 메시지를 발송합니다.
     *
     * <p>운영환경에서는 관리자 전용으로 제한될 수 있습니다.</p>
     *
     * @param me 인증 사용자 정보
     * @param authentication 인증 객체
     * @param req 테스트 발송 요청 DTO
     * @return 전송된 메시지 ID
     * @throws FirebaseMessagingException Firebase 전송 예외
     */
    @Operation(summary = "테스트 메시지 발송", description = "Firebase를 통해 테스트용 FCM 메시지를 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 전용 접근 차단")
    })
    @PostMapping("/send/test")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> sendTest(
            @AuthenticationPrincipal Object me,
            Authentication authentication,
            @RequestBody FcmTestSendRequest req
    ) throws FirebaseMessagingException {
        if (testAdminOnly) {
            boolean admin = authentication != null && authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.contains("ADMIN"));
            if (!admin)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN_ONLY");
        }
        String id = fcmService.sendTest(req);
        return Map.of("status", "ok", "messageId", id);
    }

    /**
     * 본사(HQ) 공지사항을 전체 가맹점으로 발송합니다.
     *
     * @param topic 공지 토픽명
     * @param title 공지 제목
     * @param body  공지 내용
     * @param link  클릭 시 이동할 링크
     * @return 전송된 메시지 ID
     * @throws FirebaseMessagingException Firebase 전송 예외
     */
    @Operation(summary = "HQ 공지 발송", description = "본사(HQ) 공지를 가맹점 토픽으로 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공"),
            @ApiResponse(responseCode = "400", description = "허용되지 않은 토픽명")
    })
    @PostMapping("/send/hq-notice")
    @PreAuthorize("hasRole('HQ') or hasRole('ADMIN')")
    public Map<String, Object> sendHqNotice(
            @Parameter(description = "공지 토픽명", example = "hq-notice") @RequestParam String topic,
            @Parameter(description = "공지 제목", example = "신규 프로모션 안내") @RequestParam String title,
            @Parameter(description = "공지 내용", example = "이번 주부터 새로운 메뉴 이벤트가 시작됩니다.") @RequestParam String body,
            @Parameter(description = "공지 클릭 시 이동할 링크", example = "/user/event") @RequestParam String link
    ) throws FirebaseMessagingException {
        if (!StoreTopic.isAllowed(topic))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOPIC_NOT_ALLOWED");
        String id = fcmService.sendHqNoticeToStores(topic, title, body, link);
        return Map.of("status", "ok", "messageId", id);
    }
}
