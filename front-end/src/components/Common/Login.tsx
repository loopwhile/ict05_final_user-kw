// src/components/Common/Login.tsx
import react, { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Store, Lock, Mail } from "lucide-react";
import { toast } from "sonner";
import axios from "axios";
import api from "../../lib/authApi";
import { initializePushNotifications, registerTokenWithServer } from "../../lib/fcm";

// JWT 파싱(스토어ID를 토큰에서 꺼낼 때 사용; 없으면 /me 호출)
function parseJwt(token: string): any | null {
  try {
    const payload = token.split(".")[1];
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

export default function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState<boolean>(false);
  const passRef = useRef<HTMLInputElement>(null);
  const isWeakPassword = (pwd: string) => pwd.length < 6;

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (loading) return;
    setLoading(true);
    setError("");

    if (!email || !password) {
      const msg = "이메일과 비밀번호를 입력하세요.";
      setError(msg);
      toast.error(msg);
      setLoading(false);
      return;
    }
    if (isWeakPassword(password)) {
      const msg = "비밀번호는 6자 이상이어야 합니다.";
      setError(msg);
      toast.error(msg);
      passRef.current?.focus();
      setLoading(false);
      return;
    }

    try {
      // 1) 로그인
      const res = await api.post("/login", { email, password });
      const data = res?.data || {};

      // 토큰 저장(응답 형태에 맞춰 보강)
      const accessToken =
        data.accessToken ?? res.headers?.authorization?.replace("Bearer ", "");
      const refreshToken = data.refreshToken;

      if (accessToken) {
        localStorage.setItem("accessToken", accessToken);
        api.defaults.headers.common["Authorization"] = `Bearer ${accessToken}`;
      }
      if (refreshToken) {
        localStorage.setItem("refreshToken", refreshToken);
      }

      // 2) storeId 확보 (우선순위: 응답 -> /me -> JWT claims)
      let storeId: number | undefined =
        data.storeId ?? data.user?.storeId ?? data.store?.id;

      if (!storeId && accessToken) {
        // (추천) 백엔드에 me 프로필이 있으면 여기서 가져오기
        try {
          const me = await api.get("/me", {
            headers: { Authorization: `Bearer ${accessToken}` },
          });
          storeId = me.data?.storeId ?? me.data?.store?.id ?? storeId;
        } catch {
          /* /me 없음 */
        }
      }
      if (!storeId && accessToken) {
        const claims = parseJwt(accessToken);
        storeId =
          claims?.storeId ??
          claims?.sid ??
          claims?.storeID ??
          storeId ??
          undefined;
      }

      // 3) FCM 초기화 및 서버 등록
      if (accessToken && storeId) {
        try {
          const fcmToken = await initializePushNotifications();
          if (fcmToken) {
            await registerTokenWithServer(fcmToken, storeId);
          }
        } catch (fcme) {
          console.warn("[FCM] 등록/구독 실패(무시 가능):", fcme);
        }
      } else {
        console.warn("[FCM] accessToken 또는 storeId가 없어 FCM 등록을 건너뜁니다.");
      }

      toast.success("로그인 성공");
      navigate("/dashboard", { replace: true });
    } catch (err: unknown) {
      console.error(err);
      if (axios.isAxiosError(err)) {
        const status = err.response?.status;
        const code = (err.response?.data as any)?.code as string | undefined;
        const msg = (err.response?.data as any)?.message as string | undefined;

        if (code === "WRONG_PASSWORD") {
          setError("비밀번호가 틀립니다.");
          toast.error("비밀번호가 틀립니다.");
        } else if (code === "USER_NOT_FOUND" || status === 404) {
          setError("존재하지 않는 이메일입니다.");
          toast.error("존재하지 않는 이메일입니다.");
        } else if (status === 401) {
          setError("이메일 또는 비밀번호를 확인하세요.");
          toast.error("이메일 또는 비밀번호를 확인하세요.");
        } else {
          setError(msg ?? "로그인 중 오류가 발생했습니다.");
          toast.error(msg ?? "로그인 중 오류가 발생했습니다.");
        }
      } else {
        setError("네트워크 오류가 발생했습니다.");
        toast.error("네트워크 오류가 발생했습니다.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-light-gray flex items-center justify-center p-4">
      <Card className="w-full max-w-md p-8 bg-white rounded-xl shadow-lg">
        {/* Logo & Branding */}
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-kpi-orange rounded-xl flex items-center justify-center mx-auto mb-4">
            <Store className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            ToastLab ERP
          </h1>
          <p className="text-dark-gray">프랜차이즈 통합 관리 시스템</p>
        </div>

        {/* Login Form */}
        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <Label
              htmlFor="email"
              className="text-sm font-medium text-gray-700 mb-2 block"
            >
              이메일
            </Label>
            <div className="relative">
              <Mail className="w-5 h-5 text-dark-gray absolute left-3 top-1/2 -translate-y-1/2" />
              <Input
                id="email"
                type="email"
                placeholder="이메일을 입력하세요"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="pl-10 h-12 border-gray-300 rounded-lg"
                required
              />
            </div>
          </div>

          <div>
            <Label
              htmlFor="password"
              className="text-sm font-medium text-gray-700 mb-2 block"
            >
              비밀번호
            </Label>
            <div className="relative">
              <Lock className="w-5 h-5 text-dark-gray absolute left-3 top-1/2 -translate-y-1/2" />
              <Input
                id="password"
                type="password"
                placeholder="비밀번호를 입력하세요"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="pl-10 h-12 border-gray-300 rounded-lg"
                required
              />
            </div>
          </div>

          {error && (
            <div className="rounded-md bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          )}

          <div className="space-y-3 pt-4">
            <Button
              type="submit"
              className="w-full h-12 rounded-lg font-medium bg-kpi-red hover:bg-red-600 text-white"
            >
              로그인
            </Button>

            <Button
              type="button"
              variant="outline"
              onClick={() => navigate("/register")}
              className="w-full h-12 rounded-lg font-medium border-gray-300 text-gray-700 hover:bg-gray-50"
            >
              회원가입
            </Button>
          </div>
        </form>

        {/* Demo Accounts Info (가맹점만) */}
        <div className="mt-6 p-4 bg-blue-50 rounded-lg">
          <h4 className="text-sm font-medium text-blue-900 mb-2">데모 계정</h4>
          <div className="text-xs text-blue-800 space-y-1">
            <p>
              <strong>가맹점:</strong> store@franfriend.com / demo123
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="mt-8 pt-6 border-t border-gray-200 text-center">
          <p className="text-xs text-dark-gray">
            © 2024 FranFriend ERP. All rights reserved.
          </p>
          <div className="flex justify-center gap-4 mt-2">
            <a
              href="#"
              className="text-xs text-dark-gray hover:text-gray-900"
            >
              이용약관
            </a>
            <a
              href="#"
              className="text-xs text-dark-gray hover:text-gray-900"
            >
              개인정보처리방침
            </a>
            <a
              href="#"
              className="text-xs text-dark-gray hover:text-gray-900"
            >
              고객지원
            </a>
          </div>
        </div>
      </Card>
    </div>
  );
}
