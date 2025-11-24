import React, { createContext, useContext, useState, ReactNode } from 'react';

export interface Order {
  id: string;
  orderNumber: string;
  items: Array<{
    id: string;
    name: string;
    price: number;
    quantity: number;
    options?: string[];
  }>;
  totalAmount: number;
  orderType: 'visit' | 'takeout' | 'delivery';
  paymentMethod: 'cash' | 'card' | 'voucher';
  timestamp: Date;
  status: 'pending' | 'preparing' | 'ready' | 'completed' | 'cancelled';
}

export interface CashPayments {
  visitPayments: number;    // 방문 현금결제
  takeoutPayments: number;  // 포장 현금결제
  deliveryPayments: number; // 배달 현금결제
}

export interface CardPayments {
  visitPayments: number;    // 방문 카드결제
  takeoutPayments: number;  // 포장 카드결제
  deliveryPayments: number; // 배달 카드결제
}

interface OrderContextType {
  orders: Order[];
  cashPayments: CashPayments;
  cardPayments: CardPayments;
  addOrder: (order: Omit<Order, 'id' | 'orderNumber' | 'timestamp'>) => string;
  updateOrderStatus: (orderId: string, status: Order['status']) => void;
  getOrdersByStatus: (status: Order['status']) => Order[];
  getTodayCashPayments: () => CashPayments;
  getTodayCardPayments: () => CardPayments;
  resetDailyData: () => void;
}

const OrderContext = createContext<OrderContextType | undefined>(undefined);

// 테스트용 초기 현금/카드 결제 데이터
const initialOrders: Order[] = [
  {
    id: 'test_001',
    orderNumber: '001',
    items: [{ id: '1', name: '아메리카노', price: 4500, quantity: 2 }],
    totalAmount: 9000,
    orderType: 'visit',
    paymentMethod: 'cash',
    timestamp: new Date(),
    status: 'completed'
  },
  {
    id: 'test_002',
    orderNumber: '002',
    items: [{ id: '2', name: '카페라떼', price: 5000, quantity: 1 }],
    totalAmount: 5000,
    orderType: 'takeout',
    paymentMethod: 'cash',
    timestamp: new Date(),
    status: 'completed'
  },
  {
    id: 'test_003',
    orderNumber: '003',
    items: [{ id: '3', name: '바닐라라떼', price: 5500, quantity: 1 }],
    totalAmount: 5500,
    orderType: 'visit',
    paymentMethod: 'card',
    timestamp: new Date(),
    status: 'completed'
  },
  {
    id: 'test_004',
    orderNumber: '004',
    items: [{ id: '4', name: '아이스티', price: 4000, quantity: 3 }],
    totalAmount: 12000,
    orderType: 'delivery',
    paymentMethod: 'card',
    timestamp: new Date(),
    status: 'completed'
  }
];

export function OrderProvider({ children }: { children: ReactNode }) {
  const [orders, setOrders] = useState<Order[]>(initialOrders);
  const [orderCounter, setOrderCounter] = useState(5);

  // 오늘 현금 결제 금액 계산
  const getTodayCashPayments = (): CashPayments => {
    const today = new Date().toDateString();
    
    const todayOrders = orders.filter(order => 
      order.timestamp.toDateString() === today && 
      order.paymentMethod === 'cash' &&
      order.status !== 'cancelled'
    );

    const payments = {
      visitPayments: 0,
      takeoutPayments: 0,
      deliveryPayments: 0
    };

    todayOrders.forEach(order => {
      switch (order.orderType) {
        case 'visit':
          payments.visitPayments += order.totalAmount;
          break;
        case 'takeout':
          payments.takeoutPayments += order.totalAmount;
          break;
        case 'delivery':
          payments.deliveryPayments += order.totalAmount;
          break;
      }
    });

    return payments;
  };

  // 오늘 카드 결제 금액 계산
  const getTodayCardPayments = (): CardPayments => {
    const today = new Date().toDateString();
    
    const todayOrders = orders.filter(order => 
      order.timestamp.toDateString() === today && 
      order.paymentMethod === 'card' &&
      order.status !== 'cancelled'
    );

    const payments = {
      visitPayments: 0,
      takeoutPayments: 0,
      deliveryPayments: 0
    };

    todayOrders.forEach(order => {
      switch (order.orderType) {
        case 'visit':
          payments.visitPayments += order.totalAmount;
          break;
        case 'takeout':
          payments.takeoutPayments += order.totalAmount;
          break;
        case 'delivery':
          payments.deliveryPayments += order.totalAmount;
          break;
      }
    });

    return payments;
  };

  const addOrder = (orderData: Omit<Order, 'id' | 'orderNumber' | 'timestamp'>): string => {
    const orderId = `order_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const orderNumber = orderCounter.toString().padStart(3, '0');
    
    const newOrder: Order = {
      ...orderData,
      id: orderId,
      orderNumber,
      timestamp: new Date(),
    };

    setOrders(prev => [...prev, newOrder]);
    setOrderCounter(prev => prev + 1);

    return orderId;
  };

  const updateOrderStatus = (orderId: string, status: Order['status']) => {
    setOrders(prev => 
      prev.map(order => 
        order.id === orderId ? { ...order, status } : order
      )
    );
  };

  const getOrdersByStatus = (status: Order['status']): Order[] => {
    return orders.filter(order => order.status === status);
  };

  const resetDailyData = () => {
    // 일일 마감시 사용 - 필요시 구현
    console.log('일일 데이터 초기화');
  };

  const cashPayments = getTodayCashPayments();
  const cardPayments = getTodayCardPayments();

  const value: OrderContextType = {
    orders,
    cashPayments,
    cardPayments,
    addOrder,
    updateOrderStatus,
    getOrdersByStatus,
    getTodayCashPayments,
    getTodayCardPayments,
    resetDailyData
  };

  return (
    <OrderContext.Provider value={value}>
      {children}
    </OrderContext.Provider>
  );
}

export function useOrder() {
  const context = useContext(OrderContext);
  if (!context) {
    throw new Error('useOrder must be used within an OrderProvider');
  }
  return context;
}