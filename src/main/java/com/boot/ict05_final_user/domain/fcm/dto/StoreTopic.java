package com.boot.ict05_final_user.domain.fcm.dto;

/**
 * 가맹점 관련 FCM 토픽명 유틸리티 클래스.
 *
 * <p>가맹점 단위별 토픽명을 생성하거나, 허용된 토픽인지 검증할 때 사용합니다.
 * 내부적으로 모든 주제는 {@code store-}, {@code inv-low-}, {@code expire-soon-} 등의 접두어를 사용합니다.</p>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public final class StoreTopic {

	private StoreTopic() { }

	/** 전체 스토어 공용 토픽명 */
	public static String storeAll() { return "store-all"; }

	/** 개별 스토어 공지용 토픽명 */
	public static String store(long storeId) { return "store-" + storeId; }

	/** 재고 부족 알림용 토픽명 */
	public static String invLow(long storeId) { return "inv-low-" + storeId; }

	/** 유통기한 임박 알림용 토픽명 */
	public static String expireSoon(long storeId) { return "expire-soon-" + storeId; }

	// === 구 코드 호환용 별칭 ===
	public static String notice(long storeId) { return store(storeId); }
	public static String stockLow(long storeId) { return invLow(storeId); }

	/**
	 * 주어진 토픽명이 허용된 형식인지 검증합니다.
	 *
	 * @param topic 검사할 토픽명
	 * @return 허용된 토픽이면 true, 아니면 false
	 */
	public static boolean isAllowed(String topic) {
		return topic != null && (
				topic.equals("store-all") ||
						topic.startsWith("store-") ||
						topic.startsWith("inv-low-") ||
						topic.startsWith("expire-soon-")
		);
	}
}
