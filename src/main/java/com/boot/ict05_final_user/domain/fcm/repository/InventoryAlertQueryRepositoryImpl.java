package com.boot.ict05_final_user.domain.fcm.repository;

import com.boot.ict05_final_user.domain.inventory.entity.InventoryStatus;
import com.boot.ict05_final_user.domain.inventory.entity.QStoreInventory;
import com.boot.ict05_final_user.domain.inventory.entity.QStoreMaterial;
import com.boot.ict05_final_user.domain.inventory.entity.QStoreInventoryBatch;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * {@link InventoryAlertQueryRepository} 구현체.
 *
 * <p>가맹점의 재고부족 및 유통기한 임박 재료를 탐색하여
 * 알림 발송 대상 매장 목록을 반환합니다.</p>
 *
 * <ul>
 *   <li>QueryDSL 기반의 읽기 전용 쿼리 수행</li>
 *   <li>MySQL/MariaDB 환경에서 filesort 방지를 위해 {@code ORDER BY NULL} 사용</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
@Repository
@RequiredArgsConstructor
public class InventoryAlertQueryRepositoryImpl implements InventoryAlertQueryRepository {

    private final JPAQueryFactory query;

    /**
     * 재고 부족 매장 목록 조회.
     *
     * <p>조건:
     * <ul>
     *   <li>quantity &lt; threshold</li>
     *   <li>status = LOW 또는 SHORTAGE</li>
     *   <li>optimalQuantity 존재 시 quantity &lt; optimalQuantity</li>
     * </ul>
     * </p>
     *
     * @param threshold 재고 부족 임계치
     * @return 재고 부족 매장 ID 목록
     */
    @Override
    public List<Long> findStoresWithLowStock(int threshold) {
        QStoreInventory si = QStoreInventory.storeInventory;
        QStoreMaterial sm = QStoreMaterial.storeMaterial;
        QStoreInventoryBatch sib = QStoreInventoryBatch.storeInventoryBatch;

        BigDecimal th = BigDecimal.valueOf(threshold);

        BooleanExpression cond =
                si.quantity.lt(th)
                        .or(si.status.eq(InventoryStatus.LOW))
                        .or(si.status.eq(InventoryStatus.SHORTAGE))
                        .or(si.optimalQuantity.isNotNull().and(si.quantity.lt(si.optimalQuantity)));

        return query
                .select(si.store.id)
                .from(si)
                .where(cond)
                .groupBy(si.store.id)
                .orderBy(orderByNull())
                .setHint("org.hibernate.readOnly", true)
                .setHint("org.hibernate.flushMode", "COMMIT")
                .setHint("jakarta.persistence.query.timeout", 3000)
                .fetch();
    }

    /**
     * 유통임박 매장 목록 조회.
     *
     * <p>조건:
     * <ul>
     *   <li>expiration_date ∈ [today, today+days)</li>
     * </ul>
     * </p>
     *
     * @param today 기준일
     * @param days  기준일로부터 탐색 범위(일 단위)
     * @return 유통임박 매장 ID 목록
     */
    @Override
    public List<Long> findStoresWithExpireSoon(LocalDate today, int days) {
        QStoreInventory si = QStoreInventory.storeInventory;
        QStoreMaterial sm = QStoreMaterial.storeMaterial;
        QStoreInventoryBatch sib = QStoreInventoryBatch.storeInventoryBatch;

        LocalDate start = today;
        LocalDate endExclusive = today.plusDays(days);

        BooleanExpression cond =
                sib.expirationDate.isNotNull()
                        .and(sib.expirationDate.goe(start))
                        .and(sib.expirationDate.lt(endExclusive));

        return query
                .select(si.store.id)
                .from(si)
                .join(si.storeMaterial, sm)
                .where(cond)
                .groupBy(si.store.id)
                .orderBy(orderByNull())
                .setHint("org.hibernate.readOnly", true)
                .setHint("org.hibernate.flushMode", "COMMIT")
                .setHint("jakarta.persistence.query.timeout", 3000)
                .fetch();
    }

    /**
     * ORDER BY NULL (MySQL/MariaDB filesort 회피용).
     *
     * @return 정렬 미적용용 OrderSpecifier
     */
    private OrderSpecifier<Integer> orderByNull() {
        return new OrderSpecifier<>(Order.ASC, Expressions.nullExpression());
    }
}
