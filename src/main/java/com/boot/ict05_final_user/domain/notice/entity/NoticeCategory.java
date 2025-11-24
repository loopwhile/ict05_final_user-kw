package com.boot.ict05_final_user.domain.notice.entity;

public enum NoticeCategory {

    /** 일반 공지 */
    NORMAL("일반"),

    /** 시스템 관련 공지 */
    SYSTEM("시스템"),

    /** 이벤트 공지 */
    EVENT("이벤트"),

    /** 정책/공지사항 */
    POLICY("공지");

    /** 한글 설명 */
    private final String description;

    /**
     * 생성자
     *
     * @param description 각 카테고리의 한글 설명
     */
    NoticeCategory(String description) {
        this.description = description;
    }

    /**
     * 카테고리 한글 설명을 반환한다.
     *
     * @return 카테고리 설명
     */
    public String getDescription() {
        return description;
    }
}
