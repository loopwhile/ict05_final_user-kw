package com.boot.ict05_final_user.domain.inventory.controller;

import com.boot.ict05_final_user.config.security.principal.AppUser;
import com.boot.ict05_final_user.domain.inventory.dto.StoreConsumeRequestDTO;
import com.boot.ict05_final_user.domain.inventory.dto.StoreInventoryAdjustmentWriteDTO;
import com.boot.ict05_final_user.domain.inventory.dto.StoreInventoryInWriteDTO;
import com.boot.ict05_final_user.domain.inventory.dto.StoreInventoryListDTO;
import com.boot.ict05_final_user.domain.inventory.dto.StoreInventoryRestockRequest;
import com.boot.ict05_final_user.domain.inventory.service.StoreAdjustmentService;
import com.boot.ict05_final_user.domain.inventory.service.StoreConsumptionService;
import com.boot.ict05_final_user.domain.inventory.service.StoreInboundService;
import com.boot.ict05_final_user.domain.inventory.service.StoreInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 가맹점 재고 REST 컨트롤러.
 *
 * <p>역할</p>
 * <ul>
 *   <li>가맹점 집계 재고 목록 조회</li>
 *   <li>가맹점 집계 재고 일괄 초기화(누락분 0으로 생성)</li>
 *   <li>간단 입고(집계 가산)</li>
 *   <li>신규 입고 레코드 등록(단가/정책 반영 포함, Inbound 서비스 위임)</li>
 *   <li>판매 소진 처리(소진 이력 저장, 단가 미관리)</li>
 *   <li>재고 조정(절대 수량 설정, 조정 이력 저장)</li>
 * </ul>
 *
 * <p>보안/컨텍스트</p>
 * <ul>
 *   <li>인증 컨텍스트의 {@link AppUser}에서 storeId를 취득하여 매장 소유를 강제한다.</li>
 *   <li>각 서비스 계층에서 매장-대상 엔티티 소유 검증을 재차 수행한다.</li>
 * </ul>
 *
 * <p>응답 규약</p>
 * <ul>
 *   <li>정상 처리 시 200 OK</li>
 *   <li>대상 미존재/권한 불일치/검증 실패 시 서비스 계층에서 예외를 던지며, 글로벌 예외 핸들러에서 HTTP 에러로 변환</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/API/store/inventory")
public class StoreInventoryRestController {

    private final StoreInventoryService storeInventoryService;
    private final StoreInboundService inboundService;
    private final StoreConsumptionService consumptionService;
    private final StoreAdjustmentService adjustmentService;

    private final Validator inventoryValidator;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setValidator(inventoryValidator);
    }

    /**
     * 가맹점 집계 재고 목록 조회.
     *
     * <p>GET /API/store/inventory/list</p>
     *
     * <p>설명</p>
     * <ul>
     *   <li>인증된 사용자의 storeId 기준으로 집계 재고 목록을 반환한다.</li>
     *   <li>프론트 재고 현황 테이블/카드 뷰에 사용.</li>
     * </ul>
     *
     * @param user 인증 사용자(매장 ID 보유)
     * @return {@link StoreInventoryListDTO} 리스트
     */
    @GetMapping("/list")
    public List<StoreInventoryListDTO> getStoreInventoryList(@AuthenticationPrincipal AppUser user) {
        return storeInventoryService.getStoreInventoryList(user.getStoreId());
    }

    /**
     * 가맹점 집계 재고 누락분 0으로 일괄 생성.
     *
     * <p>POST /API/store/inventory/init</p>
     *
     * <p>설명</p>
     * <ul>
     *   <li>해당 매장에 존재하는 모든 StoreMaterial을 기준으로, StoreInventory가 없는 항목만 0으로 생성한다.</li>
     *   <li>상태/업데이트일시는 엔티티의 {@code touchAfterQuantityChange()} 로 동기화된다.</li>
     * </ul>
     *
     * @param user 인증 사용자(매장 ID 보유)
     * @return 생성된 집계 재고 개수
     */
    @PostMapping("/init")
    public ResponseEntity<Integer> initInventory(@AuthenticationPrincipal AppUser user) {
        int created = storeInventoryService.initInventoryForStore(user.getStoreId());
        return ResponseEntity.ok(created);
    }

    /**
     * 간단 입고(집계 재고 가산).
     *
     * <p>POST /API/store/inventory/restock</p>
     *
     * <p>설명</p>
     * <ul>
     *   <li>기존 집계 재고 ID 기준으로 수량을 가산하고 상태/업데이트일시를 동기화한다.</li>
     *   <li>배치/단가/유통기한 관리가 필요할 경우 별도의 Inbound API(/in) 사용을 권장.</li>
     * </ul>
     *
     * @param request 집계 재고 ID 및 가산 수량 등
     */
    @PostMapping("/restock")
    public ResponseEntity<Void> restock(@RequestBody @Valid StoreInventoryRestockRequest request) {
        storeInventoryService.restock(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 신규 입고 레코드 등록(Inbound).
     *
     * <p>POST /API/store/inventory/in</p>
     *
     * <p>설명</p>
     * <ul>
     *   <li>입고 단가 해석(요청 단가 &gt; HQ 판매가 &gt; 매장 최근 입고가), 집계 반영, 입고 이력 저장을 수행한다.</li>
     *   <li>프론트의 “재입고 폼” 전용 엔드포인트.</li>
     * </ul>
     *
     * @param user 인증 사용자(매장 ID 보유)
     * @param dto  입고 요청 DTO
     * @return 생성된 입고 이력 ID
     */
    @PostMapping("/in")
    public ResponseEntity<Long> inbound(@AuthenticationPrincipal AppUser user,
                                        @RequestBody @Valid StoreInventoryInWriteDTO dto) {
        Long id = inboundService.inbound(user.getStoreId(), dto);
        return ResponseEntity.ok(id);
    }

    /**
     * 판매 소진 처리.
     *
     * <p>POST /API/store/inventory/consume</p>
     *
     * <p>설명</p>
     * <ul>
     *   <li>가맹점 재료 기준으로 정규화된 소진 라인 목록을 받아 집계 재고를 차감하고 소진 이력을 저장한다.</li>
     *   <li>레시피 기반 계산/단위 변환은 사전 단계에서 완료되었다고 가정한다.</li>
     * </ul>
     *
     * @param user 인증 사용자(매장 ID 보유)
     * @param dto  소진 요청 DTO
     */
    @PostMapping("/consume")
    public ResponseEntity<Void> consume(@AuthenticationPrincipal AppUser user,
                                        @RequestBody @Valid StoreConsumeRequestDTO dto) {
        consumptionService.consume(user.getStoreId(), dto);
        return ResponseEntity.ok().build();
    }

    /**
     * 재고 조정(절대 수량 설정).
     *
     * <p>POST /API/store/inventory/adjust</p>
     *
     * <p>설명</p>
     * <ul>
     *   <li>조정 전/후/차이를 조정 이력으로 기록하고, 집계 재고를 절대 수량으로 설정한다.</li>
     *   <li>조정 후 수량이 음수인 경우 거절된다.</li>
     * </ul>
     *
     * @param user 인증 사용자(매장 ID 보유)
     * @param dto  조정 요청 DTO
     */
    @PostMapping("/adjust")
    public ResponseEntity<Void> adjust(@AuthenticationPrincipal AppUser user,
                                       @RequestBody @Valid StoreInventoryAdjustmentWriteDTO dto) {
        adjustmentService.adjust(user.getStoreId(), dto);
        return ResponseEntity.ok().build();
    }
}
