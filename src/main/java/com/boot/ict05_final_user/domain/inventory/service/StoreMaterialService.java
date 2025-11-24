package com.boot.ict05_final_user.domain.inventory.service;

import com.boot.ict05_final_user.domain.inventory.dto.StoreMaterialCreateDTO;
import com.boot.ict05_final_user.domain.inventory.dto.StoreMaterialResponse;
import com.boot.ict05_final_user.domain.inventory.entity.InventoryStatus;
import com.boot.ict05_final_user.domain.inventory.entity.StoreInventory;
import com.boot.ict05_final_user.domain.inventory.repository.StoreInventoryRepository;
import com.boot.ict05_final_user.domain.inventory.entity.Material;
import com.boot.ict05_final_user.domain.inventory.entity.MaterialStatus;
import com.boot.ict05_final_user.domain.inventory.entity.StoreMaterial;
import com.boot.ict05_final_user.domain.inventory.repository.MaterialRepository;
import com.boot.ict05_final_user.domain.inventory.repository.StoreMaterialRepository;
import com.boot.ict05_final_user.domain.store.entity.Store;
import com.boot.ict05_final_user.domain.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 가맹점 재료(StoreMaterial) 관리 서비스.
 *
 * <p>역할</p>
 * <ul>
 *   <li>본사 재료(Material)를 가맹점에 일괄 매핑하고, 누락된 집계 재고(StoreInventory)를 0으로 생성.</li>
 *   <li>가맹점 자체 재료 생성(코드 자동 발급, 기본 상태/옵션 부여) 및 초기 재고 자동 생성.</li>
 *   <li>가맹점 재료 목록 조회.</li>
 *   <li>기존 가맹점 재료 전수에 대해 집계 재고 누락분만 0으로 보충 생성.</li>
 *   <li>가맹점 재료의 적정재고/상태 업데이트.</li>
 * </ul>
 *
 * <p>정책/규칙</p>
 * <ul>
 *   <li>초기 집계 재고는 quantity=0으로 생성하며 {@link StoreInventory#touchAfterQuantityChange()}로
 *       상태({@link InventoryStatus}) 및 업데이트 시각을 동기화한다.</li>
 *   <li>본사 재료 매핑 시 기본 상태는 {@link MaterialStatus#STOP}으로 생성(가맹점에서 활성화 전까지 비사용).</li>
 *   <li>가맹점 자체 재료 생성 시 상태는 {@link MaterialStatus#USE}로 시작.</li>
 *   <li>단가/배치/유통기한 등은 본 서비스의 책임 범위를 벗어난다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreMaterialService {

    private final StoreRepository storeRepository;
    private final MaterialRepository materialRepository;
    private final StoreMaterialRepository storeMaterialRepository;
    private final StoreInventoryRepository storeInventoryRepository;

    /**
     * 선택 가맹점에 대해 "본사 재료 → 가맹점 재료" 일괄 매핑.
     *
     * <p>처리 흐름</p>
     * <ol>
     *   <li>매장 존재 검증.</li>
     *   <li>{@link MaterialStatus#USE} 인 본사 재료 목록 조회.</li>
     *   <li>이미 매핑된 재료는 건너뛰고, 미매핑 재료에 대해 {@link StoreMaterial} 생성:
     *       <ul>
     *         <li>status=STOP, isHqMaterial=true</li>
     *         <li>코드/이름/단위/카테고리/온도/변환비율 등은 본사 재료 값 복사</li>
     *       </ul>
     *   </li>
     *   <li>각 신규 StoreMaterial에 대해 집계 재고를 quantity=0으로 생성 후 {@code touchAfterQuantityChange()} 호출.</li>
     * </ol>
     *
     * @param storeId 매장 ID
     * @return 새로 생성된 {@link StoreMaterial} 개수
     * @throws IllegalArgumentException 매장이 존재하지 않을 때
     */
    public int mapAllHqMaterialsToStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매장: " + storeId));

        // 본사에서 사용 중인 재료만 매핑
        List<Material> hqMaterials = materialRepository.findByMaterialStatus(MaterialStatus.USE);

        int created = 0;

        for (Material material : hqMaterials) {
            if (storeMaterialRepository.existsByStoreAndMaterial(store, material)) {
                continue;   // 이미 매핑된 재료는 스킵
            }

            // 코드/이름/단위/카테고리는 본사 재료에서 기본값 복사
            StoreMaterial storeMaterial = StoreMaterial.builder()
                    .store(store)
                    .material(material)
                    .code(material.getCode())                  // 필요 시 점포 prefix 적용 가능
                    .name(material.getName())
                    .category(material.getMaterialCategory().name())
                    .baseUnit(material.getBaseUnit())
                    .salesUnit(material.getSalesUnit())
                    .conversionRate(material.getConversionRate())
                    .supplier(null)                            // 가맹점에서 별도 입력
                    .temperature(material.getMaterialTemperature())
                    .status(MaterialStatus.STOP)               // 기본: 미사용
                    .optimalQuantity(null)                     // 가맹점이 나중에 입력
                    .purchasePrice(null)                       // 가맹점 최근 단가 저장소와 연계 시 사용
                    .isHqMaterial(true)
                    .build();

            storeMaterialRepository.save(storeMaterial);

            // 가맹점 재고도 같이 0으로 생성
            StoreInventory inventory = StoreInventory.builder()
                    .store(store)
                    .storeMaterial(storeMaterial)
                    .quantity(BigDecimal.ZERO)
                    .optimalQuantity(null)
                    .status(InventoryStatus.SUFFICIENT)        // 적정재고 미지정 상태이므로 일단 SUFFICIENT
                    .build();

            inventory.touchAfterQuantityChange();               // updateDate, status 정합성 유지
            storeInventoryRepository.save(inventory);

            created++;
        }

        return created;
    }

    /**
     * 가맹점 자체 재료 등록.
     *
     * <p>정책</p>
     * <ul>
     *   <li>가맹점 재료 코드는 서버에서 자동 생성(간단 규칙).</li>
     *   <li>본 메서드는 본사 재료 FK / isHqMaterial 처리하지 않음(별도 매핑 서비스 책임).</li>
     *   <li>재료 상태는 항상 {@link MaterialStatus#USE} 로 시작.</li>
     *   <li>신규 재료에 대한 집계 재고를 0으로 자동 생성하여 상태/업데이트일시를 동기화.</li>
     * </ul>
     *
     * @param dto 등록 요청 DTO
     * @return 생성된 {@link StoreMaterial} ID
     * @throws EntityNotFoundException 가맹점이 존재하지 않을 때
     */
    @Transactional
    public Long create(StoreMaterialCreateDTO dto) {
        // 1) 가맹점 조회
        Store store = storeRepository.findById(dto.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("가맹점을 찾을 수 없습니다. id=" + dto.getStoreId()));

        // 2) 코드 설정 (없으면 간단 자동 생성)
        String code = generateStoreMaterialCode(store);

        // 3) 변환비율: null 또는 0 이하면 기본값(100) 적용
        int conversionRate =
                (dto.getConversionRate() != null && dto.getConversionRate() > 0)
                        ? dto.getConversionRate()
                        : 100;

        // 4) 엔티티 생성
        StoreMaterial storeMaterial = StoreMaterial.builder()
                .store(store)
                .code(code)
                .name(dto.getName())
                .category(dto.getCategory() != null ? dto.getCategory().name() : null)
                .baseUnit(dto.getBaseUnit())
                .salesUnit(dto.getSalesUnit())
                .conversionRate(conversionRate)
                .supplier(dto.getSupplier())
                .temperature(dto.getTemperature())
                .status(MaterialStatus.USE)
                .optimalQuantity(nullSafe(dto.getOptimalQuantity()))
                .purchasePrice(nullSafe(dto.getPurchasePrice()))
                .isHqMaterial(false)
                .build();

        storeMaterialRepository.save(storeMaterial);

        // 5) 신규 가맹점 재료에 대한 재고 자동 생성(없을 때만)
        if (!storeInventoryRepository.existsByStoreAndStoreMaterial(store, storeMaterial)) {
            StoreInventory inventory = StoreInventory.builder()
                    .store(store)
                    .storeMaterial(storeMaterial)
                    .quantity(BigDecimal.ZERO)                         // 초기 재고 0
                    .optimalQuantity(nullSafe(dto.getOptimalQuantity())) // 적정 재고는 DTO 기준 복사
                    .status(InventoryStatus.SUFFICIENT)               // 초기값, 실상태는 touch에서 재계산
                    .build();

            inventory.touchAfterQuantityChange();                     // 상태/업데이트일 동기화
            storeInventoryRepository.save(inventory);
        }

        return storeMaterial.getId();
    }

    /**
     * 지정 매장의 가맹점 재료 목록 조회.
     *
     * @param storeId 매장 ID
     * @return 가맹점 재료 응답 DTO 리스트
     * @throws IllegalArgumentException 매장이 존재하지 않을 때
     */
    @Transactional(readOnly = true)
    public List<StoreMaterialResponse> getStoreMaterials(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매장입니다. id=" + storeId));

        List<StoreMaterial> list = storeMaterialRepository.findByStore(store);
        return list.stream()
                .map(StoreMaterialResponse::from)
                .toList();
    }

    /**
     * store_material 에 존재하는 모든 가맹점 재료에 대해,
     * 아직 store_inventory 가 없는 경우에만 0 재고로 생성.
     *
     * <p>대상/규칙</p>
     * <ul>
     *   <li>대상: store_material.store_id_fk = storeId 인 모든 행(HQ/자체 포함).</li>
     *   <li>이미 store_inventory 가 있는 (store, storeMaterial)은 스킵.</li>
     *   <li>생성 시 quantity=0, status=SUFFICIENT 후 {@code touchAfterQuantityChange()}로 동기화.</li>
     * </ul>
     *
     * @param storeId 매장 ID
     * @return 새로 생성된 {@link StoreInventory} 개수
     * @throws IllegalArgumentException 매장이 존재하지 않을 때
     */
    @Transactional
    public int initStoreInventoryForStore(Long storeId) {
        // 1) 매장 조회
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매장입니다. id=" + storeId));

        // 2) 이 매장의 모든 가맹점 재료 (HQ/자체 모두 포함)
        List<StoreMaterial> materials = storeMaterialRepository.findByStore(store);
        if (materials.isEmpty()) {
            return 0;
        }

        // 3) 이미 재고가 있는 StoreMaterial ID 집합
        List<StoreInventory> existingInventories = storeInventoryRepository.findByStore(store);
        Set<Long> alreadyHasInventory = new HashSet<>();
        for (StoreInventory inv : existingInventories) {
            if (inv.getStoreMaterial() != null) {
                alreadyHasInventory.add(inv.getStoreMaterial().getId());
            }
        }

        int created = 0;

        // 4) 아직 재고 없는 재료만 0 재고로 생성
        for (StoreMaterial sm : materials) {
            if (alreadyHasInventory.contains(sm.getId())) {
                continue;
            }

            StoreInventory inventory = StoreInventory.builder()
                    .store(store)
                    .storeMaterial(sm)
                    .quantity(BigDecimal.ZERO)     // 모든 재료 재고 0으로 생성
                    .optimalQuantity(null)         // 적정재고는 여기선 보정하지 않음
                    .status(InventoryStatus.SUFFICIENT)
                    .build();

            inventory.touchAfterQuantityChange(); // status + updateDate 동기화
            storeInventoryRepository.save(inventory);
            created++;
        }

        return created;
    }

    /* ===== 내부 유틸리티 ===== */

    /** 가맹점 재료 코드 간단 자동 생성(유니크 제약 충돌 시 1회 재시도). */
    private String generateStoreMaterialCode(Store store) {
        String code = "SM-" + store.getId() + "-" + System.currentTimeMillis();

        // 유니크 제약 충돌 방지용 한 번 더 시도
        if (storeMaterialRepository.existsByStoreAndCode(store, code)) {
            code = "SM-" + store.getId() + "-" + (System.currentTimeMillis() + 1);
        }
        return code;
    }

    /** BigDecimal null 안전 처리. null이면 0 반환. */
    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * 적정재고(최소 재고) 업데이트.
     *
     * @param storeId         인증 사용자 매장 ID
     * @param storeMaterialId 가맹점 재료 PK
     * @param optimalQuantity 소진 단위 기준 수량(null 허용, 0 이상)
     * @throws IllegalArgumentException 권한 불일치/입력 값 음수
     */
    @Transactional
    public void updateOptimalQuantity(Long storeId, Long storeMaterialId, Double optimalQuantity) {
        StoreMaterial sm = storeMaterialRepository
                .findByIdAndStore_Id(storeMaterialId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("재료가 존재하지 않거나 권한이 없습니다."));

        if (optimalQuantity != null && optimalQuantity < 0) {
            throw new IllegalArgumentException("적정 재고는 0 이상이어야 합니다.");
        }

        // 엔티티 필드 타입에 맞춰 세팅 : BigDecimal 사용 시 변환
        sm.setOptimalQuantity(optimalQuantity != null ? BigDecimal.valueOf(optimalQuantity) : null);
    }

    /**
     * 가맹점 재료 상태(사용/중지) 업데이트.
     *
     * @param storeId         인증 사용자 매장 ID
     * @param storeMaterialId 가맹점 재료 PK
     * @param status          {@link MaterialStatus#USE} | {@link MaterialStatus#STOP}
     * @throws IllegalArgumentException 권한 불일치
     */
    @Transactional
    public void updateStatus(Long storeId, Long storeMaterialId, MaterialStatus status) {
        StoreMaterial sm = storeMaterialRepository
                .findByIdAndStore_Id(storeMaterialId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("재료가 존재하지 않거나 권한이 없습니다."));

        sm.setStatus(status);
    }
}
