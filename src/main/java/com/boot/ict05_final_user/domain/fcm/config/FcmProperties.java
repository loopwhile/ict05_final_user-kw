package com.boot.ict05_final_user.domain.fcm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Firebase Cloud Messaging(FCM) 관련 애플리케이션 설정을
 * YAML 또는 properties 파일에서 바인딩하는 구성 클래스입니다.
 *
 * <p>본 클래스는 Spring Boot의 {@code @ConfigurationProperties}를 사용하여
 * prefix "fcm"으로 시작하는 설정 항목을 자동으로 매핑합니다.
 *
 * <p>FCM 서비스의 활성화 여부, 서비스 계정 경로, 타임아웃,
 * 웹 푸시 기본 설정(Webpush), 토큰 정리(Cleanup) 정책 등의 세부 옵션을 제공합니다.
 *
 * <p>예시 (application.yml):
 * <pre>
 * fcm:
 *   enabled: true
 *   service-account: classpath:fcm/toastlab-firebase-adminsdk.json
 *   timeout-ms: 3000
 *   webpush:
 *     icon: /icons/icon-192.png
 *     badge: /icons/badge-72.png
 *     default-link: /user
 *   cleanup:
 *     cron: 0 0 4 * * *
 *     days-inactive: 90
 * </pre>
 * @author 이경욱
 * @since 2025-11-20
 */
@Component
@ConfigurationProperties(prefix = "fcm")
@Getter
@Setter
public class FcmProperties {

    /**
     * FCM 기능 활성화 여부.
     * <p>{@code true}인 경우 FCM 발송 및 스케줄러 기능이 동작합니다.</p>
     */
    private boolean enabled = true;

    /**
     * Firebase 서비스 계정(JSON) 파일 경로.
     * <p>예: {@code file:/path/to/service-account.json} 또는 {@code classpath:/fcm/service.json}</p>
     */
    private String serviceAccount;

    /**
     * FCM Admin SDK 서버 호출 타임아웃 (단위: 밀리초).
     * <p>기본값은 3000ms입니다.</p>
     */
    private int timeoutMs = 3000;

    /**
     * 브라우저(WebPush) 알림 관련 설정을 담는 내부 클래스.
     */
    private Webpush webpush = new Webpush();

    /**
     * FCM 토큰 자동 정리 정책을 정의하는 내부 클래스.
     */
    private Cleanup cleanup = new Cleanup();

    /**
     * 웹 푸시(WebPush) 알림 설정 클래스.
     * <p>알림 아이콘, 배지, 클릭 시 기본 이동 경로 등을 정의합니다.</p>
     */
    @Getter
    @Setter
    public static class Webpush {

        /**
         * 알림 아이콘 경로.
         * <p>예: {@code /user/images/fcm/toastlab.png}</p>
         */
        private String icon = "/icons/icon-192.png";

        /**
         * 알림 배지(badge) 이미지 경로.
         * <p>예: {@code /user/images/fcm/badge-72.png}</p>
         */
        private String badge;

        /**
         * 알림 클릭 시 이동할 기본 경로.
         * <p>예: {@code /user}</p>
         */
        private String defaultLink = "/";
    }

    /**
     * 토큰 정리(Cleanup) 스케줄 정책 클래스.
     * <p>스케줄 크론 표현식 및 미사용 판단 기준 일수를 정의합니다.</p>
     */
    @Getter
    @Setter
    public static class Cleanup {

        /**
         * 정리 스케줄 실행 주기 (cron 형식).
         * <p>예: {@code 0 0 4 * * *} → 매일 새벽 4시 실행.</p>
         */
        private String cron = "0 0 4 * * *";

        /**
         * 최근 미사용으로 판단할 기준 일수.
         * <p>이 기간 동안 사용되지 않은 토큰은 정리 대상이 됩니다.</p>
         */
        private int daysInactive = 90;
    }
}
