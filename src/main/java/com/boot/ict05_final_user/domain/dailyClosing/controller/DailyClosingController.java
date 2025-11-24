package com.boot.ict05_final_user.domain.dailyClosing.controller;

import com.boot.ict05_final_user.config.security.principal.AppUser;
import com.boot.ict05_final_user.domain.dailyClosing.dto.*;
import com.boot.ict05_final_user.domain.dailyClosing.service.DailyClosingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 일일 시재 마감 화면에서 사용하는 조회용 REST 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/daily-closing")
public class DailyClosingController {

    private final DailyClosingService dailyClosingService;

    /**
     * 일일 시재 오픈(시작 시재 등록) API.
     *
     * <p>아침에 점포를 오픈할 때 해당 일자의 시작 시재(준비금)를 등록하는 용도이다.</p>
     *
     * <p>동작 순서</p>
     * <ol>
     *     <li>인증 정보(AppUser)에서 현재 가맹점의 storeId 를 추출한다.</li>
     *     <li>요청 본문으로 받은 closingDate, startingCash 를 서비스 계층에 전달한다.</li>
     *     <li>이미 마감된 일자인 경우 서비스에서 IllegalStateException 을 발생시키고,
     *         전역 예외 처리기에서 적절한 HTTP 상태 코드로 변환해 반환한다.</li>
     * </ol>
     *
     * @param user 현재 로그인한 사용자(AppUser)
     * @param request 오픈 요청 본문(일자, 시작 시재)
     */
    @PostMapping("/open")
    public void openDailyClosing(
            @AuthenticationPrincipal AppUser user,
            @RequestBody DailyClosingOpenRequest request
    ) {
        Long storeId = user.getStoreId();
        dailyClosingService.openDailyClosing(storeId, request);
    }

    /**
     * 일일 시재 마감 화면 진입 시 필요한 데이터를 조회한다.
     *
     * date 파라미터가 없으면 오늘 날짜를 기준으로 조회한다.
     * 실제 서비스에서는 로그인 정보에서 점포 아이디를 추출해 사용해야 한다.
     *
     * @param user 현재 로그인 사용자 정보
     * @param date      조회 기준 일자 (선택)
     * @return 화면에서 사용할 초기 데이터
     */
    @GetMapping("/close")
    public DailyClosingInitResponse getDailyClosing(
            @AuthenticationPrincipal AppUser user,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now();
        }

        Long storeId = extractStoreId(user);

        return dailyClosingService.getDailyClosing(storeId, date);
    }

    /**
     * 로그인 정보에서 점포 아이디를 꺼내는 헬퍼 메서드.
     *
     * @param user 인증 정보
     * @return 점포 아이디
     */
    private Long extractStoreId(AppUser user) {
        if (user == null) {
            throw new IllegalStateException("로그인 정보가 없습니다. storeId 를 추출할 수 없습니다.");
        }
        return user.getStoreId();
    }

    /**
     * 일일 시재 마감 내용을 저장한다.
     *
     * 이미 해당 일자에 마감 데이터가 있으면 수정,
     * 없으면 새로 생성하는 방식으로 처리한다.
     */
    @PostMapping("/close")
    public void saveDailyClosing(
            @AuthenticationPrincipal AppUser user,
            @RequestBody DailyClosingSaveRequest request) {
        Long storeId = extractStoreId(user);
        dailyClosingService.saveDailyClosing(storeId, request);
    }

    /**
     * 특정 기간 동안의 일일 마감 요약 리스트를 조회한다.
     *
     * <p>
     * - 로그인한 가맹점의 storeId 를 기준으로<br>
     * - closingDate 가 from 이상, to 이하인 DailyClosing 엔티티를 조회하고<br>
     * - 리스트 화면에서 사용하기 좋은 요약 DTO 리스트를 반환한다.
     * </p>
     *
     * 예시 요청:
     * <pre>
     *   GET /api/daily-closing/history?from=2025-11-01&to=2025-11-17
     * </pre>
     *
     * @param principal 현재 로그인 사용자 정보 (AppUser)
     * @param from      조회 시작 일자(포함), 포맷: yyyy-MM-dd
     * @param to        조회 종료 일자(포함), 포맷: yyyy-MM-dd
     * @return 기간 내 일일 마감 요약 리스트
     */
    @GetMapping("/history")
    public List<DailyClosingSummaryDto> getDailyClosingHistory(
            @AuthenticationPrincipal AppUser principal,
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long storeId = principal.getStoreId();
        return dailyClosingService.getDailyClosingHistory(storeId, from, to);
    }

    /**
     * 일일 시재 마감 상세를 조회한다.
     *
     * 예시:
     *  GET /api/daily-closing/detail?date=2025-11-18
     *
     * @param user 현재 로그인 사용자
     * @param date 조회할 마감 일자
     * @return 일일 시재 마감 상세 응답 DTO
     */
    @GetMapping("/detail")
    public DailyClosingDetailResponse getDailyClosingDetail(
            @AuthenticationPrincipal AppUser user,
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long storeId = extractStoreId(user);
        return dailyClosingService.getDailyClosingDetail(storeId, date);
    }
}
