
import { Capacitor } from '@capacitor/core';
import {
  PushNotifications,
  Token,
  PushNotificationSchema,
  ActionPerformed,
} from '@capacitor/push-notifications';
import { toast } from 'sonner';
import { NavigateFunction } from 'react-router-dom';

import { requestFcmToken as requestFcmTokenWeb, deleteFcmToken as deleteFcmTokenWeb, onForegroundMessage } from './firebase';
import api from './authApi'; // 백엔드 API 호출용

const isNative = Capacitor.getPlatform() !== 'web';

// --- 공통 로직: 토큰 저장 ---
const LOCAL_TOKEN_KEY = 'fcm_token';
const getSavedFcmToken = () => localStorage.getItem(LOCAL_TOKEN_KEY);
const saveFcmToken = (token: string) => {
  const prev = getSavedFcmToken();
  if (prev !== token) {
    console.log('[FCM] token changed (prev -> new)', prev, token);
    localStorage.setItem(LOCAL_TOKEN_KEY, token);
  } else {
    console.log('[FCM] token unchanged');
  }
};
const removeFcmToken = () => localStorage.removeItem(LOCAL_TOKEN_KEY);


// --- 네이티브(Android/iOS) FCM 로직 ---

const registerPushNative = async (): Promise<string | null> => {
  // 1. 권한 확인
  let permStatus = await PushNotifications.checkPermissions();

  // 2. 권한 요청
  if (permStatus.receive === 'prompt') {
    permStatus = await PushNotifications.requestPermissions();
  }

  if (permStatus.receive !== 'granted') {
    toast.warning('푸시 알림 권한이 거부되었습니다.');
    return null;
  }

  // 3. FCM 등록
  await PushNotifications.register();

  // 4. 토큰 수신 대기 (리스너를 통해 받음)
  return new Promise((resolve) => {
    PushNotifications.addListener('registration', (token: Token) => {
      console.log('[FCM] Registration token: ', token.value);
      saveFcmToken(token.value);
      resolve(token.value);
    });
    PushNotifications.addListener('registrationError', (err: any) => {
      console.error('[FCM] Registration error: ', err);
      toast.error('FCM 등록에 실패했습니다.');
      resolve(null);
    });
  });
};

/**
 * 네이티브 환경에서 알림 리스너를 설정합니다.
 * 앱의 최상위 컴포넌트(예: App.tsx)에서 한 번만 호출해야 합니다.
 */
export const addNotificationListenersNative = (navigate: NavigateFunction) => {
  // 포그라운드 알림 수신
  PushNotifications.addListener('pushNotificationReceived', (notification: PushNotificationSchema) => {
    console.log('[FCM] Push received: ', notification);
    toast.info(notification.title || '새 알림', {
      description: notification.body,
      duration: 5000,
      action: {
        label: '보기',
        onClick: () => {
          if (notification.data?.link) {
            navigate(notification.data.link);
          }
        },
      },
    });
  });

  // 알림 탭 액션
  PushNotifications.addListener('pushNotificationActionPerformed', (notification: ActionPerformed) => {
    const link = notification.notification.data?.link;
    console.log('[FCM] Push action performed: ', notification);
    if (link) {
      navigate(link);
    }
  });
};


// --- 웹 FCM 로직 ---

const registerPushWeb = async (): Promise<string | null> => {
  const token = await requestFcmTokenWeb();
  if (token) {
    saveFcmToken(token);
  }
  return token;
};


// --- 통합 인터페이스 ---

/**
 * 플랫폼에 맞는 푸시 알림 서비스를 초기화하고 토큰을 반환합니다.
 * 로그인 성공 시 호출합니다.
 */
export const initializePushNotifications = async (): Promise<string | null> => {
  try {
    if (isNative) {
      return await registerPushNative();
    } else {
      return await registerPushWeb();
    }
  } catch (e) {
    console.error('[FCM] initializePushNotifications failed', e);
    return null;
  }
};

/**
 * 서버에 FCM 토큰을 등록/업데이트합니다.
 */
export const registerTokenWithServer = async (token: string, storeId: number | string) => {
  try {
    // 1. 토큰 등록
    await api.post('/fcm/token', {
      token: token,
      platform: isNative ? Capacitor.getPlatform().toUpperCase() : 'WEB',
      deviceId: isNative ? (await PushNotifications.getId()).uuid : navigator.userAgent.slice(0, 120),
    });
    console.log('[FCM] Token registered with server.');

    // 2. 토픽 구독 (기존 로직 재사용)
    const prefRes = await api.get("/fcm/pref/me");
    const prefs = prefRes.data || {};
    const baseUrl = "/fcm/topic/subscribe";
    const encToken = encodeURIComponent(token);

    const topicPromises = [
      // 공지사항 (항상 구독)
      api.post(`${baseUrl}?token=${encToken}&topic=notice`),
      // 점주별 토픽
      prefs.invLow !== false && api.post(`${baseUrl}?token=${encToken}&topic=inv-low-${storeId}`),
      prefs.expireSoon !== false && api.post(`${baseUrl}?token=${encToken}&topic=expire-soon-${storeId}`),
      prefs.orderNew !== false && api.post(`${baseUrl}?token=${encToken}&topic=order-new-${storeId}`),
    ].filter(Boolean);

    await Promise.all(topicPromises);
    console.log('[FCM] Subscribed to topics based on preferences.');

  } catch (e) {
    console.warn('[FCM] registerTokenWithServer failed (ignorable)', e);
  }
};


/**
 * 플랫폼에 맞는 푸시 알림 서비스를 정리합니다.
 * 로그아웃 시 호출합니다.
 */
export const cleanupPushNotifications = async () => {
  const token = getSavedFcmToken();
  if (!token) return;

  try {
    // 서버에서 토큰 삭제
    await api.post('/fcm/token/revoke', { token });
    console.log('[FCM] Token revoked from server.');
  } catch (e) {
    console.error('[FCM] Server token revoke failed', e);
  } finally {
    // 클라이언트에서 토큰 삭제
    if (isNative) {
      // 네이티브는 별도 클라이언트 토큰 삭제 API가 복잡하므로 로컬 저장소만 지웁니다.
      // 어차피 새 로그인 시 새 토큰을 받습니다.
      removeFcmToken();
    } else {
      try {
        await deleteFcmTokenWeb(); // firebase/messaging의 deleteToken 호출
      } finally {
        removeFcmToken();
      }
    }
  }
};

/**
 * 웹 환경에서 포그라운드 메시지 리스너를 설정합니다.
 * 네이티브는 addNotificationListenersNative를 사용합니다.
 */
export const addForegroundListenerWeb = onForegroundMessage;
