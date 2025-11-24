package com.boot.ict05_final_user.domain.fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;

/**
 * Firebase Admin SDK 초기화 설정 클래스.
 *
 * <p>이 클래스는 {@link FcmProperties} 설정값을 기반으로 FirebaseApp과 FirebaseMessaging 빈을 생성합니다.
 * FCM 서비스 계정(JSON) 파일을 읽어 {@link FirebaseApp}을 초기화하고,
 * Spring 컨텍스트 내에서 전역적으로 사용할 수 있도록 {@link FirebaseMessaging} 빈을 등록합니다.</p>
 *
 * <p>FCM 기능이 비활성화되어 있더라도 Bean 정의는 유지되며,
 * 서비스 계정 경로가 유효하지 않을 경우 {@link Exception}을 발생시킵니다.</p>
 *
 * <h3>예시 (application.yml)</h3>
 * <pre>
 * fcm:
 *   enabled: true
 *   service-account: classpath:fcm/toastlab-firebase-adminsdk.json
 * </pre>
 * @author 이경욱
 * @since 2025-11-20
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class FirebaseConfig {

    /** FCM 관련 애플리케이션 속성 객체. */
    private final FcmProperties props;

    /** 서비스 계정 JSON 파일을 읽기 위한 리소스 로더. */
    private final ResourceLoader resourceLoader;

    /**
     * FirebaseApp Bean을 초기화합니다.
     *
     * <p>서비스 계정 JSON 파일 경로는 {@link FcmProperties#getServiceAccount()}에서 가져오며,
     * 이미 FirebaseApp이 존재하지 않는 경우에만 새로운 인스턴스를 등록합니다.</p>
     *
     * @return 초기화된 {@link FirebaseApp} 인스턴스
     * @throws Exception 서비스 계정 파일을 읽을 수 없거나 Firebase 초기화에 실패한 경우
     */
    @Bean
    public FirebaseApp firebaseApp() throws Exception {
        if (!props.isEnabled()) {
            log.warn("[FCM] disabled by configuration.");
        }

        Resource resource = resourceLoader.getResource(props.getServiceAccount());
        try (InputStream in = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            // 앱 이름 충돌 방지를 위해 명시적 이름 지정
            return FirebaseApp.initializeApp(options, "store-app");
        }
    }

    /**
     * FirebaseMessaging Bean을 등록합니다.
     *
     * <p>해당 Bean은 Firebase Admin SDK의 {@link FirebaseMessaging#getInstance(FirebaseApp)}
     * 메서드를 사용하여 생성되며, {@link FirebaseApp} Bean을 의존합니다.</p>
     *
     * @param app {@link FirebaseApp} 인스턴스
     * @return {@link FirebaseMessaging} 인스턴스
     */
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }
}
