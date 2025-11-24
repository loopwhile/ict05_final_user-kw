/**
 * 가맹점 재고 도메인 타입 정의
 *
 * - StoreInventory / StoreInventoryBatch 와 연결되는 프런트 DTO
 * - InventoryManagement 화면의 메인 데이터 소스
 * - 모든 수량은 "소진 단위" 기준(프로젝트 정책)으로 해석한다.
 */

import type { MaterialTemperature } from './storeMaterial';

/** 재고 상태 (백엔드 InventoryStatus Enum과 1:1) */
export type StoreInventoryStatus = 'SUFFICIENT' | 'LOW' | 'SHORTAGE';

/** 조정 사유(백엔드 AdjustmentReason Enum과 1:1) */
export type AdjustmentReason =
  | 'MANUAL'
  | 'DAMAGE'
  | 'LOSS'
  | 'ERROR'
  | 'REAL_AUDIT';

/**
 * 가맹점 재고 목록/상세 응답 DTO
 *
 * - GET /API/store/inventory/list
 * - StoreInventory + StoreMaterial 조인 결과를 한 번에 내려받는다.
 * - 백엔드 StoreInventoryListDTO 와 필드명/해석을 맞춘다.
 *
 * 주의:
 * - 백엔드가 id를 storeInventoryId 또는 id 로 내려줄 수 있어 둘 다 허용(옵셔널)
 * - 금액 필드는 "입고 단위" 기준 금액(매입 단가)
 */
export interface StoreInventoryResponse {
  /** 집계 재고 PK (백엔드가 storeInventoryId 또는 id 중 하나만 보낼 수 있음) */
  storeInventoryId?: number;
  id?: number;

  /** 가맹점 재료 PK (store_material_id_fk) */
  storeMaterialId: number;

  /** 가맹점 재료명 */
  name: string;

  /** 카테고리 (문자열, 없을 수 있음) */
  category: string | null;

  /** 소진 단위 (예: 개, g, 샷) */
  baseUnit: string | null;

  /** 입고 단위 (예: 박스, 봉, kg) */
  salesUnit: string | null;

  /** 현재 재고 수량(소진 단위) */
  quantity: number | null;

  /** 적정 재고 수량(소진 단위) */
  optimalQuantity: number | null;

  /** 최근 매입 단가(입고 단위 기준) */
  purchasePrice: number | null;

  /** 공급업체명 */
  supplier: string | null;

  /** 보관 온도 */
  temperature: MaterialTemperature | null;

  /** 본사 재료 여부 (true=본사 재료, false=가맹점 자체 재료) */
  hqMaterial: boolean;

  /** 가장 가까운 유통기한(YYYY-MM-DD) */
  nearestExpireDate: string | null;

  /** 최근 재고 갱신일(YYYY-MM-DD 또는 DATETIME 문자열) */
  lastUpdated: string | null;

  /** 재고 상태 (SUFFICIENT / LOW / SHORTAGE) */
  status: StoreInventoryStatus;
}

/**
 * (레거시) 가맹점 재고 '재입고' 요청 DTO
 *
 * - 엔드포인트: POST /API/store/inventory/restock
 * - 역할: 수량만 증가(단가/배치 정책 없이)
 * - 현재 신규 입고는 StoreInventoryInWriteDTO 사용을 권장
 */
export interface StoreInventoryRestockRequest {
  /** 집계 재고 PK (store_inventory_id) */
  storeInventoryId: number;

  /** 입고 수량(소진 단위) */
  quantity: number;

  /** 메모(선택) */
  memo?: string | null;
}

/**
 * 가맹점 '입고(inbound)' 요청 DTO
 *
 * - 엔드포인트: POST /API/store/inventory/in
 * - 수량은 "소진 단위" 기준
 * - HQ 재료면 unitPrice 생략(백엔드가 정책대로 보정)
 */
export interface StoreInventoryInWriteDTO {
  /** 집계 재고 PK (store_inventory_id) */
  storeInventoryId: number;
  /** 가맹점 재료 PK (store_material_id) */
  storeMaterialId: number;
  /** 입고 수량(0 이상) */
  quantity: number;
  /** 메모(선택) */
  memo?: string | null;
  /** 가맹점 자체 재료일 때만 보냄(선택). HQ 재료면 생략 */
  unitPrice?: number;

  // 필요 시 확장(백엔드 DTO가 지원 시)
  // receivedDate?: string | null;   // 'YYYY-MM-DD'
  // expirationDate?: string | null; // 'YYYY-MM-DD'
  // lotNo?: string | null;
}

/**
 * StoreInventory 도메인: 재고 조정 요청 DTO
 *
 * - 목적: 절대값 기반의 재고 수량을 지정 사유와 함께 조정
 * - 엔드포인트: POST /API/store/inventory/adjust
 * - 백엔드는 인증 컨텍스트의 storeId와 storeInventoryId 소유권을 검증
 */
export interface StoreInventoryAdjustmentWriteDTO {
  /** 집계 재고 PK (store_inventory_id) – 선택된 행의 고유키 */
  storeInventoryId: number;

  /** 선택: 가맹점 재료 PK (백엔드에서 불필요하면 보내지 않아도 됨) */
  storeMaterialId?: number;

  /** 조정 후 최종 재고 수량(절대값). 0 이상 */
  newQuantity: number;

  /** 조정 사유 (백엔드 AdjustmentReason.valueOf 로 매핑) */
  reason: AdjustmentReason;

  /** 선택 메모 */
  memo?: string | null;
}
