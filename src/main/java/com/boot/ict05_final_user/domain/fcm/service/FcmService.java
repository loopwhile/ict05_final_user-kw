package com.boot.ict05_final_user.domain.fcm.service;

import com.boot.ict05_final_user.domain.fcm.config.FcmProperties;
import com.boot.ict05_final_user.domain.fcm.dto.FcmRegisterTokenRequest;
import com.boot.ict05_final_user.domain.fcm.dto.FcmTestSendRequest;
import com.boot.ict05_final_user.domain.fcm.entity.AppType;
import com.boot.ict05_final_user.domain.fcm.entity.FcmDeviceToken;
import com.boot.ict05_final_user.domain.fcm.entity.FcmStoreSendLog;
import com.boot.ict05_final_user.domain.fcm.entity.PlatformType;
import com.boot.ict05_final_user.domain.fcm.repository.FcmDeviceTokenRepository;
import com.boot.ict05_final_user.domain.fcm.repository.FcmStoreSendLogRepository;
import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * FCM 토큰 관리, 토픽 구독 관리 및 메시지 발송을 담당하는 서비스 클래스.
 *
 * <p>본 서비스는 Firebase Admin SDK를 기반으로 하며, 주요 역할은 다음과 같습니다:</p>
 * <ul>
 *   <li>토큰 업서트 및 구독/해제 관리</li>
 *   <li>단일 또는 다중 대상 발송 (Topic, Token)</li>
 *   <li>발송 결과 및 예외 처리, 로그 기록</li>
 *   <li>FCM 장애 발생 시 토큰 비활성화 처리</li>
 * </ul>
 *
 * <p>해당 빈은 {@code fcm.enabled=true}일 때만 활성화됩니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
public class FcmService {

    private final FirebaseMessaging messaging;
    private final FcmDeviceTokenRepository tokenRepo;
    private final FcmProperties props;
    private final FcmStoreSendLogRepository storeLogRepo;

    /**
     * FCM 발송 로그 저장 시 메타 정보를 보관하는 내부 클래스.
     */
    @Getter
    @Builder
    public static class StoreLogMeta {
        @Builder.Default private AppType appType = AppType.STORE;
        @Builder.Default private String category = "GENERAL"; // NOTICE / STOCK_LOW / EXPIRE_SOON / TEST / GENERAL
        private Long storeId;
        private Long memberId;
        private String refType;
        private Long refId;
        private LocalDate refDate;
    }

    // ============================ 토큰 관리 ============================

    /**
     * 가맹점 단말의 FCM 토큰을 등록(업서트)합니다.
     *
     * @param storeId 매장 ID
     * @param memberId 회원 ID
     * @param req 토큰 등록 요청 DTO
     * @return 업서트된 {@link FcmDeviceToken} 엔티티
     */
    @Transactional
    public FcmDeviceToken upsertStoreToken(Long storeId, Long memberId, FcmRegisterTokenRequest req) {
        PlatformType platform = (req.platform() == null) ? PlatformType.WEB : req.platform();
        return tokenRepo.upsert(
                AppType.STORE,
                platform,
                storeId,
                memberId,
                req.deviceId(),
                req.token(),
                LocalDateTime.now()
        );
    }

    // ============================ 토픽 구독 관리 ============================

    /**
     * 단일 토큰을 지정된 토픽에 구독시킵니다.
     *
     * @param token 토큰 문자열
     * @param topic 구독할 토픽명
     * @throws FirebaseMessagingException FCM 오류 발생 시
     */
    public void subscribe(String token, String topic) throws FirebaseMessagingException {
        TopicManagementResponse res = messaging.subscribeToTopic(List.of(token), topic);
        log.info("[FCM] subscribe {} -> {} (success={}, fail={})",
                token, topic, res.getSuccessCount(), res.getFailureCount());
    }

    /**
     * 단일 토큰을 지정된 토픽에서 해제시킵니다.
     *
     * @param token 토큰 문자열
     * @param topic 해제할 토픽명
     * @throws FirebaseMessagingException FCM 오류 발생 시
     */
    public void unsubscribe(String token, String topic) throws FirebaseMessagingException {
        TopicManagementResponse res = messaging.unsubscribeFromTopic(List.of(token), topic);
        log.info("[FCM] unsubscribe {} -> {} (success={}, fail={})",
                token, topic, res.getSuccessCount(), res.getFailureCount());
    }

    /**
     * 다중 토큰을 지정된 토픽에 일괄 구독시킵니다.
     *
     * @param tokens 구독 대상 토큰 목록
     * @param topic 구독할 토픽명
     * @throws FirebaseMessagingException FCM 오류 발생 시
     */
    public void subscribeAll(List<String> tokens, String topic) throws FirebaseMessagingException {
        if (tokens == null || tokens.isEmpty()) return;
        TopicManagementResponse res = messaging.subscribeToTopic(tokens, topic);
        log.info("[FCM] subscribeAll {} -> {} (success={}, fail={})",
                tokens.size(), topic, res.getSuccessCount(), res.getFailureCount());
    }

    /**
     * 다중 토큰을 지정된 토픽에서 일괄 해제시킵니다.
     *
     * @param tokens 해제 대상 토큰 목록
     * @param topic 해제할 토픽명
     * @throws FirebaseMessagingException FCM 오류 발생 시
     */
    public void unsubscribeAll(List<String> tokens, String topic) throws FirebaseMessagingException {
        if (tokens == null || tokens.isEmpty()) return;
        TopicManagementResponse res = messaging.unsubscribeFromTopic(tokens, topic);
        log.info("[FCM] unsubscribeAll {} -> {} (success={}, fail={})",
                tokens.size(), topic, res.getSuccessCount(), res.getFailureCount());
    }

    // ============================ 메시지 발송 공통 로직 ============================

    /**
     * FCM 메시지를 실제 발송하고 발송 로그를 기록합니다.
     *
     * <p>WebPush 및 Android 알림 설정을 포함하며,
     * 링크, 아이콘, 배지 정보는 {@link FcmProperties}에서 불러옵니다.</p>
     *
     * @param tokenOrTopic 토큰 또는 토픽명
     * @param isTopic 토픽 여부
     * @param title 알림 제목
     * @param body 알림 본문
     * @param link 클릭 시 이동할 링크
     * @param dataExtra 부가 데이터 (null 가능)
     * @param meta 로그 메타데이터
     * @return 메시지 ID
     * @throws FirebaseMessagingException 발송 실패 시 예외
     */
    protected String sendCommonWithLog(String tokenOrTopic, boolean isTopic,
                                       String title, String body, String link,
                                       Map<String, String> dataExtra,
                                       StoreLogMeta meta) throws FirebaseMessagingException {
        final String defaultLink =
                (props != null && props.getWebpush() != null && props.getWebpush().getDefaultLink() != null)
                        ? props.getWebpush().getDefaultLink()
                        : "/";
        final String safeLink = (link == null || link.isBlank()) ? defaultLink : link;

        final String icon = (props != null && props.getWebpush() != null) ? props.getWebpush().getIcon() : null;
        final String badge = (props != null && props.getWebpush() != null) ? props.getWebpush().getBadge() : null;

        WebpushNotification.Builder webpushNoti = WebpushNotification.builder()
                .setTitle(title)
                .setBody(body);
        if (icon != null && !icon.isBlank()) webpushNoti.setIcon(icon);

        WebpushConfig.Builder webpush = WebpushConfig.builder()
                .setNotification(webpushNoti.build())
                .setFcmOptions(WebpushFcmOptions.withLink(safeLink))
                .putData("link", safeLink);
        if (badge != null && !badge.isBlank()) webpush.putData("badge", badge);

        AndroidNotification androidNoti = AndroidNotification.builder()
                .setChannelId("default")
                .setTitle(title)
                .setBody(body)
                .build();

        AndroidConfig.Builder android = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(androidNoti)
                .putData("link", safeLink);
        if (badge != null && !badge.isBlank()) android.putData("badge", badge);

        if (dataExtra != null) {
            dataExtra.forEach((k, v) -> {
                if (v != null) {
                    webpush.putData(k, v);
                    android.putData(k, v);
                }
            });
        }

        Message.Builder mb = Message.builder()
                .setWebpushConfig(webpush.build())
                .setAndroidConfig(android.build());
        if (isTopic) mb.setTopic(tokenOrTopic);
        else mb.setToken(tokenOrTopic);

        LocalDateTime now = LocalDateTime.now();
        String messageId = null;
        String errorMsg = null;

        try {
            messageId = messaging.send(mb.build());
            log.info("[FCM] send ok id={} target={} isTopic={}", messageId, tokenOrTopic, isTopic);
            return messageId;

        } catch (FirebaseMessagingException e) {
            MessagingErrorCode mec = e.getMessagingErrorCode();
            final String code = (mec != null ? mec.name() : "UNKNOWN");
            errorMsg = code + ":" + e.getMessage();

            log.warn("[FCM] send fail target={} isTopic={} code={} msg={}",
                    tokenOrTopic, isTopic, code, e.getMessage());

            if (!isTopic) handleTokenError(tokenOrTopic, e);
            throw e;

        } finally {
            if (meta != null) {
                try {
                    FcmStoreSendLog logRow = FcmStoreSendLog.builder()
                            .appType(meta.getAppType())
                            .category(meta.getCategory())
                            .storeIdFk(meta.getStoreId())
                            .memberIdFk(meta.getMemberId())
                            .topic(isTopic ? tokenOrTopic : null)
                            .token(isTopic ? null : tokenOrTopic)
                            .title(title)
                            .body(body)
                            .link(safeLink)
                            .refType(meta.getRefType())
                            .refId(meta.getRefId())
                            .refDate(meta.getRefDate())
                            .resultMessageId(messageId)
                            .resultError(errorMsg)
                            .sentAt(now)
                            .build();
                    storeLogRepo.save(logRow);
                } catch (Exception ex) {
                    log.warn("[FCM] store log insert fail: {}", ex.getMessage());
                }
            }
        }
    }

    // ============================ 고수준 발송 API ============================

    /** 테스트 발송 요청 DTO 기반 메시지 전송 */
    public String sendTest(FcmTestSendRequest req) throws FirebaseMessagingException {
        String link = "/";
        Map<String, String> extra = new HashMap<>();
        extra.put("type", "TEST");

        if (req.data() != null) {
            String maybeLink = req.data().get("link");
            if (maybeLink != null && !maybeLink.isBlank()) link = maybeLink;
            req.data().forEach((k, v) -> {
                if (v != null && !"link".equals(k)) extra.put(k, v);
            });
        }

        StoreLogMeta meta = StoreLogMeta.builder().category("TEST").build();

        return sendCommonWithLog(
                req.tokenOrTopic(),
                req.topic(),
                req.title(),
                req.body(),
                link,
                extra,
                meta
        );
    }

    /** HQ 공지 브로드캐스트 발송 (store-all 또는 store-{id}) */
    public String sendHqNoticeToStores(String topic, String title, String body, String link)
            throws FirebaseMessagingException {
        StoreLogMeta meta = StoreLogMeta.builder()
                .category("NOTICE")
                .refType("NOTICE_HQ")
                .build();

        return sendCommonWithLog(
                topic, true, title, body, link,
                Map.of("type", "HQ_NOTICE"), meta
        );
    }

    /** 재고 부족 알림 발송 */
    public String sendInventoryLow(long storeId, String title, String body, String link)
            throws FirebaseMessagingException {
        StoreLogMeta meta = StoreLogMeta.builder()
                .category("STOCK_LOW")
                .storeId(storeId)
                .refType("INVENTORY")
                .build();

        return sendCommonWithLog(
                "inv-low-" + storeId, true, title, body, link,
                Map.of("type", "INV_LOW", "storeId", String.valueOf(storeId)), meta
        );
    }

    /** 유통기한 임박 알림 발송 */
    public String sendExpireSoon(long storeId, LocalDate baseDate, String title, String body, String link)
            throws FirebaseMessagingException {
        StoreLogMeta meta = StoreLogMeta.builder()
                .category("EXPIRE_SOON")
                .storeId(storeId)
                .refType("INVENTORY")
                .refDate(baseDate)
                .build();

        return sendCommonWithLog(
                "expire-soon-" + storeId, true, title, body, link,
                Map.of("type", "EXP_SOON", "storeId", String.valueOf(storeId)), meta
        );
    }

    // ============================ 내부 유틸 ============================

    private void handleTokenError(String token, FirebaseMessagingException e) {
        MessagingErrorCode mcode = e.getMessagingErrorCode();
        if (mcode != null) {
            if (mcode == MessagingErrorCode.UNREGISTERED || mcode == MessagingErrorCode.INVALID_ARGUMENT) {
                deactivateToken(token, "messaging:" + mcode.name());
                return;
            }
            log.debug("[FCM] non-deactivation messaging error: {} token={} msg={}",
                    mcode, token, e.getMessage());
            return;
        }

        ErrorCode gcode = e.getErrorCode();
        if (gcode != null) {
            if (gcode == ErrorCode.INVALID_ARGUMENT || gcode == ErrorCode.NOT_FOUND) {
                deactivateToken(token, "generic:" + gcode.name());
                return;
            }
        }

        String msg = e.getMessage();
        if (msg != null && (
                msg.contains("registration-token-not-registered")
                        || msg.contains("invalid-registration-token")
                        || msg.contains("requested entity was not found")
        )) {
            deactivateToken(token, "message-match");
        } else {
            log.debug("[FCM] non-deactivation error token={} errCode={} msg={}",
                    token, (gcode != null ? gcode.name() : null), e.getMessage());
        }
    }

    private void deactivateToken(String token, String reason) {
        tokenRepo.findByToken(token).ifPresent(row -> row.setIsActive(false));
        log.info("[FCM] token deactivated (reason={}) token={}", reason, token);
    }
}
