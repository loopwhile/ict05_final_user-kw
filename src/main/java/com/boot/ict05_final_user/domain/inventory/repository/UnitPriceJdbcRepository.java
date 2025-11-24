package com.boot.ict05_final_user.domain.inventory.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * HQ 단가(JDBC) 조회 전용 리포지토리.
 *
 * <p>의도/역할</p>
 * <ul>
 *   <li>JPA 매핑 없이 HQ 단가 테이블(<code>unit_price</code>)로부터 필요한 값만 경량 쿼리로 조회</li>
 *   <li>현재 사용: <b>HQ 재료의 최신 SELLING 단가 1건</b> 조회</li>
 *   <li>가맹점 입고 시 “입고 단가” 기본값(HQ 판매가) 제공</li>
 * </ul>
 *
 * <p>스키마 가정</p>
 * <ul>
 *   <li>컬럼: <code>material_id_fk</code>, <code>unit_price_type</code>,
 *       <code>unit_price_selling</code>, <code>unit_price_date_from</code>, <code>unit_price_date_to</code></li>
 *   <li><code>unit_price_type</code> ∈ {'PURCHASE','SELLING'}</li>
 * </ul>
 *
 * <p>선정/정렬 규칙</p>
 * <ul>
 *   <li><code>unit_price_type = 'SELLING'</code> 필터</li>
 *   <li>유효구간 종료일 우선, 최신 우선:
 *     <code>COALESCE(unit_price_date_to, '9999-12-31')</code> DESC → <code>unit_price_date_from</code> DESC</li>
 *   <li>최상위 1건 <code>LIMIT 1</code></li>
 * </ul>
 *
 * <p>트랜잭션/성능</p>
 * <ul>
 *   <li>읽기 전용 쿼리(상위 서비스 트랜잭션 격리 수준에 의존)</li>
 *   <li>인덱스 권장:
 *     <code>(material_id_fk, unit_price_type, unit_price_date_to, unit_price_date_from)</code></li>
 * </ul>
 */
@Repository
public class UnitPriceJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UnitPriceJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * HQ 재료의 최신 <b>SELLING</b> 단가 1건을 조회.
     *
     * @param materialId HQ 재료 PK
     * @return 최신 판매 단가(Optional). 없으면 {@link Optional#empty()}
     */
    public Optional<BigDecimal> findLatestSellingPriceByMaterialId(Long materialId) {
        String sql = """
            SELECT unit_price_selling
              FROM unit_price
             WHERE material_id_fk = :materialId
               AND unit_price_type = 'SELLING'
          ORDER BY COALESCE(unit_price_date_to, TIMESTAMP('9999-12-31 00:00:00')) DESC,
                   unit_price_date_from DESC
             LIMIT 1
        """;
        return jdbc.query(
                sql,
                new MapSqlParameterSource("materialId", materialId),
                rs -> rs.next() ? Optional.of(rs.getBigDecimal(1)) : Optional.empty()
        );
    }
}
