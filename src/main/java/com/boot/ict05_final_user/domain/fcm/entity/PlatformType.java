package com.boot.ict05_final_user.domain.fcm.entity;

/**
 * FCM 토큰이 발급된 단말 플랫폼 유형을 구분하는 ENUM.
 *
 * <p>클라이언트 플랫폼별로 FCM 토큰을 관리할 때 사용됩니다.</p>
 *
 * <ul>
 *   <li>{@link #WEB} - 웹 브라우저 (Service Worker 기반)</li>
 *   <li>{@link #ANDROID} - 안드로이드 앱</li>
 *   <li>{@link #IOS} - iOS 앱</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public enum PlatformType {
    WEB, ANDROID, IOS
}
