package com.boot.ict05_final_user.domain.notice.entity;

public enum NoticePriority {

    /** 일반 우선순위 */
    NORMAL("일반"),

    /** 중요 우선순위 */
    IMPORTANT("중요"),

    /** 긴급 우선순위 */
    EMERGENCY("긴급");

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자
     *
     * @param description 각 우선순위의 한글 설명
     */
    NoticePriority(String description) {
        this.description = description;
    }

    /**
     * 우선순위 한글 설명을 반환한다.
     *
     * @return 우선순위 설명
     */
    public String getDescription() {
        return description;
    }
}
