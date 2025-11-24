// // src/pages/MainApp.jsx
// import React, { useEffect, useState } from "react";
// import { useNavigate } from "react-router-dom";
// import { Layout } from "../components/Common/Layout";
// import { StoreDashboard } from "../components/Store/Dashboard";
// import { Toaster, toast } from "sonner";
// import api from "../lib/authApi";

// export default function MainApp() {
//   const navigate = useNavigate();

//   // 현재 페이지 (사이드바 항목 id와 동일)
//   const [currentPage, setCurrentPage] = useState("dashboard");

//   // 토큰 가드 + 인터셉터 동작 확인
//   useEffect(() => {
//     const access = localStorage.getItem("accessToken");
//     if (!access) {
//       navigate("/login", { replace: true });
//       return;
//     }
//     api.get("/user").catch(() => {
//       navigate("/login", { replace: true });
//     });
//   }, [navigate]);

//   const handleLogout = () => {
//     localStorage.removeItem("accessToken");
//     localStorage.removeItem("refreshToken");
//     toast.success("로그아웃되었습니다.");
//     navigate("/login", { replace: true });
//   };

//   // 페이지 전환 핸들러 (Layout에서 호출)
//   const handlePageChange = (page) => setCurrentPage(page);

//   // 데모: 현재는 대시보드만 렌더, 필요 시 switch문으로 확장
//   const renderPage = () => {
//     switch (currentPage) {
//       case "dashboard":
//       default:
//         return <StoreDashboard />;
//     }
//   };

//   return (
//     <>
//       <Layout
//         currentPage={currentPage}
//         onPageChange={handlePageChange}
//         onLogout={handleLogout}
//       >
//         {renderPage()}
//       </Layout>
//       <Toaster />
//     </>
//   );
// }
