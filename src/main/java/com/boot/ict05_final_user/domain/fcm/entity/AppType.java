package com.boot.ict05_final_user.domain.fcm.entity;

/**
 * FCM 토큰 및 발송 로그 등에서 사용되는 앱 구분 ENUM.
 *
 * <p>본사(HQ)와 가맹점(STORE) 시스템을 구분하기 위해 사용됩니다.</p>
 *
 * <ul>
 *   <li>{@link #HQ} - 본사 관리자 웹 애플리케이션</li>
 *   <li>{@link #STORE} - 가맹점(매장) 애플리케이션</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public enum AppType {
    HQ, STORE
}
