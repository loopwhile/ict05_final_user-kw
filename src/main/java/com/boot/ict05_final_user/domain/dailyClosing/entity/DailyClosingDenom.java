package com.boot.ict05_final_user.domain.dailyClosing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 일일 시재 마감 시 입력되는 권종별 시재 정보 엔티티.
 * 예: 5만원권 3장, 1만원권 4장, 500원 10개 등.
 */
@Entity
@Table(name = "store_daily_closing_denom")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyClosingDenom {

    /** PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 부모 일일 마감 정보 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closing_id", nullable = false)
    private DailyClosing closing;

    /** 권종 금액 (예: 50000, 10000, 5000, 1000, 500, 100, 50, 10) */
    @Column(name = "denom_value", nullable = false)
    private Integer denomValue;

    /** 해당 권종 개수 */
    @Column(name = "count", nullable = false)
    private Integer count;

    /** 금액 합계 (denom_value * count) */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** 생성 일시 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
