import { useEffect, useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "../ui/card";
import { Button } from "../ui/button";
import {
  Table,
  TableHeader,
  TableRow,
  TableHead,
  TableBody,
  TableCell,
} from "../ui/table";
import { Badge } from "../ui/badge";
import { AlertTriangle, CheckCircle2, Clock } from "lucide-react";
import { toast } from "sonner";
import api from "../../lib/authApi";

type DailyClosingDenomDto = {
  denomValue: number;
  count: number;
};

type DailyClosingExpenseDto = {
  description: string;
  amount: number;
};

interface DailyClosingDetailResponse {
  closingDate: string;

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

  denoms: DailyClosingDenomDto[];
  expenses: DailyClosingExpenseDto[];
  memo?: string | null;
}

// 숫자 포맷
function formatWon(value: number | null | undefined): string {
  if (value == null) return "0원";
  return value.toLocaleString("ko-KR") + "원";
}

type Props = {
  date: string;           // "2025-11-18"
  onBack?: () => void;    // 뒤로가기 눌렀을 때
};

export function DailyClosingDetail({ date, onBack }: Props) {
  const [data, setData] = useState<DailyClosingDetailResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchDetail = async () => {
    setLoading(true);
    try {
      const res = await api.get<DailyClosingDetailResponse>(
        "/api/daily-closing/detail", // 실제 엔드포인트에 맞게 수정
        { params: { date } }
      );
      setData(res.data);
    } catch (err) {
      console.error("마감 상세 조회 실패", err);
      toast.error("마감 상세 조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date]);

  if (loading && !data) {
    return (
      <div className="py-16 text-center text-sm text-gray-500">
        마감 상세 정보를 불러오는 중입니다...
      </div>
    );
  }

  if (!data) {
    return (
      <div className="py-16 text-center text-sm text-gray-500">
        마감 상세 정보가 없습니다.
      </div>
    );
  }

  const cashSales =
    data.cashVisitSales + data.cashTakeoutSales + data.cashDeliverySales;
  const cardSales =
    data.cardVisitSales + data.cardTakeoutSales + data.cardDeliverySales;
  const totalSales = cashSales + cardSales + data.voucherSales;

  return (
    <div className="flex flex-col gap-6 pb-16">
      {/* 헤더 */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-xl font-semibold">일일 마감 상세</h2>
          <p className="text-sm text-dark-gray">
            {data.closingDate} 일자의 마감 내역입니다.
          </p>
        </div>

        {onBack && (
          <Button variant="outline" size="sm" onClick={onBack}>
            목록으로
          </Button>
        )}
      </div>

      {/* 요약 카드 */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-4">
          <div>
            <CardTitle className="text-base font-semibold">
              마감 요약
            </CardTitle>
            <p className="text-xs text-gray-500 mt-1">
              매출, 지출, 차액을 한눈에 확인할 수 있습니다.
            </p>
          </div>
          <div>
            {data.closed ? (
              <Badge className="bg-kpi-green text-white flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3" />
                마감
              </Badge>
            ) : (
              <Badge className="bg-yellow-400 text-yellow-900 flex items-center gap-1">
                <Clock className="w-3 h-3" />
                미마감
              </Badge>
            )}
          </div>
        </CardHeader>
        <CardContent className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <div>
            <div className="text-xs text-gray-500 mb-1">총 매출</div>
            <div className="text-base font-semibold">
              {formatWon(totalSales)}
            </div>
          </div>
          <div>
            <div className="text-xs text-gray-500 mb-1">현금 매출</div>
            <div className="text-base font-semibold">
              {formatWon(cashSales)}
            </div>
          </div>
          <div>
            <div className="text-xs text-gray-500 mb-1">카드 매출</div>
            <div className="text-base font-semibold">
              {formatWon(cardSales)}
            </div>
          </div>
          <div>
            <div className="text-xs text-gray-500 mb-1">상품권 매출</div>
            <div className="text-base font-semibold">
              {formatWon(data.voucherSales)}
            </div>
          </div>
          <div>
            <div className="text-xs text-gray-500 mb-1">지출 합계</div>
            <div className="text-base font-semibold">
              {formatWon(data.totalExpense)}
            </div>
          </div>
          <div>
            <div className="text-xs text-gray-500 mb-1">차액</div>
            <div
              className={
                "text-base font-semibold " +
                (data.differenceAmount === 0
                  ? "text-kpi-green"
                  : data.differenceAmount > 0
                  ? "text-blue-600"
                  : "text-kpi-red")
              }
            >
              {data.differenceAmount === 0
                ? "일치"
                : data.differenceAmount > 0
                ? "+" + formatWon(data.differenceAmount)
                : "-" + formatWon(Math.abs(data.differenceAmount))}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 권종별 / 지출 내역 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* 권종별 시재 */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base font-semibold">
              권종별 시재
            </CardTitle>
          </CardHeader>
          <CardContent>
            {data.denoms.length === 0 ? (
              <div className="py-6 text-center text-xs text-gray-500">
                등록된 권종 정보가 없습니다.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-24">권종</TableHead>
                    <TableHead className="w-24 text-right">수량</TableHead>
                    <TableHead className="text-right">금액</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.denoms.map((d, idx) => (
                    <TableRow key={idx}>
                      <TableCell>{formatWon(d.denomValue)}</TableCell>
                      <TableCell className="text-right">
                        {d.count.toLocaleString("ko-KR")}개
                      </TableCell>
                      <TableCell className="text-right">
                        {formatWon(d.denomValue * d.count)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        {/* 지출 내역 */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base font-semibold">
              지출 내역
            </CardTitle>
          </CardHeader>
          <CardContent>
            {data.expenses.length === 0 ? (
              <div className="py-6 text-center text-xs text-gray-500">
                등록된 지출 내역이 없습니다.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>항목</TableHead>
                    <TableHead className="w-32 text-right">금액</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.expenses.map((e, idx) => (
                    <TableRow key={idx}>
                      <TableCell>{e.description}</TableCell>
                      <TableCell className="text-right">
                        {formatWon(e.amount)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>

      {/* 메모 + 안내 */}
      {data.memo && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base font-semibold">메모</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="whitespace-pre-wrap text-sm">{data.memo}</p>
          </CardContent>
        </Card>
      )}

      <div className="flex items-center gap-2 text-xs text-gray-500">
        <AlertTriangle className="w-3 h-3 text-kpi-orange" />
        차액은 실제 시재와 계산된 시재의 차이를 의미합니다.
      </div>
    </div>
  );
}
