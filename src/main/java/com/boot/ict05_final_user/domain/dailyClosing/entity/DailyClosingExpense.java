package com.boot.ict05_final_user.domain.dailyClosing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 일일 시재 마감 시 입력되는 현금 지출 내역 엔티티.
 * 한 번의 마감(DailyClosing) 아래에 여러 건이 연결될 수 있다.
 */
@Entity
@Table(name = "store_daily_closing_expense")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyClosingExpense {

    /** PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 부모 일일 마감 정보 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closing_id", nullable = false)
    private DailyClosing closing;

    /** 지출 내역 설명 (예: 택배 착불, 청소용품 구매 등) */
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    /** 지출 금액 */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** 정렬용 순서 값 (UI 표시 순서) */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /** 생성 일시 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


}
