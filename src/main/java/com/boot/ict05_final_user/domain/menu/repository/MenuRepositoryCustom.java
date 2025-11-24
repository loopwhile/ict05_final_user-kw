package com.boot.ict05_final_user.domain.menu.repository;

import com.boot.ict05_final_user.domain.menu.dto.MenuDetailDTO;
import com.boot.ict05_final_user.domain.menu.dto.MenuListDTO;
import com.boot.ict05_final_user.domain.menu.dto.MenuSearchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MenuRepositoryCustom {

    // 로그인한 가맹점(storeId) 기준 메뉴 목록
    Page<MenuListDTO> listMenu(Long storeId, MenuSearchDTO dto, Pageable pageable);

    // 상세 정보 조회
    MenuDetailDTO getMenuDetail(Long menuId);
}
// MenuRepositoryCustom : MenuRepository에 직접 기능을 추가
// Page<MenuListDTO> : 리턴 타입 - MenuListDTO를 여러 개 묶어서 한 페이지 단위로 반환
// listMenu(Pageable pageable) : 메서드 이름 + 매개변수 - Pageable pageable : 스프링에서 제공하는 페이지 정보 객체