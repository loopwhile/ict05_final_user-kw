// firebase.ts — Firebase 초기화 & FCM 헬퍼

import { initializeApp } from 'firebase/app';
import {
  getAnalytics,
  isSupported as analyticsSupported,
} from 'firebase/analytics';
import {
  getMessaging,
  isSupported as messagingSupported,
  getToken,
  deleteToken,
  onMessage,
  Messaging,
} from 'firebase/messaging';

// 1) Firebase 구성 — Vite의 환경변수 사용
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID,
};

export const firebaseApp = initializeApp(firebaseConfig);

// (옵션) GA 사용 시
analyticsSupported().then((ok) => {
  if (ok) {
    getAnalytics(firebaseApp);
  }
});

// 2) Messaging 인스턴스 캐싱
let messagingPromise: Promise<Messaging | null> | null = null;

export async function getMessagingIfSupported(): Promise<Messaging | null> {
  if (!messagingPromise) {
    messagingPromise = (async () => {
      const supported = await messagingSupported();
      if (!supported) {
        console.warn('[FCM] messaging not supported in this browser');
        return null;
      }

      // SW 등록 (중복 등록 방지: 브라우저가 알아서 같은 경로는 재사용)
      if (!('serviceWorker' in navigator)) {
        console.warn('[FCM] serviceWorker not available');
        return null;
      }

      await navigator.serviceWorker.register('/firebase-messaging-sw.js');
      const messaging = getMessaging(firebaseApp);
      return messaging;
    })().catch((err) => {
      console.error('[FCM] getMessagingIfSupported error', err);
      return null;
    });
  }
  return messagingPromise;
}

// (추가) 권한 요청
async function ensureNotificationPermission(): Promise<boolean> {
  if (typeof Notification === 'undefined') {
    console.warn('[FCM] Notification API not supported');
    return false;
  }
  if (Notification.permission === 'granted') return true;
  const perm = await Notification.requestPermission();
  return perm === 'granted';
}

// (추가) 로컬 키 상수
const LOCAL_TOKEN_KEY = 'fcm_token';
const getSavedFcmToken = () => localStorage.getItem(LOCAL_TOKEN_KEY);
const saveFcmToken = (t: string) => localStorage.setItem(LOCAL_TOKEN_KEY, t);

// 3) 토큰 요청 + localStorage 저장
export async function requestFcmToken(): Promise<string | null> {
  const permOk = await ensureNotificationPermission();
  if (!permOk) {
    console.warn('[FCM] notification permission denied');
    return null;
  }

  const messaging = await getMessagingIfSupported();
  if (!messaging) return null;

  try {
    const vapidKey =
      import.meta.env.VITE_FIREBASE_VAPID_KEY ||
      import.meta.env.VITE_FIREBASE_WEB_VAPID_KEY ||
      import.meta.env.VITE_FIREBASE_PUBLIC_VAPID_KEY;

    if (!vapidKey) {
      console.warn('[FCM] VAPID key is not configured');
    }

    const token = await getToken(messaging, { vapidKey });
    if (!token) {
      console.warn('[FCM] getToken returned null');
      return null;
    }

    const prev = getSavedFcmToken();
    if (prev !== token) {
      console.log('[FCM] token changed (prev -> new)', prev, token);
      saveFcmToken(token);
    } else {
      console.log('[FCM] token unchanged');
    }
    return token;
  } catch (e) {
    console.error('[FCM] getToken failed', e);
    return null;
  }
}

// 4) 토큰 삭제 + localStorage 정리
export async function deleteFcmToken(): Promise<void> {
  const messaging = await getMessagingIfSupported();
  if (!messaging) return;

  const currentToken = localStorage.getItem('fcm_token');
  if (!currentToken) {
    console.log('[FCM] no fcm_token in localStorage');
    return;
  }

  try {
    const ok = await deleteToken(messaging);
    if (ok) {
      console.log('[FCM] token deleted from client');
      localStorage.removeItem('fcm_token');
    } else {
      console.warn('[FCM] deleteToken returned false');
    }
  } catch (e) {
    console.error('[FCM] deleteToken failed', e);
  }
}

// 5) 포그라운드 메시지 리스너 (FcmForegroundListener에서 사용)
export function onForegroundMessage(handler: (payload: any) => void) {
  getMessagingIfSupported().then((messaging) => {
    if (!messaging) return;
    onMessage(messaging, handler);
  });
}
