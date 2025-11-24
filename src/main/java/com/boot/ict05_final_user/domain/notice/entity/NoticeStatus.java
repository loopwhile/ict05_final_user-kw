package com.boot.ict05_final_user.domain.notice.entity;

public enum NoticeStatus {

    /** 활성 상태 */
    ACTIVE("활성"),

    /** 비활성 상태 */
    INACTIVE("비활성"),

    /** 삭제 상태 */
    DELETED("삭제");

    /** 한글 설명(= DB ENUM 저장값) */
    private final String description;

    /**
     * 생성자
     *
     * @param description 각 상태의 한글 설명
     */
    NoticeStatus(String description) {
        this.description = description;
    }

    /**
     * 상태의 한글 설명을 반환한다.
     *
     * @return 상태 설명
     */
    public String getDescription() {
        return description;
    }
}
