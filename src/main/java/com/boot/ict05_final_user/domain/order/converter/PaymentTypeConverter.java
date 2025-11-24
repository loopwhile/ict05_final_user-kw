package com.boot.ict05_final_user.domain.order.converter;

import com.boot.ict05_final_user.domain.order.entity.PaymentType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link PaymentType} ↔ {@link String} 변환을 담당하는 JPA AttributeConverter.
 *
 * <p>
 * 저장 규칙:
 * <ul>
 *   <li>엔티티 → DB: Enum name(CARD, CASH, VOUCHER, EXTERNAL)을 저장</li>
 *   <li>DB → 엔티티: 우선 Enum name(대소문자 무시)으로 매핑을 시도하고,
 *       실패 시 한글 라벨(예: "카드", "현금")과 비교하여 매핑</li>
 * </ul>
 * </p>
 *
 * <p>
 * 적용 방식: {@code @Converter(autoApply = false)} 이므로,
 * 해당 필드에 {@code @Convert(converter = PaymentTypeConverter.class)} 를 명시적으로 적용합니다.
 * </p>
 *
 * <p><b>Null 처리</b></p>
 * <ul>
 *   <li>엔티티 → DB: {@code null} 입력 시 {@code null}</li>
 *   <li>DB → 엔티티: {@code null} 입력 시 {@code null}</li>
 * </ul>
 *
 * <p><b>예외</b></p>
 * <ul>
 *   <li>Enum name/한글 라벨 어느 쪽에도 매핑되지 않으면 {@link IllegalArgumentException} 발생</li>
 * </ul>
 */
@Converter(autoApply = false)
public class PaymentTypeConverter implements AttributeConverter<PaymentType, String> {

    /** 엔티티의 결제수단 Enum을 DB 저장용 문자열(Enum name)로 변환합니다. */
    @Override
    public String convertToDatabaseColumn(PaymentType attribute) {
        return attribute == null ? null : attribute.name();
    }

    /** DB의 문자열을 결제수단 Enum으로 변환합니다. (Enum name → 라벨 순으로 매핑 시도) */
    @Override
    public PaymentType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;

        // 1) enum name 기준 (CARD / card 등)
        try {
            return PaymentType.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException ignore) {
        }

        // 2) 한글 라벨 기준 ("카드", "현금" 등)도 허용
        for (PaymentType type : PaymentType.values()) {
            if (type.getLabel().equals(dbData.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown PaymentType: " + dbData);
    }
}
