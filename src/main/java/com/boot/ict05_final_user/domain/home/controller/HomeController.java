package com.boot.ict05_final_user.domain.home.controller;

import com.boot.ict05_final_user.config.security.principal.AppUser;
import com.boot.ict05_final_user.domain.home.dto.*;
import com.boot.ict05_final_user.domain.home.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 대시보드 화면에서 사용하는 조회용 REST 컨트롤러.
 *
 * <p>로그인된 가맹점 기준으로 오늘 지표 카드, 인기 메뉴 Top N, 시간대별 통계를 제공합니다.</p>
 *
 * <ul>
 * <li>오늘의 KPI 요약 카드 조회</li>
 * <li>오늘의 인기 메뉴 Top N 조회</li>
 * <li>오늘의 시간대별 매출/주문수 등 시계열 조회</li>
 * </ul>
 *
 * <p>경로 규칙: 모든 엔드포인트는 "/dashboard" 하위로 노출됩니다.</p>
 *
 * @author 팀
 * @since 2025-11-24
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/dashboard")
@CrossOrigin(
        origins = {"http://localhost:3000"}, // 운영/개발 도메인 맞춰서 교체
        allowCredentials = "true"
)
public class HomeController {

    private final HomeService homeService;

    /**
     * 오늘의 KPI 카드 데이터를 조회한다.
     *
     * <p>예: 오늘 매출, 주문수, 객단가 등 핵심 지표를 한 번에 보여주기 위한 카드 용 데이터.</p>
     *
     * @param user 인증 사용자 정보. JWT에서 추출된 AppUser. 가맹점 식별자(storeId)를 포함한다.
     * @return 오늘 KPI 카드 응답 DTO
     */
    @Operation(
            summary = "오늘 KPI 요약 카드",
            description = "로그인된 가맹점 기준으로 오늘의 핵심 지표 요약을 반환합니다."
    )
    @GetMapping("/kpis/today")
    public KpiCardsResponseDTO getTodayKpis(@AuthenticationPrincipal AppUser user) {
        Long storeId = user.getStoreId();
        return homeService.getTodayKpis(storeId);
    }

    /**
     * 오늘의 인기 메뉴 Top N을 조회한다.
     *
     * <p>기본값은 5개이며, 필요 시 쿼리 파라미터로 개수를 조정할 수 있다.</p>
     *
     * @param user 인증 사용자 정보
     * @param limit 조회할 메뉴 개수. 기본값 5
     * @return 인기 메뉴 목록 응답 DTO
     */
    @Operation(
            summary = "오늘 인기 메뉴 Top N",
            description = "로그인된 가맹점 기준으로 오늘의 인기 메뉴 상위 목록을 반환합니다. 기본 5개."
    )
    @GetMapping(value = "/menus/top5")
    public TopMenusResponseDTO getTodayTopMenus(
            @AuthenticationPrincipal AppUser user,
            @RequestParam(defaultValue = "5") Integer limit
    ) {
        Long storeId = user.getStoreId();
        return homeService.getTopMenus(storeId, limit);
    }

    /**
     * 오늘의 시간대별 통계를 조회한다.
     *
     * <p>시간별 매출, 주문수 등 대시보드 시계열 차트에 사용하는 데이터.</p>
     *
     * @param user 인증 사용자 정보
     * @return 시간대별 통계 응답 DTO
     */
    @Operation(
            summary = "오늘 시간대별 통계",
            description = "로그인된 가맹점 기준으로 오늘의 시간대별 매출/주문수 등 시계열 데이터를 반환합니다."
    )
    @GetMapping("/hourly/today")
    public HourlyStatsResponseDTO getTodayHourly(
            @AuthenticationPrincipal AppUser user
    ) {
        Long storeId = user.getStoreId();
        return homeService.getTodayHourly(storeId);
    }
}
