// env에서 BASE 읽기 (CRA)
const BASE = process.env.REACT_APP_BACKEND_API_BASE_URL;

// 상대경로면 BASE를 붙이고, 절대경로면 그대로 사용
function buildUrl(u) {
  if (!u) return BASE;
  const isAbsolute = /^https?:\/\//i.test(u);
  if (isAbsolute) return u;
  const slash = u.startsWith('/') ? '' : '/';
  return `${BASE}${slash}${u}`;
}

// AccessToken 만료시 Refreshing
export async function refreshAccessToken() {
  if (!BASE) {
    throw new Error('REACT_APP_BACKEND_API_BASE_URL 이 정의되지 않았습니다');
  }

  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) throw new Error('RefreshToken이 없습니다.');

  const res = await fetch(buildUrl('/jwt/refresh'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) throw new Error('AccessToken 갱신 실패');

  const data = await res.json();
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  return data.accessToken;
}

// AccessToken과 함께 fetch
export async function fetchWithAccess(url, options = {}) {
  if (!BASE) {
    throw new Error('REACT_APP_BACKEND_API_BASE_URL 이 정의되지 않았습니다');
  }

  // 원본 options를 변형하지 않도록 얕은 복사
  const opts = { ...options, headers: { ...(options.headers || {}) } };

  // 토큰 부착
  let accessToken = localStorage.getItem('accessToken');
  if (accessToken) {
    opts.headers['Authorization'] = `Bearer ${accessToken}`;
  }

  // 상대경로면 BASE 붙이기
  let target = buildUrl(url);

  // 1차 요청
  let res = await fetch(target, opts);

  // 401이면 한번만 갱신 후 재시도
  if (res.status === 401) {
    try {
      accessToken = await refreshAccessToken();
      opts.headers['Authorization'] = `Bearer ${accessToken}`;
      res = await fetch(target, opts);
    } catch (e) {
      // 갱신 실패 시 로그아웃 처리
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      window.location.href = '/login';
      throw e;
    }
  }

  if (!res.ok) {
    throw new Error(`HTTP 오류: ${res.status}`);
  }
  return res;
}
