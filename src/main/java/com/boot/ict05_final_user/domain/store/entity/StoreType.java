package com.boot.ict05_final_user.domain.store.entity;

/**
 * 가맹점 구분을 표현하는 열거형(Enum).
 *
 * <p>JPA 엔티티에서 {@code @Enumerated(EnumType.STRING)}으로 매핑하면
 * DB에는 상수명(DIRECT/FRANCHISE)으로 저장되고,
 * 화면에는 한국어 라벨({@link #description})을 사용할 수 있다.</p>
 */

public enum StoreType {

    /** 직영점 */
    DIRECT("직영점"),

    /** 가맹점 */
    FRANCHISE("가맹점");

    /** 각 구분의 한국어 라벨(뷰 표시용) */
    private final String description;

    /**
     * 열거형 생성자.
     *
     * @param description 한국어 라벨(예: "직영점", "가맹점")
     */
    StoreType(String description) { this.description = description; }

    /**
     * 카테고리 한글 설명을 반환한다.
     *
     * @return 카테고리 설명
     */
    public String getDescription() { return description; }
}
