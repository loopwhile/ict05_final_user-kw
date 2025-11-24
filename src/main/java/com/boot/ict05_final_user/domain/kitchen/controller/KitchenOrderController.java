package com.boot.ict05_final_user.domain.kitchen.controller;

import com.boot.ict05_final_user.config.security.principal.AppUser;
import com.boot.ict05_final_user.domain.kitchen.dto.KitchenOrderResponseDTO;
import com.boot.ict05_final_user.domain.kitchen.dto.UpdateKitchenOrderStatusRequestDTO;
import com.boot.ict05_final_user.domain.kitchen.service.KitchenOrderService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 주방 주문(Kitchen Orders) 관련 API 컨트롤러.
 *
 * <p>
 * - 주방 화면에서 표시할 진행 중 주문 목록 조회<br>
 * - 특정 주문의 주방 상태 변경(예: PREPARING → COOKING → READY 등)
 * </p>
 *
 * <p><b>Security:</b> JWT 기반 인증 필요(예: {@code bearerAuth})</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kitchen-orders")
@Tag(name = "Kitchen Orders", description = "주방 주문 조회 및 상태 변경 API")
@SecurityRequirement(name = "bearerAuth") // springdoc에서 securitySchemes 이름이 'bearerAuth'일 때
public class KitchenOrderController {

    private final KitchenOrderService kitchenOrderService;

    /**
     * 주방 화면 주문 목록을 조회합니다.
     *
     * <p>로그인한 사용자의 점포 ID(AppUser.storeId) 기준으로 진행 중 주문들을 반환합니다.</p>
     *
     * @param user 인증된 사용자(스웨거 문서에서는 숨김)
     * @return 진행 중 주문 목록
     */
    @Operation(
            summary = "주방 주문 목록 조회",
            description = "인증 정보의 storeId로 해당 가맹점의 진행 중 주문을 최신순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = KitchenOrderResponseDTO.class)),
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "인가 실패")
    })
    @GetMapping
    public ResponseEntity<List<KitchenOrderResponseDTO>> getKitchenOrders(
            @Parameter(hidden = true) // 문서에서 노출하지 않음
            @AuthenticationPrincipal AppUser user
    ) {
        Long storeId = user.getStoreId();              // 로그인한 점포 ID
        log.info("[Kitchen] getKitchenOrders storeId={}", storeId);

        List<KitchenOrderResponseDTO> orders = kitchenOrderService.getKitchenOrders(storeId);
        log.info("[Kitchen] result size={}", orders.size());

        return ResponseEntity.ok(orders);
    }

    /**
     * 특정 주문의 주방 상태를 변경합니다.
     *
     * <p>예: {@code PATCH /api/kitchen-orders/{orderId}/status}</p>
     * <p>Request Body 예시: <pre>{@code { "status": "COOKING" }}</pre></p>
     *
     * @param orderId 변경할 주문 ID
     * @param req     상태 변경 요청 DTO
     * @return 변경 후 주문 정보
     */
    @Operation(
            summary = "주방 주문 상태 변경",
            description = "주문 ID와 요청 본문(status)을 받아 해당 주문의 주방 상태를 변경합니다. 예: PREPARING, COOKING, READY, COMPLETED"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공",
                    content = @Content(
                            schema = @Schema(implementation = KitchenOrderResponseDTO.class),
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 상태값 또는 요청 형식"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "인가 실패"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<KitchenOrderResponseDTO> updateStatus(
            @Parameter(description = "주문 ID", required = true, example = "123")
            @PathVariable Long orderId,
            @RequestBody(
                    description = "주문 상태 변경 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateKitchenOrderStatusRequestDTO.class))
            )
            @org.springframework.web.bind.annotation.RequestBody
            UpdateKitchenOrderStatusRequestDTO req
    ) {
        KitchenOrderResponseDTO dto = kitchenOrderService.updateStatus(orderId, req);
        return ResponseEntity.ok(dto);
    }
}
