package com.boot.ict05_final_user.domain.store.entity;

/**
 * 가맹점 운영 상태를 표현하는 열거형(Enum).
 *
 * <p>JPA 엔티티에서 {@code @Enumerated(EnumType.STRING)}과 함께 사용하면
 * DB에는 상수명(OPERATING/PREPARING/CLOSED)으로 저장되고,
 * 화면에는 한국어 라벨({@link #description})을 표시할 수 있다.</p>
 */

public enum StoreStatus {

    /** 운영 중인 가맹점 */
    OPERATING("운영"),

    /** 개점 준비 중인 가맹점 */
    PREPARING("개점준비"),

    /** 폐업한 가맹점 */
    CLOSED("폐업");

    /** 각 상태의 한국어 라벨(뷰 표시용) */
    private final String description;

    /**
     * 열거형 생성자.
     *
     * @param description 상태의 한국어 라벨(예: "운영")
     */
    StoreStatus(String description) { this.description = description; }

    /**
     * 상태의 한국어 라벨을 반환한다.
     *
     * @return 한국어 라벨(예: "운영", "개점준비", "폐업")
     */
    public String getDescription() { return description; }
}
