package com.boot.ict05_final_user.domain.order.converter;

import com.boot.ict05_final_user.domain.order.entity.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link OrderStatus} ↔ {@link String} 변환을 담당하는 JPA AttributeConverter.
 *
 * <p>
 * - DB 컬럼: 상태의 한글 라벨(예: "준비중")을 저장<br>
 * - 엔티티: {@link OrderStatus} enum을 사용
 * </p>
 *
 * <p>
 * 적용 방식: {@code @Converter(autoApply = false)} 이므로,
 * 엔티티 필드에 {@code @Convert(converter = OrderStatusConverter.class)} 로 명시 적용합니다.
 * </p>
 *
 * <p><b>Null 처리</b></p>
 * <ul>
 *   <li>엔티티 → DB: {@code null} 입력 시 {@code null} 반환</li>
 *   <li>DB → 엔티티: {@code null} 입력 시 {@code null} 반환</li>
 * </ul>
 */
@Converter(autoApply = false)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    /**
     * 엔티티의 {@link OrderStatus} 값을 DB 저장용 문자열(한글 라벨)로 변환합니다.
     *
     * @param attribute 엔티티의 상태 enum 값(Null 허용)
     * @return DB 저장용 한글 라벨 문자열 또는 {@code null}
     */
    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        return attribute == null ? null : attribute.getDbValue(); // "준비중" 같은 한글
    }

    /**
     * DB의 상태 문자열(한글 라벨)을 엔티티의 {@link OrderStatus} enum으로 변환합니다.
     *
     * @param dbData DB 컬럼의 한글 라벨 문자열(Null 허용)
     * @return 매핑된 enum 값 또는 {@code null}
     */
    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OrderStatus.from(dbData);  // 한글 -> enum
    }
}
