package com.boot.ict05_final_user.domain.inventory.entity;

/**
 * 재고 조정 사유 Enum
 *
 * <p>재고 조정 시 선택 가능한 사유를 정의하며, 각 항목은 한글 설명(description)을 가진다.</p>
 *
 * <p>조정 사유:</p>
 * <ul>
 *     <li>MANUAL: 수동 수정</li>
 *     <li>DAMAGE: 파손</li>
 *     <li>LOSS: 분실</li>
 *     <li>ERROR: 데이터 오류 정정</li>
 * </ul>
 */
public enum AdjustmentReason {

    /** 수동 수정 */
    MANUAL("수동 수정"),

    /** 파손 */
    DAMAGE("파손"),

    /** 분실 */
    LOSS("분실"),

    /** 데이터 오류 정정 */
    ERROR("데이터 오류 정정");

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자
     *
     * @param description 각 조정 사유의 한글 설명
     */
    AdjustmentReason(String description) {
        this.description = description;
    }

    /**
     * 조정 사유의 한글 설명을 반환한다.
     *
     * @return 조정 사유 설명
     */
    public String getDescription() {
        return description;
    }
}
