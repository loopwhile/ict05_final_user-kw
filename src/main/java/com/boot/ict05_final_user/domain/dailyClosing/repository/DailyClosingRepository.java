package com.boot.ict05_final_user.domain.dailyClosing.repository;

import com.boot.ict05_final_user.domain.dailyClosing.entity.DailyClosing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일일 시재 마감 헤더를 다루는 레포지토리.
 * 기본 JPA 기능 + 주문 집계용 커스텀 메서드를 함께 제공한다.
 */
public interface DailyClosingRepository
        extends JpaRepository<DailyClosing, Long>, DailyClosingRepositoryCustom {

    /**
     * 점포와 마감일 기준으로 일일 마감 엔티티를 조회한다.
     *
     * @param storeId     가맹점 아이디
     * @param closingDate 마감일
     * @return 일일 마감 정보
     */
    Optional<DailyClosing> findByStoreIdAndClosingDate(Long storeId, LocalDate closingDate);

    /**
     * 가맹점과 기간을 기준으로 일일 마감 엔티티 목록을 조회한다.
     *
     * <p>
     * - closingDate 가 from 이상, to 이하인 데이터를 모두 조회한다.<br>
     * - 결과는 closingDate 기준 내림차순으로 정렬된다.
     * </p>
     *
     * @param storeId 가맹점 아이디
     * @param from    조회 시작 일자(포함)
     * @param to      조회 종료 일자(포함)
     * @return 기간 내 일일 마감 엔티티 목록
     */
    List<DailyClosing> findDailyClosingHistory(Long storeId, LocalDate from, LocalDate to);

}
