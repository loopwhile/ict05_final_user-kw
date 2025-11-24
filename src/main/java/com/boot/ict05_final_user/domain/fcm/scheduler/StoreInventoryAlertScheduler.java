package com.boot.ict05_final_user.domain.fcm.scheduler;

import com.boot.ict05_final_user.domain.fcm.config.StoreFcmScannerProperties;
import com.boot.ict05_final_user.domain.fcm.service.InventoryAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 가맹점 재고 관련 FCM 알림 자동 발송 스케줄러.
 *
 * <p>재고 부족 및 유통기한 임박 항목을 주기적으로 스캔하고,
 * {@link InventoryAlertService}를 통해 각 매장 토픽에 알림을 발송합니다.</p>
 *
 * <ul>
 *   <li>조건: {@code fcm.scanner.enabled=true} 일 때만 활성화</li>
 *   <li>설정: 크론, 임계치, 일수, 발송 상한은 {@link StoreFcmScannerProperties}에서 제어</li>
 *   <li>스케줄링: Spring {@code @Scheduled} Cron 표현식 사용</li>
 * </ul>
 *
 * <p>FCM 스캐너 프로퍼티는 환경별로 설정할 수 있으며,
 * 운영 환경에서는 트래픽 및 알림 정책에 따라 주기를 조정할 수 있습니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "fcm.scanner.enabled", havingValue = "true")
public class StoreInventoryAlertScheduler {

    private final InventoryAlertService invService;
    private final StoreFcmScannerProperties props;

    /**
     * 재고 부족 스캔 및 FCM 발송 작업.
     *
     * <p>기본 실행 주기: 20분마다 (0 0/20 * * * *)</p>
     * <p>설정 키:
     * <ul>
     *   <li>{@code fcm.scanner.low-cron}</li>
     *   <li>{@code fcm.scanner.low-threshold}</li>
     *   <li>{@code fcm.scanner.stock-low-max}</li>
     * </ul>
     * </p>
     */
    @Scheduled(cron = "${fcm.scanner.low-cron:0 0/20 * * * *}")
    public void lowStockJob() {
        final int threshold = props.getLowThreshold();
        final int cap = props.getStockLowMax();

        int sent = invService.scanAndNotifyLowStock(threshold, cap);
        log.info("[Scheduler][INV_LOW] threshold={} cap={} sent={}", threshold, cap, sent);
    }

    /**
     * 유통기한 임박 스캔 및 FCM 발송 작업.
     *
     * <p>기본 실행 주기: 매일 09:10 (0 10 9 * * *)</p>
     * <p>설정 키:
     * <ul>
     *   <li>{@code fcm.scanner.expire-cron}</li>
     *   <li>{@code fcm.scanner.expire-soon-days-default}</li>
     *   <li>{@code fcm.scanner.expire-soon-max}</li>
     * </ul>
     * </p>
     */
    @Scheduled(cron = "${fcm.scanner.expire-cron:0 10 9 * * *}")
    public void expireSoonJob() {
        final LocalDate baseDate = LocalDate.now();
        final int days = props.getExpireSoonDaysDefault();
        final int cap = props.getExpireSoonMax();

        int sent = invService.scanAndNotifyExpireSoon(baseDate, days, cap);
        log.info("[Scheduler][EXP_SOON] baseDate={} days={} cap={} sent={}", baseDate, days, cap, sent);
    }
}
