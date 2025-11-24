package com.boot.ict05_final_user.domain.fcm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * FCM 기기 토큰 엔티티.
 *
 * <p>사용자(본사/가맹점)의 기기별 FCM 토큰 정보를 저장하며,
 * 토큰 활성화/비활성화, 마지막 접속일, 업데이트 시각 등을 관리합니다.</p>
 *
 * <ul>
 *   <li>테이블명: {@code fcm_device_token}</li>
 *   <li>고유 제약조건: {@code uq_fcm_token}</li>
 *   <li>인덱스: member_id_fk, store_id_fk, staff_id_fk</li>
 * </ul>
 *
 * <p>비활성화된 토큰은 스케줄러({@code FcmTokenCleanupScheduler})에 의해 주기적으로 정리됩니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Entity
@Table(
        name = "fcm_device_token",
        indexes = {
                @Index(name="ix_device_member", columnList = "member_id_fk"),
                @Index(name="ix_device_store",  columnList = "store_id_fk"),
                @Index(name="ix_device_staff",  columnList = "staff_id_fk")
        },
        uniqueConstraints = @UniqueConstraint(name="uq_fcm_token", columnNames = "token")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fcmDeviceTokenId;

    /** 앱 구분 (HQ 또는 STORE) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AppType appType;

    /** 단말 플랫폼 (WEB, ANDROID, IOS) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlatformType platform;

    /** FCM 등록 토큰 (고유) */
    @Column(nullable = false, length = 512)
    private String token;

    /** 클라이언트 디바이스 ID (선택적) */
    @Column(length = 128)
    private String deviceId;

    /** 연관 사용자 FK */
    @Column(name = "member_id_fk")
    private Long memberIdFk;

    /** 연관 매장 FK */
    @Column(name = "store_id_fk")
    private Long storeIdFk;

    /** 연관 직원 FK (본사용) */
    @Column(name = "staff_id_fk")
    private Long staffIdFk;

    /** 토큰 활성 상태 */
    @Column(nullable = false)
    private Boolean isActive = true;

    /** 마지막 접속 시각 (포그라운드 진입 시 갱신) */
    private LocalDateTime lastSeenAt;

    /** 생성 시각 */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 수정 시각 */
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** 업데이트 시각 자동 갱신 */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 최초 생성 시 기본값 세팅 */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (isActive == null) isActive = true;
    }
}
