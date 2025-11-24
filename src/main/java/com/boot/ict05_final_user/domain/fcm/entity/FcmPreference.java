package com.boot.ict05_final_user.domain.fcm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * FCM 알림 선호도 엔티티.
 *
 * <p>사용자별로 공지, 재고부족, 유통임박 등 알림 수신 여부를 설정하며,
 * 알림 스캐너 또는 토픽 동기화 시 참고됩니다.</p>
 *
 * <ul>
 *   <li>테이블명: {@code fcm_preference}</li>
 *   <li>대상: STORE 앱 사용자 중심</li>
 *   <li>필드: 알림 카테고리 및 유통임박 기준일</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Entity
@Table(
		name = "fcm_preference",
		indexes = {
				@Index(name="ix_pref_member", columnList = "member_id_fk"),
				@Index(name="ix_pref_store",  columnList = "store_id_fk"),
				@Index(name="ix_pref_staff",  columnList = "staff_id_fk")
		}
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmPreference {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long fcmPreferenceId;

	/** 앱 구분 (HQ / STORE) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private AppType appType = AppType.STORE;

	@Column(name="member_id_fk")
	private Long memberIdFk;

	@Column(name="store_id_fk")
	private Long storeIdFk;

	@Column(name="staff_id_fk")
	private Long staffIdFk;

	/** 공지 알림 수신 여부 */
	@Column(nullable = false)
	private Boolean catNotice = true;

	/** 재고 부족 알림 수신 여부 */
	@Column(nullable = false)
	private Boolean catStockLow = true;

	/** 유통 임박 알림 수신 여부 */
	@Column(nullable = false)
	private Boolean catExpireSoon = true;

	/** 유통임박 알림 기준 일수 (예: 3일 전) */
	@Column(nullable = false)
	private Integer thresholdDays = 3;

	/** 생성 시각 */
	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	/** 수정 시각 */
	@Column(nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();

	/** 최초 생성 시 기본값 세팅 */
	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) createdAt = now;
		if (updatedAt == null) updatedAt = now;
		if (appType == null) appType = AppType.STORE;
		if (catNotice == null) catNotice = true;
		if (catStockLow == null) catStockLow = true;
		if (catExpireSoon == null) catExpireSoon = true;
		if (thresholdDays == null) thresholdDays = 3;
	}

	/** 수정 시 updatedAt 자동 갱신 */
	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
