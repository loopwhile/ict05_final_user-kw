import api from '../lib/authApi';
import type {
  StoreInventoryResponse,
  StoreInventoryInWriteDTO,
  StoreInventoryAdjustmentWriteDTO,
} from '../types/storeInventory';

const BASE_PATH = '/API/store/inventory';

/**
 * 가맹점 재고 초기화
 *
 * - 지정 매장에 대해 StoreMaterial 기준으로 StoreInventory를 0부터 생성한다.
 * - 이미 존재하는 (store, storeMaterial) 조합은 건너뛰고, 없는 것만 새로 추가한다.
 *
 * POST /API/store/inventory/init
 *
 * JWT(@AuthenticationPrincipal) 기반: 파라미터 없음
 * @returns 새로 생성된 StoreInventory 행 개수
 */
export async function initStoreInventory(): Promise<number> {
  const res = await api.post<number>(`${BASE_PATH}/init`);
  return res.data;
}

/**
 * 가맹점 재고 목록 조회
 *
 * - InventoryManagement 화면의 메인 그리드 데이터 소스
 * - StoreInventory + StoreMaterial 조인 결과를 내려주는 백엔드 DTO와 1:1로 매핑
 *
 * GET /API/store/inventory/list
 * 
 * JWT(@AuthenticationPrincipal) 기반: 파라미터 없음
 * @returns 재고 목록 DTO 배열
 */
export async function fetchStoreInventory(): Promise<StoreInventoryResponse[]> {
  const res = await api.get<StoreInventoryResponse[]>(`${BASE_PATH}/list`);
  return res.data;
}

/** 입고(inbound) 등록
 * - POST /API/store/inventory/in
 * - 반환: 생성된 입고 PK (백엔드가 Long을 반환한다고 가정)
 */
export async function inboundStoreInventory(payload: StoreInventoryInWriteDTO): Promise<number> {
  if (
    !payload ||
    typeof payload.storeInventoryId !== 'number' ||
    typeof payload.storeMaterialId !== 'number' ||  // 추가 검증
    typeof payload.quantity !== 'number' ||
    Number.isNaN(payload.quantity) ||
    payload.quantity < 0
  ) {
    throw new Error('유효하지 않은 입고 요청입니다.');
  }
  const res = await api.post<number>(`${BASE_PATH}/in`, payload);
  return res.data;
}

/**
 * 재고 조정
 *
 * - 엔드포인트: POST /API/store/inventory/adjust
 * - 계약: 컨트롤러가 인증 사용자(AppUser)의 storeId를 내부에서 사용
 * - 보완:
 *   - 백엔드: storeInventoryId 소유권(storeId) 검증 필수
 *   - 프런트: 음수/NaN 방지 등 기본 유효성 검사 후 호출 권장
 */
export async function adjustStoreInventory(
  payload: StoreInventoryAdjustmentWriteDTO,
): Promise<void> {
  if (
    payload == null ||
    typeof payload.storeInventoryId !== 'number' ||
    typeof payload.newQuantity !== 'number' ||
    Number.isNaN(payload.newQuantity) ||
    payload.newQuantity < 0
  ) {
    throw new Error('유효하지 않은 조정 요청입니다.');
  }

  await api.post(`${BASE_PATH}/adjust`, payload);
}
