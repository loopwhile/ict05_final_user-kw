// src/pages/StoreMenuManagement.tsx
import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { toast } from 'sonner';
import { Package } from 'lucide-react';

import { Card } from '../ui/card';
import { Button } from '../ui/button';
import { Switch } from '../ui/switch';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../ui/dialog';
import { ScrollArea } from '../ui/scroll-area';
import { useConfirmDialog } from '../Common/ConfirmDialog';

/* ======================
   공통 axios 인스턴스 (JWT 자동 첨부)
====================== */

const api = axios.create({
  baseURL: import.meta.env.VITE_BACKEND_API_BASE_URL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');

  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

/* ======================
   타입 정의
====================== */

type SoldOutStatus = 'ON_SALE' | 'SOLD_OUT';
type MenuShow = 'SHOW' | 'HIDE';

export type StoreMenu = {
  menuId: number;
  menuName: string;
  menuNameEnglish: string;
  menuCategoryId: number;
  menuCategoryName: string;
  menuPrice: number;
  menuKcal: number;
  menuInformation: string;
  menuCode: string;
  soldOutStatus: SoldOutStatus; // 화면에서 쓰는 판매/품절 상태
  menuShow: MenuShow;
};

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // 현재 페이지(0-based)
  size: number;
};

/* ======================
   헬퍼 함수
====================== */

const getCategoryEmoji = (categoryName: string): string => {
  if (categoryName.includes('세트')) return '🍔';
  if (categoryName.includes('토스트')) return '🍞';
  if (categoryName.includes('사이드')) return '🍟';
  if (categoryName.includes('음료')) return '🥤';
  return '🍽️';
};

/**
 * 카테고리 버튼 정의
 * value:
 *  - all          : 전체
 *  - set/toast/side/drink : 카테고리 이름 필터
 *  - available    : 판매중(ON_SALE)
 *  - soldout      : 품절(SOLD_OUT)
 *
 * 🔹 categoryName 은 백엔드 MenuSearchDTO.categoryName 으로 그대로 전달됨
 */
const CATEGORY_FILTERS = [
  { label: '전체', value: 'all' as const, categoryName: undefined },
  { label: '세트', value: 'set' as const, categoryName: '세트' },
  { label: '토스트', value: 'toast' as const, categoryName: '토스트' },
  { label: '사이드', value: 'side' as const, categoryName: '사이드' },
  { label: '음료', value: 'drink' as const, categoryName: '음료' },
  { label: '판매중', value: 'available' as const, categoryName: undefined },
  { label: '품절', value: 'soldout' as const, categoryName: undefined },
];

type CategoryValue =
  | 'all'
  | 'set'
  | 'toast'
  | 'side'
  | 'drink'
  | 'available'
  | 'soldout';

/* ======================
   컴포넌트
====================== */

export const StoreMenuManagement: React.FC = () => {
  const [menus, setMenus] = useState<StoreMenu[]>([]);
  const [loading, setLoading] = useState(true);

  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedMenu, setSelectedMenu] = useState<StoreMenu | null>(null);

  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<CategoryValue>('all');

  const { dialog, confirm } = useConfirmDialog();

  // 서버 페이징 상태
  const [page, setPage] = useState(0); // 0-based
  const pageSize = 10;

  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  /* ======================
     메뉴 목록 조회 (백엔드 페이징/검색/필터 사용)
  ====================== */

  const fetchMenus = async () => {
    setLoading(true);
    try {
      const params: any = {
        page,
        size: pageSize,
      };

      // 검색어 → MenuSearchDTO.s / type=name
      if (searchTerm.trim() !== '') {
        params.s = searchTerm.trim();
        params.type = 'name';
      }

      // 카테고리/판매상태 필터 → MenuSearchDTO.categoryName / storeMenuSoldout
      const cat = CATEGORY_FILTERS.find((c) => c.value === selectedCategory);

      if (cat?.categoryName) {
        params.categoryName = cat.categoryName;
      }

      if (selectedCategory === 'available') {
        params.storeMenuSoldout = 'ON_SALE';
      } else if (selectedCategory === 'soldout') {
        params.storeMenuSoldout = 'SOLD_OUT';
      }

      const res = await api.get<PageResponse<any>>('/API/menu/list', {
        params,
      });

      const raw = res.data;

      const normalized: StoreMenu[] = (raw.content ?? []).map((m: any) => ({
        menuId: m.menuId,
        menuName: m.menuName,
        menuNameEnglish: m.menuNameEnglish,
        menuCategoryId: m.menuCategoryId,
        menuCategoryName: m.menuCategoryName,
        menuPrice: m.menuPrice,
        menuKcal: m.menuKcal,
        menuInformation: m.menuInformation,
        menuCode: m.menuCode,
        menuShow: m.menuShow as MenuShow,
        soldOutStatus: (m.storeMenuSoldout ?? 'ON_SALE') as SoldOutStatus,
      }));

      setMenus(normalized);
      setTotalElements(raw.totalElements);
      setTotalPages(raw.totalPages || 1);
      setPage(raw.number); // 서버 기준으로 동기화
    } catch (err) {
      console.error(err);
      toast.error('메뉴 목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 초기 로딩 + page/search/category 변경 시 재조회
  useEffect(() => {
    fetchMenus();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, searchTerm, selectedCategory]);

  // 검색어나 카테고리가 바뀌면 0페이지부터
  useEffect(() => {
    setPage(0);
  }, [searchTerm, selectedCategory]);

  /* ======================
     서버에 품절 상태 업데이트 (로그인한 가맹점 기준)
  ====================== */

  const updateSoldOutOnServer = async (
    menuId: number,
    status: SoldOutStatus,
  ) => {
    await api.patch(`/API/menu/${menuId}/sold-out`, {
      storeMenuSoldout: status,
    });
  };

  // 판매 상태 토글 (Switch)
  const handleToggleStatus = async (menuId: number, isOnSale: boolean) => {
    const newStatus: SoldOutStatus = isOnSale ? 'ON_SALE' : 'SOLD_OUT';

    try {
      await updateSoldOutOnServer(menuId, newStatus);

      setMenus((prev) =>
        prev.map((menu) =>
          menu.menuId === menuId
            ? { ...menu, soldOutStatus: newStatus }
            : menu,
        ),
      );

      const target = menus.find((m) => m.menuId === menuId);
      if (target) {
        toast.success(
          `${target.menuName}을(를) ${
            newStatus === 'ON_SALE'
              ? '판매중으로 변경했습니다.'
              : '품절 처리했습니다.'
          }`,
        );
      }
    } catch (e) {
      console.error(e);
      toast.error('판매 상태 변경에 실패했습니다.');
    }
  };

  const handleSoldOut = (menu: StoreMenu) => {
    confirm({
      title: '품절 처리',
      description: `${menu.menuName}을(를) 품절 처리하시겠습니까?`,
      type: 'warning',
      confirmText: '품절 처리',
      onConfirm: async () => {
        try {
          await updateSoldOutOnServer(menu.menuId, 'SOLD_OUT');

          setMenus((prev) =>
            prev.map((m) =>
              m.menuId === menu.menuId
                ? { ...m, soldOutStatus: 'SOLD_OUT' }
                : m,
            ),
          );
          toast.success(`${menu.menuName}을(를) 품절 처리했습니다.`);
        } catch (e) {
          console.error(e);
          toast.error('품절 처리에 실패했습니다.');
        }
      },
    });
  };

  const handleRestock = async (menu: StoreMenu) => {
    try {
      await updateSoldOutOnServer(menu.menuId, 'ON_SALE');

      setMenus((prev) =>
        prev.map((m) =>
          m.menuId === menu.menuId ? { ...m, soldOutStatus: 'ON_SALE' } : m,
        ),
      );
      toast.success(`${menu.menuName} 재입고 완료되었습니다.`);
    } catch (e) {
      console.error(e);
      toast.error('재입고 처리에 실패했습니다.');
    }
  };

  // 상세 모달
  const handleMenuDetail = async (menu: StoreMenu) => {
    try {
      const res = await api.get<any>(`/API/menu/${menu.menuId}`);

      const detail: StoreMenu = {
        ...menu,
        ...res.data,
      };

      setSelectedMenu(detail);
      setIsDetailModalOpen(true);
    } catch (err) {
      console.error(err);
      toast.error('메뉴 상세 정보를 불러오지 못했습니다.');
    }
  };

  /* ======================
     JSX 렌더링
  ====================== */

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1>메뉴 관리</h1>
          <p className="text-dark-gray">
            {loading ? '불러오는 중...' : `총 ${totalElements}개 항목`}
          </p>
        </div>
      </div>

      {/* Search & Filters */}
      <Card className="p-4">
        <div className="flex flex-col gap-4">
          <div className="relative max-w-md">
            <Package className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
            <input
              type="text"
              placeholder="메뉴명, 키워드로 검색"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-kpi-red focus:border-transparent"
            />
          </div>

          <div className="flex flex-wrap gap-2">
            {CATEGORY_FILTERS.map((category) => (
              <button
                key={category.value}
                onClick={() => setSelectedCategory(category.value)}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${
                  selectedCategory === category.value
                    ? 'bg-kpi-red text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                {category.label}
              </button>
            ))}
          </div>
        </div>
      </Card>

      {/* Menu Table */}
      <Card>
        <div className="overflow-x-auto">
          {loading ? (
            <div className="text-center py-10 text-gray-500">불러오는 중...</div>
          ) : menus.length === 0 ? (
            <div className="text-center py-16">
              <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500">조건에 맞는 메뉴가 없습니다.</p>
            </div>
          ) : (
            <>
              <table className="w-full">
                <thead className="bg-gray-50 border-b">
                  <tr>
                    <th className="px-6 py-4 text-left text-sm font-medium text-gray-600">
                      메뉴정보
                    </th>
                    <th className="px-6 py-4 text-left text-sm font-medium text-gray-600">
                      가격
                    </th>
                    <th className="px-6 py-4 text-left text-sm font-medium text-gray-600">
                      판매상태
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {menus.map((menu) => (
                    <tr key={menu.menuId} className="hover:bg-gray-50">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-4">
                          <div className="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center text-lg">
                            {getCategoryEmoji(menu.menuCategoryName)}
                          </div>
                          <div>
                            <div
                              className="font-medium text-gray-900 cursor-pointer hover:text-kpi-orange"
                              onClick={() => handleMenuDetail(menu)}
                            >
                              {menu.menuName}
                            </div>
                            <div className="text-xs text-gray-500">
                              {menu.menuCategoryName}
                            </div>
                          </div>
                        </div>
                      </td>

                      <td className="px-6 py-4">
                        ₩{menu.menuPrice.toLocaleString()}
                      </td>

                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <Switch
                            checked={menu.soldOutStatus === 'ON_SALE'}
                            onCheckedChange={(checked) =>
                              handleToggleStatus(menu.menuId, checked)
                            }
                          />
                          <span
                            className={`text-sm ${
                              menu.soldOutStatus === 'SOLD_OUT'
                                ? 'text-gray-400'
                                : 'text-green-600'
                            }`}
                          >
                            {menu.soldOutStatus === 'SOLD_OUT'
                              ? '품절'
                              : '판매중'}
                          </span>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* 서버 페이징 바 */}
              <div className="flex items-center justify-between px-6 py-4 border-t">
                <span className="text-sm text-gray-500">
                  {`${page + 1} / ${totalPages} 페이지`}
                </span>
                <div className="flex items-center gap-2">
                  {/* 이전 버튼 */}
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0}
                    onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                  >
                    이전
                  </Button>

                  {/* 숫자 페이지 버튼들 */}
                  {Array.from({ length: totalPages }, (_, idx) => idx).map(
                    (idx) => (
                      <Button
                        key={idx}
                        size="sm"
                        variant={idx === page ? 'default' : 'outline'}
                        onClick={() => setPage(idx)}
                      >
                        {idx + 1}
                      </Button>
                    ),
                  )}

                  {/* 다음 버튼 */}
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages - 1}
                    onClick={() =>
                      setPage((prev) => Math.min(prev + 1, totalPages - 1))
                    }
                  >
                    다음
                  </Button>
                </div>
              </div>
            </>
          )}
        </div>
      </Card>

      {/* Menu Detail Modal */}
      <Dialog open={isDetailModalOpen} onOpenChange={setIsDetailModalOpen}>
        <DialogContent className="max-w-2xl">
          {selectedMenu && (
            <>
              <DialogHeader>
                <DialogTitle>{selectedMenu.menuName}</DialogTitle>
                <DialogDescription>
                  {selectedMenu.menuNameEnglish}
                </DialogDescription>
              </DialogHeader>
              <ScrollArea className="p-4">
                <p className="text-gray-700 mb-3">
                  {selectedMenu.menuInformation}
                </p>
                <p className="text-sm text-gray-500">
                  상품코드: {selectedMenu.menuCode}
                </p>
                <p className="text-sm text-gray-500">
                  카테고리: {selectedMenu.menuCategoryName}
                </p>
                <p className="text-sm text-gray-500">
                  칼로리: {selectedMenu.menuKcal}kcal
                </p>
                <p className="text-sm text-gray-500 mb-4">
                  가격: ₩{selectedMenu.menuPrice.toLocaleString()}
                </p>

                <div className="mt-4 flex gap-2">
                  {selectedMenu.soldOutStatus === 'SOLD_OUT' ? (
                    <Button
                      onClick={() => handleRestock(selectedMenu)}
                      className="bg-kpi-green text-white"
                    >
                      재입고
                    </Button>
                  ) : (
                    <Button
                      onClick={() => handleSoldOut(selectedMenu)}
                      variant="outline"
                      className="text-kpi-orange border-kpi-orange"
                    >
                      품절 처리
                    </Button>
                  )}
                  <Button
                    variant="outline"
                    onClick={() => setIsDetailModalOpen(false)}
                  >
                    닫기
                  </Button>
                </div>
              </ScrollArea>
            </>
          )}
        </DialogContent>
      </Dialog>

      {dialog}
    </div>
  );
};
