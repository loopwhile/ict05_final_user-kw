/**
 * 가맹점 재료 도메인 공통 타입 정의
 *
 * - 백엔드 Material / StoreMaterial 엔티티와 1:1은 아니지만,
 *   주요 Enum/DTO 구조/명세를 그대로 맞춘다.
 * - 수량/금액의 단위 기준에 유의:
 *   - 수량: 기본적으로 "소진 단위"
 *   - 금액: 기본적으로 "입고 단위(매입 단가)"
 */

/** 가맹점 재료 상태 */
export type MaterialStatus = 'USE' | 'STOP';

/** StoreMaterialStatus 는 MaterialStatus 의 별칭(동일 의미) */
export type StoreMaterialStatus = MaterialStatus;

/** 보관 온도 */
export type MaterialTemperature = 'TEMPERATURE' | 'REFRIGERATE' | 'FREEZE';

/** 재료 카테고리 (본사 MaterialCategory Enum과 동일) */
export type MaterialCategory =
  | 'BASE'
  | 'TOPPING'
  | 'SIDE'
  | 'SAUCE'
  | 'BEVERAGE'
  | 'PACKAGE'
  | 'ETC';

/**
 * 가맹점 재료 등록 요청 DTO
 *
 * - 엔드포인트: POST /API/store/material
 * - 백엔드 StoreMaterialCreateDTO 와 필드명을 최대한 맞춘다.
 * - 단위/환산:
 *   - baseUnit: 소진 단위
 *   - salesUnit: 입고 단위
 *   - conversionRate: 입고→소진 환산 (예: 1박스=100ea → 100)
 */
export interface StoreMaterialCreateRequest {
  /** 본사 재료 매핑 여부 (true=본사 재료, false=가맹점 자체 재료) */
  hqMaterial?: boolean | null;

  /** 본사 재료 PK (hq material id). hqMaterial=true 일 때 의미 있음 */
  materialId?: number | null;

  /** 가맹점 표시명 (재료명) */
  name: string;

  /** 카테고리 (MaterialCategory) */
  category?: MaterialCategory | null;

  /** 소진 단위 (예: 개, g, 샷) */
  baseUnit?: string | null;

  /** 입고 단위 (예: 박스, 봉, kg) */
  salesUnit?: string | null;

  /** 변환비율(입고→소진). 예: 1박스=100ea → 100 */
  conversionRate?: number | null;

  /** 공급업체명 (대표/최근 공급처) */
  supplier?: string | null;

  /** 보관온도 */
  temperature?: MaterialTemperature | null;

  /** 적정 재고(소진 단위) */
  optimalQuantity?: number | null;

  /** 최근 매입 단가(입고 단위 기준) */
  purchasePrice?: number | null;
}

/**
 * 가맹점 재료 목록/상세 응답 DTO
 *
 * - 엔드포인트: GET /API/store/material/list
 * - 다른 화면에서도 재사용 가능하도록 넉넉한 필드 포함
 */
export interface StoreMaterialResponse {
  /** 가맹점 재료 PK (store_material_id) */
  id: number;

  /** 가맹점 ID (store_id_fk) */
  storeId: number;

  /** 가맹점 재료 코드 */
  code: string;

  /** 가맹점 재료명(표시명) */
  name: string;

  /** 카테고리 */
  category: MaterialCategory | null;

  /** 소진 단위 */
  baseUnit: string | null;

  /** 입고 단위 */
  salesUnit: string | null;

  /** 재료 상태 (USE/STOP) */
  status: MaterialStatus;

  /** 적정 재고(소진 단위) */
  optimalQuantity: number | null;

  /** 최근 매입 단가(입고 단위 기준) */
  purchasePrice: number | null;

  /** 공급업체명 */
  supplier: string | null;

  /** 보관온도 */
  temperature: MaterialTemperature | null;

  /** 본사 재료 여부 (true=본사 재료, false=가맹점 자체 재료) */
  hqMaterial: boolean;
}

/**
 * 가맹점 재료 적정재고(최소 재고) 업데이트 DTO
 *
 * - 목적: 상세 팝업에서 최소 재고(적정재고) 저장
 * - 비고: null 허용(미설정), 소진 단위 기준
 */
export interface StoreMaterialUpdateOptimalDTO {
  /** 가맹점 재료 PK (store_material_id) */
  storeMaterialId: number;

  /** 적정 재고 수량(소진 단위). null 이면 미설정 */
  optimalQuantity: number | null;
}

/**
 * 가맹점 재료 상태(사용/중지) 업데이트 DTO
 *
 * - 목적: 본사 재료 사용 토글 on/off
 * - 비고: 본사 재료가 아닌 경우 토글을 숨기거나 요청 자체를 막는 편이 안전
 */
export interface StoreMaterialUpdateStatusDTO {
  /** 가맹점 재료 PK (store_material_id) */
  storeMaterialId: number;

  /** 'USE' | 'STOP' */
  status: StoreMaterialStatus;
}
