// src/main.tsx
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route, useNavigate } from "react-router-dom";
import App from "./App";
import Login from "./components/Common/Login";
import { Register } from "./components/Common/Register";
import api from "./lib/authApi";
import { toast } from "sonner";
import "./index.css";
import { requestFcmToken } from "./lib/firebase";

    const serviceWorkerUrl = '/firebase-messaging-sw.js';

if ('serviceWorker' in navigator) {
  // 앱 시작 시 한 번만 등록
  navigator.serviceWorker
    .register(swPath)
    .then((reg) => console.log('[SW] registered:', reg.scope))
    .catch((err) => console.warn('[SW] register failed:', err));
}

// Login용 래퍼: 로그인 성공 후 FCM 등록까지 처리
function LoginPage() {
  const navigate = useNavigate();

  const handleLogin = async (cred: { email: string; password: string }) => {
    try {
      // 1) 로그인 요청 (예시: 응답에서 accessToken, storeId 획득)
      //    프로젝트 응답 형태에 맞게 키 이름만 맞춰주세요.
      const res = await api.post("/login", cred);
      const accessToken = res.data?.accessToken ?? res.headers?.authorization?.replace('Bearer ', '');
      const storeId = res.data?.storeId ?? res.data?.user?.storeId;

      // 2) 브라우저 FCM 토큰 발급 (권한 요청 포함)
      const token = await requestFcmToken();

      // 3) 토큰 업서트 & 기본 토픽 구독
      if (accessToken && token) {
        await api.post(
          "/fcm/token",
          { token, platform: "WEB", deviceId: navigator.userAgent.slice(0, 120) },
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );

        if (storeId) {
          await api.post(
            `/fcm/topic/subscribe?token=${encodeURIComponent(token)}&topic=store-${storeId}`,
            {},
            { headers: { Authorization: `Bearer ${accessToken}` } }
          );
        }
      }

      // 4) 대시보드로 이동
      toast.success("로그인 성공");
      navigate("/dashboard", { replace: true });
    } catch (e) {
      console.error(e);
      toast.error("로그인 실패");
    }
  };

  // 기존 Login 컴포넌트가 onLogin을 받도록(또는 내부에서 호출하도록) 되어있다는 가정
  return <Login onLogin={handleLogin} />;
}

// Register용 래퍼: props 채워서 내려보내기
function RegisterPage() {
  const navigate = useNavigate();

  const handleRegister = async (userData: any) => {
    try {
      // 백엔드 회원가입: baseURL = http://localhost:8082/user
      // → 최종 요청 URL = http://localhost:8082/user/join
      await api.post("/join", {
        email: userData.email,
        password: userData.password,
        name: userData.name,
        phone: userData.phone,
      });
      toast.success("회원가입이 완료되었습니다. 로그인해 주세요.");
      navigate("/login", { replace: true });
    } catch (e) {
      console.error(e);
      toast.error("회원가입 중 오류가 발생했습니다.");
    }
  };

  return (
    <Register
      onRegister={handleRegister}
      onBackToLogin={() => navigate("/login", { replace: true })}
    />
  );
}

// 'npm run build' 시에는 'production', 'npm run dev' 시에는 'development'가 됩니다.
const basename = import.meta.env.MODE === 'production' ? '/user' : '/';

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter basename={import.meta.env.BASE_URL}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/dashboard/*" element={<App />} />
        <Route path="/" element={<App />} />
        {/* /user/ 경로에 대한 명시적 라우트 추가 */}
        <Route path="/user" element={<App />} />
        <Route path="/user/" element={<App />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);
