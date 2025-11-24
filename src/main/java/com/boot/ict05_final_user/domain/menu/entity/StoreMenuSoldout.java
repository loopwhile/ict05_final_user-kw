package com.boot.ict05_final_user.domain.menu.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 가맹점 메뉴 판매 상태(품절 여부)를 나타내는 Enum.
 *
 * <p>매장 단위로 메뉴의 판매 가능 여부를 관리합니다.</p>
 */
@Schema(description = "가맹점 메뉴 판매 상태(품절 여부)", allowableValues = {"ON_SALE", "SOLD_OUT"})
public enum StoreMenuSoldout {

    @Schema(description = "판매 가능")
    ON_SALE(0, "판매중"),

    @Schema(description = "품절")
    SOLD_OUT(1, "품절");

    /** 숫자 코드(영속/전송 시 사용 가능) */
    private final int code;

    /** 한글 라벨(표시용) */
    private final String label;

    StoreMenuSoldout(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 숫자 코드를 반환합니다. */
    public int getCode() { return code; }

    /** 한글 라벨을 반환합니다. */
    public String getLabel() { return label; }

    /**
     * 정수 코드에서 Enum으로 변환합니다.
     *
     * @param code 상태 코드
     * @return 매핑된 {@link StoreMenuSoldout}
     */
    public static StoreMenuSoldout fromCode(int code) {
        return code == 1 ? SOLD_OUT : ON_SALE;
    }
}
