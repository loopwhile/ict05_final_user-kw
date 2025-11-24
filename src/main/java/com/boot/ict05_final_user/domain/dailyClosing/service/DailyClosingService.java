package com.boot.ict05_final_user.domain.dailyClosing.service;

import com.boot.ict05_final_user.domain.dailyClosing.dto.*;
import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosing;
import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosingDenom;
import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosingExpense;
import com.boot.ict05_final_user.domain.dailyClosing.repository.DailyClosingRepository;
import com.boot.ict05_final_user.domain.dailyClosing.repository.DailyClosingRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 일일 시재 마감 조회와 관련된 도메인 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyClosingService {

    private final DailyClosingRepository dailyClosingRepository;

    /**
     * 일일 시재 오픈(시작 시재 등록)을 처리한다.
     *
     * <p>동작 순서</p>
     * <ol>
     *     <li>요청 일자(closingDate)로 기존 DailyClosing 엔티티를 조회한다.</li>
     *     <li>이미 마감(closed = true)된 건이 존재하면 예외를 발생시켜 오픈을 막는다.</li>
     *     <li>엔티티가 없으면 새 DailyClosing 을 생성해 시작 시재(startingCash)만 채운다.</li>
     *     <li>엔티티가 있지만 미마감(closed = false) 상태라면 시작 시재만 갱신한다.</li>
     *     <li>모든 금액 필드를 0으로 초기화하고 closed = false 로 설정한 뒤 저장한다.</li>
     * </ol>
     *
     * @param storeId 가맹점 아이디
     * @param request 오픈 요청 DTO (일자, 시작 시재)
     * @throws IllegalArgumentException closingDate 가 누락된 경우
     * @throws IllegalStateException    이미 마감이 완료된 일자인 경우
     */
    public void openDailyClosing(Long storeId, DailyClosingOpenRequest request) {

        LocalDate closingDate = request.getClosingDate();
        if (closingDate == null) {
            throw new IllegalArgumentException("closingDate 는 필수 값입니다.");
        }

        // 이미 마감된 날이면 오픈을 허용하지 않는다.
        DailyClosing existing = dailyClosingRepository
                .findByStoreIdAndClosingDate(storeId, closingDate)
                .orElse(null);

        if (existing != null && existing.isClosed()) {
            throw new IllegalStateException("이미 마감이 완료된 일자입니다.");
        }

        DailyClosing closing;

        if (existing == null) {
            // 처음 오픈하는 날: 새 엔티티 생성
            closing = DailyClosing.builder()
                    .storeId(storeId)
                    .closingDate(closingDate)
                    .cashVisitSales(0L)
                    .cashTakeoutSales(0L)
                    .cashDeliverySales(0L)
                    .cardVisitSales(0L)
                    .cardTakeoutSales(0L)
                    .cardDeliverySales(0L)
                    .voucherSales(0L)
                    .totalDiscount(0L)
                    .totalRefund(0L)
                    .startingCash(
                            request.getStartingCash() == null ? 0L : request.getStartingCash()
                    )
                    .totalExpense(0L)
                    .depositAmount(0L)
                    .calculatedCash(0L)
                    .actualCash(0L)
                    .carryoverCash(0L)
                    .differenceAmount(0L)
                    .isClosed(false)
                    .build();
        } else {
            // 이미 존재하지만 아직 마감 전인 날: 시작금만 수정
            closing = existing;
            closing.setStartingCash(
                    request.getStartingCash() == null ? 0L : request.getStartingCash()
            );
            closing.setClosed(false);
        }

        dailyClosingRepository.save(closing);
    }

    /**
     * 일일 시재 마감 화면에서 사용할 초기 데이터를 조회한다.
     *
     * <p>동작 케이스</p>
     * <ul>
     *     <li>1) store_daily_closing 행이 있고 closed = true 인 경우
     *         → 이미 마감된 날, DB 스냅샷을 그대로 반환한다.</li>
     *     <li>2) 행이 없거나, 행은 있지만 closed = false 인 경우
     *         → 미마감 상태, 주문 집계는 실시간으로 계산하고
     *         → DailyClosing 이 있으면 시작금·지출·권종 등은 그 값을 채워준다.</li>
     * </ul>
     *
     * @param storeId 가맹점 아이디
     * @param date    기준 일자
     * @return 화면에서 사용할 초기 데이터 응답 DTO
     */
    public DailyClosingInitResponse getDailyClosing(Long storeId, LocalDate date) {

        // 0. 해당 일자의 DailyClosing 엔티티 조회
        DailyClosing closing = dailyClosingRepository
                .findByStoreIdAndClosingDate(storeId, date)
                .orElse(null);

        // 1. 이미 마감된 날(closed = true) 이면 스냅샷 반환
        if (closing != null && closing.isClosed()) {
            DailyClosingInitResponse resp = DailyClosingInitResponse.fromClosing(closing);

            // 지출 상세
            dailyClosingRepository.findExpensesByClosing(closing)
                    .forEach(e -> resp.getExpenses().add(DailyClosingExpenseDto.from(e)));

            // 권종 상세
            dailyClosingRepository.findDenomsByClosing(closing)
                    .forEach(d -> resp.getDenoms().add(DailyClosingDenomDto.from(d)));

            // 이 경우에는 resp.closed 가 true 여야 함 (fromClosing 내부에서 세팅)
            return resp;
        }

        // 2. 미마감 상태(행 없음 or closed = false) → 주문 집계는 실시간 계산
        DailyClosingRepositoryCustom.OrderDailySummary summary =
                dailyClosingRepository.getOrderDailySummary(storeId, date);

        DailyClosingInitResponse resp = DailyClosingInitResponse.builder()
                .cashVisit(summary.cashVisit)
                .cashTakeout(summary.cashTakeout)
                .cashDelivery(summary.cashDelivery)
                .cardVisit(summary.cardVisit)
                .cardTakeout(summary.cardTakeout)
                .cardDelivery(summary.cardDelivery)
                .voucherTotal(summary.voucherTotal)
                .totalDiscount(summary.totalDiscount)
                .totalRefund(summary.totalRefund)
                .closed(false)   // ★ 미마감이므로 항상 false
                .build();

        // 2-1. 아직 DailyClosing 행이 없다면(오픈도 안 한 상태) 여기서 바로 반환
        if (closing == null) {
            return resp;
        }

        // 2-2. DailyClosing 은 있지만 아직 마감 전(closed = false)인 날
        //      → 시작금, 지출, 권종, 차액 메모 등은 저장된 값으로 채워준다.
        resp.setStartingCash(closing.getStartingCash());
        resp.setTotalExpense(closing.getTotalExpense());
        resp.setDepositAmount(closing.getDepositAmount());
        resp.setCalculatedCash(closing.getCalculatedCash());
        resp.setActualCash(closing.getActualCash());
        resp.setCarryoverCash(closing.getCarryoverCash());
        resp.setDifferenceAmount(closing.getDifferenceAmount());
        resp.setDifferenceMemo(closing.getDifferenceMemo());

        dailyClosingRepository.findExpensesByClosing(closing)
                .forEach(e -> resp.getExpenses().add(DailyClosingExpenseDto.from(e)));

        dailyClosingRepository.findDenomsByClosing(closing)
                .forEach(d -> resp.getDenoms().add(DailyClosingDenomDto.from(d)));

        // 이 케이스도 미마감이므로 resp.closed 는 false 그대로 유지
        return resp;
    }

    /**
     * 일일 시재 마감 내용을 저장한다.
     *
     * 동작 순서
     * 1. 점포 아이디와 마감 일자를 기준으로 DailyClosing 엔티티를 조회한다.
     *    이미 존재하면 수정하고, 없으면 새로 생성한다.
     * 2. 주문 테이블 기준 매출 합계를 다시 계산해서 DailyClosing 매출 필드에 반영한다.
     * 3. 화면에서 전달된 시재 관련 금액, 지출 목록, 권종 목록을 DailyClosing 엔티티에 매핑한다.
     * 4. 마감 여부 플래그를 true 로 설정하고 저장한다.
     *
     * 주문 데이터와 시재 데이터가 항상 같은 기준으로 저장되도록
     * 조회와 저장에서 동일한 매출 집계 메서드(getOrderDailySummary)를 사용한다.
     *
     * @param storeId 가맹점 아이디
     * @param request 일일 시재 마감 저장 요청 본문
     */
    @Transactional
    public void saveDailyClosing(Long storeId, DailyClosingSaveRequest request) {

        LocalDate closingDate = request.getClosingDate();
        if (closingDate == null) {
            throw new IllegalArgumentException("closingDate 는 필수 값입니다.");
        }

        // 1. 기존 DailyClosing 조회 또는 신규 생성
        DailyClosing existing = dailyClosingRepository
                .findByStoreIdAndClosingDate(storeId, closingDate)
                .orElse(null);

        DailyClosing closing;
        if (existing == null) {
            // 처음 저장하는 날
            closing = DailyClosing.builder()
                    .storeId(storeId)
                    .closingDate(closingDate)
                    .cashVisitSales(0L)
                    .cashTakeoutSales(0L)
                    .cashDeliverySales(0L)
                    .cardVisitSales(0L)
                    .cardTakeoutSales(0L)
                    .cardDeliverySales(0L)
                    .voucherSales(0L)
                    .totalDiscount(0L)
                    .totalRefund(0L)
                    .startingCash(0L)
                    .totalExpense(0L)
                    .depositAmount(0L)
                    .calculatedCash(0L)
                    .actualCash(0L)
                    .carryoverCash(0L)
                    .differenceAmount(0L)
                    .isClosed(false)
                    .build();
        } else {
            closing = existing;
        }

        // 2. 주문 기준 매출 합계 다시 계산
        DailyClosingRepositoryCustom.OrderDailySummary summary =
                dailyClosingRepository.getOrderDailySummary(storeId, closingDate);

        closing.setCashVisitSales(summary.cashVisit);
        closing.setCashTakeoutSales(summary.cashTakeout);
        closing.setCashDeliverySales(summary.cashDelivery);

        closing.setCardVisitSales(summary.cardVisit);
        closing.setCardTakeoutSales(summary.cardTakeout);
        closing.setCardDeliverySales(summary.cardDelivery);

        closing.setVoucherSales(summary.voucherTotal);
        closing.setTotalDiscount(summary.totalDiscount);
        closing.setTotalRefund(summary.totalRefund);

        // 3. 시재 관련 금액 필드 매핑
        closing.setStartingCash(nvl(request.getStartingCash()));
        closing.setTotalExpense(nvl(request.getTotalExpense()));
        closing.setDepositAmount(nvl(request.getDepositAmount()));
        closing.setCalculatedCash(nvl(request.getCalculatedCash()));
        closing.setActualCash(nvl(request.getActualCash()));
        closing.setCarryoverCash(nvl(request.getCarryoverCash()));
        closing.setDifferenceAmount(nvl(request.getDifferenceAmount()));
        closing.setDifferenceMemo(request.getDifferenceMemo());

        // 4. 지출, 권종 목록 매핑
        mapExpenses(closing, request);
        mapDenoms(closing, request);

        // 5. 마감 여부 플래그
        closing.setClosed(true);

        // 6. 저장
        dailyClosingRepository.save(closing);
    }

    /**
     * Long 타입 값이 null 인 경우 0L 로 치환한다.
     *
     * @param value 입력 값
     * @return null 이면 0L, 아니면 원본 값
     */
    private Long nvl(Long value) {
        return Objects.requireNonNullElse(value, 0L);
    }

    /**
     * 저장 요청 DTO 에 포함된 지출 목록을 DailyClosing 엔티티에 매핑한다.
     *
     * 동작 순서
     * 1. 기존에 연결되어 있던 지출 컬렉션을 전부 비운다.
     * 2. 요청 DTO 에서 지출 항목 리스트를 하나씩 꺼내 DailyClosingExpense 엔티티로 생성한다.
     * 3. 생성된 엔티티마다 부모 관계를 설정한 뒤 DailyClosing 의 expenses 컬렉션에 추가한다.
     *
     * 주의
     * - orphanRemoval = true 설정으로 인해 컬렉션을 비우면 기존 레코드는 삭제된다.
     * - 프런트에서 내려온 리스트 전체가 이 일자의 최신 상태라는 가정으로 동작한다.
     *
     * @param closing  부모 DailyClosing 엔티티
     * @param request  저장 요청 DTO
     */
    private void mapExpenses(DailyClosing closing, DailyClosingSaveRequest request) {
        closing.clearExpenses();

        if (request.getExpenses() == null) {
            return;
        }

        int order = 1;

        for (DailyClosingExpenseDto dto : request.getExpenses()) {
            if (dto == null) {
                continue;
            }

            DailyClosingExpense expense = DailyClosingExpense.builder()
                    .description(dto.getDescription())
                    .amount(nvl(dto.getAmount()))
                    .sortOrder(order++)
                    .build();

            closing.addExpense(expense);
        }
    }

    /**
     * 저장 요청 DTO 에 포함된 권종별 시재 목록을 DailyClosing 엔티티에 매핑한다.
     *
     * 동작 순서
     * 1. 기존에 연결되어 있던 권종 컬렉션을 전부 비운다.
     * 2. 요청 DTO 에서 권종 항목 리스트를 하나씩 꺼내 DailyClosingDenom 엔티티로 생성한다.
     * 3. 생성된 엔티티마다 부모 관계를 설정한 뒤 DailyClosing 의 denoms 컬렉션에 추가한다.
     *
     * 금액(amount) 필드는 프런트에서 denomValue * count 로 계산해서 보내거나,
     * null 이 들어오면 여기서 0 으로 치환한다.
     *
     * @param closing  부모 DailyClosing 엔티티
     * @param request  저장 요청 DTO
     */
    private void mapDenoms(DailyClosing closing, DailyClosingSaveRequest request) {
        // 기존 권종 목록 제거
        closing.clearDenoms();

        if (request.getDenoms() == null) {
            return;
        }

        for (DailyClosingDenomDto dto : request.getDenoms()) {
            if (dto == null) {
                continue;
            }

            DailyClosingDenom denom = DailyClosingDenom.builder()
                    .denomValue(dto.getDenomValue())
                    .count(dto.getCount())
                    .amount(nvl(dto.getAmount()))
                    .build();

            closing.addDenom(denom);
        }
    }

    /**
     * 가맹점과 기간을 기준으로 일일 마감 요약 목록을 조회한다.
     *
     * <p>
     * - 로그인한 가맹점의 storeId 를 기준으로<br>
     * - closingDate 가 from 이상, to 이하인 DailyClosing 엔티티를 조회하고<br>
     * - 리스트 화면에서 사용하기 좋은 요약 DTO(DailyClosingSummaryDto) 리스트로 변환한다.
     * </p>
     *
     * @param storeId 조회할 가맹점 아이디
     * @param from    조회 시작 일자(포함)
     * @param to      조회 종료 일자(포함)
     * @return 기간 내 일일 마감 요약 DTO 리스트 (내림차순 정렬)
     */
    public List<DailyClosingSummaryDto> getDailyClosingHistory(
            Long storeId,
            LocalDate from,
            LocalDate to) {
        List<DailyClosing> closings =
                dailyClosingRepository.findDailyClosingHistory(storeId, from, to);

        return closings.stream()
                .map(DailyClosingSummaryDto::from)
                .toList();
    }

    /**
     * 일일 마감 상세 한 건을 조회한다.
     *
     * - storeId + closingDate 로 DailyClosing 한 건을 찾고
     * - 연결된 권종, 지출 목록을 함께 조회해서 DTO 로 변환한다.
     *
     * @param storeId 가맹점 아이디
     * @param date    마감 일자
     * @return 상세 조회 응답 DTO
     */
    public DailyClosingDetailResponse getDailyClosingDetail(Long storeId, LocalDate date) {

        DailyClosing closing = dailyClosingRepository
                .findByStoreIdAndClosingDate(storeId, date)
                .orElseThrow(() -> new IllegalStateException("해당 일자의 시재 마감 데이터가 없습니다."));

        List<DailyClosingExpense> expenses = dailyClosingRepository.findExpensesByClosing(closing);
        List<DailyClosingDenom> denoms = dailyClosingRepository.findDenomsByClosing(closing);

        return DailyClosingDetailResponse.from(closing, denoms, expenses);
    }

}
