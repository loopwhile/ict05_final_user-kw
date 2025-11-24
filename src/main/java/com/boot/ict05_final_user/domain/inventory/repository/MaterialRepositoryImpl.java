package com.boot.ict05_final_user.domain.inventory.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * {@link MaterialRepositoryCustom} 구현체.
 *
 * <p>가이드</p>
 * <ul>
 *   <li>QueryDSL 사용 시: JPAQueryFactory 주입 후, 타입 세이프 쿼리로 구현한다.</li>
 *   <li>Native SQL이 필요한 경우: 스키마 의존도를 주석으로 명확히 남긴다.</li>
 *   <li>성능 튜닝 포인트(인덱스 힌트/배치 크기 등)는 메서드 단 Javadoc에 이유와 근거를 남긴다.</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class MaterialRepositoryImpl implements MaterialRepositoryCustom {
    // 예. private final JPAQueryFactory queryFactory;

    // 커스텀 메서드 구현을 이 아래에 추가한다.
}
