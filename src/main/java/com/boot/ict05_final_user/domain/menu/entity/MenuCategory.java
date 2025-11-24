package com.boot.ict05_final_user.domain.menu.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

/**
 * 메뉴 카테고리 엔티티.
 *
 * <p>대/중/소 3단계까지 확장 가능한 자기 참조 트리 구조를 가집니다.
 * 각 카테고리는 선택적으로 상위 카테고리(부모)를 가질 수 있습니다.</p>
 */
@Entity     // DB 테이블이랑 연결
@Table(name = "menu_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder    // 객체를 만드는 방법을 제공
@Schema(description = "메뉴 카테고리 엔티티")
public class MenuCategory {

    @Id     // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY)     // id 숫자를 자동 증가
    @Column(name = "menu_category_id")
    @Schema(description = "카테고리 ID",accessMode = Schema.AccessMode.READ_ONLY)
    private Long menuCategoryId;

    /** 상위 카테고리 (대중소 구조 지원) */
    @ManyToOne(fetch = FetchType.LAZY)      // 1개의 자식 카테고리(소)는 1개의 부모 카테고리(상위)를 참조 / LAZY(지연로딩): 진짜 필요할 때만 DB에서 부모 가져옴
    @JoinColumn(name = "menu_category_parent_id")
    @Schema(description = "상위(부모) 카테고리", implementation = MenuCategory.class, nullable = true)
    private MenuCategory menuCategoryParentId;

    /** 카테고리명 */
    @Column(name = "menu_category_name")
    @Schema(description = "카테고리명", nullable = false)
    private String menuCategoryName;

    /** 단계 구분(대=1, 중=2, 소=3) */
    @Column(name = "menu_category_level")
    @Schema(description = "카테고리 레벨(대=1, 중=2, 소=3)")
    private Short menuCategoryLevel;

}
