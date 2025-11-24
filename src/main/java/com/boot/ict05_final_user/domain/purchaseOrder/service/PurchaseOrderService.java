package com.boot.ict05_final_user.domain.purchaseOrder.service;

import com.boot.ict05_final_user.domain.purchaseOrder.dto.*;
import com.boot.ict05_final_user.domain.purchaseOrder.entity.PurchaseOrderStatus;
import com.boot.ict05_final_user.domain.purchaseOrder.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가맹점 발주 도메인의 비즈니스 로직을 담당하는 서비스 클래스.
 *
 * <p>
 * 컨트롤러와 레포지토리 사이에서 다음 역할을 수행한다.
 * </p>
 * <ul>
 *     <li>발주 목록 조회 (검색 + 페이징)</li>
 *     <li>단일 발주 상세 조회</li>
 *     <li>발주 신규 등록 및 수정</li>
 *     <li>발주 및 발주 상세 삭제</li>
 *     <li>발주 상태 변경 및 본사와의 상태 연동</li>
 * </ul>
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;

    /**
     * 발주 목록을 검색 조건과 페이징 정보에 따라 조회한다.
     *
     * <p>
     * - 상태, 발주코드, 공급업체명, 대표 품목명 등으로 검색할 수 있다.<br>
     * - 실제 쿼리는 {@link PurchaseOrderRepository#listPurchase(PurchaseOrderSearchDTO, Pageable)} 에서 처리한다.
     * </p>
     *
     * @param purchaseOrderSearchDTO 검색 조건 DTO
     * @param pageable               페이지 번호, 크기, 정렬 정보
     * @return 발주 목록 페이지 (요약 정보 DTO 리스트)
     */
    @Transactional(readOnly = true)
    public Page<PurchaseOrderListDTO> selectAllPurchase(PurchaseOrderSearchDTO purchaseOrderSearchDTO, Pageable pageable) {
        return purchaseOrderRepository.listPurchase(purchaseOrderSearchDTO, pageable);
    }

    /**
     * 단일 발주의 상세 정보를 조회한다.
     *
     * <p>
     * - 발주 헤더 정보와 품목 리스트를 함께 조회하여
     *   {@link PurchaseOrderDetailDTO} 에 매핑해 반환한다.
     * </p>
     *
     * @param id 발주 ID (PK)
     * @return 발주 상세 DTO, 존재하지 않으면 null
     */
    @Transactional(readOnly = true)
    public PurchaseOrderDetailDTO getPurchaseOrderDetail(Long id) {
        return purchaseOrderRepository.findPurchaseOrderDetail(id);
    }

    /**
     * 신규 발주를 생성한다. (헤더 + 상세 일괄 등록)
     *
     * <p>
     * - 발주 코드 생성, 대표 품목/공급업체 결정, 상세 품목 단가/합계 계산 등은<br>
     *   {@link PurchaseOrderRepository#createPurchaseOrder(PurchaseOrderRequestsDTO)} 에서 처리한다.
     * </p>
     *
     * @param dto 발주 생성 요청 DTO
     * @return 생성된 발주 ID
     */
    @Transactional
    public Long createPurchaseOrder(PurchaseOrderRequestsDTO dto) {
        return purchaseOrderRepository.createPurchaseOrder(dto);
    }

    /**
     * 기존 발주를 수정한다. (헤더 + 상세 upsert)
     *
     * <p>
     * 비즈니스 규칙:
     * </p>
     * <ul>
     *     <li>현재 상태가 PENDING 인 발주만 수정 가능</li>
     *     <li>상세 품목은 storeMaterialId 기준으로 추가/수정/삭제</li>
     *     <li>수정 후 총액과 품목 수를 다시 계산해 헤더에 반영</li>
     * </ul>
     *
     * @param id  수정할 발주 ID
     * @param dto 수정 요청 DTO
     * @throws IllegalArgumentException 존재하지 않는 발주 ID 인 경우
     * @throws IllegalStateException    수정 불가 상태에서 호출된 경우
     */
    @Transactional
    public void updatePurchaseOrder(Long id, PurchaseOrderRequestsDTO dto) {
        purchaseOrderRepository.updatePurchaseOrder(id, dto);
    }

     /**
            * 발주 전체를 삭제한다. (헤더 + 상세 모두 삭제)
            *
            * <p>
     * FK 제약을 고려하여, 레포지토리 구현 내부에서
     * 상세 → 헤더 순서로 삭제를 수행한다.
            * </p>
            *
            * @param id 삭제할 발주 ID
     * @throws IllegalArgumentException 존재하지 않는 발주 ID 인 경우
     */
    @Transactional
    public void deletePurchaseOrder(Long id) {
        purchaseOrderRepository.deletePurchaseOrder(id);
    }

    /**
     * 단일 발주 상세 품목을 삭제한다.
     *
     * <p>
     * - 삭제 후 남은 상세가 없으면 발주 헤더도 함께 삭제된다.<br>
     * - 남아 있는 경우에는 헤더의 품목 수만 갱신한다.
     * </p>
     *
     * @param detailId 삭제할 발주 상세 ID
     * @throws IllegalArgumentException detailId 에 해당하는 상세가 없는 경우
     */
    @Transactional
    public void deletePurchaseOrderDetail(Long detailId) {
        purchaseOrderRepository.deletePurchaseOrderDetail(detailId);
    }

    /**
     * 발주 ID 기준으로 발주 상태를 변경한다.
     *
     * <p>
     * - 가맹점 화면에서 상태를 변경할 때 사용한다.<br>
     * - 상태 변경 후 실제 쿼리는 QueryDSL 기반 레포지토리에서 수행된다.
     * </p>
     *
     * @param id     상태를 변경할 발주 ID
     * @param status 변경할 상태 값
     */
    @Transactional
    public void updateStatusById(Long id, PurchaseOrderStatus status) {
        purchaseOrderRepository.updateStatusById(id, status);
    }

    /**
     * 발주 코드 기준으로 발주 상태를 변경한다.
     *
     * <p>
     * - 본사 → 가맹점 상태 연동 시, 발주 코드만 넘어오는 경우 사용한다.<br>
     * - 예: HQ 서버에서 상태가 변경되었을 때, 가맹점 서버로 콜백 요청을 보내는 경우.
     * </p>
     *
     * @param orderCode 상태를 변경할 발주 코드
     * @param status    변경할 상태 값
     */
    @Transactional
    public void updateStatusByOrderCode(String orderCode, PurchaseOrderStatus status) {
        purchaseOrderRepository.updateStatusByOrderCode(orderCode, status);
    }

}
