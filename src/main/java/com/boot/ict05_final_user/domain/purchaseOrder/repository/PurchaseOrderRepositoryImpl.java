package com.boot.ict05_final_user.domain.purchaseOrder.repository;

import com.boot.ict05_final_user.domain.inventory.entity.QMaterial;
import com.boot.ict05_final_user.domain.inventory.entity.QStoreMaterial;
import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import com.boot.ict05_final_user.domain.purchaseOrder.dto.*;
import com.boot.ict05_final_user.domain.purchaseOrder.entity.*;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 가맹점 발주 관련 QueryDSL 커스텀 레포지토리 구현체.
 *
 * <p>
 * 발주 목록 조회, 상세 조회, 등록, 수정, 삭제 및
 * 상태 변경, 본사 연동용 쿼리를 QueryDSL 기반으로 제공한다.
 * </p>
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderRepositoryImpl implements PurchaseOrderRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager em;

    /**
     * 발주 목록을 검색 조건 및 페이징 정보에 따라 조회한다.
     *
     * <p>
     * - 상태, 발주코드, 공급업체, 대표품목명으로 검색 가능<br>
     * - 상세가 하나도 없는 발주(purchase_order_detail 미존재)는 목록에서 제외한다.<br>
     * - 결과는 id 기준 내림차순 정렬한다.
     * </p>
     *
     * @param purchaseOrderSearchDTO 검색 조건 (상태, 검색 타입, 키워드)
     * @param pageable               페이지 번호, 크기, 정렬 정보
     * @return 발주 목록 페이지 (헤더 정보만 포함)
     */
    @Override
    public Page<PurchaseOrderListDTO> listPurchase(PurchaseOrderSearchDTO purchaseOrderSearchDTO, Pageable pageable) {
        QPurchaseOrder po = QPurchaseOrder.purchaseOrder;

        // 발주 데이터 목록 조회
        List<PurchaseOrderListDTO> content = queryFactory
                .select(Projections.fields(PurchaseOrderListDTO.class,
                        po.id,
                        po.orderCode,
                        po.supplier,
                        po.mainItemName,
                        po.itemCount,
                        po.totalPrice,
                        po.orderDate,
                        po.actualDeliveryDate.as("actualDeliveryDate"),
                        po.priority,
                        po.status
                ))
                .from(po)
                .where(
                        eqOrderCode(purchaseOrderSearchDTO, po),
                        eqStoreId(purchaseOrderSearchDTO.getStoreId(), po),
                        po.details.isNotEmpty()
                )
                .orderBy(po.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 조회
        Long total = queryFactory
                .select(po.count())
                .from(po)
                .where(
                        eqOrderCode(purchaseOrderSearchDTO, po),
                        eqStoreId(purchaseOrderSearchDTO.getStoreId(), po),
                        po.details.isNotEmpty()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 발주 검색용 동적 where 절을 생성한다.
     *
     * <p>
     * - 상태 필터는 항상 AND 로 묶인다.<br>
     * - type 이 null 이면 "all" 로 간주한다.<br>
     * - 키워드가 비어있으면 상태만 필터링한다.
     * </p>
     *
     * @param dto 검색 DTO (상태, type, s)
     * @param po  QPurchaseOrder
     * @return QueryDSL BooleanExpression (필터 조건)
     */
    private BooleanExpression eqOrderCode(PurchaseOrderSearchDTO dto, QPurchaseOrder po) {

        // BooleanExpression condition = null;
        // 기본값 true
        BooleanExpression condition = Expressions.asBoolean(true).isTrue();

        // 상태 필터
        if(dto.getPurchaseOrderStatus() != null) {
            condition = condition.and(po.status.eq(dto.getPurchaseOrderStatus()));
        }

        String type = dto.getType();
        String keyword = dto.getS();

        if (keyword == null || keyword.trim().isEmpty()) {
            return condition; // 상태만 필터링
        }

        if (type == null) type = "all";

        switch (type) {
            case "orderCode":
                condition = condition.and(po.orderCode.containsIgnoreCase(keyword));
                break;
            case "supplier":
                condition = condition.and(po.supplier.containsIgnoreCase(keyword));
                break;
            case "mainItemName":
                condition = condition.and(po.mainItemName.containsIgnoreCase(keyword));
                break;
            case "all":
            default:
                condition = condition.and(
                        po.orderCode.containsIgnoreCase(keyword)
                                .or(po.supplier.containsIgnoreCase(keyword))
                                .or(po.mainItemName.containsIgnoreCase(keyword))
                );
        }
        return condition;
    }

    private BooleanExpression eqStoreId(Long storeId, QPurchaseOrder po) {
        return storeId != null ? po.store.id.eq(storeId) : null;
    }

    /**
     * 조건에 맞는 발주 총 건수를 반환한다.
     *
     * <p>
     * 화면에서 별도 total 쿼리가 필요한 경우를 위한 메서드이며,<br>
     * {@link #listPurchase(PurchaseOrderSearchDTO, Pageable)} 와 같은 필터 조건을 사용한다.
     * </p>
     *
     * @param purchaseOrderSearchDTO 검색 조건
     * @return 조건에 해당하는 발주 건수
     */
    @Override
    public long countPurchase(PurchaseOrderSearchDTO purchaseOrderSearchDTO) {
        QPurchaseOrder purchaseOrder = QPurchaseOrder.purchaseOrder;

        long total = queryFactory
                .select(purchaseOrder.count())
                .from(purchaseOrder)
                .where(
                        eqOrderCode(purchaseOrderSearchDTO, purchaseOrder),
                        eqStoreId(purchaseOrderSearchDTO.getStoreId(), purchaseOrder)
                )
                .fetchOne();

        return total;
    }

    /**
     * 발주 상세 정보를 조회한다.
     *
     * <p>
     * - 발주 헤더 정보(PurchaseOrderDetailDTO)<br>
     * - 발주 품목 목록(List&lt;PurchaseOrderItemDTO&gt;)<br>
     * 를 함께 조회하여 DTO 에 매핑한다.
     * </p>
     *
     * <p>
     * 품목 쿼리에서는<br>
     * - StoreMaterial 기준으로 가맹점 재료 id / 재료명<br>
     * - HQ Material 기준으로 본사 재료 id<br>
     * 를 함께 가져온다.
     * </p>
     *
     * @param id 발주 ID
     * @return 발주 상세 DTO (없으면 null 반환)
     */
    @Override
    public PurchaseOrderDetailDTO findPurchaseOrderDetail(Long id) {
        QPurchaseOrder po = QPurchaseOrder.purchaseOrder;
        QPurchaseOrderDetail pod = QPurchaseOrderDetail.purchaseOrderDetail;
        QStoreMaterial storeMaterial  = QStoreMaterial.storeMaterial;
        QMaterial hqMaterial = QMaterial.material;

        // 메인 발주 정보
        PurchaseOrderDetailDTO header = queryFactory
                .select(Projections.fields(PurchaseOrderDetailDTO.class,
                        po.id,
                        po.orderCode.as("orderCode"),
                        po.supplier,
                        po.orderDate.as("orderDate"),
                        po.actualDeliveryDate.as("actualDeliveryDate"),
                        po.status,
                        po.priority,
                        po.remark.as("notes"),
                        po.totalPrice.as("totalPrice"),
                        po.itemCount.as("itemCount")
                ))
                .from(po)
                .where(po.id.eq(id))
                .fetchOne();

        if (header == null) return null;

        // 발주 상세 품목 리스트
        List<PurchaseOrderItemDTO> items = queryFactory
                .select(Projections.fields(PurchaseOrderItemDTO.class,
                        pod.id,
                        storeMaterial.id.as("storeMaterialId"),        // 가맹점 재료 id
                        hqMaterial.id.as("materialId"),                // 본사 재료 id
                        storeMaterial.name.as("materialName"),         // 화면용 재료명
                        pod.count.as("count"),
                        pod.unitPrice.as("unitPrice"),
                        pod.totalPrice.as("totalPrice")
                ))
                .from(pod)
                .join(pod.material, storeMaterial)         // PurchaseOrderDetail.material = StoreMaterial
                .leftJoin(storeMaterial.material, hqMaterial) // StoreMaterial.material = HQ Material
                .where(pod.purchaseOrder.id.eq(id))
                .fetch();

        System.out.println(
                queryFactory
                        .select(pod.count, pod.unitPrice, pod.totalPrice)
                        .from(pod)
                        .where(pod.purchaseOrder.id.eq(id))
                        .fetch()
        );
        System.out.println("✅ items: " + items);

        header.setItems(items);
        return header;
    }

    /**
     * 신규 발주를 생성한다. (헤더 + 상세 일괄 생성)
     *
     * <p>처리 순서</p>
     * <ol>
     *   <li>발주 코드 생성 (ORD + yyyyMMdd + 랜덤 4자리)</li>
     *   <li>첫 품목의 {@code StoreMaterial} 로부터 가맹점/공급업체/대표품목명 결정</li>
     *   <li>발주 헤더 {@link PurchaseOrder} 영속화</li>
     *   <li>각 품목에 대해 단가/합계를 계산해 상세 {@link PurchaseOrderDetail} 영속화</li>
     *   <li>총액/품목수 헤더 갱신</li>
     * </ol>
     *
     * <p>예외</p>
     * <ul>
     *   <li>요청 DTO 또는 품목이 비어 있으면 {@link IllegalArgumentException}</li>
     *   <li>첫 품목 {@code StoreMaterial} 미존재 시 {@link IllegalArgumentException}</li>
     *   <li>{@code StoreMaterial.material} 미매핑 시 {@link IllegalStateException}</li>
     * </ul>
     *
     * @param dto 발주 생성 요청 DTO
     * @return 생성된 발주 ID
     * @since 2025-11-19
     */
    @Override
    public long createPurchaseOrder(PurchaseOrderRequestsDTO dto) {

        // QueryDSL Q타입(다른 메서드에서 사용할 수 있어 유지)
        QPurchaseOrder po = QPurchaseOrder.purchaseOrder;
        QPurchaseOrderDetail pod = QPurchaseOrderDetail.purchaseOrderDetail;
        QStoreMaterial storeMaterial = QStoreMaterial.storeMaterial;

        // 0) 방어 로직
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("발주 품목이 비어 있습니다.");
        }

        // 1) 발주 코드 생성
        String orderCode = generateOrderCode();

        // 2) 첫 품목 기준 정보 로드(가맹점/대표명/공급업체)
        Long firstStoreMaterialId = dto.getItems().get(0).getStoreMaterialId();
        StoreMaterial firstStoreMaterial = queryFactory
                .selectFrom(storeMaterial)
                .where(storeMaterial.id.eq(firstStoreMaterialId))
                .fetchOne();

        if (firstStoreMaterial == null) {
            throw new IllegalArgumentException("첫 번째 품목 StoreMaterial이 존재하지 않습니다. id=" + firstStoreMaterialId);
        }
        if (firstStoreMaterial.getMaterial() == null || firstStoreMaterial.getMaterial().getId() == null) {
            throw new IllegalStateException("본사 재료와 매핑되지 않은 가맹점 재료입니다. id=" + firstStoreMaterialId);
        }

        // 3) 헤더 persist (JPQL INSERT 제거 → 엔티티 영속화로 전환)
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .store(firstStoreMaterial.getStore())
                .orderCode(orderCode)
                .supplier(firstStoreMaterial.getSupplier() != null ? firstStoreMaterial.getSupplier() : "미지정")
                .mainItemName(firstStoreMaterial.getName())
                .priority(dto.getPriority())
                .remark(dto.getNotes())
                .status(PurchaseOrderStatus.PENDING)
                .orderDate(LocalDate.now())
                .build();
        em.persist(purchaseOrder);

        // 4) 상세 persist + 총액 집계
        BigDecimal totalPrice = BigDecimal.ZERO;
        int itemCount = 0;

        for (PurchaseOrderItemDTO itemDTO : dto.getItems()) {
            Long storeMaterialId = itemDTO.getStoreMaterialId();
            Integer cnt = itemDTO.getCount();

            if (storeMaterialId == null || cnt == null || cnt <= 0) continue;

            StoreMaterial sm = queryFactory
                    .selectFrom(storeMaterial)
                    .where(storeMaterial.id.eq(storeMaterialId))
                    .fetchOne();

            if (sm == null) {
                throw new IllegalArgumentException("StoreMaterial 없음 id=" + storeMaterialId);
            }
            if (sm.getMaterial() == null || sm.getMaterial().getId() == null) {
                throw new IllegalStateException("본사 재료와 매핑되지 않은 가맹점 재료입니다. id=" + storeMaterialId);
            }

            BigDecimal unitPrice = sm.getPurchasePrice() != null ? sm.getPurchasePrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cnt));

            PurchaseOrderDetail detail = PurchaseOrderDetail.builder()
                    .purchaseOrder(purchaseOrder)
                    .material(sm)                 // StoreMaterial 연관
                    .unitPrice(unitPrice)
                    .count(cnt)
                    .totalPrice(lineTotal)
                    .build();

            em.persist(detail);

            totalPrice = totalPrice.add(lineTotal);
            itemCount++;
        }

        // 5) 헤더 합계/품목수 반영
        purchaseOrder.setTotalPrice(totalPrice);
        purchaseOrder.setItemCount(itemCount);

        return purchaseOrder.getId();
    }


    /**
     * 발주 코드를 생성한다.
     *
     * <p>
     * 형식: {@code ORD + yyyyMMdd + 4자리 랜덤 숫자}
     * </p>
     *
     * @return 생성된 발주 코드 문자열
     */
    private String generateOrderCode() {
        String prefix = "ORD";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long random = (long) (Math.random() * 9000) + 1000; // 4자리 랜덤
        return prefix + datePart + random;
    }

    /**
     * 기존 발주를 수정한다. (헤더 + 상세 upsert)
     *
     * <p>
     * 비즈니스 규칙<br>
     * - 현재 상태가 PENDING 인 경우에만 수정 가능<br>
     * - 헤더: 우선순위, 비고를 갱신<br>
     * - 상세: storeMaterialId 기준 upsert<br>
     *   - 요청에 없는 재료는 삭제<br>
     *   - 존재하면 수량/단가/총액 업데이트<br>
     *   - 없으면 신규 insert<br>
     * - 마지막에 상세 합계를 다시 계산하여 헤더의 총액/품목 수를 맞춘다.
     * </p>
     *
     * @param id  수정할 발주 ID
     * @param dto 수정 요청 DTO
     * @throws IllegalArgumentException 존재하지 않는 발주 ID 인 경우
     * @throws IllegalStateException    PENDING 이 아닌 상태에서 수정 요청 시
     */
    @Override
    public void updatePurchaseOrder(Long id, PurchaseOrderRequestsDTO dto) {
        QPurchaseOrder po = QPurchaseOrder.purchaseOrder;
        QPurchaseOrderDetail pod = QPurchaseOrderDetail.purchaseOrderDetail;
        QStoreMaterial storeMaterial = QStoreMaterial.storeMaterial;

        // 0) 상태 가드: PENDING만 수정 허용
        PurchaseOrderStatus current = queryFactory
                .select(po.status)
                .from(po)
                .where(po.id.eq(id))
                .fetchOne();
        if (current == null) {
            throw new IllegalArgumentException("존재하지 않는 발주 ID: " + id);
        }
        if (current != PurchaseOrderStatus.PENDING) {
            // 컨트롤러에서 409로 매핑할 수 있게 런타임 예외 던짐
            throw new IllegalStateException("현재 상태(" + current + ")에서는 수정할 수 없습니다. 대기중 상태에서만 수정 가능합니다.");
        }

        // 헤더 갱신
        queryFactory.update(po)
                .set(po.priority, dto.getPriority())
                .set(po.remark, dto.getNotes())
                .where(po.id.eq(id))
                .execute();

        List<PurchaseOrderItemDTO> items = Optional.ofNullable(dto.getItems()).orElse(List.of());

        // 요청이 비어있으면 상세는 그대로 두고 헤더 합계만 정합성 맞추고 종료
        if (items.isEmpty()) {
            recalcAndUpdateHeaderTotals(id, pod, po);
            return;
        }

        // 요청 materialId 세트 (0개/음수 필터 아웃)
        Set<Long> reqStoreMaterialIds = items.stream()
                .map(PurchaseOrderItemDTO::getStoreMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!items.isEmpty() && reqStoreMaterialIds.isEmpty()) {
            throw new IllegalStateException("요청 품목에 storeMaterialId가 비어 있습니다. 프론트에서 storeMaterialId를 전송하도록 수정이 필요합니다.");
        }

        // 기존 storeMaterialId 목록
        List<Long> existingStoreMaterialIds = queryFactory
                .select(pod.material.id)
                .from(pod)
                .where(pod.purchaseOrder.id.eq(id))
                .fetch();

        List<Long> deleteBySmIds = existingStoreMaterialIds.stream()
                .filter(mid -> !reqStoreMaterialIds.contains(mid))
                .toList();

        if (!deleteBySmIds.isEmpty()) {
            queryFactory.delete(pod)
                    .where(pod.purchaseOrder.id.eq(id)
                            .and(pod.material.id.in(deleteBySmIds)))
                    .execute();
        }

        // upsert: materialId 기준으로 수량/단가/총액 갱신, 없으면 삽입
        for (PurchaseOrderItemDTO item : items) {
            Long storeMaterialId = item.getStoreMaterialId();
            if (storeMaterialId == null) continue;

            int count = Math.max(0, Optional.ofNullable(item.getCount()).orElse(0));

            // StoreMaterial에서 단가, HQ 매핑 검사
            StoreMaterial sm = queryFactory
                    .selectFrom(storeMaterial)
                    .where(storeMaterial.id.eq(storeMaterialId))
                    .fetchOne();
            if (sm == null) {
                throw new IllegalArgumentException("존재하지 않는 가맹점 재료 ID: " + storeMaterialId);
            }
            if (sm.getMaterial() == null || sm.getMaterial().getId() == null) {
                throw new IllegalStateException("본사 재료와 매핑되지 않은 가맹점 재료입니다. id=" + storeMaterialId);
            }

            BigDecimal unitPrice = sm.getPurchasePrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(count));

            Long detailId = queryFactory
                    .select(pod.id)
                    .from(pod)
                    .where(pod.purchaseOrder.id.eq(id)
                            .and(pod.material.id.eq(storeMaterialId)))
                    .fetchOne();

            if (detailId != null) {
                queryFactory.update(pod)
                        .set(pod.count, count)
                        .set(pod.unitPrice, unitPrice)
                        .set(pod.totalPrice, totalPrice)
                        .where(pod.id.eq(detailId))
                        .execute();
            } else {
                queryFactory.insert(pod)
                        .columns(pod.purchaseOrder.id, pod.material.id, pod.count, pod.unitPrice, pod.totalPrice)
                        .values(id, storeMaterialId, count, unitPrice, totalPrice)
                        .execute();
            }
        }

        // 합계 재계산 후 헤더 반영
        recalcAndUpdateHeaderTotals(id, pod, po);
    }

    /**
     * 특정 발주의 상세 합계를 재계산하여 헤더에 반영한다.
     *
     * <p>
     * - 상세 수량의 count() 로 itemCount 업데이트<br>
     * - totalPrice 합계로 발주 총액 업데이트<br>
     * - 상세가 없으면 품목 수는 0, 총액은 0 으로 세팅한다.
     * </p>
     *
     * @param orderId 발주 ID
     * @param pod     QPurchaseOrderDetail
     * @param po      QPurchaseOrder
     */
    private void recalcAndUpdateHeaderTotals(Long orderId, QPurchaseOrderDetail pod, QPurchaseOrder po) {
        Long itemCount = queryFactory
                .select(pod.count())
                .from(pod)
                .where(pod.purchaseOrder.id.eq(orderId))
                .fetchOne();

        BigDecimal totalAmount = queryFactory
                .select(pod.totalPrice.sum())
                .from(pod)
                .where(pod.purchaseOrder.id.eq(orderId))
                .fetchOne();

        queryFactory
                .update(po)
                .set(po.itemCount, itemCount != null ? itemCount.intValue() : 0)
                .set(po.totalPrice, totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .where(po.id.eq(orderId))
                .execute();
    }

    /**
     * 발주 전체(헤더 + 상세)를 삭제한다.
     *
     * <p>
     * - 존재 여부를 먼저 검사한 뒤, 상세를 모두 삭제하고 헤더를 삭제한다.<br>
     * - FK 제약을 고려해 항상 상세를 먼저 삭제한다.
     * </p>
     *
     * @param id 삭제할 발주 ID
     * @throws IllegalArgumentException 존재하지 않는 발주 ID 인 경우
     */
    @Override
    public void deletePurchaseOrder(Long id) {
        QPurchaseOrder po = QPurchaseOrder.purchaseOrder;
        QPurchaseOrderDetail pod = QPurchaseOrderDetail.purchaseOrderDetail;

        // 1️존재 여부 검증
        Long exists = queryFactory
                .select(po.id.count())
                .from(po)
                .where(po.id.eq(id))
                .fetchOne();

        if (exists == null || exists == 0) {
            throw new IllegalArgumentException("존재하지 않는 발주 ID: " + id);
        }

        // 발주 상세 먼저 삭제 (FK 제약 보호)
        long deletedDetails = queryFactory.delete(pod)
                .where(pod.purchaseOrder.id.eq(id))
                .execute();

        // 발주 헤더 삭제
        long deletedHeader = queryFactory.delete(po)
                .where(po.id.eq(id))
                .execute();

        // 남은 품목 수 다시 계산
        Long remainingCount = queryFactory
                .select(pod.count())
                .from(pod)
                .where(pod.purchaseOrder.id.eq(id))
                .fetchOne();

        // 발주 테이블의 품목 수 업데이트
        queryFactory
                .update(po)
                .set(po.itemCount, remainingCount != null ? remainingCount.intValue() : 0)
                .where(po.id.eq(id))
                .execute();
    }

    /**
     * 단일 발주 상세 품목을 삭제한다.
     *
     * <p>
     * - 해당 상세가 속한 발주 ID를 먼저 조회하고,<br>
     * - 상세 삭제 후 남은 상세 수가 0이면 발주 헤더도 함께 삭제한다.<br>
     * - 남아있으면 품목 수만 갱신한다.
     * </p>
     *
     * @param detailId 삭제할 발주 상세 ID
     * @throws IllegalArgumentException detailId 에 해당하는 상세가 없을 경우
     */
    @Override
    public void deletePurchaseOrderDetail(Long detailId) {
        QPurchaseOrderDetail pod = QPurchaseOrderDetail.purchaseOrderDetail;
        QPurchaseOrder po = QPurchaseOrder.purchaseOrder;

        // 삭제할 상세의 발주 ID 조회
        Long orderId = queryFactory
                .select(pod.purchaseOrder.id)
                .from(pod)
                .where(pod.id.eq(detailId))
                .fetchOne();

        if (orderId == null) {
            throw new IllegalArgumentException("해당 발주 상세가 존재하지 않습니다. detailId=" + detailId);
        }

        // 상세 행 삭제
        queryFactory
                .delete(pod)
                .where(pod.id.eq(detailId))
                .execute();

        // 남은 상세 수 재계산
        Long remainingCount = queryFactory
                .select(pod.count())
                .from(pod)
                .where(pod.purchaseOrder.id.eq(orderId))
                .fetchOne();

        // 남은 상세가 0개면 발주 헤더도 삭제
        if (remainingCount == null || remainingCount == 0) {
            queryFactory
                    .delete(po)
                    .where(po.id.eq(orderId))
                    .execute();
        } else {
            // 아니면 품목 수만 갱신
            queryFactory
                    .update(po)
                    .set(po.itemCount, remainingCount.intValue())
                    .where(po.id.eq(orderId))
                    .execute();
        }
    }

    /**
     * 발주 ID 기준으로 발주 코드를 조회한다.
     *
     * <p>
     * 본사 연동(동기화) 시, 가맹점 발주 ID만 있는 상태에서
     * 발주 코드를 얻기 위해 사용한다.
     * </p>
     *
     * @param id 발주 ID
     * @return 발주 코드(Optional)
     */
    @Override
    public Optional<String> findOrderCodeById(Long id) {
        QPurchaseOrder po = QPurchaseOrder.purchaseOrder;

        String code = queryFactory
                .select(po.orderCode)
                .from(po)
                .where(po.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(code);
    }

    /**
     * 발주 ID 기준으로 발주 상태를 변경한다.
     *
     * <p>
     * - HQ 동기화, 가맹점 화면 등에서 공통으로 사용하는 상태 변경용 쿼리<br>
     * - flush / clear 를 수동으로 호출하여 1차 캐시와 실제 DB 상태를 맞춘다.
     * </p>
     *
     * @param id     발주 ID
     * @param status 변경할 상태
     * @return 업데이트된 행 수 (0 또는 1)
     */
    @Override
    public int updateStatusById(Long id, PurchaseOrderStatus status) {
        QPurchaseOrder po = QPurchaseOrder.purchaseOrder;

        em.flush();

        var update = queryFactory
                .update(po)
                .set(po.status, status);

        // 검수완료로 바뀌는 경우에만 실제납기일을 오늘 날짜로 세팅
        if (status == PurchaseOrderStatus.DELIVERED) {
            update.set(po.actualDeliveryDate, LocalDate.now());
        }

        long updated = update
                .where(po.id.eq(id))
                .execute();

        em.clear();

        return (int) updated;
    }

    @Override
    public int updateStatusByOrderCode(String orderCode, PurchaseOrderStatus status) {
        QPurchaseOrder po = QPurchaseOrder.purchaseOrder;

        em.flush();

        var update = queryFactory
                .update(po)
                .set(po.status, status);

        if (status == PurchaseOrderStatus.DELIVERED) {
            update.set(po.actualDeliveryDate, LocalDate.now());
        }

        long updated = update
                .where(po.orderCode.eq(orderCode))
                .execute();

        em.clear();

        return (int) updated;
    }
}
