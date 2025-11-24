import React, { useEffect, useState, useCallback } from 'react';
import { 
  Building2, 
  Store, 
  Menu, 
  Settings, 
  LogOut, 
  ChevronDown,
  ChevronRight,
  Home,
  Users,
  Package,
  Truck,
  MessageSquare,
  BarChart3,
  ShoppingCart,
  Calculator,
  UserCheck,
  BookOpen,
  Calendar,
  CircleDollarSign,
  Clock,
  ClipboardList
} from 'lucide-react';
import { Button } from '../ui/button';
import { useNavigate } from "react-router-dom";
import api from "../../lib/authApi";
import { Capacitor } from '@capacitor/core';
import { toast } from 'sonner';
import {
  cleanupPushNotifications,
  addNotificationListenersNative,
  addForegroundListenerWeb,
} from '../../lib/fcm';
import defaultProfile from "/images/default-profile.png"; // 기본 이미지

interface LayoutProps {
  children: React.ReactNode;
  userType: 'HQ' | 'Store';
  currentPage: string;
  onPageChange: (page: string) => void; 
  memberName: string;              
  storeName?: string | null;
  memberImagePath?: string | null;
}

interface MenuItem {
  id: string;
  label: string;
  icon: React.ComponentType<any>;
  children?: MenuItem[];
}

const hqMenuItems: MenuItem[] = [
  { id: 'dashboard', label: '대시보드', icon: Home },
  { id: 'stores', label: '가맹점 관리', icon: Building2 },
  { id: 'menu', label: '메뉴 관리', icon: Package },
  { id: 'inventory', label: '재고 관리', icon: Package },
  { id: 'staff', label: '직원 관리', icon: Users },
  { id: 'logistics', label: '물류/발주', icon: Truck },
  { id: 'notice', label: '공지사항', icon: MessageSquare },
  { id: 'reports', label: '리포트', icon: BarChart3 },
];

const storeMenuItems: MenuItem[] = [
  { id: 'dashboard', label: '대시보드', icon: Home },
  { id: 'menu', label: '메뉴 관리', icon: Package },
  { 
    id: 'orders', 
    label: '주문/결제', 
    icon: ShoppingCart,
    children: [
      { id: 'order-pos', label: '주문등록', icon: ShoppingCart },
      { id: 'order-list', label: '주문리스트', icon: Package },
      { id: 'order-kitchen', label: '주방화면', icon: Settings }
    ]
  },
  { 
    id: 'inventory', 
    label: '재고/발주', 
    icon: Package,
    children: [
      { id: 'inventory-management', label: '재고 관리', icon: Package},
      { id: 'inventory-orders', label: '발주 관리', icon: Truck }
    ]
  },
  { 
    id: 'finance', 
    label: '정산', 
    icon: Calculator,
    children: [
      { id: 'daily-closing', label: '일일 시재/마감', icon: CircleDollarSign },
      { id: 'daily-closing-list', label: '일일 마감 내역', icon: ClipboardList }
    ]
  },
  { 
    id: 'staff', 
    label: '직원 관리', 
    icon: UserCheck,
    children: [
      { id: 'staff-list', label: '직원 목록', icon: Users },
      { id: 'staff-schedule', label: '근무 일정', icon: Calendar },
      
    ]
  },
        { id: 'notice', label: '공지사항', icon: BookOpen },  { 
    id: 'reports', 
    label: '리포트', 
    icon: BarChart3,
    children: [
      { id: 'kpi-report',      label: 'KPI 분석',     icon: BarChart3 },
      { id: 'order-report',    label: '주문 분석',     icon: ShoppingCart },
      { id: 'menu-report',     label: '메뉴 분석',     icon: Package },
      { id: 'material-report', label: '재료 분석',     icon: Package },
      { id: 'daytime-report',  label: '시간/요일 분석', icon: Clock }
    ]
  },
];

export function Layout({ children, userType, currentPage, onPageChange, memberName, storeName, memberImagePath,}: LayoutProps) {

  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [expandedMenus, setExpandedMenus] = useState<string[]>(() => {
    // 초기 로드 시 현재 페이지가 서브메뉴에 속하면 해당 메뉴를 자동으로 확장
    const menuItems = userType === 'HQ' ? hqMenuItems : storeMenuItems;
    const autoExpand: string[] = [];
    
    menuItems.forEach(item => {
      if (item.children && item.children.some(child => child.id === currentPage)) {
        autoExpand.push(item.id);
      }
    });
    
    return autoExpand;
  });

  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      // 1) 서버 및 클라이언트의 FCM 토큰 정리
      await cleanupPushNotifications();

      // 2) 서버 로그아웃(리프레시 토큰 무효화)
      const refreshToken = localStorage.getItem("refreshToken");
      if (refreshToken) {
        await api.post("/logout", { refreshToken }).catch(() => {});
      }
    } finally {
      // 3) 클라이언트 로컬 정보 최종 정리 및 로그인 페이지로 이동
      delete (api as any).defaults?.headers?.common?.Authorization;
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      // fcm_token은 cleanupPushNotifications에서 이미 처리됨
      navigate("/login", { replace: true });
    }
  };
  
  const menuItems = userType === 'HQ' ? hqMenuItems : storeMenuItems;

  const toggleMenu = (menuId: string) => {
    setExpandedMenus(prev => 
      prev.includes(menuId) 
        ? prev.filter(id => id !== menuId)
        : [...prev, menuId]
    );
  };

  const navigateByLink = useCallback((rawLink: string) => {
    if (!rawLink) return;

    // 1) 절대/상대 URL 모두 처리
    const url = new URL(rawLink, window.location.origin);
    let path = url.pathname; // ex) /user/inventory/low, /user/notice/list

    // 2) 백엔드 context-path(/user) 제거
    if (path.startsWith("/user")) {
      path = path.substring("/user".length) || "/";
    }

    // 3) path -> currentPage 매핑
    const map: Record<string, string> = {
      "/": "dashboard",
      "/dashboard": "dashboard",
      "/notice/list": "notice",
      "/inventory/low": "inventory-management",
      "/inventory/expire": "inventory-management",
      "/reports/kpi": "kpi-report",
      "/reports/orders": "order-report",
      "/settings/notifications": "settings-notifications",
    };

    const pageId = map[path];
    if (pageId) {
      onPageChange(pageId);
    } else {
      window.location.href = url.toString();
    }
  }, [onPageChange]);

  useEffect(() => {
    const isNative = Capacitor.getPlatform() !== 'web';

    if (isNative) {
      addNotificationListenersNative(navigateByLink);
    } else {
      const unsubscribe = addForegroundListenerWeb((payload: any) => {
        console.log('[FCM] Foreground:', payload.notification?.title, payload.notification?.body);
        toast.info(payload.notification?.title || '새 알림', {
          description: payload.notification?.body,
          duration: 5000,
          action: {
            label: '보기',
            onClick: () => {
              if (payload.data?.link) {
                navigateByLink(payload.data.link);
              }
            },
          },
        });
      });
      return () => {
        if (typeof unsubscribe === 'function') {
          unsubscribe();
        }
      };
    }
  }, [navigateByLink]);

  const BACKEND_BASE_URL = import.meta.env.VITE_BACKEND_API_BASE_URL;
  const staticRoot = BACKEND_BASE_URL.replace(/\/api\/?$/, ""); 
  const customTitles: Record<string, string> = {
    "settings-notifications": "가맹점 알림 설정",
    "mypage": "마이페이지",
  };

  // 헤더에서 쓸 프로필 이미지 src
  const profileSrc =
    memberImagePath && memberImagePath.trim().length > 0
      ? `${staticRoot}/uploads/profile/${memberImagePath}`
      : defaultProfile;

  return (
    <div className="min-h-screen bg-light-gray flex">
      {/* Sidebar */}
      <div className={`bg-navy-sidebar text-white transition-all duration-300 ${
        sidebarCollapsed ? 'w-16' : 'w-64'
      }`}>
        {/* Logo & Header */}
        <div className="p-4 border-b border-white/20">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-kpi-orange rounded-lg flex items-center justify-center">
              <Store className="w-5 h-5" />
            </div>
            {!sidebarCollapsed && (
              <div>
                <h1 className="font-bold">ToastLab ERP</h1>
                <p className="text-sm text-white/70">{userType === 'HQ' ? '본사' : '가맹점'}</p>
              </div>
            )}
          </div>
        </div>

        {/* Navigation */}
        <nav className="p-4 space-y-2">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const isActive = currentPage === item.id || (item.children && item.children.some(child => child.id === currentPage));
            const isExpanded = expandedMenus.includes(item.id);
            
            return (
              <div key={item.id}>
                <button
                  onClick={() => {
                    if (item.children) {
                      toggleMenu(item.id);
                    } else {
                      onPageChange(item.id);
                    }
                  }}
                  className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${
                    isActive 
                      ? 'bg-white/20 text-white' 
                      : 'text-white/80 hover:bg-white/10 hover:text-white'
                  }`}
                >
                  <Icon className="w-5 h-5 flex-shrink-0" />
                  {!sidebarCollapsed && (
                    <>
                      <span className="flex-1 text-left">{item.label}</span>
                      {item.children && (
                        isExpanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />
                      )}
                    </>
                  )}
                </button>
                
                {item.children && isExpanded && !sidebarCollapsed && (
                  <div className="ml-8 mt-1 space-y-1">
                    {item.children.map((child) => {
                      const ChildIcon = child.icon;
                      const isChildActive = currentPage === child.id;
                      return (
                        <button
                          key={child.id}
                          onClick={() => onPageChange(child.id)}
                          className={`w-full flex items-center gap-2 px-2 py-1 rounded text-sm transition-colors ${
                            isChildActive 
                              ? 'bg-white/20 text-white' 
                              : 'text-white/70 hover:text-white hover:bg-white/5'
                          }`}
                        >
                          <ChildIcon className="w-4 h-4" />
                          {child.label}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </nav>

        {/* Bottom Actions */}
        <div className="space-y-2">
          {!sidebarCollapsed && (
            <>
              <Button 
                variant="ghost" 
                className="w-full justify-start text-white/80 hover:text-white hover:bg-white/10"
                onClick={() => onPageChange('settings-notifications')}
              >
                <Settings className="w-4 h-4 mr-2" />
                알림 설정
              </Button>
              <Button 
                variant="ghost" 
                onClick={handleLogout}
                className="w-full justify-start text-white/80 hover:text-white hover:bg-white/10"
              >
                <LogOut className="w-4 h-4 mr-2" />
                로그아웃
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Top Bar */}
        <header className="bg-white border-b border-gray-200 px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
              >
                <Menu className="w-5 h-5" />
              </Button>
              <div>
                <h2 className="text-lg font-semibold text-gray-900">
                  {(() => {
                    if (currentPage === 'settings-notifications') return '알림 설정'; // ✅ 추가
                    const topLevelMenu = menuItems.find(item => item.id === currentPage);
                    if (topLevelMenu) return topLevelMenu.label;
                    for (const item of menuItems) {
                      if (item.children) {
                        const subMenu = item.children.find(child => child.id === currentPage);
                        if (subMenu) return subMenu.label;
                      }
                    }
                    return '대시보드';
                  })()}
                </h2>
                <p className="text-sm text-dark-gray">
                  {userType === 'HQ' ? '본사 관리 시스템' : '가맹점 관리 시스템'}
                </p>
              </div>
            </div>
            
            <div
              className="flex items-center gap-4 cursor-pointer hover:opacity-80 transition"
              onClick={() => onPageChange("mypage")}
            >
              <div className="text-right">
                {/* 첫 줄: 로그인한 사람 이름 */}
                <p className="text-sm font-medium text-gray-900">
                  {memberName + " 점주" || "점주"}
                </p>

                {/* 둘째 줄: 가맹점 이름 */}
                <p className="text-xs text-dark-gray">
                  {storeName || "가맹점"}
                </p>
              </div>
              <img
                src={profileSrc}
                alt="프로필 이미지"
                className="w-8 h-8 rounded-full object-cover border border-gray-200"
              />
            </div>
          </div>
        </header>

        {/* Content Area */}
        <main className="flex-1 p-6 overflow-auto">
          {children}
        </main>
      </div>
    </div>
  );
}