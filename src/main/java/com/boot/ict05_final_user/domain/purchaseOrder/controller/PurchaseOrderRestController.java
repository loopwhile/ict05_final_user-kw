package com.boot.ict05_final_user.domain.purchaseOrder.controller;

import com.boot.ict05_final_user.config.security.principal.AppUser;
import com.boot.ict05_final_user.domain.purchaseOrder.dto.*;
import com.boot.ict05_final_user.domain.purchaseOrder.entity.PurchaseOrderStatus;
import com.boot.ict05_final_user.domain.purchaseOrder.repository.PurchaseOrderRepository;
import com.boot.ict05_final_user.domain.purchaseOrder.service.OrderSyncService;
import com.boot.ict05_final_user.domain.purchaseOrder.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 가맹점 발주 관련 REST 컨트롤러.
 * <p>
 * 발주 목록 조회, 상세 조회, 등록, 수정, 삭제 및
 * 본사와의 상태 연동 기능을 제공한다.
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/purchase")
@Tag(name = "발주 API", description = "발주 등록/조회/수정 기능 제공")
@Slf4j
public class PurchaseOrderRestController {

    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final OrderSyncService orderSyncService;

    /**
     * 발주 목록 페이징 조회
     *
     * @param purchaseOrderSearchDTO 검색 조건 DTO (점포, 기간, 상태 등)
     * @param status                 상태 필터 (선택)
     * @param pageable               페이징 정보
     * @return 발주 목록 페이지
     */
    @GetMapping("/list")
    @Operation(
            summary = "발주 목록 조회",
            description = "검색 조건과 페이징 정보를 이용하여 가맹점 발주 목록을 조회한다."
    )
    public ResponseEntity<Page<PurchaseOrderListDTO>> listPurchase(
            @AuthenticationPrincipal AppUser appUser,
            @ModelAttribute PurchaseOrderSearchDTO purchaseOrderSearchDTO,
            @RequestParam(value = "status", required = false) PurchaseOrderStatus status,
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Long storeId = appUser.getStoreId();
        purchaseOrderSearchDTO.setStoreId(storeId);

        if (status != null) purchaseOrderSearchDTO.setPurchaseOrderStatus(status);
        Page<PurchaseOrderListDTO> result = purchaseOrderService.selectAllPurchase(purchaseOrderSearchDTO, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * 발주 상세 조회
     *
     * @param id 발주 ID
     * @return 발주 상세 정보 DTO
     */
    @GetMapping("/detail/{id}")
    @Operation(
            summary = "발주 상세 조회",
            description = "발주 ID를 기준으로 발주 헤더 및 품목 상세 정보를 조회한다."
    )
    public ResponseEntity<PurchaseOrderDetailDTO> getPurchaseOrderDetail(@PathVariable Long id) {
        PurchaseOrderDetailDTO detail = purchaseOrderService.getPurchaseOrderDetail(id);
        return ResponseEntity.ok(detail);
    }

    /**
     * 발주 등록
     *
     * @param dto 발주 생성 요청 DTO
     * @return 생성된 발주 ID
     */
    @PostMapping("/create")
    @Operation(
            summary = "발주 등록",
            description = "가맹점에서 신규 발주를 등록한다."
    )
    public ResponseEntity<Long> createPurchaseOrder(@RequestBody PurchaseOrderRequestsDTO dto) {
        log.info("발주 등록 요청 들어옴: {}", dto);
        Long newOrderId = purchaseOrderService.createPurchaseOrder(dto);
        return ResponseEntity.ok(newOrderId);
    }

    /**
     * 발주 수정
     *
     * @param id  수정 대상 발주 ID
     * @param dto 수정 요청 DTO
     * @return HTTP 200 응답
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "발주 수정",
            description = "기존 발주의 헤더 및 품목 정보를 수정한다."
    )
    public ResponseEntity<?> updatePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequestsDTO dto) {
        log.info("PUT 요청 들어옴 id={}", id);
        log.info(dto.getItems().toString());
        purchaseOrderService.updatePurchaseOrder(id, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * 발주 전체 삭제 (헤더 + 품목)
     *
     * @param id 삭제할 발주 ID
     * @return HTTP 204 응답
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "발주 삭제",
            description = "발주 헤더 및 품목 전체를 삭제한다."
    )
    public ResponseEntity<?> deletePurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.noContent().build(); // 204 응답
    }

    /**
     * 발주 상세 품목 삭제
     *
     * @param detailId 삭제할 발주 상세 품목 ID
     * @return HTTP 204 응답
     */
    @DeleteMapping("/detail/item/{detailId}")
    @Operation(
            summary = "발주 상세 품목 삭제",
            description = "발주에 포함된 개별 품목을 삭제한다."
    )
    public ResponseEntity<?> deletePurchaseOrderDetail(@PathVariable Long detailId) {
        purchaseOrderService.deletePurchaseOrderDetail(detailId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 가맹점 발주 상태 변경
     *
     * <p>가맹점에서 배송상태(PENDING → RECEIVED → DELIVERED 등)를 직접 변경할 때 호출한다.<br>
     * 상태 변경 후 본사 서버에 동기화 요청도 자동 수행한다.</p>
     *
     * @param id     변경할 발주 ID
     * @param status 새 상태
     */
    @PutMapping("/status/{id}")
    public ResponseEntity<?> updatePurchaseStatus(
            @PathVariable Long id,
            @RequestParam("status") String status
    ) {
        try {
            PurchaseOrderStatus newStatus = PurchaseOrderStatus.valueOf(status.toUpperCase());
            purchaseOrderService.updateStatusById(id, newStatus);   // 로컬 DB 반영
            log.info("[STORE] 로컬 DB 상태 업데이트 완료: id={}, status={}", id, newStatus);

            // 3️⃣ HQ 동기화 (백엔드 → 백엔드)
            purchaseOrderRepository.findOrderCodeById(id).ifPresent(orderCode -> {
                try {
                    orderSyncService.syncToHQ(orderCode, newStatus.name());
                    log.info("[STORE] HQ 동기화 성공: orderCode={}, status={}", orderCode, newStatus);
                } catch (Exception e) {
                    // HQ 서버 미응답, 네트워크 오류, 인증 실패 등
                    log.warn("[STORE] HQ 동기화 실패: orderCode={}, status={}, err={}", orderCode, newStatus, e.getMessage());
                }
            });
            return ResponseEntity.ok("상태 변경 및 본사 동기화 완료");

        } catch (IllegalArgumentException e) {
            log.error("[STORE] 잘못된 상태 요청: {}", status);
            return ResponseEntity.badRequest().body("잘못된 상태 값: " + status);

        } catch (Exception e) {
            log.error("[STORE] 상태 변경 중 예외 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("상태 변경 중 서버 오류 발생");
        }
    }

    /**
     * 본사 → 가맹점 발주 상태 동기화
     *
     * <p>
     * 본사 서버에서 상태 변경 후, 가맹점 쪽으로 상태 값을 동기화할 때 호출하는 API 이다.
     * </p>
     *
     * @param orderCode 발주 코드
     * @param status    새 상태 문자열 (Enum 이름)
     * @return 처리 결과 메시지
     */
    @PutMapping("/sync/status")
    public ResponseEntity<?> syncStatusFromHQ(
            @RequestParam("orderCode") String orderCode,
            @RequestParam("status") String status
    ) {
        log.info("[STORE] 본사로부터 동기화 요청 수신: orderCode={}, status={}", orderCode, status);
        try {
            PurchaseOrderStatus newStatus = PurchaseOrderStatus.valueOf(status.toUpperCase());
            purchaseOrderService.updateStatusByOrderCode(orderCode, newStatus); // 로컬 DB 반영
            return ResponseEntity.ok("가맹점 상태 동기화 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("잘못된 상태 값: " + status);
        }
    }

}
