import React, { useEffect, useMemo, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { AlertTriangle, CalendarIcon, Download, Star, ThumbsUp, TrendingUp } from 'lucide-react';
import { Popover, PopoverContent, PopoverTrigger } from '../../../components/ui/popover';
import { Calendar } from '../../../components/ui/calendar';
import { fmtMoneyInt, fmtPercent1, tz } from '../../../lib/format';
import api from '../../../lib/authApi';
import { KPICard } from '../../Common/KPICard';

// ====== 로컬 타입(파일 단독 사용 가능) ======
type ViewBy = 'DAY' | 'MONTH';

type PageResp<T> = {
  items: T[];
  nextCursor: string | null;
};

// 상단 요약 카드용
type MenuTopMenu = {
  menuId: number;
  menuName: string;
  quantity: number;
};

type MenuCategoryRank = {
  categoryId: number;
  categoryName: string;
  sales: number;
};

type MenuSalesContribution = {
  menuId: number;
  menuName: string;
  sales: number;
  contributionRate: number; // %
};

type MenuLowPerform = {
  menuId: number;
  menuName: string;
  sales: number;
  quantity: number;
};

type MenuSummary = {
  topMenusByQty: MenuTopMenu[];
  topCategoriesBySales: MenuCategoryRank[];
  topMenusBySalesContribution: MenuSalesContribution[];
  lowPerformMenus: MenuLowPerform[];
};

// 테이블 행 (일별)
type MenuDailyRow = {
  orderDate: string;    // YYYY-MM-DD
  categoryName: string;
  menuName: string;
  quantity: number;
  sales: number;
  orderCount: number;
};

// 테이블 행 (월별)
type MenuMonthlyRow = {
  yearMonth: string;    // YYYY-MM
  menuName: string;
  categoryName: string;
  quantity: number;
  sales: number;
  orderCount: number;
};

type MenuRow = MenuDailyRow | MenuMonthlyRow;

const PAGE_SIZE_OPTIONS = [20, 40, 60, 80, 100];

function formatDateLocal(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export default function MenuReport() {

  const today = new Date();
  const [end, setEnd] = useState<Date>(() => today);
  const [start, setStart] = useState<Date>(() => {
    const d = new Date(today);
    d.setDate(d.getDate() - 6); // 최근 7일
    return d;
  });

  const [viewBy, setViewBy] = useState<ViewBy>('DAY');
  const [pageSize, setPageSize] = useState<number>(20);

  const [summary, setSummary] = useState<MenuSummary | null>(null);
  const [rows, setRows] = useState<MenuRow[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const startStr = useMemo(() => formatDateLocal(start), [start]);
  const endStr   = useMemo(() => formatDateLocal(end),   [end]);

  // ==========================
  // 상단 카드 요약 로드
  // ==========================
  async function loadSummary() {
    try {
      const { data } = await api.get<MenuSummary>('/api/analytics/menus/summary');
      setSummary(data);
    } catch {
      setSummary(null);
    }
  }

  // ==========================
  // 테이블 첫 페이지 로드
  // ==========================
  async function loadFirst() {
    setLoading(true);
    let alive = true;
    try {
      const url =
        viewBy === 'DAY'
          ? '/api/analytics/menus/day-rows'
          : '/api/analytics/menus/month-rows';

      // 표 + 상단 요약 동시 로드
      const [rowsRes, summaryRes] = await Promise.all([
        api.get<PageResp<MenuRow>>(url, {
          params: {
            start: startStr,
            end: endStr,
            size: pageSize,
            cursor: null,
          },
        }),
        api.get<MenuSummary>('/api/analytics/menus/summary'),
      ]);
      if (!alive) return;
      setRows(rowsRes.data.items);
      setCursor(rowsRes.data.nextCursor);
      setSummary(summaryRes.data);
    } catch {
      if (!alive) return;
      setRows([]);
      setCursor(null);
      setSummary(null);
    } finally {
      if (alive) setLoading(false);
    }
    return () => { alive = false; };
  }

  // ==========================
  // 테이블 추가 로드(더보기)
  // ==========================
  async function loadMore() {
    if (!cursor) return;
    setLoading(true);
    let alive = true;
    try {
      const url =
        viewBy === 'DAY'
          ? '/api/analytics/menus/day-rows'
          : '/api/analytics/menus/month-rows';

      const { data } = await api.get<PageResp<MenuRow>>(url, {
        params: {
          start: startStr,
          end: endStr,
          size: pageSize,
          cursor,
        },
      });

      if (!alive) return;
      setRows((prev) => [...prev, ...data.items]);
      setCursor(data.nextCursor);
    } finally {
      if (alive) setLoading(false);
    }
    return () => { alive = false; };
  }

  // ==========================
  // PDF 다운로드
  // ==========================
  async function handleDownloadReport() {
    try {
      setDownloading(true);
      const { data } = await api.get<Blob>(
        '/api/analytics/menus/report',
        {
          params: {
            start: startStr,
            end: endStr,
            viewBy,
          },
          responseType: 'blob',
        } as any
      );

      const url = window.URL.createObjectURL(data);

      const link = document.createElement('a');
      const viewLabel = viewBy === 'DAY' ? 'day' : 'month';
      link.href = url;
      link.download = `menu-report_${viewLabel}_${startStr}_${endStr}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      console.error(e);
      alert('메뉴 분석 리포트 다운로드 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
    } finally {
      setDownloading(false);
    }
  }


  useEffect(() => {
    loadFirst();
  }, []);

  return (
    <div className="space-y-6">
      {/* 헤더 + 필터 */}
      <div className="flex flex-wrap gap-2 items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">메뉴 분석</h1>
          <p className="text-sm text-gray-600">
            타임존: {tz} / 상단 카드는 이번달 1일 ~ 어제 기준(MTD), 테이블은 선택한 기간 기준
          </p>
        </div>
        <div className="flex flex-wrap gap-2 items-center justify-end">
          {/* 시작일 */}
          <Popover>
            <PopoverTrigger asChild>
              <Button variant="outline">
                <CalendarIcon className="w-4 h-4 mr-2" />
                시작일: {start.toLocaleDateString('ko-KR')}
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-auto p-0">
              <Calendar
                mode="single"
                selected={start}
                onSelect={(d: any) => d && setStart(d)}
                initialFocus
              />
            </PopoverContent>
          </Popover>

          {/* 종료일 */}
          <Popover>
            <PopoverTrigger asChild>
              <Button variant="outline">
                <CalendarIcon className="w-4 h-4 mr-2" />
                종료일: {end.toLocaleDateString('ko-KR')}
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-auto p-0">
              <Calendar
                mode="single"
                selected={end}
                onSelect={(d: any) => d && setEnd(d)}
                initialFocus
              />
            </PopoverContent>
          </Popover>

          {/* 일별/월별 토글 */}
          <div className="flex rounded-md border bg-gray-50 overflow-hidden">
            <button
              className={`px-3 py-2 text-sm font-medium ${
                viewBy === 'DAY'
                  ? 'bg-kpi-red text-white'
                  : 'text-gray-700 hover:bg-white'
              }`}
              onClick={() => {
                if (viewBy !== 'DAY') {
                  setViewBy('DAY');
                  setRows([]);
                  setCursor(null);
                }
              }}
            >
              일별
            </button>
            <button
              className={`px-3 py-2 text-sm font-medium ${
                viewBy === 'MONTH'
                  ? 'bg-kpi-red text-white'
                  : 'text-gray-700 hover:bg-white'
              }`}
              onClick={() => {
                if (viewBy !== 'MONTH') {
                  setViewBy('MONTH');
                  setRows([]);
                  setCursor(null);
                }
              }}
            >
              월별
            </button>
          </div>

          {/* 출력개수 */}
          <div className="flex items-center gap-2">
            <span className="text-sm text-gray-600">출력개수</span>
            <select
              value={pageSize}
              onChange={(e) => setPageSize(Number(e.target.value))}
              className="h-9 rounded-md border px-2 text-sm bg-white"
            >
              {PAGE_SIZE_OPTIONS.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}개
                </option>
              ))}
            </select>
          </div>

          {/* 조회 버튼 */}
          <Button onClick={loadFirst} disabled={loading}>
            {loading ? '조회 중…' : '조회'}
          </Button>

          {/* 리포트 다운로드(향후 PDF/엑셀) */}
          <Button onClick={handleDownloadReport} disabled={downloading}>
            <Download className="w-4 h-4 mr-2" />
            {downloading ? '다운로드 중…' : '리포트 다운로드'}
          </Button>
        </div>
      </div>

      {/* 상단 요약 카드 4개 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <KPICard
          title="판매수량 TOP3 메뉴"
          value={
            <div className="text-sm space-y-1">
              {summary?.topMenusByQty?.length
                ? summary.topMenusByQty.map((m) => (
                    <div key={m.menuId} className="flex justify-between">
                      <span className="text-gray-700">{m.menuName}</span>
                      <span className="font-semibold">{m.quantity.toLocaleString()}개</span>
                    </div>
                  ))
                : <div className="text-white-400">데이터 없음</div>
              }
            </div>
          }
          icon={ThumbsUp}
          color="orange"
        />

        <KPICard
          title="매출 TOP3 카테고리"
          value={
            <div className="text-sm space-y-1">
              {summary?.topCategoriesBySales?.length
                ? summary.topCategoriesBySales.map((c) => (
                    <div key={c.categoryId} className="flex justify-between">
                      <span className="text-gray-700">{c.categoryName}</span>
                      <span className="font-semibold">₩{fmtMoneyInt(c.sales)}</span>
                    </div>
                  ))
                : <div className="text-white-400">데이터 없음</div>
              }
            </div>
          }
          icon={TrendingUp}
          color="red"
        />

        <KPICard
          title="매출기여도 TOP3 메뉴"
          value={
            <div className="text-sm space-y-1">
              {summary?.topMenusBySalesContribution?.length
                ? summary.topMenusBySalesContribution.map((m) => (
                    <div key={m.menuId} className="flex justify-between">
                      <span className="text-gray-700">{m.menuName}</span>
                      <span className="font-semibold">{fmtPercent1(m.contributionRate)}</span>
                    </div>
                  ))
                : <div className="text-white-400">데이터 없음</div>
              }
            </div>
          }
          icon={Star}
          color="green"
        />

        <KPICard
          title="저성과 메뉴"
          value={
            <div className="text-sm space-y-1">
              {summary?.lowPerformMenus?.length
                ? summary.lowPerformMenus.map((m) => (
                    <div key={m.menuId} className="flex justify-between">
                      <span className="text-gray-700">{m.menuName}</span>
                      <span className="font-semibold">
                        ₩{fmtMoneyInt(m.sales)} / {m.quantity.toLocaleString()}개
                      </span>
                    </div>
                  ))
                : <div className="text-white-400">데이터 없음</div>
              }
            </div>
          }
          icon={AlertTriangle}
          color="purple"
        />
      </div>

      {/* ===== 테이블 영역 ===== */}
      <Card className="bg-white rounded-xl shadow-sm overflow-hidden">
        <CardHeader className="px-6 py-4 border-b bg-light-gray">
          <CardTitle className="text-base font-semibold text-gray-900">
            {viewBy === 'DAY'
              ? '메뉴 분석 (일별 / 메뉴 단위)'
              : '메뉴 분석 (월별 집계 / 메뉴 단위)'}
            {' '}({startStr} ~ {endStr})
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full">
              {viewBy === 'DAY' ? (
                <>
                  <thead className="bg-light-gray border-b">
                    <tr>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">날짜</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">카테고리</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">메뉴</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">판매수량</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">매출액</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">주문수</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {(rows as MenuDailyRow[]).map((r, i) => (
                      <tr key={i} className="hover:bg-gray-50">
                        <td className="px-6 py-3 text-center text-sm text-gray-900">{r.orderDate}</td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-left">{r.categoryName}</td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-left">{r.menuName}</td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          {r.quantity.toLocaleString()}
                        </td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          ₩{fmtMoneyInt(r.sales)}
                        </td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          {r.orderCount.toLocaleString()}
                        </td>
                      </tr>
                    ))}

                    {rows.length === 0 && (
                      <tr>
                        <td colSpan={6} className="px-6 py-8 text-center text-sm text-dark-gray">
                          데이터가 없습니다.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </>
              ) : (
                <>
                  <thead className="bg-light-gray border-b">
                    <tr>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">월</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">메뉴</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">카테고리</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">판매수량</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">매출액</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">주문수</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {(rows as MenuMonthlyRow[]).map((r, i) => (
                      <tr key={i} className="hover:bg-gray-50">
                        <td className="px-6 py-3 text-center text-sm text-gray-900">{r.yearMonth}</td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-center">{r.menuName}</td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-center">{r.categoryName}</td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          {r.quantity.toLocaleString()}
                        </td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          ₩{fmtMoneyInt(r.sales)}
                        </td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          {r.orderCount.toLocaleString()}
                        </td>
                      </tr>
                    ))}

                    {rows.length === 0 && (
                      <tr>
                        <td colSpan={7} className="px-6 py-8 text-center text-sm text-dark-gray">
                          데이터가 없습니다.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </>
              )}
            </table>
          </div>

          {/* 더보기 */}
          {cursor && (
            <div className="px-6 py-4 border-t bg-light-gray flex justify-center">
              <Button onClick={loadMore} disabled={loading}>
                {loading ? '불러오는 중…' : '더보기'}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}