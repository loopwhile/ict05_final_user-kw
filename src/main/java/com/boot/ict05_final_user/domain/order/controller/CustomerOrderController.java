package com.boot.ict05_final_user.domain.order.controller;

import com.boot.ict05_final_user.config.security.principal.AppUser;
import com.boot.ict05_final_user.domain.order.dto.*;
import com.boot.ict05_final_user.domain.order.service.CustomerOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 가맹점 주문(Customer Orders) API 컨트롤러.
 *
 * <p>
 * - 주문 생성<br>
 * - 가맹점 기준 주문 목록 조회(검색/필터/페이징)<br>
 * - 주문 상태 변경<br>
 * - 주문 상세 조회
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer-orders")
@SecurityRequirement(name = "bearerAuth")
public class CustomerOrderController {

    private final CustomerOrderService orderService;

    /**
     * 주문을 생성합니다.
     *
     * @param user    인증 사용자
     * @param req     주문 생성 요청 DTO
     * @param request 원시 HTTP 요청(Authorization 등 헤더 확인용)
     * @return 생성된 주문 응답 DTO
     */
    @Operation(
            summary = "주문 생성",
            description = "요청 본문을 바탕으로 주문을 생성합니다. 인증 정보의 가맹점 ID를 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = CreateOrderResponseDTO.class), mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "인가 실패"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류")
    })
    @PostMapping
    public ResponseEntity<CreateOrderResponseDTO> create(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AppUser user,
            @RequestBody(
                    description = "주문 생성 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateOrderRequestDTO.class))
            )
            @org.springframework.web.bind.annotation.RequestBody
            CreateOrderRequestDTO req,
            HttpServletRequest request
    ) {
        String authHeader = request.getHeader("Authorization");
        log.info("Authorization header = {}", authHeader); // 토큰 확인용

        if (user == null) {
            log.warn("Unauthenticated POST /api/customer-orders 요청");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long storeId = user.getStoreId();
        log.info("create order by storeId={}", storeId);

        return ResponseEntity.ok(orderService.create(req, storeId));
    }

    /**
     * 가맹점 기준 주문 목록을 조회합니다.
     *
     * <p>검색/필터는 쿼리 파라미터로 전달되며, 페이징/정렬 정보를 지원합니다.</p>
     *
     * @param user        인증 사용자
     * @param keyword     검색어
     * @param status      주문 상태
     * @param paymentType 결제 수단
     * @param orderType   주문 유형
     * @param period      조회 기간 프리셋
     * @param pageable    페이징/정렬 정보
     * @return 주문 목록 페이지
     */
    @Operation(
            summary = "가맹점 주문 목록 조회",
            description = "로그인한 가맹점(storeId) 기준으로 주문 목록을 조회합니다. 검색/필터/페이징을 지원합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = CustomerOrderListDTO.class)),
                            mediaType = "application/json"
                    )),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "인가 실패")
    })
    @GetMapping
    public ResponseEntity<Page<CustomerOrderListDTO>> listForStore(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AppUser user,
            @Parameter(description = "검색어")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "주문 상태")
            @RequestParam(required = false) String status,
            @Parameter(description = "결제 수단")
            @RequestParam(required = false) String paymentType,
            @Parameter(description = "주문 유형")
            @RequestParam(required = false) String orderType,
            @Parameter(description = "조회 기간 프리셋")
            @RequestParam(required = false, defaultValue = "all") String period,
            @PageableDefault(page = 0, size = 20, sort = "id", direction = Sort.Direction.DESC)
            @ParameterObject Pageable pageable
    ) {
        if (user == null || user.getStoreId() == null) {
            log.warn("Forbidden: user is null or storeId is null");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        final Long storeId = user.getStoreId();
        log.info("list orders for storeId={}", storeId);

        CustomerOrderSearchDTO search = new CustomerOrderSearchDTO();
        search.setKeyword(keyword);
        search.setStatus(status);
        search.setPaymentType(paymentType);
        search.setOrderType(orderType);
        search.setPeriod(period);

        Page<CustomerOrderListDTO> pageResult =
                orderService.searchOrderListPage(storeId, search, pageable);

        log.info("orders api result size={}, totalElements={}",
                pageResult.getNumberOfElements(), pageResult.getTotalElements());

        return ResponseEntity.ok(pageResult);
    }

    /**
     * 주문 상태를 변경합니다.
     *
     * @param orderId 주문 ID
     * @param body    상태 변경 요청 DTO
     * @return 본문 없는 204 응답
     */
    @Operation(
            summary = "주문 상태 변경",
            description = "주문 ID와 요청 본문(status)을 받아 주문 상태를 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "인가 실패"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateStatus(
            @Parameter(description = "주문 ID", required = true)
            @PathVariable Long orderId,
            @RequestBody(
                    description = "주문 상태 변경 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateStatusRequestDTO.class))
            )
            @org.springframework.web.bind.annotation.RequestBody
            UpdateStatusRequestDTO body
    ) {
        orderService.updateStatus(orderId, body.getStatus());
        return ResponseEntity.noContent().build();
    }

    /**
     * 주문 상세를 조회합니다(가맹점 기준 접근 제어).
     *
     * @param user    인증 사용자
     * @param orderId 주문 ID
     * @return 주문 상세 DTO
     */
    @Operation(
            summary = "주문 상세 조회",
            description = "로그인한 가맹점(storeId) 기준으로 단일 주문 상세를 조회합니다. 소유 매장이 아닌 경우 403을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomerOrderDetailDTO.class), mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "인가 실패(다른 매장 주문 접근)"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<CustomerOrderDetailDTO> getOrderDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AppUser user,
            @Parameter(description = "주문 ID", required = true)
            @PathVariable Long orderId
    ) {
        if (user == null) {
            log.warn("Unauthenticated GET /api/customer-orders/{} 요청", orderId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long storeId = user.getStoreId();
        log.info("get order detail orderId={}, storeId={}", orderId, storeId);

        try {
            CustomerOrderDetailDTO dto = orderService.getOrderDetail(storeId, orderId);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
