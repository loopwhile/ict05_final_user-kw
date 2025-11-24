package com.boot.ict05_final_user.domain.menu.repository;

import com.boot.ict05_final_user.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository  extends JpaRepository<Menu,Long>, MenuRepositoryCustom {
}   // 메뉴 테이블(Menu 엔티티)과 관련된 데이터 저장소(Repository) 역할

// extends JpaRepository<Menu, Long> : MenuRepository가 JPA 기본 기능을 다 상속받음 = DB에서 Menu 테이블을 다루는 CRUD 기능이 자동 생성
// MenuRepositoryCustom : 직접 만든 커스텀 기능도 같이 씀. 기본적인 CRUD 말고 특별한 SQL 쿼리나 복잡한 조건 검색이 필요할 때
