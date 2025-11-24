package com.boot.ict05_final_user.domain.fcm.scheduler;

import com.boot.ict05_final_user.domain.fcm.repository.FcmDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * FCM 기기 토큰 정리 스케줄러.
 *
 * <p>활성화되지 않은 오래된 FCM 토큰을 주기적으로 비활성화하는 역할을 수행합니다.
 * {@code fcm.enabled=true} 일 때만 동작하며, 크론 및 비활성 기간은
 * {@code application.properties}에서 설정할 수 있습니다.</p>
 *
 * <ul>
 *   <li>기본 실행 주기: 매일 03:00</li>
 *   <li>비활성 기준: 최근 {@code daysInactive} 일 동안 갱신되지 않은 토큰</li>
 *   <li>처리 대상: {@code updatedAt < cutoff} 인 토큰 전체</li>
 * </ul>
 *
 * <p>실행 시 {@link FcmDeviceTokenRepository#deactivateAllByUpdatedAtBefore(LocalDateTime)}
 * 를 호출하여 토큰 상태를 일괄 비활성화합니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
public class FcmTokenCleanupScheduler {

	private final FcmDeviceTokenRepository tokenRepo;

	@Value("${fcm.cleanup.cron:0 0 3 * * *}")
	private String cron; // 참고용 로깅 필드

	@Value("${fcm.cleanup.days-inactive:90}")
	private int daysInactive;

	/**
	 * 비활성 FCM 토큰 정리 작업을 수행합니다.
	 *
	 * <p>최근 {@code daysInactive} 일간 갱신 이력이 없는 토큰을 비활성화하며,
	 * 기본 스케줄은 매일 새벽 3시에 실행됩니다.</p>
	 */
	@Transactional
	@Scheduled(cron = "${fcm.cleanup.cron:0 0 3 * * *}")
	public void cleanup() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(daysInactive);
		int deactivated = tokenRepo.deactivateAllByUpdatedAtBefore(cutoff);
		if (deactivated > 0) {
			log.info("[FCM] token cleanup by updatedAt cutoff={} deactivated={}", cutoff, deactivated);
		}
	}
}
