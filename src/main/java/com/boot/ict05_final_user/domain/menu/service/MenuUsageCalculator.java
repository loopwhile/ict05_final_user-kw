package com.boot.ict05_final_user.domain.menu.service;

import com.boot.ict05_final_user.domain.inventory.entity.Material;
import com.boot.ict05_final_user.domain.menu.entity.Menu;
import com.boot.ict05_final_user.domain.menu.entity.MenuRecipe;
import com.boot.ict05_final_user.domain.menu.repository.MenuRecipeRepository;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrder;
import com.boot.ict05_final_user.domain.order.entity.CustomerOrderDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 메뉴 레시피를 바탕으로 주문 제작에 필요한 재료 총량을 계산하는 컴포넌트.
 *
 * <p>
 * 동작 개요:
 * <ol>
 *   <li>주문 내 각 {@link CustomerOrderDetail} 에서 메뉴와 수량을 취득</li>
 *   <li>메뉴별 레시피({@link MenuRecipe})를 조회하여 재료별 1개당 필요 수량을 가져옴</li>
 *   <li>주문 수량을 곱하여 재료별 총 필요 수량을 누적 집계</li>
 *   <li>가공-only 항목 등 재고와 무관한 레시피(재료가 null)는 제외</li>
 * </ol>
 * </p>
 *
 * <p>
 * 단위/정밀도:
 * <ul>
 *   <li>레시피 단위(예: g, ml, EA 등) 그대로 합산</li>
 *   <li>단위 변환(예: kg → g)은 수행하지 않음</li>
 *   <li>{@link BigDecimal} 정밀도 유지, 별도 반올림 규칙 적용하지 않음</li>
 * </ul>
 * </p>
 *
 * <p>
 * 반환 형태: {@code materialId → 총 필요 수량}. 재료 ID 기준으로 합산됩니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class MenuUsageCalculator {

    private final MenuRecipeRepository recipeRepo; // 메뉴별 레시피 조회

    /**
     * 주문 전체에 대해 재료별 필요 수량을 계산합니다.
     *
     * <p>
     * 처리 규칙:
     * <ul>
     *   <li>주문 디테일 수량이 null이면 1로 간주</li>
     *   <li>레시피의 {@code material} 이 null인 항목은 집계 제외</li>
     *   <li>동일 {@code materialId} 는 누적 합산</li>
     * </ul>
     * </p>
     *
     * @param order 계산 대상 주문
     * @return 재료 ID를 키로, 총 필요 수량을 값으로 갖는 맵
     */
    public Map<Long, BigDecimal> calcMaterialsForOrder(CustomerOrder order) {   // 주문 전체의 필요 재료 합계
        Map<Long, BigDecimal> need = new HashMap<>();                           // materialId -> 총 필요수량

        for (CustomerOrderDetail d : order.getDetails()) {                      // 주문의 각 디테일(메뉴, 수량) 순회
            Menu menu = d.getMenuIdFk();                          // 주문 디테일에 연결된 메뉴 엔티티
            if (menu == null) continue;                           // (방어) 메뉴가 없으면 스킵

            Long menuId = menu.getMenuId();                           // 메뉴 PK
            int qty = (d.getQuantity() == null) ? 1               // 주문 수량 (null이면 1로 처리)
                    : d.getQuantity();
            // 주문 수량

            List<MenuRecipe> recipe = recipeRepo.findByMenu(menu);          // 해당 메뉴의 레시피 전부 가져옴
            for (MenuRecipe r : recipe) {                                       // 레시피의 각 재료
                Material mat = r.getMaterial();                   // 레시피에 연결된 재료(재고 대상)
                if (mat == null) continue;                        // (옵션) 가공-only 항목이면 스킵

                Long materialId = mat.getId();                    // 재료 PK
                BigDecimal perOne = r.getRecipeQty();             // 1개 만들 때 필요한 수량 (필드명: recipeQty)
                BigDecimal total = perOne                         // 총 필요 수량 = 1개 필요량 × 주문 수량
                        .multiply(BigDecimal.valueOf(qty));

                // (단위 변환 필요시) 여기서 Material 기본단위로 변환 후 합산하면 됨.

                // 같은 재료가 여러 메뉴/항목에 걸쳐 나오면 누적
                need.merge(materialId, total, BigDecimal::add);                // 같은 재료가 여러 메뉴에 있으면 누적
            }
        }
        return need;                                                            // materialId -> 총 필요수량
    }
}
