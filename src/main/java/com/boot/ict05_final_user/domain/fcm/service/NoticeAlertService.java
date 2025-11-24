package com.boot.ict05_final_user.domain.fcm.service;

import com.boot.ict05_final_user.domain.fcm.dto.StoreTopic;
import com.boot.ict05_final_user.domain.fcm.entity.AppType;
import com.boot.ict05_final_user.domain.fcm.entity.FcmStoreSendLog;
import com.boot.ict05_final_user.domain.fcm.repository.FcmStoreSendLogRepository;
import com.boot.ict05_final_user.domain.notice.entity.Notice;
import com.boot.ict05_final_user.domain.notice.repository.NoticeRepository;
import com.boot.ict05_final_user.domain.store.entity.Store;
import com.boot.ict05_final_user.domain.store.repository.StoreRepository;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 공지사항 등록/수정 시 FCM 브로드캐스트를 수행하는 서비스.
 *
 * <p>본 서비스는 HQ 서버에서 공지 이벤트 발생 시 호출되어,
 * 가맹점 단위 토픽({@code store-{storeId}})으로 공지 알림을 전송합니다.</p>
 *
 * <ul>
 *   <li>공지 등록/수정 시 브로드캐스트 발송</li>
 *   <li>개별 매장 단위 발송 기능 제공</li>
 *   <li>FCM 실패 로그 및 전송 이력 기록</li>
 * </ul>
 *
 * <p>각 알림은 {@link FcmService#sendHqNoticeToStores(String, String, String, String)}를 통해 전송됩니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeAlertService {

    private final NoticeRepository noticeRepository;
    private final FcmService fcmService;
    private final FcmStoreSendLogRepository storeLogRepo;
    private final StoreRepository storeRepository;

    // =========================
    // 1. HQ → USER 브로드캐스트
    // =========================

    /**
     * 새 공지 등록 시 모든 가맹점에 알림을 발송합니다.
     *
     * <p>HQ 서버에서 {@code /fcm/notice/created/{noticeId}} 엔드포인트를 호출할 때 트리거됩니다.</p>
     *
     * @param noticeId 공지 ID
     * @return 성공적으로 발송된 매장 수
     */
    @Transactional
    public int sendNoticeCreatedBroadcast(Long noticeId) {
        return broadcastToAllStores(noticeId, "NOTICE_CREATED", "[공지] 새 공지 등록");
    }

    /**
     * 공지 수정 시 모든 가맹점에 알림을 발송합니다.
     *
     * <p>HQ 서버에서 {@code /fcm/notice/updated/{noticeId}} 엔드포인트를 호출할 때 트리거됩니다.</p>
     *
     * @param noticeId 공지 ID
     * @return 성공적으로 발송된 매장 수
     */
    @Transactional
    public int sendNoticeUpdatedBroadcast(Long noticeId) {
        return broadcastToAllStores(noticeId, "NOTICE_UPDATED", "[공지] 공지 수정");
    }

    /**
     * 전체 가맹점을 대상으로 공지 알림을 브로드캐스트합니다.
     *
     * <p>운영 중인 모든 매장을 대상으로 순차 발송하며,
     * 개별 매장에서 오류 발생 시 로그만 남기고 다음 매장으로 진행합니다.</p>
     *
     * @param noticeId 공지 ID
     * @param category 알림 카테고리 (예: NOTICE_CREATED, NOTICE_UPDATED)
     * @param prefixTitle 공지 제목 접두어
     * @return 성공적으로 발송된 매장 수
     */
    @Transactional
    protected int broadcastToAllStores(Long noticeId, String category, String prefixTitle) {
        Notice notice = noticeRepository.findById(noticeId).orElse(null);
        if (notice == null) {
            log.warn("[NoticeAlert] notice not found id={}", noticeId);
            return 0;
        }

        List<Store> stores = storeRepository.findAll();
        int totalSent = 0;

        for (Store store : stores) {
            Long storeId = store.getId(); // 필요 시 getStoreId()로 변경
            if (storeId == null) continue;
            int sent = doSend(storeId, null, noticeId, category, prefixTitle, notice);
            totalSent += sent;
        }

        log.info("[NoticeAlert] broadcast done noticeId={} category={} totalSent={}",
                noticeId, category, totalSent);
        return totalSent;
    }

    // =========================
    // 2. 개별 매장 단위 발송
    // =========================

    /**
     * 특정 매장에 새 공지 등록 알림을 발송합니다.
     *
     * @param storeId 매장 ID
     * @param memberId 회원 ID (선택적)
     * @param noticeId 공지 ID
     * @return 발송 성공 시 1, 실패 시 0
     */
    @Transactional
    public int sendNoticeCreated(Long storeId, Long memberId, Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId).orElse(null);
        return doSend(storeId, memberId, noticeId, "NOTICE_CREATED", "[공지] 새 공지 등록", notice);
    }

    /**
     * 특정 매장에 공지 수정 알림을 발송합니다.
     *
     * @param storeId 매장 ID
     * @param memberId 회원 ID (선택적)
     * @param noticeId 공지 ID
     * @return 발송 성공 시 1, 실패 시 0
     */
    @Transactional
    public int sendNoticeUpdated(Long storeId, Long memberId, Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId).orElse(null);
        return doSend(storeId, memberId, noticeId, "NOTICE_UPDATED", "[공지] 공지 수정", notice);
    }

    // =========================
    // 3. 공통 발송 처리 로직
    // =========================

    /**
     * 단일 매장 대상 공지 알림 발송을 수행합니다.
     *
     * <p>공지 본문과 제목을 정리한 뒤, {@link FcmService#sendHqNoticeToStores(String, String, String, String)}
     * 를 통해 {@code store-{storeId}} 토픽으로 발송합니다.</p>
     *
     * <p>발송 결과는 {@code fcm_store_send_log} 테이블에 기록됩니다.</p>
     *
     * @param storeId 매장 ID
     * @param memberId 회원 ID (선택적)
     * @param noticeId 공지 ID
     * @param category 알림 카테고리
     * @param prefixTitle 제목 접두어
     * @param noticeOrNull 공지 엔티티 (null 가능)
     * @return 발송 성공 시 1, 실패 시 0
     */
    private int doSend(Long storeId,
                       Long memberId,
                       Long noticeId,
                       String category,
                       String prefixTitle,
                       Notice noticeOrNull) {

        if (storeId == null) {
            log.warn("[NoticeAlert] storeId is null, skip send (noticeId={})", noticeId);
            return 0;
        }

        Notice notice = noticeOrNull != null ? noticeOrNull : noticeRepository.findById(noticeId).orElse(null);
        if (notice == null) {
            log.warn("[NoticeAlert] notice not found id={}", noticeId);
            return 0;
        }

        String title = prefixTitle;
        if (notice.getTitle() != null && !notice.getTitle().isBlank()) {
            title = prefixTitle + " - " + notice.getTitle();
        }

        String body = notice.getBody();
        if (body == null) body = "";
        if (body.length() > 80) body = body.substring(0, 77) + "...";

        String link = "/notice/list";
        String topic = StoreTopic.store(storeId);

        String messageId = null;
        String error = null;

        try {
            messageId = fcmService.sendHqNoticeToStores(topic, title, body, link);
        } catch (FirebaseMessagingException e) {
            error = e.getMessage();
            log.warn("[NoticeAlert] send fail storeId={} noticeId={}", storeId, noticeId, e);
        }

        FcmStoreSendLog logRow = FcmStoreSendLog.builder()
                .appType(AppType.STORE)
                .category("NOTICE")
                .storeIdFk(storeId)
                .memberIdFk(memberId)
                .topic(topic)
                .title(title)
                .body(body)
                .link(link)
                .refType("NOTICE")
                .refId(noticeId)
                .sentAt(LocalDateTime.now())
                .resultMessageId(messageId)
                .resultError(error)
                .build();

        storeLogRepo.save(logRow);

        return (messageId != null ? 1 : 0);
    }
}
