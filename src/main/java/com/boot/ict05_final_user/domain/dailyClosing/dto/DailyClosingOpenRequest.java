package com.boot.ict05_final_user.domain.dailyClosing.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 일일 시재 오픈(시작금 등록)에 사용하는 요청 DTO.
 */
@Getter
@Setter
public class DailyClosingOpenRequest {

    /** 마감 기준 일자 (오픈일) */
    private LocalDate closingDate;

    /** 시작 시재(준비금) */
    private Long startingCash;
}
