// src/App.tsx
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";            // ✅ navigate 사용
import { Layout } from "./components/Common/Layout";
// import LoginPage from "./pages/LoginPage";
// import { Register } from "./components/Common/Register";

import { StoreDashboard } from "./components/Store/Dashboard";
import { StoreMenuManagement } from "./components/Store/MenuManagement";
import { OrderSystem } from "./components/Store/OrderSystem";
import { OrderList } from "./components/Store/OrderList";
import { KitchenDisplay } from "./components/Store/KitchenDisplay";
import { InventoryManagement } from "./components/Store/InventoryManagement";
import { InventoryOrders } from "./components/Store/InventoryOrders";
import { FinanceManagement } from "./components/Store/FinanceManagement";
import { StaffList } from "./components/Store/StaffList";
import { StaffWorkReports } from "./components/Store/StaffWorkReports";
import { NoticeEducation } from "./components/Store/NoticeEducation";
import { DailyClosingPage } from "./components/Store/DailyClosingPage";
import { DailyClosingList } from "./components/Store/DailyClosingList";
import { DailyClosingDetail } from "./components/Store/DailyClosingDetail";
import { MyPage} from "./components/Common/MyPage";
import { Toaster } from "./components/ui/sonner";
import { ErrorBoundary } from "./components/Common/ErrorBoundary";
import { OrderProvider } from "./components/Common/OrderContext";
// src/App.tsx (상단 imports)
import { StaffSchedule } from "./components/Store/StaffSchedule"; // ⬅️ 추가
import api from "./lib/authApi";
import KpiReport from "./components/Store/reports/KpiReport";
import OrderReport from "./components/Store/reports/OrderReport";
import NotificationSettings from "./components/Store/NotificationSettings";
import MenuReport from "./components/Store/reports/MenuReport";
import TimeReport from "./components/Store/reports/TimeReport";
import MaterialReport from "./components/Store/reports/MaterialReport";
 // ✅ 인터셉터/강제로그아웃 핸들러

type HeaderInfo = {
  memberName: string;
  storeName?: string | null;
  memberImagePath?: string | null;
};

export default function App() {
  const navigate = useNavigate();
  const [currentPage, setCurrentPage] = useState("dashboard");
  const [ready, setReady] = useState(false);
  const [headerInfo, setHeaderInfo] = useState<HeaderInfo | null>(null);
  const [selectedClosingDate, setSelectedClosingDate] = useState<string | null>(null);
  const [closingListFromDate, setClosingListFromDate] = useState<string | undefined>(undefined);
  const [closingListToDate, setClosingListToDate] = useState<string | undefined>(undefined);

  useEffect(()=> {
    const access = localStorage.getItem("accessToken");
    if(!access){
      navigate("/login", {replace: true});
      return;
    }
    // 1) 토큰 검증
    api
      .get("/me")
      .then(() => {
        setReady(true);
      })
      .catch(() => {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        navigate("/login", { replace: true });
      });

    // 2) 헤더용 마이페이지 정보 한번 더 가져오기
    api
      .get("/api/myPage")
      .then((res) => {
        setHeaderInfo({
          memberName: res.data.name,       // 응답의 name
          storeName: res.data.storeName,   // 응답의 storeName
          memberImagePath: res.data.memberImagePath,
        });
      })
      .catch((err) => {
        console.error("헤더용 myPage 조회 실패", err);
      });
  }, [navigate]);
  // if (!ready) return null; // <-- ready 전에 Dashboard가 useEffect 실행 못함
  // const handleLogout =() => {
  //   localStorage.removeItem("accessToken");
  //   localStorage.removeItem("refreshToken");
  //   toast.success("로그아웃되었습니다.");
  //   navigate("/login", {replace : true});
  // }

  const handlePageChange = (page: string) => setCurrentPage(page);

  const handleProfileImageChange = (path: string | null) => {
    setHeaderInfo((prev) => ({
      ...(prev ?? { memberName: "", storeName: null }),
      memberImagePath: path,
    }));
  };

  const renderPage = () => {
    switch (currentPage) {
      case "dashboard": return <StoreDashboard/>;
      case "menu": return <ErrorBoundary><StoreMenuManagement /></ErrorBoundary>;
      case "orders":
      case "order-pos": return (<OrderProvider><ErrorBoundary><OrderSystem /></ErrorBoundary></OrderProvider>);
      case "order-list": return (<OrderProvider><ErrorBoundary><OrderList /></ErrorBoundary></OrderProvider> )
      case "order-kitchen":return (<OrderProvider><ErrorBoundary><KitchenDisplay /></ErrorBoundary></OrderProvider>);
      case "inventory":
      case "inventory-orders": return <ErrorBoundary><InventoryOrders /></ErrorBoundary>;
      case "inventory-management": return <ErrorBoundary><InventoryManagement /></ErrorBoundary>;
      case "finance": return <ErrorBoundary><FinanceManagement /></ErrorBoundary>;
      case "daily-closing": return (<OrderProvider><ErrorBoundary><DailyClosingPage onPageChange={handlePageChange}/></ErrorBoundary></OrderProvider>);
      case "daily-closing-list":
        return (
          <OrderProvider>
            <ErrorBoundary>
              <DailyClosingList
                // 처음 들어올 때는 undefined라서, 컴포넌트 내부 기본값(일주일/오늘) 사용
                initialFromDate={closingListFromDate}
                initialToDate={closingListToDate}
                // 사용자가 날짜 바꾸면 App에 반영
                onDateRangeChange={(from, to) => {
                  setClosingListFromDate(from);
                  setClosingListToDate(to);
                }}
                // 상세보기 눌렀을 때
                onSelectDate={(date) => {
                  setSelectedClosingDate(date);            
                  setCurrentPage("daily-closing-detail");  
                }}
              />
            </ErrorBoundary>
          </OrderProvider>
        );
      case "daily-closing-detail":
        return (
          <OrderProvider>
            <ErrorBoundary>
              {selectedClosingDate ? (
                <DailyClosingDetail
                  date={selectedClosingDate}
                  onBack={() => setCurrentPage("daily-closing-list")}
                />
              ) : (
                // 혹시 날짜 없이 들어왔을 때는 리스트로 보내기
                <DailyClosingList
                  onSelectDate={(date) => {
                    setSelectedClosingDate(date);
                    setCurrentPage("daily-closing-detail");
                  }}
                />
              )}
            </ErrorBoundary>
          </OrderProvider>
        );  
      case "mypage": return <ErrorBoundary><MyPage onProfileImageChange={handleProfileImageChange}/></ErrorBoundary>
      case "staff":
      case "staff-list": return <ErrorBoundary><StaffList /></ErrorBoundary>;
    
      // renderContent() 내부 switch
      case "staff-schedule": return <ErrorBoundary><StaffSchedule /></ErrorBoundary>;
      case "notice": return <ErrorBoundary><NoticeEducation /></ErrorBoundary>;
      case "kpi-report": return <ErrorBoundary><KpiReport /></ErrorBoundary>;
      case "order-report": return <ErrorBoundary><OrderReport /></ErrorBoundary>;
      case "menu-report": return <ErrorBoundary><MenuReport /></ErrorBoundary>;
      case "material-report": return <ErrorBoundary><MaterialReport /></ErrorBoundary>;
      case "daytime-report": return <ErrorBoundary><TimeReport /></ErrorBoundary>;
      
      case "settings-notifications": return (<ErrorBoundary><NotificationSettings /></ErrorBoundary>);
      default:
        return (
          <div className="flex items-center justify-center h-64 bg-white rounded-xl shadow-sm">
            <div className="text-center">
              <h2 className="text-xl font-semibold text-gray-900 mb-2">{currentPage} 페이지</h2>
              <p className="text-dark-gray">가맹점 {currentPage} 기능이 곧 추가됩니다.</p>
            </div>
          </div>
        );
    }
  };

return (
  <>
    <Layout
      userType="Store"
      memberName={headerInfo?.memberName ?? ""}  // 없으면 빈 문자열
      storeName={headerInfo?.storeName ?? ""}
      currentPage={currentPage}
      onPageChange={handlePageChange}
      memberImagePath={headerInfo?.memberImagePath ?? null} 
    >
      {renderPage()}
    </Layout>
    <Toaster />
  </>
);

}
