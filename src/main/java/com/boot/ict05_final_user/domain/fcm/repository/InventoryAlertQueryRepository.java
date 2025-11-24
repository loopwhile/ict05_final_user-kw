package com.boot.ict05_final_user.domain.fcm.repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 인벤토리(F&B 재고) 알림용 쿼리 리포지토리.
 *
 * <p>QueryDSL 또는 Native SQL을 통해
 * 재고 부족 및 유통 임박 상태의 매장을 조회합니다.</p>
 *
 * <ul>
 *   <li>재고부족: 현재 수량 &lt; 임계치</li>
 *   <li>유통임박: 유통기한이 [today, today+days) 구간에 포함</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public interface InventoryAlertQueryRepository {

    /**
     * 재고 부족 매장 조회.
     *
     * @param threshold 수량 임계치
     * @return 임계치 미만 재고를 가진 매장 ID 목록
     */
    List<Long> findStoresWithLowStock(int threshold);

    /**
     * 유통임박 매장 조회.
     *
     * @param today 기준일
     * @param days  기준일로부터 포함할 일수
     * @return 유통기한이 임박한 매장 ID 목록
     */
    List<Long> findStoresWithExpireSoon(LocalDate today, int days);
}
