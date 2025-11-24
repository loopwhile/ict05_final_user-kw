package com.boot.ict05_final_user.domain.order.converter;

import com.boot.ict05_final_user.domain.order.entity.OrderType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link OrderType} ↔ {@link String} 변환을 담당하는 JPA AttributeConverter.
 *
 * <p>
 * - DB 컬럼에는 Enum의 DB 표현값(예: 코드/키 문자열)을 저장하고,<br>
 * - 엔티티 필드에는 {@link OrderType} Enum을 사용합니다.
 * </p>
 *
 * <p>
 * 적용 방식: {@code @Converter(autoApply = false)} 이므로,
 * 엔티티 필드에 {@code @Convert(converter = OrderTypeConverter.class)}로 명시 적용합니다.
 * </p>
 *
 * <p><b>Null 처리</b></p>
 * <ul>
 *   <li>엔티티 → DB: {@code null} 입력 시 {@code null} 반환</li>
 *   <li>DB → 엔티티: {@code null} 입력 시 {@code null} 반환</li>
 * </ul>
 */
@Converter(autoApply = false)
public class OrderTypeConverter implements AttributeConverter<OrderType, String> {

    /**
     * 엔티티의 {@link OrderType} 값을 DB 저장용 문자열로 변환합니다.
     *
     * @param attribute 엔티티의 주문 유형 enum 값(Null 허용)
     * @return DB 저장용 문자열 또는 {@code null}
     */
    @Override
    public String convertToDatabaseColumn(OrderType attribute) {
        return attribute == null ? null : attribute.getDbValue(); // "VISIT" 등
    }

    /**
     * DB의 문자열 값을 엔티티의 {@link OrderType} enum으로 변환합니다.
     *
     * @param dbData DB 컬럼의 문자열(Null 허용)
     * @return 매핑된 enum 값 또는 {@code null}
     */
    @Override
    public OrderType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OrderType.from(dbData);
    }
}
