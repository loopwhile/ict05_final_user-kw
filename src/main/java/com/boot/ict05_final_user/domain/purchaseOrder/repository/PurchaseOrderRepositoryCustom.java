package com.boot.ict05_final_user.domain.purchaseOrder.repository;

import com.boot.ict05_final_user.domain.purchaseOrder.dto.PurchaseOrderDetailDTO;
import com.boot.ict05_final_user.domain.purchaseOrder.dto.PurchaseOrderListDTO;
import com.boot.ict05_final_user.domain.purchaseOrder.dto.PurchaseOrderRequestsDTO;
import com.boot.ict05_final_user.domain.purchaseOrder.dto.PurchaseOrderSearchDTO;
import com.boot.ict05_final_user.domain.purchaseOrder.entity.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PurchaseOrderRepositoryCustom {

    // 발주 목록 조회
    Page<PurchaseOrderListDTO> listPurchase(PurchaseOrderSearchDTO purchaseOrderSearchDTO, Pageable pageable);
    // 발주 총 개수
    long countPurchase(PurchaseOrderSearchDTO purchaseOrderSearchDTO);
    // 발주 상세 조회
    PurchaseOrderDetailDTO findPurchaseOrderDetail(Long id);
    // 발주 등록
    long createPurchaseOrder(PurchaseOrderRequestsDTO dto);
    // 발주 수정
    void updatePurchaseOrder(Long id, PurchaseOrderRequestsDTO dto);
    // 발주 전체(헤더+품목) 삭제
    void deletePurchaseOrder(Long id);
    // 발주 상세 품목 삭제
    void deletePurchaseOrderDetail(Long detailId);
    // 발주 상태 연동
    Optional<String> findOrderCodeById(Long id);
    // 발주 상태 변경
    int updateStatusById(Long id, PurchaseOrderStatus status);
    int updateStatusByOrderCode(String orderCode, PurchaseOrderStatus status);

}
