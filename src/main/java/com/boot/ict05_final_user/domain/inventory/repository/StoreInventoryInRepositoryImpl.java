package com.boot.ict05_final_user.domain.inventory.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * {@link StoreInventoryInRepositoryCustom} 구현체.
 *
 * <p>가이드</p>
 * <ul>
 *   <li>QueryDSL 사용 시 JPAQueryFactory 주입 후 타입 세이프 쿼리로 구현.</li>
 *   <li>Native SQL 필요 시 스키마 의존성과 인덱스 전략을 Javadoc에 명시.</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class StoreInventoryInRepositoryImpl implements StoreInventoryInRepositoryCustom {
    // 예. private final JPAQueryFactory queryFactory;

    // 커스텀 메서드 구현을 이 아래에 추가한다.
}
