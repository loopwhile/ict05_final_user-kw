package com.boot.ict05_final_user.domain.fcm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 가맹점 재고 관련 FCM 알림 스캐너 설정 클래스.
 *
 * <p>이 클래스는 {@code fcm.scanner} prefix를 가진 환경설정 값을 바인딩하며,
 * 가맹점 재고 부족 및 유통기한 임박 알림 스캐너의 스케줄러 동작 주기,
 * 임계값, 안전장치 등의 정책을 정의합니다.</p>
 *
 * <h3>예시 (application.yml)</h3>
 * <pre>
 * fcm:
 *   scanner:
 *     enabled: true
 *     cron: 0 0/30 * * * *
 *     stock-low-max: 100
 *     expire-soon-max: 100
 *     expire-soon-days-default: 3
 *     low-threshold: 1
 * </pre>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Component
@ConfigurationProperties(prefix = "fcm.scanner")
@Getter
@Setter
public class StoreFcmScannerProperties {

	/**
	 * 스캐너 스케줄러 활성화 여부.
	 * <p>{@code true}로 설정 시, 가맹점 재고 및 유통기한 검사 스케줄러가 주기적으로 실행됩니다.</p>
	 */
	private boolean enabled = false;

	/**
	 * 스케줄 실행 주기 (Cron 표현식).
	 * <p>예: {@code 0 0/30 * * * *} → 30분 간격으로 실행.</p>
	 */
	private String cron = "0 0/30 * * * *";

	/**
	 * 스캐너 실행 시 처리할 최대 행 수(재고 부족 항목).
	 * <p>안정성을 위한 안전장치로, 처리 대상이 과도할 경우 일부만 알림 처리됩니다.</p>
	 */
	private int stockLowMax = 100;

	/**
	 * 스캐너 실행 시 처리할 최대 행 수(유통기한 임박 항목).
	 */
	private int expireSoonMax = 100;

	/**
	 * 유통기한 임박 판단 기준 (일 단위).
	 * <p>기본값: 3일 이내 만료 시 임박으로 간주합니다.</p>
	 */
	private int expireSoonDaysDefault = 3;

	/**
	 * 재고 부족 판단 임계 수량.
	 * <p>예: {@code 1} → 재고가 1 이하일 경우 부족으로 간주.</p>
	 */
	private int lowThreshold = 1;
}
