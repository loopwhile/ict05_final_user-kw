// src/lib/authApi.ts
import axios from "axios";

// ✅ 토큰 저장 위치 (예시: localStorage)
const ACCESS_TOKEN_KEY = "accessToken";
const REFRESH_TOKEN_KEY = "refreshToken";

// ✅ axios 인스턴스 생성
const api = axios.create({
  baseURL: import.meta.env.VITE_BACKEND_API_BASE_URL,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

// 1) 앱 부팅 시 기본 Authorization 헤더 세팅
const t0 = localStorage.getItem(ACCESS_TOKEN_KEY);
if (t0) api.defaults.headers.common.Authorization = `Bearer ${t0}`;

// ✅ 요청 인터셉터 : accessToken 자동 첨부
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let waiters: Array<(newAccess: string) => void> = [];

// ✅ 응답 인터셉터 : 401 발생 시 자동 토큰 재발급
api.interceptors.response.use(
  (response) => response, // 정상응답은 그대로 통과
  async (error) => {
    const originalRequest = error?.config || {};
    const status = error?.response?.status as number | undefined;

    // ✅ 지금 페이지가 로그인 화면인지 체크 (이전 버전에서 가져온 로직)
    const onLoginPage =
      typeof window !== "undefined" && window.location.pathname === "/login";

    // ✅ 로그인 화면에서 나는 401 은 리프레시/리다이렉트 하지 않고 그냥 에러만 던지기
    if (status === 401 && onLoginPage) {
      return Promise.reject(error);
    }

    // ❌ accessToken 만료 시 처리
    if (status === 401 && !originalRequest._retry) {
      (originalRequest as any)._retry = true; // 무한루프 방지

      // 이미 다른 요청이 리프레시 중이면 큐에 대기시킴
      if (isRefreshing) {
        return new Promise((resolve) => {
          waiters.push((newAccess) => {
            originalRequest.headers = {
              ...(originalRequest.headers || {}),
              Authorization: `Bearer ${newAccess}`,
            };
            resolve(api(originalRequest)); // 새 토큰으로 원요청 재시도
          });
        });
      }

      // 내가 리프레시 담당
      isRefreshing = true;

      try {
        // refreshToken 가져오기
        const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
        if (!refreshToken) throw new Error("No refresh token");

        // 🔄 refresh 요청
        const res = await api.post("/jwt/refresh", null, {
          headers: { "X-Refresh-Token": refreshToken },
        });

        const newAccessToken: string = res.data.accessToken;
        const newRefreshToken: string | undefined = res.data.refreshToken;

        if (!newAccessToken) throw new Error("No access token");

        // 새 토큰 저장
        localStorage.setItem(ACCESS_TOKEN_KEY, newAccessToken);
        if (newRefreshToken) {
          localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken);
        }

        // 기본 헤더도 갱신
        api.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;

        // 대기 중 요청들 재개
        if (waiters.length) {
          waiters.forEach((fn) => fn(newAccessToken));
          waiters = [];
        }

        // Authorization 헤더 갱신 후 재요청
        originalRequest.headers = {
          ...(originalRequest.headers || {}),
          Authorization: `Bearer ${newAccessToken}`,
        };

        return api(originalRequest);
      } catch (refreshError) {
        console.error("🔒 Token refresh failed:", refreshError);

        // 로그인 만료 처리
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);

        // ✅ 이미 로그인 페이지면 여기서도 리다이렉트 안 함
        if (!onLoginPage) {
          window.location.href = "/login";
        }

        return Promise.reject(error);
      } finally {
        isRefreshing = false;
      }
    }

    // 위 조건에 해당하지 않으면 그냥 에러 전달
    return Promise.reject(error);
  }
);

export default api;
