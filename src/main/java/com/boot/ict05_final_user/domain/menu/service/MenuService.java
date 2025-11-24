package com.boot.ict05_final_user.domain.menu.service;

import com.boot.ict05_final_user.domain.menu.dto.MenuDetailDTO;
import com.boot.ict05_final_user.domain.menu.dto.MenuListDTO;
import com.boot.ict05_final_user.domain.menu.dto.MenuSearchDTO;
import com.boot.ict05_final_user.domain.menu.entity.StoreMenu;
import com.boot.ict05_final_user.domain.menu.entity.StoreMenuSoldout;
import com.boot.ict05_final_user.domain.menu.repository.MenuRepository;
import com.boot.ict05_final_user.domain.menu.repository.StoreMenuRepository;
import com.boot.ict05_final_user.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메뉴 도메인의 비즈니스 로직을 담당하는 서비스.
 *
 * <p>
 * - 가맹점 기준의 메뉴 목록 조회(검색/필터/페이징/정렬 연계)<br>
 * - 단일 메뉴 상세 조회<br>
 * - 가맹점 단위 품절 상태 갱신
 * </p>
 *
 * <p><b>트랜잭션 경계</b></p>
 * <ul>
 *   <li>클래스 레벨 {@link Transactional}: 기본적으로 쓰기 트랜잭션</li>
 *   <li>읽기 전용 작업이 많다면 메서드 단위로 {@code readOnly=true} 적용을 고려</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional      // DB 작업은 하나의 트랜잭션 단위로 처리 - 중간에 에러 나면 모두 취소
@Slf4j  // 로그를 찍을 수 있음
public class MenuService {

    private final MenuRepository menuRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final StoreRepository storeRepository;

    /**
     * 로그인한 가맹점 기준으로 메뉴 목록을 페이지 단위로 조회합니다.
     *
     * <p>검색/필터 값({@link MenuSearchDTO})과 페이징/정렬({@link Pageable})을 그대로 위임하여
     * DTO 페이지를 반환합니다.</p>
     *
     * @param storeId       가맹점 ID
     * @param menuSearchDTO 검색/필터 파라미터(Null 허용)
     * @param pageable      페이지 정보(번호, 크기, 정렬)
     * @return 페이징 처리된 메뉴 리스트 DTO
     */
    public Page<MenuListDTO> selectAllStoreMenu(Long storeId, MenuSearchDTO menuSearchDTO, Pageable pageable) {
        var menus = menuRepository.listMenu(storeId, menuSearchDTO, pageable);

        log.info("storeId={}, rows={}", storeId, menus.getNumberOfElements());
        menus.getContent().forEach(m ->
                log.info("id={}, name={}", m.getMenuId(), m.getMenuName())
        );

        return menus;
    }

    /**
     * 단일 메뉴의 상세 정보를 조회합니다.
     *
     * <p>레시피/재료 조인을 통해 재료명 목록을 포함한 상세 DTO를 반환합니다.</p>
     *
     * @param menuId 메뉴 ID
     * @return 메뉴 상세 DTO(없으면 {@code null})
     */
    @Transactional(readOnly = true)
    public MenuDetailDTO selectStoreMenuDetail(Long menuId) {
        return menuRepository.getMenuDetail(menuId);
    }

    /**
     * 가맹점 단위 메뉴 품절 상태를 갱신합니다.
     *
     * <p>
     * - 기존 매핑이 없으면 {@link StoreMenu}를 생성한 뒤 기본값을 적용하여 저장합니다.<br>
     * - 이후 품절 상태를 요청 상태로 갱신하며, JPA Dirty Checking으로 반영됩니다.
     * </p>
     *
     * @param storeId 가맹점 ID
     * @param menuId  메뉴 ID
     * @param status  변경할 품절 상태
     * @throws IllegalArgumentException 가맹점 또는 메뉴가 존재하지 않는 경우
     */
    public void updateSoldOutStatus(Long storeId, Long menuId, StoreMenuSoldout status) {
        StoreMenu storeMenu = storeMenuRepository
                .findByStoreIdAndMenuId(storeId, menuId)
                .orElseGet(() -> {
                    var store = storeRepository.findById(storeId)
                            .orElseThrow(() -> new IllegalArgumentException("store not found: " + storeId));
                    var menu = menuRepository.findById(menuId)
                            .orElseThrow(() -> new IllegalArgumentException("menu not found: " + menuId));

                    StoreMenu sm = new StoreMenu();
                    sm.setStore(store);
                    sm.setMenu(menu);
                    sm.setStoreMenuSoldout(StoreMenuSoldout.ON_SALE); // 기본값
                    return storeMenuRepository.save(sm);
                });

        storeMenu.setStoreMenuSoldout(status);
    }
}
