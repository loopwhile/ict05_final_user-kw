/**
 * InventoryManagement.tsx
 *
 * 가맹점 재고 관리 화면
 * - 목록, 검색/페이징, 입고, 재고조정, 발주 장바구니
 * - 백엔드 계약:
 *   - GET  /API/store/inventory/list           → StoreInventoryResponse[]
 *   - POST /API/store/inventory/init           → number (생성된 StoreInventory 개수)
 *   - POST /API/store/inventory/in             → number (생성된 입고 PK)
 *   - POST /API/store/inventory/adjust         → void
 *   - POST /API/store/material                  → number | {id: number}
 *
 * 단위 규칙:
 *   - 수량(quantity): "소진 단위" 기준
 *   - 금액(unitPrice/purchasePrice): "입고 단위" 기준
 */

import React, { useEffect, useState, useRef, useMemo } from 'react';
import { DataTable, Column } from '../Common/DataTable';
import { FormModal } from '../Common/FormModal';
import { ConfirmDialog, useConfirmDialog } from '../Common/ConfirmDialog';
import { StatusBadge } from '../Common/StatusBadge';
import { Card } from '../ui/card';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { Progress } from '../ui/progress';
import api from '../../lib/authApi';
import {
  Package,
  AlertTriangle,
  ShoppingCart,
  Truck,
  Calendar,
  Plus,
  Settings
} from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '../ui/dialog';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Checkbox } from '../ui/checkbox';
import type { CheckedState } from '@radix-ui/react-checkbox';
import { toast } from 'sonner';

import {
  createStoreMaterial,
  fetchStoreMaterials // 현재 화면에서는 사용하지 않지만, 재사용 고려해 유지
} from '../../services/storeMaterialApi';

import {
  fetchStoreInventory,
  inboundStoreInventory,
  initStoreInventory,
  adjustStoreInventory,
} from '../../services/storeInventoryApi';

import type {
  StoreMaterialCreateRequest,
  MaterialTemperature,
  MaterialStatus,
} from '../../types/storeMaterial';

import type {
  StoreInventoryResponse,
  StoreInventoryInWriteDTO
} from '../../types/storeInventory';

import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationPrevious,
  PaginationNext,
} from '../ui/pagination';

/* =========================================================================
   로컬 타입 정의 (백엔드 DTO → 테이블/모달에서 쓰기 쉬운 형태로 변환)
   ========================================================================= */
type StockStatus = 'sufficient' | 'low' | 'shortage';
type OrderStatus = 'pending' | 'approved' | 'shipping' | 'delivered';

/**
 * 재고 상태 계산 (백엔드 InventoryStatus.from 규칙과 동일한 의미의 프런트 가드)
 * - current == null | <= 0  → shortage
 * - optimal == null | <= 0  → sufficient
 * - current < optimal       → low
 * - else                    → sufficient
 */
function calcStockStatus(
  current: number | null | undefined,
  optimal: number | null | undefined,
): StockStatus {
  if (current == null || current <= 0) return 'shortage';
  if (optimal == null || optimal <= 0) return 'sufficient';
  return current < optimal ? 'low' : 'sufficient';
}

/** 상태별 배지/색상 메타 */
function getStockStatusDisplay(status: StockStatus) {
  switch (status) {
    case 'shortage':
      return { status, text: '품절', textColor: 'text-red-600', badgeClass: 'bg-red-100 text-red-800' };
    case 'low':
      return { status, text: '부족', textColor: 'text-orange-600', badgeClass: 'bg-orange-100 text-orange-800' };
    case 'sufficient':
    default:
      return { status: 'sufficient' as StockStatus, text: '충분', textColor: 'text-green-600', badgeClass: 'bg-green-100 text-green-800' };
  }
}

/** 화면 테이블용 아이템 */
interface InventoryItem {
  id: number;                   // 테이블 row key / 체크박스용
  storeInventoryId: number;     // 집계 재고 PK
  storeMaterialId: number;      // 가맹점 재료 PK
  materialId: number;           // 과거 하위 호환(=storeMaterialId). 필요 없으면 제거 가능.
  name: string;
  category: string;
  currentStock: number;
  optimalQuantity: number;      // 화면은 최소/최대 대신 적정재고 단일 개념 사용
  unit: string;                 // 소진 단위
  unitPrice: number;            // 입고 단위 기준 금액 (없으면 0)
  lastRestocked: string;        // ISO 또는 YYYY-MM-DD
  expiryDate: string;           // YYYY-MM-DD
  supplier: string;
  status: StockStatus;
  weeklyUsage: number;          // 현재 미사용. 향후 분석 연동 시 사용.
  hqMaterial?: boolean;         // 본사 제품 여부
}

interface OrderItem {
  name: string;
  quantity: number;
  unit: string;
  unitPrice: number;
}

interface Order {
  id: string;
  items: OrderItem[];
  supplier: string;
  orderDate: string;
  expectedDate: string;
  status: OrderStatus;
  total: number;
}

/** 발주 DTO (백엔드 PurchaseOrderRequestsDTO와 호환) */
interface PurchaseOrderItemDTO {
  storeMaterialId: number;
  count: number;
}
interface PurchaseOrderRequestsDTO {
  priority: 'NORMAL' | 'URGENT';
  notes?: string;
  items: PurchaseOrderItemDTO[];
}

/** 장바구니 확장 타입 */
type CartItem = InventoryItem & { orderQuantity: number; totalPrice: number };

/* =========================================================================
   유틸 매핑
   ========================================================================= */
function mapCategoryLabel(cat?: string | null): string {
  switch (cat) {
    case 'BASE':     return '주재료(BASE)';
    case 'TOPPING':  return '토핑/부재료(TOPPING)';
    case 'SIDE':     return '사이드(SIDE)';
    case 'SAUCE':    return '소스/조미료(SAUCE)';
    case 'BEVERAGE': return '음료(BEVERAGE)';
    case 'PACKAGE':  return '포장재(PACKAGE)';
    case 'ETC':      return '기타(ETC)';
    default:         return '미분류';
  }
}

/** 재료 상태를 재고 뱃지로 단순 매핑 (지금은 STOP이면 품절 느낌) */
function mapStatus(smStatus: MaterialStatus): StockStatus {
  return smStatus === 'STOP' ? 'shortage' : 'sufficient';
}

/** 백엔드 재고 상태 문자열 → 프런트 상태로 변환 */
function toStockStatus(domainStatus?: string | null): StockStatus {
  switch (domainStatus) {
    case 'SHORTAGE': return 'shortage';
    case 'LOW':      return 'low';
    case 'SUFFICIENT':
    default:         return 'sufficient';
  }
}

/**
 * StoreInventoryResponse → 화면용 InventoryItem 변환
 * - 백엔드 필드가 일부 없더라도 UI가 깨지지 않도록 안전 가드 적용
 */
function mapStoreInventoryToInventoryItem(si: StoreInventoryResponse): InventoryItem {
  const optimal   = si.optimalQuantity ?? 0;
  const unit      = si.baseUnit || '개';
  const quantity  = si.quantity ?? 0;

  // id 필드 가변 대응 (storeInventoryId 또는 id)
  const storeInventoryId = (si.storeInventoryId ?? si.id)!;
  const storeMaterialId  = si.storeMaterialId!;

  const status: StockStatus = toStockStatus(si.status ?? 'SUFFICIENT');

  const lastRestocked = si.lastUpdated ?? '';
  const expiryDate    = si.nearestExpireDate
    ?? new Date(Date.now() + 7*24*60*60*1000).toISOString().split('T')[0]; // 기본 7일 후
  const unitPrice     = si.purchasePrice ?? 0;
  const supplier      = si.supplier ?? (si.hqMaterial ? '본사' : '');

  return {
    id: storeInventoryId,
    storeInventoryId,
    storeMaterialId,
    materialId: storeMaterialId, // 하위 호환. 제거 가능.
    name: si.name,
    category: si.category ?? '기타',
    currentStock: quantity,
    optimalQuantity: optimal,
    unit,
    unitPrice,
    lastRestocked,
    expiryDate,
    supplier,
    status,
    weeklyUsage: 0,
    hqMaterial: si.hqMaterial ?? undefined,
  };
}

/* =========================================================================
   메인 컴포넌트
   ========================================================================= */
export function InventoryManagement() {
  // 화면 상태
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [filteredInventory, setFilteredInventory] = useState<InventoryItem[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(1);
  const [hasInventory, setHasInventory] = useState<boolean>(false);
  const [orders, setOrders] = useState<Order[]>([]); // 샘플 대신 런타임 추가
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [isCartModalOpen, setIsCartModalOpen] = useState(false);
  const [modalType, setModalType] = useState<'restock' | 'adjust' | 'order' | 'register'>('restock');
  const [selectedItem, setSelectedItem] = useState<InventoryItem | null>(null);
  const [selectedItems, setSelectedItems] = useState<number[]>([]);
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const { dialog } = useConfirmDialog();

  const submitOrderRef = useRef(false);

  // 필터/카운트(메모이제이션은 과하지 않게 유지)
  const inventoryFilters = [
    { label: '충분',   value: 'sufficient', count: inventory.filter(i => i.status === 'sufficient').length },
    { label: '부족',   value: 'low',        count: inventory.filter(i => i.status === 'low').length },
    { label: '품절',   value: 'shortage',   count: inventory.filter(i => i.status === 'shortage').length }
  ];
  const orderFilters = [
    { label: '대기중', value: 'pending',   count: orders.filter(o => o.status === 'pending').length },
    { label: '승인됨', value: 'approved',  count: orders.filter(o => o.status === 'approved').length },
    { label: '배송중', value: 'shipping',  count: orders.filter(o => o.status === 'shipping').length },
    { label: '완료',   value: 'delivered', count: orders.filter(o => o.status === 'delivered').length }
  ];

  const PAGE_SIZE = 10;

  /** 최초 로드 시 재고 목록 로딩 */
  useEffect(() => {
    const loadInventory = async () => {
      try {
        const list = await fetchStoreInventory();
        const mapped = list.map(mapStoreInventoryToInventoryItem);
        setInventory(mapped);
        setFilteredInventory(mapped);
        setHasInventory(mapped.length > 0);
      } catch (e) {
        console.error(e);
        toast.error('가맹점 재고 목록을 불러오지 못했습니다.');
      }
    };
    loadInventory();
  }, []);

  /** 검색어 변화 시 필터 적용 (inventory 변경 시도 재적용) */
  useEffect(() => {
    if (searchTerm) {
      setPage(1);
      const q = searchTerm.toLowerCase();
      const filtered = inventory.filter(item =>
        item.name.toLowerCase().includes(q) ||
        (item.category?.toLowerCase().includes(q) ?? false)
      );
      setFilteredInventory(filtered);
    } else {
      setFilteredInventory(inventory);
    }
  }, [searchTerm, inventory]);

  /** 페이징 계산 */
  const totalItems = filteredInventory.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE));
  const pagedInventory = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredInventory.slice(start, start + PAGE_SIZE);
  }, [filteredInventory, page]);

  /** 현재 페이지가 총 페이지 초과 시 보정 */
  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [totalPages, page]);

  /** 검색 입력 */
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
    setPage(1);
  };

  /* ------------------------- 선택/전체선택 ------------------------- */
  function handleItemSelect(itemId: number, checked: boolean) {
    setSelectedItems(prev => (checked ? [...prev, itemId] : prev.filter(id => id !== itemId)));
  }
  function handleSelectAll(checked: CheckedState) {
    const isChecked = checked === true;
    setSelectedItems(isChecked ? inventory.map(item => item.id) : []);
  }

  /* ------------------------- 테이블 컬럼 정의 ------------------------- */
  const inventoryColumns: Column<InventoryItem>[] = [
    {
      key: 'select',
      label: (
        <Checkbox
          checked={selectedItems.length === inventory.length && inventory.length > 0}
          onCheckedChange={handleSelectAll}
          className="border-gray-300"
        />
      ),
      render: (_, row) => (
        <Checkbox
          checked={selectedItems.includes(row.id)}
          onCheckedChange={(checked: any) => handleItemSelect(row.id, checked === true)}
          className="border-gray-300"
        />
      ),
      width: '60px'
    },
    {
      key: 'name',
      label: '품목정보',
      sortable: true,
      render: (value, row) => (
        <div>
          <div
            className="font-medium text-gray-900 cursor-pointer hover:text-kpi-red transition-colors"
            onClick={() => handleItemDetail(row)}
          >
            {value}
          </div>
          <div className="text-sm text-dark-gray">
            {row.category}
          </div>
        </div>
      )
    },
    {
      key: 'currentStock',
      label: '재고현황',
      sortable: true,
      render: (value, row) => {
        const percentage = row.optimalQuantity > 0 ? (value / row.optimalQuantity) * 100 : 0;
        const isLow = value <= row.optimalQuantity;
        return (
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className={`font-medium ${isLow ? 'text-kpi-red' : 'text-gray-900'}`}>
                {value} {row.unit}
              </span>
              {isLow && <AlertTriangle className="w-4 h-4 text-kpi-red" />}
            </div>
            <Progress
              value={percentage}
              className={`h-2 ${
                percentage <= 20 ? '[&>div]:bg-kpi-red' :
                percentage <= 50 ? '[&>div]:bg-kpi-orange' : '[&>div]:bg-kpi-green'
              }`}
            />
            <div className="text-xs text-dark-gray mt-1">
              적정: {row.optimalQuantity}
            </div>
          </div>
        );
      }
    },
    {
      key: 'weeklyUsage',
      label: '주간사용량',
      sortable: true,
      render: (value, row) => {
        // value는 소진 단위/주. 0이면 남은 일수 계산 불가 → '-'
        const daysLeft = value > 0 ? Math.floor(row.currentStock / (value / 7)) : 0;
        return (
          <div>
            <div className="font-medium text-gray-900">
              {value} {row.unit}
            </div>
            <div className={`text-xs ${daysLeft <= 2 && value > 0 ? 'text-kpi-red' : 'text-dark-gray'}`}>
              {value > 0 ? `약 ${daysLeft}일분` : '-'}
            </div>
          </div>
        );
      }
    },
    {
      key: 'expiryDate',
      label: '유통기한',
      sortable: true,
      render: (value) => {
        const expiryDate = new Date(value);
        const today = new Date();
        const daysUntilExpiry = Math.ceil((expiryDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
        return (
          <div>
            <div className="text-sm text-gray-900">{expiryDate.toLocaleDateString('ko-KR')}</div>
            <div
              className={`text-xs ${
                daysUntilExpiry <= 3 ? 'text-kpi-red' : daysUntilExpiry <= 7 ? 'text-kpi-orange' : 'text-dark-gray'
              }`}
            >
              {daysUntilExpiry <= 0 ? '기한만료' : `${daysUntilExpiry}일 남음`}
            </div>
          </div>
        );
      }
    },
    {
      key: 'supplier',
      label: '공급업체',
      render: (value, row) => (
        <div>
          <div className="text-sm text-gray-900">{row.hqMaterial ? '본사' : value}</div>
          <div className="text-xs text-dark-gray">₩{(row.unitPrice || 0).toLocaleString()}/{row.unit}</div>
        </div>
      )
    },
    {
      key: 'status',
      label: '상태',
      sortable: true,
      render: (value: StockStatus) => {
        const meta = getStockStatusDisplay(value);
        const badgeVariant =
          value === 'sufficient' ? 'active'
          : value === 'low'       ? 'warning'
          : 'closed';
        return <StatusBadge status={badgeVariant} text={meta.text} />;
      },
    }
  ];

  /* ------------------------- 상단 버튼/액션 ------------------------- */
  const handleRestock = (item: InventoryItem) => { setSelectedItem(item); setModalType('restock'); setIsModalOpen(true); };
  const handleAdjust = (item: InventoryItem)  => { setSelectedItem(item); setModalType('adjust'); setIsModalOpen(true); };
  const handleRegisterItem = () => { setSelectedItem(null); setModalType('register'); setIsModalOpen(true); };

  /** 집계 재고 초기화 (StoreMaterial 기준으로 없는 조합만 생성) */
  const handleInitInventory = async () => {
    try {
      setIsLoading(true);
      const created = await initStoreInventory();
      if (created > 0) toast.success(`초기 재고 ${created}개를 생성했습니다.`);
      else toast.info('이미 재고가 모두 생성되어 있습니다.');
      const list = await fetchStoreInventory();
      const mapped = list.map(mapStoreInventoryToInventoryItem);
      setInventory(mapped);
      setHasInventory(list.length > 0);
    } catch (e: any) {
      console.error(e);
      toast.error(e?.response?.data?.message || '초기 재고 세팅 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  /** 선택 항목을 장바구니로 투입 (적정-현재를 기본 발주수량으로) */
  const handleBulkOrder = () => {
    if (selectedItems.length === 0) {
      toast.error('발주할 품목을 선택해주세요.');
      return;
    }
    const selectedInventoryItems = inventory.filter(item => selectedItems.includes(item.id));
    const cartData: CartItem[] = selectedInventoryItems.map(item => {
      const qty = Math.max(item.optimalQuantity - item.currentStock, 0);
      return { ...item, orderQuantity: qty, totalPrice: qty * item.unitPrice };
    });
    setCartItems(cartData);
    setIsCartModalOpen(true);
  };

  /** 상세 보기 */
  const handleItemDetail = (item: InventoryItem) => { setSelectedItem(item); setIsDetailModalOpen(true); };

  /**
   * 상세 팝업: 적정재고(최소 재고) 수정
   * - 현재는 프런트 상태만 갱신. 실제 저장은 StoreMaterial API에 PATCH 필요.
   * - 백엔드 연결 시 updateStoreMaterialOptimalQuantity 호출로 대체.
   */
  const handleUpdateMinStock = async (newMinStock: number) => {
    if (!selectedItem) return;
    try {
      setInventory(prev =>
        prev.map(item =>
          item.id === selectedItem.id
            ? { ...item, optimalQuantity: newMinStock, status: calcStockStatus(item.currentStock, newMinStock) }
            : item,
        ),
      );
      setSelectedItem(prev =>
        prev ? { ...prev, optimalQuantity: newMinStock, status: calcStockStatus(prev.currentStock, newMinStock) } : prev,
      );
      toast.success(`${selectedItem.name}의 최소 재고량이 ${newMinStock}${selectedItem.unit}로 설정되었습니다.`);
    } catch {
      toast.error('오류가 발생했습니다.');
    }
  };

  /**
   * 모달 제출 (입고/조정/발주/재료등록)
   * - 각 케이스별로 백엔드 호출 및 화면 상태 보정
   */
  const handleSubmit = async (data: any) => {
    setIsLoading(true);
    try {
      // UX: 너무 즉시 닫히면 사용자 인지가 어려워 1초 정도 페이크 대기
      await new Promise(resolve => setTimeout(resolve, 1000));

      if (modalType === 'restock' && selectedItem) {
        // 입고 등록
        const parsedQty = Number(data.quantity);
        const parsedUnitPrice =
          data.unitPrice !== undefined && data.unitPrice !== ''
            ? Number(data.unitPrice)
            : undefined; // 미입력 시 undefined → 백엔드 정책(본사/HQ) 따라 보정

        const payload: StoreInventoryInWriteDTO = {
          storeInventoryId: selectedItem.storeInventoryId ?? selectedItem.id,
          storeMaterialId:  selectedItem.storeMaterialId,
          quantity: parsedQty,
          memo: data.memo ?? '',
          ...(parsedUnitPrice !== undefined ? { unitPrice: parsedUnitPrice } : {}),
        };

        const id = await inboundStoreInventory(payload);

        const list = await fetchStoreInventory();
        const mapped = list.map(mapStoreInventoryToInventoryItem);
        setInventory(mapped);

        toast.success(`${selectedItem.name} ${parsedQty}${selectedItem.unit} 입고 완료 (#${id})`);

      } else if (modalType === 'adjust' && selectedItem) {
        // 절대값 재고 조정
        const newQty = parseInt(data.newStock, 10);
        await adjustStoreInventory({
          storeInventoryId: selectedItem.storeInventoryId ?? selectedItem.id,
          storeMaterialId: selectedItem.storeMaterialId, // 백엔드가 요구하지 않으면 제거 가능
          newQuantity: newQty,
          reason: data.reason, // 'REAL_AUDIT' 등
        });
        setInventory(prev =>
          prev.map(item =>
            item.id === selectedItem.id
              ? { ...item, currentStock: newQty, status: calcStockStatus(newQty, item.optimalQuantity) }
              : item,
          ),
        );
        setSelectedItem(prev =>
          prev ? { ...prev, currentStock: newQty, status: calcStockStatus(newQty, prev.optimalQuantity) } : prev,
        );
        toast.success(`${selectedItem.name} 재고 조정 완료`);

      } else if (modalType === 'order') {
        // 발주 등록(현재는 로컬 상태 업데이트; 실제 API는 PurchaseOrder 모듈에서 처리)
        const newOrder: Order = {
          id: `PO-${String(Date.now()).slice(-6)}`,
          items: [{
            name: data.itemName,
            quantity: Number(data.quantity),
            unit: data.unit,
            unitPrice: Number(data.unitPrice)
          }],
          supplier: data.supplier,
          orderDate: new Date().toISOString().split('T')[0],
          expectedDate: data.expectedDate,
          status: 'pending',
          total: Number(data.quantity) * Number(data.unitPrice)
        };
        setOrders(prev => [newOrder, ...prev]);
        toast.success('발주 등록이 완료되었습니다.');

      } else if (modalType === 'register') {
        // 가맹점 재료 신규 등록
        const payload: StoreMaterialCreateRequest = {
          name: data.itemName,
          category: data.category ?? null,
          baseUnit: data.baseUnit,
          salesUnit: data.salesUnit,
          optimalQuantity: data.optimalQuantity ? Number(data.optimalQuantity) : null,
          purchasePrice: data.purchasePrice ? Number(data.purchasePrice) : null,
          supplier: data.supplier || null,
          temperature: (data.temperature || null) as MaterialTemperature | null,
        };
        const newId = await createStoreMaterial(payload);
        toast.success(`'${payload.name}' 재료가 등록되었습니다. (#${newId})`);
        // 필요 시 재조회하여 목록 갱신 가능
        // const list = await fetchStoreInventory(); setInventory(list.map(mapStoreInventoryToInventoryItem));
      }

      setIsModalOpen(false);
    } catch (e: any) {
      console.error(e);
      toast.error(
        e?.response?.data?.message ||
          (modalType === 'restock'
            ? '입고 처리 중 오류가 발생했습니다.'
            : '오류가 발생했습니다.'),
      );
    } finally {
      setIsLoading(false);
    }
  };

  /** 모달 폼 필드 스키마 */
  const getFormFields = () => {
    if (modalType === 'restock') {
      return [
        { name: 'quantity', label: `입고 수량 (${selectedItem?.unit})`, type: 'number' as const, required: true, placeholder: '입고할 수량을 입력하세요' },
        // HQ 재료면 unitPrice 숨김, 가맹점 재료면 동적 노출 고려 가능
        { name: 'memo', label: '메모', type: 'text' as const, required: false, placeholder: '입고 관련 메모 (선택)' }
      ];
    } else if (modalType === 'adjust') {
      return [
        { name: 'newStock', label: `새 재고 수량 (${selectedItem?.unit})`, type: 'number' as const, required: true, placeholder: '조정할 재고 수량을 입력하세요' },
        {
          name: 'reason', label: '조정 사유', type: 'select' as const, required: true,
          options: [
            { value: 'MANUAL', label: '수동 수정' },
            { value: 'DAMAGE', label: '파손' },
            { value: 'LOSS', label: '분실' },
            { value: 'ERROR', label: '데이터 오류 정정' },
            { value: 'REAL_AUDIT', label: '실사 조정' },
          ]
        }
      ];
    } else if (modalType === 'order') {
      // 현재는 데모 수준. 실제 발주 화면/모듈에선 별도 페이지를 권장.
      return [
        { name: 'itemName',   label: '품목명', type: 'text' as const, required: true },
        { name: 'quantity',   label: '수량',   type: 'number' as const, required: true },
        { name: 'unit',       label: '단위',   type: 'text' as const, required: true, placeholder: 'kg, 개, 통 등' },
        { name: 'unitPrice',  label: '단가',   type: 'number' as const, required: true },
        { name: 'supplier',   label: '공급업체', type: 'text' as const, required: true },
        { name: 'expectedDate', label: '희망 납기일', type: 'date' as const, required: true }
      ];
    }
    // register
    return [
      { name: 'itemName',  label: '품목명',     type: 'text' as const, required: true },
      {
        name: 'category',  label: '카테고리',  type: 'select' as const, required: true,
        options: [
          { value: 'BASE',     label: '주재료(BASE)' },
          { value: 'TOPPING',  label: '토핑/부재료(TOPPING)' },
          { value: 'SIDE',     label: '사이드(SIDE)' },
          { value: 'SAUCE',    label: '소스/조미료(SAUCE)' },
          { value: 'BEVERAGE', label: '음료(BEVERAGE)' },
          { value: 'PACKAGE',  label: '포장재(PACKAGE)' },
          { value: 'ETC',      label: '기타(ETC)' },
        ],
      },
      { name: 'baseUnit',  label: '소진 단위', type: 'text' as const, required: true, placeholder: '개, g, 샷 등' },
      { name: 'salesUnit', label: '입고 단위', type: 'text' as const, required: true, placeholder: '박스, 봉, kg 등' },
      { name: 'optimalQuantity', label: '적정 재고 (소진 단위)', type: 'number' as const, required: false, placeholder: '예: 30' },
      { name: 'purchasePrice',   label: '매입 단가 (입고 단위)', type: 'number' as const, required: false, placeholder: '예: 15000' },
      {
        name: 'supplier', label: '공급업체', type: 'text' as const, required: false, placeholder: '대표 공급업체명',
      },
      {
        name: 'temperature', label: '보관 온도', type: 'select' as const, required: false,
        options: [
          { value: 'TEMPERATURE', label: '상온' },
          { value: 'REFRIGERATE', label: '냉장' },
          { value: 'FREEZE',      label: '냉동' },
        ],
      },
    ];
  };

  /** 모달 제목 */
  const getModalTitle = () => {
    if (modalType === 'restock') return `${selectedItem?.name} 입고`;
    if (modalType === 'adjust')  return `${selectedItem?.name} 재고 조정`;
    if (modalType === 'order')   return '새 발주 등록';
    return '새 자재 등록';
  };

  // 카드 지표
  const lowStockItems = inventory.filter(i => i.status === 'low' || i.status === 'shortage').length;
  const pendingOrders = orders.filter(o => o.status === 'pending' || o.status === 'approved').length;
  const expiringItems = inventory.filter((item) => {
    const expiryDate = new Date(item.expiryDate);
    const today = new Date();
    const daysUntilExpiry = Math.ceil((expiryDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    return daysUntilExpiry <= 7 && daysUntilExpiry > 0;
  }).length;

  return (
    <div className="space-y-6">
      {/* ======================= 요약 카드 ======================= */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <Card className="p-6 bg-white rounded-xl shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-dark-gray mb-1">총 품목</p>
              <p className="text-2xl font-bold text-gray-900">{totalItems}</p>
              <p className="text-xs text-dark-gray">재고 관리 품목</p>
            </div>
            <Package className="w-8 h-8 text-kpi-green" />
          </div>
        </Card>

        <Card className="p-6 bg-white rounded-xl shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-dark-gray mb-1">재고 부족</p>
              <p className="text-2xl font-bold text-kpi-red">{lowStockItems}</p>
              <p className="text-xs text-dark-gray">조치 필요</p>
            </div>
            <AlertTriangle className="w-8 h-8 text-kpi-red" />
          </div>
        </Card>

        <Card className="p-6 bg-white rounded-xl shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-dark-gray mb-1">유통기한 임박</p>
              <p className="text-2xl font-bold text-kpi-orange">{expiringItems}</p>
              <p className="text-xs text-dark-gray">7일 이내 유통기한</p>
            </div>
            <Calendar className="w-8 h-8 text-kpi-orange" />
          </div>
        </Card>

        <Card className="p-6 bg-white rounded-xl shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-dark-gray mb-1">처리 중 발주</p>
              <p className="text-2xl font-bold text-kpi-purple">{pendingOrders}</p>
              <p className="text-xs text-dark-gray">승인 대기/배송중</p>
            </div>
            <Truck className="w-8 h-8 text-kpi-purple" />
          </div>
        </Card>
      </div>

      {/* ======================= 부족 알림 ======================= */}
      {lowStockItems > 0 && (
        <Card className="p-6 bg-red-50 border border-red-200 rounded-xl">
          <div className="flex items-center gap-3 mb-4">
            <AlertTriangle className="w-6 h-6 text-kpi-red" />
            <div>
              <h3 className="font-semibold text-gray-900">재고 부족 알림</h3>
              <p className="text-sm text-dark-gray">{lowStockItems}개 품목의 재고가 부족합니다.</p>
            </div>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {inventory
              .filter(i => i.status === 'low' || i.status === 'shortage')
              .slice(0, 4)
              .map(item => (
                <Button
                  key={item.id}
                  onClick={() => handleRestock(item)}
                  variant="outline"
                  className="flex-col h-auto py-3 border-kpi-red text-kpi-red hover:bg-red-50"
                >
                  <span className="font-medium">{item.name}</span>
                  <span className="text-xs">
                    {item.currentStock}/{item.optimalQuantity} {item.unit}
                  </span>
                </Button>
              ))}
          </div>
        </Card>
      )}

      {/* ======================= 페이징 전체 검색 기능 보완 ======================= */}
      <div className="flex justify-between items-center hidden">
        <div className="flex gap-3">
          <Input
            value={searchTerm}
            onChange={handleSearchChange}
            placeholder="품목명, 카테고리로 검색"
          />
        </div>
      </div>

      {/* ======================= 재고 헤더(커스텀) ======================= */}


      {/* ======================= 헤더/액션 ======================= */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">재고 관리</h2>
          <p className="text-sm text-dark-gray">총 {filteredInventory.length}개 항목</p>
        </div>
        <div className="flex gap-3">
          {!hasInventory && (
            <Button
              variant="outline"
              onClick={handleInitInventory}
              className="border-kpi-green text-kpi-green hover:bg-green-50"
              disabled={isLoading}
            >
              초기 재고 세팅
            </Button>
          )}

          <Button onClick={handleRegisterItem} variant="outline" className="border-kpi-green text-kpi-green hover:bg-green-50">
            <Plus className="w-4 h-4 mr-2" />재료등록
          </Button>

          <Button onClick={handleBulkOrder} className="bg-kpi-red hover:bg-red-600 text-white relative">
            <ShoppingCart className="w-4 h-4 mr-2" />
            발주 등록 {selectedItems.length > 0 && `(${selectedItems.length})`}
          </Button>
        </div>
      </div>
      
      {/* ======================= 테이블 ======================= */}
      <DataTable
        data={pagedInventory}              // 필터 + 페이징 결과만 랜더링
        columns={inventoryColumns}
        title="재고 현황"
        searchPlaceholder="품목명, 카테고리로 검색"
        filters={inventoryFilters}
        showActions={false}
        hideHeaderSummary={true}  // ← 공통 테이블 헤더(제목/총개수) 숨김
      />

      {/* ======================= 페이징 ======================= */}
      <Pagination className="mt-4">
        <PaginationContent>
          <PaginationItem>
            <PaginationPrevious
              href="#"
              onClick={(e) => { e.preventDefault(); if (page > 1) setPage(page - 1); }}
              className={page === 1 ? 'pointer-events-none opacity-50' : ''}
            />
          </PaginationItem>

          {Array.from({ length: totalPages }).map((_, idx) => {
            const p = idx + 1;
            return (
              <PaginationItem key={p}>
                <PaginationLink
                  href="#"
                  isActive={p === page}
                  onClick={(e) => { e.preventDefault(); setPage(p); }}
                >
                  {p}
                </PaginationLink>
              </PaginationItem>
            );
          })}

          <PaginationItem>
            <PaginationNext
              href="#"
              onClick={(e) => { e.preventDefault(); if (page < totalPages) setPage(page + 1); }}
              className={page === totalPages ? 'pointer-events-none opacity-50' : ''}
            />
          </PaginationItem>
        </PaginationContent>
      </Pagination>

      {/* ======================= 폼 모달 ======================= */}
      <FormModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={getModalTitle()}
        fields={getFormFields()}
        onSubmit={handleSubmit}
        initialData={modalType === 'adjust' ? { newStock: selectedItem?.currentStock } : {}}
        isLoading={isLoading}
        maxWidth="md"
      />

      {/* ======================= 상세 모달 ======================= */}
      <Dialog open={isDetailModalOpen} onOpenChange={setIsDetailModalOpen}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Package className="w-5 h-5" />
              재고 상세 정보
            </DialogTitle>
            <DialogDescription>선택한 재고 품목의 상세 정보를 확인하고 관리할 수 있습니다.</DialogDescription>
          </DialogHeader>

          {selectedItem && (
            <ItemDetailContent
              item={selectedItem}
              onUpdateMinStock={handleUpdateMinStock}
              onRestock={() => { setIsDetailModalOpen(false); handleRestock(selectedItem); }}
              onAdjust={() => { setIsDetailModalOpen(false); handleAdjust(selectedItem); }}
            />
          )}
        </DialogContent>
      </Dialog>

      {/* ======================= 장바구니 모달 ======================= */}
      <Dialog open={isCartModalOpen} onOpenChange={setIsCartModalOpen}>
        <DialogContent className="max-w-6xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <ShoppingCart className="w-5 h-5" />
              발주 장바구니 ({cartItems.length}개 품목)
            </DialogTitle>
            <DialogDescription>선택한 품목들의 발주 정보를 확인하고 수정할 수 있습니다.</DialogDescription>
          </DialogHeader>

          {cartItems.length > 0 && (
            <OrderCartContent
              items={cartItems}
              onUpdateQuantity={(itemId, quantity) => {
                setCartItems(prev =>
                  prev.map(item => (item.id === itemId ? { ...item, orderQuantity: quantity, totalPrice: quantity * item.unitPrice } : item))
                );
              }}
              onRemoveItem={itemId => {
                setCartItems(prev => prev.filter(item => item.id !== itemId));
                setSelectedItems(prev => prev.filter(id => id !== itemId));
              }}
              onSubmitOrder={async ({ priority, notes }) => {
                // 실제 발주 생성 API 통합부
                if (submitOrderRef.current) return;
                submitOrderRef.current = true;
                try {
                  if (cartItems.length === 0) { toast.error('발주 품목을 추가해주세요.'); return; }
                  if (cartItems.some(ci => ci.orderQuantity <= 0)) { toast.error('발주 수량을 확인해주세요.'); return; }

                  const dto: PurchaseOrderRequestsDTO = {
                    priority: (priority as 'NORMAL' | 'URGENT') ?? 'NORMAL',
                    notes: (notes ?? '').trim(),
                    items: cartItems
                      .map(ci => ({
                        storeMaterialId: Number(ci.storeMaterialId ?? ci.id),
                        count: Number(ci.orderQuantity),
                      }))
                      .filter(it =>
                        Number.isFinite(it.storeMaterialId) && it.storeMaterialId > 0 &&
                        Number.isFinite(it.count) && it.count > 0
                      ),
                  };
                  if (!dto.items.length) { toast.error('유효한 발주 품목이 없습니다.'); return; }

                  const res = await api.post<number>('/api/purchase/create', dto);

                  toast.success(`발주 등록 완료 #${res.data}`);
                  setIsCartModalOpen(false);
                  setSelectedItems([]);
                  setCartItems([]);

                  // 로컬 목록에 반영(간단 요약만)
                  setOrders(prev => [
                    {
                      id: String(res.data),
                      items: cartItems.map(ci => ({ name: ci.name, quantity: ci.orderQuantity, unit: ci.unit, unitPrice: ci.unitPrice })),
                      supplier: (cartItems[0] as any)?.supplier ?? '미지정',
                      orderDate: new Date().toISOString().split('T')[0],
                      expectedDate: '',
                      status: 'pending',
                      total: cartItems.reduce((s, ci) => s + ci.totalPrice, 0),
                    },
                    ...prev,
                  ]);
                } catch (e: any) {
                  console.error(e);
                  toast.error(e?.response?.data?.message || '발주 등록 중 오류가 발생했습니다.');
                } finally {
                  submitOrderRef.current = false;
                }
              }}
            />
          )}
        </DialogContent>
      </Dialog>

      {dialog}
    </div>
  );
}

/* =========================================================================
   하위 컴포넌트: 상세 / 장바구니
   ========================================================================= */

/**
 * 상세 패널
 * - 적정재고 inline 편집
 * - 입고/조정 트리거 버튼 제공
 */
function ItemDetailContent({
  item,
  onUpdateMinStock,
  onRestock,
  onAdjust
}: {
  item: InventoryItem;
  onUpdateMinStock: (minStock: number) => void;
  onRestock: () => void;
  onAdjust: () => void;
}) {
  const [editingMinStock, setEditingMinStock] = useState(false);
  const [minStockValue, setMinStockValue] = useState(item.optimalQuantity.toString());

  const handleSaveMinStock = () => {
    const newMinStock = parseInt(minStockValue, 10);
    if (isNaN(newMinStock) || newMinStock < 0) {
      toast.error('올바른 수량을 입력해주세요.');
      return;
    }
    onUpdateMinStock(newMinStock);
    setEditingMinStock(false);
  };

  const handleCancelEdit = () => {
    setMinStockValue(item.optimalQuantity.toString());
    setEditingMinStock(false);
  };

  const statusMeta = getStockStatusDisplay(item.status);

  const expiryDate = new Date(item.expiryDate);
  const today = new Date();
  const daysUntilExpiry = Math.ceil((expiryDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  const daysLeft = item.weeklyUsage > 0 ? Math.floor(item.currentStock / (item.weeklyUsage / 7)) : 0;

  return (
    <div className="space-y-6">
      {/* 기본 정보 / 재고 현황 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card className="p-4">
          <h3 className="font-semibold text-gray-900 mb-4">기본 정보</h3>
          <div className="space-y-3">
            <div className="flex justify-between"><span className="text-gray-600">품목명</span><span className="font-medium">{item.name}</span></div>
            <div className="flex justify-between"><span className="text-gray-600">카테고리</span><span>{item.category}</span></div>
            <div className="flex justify-between"><span className="text-gray-600">공급업체</span><span>{item.supplier}</span></div>
            <div className="flex justify-between"><span className="text-gray-600">단가</span><span>₩{(item.unitPrice || 0).toLocaleString()}/{item.unit}</span></div>
          </div>
        </Card>

        <Card className="p-4">
          <h3 className="font-semibold text-gray-900 mb-4">재고 현황</h3>
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-gray-600">현재 재고</span>
              <span className={`font-semibold ${statusMeta.textColor}`}>
                {item.currentStock} {item.unit}
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-600">재고 상태</span>
              <Badge className={statusMeta.badgeClass}>{statusMeta.text}</Badge>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-600">적정 재고</span>
              <div className="flex items-center gap-2">
                {editingMinStock ? (
                  <div className="flex items-center gap-2">
                    <Input value={minStockValue} onChange={(e) => setMinStockValue(e.target.value)} className="w-20 h-8 text-sm" type="number" min="0" />
                    <span className="text-sm text-gray-500">{item.unit}</span>
                    <Button size="sm" onClick={handleSaveMinStock} className="h-8 px-2">저장</Button>
                    <Button size="sm" variant="outline" onClick={handleCancelEdit} className="h-8 px-2">취소</Button>
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <span>{item.optimalQuantity} {item.unit}</span>
                    <Button size="sm" variant="ghost" onClick={() => setEditingMinStock(true)} className="h-8 w-8 p-0">
                      <Settings className="w-4 h-4" />
                    </Button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* 사용/유통기한 패널 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card className="p-4">
          <h3 className="font-semibold text-gray-900 mb-4">사용량 정보</h3>
          <div className="space-y-3">
            <div className="flex justify-between"><span className="text-gray-600">주간 사용량</span><span>{item.weeklyUsage} {item.unit}</span></div>
            <div className="flex justify-between"><span className="text-gray-600">일평균 사용량</span><span>{(item.weeklyUsage / 7).toFixed(1)} {item.unit}</span></div>
            <div className="flex justify-between"><span className="text-gray-600">예상 소진일</span><span className={daysLeft <= 3 && item.weeklyUsage > 0 ? 'text-red-600 font-medium' : ''}>{item.weeklyUsage > 0 ? `약 ${daysLeft}일 후` : '-'}</span></div>
            <div className="flex justify-between"><span className="text-gray-600">마지막 입고</span><span>{item.lastRestocked ? new Date(item.lastRestocked).toLocaleDateString('ko-KR') : '-'}</span></div>
          </div>
        </Card>

        <Card className="p-4">
          <h3 className="font-semibold text-gray-900 mb-4">유통기한</h3>
          <div className="space-y-3">
            <div className="flex justify-between"><span className="text-gray-600">유통기한</span><span>{expiryDate.toLocaleDateString('ko-KR')}</span></div>
            <div className="flex justify-between">
              <span className="text-gray-600">남은 기간</span>
              <span className={daysUntilExpiry <= 3 ? 'text-red-600 font-medium' : daysUntilExpiry <= 7 ? 'text-orange-600 font-medium' : ''}>
                {daysUntilExpiry <= 0 ? '기한만료' : `${daysUntilExpiry}일 남음`}
              </span>
            </div>
            <div className="flex justify-between"><span className="text-gray-600">재고 가치</span><span>₩{((item.currentStock || 0) * (item.unitPrice || 0)).toLocaleString()}</span></div>
          </div>
        </Card>
      </div>

      {/* 재고 레벨 게이지 */}
      <Card className="p-4">
        <h3 className="font-semibold text-gray-900 mb-4">재고 레벨</h3>
        <div className="space-y-3">
          <div className="relative">
            <Progress value={item.optimalQuantity > 0 ? (item.currentStock / item.optimalQuantity) * 100 : 0} className="h-6" />
            <div className="absolute inset-0 flex items-center justify-center">
              <span className="text-sm font-medium text-gray-700">
                {item.currentStock} / {item.optimalQuantity} {item.unit}
              </span>
            </div>
          </div>
          <div className="flex justify-between text-sm text-gray-600">
            <span>최소: {item.optimalQuantity}{item.unit}</span>
            <span>현재: {item.currentStock}{item.unit}</span>
            <span>최대: {item.optimalQuantity > 0 ? item.optimalQuantity * 2 : 0}{item.unit}</span>
          </div>
        </div>
      </Card>

      <div className="flex gap-3 pt-4 border-t">
        <Button onClick={onRestock} className="bg-kpi-green hover:bg-green-600 text-white">
          <Plus className="w-4 h-4 mr-2" />입고
        </Button>
        <Button onClick={onAdjust} variant="outline" className="border-kpi-orange text-kpi-orange hover:bg-orange-50">
          <Settings className="w-4 h-4 mr-2" />재고 조정
        </Button>
        {(item.status === 'low' || item.status === 'shortage') && (
          <Button variant="outline" className="border-kpi-red text-kpi-red hover:bg-red-50">
            <ShoppingCart className="w-4 h-4 mr-2" />발주 등록
          </Button>
        )}
      </div>
    </div>
  );
}

/**
 * 발주 장바구니
 * - 현재 화면 내에서 간단 처리. 실제 운영에선 별도 발주 페이지 추천.
 */
function OrderCartContent({
  items,
  onUpdateQuantity,
  onRemoveItem,
  onSubmitOrder
}: {
  items: CartItem[];
  onUpdateQuantity: (itemId: number, quantity: number) => void;
  onRemoveItem: (itemId: number) => void;
  onSubmitOrder: (orderData: {
    supplier: string;
    expectedDate: string;
    priority: string;
    notes: string;
  }) => void;
}) {
  const [submitting, setSubmitting] = useState(false);
  const [orderForm, setOrderForm] = useState<{ priority: string; notes: string }>({
    priority: 'NORMAL',
    notes: '',
  });

  const totalAmount = items.reduce((sum, item) => sum + item.totalPrice, 0);
  const totalItems = items.reduce((sum, item) => sum + item.orderQuantity, 0);

  const handleQuantityChange = (itemId: number, newQuantity: string) => {
    const quantity = parseInt(newQuantity, 10) || 0;
    if (quantity >= 0) onUpdateQuantity(itemId, quantity);
  };

  /** 품목에서 공급업체 추론 (단순 로직) */
  const inferSupplier = () => {
    if (items.length === 0) return '미지정';
    const first = (items[0] as any).supplier as string | undefined;
    const allSame = !!first && items.every((it) => (it as any).supplier === first);
    if (allSame && first) return first;
    const hasAny = items.some((it) => !!(it as any).supplier);
    return hasAny ? '여러 공급처' : '미지정';
  };

  const handleSubmit = async () => {
    if (submitting) return;          // 재클릭 차단
    setSubmitting(true);
    try {
      if (items.length === 0) { toast.error('발주 품목을 추가해주세요.'); return; }
      if (items.some((item) => item.orderQuantity <= 0)) { toast.error('발주 수량을 확인해주세요.'); return; }

      await onSubmitOrder({
        supplier: inferSupplier(),
        expectedDate: '',
        priority: orderForm.priority,
        notes: orderForm.notes,
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* 발주 품목 목록 */}
      <Card className="p-4">
        <div className="flex justify-between items-center mb-4">
          <h3 className="font-semibold text-gray-900">발주 품목 ({items.length}개)</h3>
        </div>

        <div className="space-y-3">
          {items.map((item) => (
            <div key={item.id} className="flex items-center gap-4 p-3 bg-gray-50 rounded-lg">
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <h4 className="font-medium text-gray-900">{item.name}</h4>
                    <div className="mt-1 text-sm text-gray-500 space-y-1">
                      <div className="flex items-center gap-2">
                        <span>현재 재고:</span>
                        <span>{item.currentStock}{item.unit}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span>적정 재고:</span>
                        <span>{item.optimalQuantity}{item.unit}</span>
                      </div>
                    </div>
                  </div>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => onRemoveItem(item.id)}
                    className="text-red-500 hover:text-red-700"
                  >
                    ✕
                  </Button>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2">
                  <Label className="text-sm text-gray-600">수량:</Label>
                  <Input
                    type="number"
                    value={item.orderQuantity}
                    onChange={(e) => handleQuantityChange(item.id, e.target.value)}
                    className="w-20 h-8 text-center"
                    min="0"
                  />
                  <span className="text-sm text-gray-600">{item.unit}</span>
                </div>
                <div className="text-right">
                  <div className="text-sm text-gray-600">
                    단가: ₩{(item.unitPrice || 0).toLocaleString()}
                  </div>
                  <div className="font-medium text-gray-900">
                    ₩{(item.totalPrice || 0).toLocaleString()}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* 발주 메타(간략 버전) */}
      <Card className="p-4">
        <h3 className="font-semibold text-gray-900 mb-4">발주 정보</h3>
        <div className="grid grid-cols-1 gap-4">
          <div>
            <Label htmlFor="priority">우선순위</Label>
            <select
              id="priority"
              value={orderForm.priority}
              onChange={(e) => setOrderForm((prev) => ({ ...prev, priority: e.target.value }))}
              className="mt-1 w-40 sm:w-48 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-kpi-red"
            >
              <option value="NORMAL">일반</option>
              <option value="URGENT">우선</option>
            </select>
          </div>

          <div>
            <Label htmlFor="notes">특이사항</Label>
            <Input
              id="notes"
              value={orderForm.notes}
              onChange={(e) => setOrderForm((prev) => ({ ...prev, notes: e.target.value }))}
              placeholder="특별 요청사항이 있다면 입력하세요"
              className="mt-1"
            />
          </div>
        </div>
      </Card>

      {/* 발주 요약/등록 */}
      <Card className="p-4 bg-kpi-red/5 border-kpi-red/20">
        <div className="flex justify-between items-center mb-4">
          <div>
            <h3 className="font-semibold text-gray-900">발주 요약</h3>
            <p className="text-sm text-gray-600">총 {items.length}개 품목, {(totalItems || 0).toLocaleString()}개</p>
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-gray-900">₩{(totalAmount || 0).toLocaleString()}</div>
            <div className="text-sm text-gray-600">총 발주 금액</div>
          </div>
        </div>

        <div className="flex gap-3 pt-4 border-t border-kpi-red/20">
          <Button
            variant="outline"
            onClick={() => setOrderForm({ priority: 'NORMAL', notes: '' })}
            className="flex-1"
          >
            초기화
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={submitting}
            className="flex-1 bg-kpi-red hover:bg-red-600 text-white"
          >
            <ShoppingCart className="w-4 h-4 mr-2" />
            {submitting ? '등록 중…' : '발주 등록'}
          </Button>
        </div>
      </Card>
    </div>
  );
}
