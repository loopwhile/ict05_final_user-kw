package com.boot.ict05_final_user.domain.fcm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 가맹점 단위 FCM 발송 로그 엔티티.
 *
 * <p>공지사항, 재고부족, 유통임박, 테스트 등 모든 가맹점 발송 내역을 기록합니다.</p>
 *
 * <ul>
 *   <li>테이블명: {@code fcm_store_send_log}</li>
 *   <li>로그 범위: NOTICE / STOCK_LOW / EXPIRE_SOON / TEST</li>
 *   <li>본사용 {@code fcm_send_log} 와 분리 저장</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Entity
@Table(
        name = "fcm_store_send_log",
        indexes = {
                @Index(name = "ix_store_log_store", columnList = "store_id_fk"),
                @Index(name = "ix_store_log_member", columnList = "member_id_fk"),
                @Index(name = "ix_store_log_category", columnList = "category"),
                @Index(name = "ix_store_log_ref", columnList = "ref_type,ref_id"),
                @Index(name = "ix_store_log_ref_date", columnList = "ref_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmStoreSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fcmStoreSendLogId;

    /** 앱 구분 (대부분 STORE) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AppType appType;

    /** 알림 카테고리 (NOTICE / STOCK_LOW / EXPIRE_SOON / TEST 등) */
    @Column(nullable = false, length = 32)
    private String category;

    /** 매장 및 사용자 참조키 */
    @Column(name = "store_id_fk")
    private Long storeIdFk;

    @Column(name = "member_id_fk")
    private Long memberIdFk;

    /** 토픽명 또는 단일 토큰 */
    @Column(length = 255)
    private String topic;

    @Column(length = 512)
    private String token;

    /** 알림 제목 및 본문 */
    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    /** 클릭 이동 링크 (data.link, WebpushFcmOptions) */
    @Column(length = 1024)
    private String link;

    /** 참조 비즈니스 타입 (예: NOTICE, INVENTORY 등) */
    @Column(name = "ref_type", length = 32)
    private String refType;

    /** 참조 ID (예: notice_id 등) */
    @Column(name = "ref_id")
    private Long refId;

    /** 스캔 기준일 등 추가 정보 */
    @Column(name = "ref_date")
    private LocalDate refDate;

    /** Firebase 메시지 ID */
    @Column(name = "result_message_id", length = 255)
    private String resultMessageId;

    /** 오류 메시지 (있을 경우) */
    @Column(name = "result_error", length = 512)
    private String resultError;

    /** 발송 시각 */
    @Column(nullable = false)
    private LocalDateTime sentAt;

    /** 로그 생성 시각 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 생성 시 기본값 자동 설정 */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (sentAt == null) sentAt = now;
        if (createdAt == null) createdAt = now;
        if (appType == null) appType = AppType.STORE;
        if (category == null) category = "GENERAL";
    }
}
