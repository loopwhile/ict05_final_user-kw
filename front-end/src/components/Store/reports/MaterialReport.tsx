import React, { useEffect, useMemo, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { AlertTriangle, CalendarIcon, Clock, Download, Package, TrendingDown, TrendingUp } from 'lucide-react';
import { Popover, PopoverContent, PopoverTrigger } from '../../../components/ui/popover';
import { Calendar } from '../../../components/ui/calendar';
import { displayInbound, fmtMoneyInt, fmtPercent1, tz } from '../../../lib/format';
import api from '../../../lib/authApi';
import { KPICard } from '../../Common/KPICard';

// ==========================
// 타입 정의
// ==========================
type ViewBy = 'DAY' | 'MONTH';

type PageResp<T> = {
  items: T[];
  nextCursor: string | null;
};

// 상단 요약 카드용
type MaterialTopItem = {
  materialId: number;
  materialName: string;
  unitName: string;
  usedQuantity: number;
  cost: number;
};

type MaterialSummary = {
  topByUsage: MaterialTopItem[];
  topByCost: MaterialTopItem[];
  currentCostRate: number;  // 0~100 (소수 1자리)
  prevCostRate: number;     // 0~100 (소수 1자리)
  costRateDiff: number;     // percentage point
  lowStockCount: number;
  expireSoonCount: number;
};

// 테이블 행 타입
type MaterialDailyRow = {
  useDate: string;
  materialName: string;
  usedQuantity: number;
  unitName: string;
  cost: number;
  salesShare: number;
  lastInboundDate?: string | null;
};

type MaterialMonthlyRow = {
  yearMonth: string;
  materialName: string;
  usedQuantity: number;
  cost: number;
  costRate: number;
  lastInboundMonth?: string | null;
};

type MaterialRow = MaterialDailyRow | MaterialMonthlyRow;

const PAGE_SIZE_OPTIONS = [20, 40, 60, 80, 100];

// ==========================
// 유틸 함수
// ==========================
function formatDateLocal(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}


// ==========================
// 메인 컴포넌트
// ==========================
export default function MaterialReport() {

  const today = new Date();
  const [end, setEnd] = useState<Date>(() => today);
  const [start, setStart] = useState<Date>(() => {
    const d = new Date(today);
    d.setDate(d.getDate() - 6); // 최근 7일
    return d;
  });

  const [viewBy, setViewBy] = useState<ViewBy>('DAY');
  const [pageSize, setPageSize] = useState<number>(20);

  const [summary, setSummary] = useState<MaterialSummary | null>(null);
  const [rows, setRows] = useState<MaterialRow[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const startStr = useMemo(() => formatDateLocal(start), [start]);
  const endStr   = useMemo(() => formatDateLocal(end),   [end]);

  // ==========================
  // 상단 카드 요약 로드 (주문분석과 동일 패턴)
  // ==========================
  async function loadSummary() {
    try {
      const { data } = await api.get<MaterialSummary>('/api/analytics/materials/summary');
      setSummary(data);
    } catch (e) {
      console.error('재료 요약 조회 실패', e);
      setSummary(null);
    }
  }

  // ==========================
  // 테이블 첫 페이지 로드 (주문분석 스타일)
  // ==========================
  async function loadFirst() {
    setLoading(true);
    try {
      const url =
        viewBy === 'DAY'
          ? '/api/analytics/materials/day-rows'
          : '/api/analytics/materials/month-rows';

      const { data } = await api.get<PageResp<MaterialRow>>(url, {
        params: {
          start: startStr,
          end: endStr,
          size: pageSize,
          cursor: null,
        },
      });

      setRows(data.items);
      setCursor(data.nextCursor);
    } catch (e) {
      console.error('재료 분석 조회 실패', e);
      alert('재료 분석 데이터를 불러오지 못했습니다. 콘솔 로그를 확인해주세요.');
      setRows([]);
      setCursor(null);
    } finally {
      setLoading(false);
    }

    // 주문분석과 동일하게 카드 별도 로드
    loadSummary();
  }

  // ==========================
  // 테이블 추가 로드(더보기)
  // ==========================
  async function loadMore() {
    if (!cursor) return;
    setLoading(true);
    try {
      const url =
        viewBy === 'DAY'
          ? '/api/analytics/materials/day-rows'
          : '/api/analytics/materials/month-rows';

      const { data } = await api.get<PageResp<MaterialRow>>(url, {
        params: {
          start: startStr,
          end: endStr,
          size: pageSize,
          cursor,
        },
      });

      setRows(prev => [...prev, ...data.items]);
      setCursor(data.nextCursor);
    } catch (e) {
      console.error('재료 분석 더보기 실패', e);
      alert('추가 데이터를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }

  // ==========================
  // PDF 다운로드
  // ==========================
  async function handleDownloadReport() {
    try {
      setDownloading(true);
      const { data } = await api.get<Blob>(
        '/api/analytics/materials/report',
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
      link.download = `material-report_${viewLabel}_${startStr}_${endStr}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      console.error(e);
      alert('재료 분석 리포트 다운로드 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
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
          <h1 className="text-2xl font-semibold">재료 분석</h1>
          <p className="text-sm text-gray-600">
            타임존: {tz} / 상단 카드는 이번달 1일 ~ 어제 기준(MTD)
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

          {/* 리포트 다운로드 */}
          <Button onClick={handleDownloadReport} disabled={downloading}>
            <Download className="w-4 h-4 mr-2" />
            {downloading ? '다운로드 중…' : '리포트 다운로드'}
          </Button>
        </div>
      </div>

      {/* 상단 요약 카드 4개 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {/* 1. 사용량/원가 TOP5 통합 카드 */}
        <KPICard
          title="재료 TOP5"
          value={
            <div className="text-xs space-y-2">
              <div className="font-semibold text-white-600">사용량 기준</div>
              {summary && summary.topByUsage && summary.topByUsage.length > 0 ? (
                summary.topByUsage.slice(0, 5).map((m) => (
                  <div key={m.materialId} className="flex justify-between">
                    <span className="text-white-700 truncate">{m.materialName}</span>
                    <span className="font-semibold">
                      {m.usedQuantity.toFixed(1)} {m.unitName}
                    </span>
                  </div>
                ))
              ) : (
                <div className="text-white-400">데이터 없음</div>
              )}

              <div className="mt-3 font-semibold text-white-600">원가 기준</div>
              {summary && summary.topByCost && summary.topByCost.length > 0 ? (
                summary.topByCost.slice(0, 5).map((m) => (
                  <div key={`cost-${m.materialId}`} className="flex justify-between">
                    <span className="text-gray-700 truncate">{m.materialName}</span>
                    <span className="font-semibold">₩{fmtMoneyInt(m.cost)}</span>
                  </div>
                ))
              ) : (
                <div className="text-white-400">데이터 없음</div>
              )}
            </div>
          }
          icon={Package}
          color="orange"
        />

        {/* 2. 원가율 */}
        <KPICard
          title="재료 원가율"
          value={
            <div className="space-y-1">
              <div className="text-2xl font-bold">
                {fmtPercent1(summary?.currentCostRate ?? 0)}
              </div>
              <div className="text-sm text-white-600">
                전월: {fmtPercent1(summary?.prevCostRate ?? 0)}
              </div>
              <div
                className={`text-sm flex items-center gap-1 ${
                  (summary?.costRateDiff ?? 0) >= 0
                    ? 'text-red-400'
                    : 'text-green-400'
                }`}
              >
                {(summary?.costRateDiff ?? 0) >= 0 ? (
                  <TrendingUp className="w-4 h-4" />
                ) : (
                  <TrendingDown className="w-4 h-4" />
                )}
                {summary?.costRateDiff != null
                  ? `${Math.abs(summary.costRateDiff).toFixed(1)}%p`
                  : '—'}
              </div>
            </div>
          }
          icon={TrendingUp}
          color="red"
        />

        {/* 3. 재고 부족 위험 */}
        <KPICard
          title="재고 부족 위험"
          value={
            <div className="space-y-1">
              <div className="text-2xl font-bold text-orange-400">
                {summary?.lowStockCount ?? 0}개
              </div>
              <div className="text-sm text-white-600">
                재고 부족 또는 품절 상태
              </div>
            </div>
          }
          icon={AlertTriangle}
          color="green"
        />

        {/* 4. 유통기한 임박 */}
        <KPICard
          title="유통기한 임박"
          value={
            <div className="space-y-1">
              <div className="text-2xl font-bold text-red-400">
                {summary?.expireSoonCount ?? 0}개
              </div>
              <div className="text-sm text-white-600">
                7일 이내 유통기한 도래
              </div>
            </div>
          }
          icon={Clock}
          color="purple"
        />
      </div>

      {/* ===== 테이블 영역 ===== */}
      <Card className="bg-white rounded-xl shadow-sm overflow-hidden">
        <CardHeader className="px-6 py-4 border-b bg-light-gray">
          <CardTitle className="text-base font-semibold text-gray-900">
            {viewBy === 'DAY'
              ? '재료 분석 (일별 / 재료 단위)'
              : '재료 분석 (월별 집계 / 재료 단위)'}
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
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">재료명</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">사용량</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">원가</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">매출대비</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">최근입고일</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {(rows as MaterialDailyRow[]).map((r, i) => (
                      <tr key={i} className="hover:bg-gray-50">
                        <td className="px-6 py-3 text-center text-sm text-gray-900">{r.useDate}</td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-left">{r.materialName}</td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          {r.usedQuantity.toFixed(1)} {r.unitName}
                        </td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          ₩{fmtMoneyInt(r.cost)}
                        </td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          {fmtPercent1(r.salesShare)}
                        </td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-center">
                          {displayInbound(r.lastInboundDate)}
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
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">재료명</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">사용량</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">원가</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">원가율</th>
                      <th className="px-6 py-3 text-center text-sm font-semibold text-gray-900">최근입고월</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {(rows as MaterialMonthlyRow[]).map((r, i) => (
                      <tr key={i} className="hover:bg-gray-50">
                        <td className="px-6 py-3 text-center text-sm text-gray-900">{r.yearMonth}</td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-left">{r.materialName}</td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          {r.usedQuantity.toFixed(1)}
                        </td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          ₩{fmtMoneyInt(r.cost)}
                        </td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-right">
                          {fmtPercent1(r.costRate)}
                        </td>
                        <td className="px-6 py-3 text-sm text-gray-900 text-center">
                          {displayInbound(r.lastInboundMonth)}
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
