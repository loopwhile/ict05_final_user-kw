package com.boot.ict05_final_user.domain.menu.entity;

import com.boot.ict05_final_user.domain.store.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

/**
 * 가맹점별 메뉴 설정(StoreMenu) 엔티티.
 *
 * <p>특정 매장에 귀속된 메뉴와 그 품절 상태를 관리합니다.
 * (store × menu) 조합은 유니크하며, 품절 상태는 매장 단위로 독립적으로 관리됩니다.</p>
 */
@Entity
@Table(
        name = "store_menu",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_store_menu_store_menu",
                        columnNames = {"store_id_fk", "menu_id_fk"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "가맹점별 메뉴 설정 엔티티")
public class StoreMenu {

    /** 가맹점 메뉴 시퀀스 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_menu_id")
    @Schema(description = "가맹점-메뉴 매핑 ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long storeMenuId;

    /** 메뉴 FK */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id_fk")
    @Schema(description = "연결 메뉴", implementation = Menu.class, nullable = false)
    private Menu menu;

    /** 매장 FK */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id_fk")
    @Schema(description = "연결 매장", implementation = Store.class, nullable = false)
    private Store store;

    /** 품절 상태 */
    @Builder.Default
    @Column(name = "store_menu_soldout")
    @Convert(converter = StoreMenuSoldoutConverter.class)
    @Schema(description = "가맹점 단위 품절 상태", implementation = StoreMenuSoldout.class, nullable = false)
    private StoreMenuSoldout storeMenuSoldout = StoreMenuSoldout.ON_SALE;

    /**
     * 품절/해제 상태를 토글합니다.
     *
     * <p>ON_SALE ⇄ SOLD_OUT 전환.</p>
     */
    public void toggleSoldOut() {
        this.storeMenuSoldout =
                (this.storeMenuSoldout == StoreMenuSoldout.ON_SALE)
                        ? StoreMenuSoldout.SOLD_OUT
                        : StoreMenuSoldout.ON_SALE;
    }

}
