package com.boot.ict05_final_user.domain.menu.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link StoreMenuSoldout} ↔ {@link Integer} 매핑을 담당하는 JPA AttributeConverter.
 *
 * <p>
 * - DB 컬럼: 정수 코드(0=ON_SALE, 1=SOLD_OUT)<br>
 * - 엔티티: {@link StoreMenuSoldout} Enum
 * </p>
 *
 * <p><b>Null 처리 규칙</b></p>
 * <ul>
 *   <li>엔티티 → DB: {@code null} 이면 0(ON_SALE)로 저장</li>
 *   <li>DB → 엔티티: {@code null} 이면 {@link StoreMenuSoldout#ON_SALE} 반환</li>
 * </ul>
 */
@Converter(autoApply = true)
public class StoreMenuSoldoutConverter implements AttributeConverter<StoreMenuSoldout, Integer> {

    /**
     * 엔티티 필드를 DB 정수 코드로 변환합니다.
     *
     * @param attribute {@link StoreMenuSoldout} 값(Null 허용)
     * @return 정수 코드(Null 입력 시 0)
     */
    @Override
    public Integer convertToDatabaseColumn(StoreMenuSoldout attribute) {
        if (attribute == null) return 0;
        return attribute.getCode();
    }

    /**
     * DB 정수 코드를 엔티티 Enum으로 변환합니다.
     *
     * @param dbData DB의 정수 코드(Null 허용)
     * @return 매핑된 {@link StoreMenuSoldout} 값(Null 입력 시 ON_SALE)
     */
    @Override
    public StoreMenuSoldout convertToEntityAttribute(Integer dbData) {
        if (dbData == null) return StoreMenuSoldout.ON_SALE;
        return StoreMenuSoldout.fromCode(dbData);
    }

}
