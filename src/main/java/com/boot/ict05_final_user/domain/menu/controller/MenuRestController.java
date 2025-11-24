package com.boot.ict05_final_user.domain.menu.controller;

import com.boot.ict05_final_user.domain.menu.dto.*;
import com.boot.ict05_final_user.domain.menu.entity.StoreMenuSoldout;
import com.boot.ict05_final_user.domain.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 메뉴 조회/상세/품절 상태 변경을 제공하는 REST 컨트롤러.
 *
 * <p>로그인한 가맹점의 {@code storeId} 기준으로 서버 페이징/검색/필터가 적용된
 * 메뉴 목록과 상세를 제공합니다. 또한 가맹점 단위의 품절 상태를 변경할 수 있습니다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/API")
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Menus", description = "메뉴 목록/상세 조회 및 품절 상태 변경 API")
@SecurityRequirement(name = "bearerAuth")
public class MenuRestController {

    private final MenuService menuService;

    /**
     * 메뉴 목록 API (로그인 가맹점 기준 + 서버 페이징/검색/필터)
     *
     * @param menuSearchDTO 검색/필터 조건 DTO
     * @param pageable      페이징/정렬 정보
     * @param storeId       인증 정보에서 추출한 가맹점 ID
     * @return 메뉴 목록 페이지
     * @throws IllegalStateException 인증 정보에 storeId가 없는 경우
     */
    @Operation(
            summary = "메뉴 목록 조회",
            description = "로그인한 가맹점(storeId) 기준으로 서버 페이징/검색/필터가 적용된 메뉴 목록을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            // springdoc가 Page<>를 자동 전개하지만, 대표 스키마로 항목 타입을 명시
                            array = @ArraySchema(schema = @Schema(implementation = MenuListDTO.class)),
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "인가 실패")
    })
    @GetMapping("/menu/list")
    public Page<MenuListDTO> getMenuList(
            @ParameterObject
            MenuSearchDTO menuSearchDTO,
            @PageableDefault(page = 0, size = 10, sort = "menuId", direction = Sort.Direction.DESC)
            @ParameterObject
            Pageable pageable,
            // 로그인한 사용자 객체에서 storeId 뽑기
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "storeId") Long storeId
    ) {
        if (storeId == null) {
            throw new IllegalStateException("로그인한 가맹점(storeId)을 찾을 수 없습니다.");
        }
        return menuService.selectAllStoreMenu(storeId, menuSearchDTO, pageable);
    }

    /**
     * 메뉴 상세 API.
     *
     * @param id 메뉴 ID
     * @return 메뉴 상세 DTO (없으면 404)
     */
    @Operation(
            summary = "메뉴 상세 조회",
            description = "메뉴 ID로 단일 메뉴 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MenuDetailDTO.class), mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "대상 메뉴를 찾을 수 없음")
    })
    @GetMapping("/menu/{id}")
    public ResponseEntity<MenuDetailDTO> getMenuDetail(
            @Parameter(description = "메뉴 ID", example = "211", required = true)
            @PathVariable Long id
    ) {
        MenuDetailDTO dto = menuService.selectStoreMenuDetail(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * 품절 상태 변경 요청 바디.
     */
    @Getter
    @Setter
    @Schema(description = "품절 상태 변경 요청 바디")
    public static class SoldOutUpdateRequest {
        /**
         * 변경할 품절 상태 값(가맹점 단위 품절 Enum/값).
         * <p>구현체 Enum 값은 시스템 정의에 따릅니다.</p>
         */
        @Schema(description = "품절 상태", implementation = StoreMenuSoldout.class, nullable = false)
        private StoreMenuSoldout storeMenuSoldout;
    }

    /**
     * 품절 상태 변경 (로그인 가맹점 기준).
     *
     * @param menuId  대상 메뉴 ID
     * @param request 품절 상태 변경 요청 바디
     * @param storeId 인증 정보에서 추출한 가맹점 ID
     * @return 204 No Content
     * @throws IllegalStateException 인증 정보에 storeId가 없는 경우
     */
    @Operation(
            summary = "메뉴 품절 상태 변경",
            description = "로그인한 가맹점(storeId) 기준으로 특정 메뉴의 품절 상태를 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "변경 성공(본문 없음)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 본문/상태 값"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "인가 실패"),
            @ApiResponse(responseCode = "404", description = "대상 메뉴를 찾을 수 없음")
    })
    @PatchMapping("/menu/{menuId}/sold-out")
    public ResponseEntity<Void> updateSoldOutStatus(
            @Parameter(description = "메뉴 ID", example = "211", required = true)
            @PathVariable Long menuId,
            @RequestBody(
                    description = "품절 상태 변경 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SoldOutUpdateRequest.class))
            )
            @org.springframework.web.bind.annotation.RequestBody
            SoldOutUpdateRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "storeId") Long storeId
    ) {
        if (storeId == null) {
            throw new IllegalStateException("로그인한 가맹점(storeId)을 찾을 수 없습니다.");
        }
        menuService.updateSoldOutStatus(storeId, menuId, request.getStoreMenuSoldout());
        return ResponseEntity.noContent().build();
    }

}
