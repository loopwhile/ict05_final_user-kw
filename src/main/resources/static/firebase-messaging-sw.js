/* firebase-messaging-sw.js — 백그라운드 알림 표시 + 클릭 하드닝 */
importScripts('https://www.gstatic.com/firebasejs/12.5.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/12.5.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyA7m5jVdo-w7TBG6h6wW4h6mc5gbNjqYlU",
  authDomain: "ict05-final.firebaseapp.com",
  projectId: "ict05-final",
  storageBucket: "ict05-final.firebasestorage.app",
  messagingSenderId: "382264607725",
  appId: "1:382264607725:web:a8187a0a64f88667045de4",
  measurementId: "G-VLKM1XP7ZG"
});

const messaging = firebase.messaging();

function normalizeLink(raw) {
  const origin = self.location.origin;
  const base = origin + '/user';
  if (!raw) return base;
  if (raw.startsWith('http://') || raw.startsWith('https://')) return raw;
  if (raw.startsWith('/')) return origin + raw;
  return base.replace(/\/+$/,'') + '/' + raw.replace(/^\/+/, '');
}

messaging.onBackgroundMessage((payload) => {
  const title = payload?.notification?.title || payload?.data?.title || '알림';
  const body  = payload?.notification?.body  || payload?.data?.body  || '';
  const link  = normalizeLink(payload?.data?.link);
  self.registration.showNotification(title, {
    body,
    icon: '/user/images/fcm/toastlab.png',
    badge: '/user/images/fcm/badge-72.png',
    data: { link },
    tag: payload?.data?.type ? ('store-fcm-' + payload.data.type) : 'store-fcm'
  });
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const link = normalizeLink(event.notification?.data?.link);

  event.waitUntil((async () => {
    const clientList = await clients.matchAll({ type: 'window', includeUncontrolled: true });
    // /user 이미 열려있으면 포커스 후 navigate 시도
    const existing = clientList.find(c => c.url.includes('/user'));
    if (existing) {
      await existing.focus();
      try { existing.navigate(link); } catch (_) {}
      return;
    }
    await clients.openWindow(link);
  })());
});
