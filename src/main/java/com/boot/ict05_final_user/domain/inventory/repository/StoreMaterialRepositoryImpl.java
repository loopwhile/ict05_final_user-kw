package com.boot.ict05_final_user.domain.inventory.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * {@link StoreMaterialRepositoryCustom} 구현체(현재 빈 구현).
 *
 * <p>가이드</p>
 * <ul>
 *   <li>QueryDSL 사용 시 JPAQueryFactory 주입</li>
 *   <li>성능/인덱스 전략은 각 메서드 Javadoc에 명시</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class StoreMaterialRepositoryImpl implements StoreMaterialRepositoryCustom {
    // 예. private final JPAQueryFactory queryFactory;
}
