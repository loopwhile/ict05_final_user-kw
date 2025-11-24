import { useEffect, useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import {
  Table,
  TableHeader,
  TableRow,
  TableHead,
  TableBody,
  TableCell,
} from "../ui/table";
import { Badge } from "../ui/badge";
import { CalendarDays, CheckCircle2, Clock, AlertTriangle } from "lucide-react";
import { toast } from "sonner";
import api from "../../lib/authApi";

// 마감 내역 한 줄 타입
interface DailyClosingSummary {
  id: number;
  closingDate: string; // "2025-11-17"

  cashVisitSales: number;
  cashTakeoutSales: number;
  cashDeliverySales: number;

  cardVisitSales: number;
  cardTakeoutSales: number;
  cardDeliverySales: number;

  voucherSales: number;

  totalExpense: number;
  differenceAmount: number;

  closed: boolean;
}

// 날짜 문자열(YYYY-MM-DD)로 변환
function formatDateInputValue(date: Date): string {
  return date.toISOString().slice(0, 10);
}

// 숫자를 원화 문자열로
function formatWon(value: number | null | undefined): string {
  if (value == null) return "0";
  return value.toLocaleString("ko-KR") + "원";
}

type Props = {
  onSelectDate?: (date: string) => void;

   // 🔹 App에서 기억해 줄 기간 값
  initialFromDate?: string;
  initialToDate?: string;

  // 🔹 input이 바뀔 때 App에 알려줄 콜백
  onDateRangeChange?: (from: string, to: string) => void;
};

export function DailyClosingList({ onSelectDate, initialFromDate, initialToDate, onDateRangeChange}: Props) {
  const today = new Date();
  const weekAgo = new Date();
  weekAgo.setDate(today.getDate() - 31);

  const [fromDate, setFromDate] = useState<string>(
    initialFromDate ?? formatDateInputValue(weekAgo)
  );
  const [toDate, setToDate] = useState<string>(
    initialToDate ?? formatDateInputValue(today)
  );

  const [items, setItems] = useState<DailyClosingSummary[]>([]);
  const [loading, setLoading] = useState(false);

  // 마감 내역 조회
  const fetchHistory = async (range?: { from: string; to: string }) => {
    const useFrom = range?.from ?? fromDate;
    const useTo = range?.to ?? toDate;

    setLoading(true);
    try {
      const res = await api.get<DailyClosingSummary[]>(
        "/api/daily-closing/history",
        {
          params: {
            from: useFrom,
            to: useTo,
          },
        }
      );
      setItems(res.data ?? []);
    } catch (err) {
      console.error("마감 내역 조회 실패", err);
      toast.error("마감 내역 조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 최초 진입 시 1회 조회
  useEffect(() => {
    fetchHistory();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 한 줄 클릭했을 때 (상세 화면으로 이동시킬 때 사용)
  const handleRowClick = (date: string) => {
    if (onSelectDate) {
      onSelectDate(date); // 부모에서 Date 넘겨서 DailyClosingPage 로 전환
    } else {
      console.log("선택한 마감 일자 : ", date);
    }
  };

  // 🔹 조회 기간 초기화 핸들러
  const handleResetRange = () => {
    const today = new Date();
    const weekAgo = new Date();
    weekAgo.setDate(today.getDate() - 31);

    const defaultFrom = formatDateInputValue(weekAgo);
    const defaultTo = formatDateInputValue(today);

    // 로컬 state 초기화
    setFromDate(defaultFrom);
    setToDate(defaultTo);

    // 부모(App)에게도 알려주기
    onDateRangeChange?.(defaultFrom, defaultTo);

    // 초기화된 기간으로 다시 조회
    fetchHistory({ from: defaultFrom, to: defaultTo });
  };

  return (
    <div className="flex flex-col gap-6 pb-16">
      {/* 헤더 */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-xl font-semibold">일일 마감 내역</h2>
          <p className="text-sm text-dark-gray">
            기간을 선택해서 점포의 일별 마감 내역을 조회합니다.
          </p>
        </div>
      </div>

      {/* 기간 필터 */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base font-semibold">
            <CalendarDays className="w-4 h-4" />
            조회 기간
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-3 items-center">
          <div className="flex items-center gap-2">
            <span className="text-sm text-gray-600">시작일</span>
            <Input
              type="date"
              className="w-40"
              value={fromDate}
              onChange={(e) => {
                const next = e.target.value;
                setFromDate(next);
                onDateRangeChange?.(next, toDate);   
              }}
            />
          </div>
          <span className="text-gray-400">~</span>
          <div className="flex items-center gap-2">
            <span className="text-sm text-gray-600">종료일</span>
            <Input
              type="date"
              className="w-40"
              value={toDate}
              onChange={(e) => {
                const next = e.target.value;
                setToDate(next);
                onDateRangeChange?.(fromDate, next);  // 🔹 부모에 전달
              }}
            />
          </div>

          <div className="ml-auto flex gap-2">
            <Button
              className="bg-kpi-red text-white"
              variant="outline"
              onClick={handleResetRange}
              disabled={loading}
            >
              초기화
            </Button>

            <Button
              className="bg-kpi-green text-white"
              onClick={() => fetchHistory()}
              disabled={loading}
            >
              {loading ? "조회 중..." : "조회"}
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 마감 내역 테이블 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold">
            마감 내역 목록
          </CardTitle>
        </CardHeader>
        <CardContent>
          {items.length === 0 ? (
            <div className="py-10 text-center text-sm text-gray-500">
              선택한 기간에 해당하는 마감 내역이 없습니다.
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-32">마감일자</TableHead>
                  <TableHead className="w-28 text-right">현금 매출</TableHead>
                  <TableHead className="w-28 text-right">카드 매출</TableHead>
                  <TableHead className="w-24 text-right">상품권</TableHead>
                  <TableHead className="w-24 text-right">지출 합계</TableHead>
                  <TableHead className="w-24 text-right">차액</TableHead>
                  <TableHead className="w-28 text-center">상태</TableHead>
                  <TableHead className="w-28 text-center">동작</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((item) => {
                  const cashSales =
                    item.cashVisitSales +
                    item.cashTakeoutSales +
                    item.cashDeliverySales;
                  const cardSales =
                    item.cardVisitSales +
                    item.cardTakeoutSales +
                    item.cardDeliverySales;
                  const diff = item.differenceAmount ?? 0;

                  return (
                    <TableRow
                      key={item.id}
                      className="hover:bg-gray-50"
                    >
                      <TableCell className="text-sm">
                        {item.closingDate}
                      </TableCell>
                      <TableCell className="text-right text-sm">
                        {formatWon(cashSales)}                   
                      </TableCell>
                      <TableCell className="text-right text-sm">
                        {formatWon(cardSales)}                  
                      </TableCell>
                      <TableCell className="text-right text-sm">
                        {formatWon(item.voucherSales)}           
                      </TableCell>
                      <TableCell className="text-right text-sm">
                        {formatWon(item.totalExpense)}           
                      </TableCell>
                      <TableCell className="text-right text-sm">
                        {diff === 0 ? (
                          <span className="text-kpi-green font-medium">
                            일치
                          </span>
                        ) : diff > 0 ? (
                          <span className="text-blue-600 font-medium">
                            +{formatWon(diff)}                   
                          </span>
                        ) : (
                          <span className="text-kpi-red font-medium">
                            -{formatWon(Math.abs(diff))}         
                          </span>
                        )}
                      </TableCell>
                        <TableCell className="w-28 px-2">
                            <div className="flex justify-center"> 
                                {item.closed ? (
                                <Badge
                                    variant="outline"
                                    className="border-kpi-green text-kpi-green flex items-center gap-1 justify-center"
                                >
                                    <CheckCircle2 className="w-3 h-3" />
                                    마감
                                </Badge>
                                ) : (
                                <Badge
                                    variant="outline"
                                    className="border-yellow-500 text-yellow-600 flex items-center gap-1 justify-center"
                                >
                                    <Clock className="w-3 h-3" />
                                    미마감
                                </Badge>
                                )}
                            </div>    
                        </TableCell>
                        <TableCell className="text-center">
                            <Button
                            size="sm"
                            variant="outline"
                            className="text-xs"
                            onClick={() => handleRowClick(item.closingDate)}
                            >
                            상세보기
                            </Button>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}

          <div className="mt-3 flex items-center gap-2 text-xs text-gray-500">
            <AlertTriangle className="w-3 h-3 text-kpi-orange" />
            차액은 실제 시재와 계산된 시재의 차이를 의미합니다. 마감 상세 화면에서
            시재와 지출 내역을 확인할 수 있습니다.
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
