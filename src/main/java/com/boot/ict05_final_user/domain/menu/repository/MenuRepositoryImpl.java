package com.boot.ict05_final_user.domain.menu.repository;

import com.boot.ict05_final_user.domain.menu.dto.MenuDetailDTO;
import com.boot.ict05_final_user.domain.menu.dto.MenuListDTO;
import com.boot.ict05_final_user.domain.menu.dto.MenuSearchDTO;
import com.boot.ict05_final_user.domain.menu.entity.MenuShow;
import com.boot.ict05_final_user.domain.menu.entity.QMenu;
import com.boot.ict05_final_user.domain.menu.entity.QMenuCategory;
import com.boot.ict05_final_user.domain.menu.entity.QMenuRecipe;
import com.boot.ict05_final_user.domain.menu.entity.QStoreMenu;
import com.boot.ict05_final_user.domain.inventory.entity.QMaterial;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * {@link MenuRepositoryCustom} 구현체.
 *
 * <p>QueryDSL을 사용하여 가맹점 기준의 메뉴 목록/상세 조회를 제공합니다.
 * 검색/필터/정렬/페이징을 지원하며, 품절 상태는 가맹점(Store) 단위로 적용됩니다.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MenuRepositoryImpl implements MenuRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 로그인한 가맹점(storeId) 기준으로 메뉴 목록을 조회합니다.
     *
     * <ul>
     *   <li>검색: 이름(대/소문자 무시)</li>
     *   <li>카테고리 필터: menuCategoryId 우선, 없으면 categoryName 포함 검색</li>
     *   <li>판매 상태: 파라미터 없으면 기본으로 {@code MenuShow.SHOW}</li>
     *   <li>품절 상태: 가맹점별 {@code StoreMenu} 조인 후 필터</li>
     *   <li>정렬: {@link Pageable#getSort()} → QueryDSL OrderSpecifier로 변환</li>
     *   <li>페이징: ID 사전 조회 후 실제 행 조회(두 단계)</li>
     * </ul>
     *
     * @param storeId  가맹점 ID
     * @param dto      검색/필터 파라미터(Null 허용)
     * @param pageable 페이징/정렬 정보
     * @return 메뉴 목록 페이지
     */
    @Override
    public Page<MenuListDTO> listMenu(Long storeId, MenuSearchDTO dto, Pageable pageable) {
        if (dto == null) dto = new MenuSearchDTO();

        QMenu menu = QMenu.menu;
        QMenuCategory category = QMenuCategory.menuCategory;
        QStoreMenu storeMenu = QStoreMenu.storeMenu;

        // WHERE 조건 (menu + storeMenu 기준)
        BooleanExpression where = andAll(
                eqNameOrInfo(dto, menu),
                eqCategory(dto, menu, category)
        );

        // 본사 메뉴 SHOW/HIDE 필터
        if (dto.getMenuShow() != null) {
            where = andAll(where, menu.menuShow.eq(dto.getMenuShow()));
        } else {
            where = andAll(where, menu.menuShow.eq(MenuShow.SHOW));
        }

        // 품절 필터 (가맹점별)
        where = andAll(where, eqSoldOutStatus(dto, storeMenu));

        // 정렬 (기본: menuId DESC)
        Sort sort = (pageable.getSort().isSorted())
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "menuId");

        // 1) page 대상 menuId 먼저 조회
        List<Long> pageIds = queryFactory
                .select(menu.menuId)
                .from(menu)
                .leftJoin(menu.menuCategory, category)
                .leftJoin(storeMenu)
                .on(storeMenu.menu.eq(menu)
                        .and(storeMenu.store.id.eq(storeId)))  // 로그인한 가맹점 기준
                .where(where)
                .orderBy(toOrderSpec(menu, sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        log.info("[listMenu] storeId={}, pageIds size={}, ids={}", storeId, pageIds.size(), pageIds);

        if (pageIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 2) 실제 행 조회 (메뉴 + 가맹점별 품절 상태)
        var rows = queryFactory
                .select(Projections.tuple(
                        menu.menuId,
                        menu.menuName,
                        menu.menuNameEnglish,
                        category.menuCategoryId,
                        category.menuCategoryName,
                        menu.menuPrice,
                        menu.menuKcal,
                        menu.menuInformation,
                        menu.menuCode,
                        storeMenu.storeMenuSoldout,
                        menu.menuShow
                ))
                .from(menu)
                .leftJoin(menu.menuCategory, category)
                .leftJoin(storeMenu)
                .on(storeMenu.menu.eq(menu)
                        .and(storeMenu.store.id.eq(storeId)))  // 로그인한 가맹점 기준
                .where(menu.menuId.in(pageIds))
                .orderBy(toOrderSpec(menu, sort))
                .fetch();

        log.info("[listMenu] rows fetched={}", rows.size());

        Map<Long, MenuListDTO> map = new LinkedHashMap<>();
        for (var t : rows) {
            Long id = t.get(menu.menuId);
            MenuListDTO v = map.computeIfAbsent(id, k -> {
                MenuListDTO d = new MenuListDTO();
                d.setMenuId(t.get(menu.menuId));
                d.setMenuName(t.get(menu.menuName));
                d.setMenuNameEnglish(t.get(menu.menuNameEnglish));
                d.setMenuCategoryId(t.get(category.menuCategoryId));
                d.setMenuCategoryName(t.get(category.menuCategoryName));
                d.setMenuPrice(t.get(menu.menuPrice));
                d.setMenuKcal(t.get(menu.menuKcal));
                d.setMenuInformation(t.get(menu.menuInformation));
                d.setMenuCode(t.get(menu.menuCode));
                d.setStoreMenuSoldout(t.get(storeMenu.storeMenuSoldout));
                d.setMenuShow(t.get(menu.menuShow));
                return d;
            });
        }
        List<MenuListDTO> content = new ArrayList<>(map.values());

        // 3) Count (storeId 기준)
        Long total = queryFactory
                .select(menu.menuId.count())
                .from(menu)
                .leftJoin(menu.menuCategory, category)
                .leftJoin(storeMenu)
                .on(storeMenu.menu.eq(menu)
                        .and(storeMenu.store.id.eq(storeId)))
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 이름/설명 검색 조건을 생성합니다.
     *
     * <p>현재 구현은 {@code type}이 {@code name}이거나 기타인 경우 모두
     * {@code menu.menuName.containsIgnoreCase(s)}만 적용합니다.</p>
     *
     * @param dto  검색 DTO
     * @param menu QMenu
     * @return BooleanExpression 또는 null
     */
    private BooleanExpression eqNameOrInfo(MenuSearchDTO dto, QMenu menu) {
        String kw = dto.getS();
        if (!StringUtils.hasText(kw)) return null;

        String type = Optional.ofNullable(dto.getType()).orElse("all");
        return switch (type) {
            case "name" -> menu.menuName.containsIgnoreCase(kw);
            default -> menu.menuName.containsIgnoreCase(kw);
        };
    }

    /**
     * 카테고리 검색 조건을 생성합니다.
     *
     * <p>우선순위: menuCategoryId → categoryName 포함 검색 → 조건 없음</p>
     *
     * @param dto      검색 DTO
     * @param menu     QMenu
     * @param category QMenuCategory
     * @return BooleanExpression 또는 null
     */
    private BooleanExpression eqCategory(MenuSearchDTO dto, QMenu menu, QMenuCategory category) {
        // 1) menuCategoryId 우선 사용
        if (dto.getMenuCategoryId() != null && dto.getMenuCategoryId() != 0) {
            return menu.menuCategory.menuCategoryId.eq(dto.getMenuCategoryId());
        }
        // 2) 없으면 카테고리 이름으로 필터
        if (StringUtils.hasText(dto.getCategoryName())) {
            return category.menuCategoryName.containsIgnoreCase(dto.getCategoryName());
        }
        return null;
    }

    /**
     * 가맹점 품절 상태 필터 조건을 생성합니다.
     *
     * @param dto       검색 DTO
     * @param storeMenu QStoreMenu
     * @return BooleanExpression 또는 null
     */
    private BooleanExpression eqSoldOutStatus(MenuSearchDTO dto, QStoreMenu storeMenu) {
        if (dto.getStoreMenuSoldout() == null) return null;
        return storeMenu.storeMenuSoldout.eq(dto.getStoreMenuSoldout());
    }

    /**
     * 여러 조건을 AND로 결합합니다.
     *
     * @param exps 결합할 조건들
     * @return 결합된 표현식 또는 null
     */
    private BooleanExpression andAll(BooleanExpression... exps) {
        BooleanExpression result = null;
        for (BooleanExpression exp : exps) {
            if (exp == null) continue;
            result = (result == null) ? exp : result.and(exp);
        }
        return result;
    }

    /**
     * {@link Sort} 정보를 QueryDSL의 {@code OrderSpecifier[]}로 변환합니다.
     *
     * <p>지원 필드: menuId, menuName, menuPrice, menuKcal (기타는 menuId DESC)</p>
     *
     * @param menu QMenu
     * @param sort Spring Data Sort
     * @return OrderSpecifier 배열
     */
    private com.querydsl.core.types.OrderSpecifier<?>[] toOrderSpec(QMenu menu, Sort sort) {
        return sort.stream()
                .map(order -> {
                    com.querydsl.core.types.Order direction = order.isAscending()
                            ? com.querydsl.core.types.Order.ASC
                            : com.querydsl.core.types.Order.DESC;
                    return switch (order.getProperty()) {
                        case "menuId" -> new com.querydsl.core.types.OrderSpecifier<>(direction, menu.menuId);
                        case "menuName" -> new com.querydsl.core.types.OrderSpecifier<>(direction, menu.menuName);
                        case "menuPrice" -> new com.querydsl.core.types.OrderSpecifier<>(direction, menu.menuPrice);
                        case "menuKcal" -> new com.querydsl.core.types.OrderSpecifier<>(direction, menu.menuKcal);
                        default ->
                                new com.querydsl.core.types.OrderSpecifier<>(com.querydsl.core.types.Order.DESC, menu.menuId);
                    };
                })
                .toArray(com.querydsl.core.types.OrderSpecifier[]::new);
    }

    /**
     * 단일 메뉴의 상세 정보를 조회합니다.
     *
     * <p>레시피/재료를 조인해 재료 이름 리스트를 구성하며,
     * 중복을 제거하고 입력 순서를 보존합니다.</p>
     *
     * @param menuId 메뉴 ID
     * @return 메뉴 상세 DTO(없으면 null)
     */
    @Override
    public MenuDetailDTO getMenuDetail(Long menuId) {
        QMenu menu = QMenu.menu;
        QMenuCategory category = QMenuCategory.menuCategory;
        QMenuRecipe recipe = QMenuRecipe.menuRecipe;
        QMaterial material = QMaterial.material;

        var rows = queryFactory
                .select(Projections.tuple(
                        menu.menuId,
                        menu.menuName,
                        menu.menuNameEnglish,
                        category.menuCategoryId,
                        category.menuCategoryName,
                        menu.menuPrice,
                        menu.menuKcal,
                        menu.menuInformation,
                        menu.menuCode,
                        menu.menuShow,
                        material.name
                ))
                .from(menu)
                .leftJoin(menu.menuCategory, category)
                .leftJoin(menu.recipe, recipe)
                .leftJoin(recipe.material, material)
                .where(menu.menuId.eq(menuId))
                .fetch();

        log.info("[getMenuDetail] id={}, rows={}", menuId, rows.size());

        if (rows.isEmpty()) {
            return null;
        }

        var first = rows.get(0);

        MenuDetailDTO dto = new MenuDetailDTO();
        dto.setMenuId(first.get(menu.menuId));
        dto.setMenuName(first.get(menu.menuName));
        dto.setMenuNameEnglish(first.get(menu.menuNameEnglish));
        dto.setMenuCategoryId(first.get(category.menuCategoryId));
        dto.setMenuCategoryName(first.get(category.menuCategoryName));
        dto.setMenuPrice(first.get(menu.menuPrice));
        dto.setMenuKcal(first.get(menu.menuKcal));
        dto.setMenuInformation(first.get(menu.menuInformation));
        dto.setMenuCode(first.get(menu.menuCode));
        dto.setMenuShow(first.get(menu.menuShow));

        // 재료 이름 목록만 추출해서 DTO에 세팅
        Set<String> ingredientNames = new LinkedHashSet<>();
        for (var t : rows) {
            String name = t.get(material.name);
            if (name != null && !name.isBlank()) {
                ingredientNames.add(name);
            }
        }
        dto.setIngredientNames(new ArrayList<>(ingredientNames));

        return dto;
    }
}
