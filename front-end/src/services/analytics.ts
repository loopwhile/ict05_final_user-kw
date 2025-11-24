type ViewBy = 'DAY' | 'MONTH';

export interface KpiRow {
  label: string;       // '2025-09-30' 같은 날짜/월 라벨
  sales: number;
  tx: number;
  upt: number;
  ads: number;
  aur: number;
}
export interface PageResp<T> {
  items: T[];
  nextCursor: string | null;
}

const base = '/user/api/analytics';

const commonInit = () => ({
  headers: { 'X-Timezone': Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC' }
});

export async function fetchKpiRows(params: {
  storeId: number;
  start: string;     // 'YYYY-MM-DD'
  end: string;       // 'YYYY-MM-DD'
  viewBy: ViewBy;
  size?: number;
  cursor?: string | null;
}): Promise<PageResp<KpiRow>> {
  const { storeId, start, end, viewBy, size = 50, cursor } = params;
  const qs = new URLSearchParams({
    storeId: String(storeId),
    start, end,
    viewBy,
    size: String(size)
  });
  if (cursor) qs.set('cursor', cursor);

  const res = await fetch(`${base}/kpi/rows?${qs.toString()}`, { ...commonInit() });
  if (!res.ok) throw new Error('Failed to load KPI rows');
  return res.json();
}

// (선택) 요약 카드용 엔드포인트가 있다면 사용
export interface KpiSummary {
  sales: number; tx: number; upt: number; ads: number; aur: number;
  // comp 계열이 있다면: wowPercentDay / wowPercentMonth 등
  wowPercent?: number; // 없으면 프런트에서 -100 사용
}
export async function fetchKpiSummary(params: {
  storeId: number; start: string; end: string;
}): Promise<KpiSummary> {
  const qs = new URLSearchParams({
    storeId: String(params.storeId),
    start: params.start,
    end: params.end
  });
  const res = await fetch(`${base}/kpi/summary?${qs.toString()}`, { ...commonInit() });
  if (!res.ok) throw new Error('Failed to load KPI summary');
  return res.json();
}
