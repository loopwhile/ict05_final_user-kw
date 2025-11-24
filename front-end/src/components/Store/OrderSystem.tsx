// src/pages/OrderSystem.tsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Card } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Input } from '../ui/input';
import { useOrder } from '../Common/OrderContext';
import {
  Plus,
  Minus,
  ShoppingCart,
  CreditCard,
  X,
  Store,
  Package,
  Truck,
  Gift,
  Percent,
} from 'lucide-react';
import { toast } from 'sonner';

/* ============================
   공통 axios 인스턴스
============================ */
const api = axios.create({
  baseURL: import.meta.env.VITE_BACKEND_API_BASE_URL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  // 🔥 로그인할 때 localStorage에 저장한 토큰 키 이름과 반드시 같아야 함!!
  const token = localStorage.getItem('accessToken'); // 예: 'accessToken' / 'storeAccessToken'

  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});


/* ============================
   타입 정의
============================ */
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
  ingredients: string;

  // 🔥 백엔드 필드 그대로 사용
  storeMenuSoldout: SoldOutStatus;

  menuShow: MenuShow;
};

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
};

interface MenuItem {
  id: number;
  name: string;
  price: number;
  image: string;
  available: boolean; // ← 품절 여부
}

interface MenuCategoryWithItems {
  id: string;
  name: string;
  items: MenuItem[];
}

interface OrderItem {
  id: number;
  name: string;
  price: number;
  quantity: number;
  image: string;
  options?: string[];
}

interface Order {
  id: string;
  items: OrderItem[];
  total: number;
  originalTotal: number;
  discount: number;
  status: 'preparing' | 'cooking' | 'ready' | 'completed';
  orderTime: Date | string;
  customer?: string;
  paymentMethod: string;
  orderType: '방문' | '포장' | '배달';
}

/* ============================
   유틸: 카테고리별 이모지
============================ */
const getEmojiForCategory = (categoryName: string) => {
  if (categoryName.includes('세트') || categoryName.includes('버거')) return '🍔';
  if (categoryName.includes('토스트')) return '🥪';
  if (categoryName.includes('사이드') || categoryName.includes('튀김')) return '🍟';
  if (categoryName.includes('음료') || categoryName.includes('콜라') || categoryName.includes('사이다')) return '🥤';
  return '🍔';
};

const ALL_CATEGORY_KEY = 'ALL';
const PAGE_SIZE = 16;
const EXCLUDED_CATEGORY_NAMES = ['메뉴'];


/* ============================
    주문 등록 화면
============================ */
export function OrderSystem() {
  const [menuCategories, setMenuCategories] = useState<MenuCategoryWithItems[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>(ALL_CATEGORY_KEY);
  const [currentPage, setCurrentPage] = useState<number>(1);

  const [cart, setCart] = useState<OrderItem[]>([]);
  const [orderType, setOrderType] = useState<'방문' | '포장' | '배달'>('방문');
  const [customerName, setCustomerName] = useState('');
  const [currentTime, setCurrentTime] = useState(new Date());
  const [paymentMethod, setPaymentMethod] = useState('');
  const [discount, setDiscount] = useState(0);
  const [discountType, setDiscountType] = useState<'amount' | 'percent'>('amount');
  const [customDiscountValue, setCustomDiscountValue] = useState('');
  const [orders, setOrders] = useState<Order[]>([]);

  const { addOrder } = useOrder();

  /* ============================
      시계
  ============================ */
  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  /* ============================
      메뉴 조회 (DB → 프론트 변환)
  ============================ */
  useEffect(() => {
    const fetchMenus = async () => {
      try {
        const pageSize = 200;
        let page = 0;
        let totalPages = 1;
        const all: StoreMenu[] = [];

        do {
          const res = await api.get<PageResponse<any>>('/API/menu/list', {
            params: { page, size: pageSize },
          });

          const data = res.data?.content ?? [];
          totalPages = res.data?.totalPages ?? 1;

          const normalized: StoreMenu[] = data.map((m: any) => ({
            menuId: m.menuId,
            menuName: m.menuName,
            menuNameEnglish: m.menuNameEnglish,
            menuCategoryId: m.menuCategoryId,
            menuCategoryName: m.menuCategoryName,
            menuPrice: m.menuPrice,
            menuKcal: m.menuKcal,
            menuInformation: m.menuInformation,
            menuCode: m.menuCode,
            ingredients: m.ingredients ?? '',
            menuShow: m.menuShow,
            storeMenuSoldout: m.storeMenuSoldout ?? 'ON_SALE', // ⭐ 핵심
          }));

          all.push(...normalized);
          page += 1;
        } while (page < totalPages);

        const categoryMap = new Map<string, MenuCategoryWithItems>();

        all.forEach((m) => {
          const catId = String(m.menuCategoryId);
          const catName = (m.menuCategoryName ?? '').trim();

          if (EXCLUDED_CATEGORY_NAMES.includes(catName)) return;
          if (m.menuShow !== 'SHOW') return;

          const emoji = getEmojiForCategory(catName);

          // 품절 여부 판단
          const available = m.storeMenuSoldout === 'ON_SALE';

          if (!categoryMap.has(catId)) {
            categoryMap.set(catId, { id: catId, name: catName, items: [] });
          }

          categoryMap.get(catId)!.items.push({
            id: m.menuId,
            name: m.menuName,
            price: m.menuPrice,
            image: emoji,
            available,
          });
        });

        const categoryList = [...categoryMap.values()]
          .sort((a, b) => a.name.localeCompare(b.name, 'ko'))
          .map((c) => ({
            ...c,
            items: c.items.sort((a, b) => a.name.localeCompare(b.name, 'ko')),
          }));

        setMenuCategories(categoryList);
        setSelectedCategory(ALL_CATEGORY_KEY);
        setCurrentPage(1);
      } catch (e) {
        console.error('메뉴 조회 실패:', e);
        toast.error('메뉴를 불러오지 못했습니다.');
      }
    };

    fetchMenus();
  }, []);

  /* ============================
      주문 히스토리 로드
  ============================ */
  useEffect(() => {
    const existingOrders = localStorage.getItem('allOrders');
    if (existingOrders) {
      try {
        const parsed = JSON.parse(existingOrders);
        setOrders(parsed);
      } catch (e) {}
    }
  }, []);

  /* ============================
      장바구니 추가
  ============================ */
  const addToCart = (item: MenuItem | OrderItem) => {
    const available = (item as MenuItem).available;
    if (!available) {
      toast.error('품절된 상품입니다.');
      return;
    }

    const exists = cart.find((c) => c.id === item.id);
    if (exists) {
      setCart(
        cart.map((c) =>
          c.id === item.id ? { ...c, quantity: c.quantity + 1 } : c,
        ),
      );
    } else {
      setCart([
        ...cart,
        {
          id: item.id,
          name: item.name,
          price: item.price,
          quantity: 1,
          image: item.image,
        },
      ]);
    }
  };

  /* ============================
      장바구니 감소/삭제
  ============================ */
  const removeFromCart = (id: number) => {
    const existing = cart.find((i) => i.id === id);
    if (existing && existing.quantity > 1) {
      setCart(
        cart.map((i) =>
          i.id === id ? { ...i, quantity: i.quantity - 1 } : i,
        ),
      );
    } else {
      setCart(cart.filter((i) => i.id !== id));
    }
  };

  const clearCart = () => {
    setCart([]);
    setDiscount(0);
  };

  /* ============================
      가격 계산
  ============================ */
  const subtotal = () =>
    cart.reduce((sum, item) => sum + item.price * item.quantity, 0);

  const total = () => {
    const s = subtotal();
    if (discountType === 'percent') return s - (s * discount) / 100;
    return s - discount;
  };

  /* ============================
      결제처리 → 백엔드 주문 생성
  ============================ */
  const processPayment = async (method: string) => {
    if (cart.length === 0) {
      toast.error('상품을 선택하세요');
      return;
    }

    try {
      const stored = JSON.parse(localStorage.getItem('allOrders') || '[]');

      let max = 0;
      [...stored, ...orders].forEach((o) => {
        const n = parseInt(String(o.id).replace('#', ''), 10);
        if (!isNaN(n) && n > max) max = n;
      });

      const orderId = `#${String(max + 1).padStart(4, '0')}`;

      const s = subtotal();
      const t = total();
      const disc = s - t;

      const mapOrderType = {
        방문: 'VISIT',
        포장: 'TAKEOUT',
        배달: 'DELIVERY',
      };

      const mapPayment = {
        현금: 'cash',
        카드: 'card',
        상품권: 'voucher',
      };

      await api.post('/api/customer-orders', {
        // storeId 안 보냄
        orderCode: orderId,
        orderType: mapOrderType[orderType],
        paymentType: mapPayment[method] || 'cash',
        totalPrice: t,
        discount: disc,
        customerName: customerName || null,
        items: cart.map((item) => ({
          menuId: item.id,
          quantity: item.quantity,
          unitPrice: item.price,
        })),
      });

      const newOrder: Order = {
        id: orderId,
        items: [...cart],
        total: t,
        originalTotal: s,
        discount: disc,
        status: 'preparing',
        orderTime: new Date(),
        customer: customerName || undefined,
        paymentMethod: method,
        orderType,
      };

      const updated = [newOrder, ...orders];
      setOrders(updated);

      localStorage.setItem(
        'allOrders',
        JSON.stringify([newOrder, ...stored]),
      );

      addOrder({
        items: cart.map((item) => ({
          id: String(item.id),
          name: item.name,
          price: item.price,
          quantity: item.quantity,
        })),
        totalAmount: t,
        orderType: mapOrderType[orderType],
        paymentMethod: mapPayment[method] || 'cash',
        status: 'preparing',
      });

      toast.success(`결제완료: ${orderId}`);
      setCart([]);
      setCustomerName('');
      setDiscount(0);
    } catch (e) {
      console.error(e);
      toast.error('결제 오류');
    }
  };

  /* ============================
      카테고리 필터링 / 페이지 처리
  ============================ */
  const filteredItems =
    selectedCategory === ALL_CATEGORY_KEY
      ? menuCategories.flatMap((c) => c.items)
      : menuCategories.find((c) => c.id === selectedCategory)?.items ?? [];

  const totalPages = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE));

  const pageItems = filteredItems.slice(
    (currentPage - 1) * PAGE_SIZE,
    (currentPage - 1) * PAGE_SIZE + PAGE_SIZE,
  );

  const goToPage = (p: number) => {
    if (p < 1 || p > totalPages) return;
    setCurrentPage(p);
  };

  /* ============================
      JSX
  ============================ */
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">주문 등록</h1>
          <p className="text-dark-gray">
            {currentTime.toLocaleString('ko-KR')} | {orderType} 주문
          </p>
        </div>
      </div>

      <div className="grid grid-cols-12 gap-6">
        {/* 메뉴 영역 */}
        <div className="col-span-8">
          <div className="sticky top-0 bg-white pb-3 z-10">
            <div className="flex gap-2 mb-2 overflow-x-auto">

              {/* 전체 탭 */}
              <Button
                key="all"
                onClick={() => {
                  setSelectedCategory(ALL_CATEGORY_KEY);
                  setCurrentPage(1);
                }}
                className={`rounded-full px-4 py-2 text-sm font-medium ${
                  selectedCategory === ALL_CATEGORY_KEY
                    ? 'bg-kpi-red text-white'
                    : 'bg-gray-100 text-gray-700'
                }`}
              >
                전체
              </Button>

              {/* 카테고리 탭 */}
              {menuCategories.map((c) => (
                <Button
                  key={c.id}
                  onClick={() => {
                    setSelectedCategory(c.id);
                    setCurrentPage(1);
                  }}
                  className={`rounded-full px-4 py-2 text-sm font-medium ${
                    selectedCategory === c.id
                      ? 'bg-kpi-red text-white'
                      : 'bg-gray-100 text-gray-700'
                  }`}
                >
                  {c.name}
                </Button>
              ))}
            </div>
          </div>

          {/* 메뉴 카드 */}
          <div className="grid grid-cols-4 gap-4">
            {pageItems.map((item) => (
              <Card
                key={item.id}
                className={`p-4 cursor-pointer ${
                  item.available
                    ? 'hover:scale-105 hover:shadow-lg'
                    : 'opacity-40 cursor-not-allowed'
                }`}
                onClick={() => item.available && addToCart(item)}
              >
                <div className="text-center">
                  <div className="text-4xl mb-3">{item.image}</div>
                  <h3 className="font-medium mb-2">{item.name}</h3>
                  <div className="text-lg font-semibold text-kpi-red">
                    {item.price.toLocaleString()}원
                  </div>
                  {!item.available && (
                    <Badge variant="destructive" className="mt-2">
                      품절
                    </Badge>
                  )}
                </div>
              </Card>
            ))}
          </div>

          {/* 페이지네이션 */}
          {filteredItems.length > PAGE_SIZE && (
            <div className="flex justify-center gap-2 mt-4">
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage === 1}
                onClick={() => goToPage(currentPage - 1)}
              >
                이전
              </Button>

              {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
                <Button
                  key={p}
                  variant={p === currentPage ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => goToPage(p)}
                >
                  {p}
                </Button>
              ))}

              <Button
                variant="outline"
                size="sm"
                disabled={currentPage === totalPages}
                onClick={() => goToPage(currentPage + 1)}
              >
                다음
              </Button>
            </div>
          )}
        </div>

        {/* 오른쪽 주문 내역 */}
        <div className="col-span-4">
          <Card className="p-4 sticky top-4">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold flex items-center gap-2">
                <ShoppingCart className="w-5 h-5" />
                주문 내역
              </h3>

              {cart.length > 0 && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={clearCart}
                  className="text-red-600"
                >
                  <X className="w-4 h-4" />
                </Button>
              )}
            </div>

            {/* 고객 정보 */}
            <div className="mb-4 space-y-2">
              <Input
                placeholder="고객명 (선택)"
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
              />

              <div className="flex gap-1">
                <Button
                  size="sm"
                  variant={orderType === '방문' ? 'default' : 'outline'}
                  onClick={() => setOrderType('방문')}
                  className="flex-1"
                >
                  방문
                </Button>
                <Button
                  size="sm"
                  variant={orderType === '포장' ? 'default' : 'outline'}
                  onClick={() => setOrderType('포장')}
                  className="flex-1"
                >
                  포장
                </Button>
                <Button
                  size="sm"
                  variant={orderType === '배달' ? 'default' : 'outline'}
                  onClick={() => setOrderType('배달')}
                  className="flex-1"
                >
                  배달
                </Button>
              </div>
            </div>

            {/* 카트 */}
            <div className="space-y-3 mb-4 max-h-80 overflow-y-auto">
              {cart.length === 0 ? (
                <div className="text-center py-6 text-gray-500">
                  <ShoppingCart className="w-8 h-8 mx-auto mb-2" />
                  주문할 상품을 선택하세요
                </div>
              ) : (
                cart.map((item) => (
                  <div
                    key={item.id}
                    className="flex items-center justify-between p-2 bg-gray-50 rounded"
                  >
                    <div className="flex items-center gap-2">
                      <span className="text-lg">{item.image}</span>
                      <div>
                        <div className="font-medium text-sm">{item.name}</div>
                        <div className="text-xs text-gray-500">
                          {item.price.toLocaleString()}원
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-1">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => removeFromCart(item.id)}
                        className="w-6 h-6 p-0"
                      >
                        <Minus className="w-3 h-3" />
                      </Button>

                      <span className="mx-2 min-w-[20px] text-center">
                        {item.quantity}
                      </span>

                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => addToCart(item)}
                        className="w-6 h-6 p-0"
                      >
                        <Plus className="w-3 h-3" />
                      </Button>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* 금액 */}
            {cart.length > 0 && (
              <div className="border-t pt-4 space-y-2">
                <div className="flex justify-between text-sm">
                  <span>소계</span>
                  <span>{subtotal().toLocaleString()}원</span>
                </div>

                {discount > 0 && (
                  <div className="flex justify-between text-sm text-red-600">
                    <span>할인</span>
                    <span>
                      -{(subtotal() - total()).toLocaleString()}원
                    </span>
                  </div>
                )}

                <div className="flex justify-between text-lg font-semibold pt-2 border-t">
                  <span>총액</span>
                  <span className="text-kpi-red">{total().toLocaleString()}원</span>
                </div>
              </div>
            )}

            {/* 결제 버튼 */}
            {cart.length > 0 && (
              <div className="space-y-2 mt-4">
                <Button
                  onClick={() => processPayment('카드')}
                  className="w-full bg-kpi-red text-white"
                >
                  <CreditCard className="w-4 h-4 mr-2" />
                  카드 결제
                </Button>

                <Button
                  onClick={() => processPayment('현금')}
                  variant="outline"
                  className="w-full"
                >
                  <Package className="w-4 h-4 mr-2" />
                  현금 결제
                </Button>

                <Button
                  onClick={() => processPayment('상품권')}
                  variant="outline"
                  className="w-full"
                >
                  <Gift className="w-4 h-4 mr-2" />
                  상품권 결제
                </Button>
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
