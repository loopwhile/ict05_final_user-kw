/// <reference types="vite/client" />

// 선택: 내가 쓰는 환경변수 키를 명시해두면 자동완성/타입체크가 돼요.
interface ImportMetaEnv {
  readonly VITE_BACKEND_API_BASE_URL: string;
  // 다른 VITE_ 키가 있으면 여기에 추가
  readonly VITE_SYNC_SHARED_SECRET: string;
  readonly VITE_FIREBASE_API_KEY: string;
  readonly VITE_FIREBASE_AUTH_DOMAIN: string;
  readonly VITE_FIREBASE_PROJECT_ID: string;
  readonly VITE_FIREBASE_STORAGE_BUCKET: string;
  readonly VITE_FIREBASE_MESSAGING_SENDER_ID: string;
  readonly VITE_FIREBASE_APP_ID: string;
  readonly VITE_FIREBASE_MEASUREMENT_ID: string;
  readonly VITE_FIREBASE_VAPID_KEY: string;
}
interface ImportMeta {
  readonly env: ImportMetaEnv;
  
}
