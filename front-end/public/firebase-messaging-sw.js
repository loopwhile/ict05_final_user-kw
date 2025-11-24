/* public/firebase-messaging-sw.js
 * firebase-messaging-sw.js — 백그라운드 알림 표시 + 클릭 하드닝
 */
importScripts('https://www.gstatic.com/firebasejs/12.5.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/12.5.0/firebase-messaging-compat.js');

// TODO: 반드시 Firebase 콘솔의 Web App 설정값으로 확인/수정
firebase.initializeApp({
  apiKey: "AIzaSyA7m5jVdo-w7TBG6h6wW4h6mc5gbNjqYlU",
  authDomain: "ict05-final.firebaseapp.com",
  projectId: "ict05-final",
  storageBucket: "ict05-final.appspot.com", // ← 콘솔과 일치 확인
  messagingSenderId: "382264607725",
  appId: "1:382264607725:web:a8187a0a64f88667045de4",
  measurementId: "G-VLKM1XP7ZG"
});

const messaging = firebase.messaging();

// 동일 출처만 허용 + /user 베이스 안전 정규화
function normalizeLink(raw) {
  const origin = self.location.origin;
  const base = origin + '/user';

  if (!raw || typeof raw !== 'string') return base;

  // 절대 URL 시도 → 동일 출처만 허용
  try {
    const u = new URL(raw, base); // raw가 상대경로여도 안전
    if (u.origin !== origin) {
      // 외부 링크는 차단하고 /user 홈으로
      return base;
    }
    // /user 중복 방지: /user로 시작하면 그대로, 아니면 /user 접두
    if (u.pathname.startsWith('/user')) {
      return u.toString();
    }
    // 동일 출처, /user 미포함 → /user 접두로 합성
    const merged = new URL('/user' + (u.pathname.startsWith('/') ? '' : '/') + u.pathname, origin);
    merged.search = u.search;
    merged.hash = u.hash;
    return merged.toString();
  } catch (e) {
    // URL 파싱 실패 시 기본값
    return base;
  }
}

messaging.onBackgroundMessage((payload) => {
  const title = payload?.notification?.title || payload?.data?.title || '알림';
  const body  = payload?.notification?.body  || payload?.data?.body  || '';
  const link  = normalizeLink(payload?.data?.link);

  const tagBase = payload?.data?.type ? ('store-fcm-' + payload.data.type) : 'store-fcm';
  // 메시지 고유성이 필요하면 아래처럼 넓혀도 됨:
  // const tag = `${tagBase}-${Date.now()}`;
  const tag = tagBase;

  self.registration.showNotification(title, {
    body,
    // 정적 파일 경로 점검: public/[user/]images/...
    icon: '/images/fcm/toastlab.png',
    badge: '/images/fcm/badge-72.png',
    data: { link },
    tag
  });
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  const link = normalizeLink(event.notification?.data?.link);

  event.waitUntil((async () => {
    const all = await clients.matchAll({ type: 'window', includeUncontrolled: true });

    // 이미 /user가 열린 탭이 있으면 포커스+내비게이션
    const existing = all.find(c => c.url && c.url.startsWith(self.location.origin + '/user'));
    if (existing) {
      await existing.focus();
      try { await existing.navigate(link); } catch (_) { /* 일부 브라우저에서 실패 가능 */ }
      return;
    }
    // 없으면 새 탭
    await clients.openWindow(link);
  })());
});

// (선택) SW 업데이트 빠르게 반영
// self.addEventListener('install', () => self.skipWaiting());
// self.addEventListener('activate', (event) => event.waitUntil(clients.claim()));
