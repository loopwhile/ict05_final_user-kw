package com.boot.ict05_final_user.domain.staff.controller;

import com.boot.ict05_final_user.domain.staff.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

/**
 * 직원 관리 화면 컨트롤러.
 *
 * <p>
 * - 직원 목록 페이지<br>
 * - 직원 등록 페이지<br>
 * - 직원 수정 페이지<br>
 * - 기타 화면 렌더링 담당
 * </p>
 *
 * Spring MVC 기반 HTML 렌더링용 컨트롤러이며,
 * Swagger API 문서 대상이 아니다.
 */
@RequiredArgsConstructor
@Controller
public class StaffController {

    private final StaffService staffService;

}
