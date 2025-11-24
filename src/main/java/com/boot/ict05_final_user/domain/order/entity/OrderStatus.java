package com.boot.ict05_final_user.domain.order.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 주문 상태 Enum.
 *
 * <p>DB에는 한글 라벨(예: "준비중")을 저장하며, 애플리케이션 로직에서는 영문 키(Enum 상수)를 사용합니다.</p>
 */
@Schema(
        description = "주문 상태",
        allowableValues = {
                "PENDING", "PAID", "PREPARING", "COOKING", "COMPLETED",
                "CANCELED", "READY", "REFUNDED"
        }
)
public enum OrderStatus {
    PENDING("대기"),
    PAID("결제완료"),
    PREPARING("준비중"),
    COOKING("조리중"),
    COMPLETED("완료"),
    CANCELED("취소"),
    READY("픽업대기"),
    REFUNDED("환불");

    /** DB 저장용 한글 라벨 */
    private final String dbValue;

    OrderStatus(String dbValue) { this.dbValue = dbValue; }

    /**
     * DB 저장용 한글 라벨을 반환합니다.
     *
     * @return 한글 라벨
     */
    public String getDbValue() { return dbValue; }

    /**
     * DB 한글 라벨에서 Enum 상수로 변환합니다.
     *
     * @param dbValue DB에 저장된 한글 라벨
     * @return 매핑된 주문 상태
     * @throws IllegalArgumentException 알 수 없는 라벨인 경우
     */
    public static OrderStatus from(String dbValue) {
        for (OrderStatus v : values()) {
            if (v.dbValue.equals(dbValue)) return v;
        }
        throw new IllegalArgumentException("Unknown OrderStatus dbValue=" + dbValue);
    }
}
