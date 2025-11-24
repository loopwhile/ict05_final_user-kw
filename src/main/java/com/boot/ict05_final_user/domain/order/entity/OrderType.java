package com.boot.ict05_final_user.domain.order.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 주문 형태 Enum.
 *
 * <p>방문/포장/배달의 주문 유형을 표현하며, DB 저장용 코드와 한글 라벨을 함께 보유합니다.</p>
 */
@Schema(description = "주문 형태", allowableValues = {"VISIT", "TAKEOUT", "DELIVERY"})
public enum OrderType {
    VISIT("VISIT", "방문"),
    TAKEOUT("TAKEOUT", "포장"),
    DELIVERY("DELIVERY", "배달");

    /** DB 저장용 코드 */
    private final String dbValue;

    /** 화면 표기를 위한 한글 라벨 */
    private final String label;

    OrderType(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    /** DB 저장용 코드를 반환합니다. */
    public String getDbValue() { return dbValue; }

    /** 한글 라벨을 반환합니다. */
    public String getLabel()   { return label; }

    /**
     * DB 저장용 코드에서 Enum으로 변환합니다.
     *
     * @param dbValue DB 저장용 코드
     * @return 매핑된 주문 형태
     * @throws IllegalArgumentException 알 수 없는 코드인 경우
     */
    public static OrderType from(String dbValue) {
        for (OrderType v : values()) if (v.dbValue.equals(dbValue)) return v;
        throw new IllegalArgumentException("Unknown OrderType dbValue=" + dbValue);
    }
}
