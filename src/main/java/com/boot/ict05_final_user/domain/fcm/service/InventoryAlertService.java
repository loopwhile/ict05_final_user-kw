package com.boot.ict05_final_user.domain.fcm.service;

import com.boot.ict05_final_user.domain.fcm.repository.InventoryAlertQueryRepository;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 재고 부족 및 유통기한 임박 항목을 스캔하고
 * 해당 매장 토픽에 FCM 알림을 발송하는 서비스.
 *
 * <p>이 서비스는 주기적 또는 수동으로 호출되어,
 * {@code inv-low-{storeId}}, {@code expire-soon-{storeId}} 등의 토픽으로
 * 매장별 알림을 전송합니다.</p>
 *
 * <ul>
 *   <li>조회는 {@link InventoryAlertQueryRepository}에서 QueryDSL로 수행</li>
 *   <li>매장 단위 FCM 발송은 {@link FcmService}를 통해 처리</li>
 *   <li>개별 매장 발송 실패 시 예외를 로깅하고 다음 매장으로 계속 진행</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryAlertService {

    private final InventoryAlertQueryRepository inventoryRepo;
    private final FcmService fcmService;

    /**
     * 재고 부족 상태를 스캔하여 해당 매장에 FCM 알림을 발송합니다.
     *
     * <p>재고 수량이 {@code threshold} 미만인 매장을 조회하여
     * {@code inv-low-{storeId}} 토픽으로 공지를 발송합니다.</p>
     *
     * @param threshold 임계 수량 (1 이상)
     * @return 성공적으로 발송된 매장 수
     */
    @Transactional(readOnly = true)
    public int scanAndNotifyLowStock(int threshold) {
        if (threshold <= 0) {
            log.warn("[FCM][INV_LOW] invalid threshold={}, force set to 1", threshold);
            threshold = 1;
        }

        List<Long> storeList = inventoryRepo.findStoresWithLowStock(threshold);
        Set<Long> stores = new LinkedHashSet<>(storeList);

        int success = 0;
        for (Long storeId : stores) {
            try {
                fcmService.sendInventoryLow(
                        storeId,
                        "[재고부족] 확인 필요",
                        "일부 재료의 재고가 임계치 미만입니다.",
                        "/user/inventory/low"
                );
                success++;
            } catch (FirebaseMessagingException e) {
                log.warn("[FCM][INV_LOW] send fail storeId={} code={}", storeId, e.getErrorCode(), e);
            } catch (RuntimeException e) {
                log.warn("[FCM][INV_LOW] send fail storeId={} err={}", storeId, e.getMessage(), e);
            }
        }

        log.info("[FCM][INV_LOW] threshold={} target={} success={}", threshold, stores.size(), success);
        return success;
    }

    /**
     * 유통기한 임박 상태를 스캔하여 해당 매장에 FCM 알림을 발송합니다.
     *
     * <p>기준일 {@code today}를 기준으로 {@code days} 일 이내에 만료되는 자재가 있는
     * 매장을 조회하여 {@code expire-soon-{storeId}} 토픽으로 발송합니다.</p>
     *
     * @param today 기준일 (null 시 현재 일자)
     * @param days 오늘로부터 며칠 후까지 조회 (0 이상)
     * @return 성공적으로 발송된 매장 수
     */
    @Transactional(readOnly = true)
    public int scanAndNotifyExpireSoon(LocalDate today, int days) {
        if (today == null) today = LocalDate.now();
        if (days < 0) {
            log.warn("[FCM][EXP_SOON] invalid days={}, force set to 0", days);
            days = 0;
        }

        List<Long> storeList = inventoryRepo.findStoresWithExpireSoon(today, days);
        Set<Long> stores = new LinkedHashSet<>(storeList);

        int success = 0;
        for (Long storeId : stores) {
            try {
                fcmService.sendExpireSoon(
                        storeId,
                        today,
                        "[유통임박] 확인 필요",
                        "일부 재료의 유통기한이 임박했습니다.",
                        "/user/inventory/expire"
                );
                success++;
            } catch (FirebaseMessagingException e) {
                log.warn("[FCM][EXP_SOON] send fail storeId={} code={}", storeId, e.getErrorCode(), e);
            } catch (RuntimeException e) {
                log.warn("[FCM][EXP_SOON] send fail storeId={} err={}", storeId, e.getMessage(), e);
            }
        }

        log.info("[FCM][EXP_SOON] baseDate={} days={} target={} success={}",
                today, days, stores.size(), success);
        return success;
    }

    /**
     * 재고 부족 스캔 + 발송 (상한 제한 포함).
     *
     * <p>{@code maxTargets}를 지정하면 처리할 최대 매장 수를 제한합니다.</p>
     *
     * @param threshold 임계 수량 (1 이상)
     * @param maxTargets 최대 발송 매장 수 (0 이하는 무제한)
     * @return 성공적으로 발송된 매장 수
     */
    @Transactional(readOnly = true)
    public int scanAndNotifyLowStock(int threshold, int maxTargets) {
        if (threshold <= 0) threshold = 1;

        List<Long> list = inventoryRepo.findStoresWithLowStock(threshold);
        if (maxTargets > 0 && list.size() > maxTargets) {
            list = list.subList(0, maxTargets);
        }

        int success = 0;
        for (Long storeId : new LinkedHashSet<>(list)) {
            try {
                fcmService.sendInventoryLow(
                        storeId,
                        "[재고부족] 확인 필요",
                        "일부 재료의 재고가 임계치 미만입니다.",
                        "/user/inventory/low"
                );
                success++;
            } catch (Exception e) {
                log.warn("[FCM][INV_LOW] send fail storeId={} err={}", storeId, e.getMessage(), e);
            }
        }

        log.info("[FCM][INV_LOW] threshold={} capped={} success={}", threshold, list.size(), success);
        return success;
    }

    /**
     * 유통기한 임박 스캔 + 발송 (상한 제한 포함).
     *
     * @param today 기준일 (null 시 현재 일자)
     * @param days 오늘로부터 며칠 후까지 조회
     * @param maxTargets 최대 발송 매장 수 (0 이하는 무제한)
     * @return 성공적으로 발송된 매장 수
     */
    @Transactional(readOnly = true)
    public int scanAndNotifyExpireSoon(LocalDate today, int days, int maxTargets) {
        if (today == null) today = LocalDate.now();
        if (days < 0) days = 0;

        List<Long> list = inventoryRepo.findStoresWithExpireSoon(today, days);
        if (maxTargets > 0 && list.size() > maxTargets) {
            list = list.subList(0, maxTargets);
        }

        int success = 0;
        for (Long storeId : new LinkedHashSet<>(list)) {
            try {
                fcmService.sendExpireSoon(
                        storeId,
                        today,
                        "[유통임박] 확인 필요",
                        "일부 재료의 유통기한이 임박했습니다.",
                        "/user/inventory/expire"
                );
                success++;
            } catch (Exception e) {
                log.warn("[FCM][EXP_SOON] send fail storeId={} err={}", storeId, e.getMessage(), e);
            }
        }

        log.info("[FCM][EXP_SOON] baseDate={} days={} capped={} success={}",
                today, days, list.size(), success);
        return success;
    }
}
