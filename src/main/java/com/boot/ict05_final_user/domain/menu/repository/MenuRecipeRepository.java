package com.boot.ict05_final_user.domain.menu.repository;

import com.boot.ict05_final_user.domain.menu.entity.Menu;
import com.boot.ict05_final_user.domain.menu.entity.MenuRecipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRecipeRepository extends JpaRepository<MenuRecipe, Long> {

    // 메뉴 기준 레시피 전체 조회
    List<MenuRecipe> findByMenu(Menu menu);
}
