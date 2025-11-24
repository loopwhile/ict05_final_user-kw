import { useState, useEffect  } from "react";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "../ui/tabs";
import {
  Calculator,
  DollarSign,
  CreditCard,
  Gift,
  TrendingDown,
  CheckCircle,
  Banknote,
  AlertTriangle,
  Package,
} from "lucide-react";
import { toast } from "sonner";
import { KPICard } from "../Common/KPICard";
import { Label } from "../ui/label";
import api from "../../lib/authApi";

type DailyClosingExpenseDto = {
  description: string;
  amount: number;
};

type DailyClosingDenomDto = {
  denomValue: number;
  count: number;
};

type DailyClosingInitResponse = {
  cashVisit: number;
  cashTakeout: number;
  cashDelivery: number;
  cardVisit: number;
  cardTakeout: number;
  cardDelivery: number;
  voucherTotal: number;
  totalDiscount: number;
  totalRefund: number;
  startingCash: number | null;
  totalExpense: number | null;
  depositAmount: number | null;
  calculatedCash: number | null;
  actualCash: number | null;
  carryoverCash: number | null;
  differenceAmount: number | null;
  differenceMemo: string | null;
  closed: boolean;
  expenses: DailyClosingExpenseDto[];
  denoms: DailyClosingDenomDto[];
};

// 화면용 지출 배열
type UiExpense = {
  id: number;
  description: string;
  amount: number;
};

type DailyClosingPageProps = {
  onPageChange?: (page: string) => void;
};

// 권종별 화폐
const denominations = [
  { value: 50000, name: "5만원권", type: "note", color: "text-yellow-600" },
  { value: 10000, name: "1만원권", type: "note", color: "text-green-600" },
  { value: 5000, name: "5천원권", type: "note", color: "text-red-600" },
  { value: 1000, name: "1천원권", type: "note", color: "text-blue-600" },
  { value: 500, name: "500원", type: "coin", color: "text-gray-600" },
  { value: 100, name: "100원", type: "coin", color: "text-gray-500" },
  { value: 50, name: "50원", type: "coin", color: "text-gray-400" },
  { value: 10, name: "10원", type: "coin", color: "text-gray-300" },
];

export function DailyClosingPage({ onPageChange }: DailyClosingPageProps) {

  const [loading, setLoading] = useState(false);

  // [ 기본 데이터 ]
  // 결제 요약
  const [cashPayments, setCashPayments] = useState({
    visit: 0,
    takeout: 0,
    delivery: 0,
  });
  const [cardPayments, setCardPayments] = useState({
    visit: 0,
    takeout: 0,
    delivery: 0,
  });
  const [voucherTotal, setVoucherTotal] = useState(0);
  const [refundAmount, setRefundAmount] = useState(0);
  const [discountAmount, setDiscountAmount] = useState(0);

  // 지출
  const [expenses, setExpenses] = useState<UiExpense[]>([]);
  const [newExpense, setNewExpense] = useState({ description: "", amount: "" });

  // 시재 관련
  const [startingCash, setStartingCash] = useState(0);
  const [depositAmount, setDepositAmount] = useState(0);
  const [differenceMemo, setDifferenceMemo] = useState("");

  const [denomCounts, setDenomCounts] = useState<Record<number, number>>({
    50000: 0, 10000: 0, 5000: 0, 1000: 0, 500: 0, 100: 0, 50: 0, 10: 0,
  });

  const [isClosed, setIsClosed] = useState(false);

  const [startingCashLocked, setStartingCashLocked] = useState(false);

  // 오늘 날짜 문자열 (YYYY-MM-DD)
  //const todayStr = new Date().toISOString().slice(0, 10);
  const todayStr = (() => {
    const d = new Date(); // 로컬 시간
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${y}-${m}-${day}`;
  })();

  // 현금 매출
  const totalCash =
    cashPayments.visit +
    cashPayments.takeout +
    cashPayments.delivery;

  // 카드 매출
  const totalCard =
    cardPayments.visit +
    cardPayments.takeout +
    cardPayments.delivery;

  // 총 매출
  const totalSales = totalCash + totalCard + voucherTotal;
  const totalExpenses = expenses.reduce((sum, e) => sum + e.amount, 0);
  const netSales = totalSales - discountAmount - refundAmount;

  // 시재 계산
  const actualCashFromCounts = denominations.reduce(
    (total, denom) => total + (denomCounts[denom.value] * denom.value),
    0
  );

  const calculatedCash =
    startingCash + totalCash - totalExpenses - depositAmount;

  const difference = actualCashFromCounts - calculatedCash;
  const carryoverCash = calculatedCash;

  useEffect(() => {
    const fetchDailyClosing = async () => {
      try {
        setLoading(true);
        const res = await api.get<DailyClosingInitResponse>(
          "/api/daily-closing/close",
          { params: { date: todayStr } }
        );
        const data = res.data;

        // 결제 요약
        setCashPayments({
          visit: data.cashVisit,
          takeout: data.cashTakeout,
          delivery: data.cashDelivery,
        });
        setCardPayments({
          visit: data.cardVisit,
          takeout: data.cardTakeout,
          delivery: data.cardDelivery,
        });
        setVoucherTotal(data.voucherTotal);

        setDiscountAmount(data.totalDiscount ?? 0);
        setRefundAmount(data.totalRefund ?? 0);

        // 시재 기본값
        setStartingCash(data.startingCash ?? 0);
        setDepositAmount(data.depositAmount ?? 0);
        setDifferenceMemo(data.differenceMemo ?? "");
        setIsClosed(data.closed);

        // 오픈 후 시작금 수정 잠금
        setStartingCashLocked(data.startingCash !== null && data.startingCash !== undefined);

        // 지출: DTO 에 id 가 없으므로 화면용 id 는 index 로 만든다
        const uiExpenses: UiExpense[] =
          (data.expenses || []).map((e, idx) => ({
            id: idx + 1,
            description: e.description,
            amount: e.amount,
          }));
        setExpenses(uiExpenses);

        // 권종: denomValue, count 에서 counts 맵으로 변환
        const nextCounts: Record<number, number> = {
          50000: 0, 10000: 0, 5000: 0, 1000: 0, 500: 0, 100: 0, 50: 0, 10: 0,
        };
        (data.denoms || []).forEach((d) => {
          nextCounts[d.denomValue] = d.count;
        });
        setDenomCounts(nextCounts);
      } catch (err) {
        console.error(err);
        toast.error("일일 시재 정보를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchDailyClosing();
  }, [todayStr]);

  // 이벤트
  const handleOpen = async () => {
    if (isClosed || startingCashLocked) return;
    
    const ok = window.confirm("오늘 일자의 오픈 시재를 저장하시겠습니까?");
    if (!ok) {
      return;
    }

    try {
      setLoading(true);
      const payload = {
        closingDate: todayStr,
        startingCash,
      };
      await api.post("/api/daily-closing/open", payload);
      setStartingCashLocked(true);
      toast.success("오픈 시재가 저장되었습니다.");
    } catch (err) {
      console.error(err);
      toast.error("오픈 시재 저장에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleAddExpense = () => {
    if (isClosed) return; // 마감 후 입력 방지
    if (!newExpense.description || !newExpense.amount) {
      toast.error("지출 내역과 금액을 입력하세요.");
      return;
    }
    const expense = {
      id: Date.now(),
      description: newExpense.description,
      amount: parseInt(newExpense.amount),
    };
    setExpenses([...expenses, expense]);
    setNewExpense({ description: "", amount: "" });
    toast.success("지출 항목이 추가되었습니다.");
  };

  const handleRemoveExpense = (id: number) => {
    if (isClosed) return; // 마감 후 입력 방지
    setExpenses(expenses.filter((e) => e.id !== id));
  }

  const handleDenomCountChange = (value: number, count: number) => {
    if (isClosed) return; // 마감 후 입력 방지
    setDenomCounts((prev) => ({ ...prev, [value]: count }));
  };

  const handleCompleteClosing = async () => {
    if (isClosed) {
      return;
    }
    // 차액이 있을 때 메모 필수
    if (difference !== 0 && !differenceMemo.trim()) {
      toast.error("차액이 있을 경우 사유 메모를 입력해야 마감할 수 있습니다.");
      return;
    }

    const ok = window.confirm("오늘 일자를 마감 처리하시겠습니까?");
    if (!ok) {
      return;
    }

    try {
      setLoading(true);

      // 테스트용 로그
      console.log("[DailyClosingPage] complete closing, go list");

      const payload = {
        closingDate: todayStr,
        startingCash,
        totalExpense: totalExpenses,
        depositAmount,
        calculatedCash,
        actualCash: actualCashFromCounts,
        carryoverCash,
        differenceAmount: difference,
        differenceMemo: differenceMemo || null,
        expenses: expenses.map((e) => ({
          description: e.description,
          amount: e.amount,
        })),
        denoms: denominations
          .map((d) => ({
            denomValue: d.value,
            count: denomCounts[d.value] || 0,
          }))
          .filter((d) => d.count > 0), // 개수 0 인 것은 빼도 된다
      };

      // 백엔드에 마감 저장 요청
      await api.post("/api/daily-closing/close", payload);

      setIsClosed(true);
      toast.success("일일 마감이 완료되었습니다.");

      if (onPageChange) {
        console.log("[DailyClosingPage] go list");
        onPageChange("daily-closing-list");
      }
    } catch (err) {
      console.error(err);
      toast.error("일일 마감 저장에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col gap-14 pb-16">
      {/* 헤더 */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2 className="text-xl font-semibold">일일 시재/마감</h2>
          <p className="text-sm text-dark-gray">
            마감일자: {new Date().toLocaleDateString("ko-KR")}
          </p>
        </div>
        <div className="flex gap-3">
          <Button
            variant="outline"
            className="gap-2"
            onClick={handleOpen}
            disabled={isClosed || startingCashLocked || loading}
          >
            <Package className="w-4 h-4" />
            오픈 시재 저장
          </Button>
          <Button
            className="bg-kpi-green text-white gap-2"
            onClick={handleCompleteClosing}
            disabled={isClosed || loading}
          >
            <CheckCircle className="w-4 h-4" /> 마감 완료
          </Button>
        </div>
      </div>

      {/* KPI 카드 */}
      <section>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <KPICard
            title="총 매출액"
            value={`₩${totalSales.toLocaleString()}`}
            icon={DollarSign}
            color="green"
            change={`순매출 ₩${netSales.toLocaleString()}`}
          />
          <KPICard
            title="결제 비율"
            value={`${((totalCash / totalSales) * 100).toFixed(1)}%`}
            icon={CreditCard}
            color="purple"
            change={`현금 ${(totalCash / totalSales * 100).toFixed(1)}% · 카드 ${(totalCard / totalSales * 100).toFixed(1)}%`}
          />
          <KPICard
            title="할인 / 환불"
            value={`₩${(discountAmount + refundAmount).toLocaleString()}`}
            icon={TrendingDown}
            color="orange"
            change={`할인 ₩${discountAmount.toLocaleString()} · 환불 ₩${refundAmount.toLocaleString()}`}
          />
        </div>
      </section>

      {/* 거래내역 탭 */}
      <section>
        <Tabs defaultValue="cash" className="pt-6">
          <TabsList>
            <TabsTrigger value="cash">현금</TabsTrigger>
            <TabsTrigger value="card">카드</TabsTrigger>
            <TabsTrigger value="voucher">상품권</TabsTrigger>
          </TabsList>

          {/* 현금 */}
          <TabsContent value="cash">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-lg font-semibold">
                  <DollarSign className="w-5 h-5 text-kpi-green" />
                  현금 거래
                </CardTitle>
              </CardHeader>
              <CardContent>
               <div className="grid grid-cols-3 text-center">
                  <div>방문: ₩{cashPayments.visit.toLocaleString()}</div>
                  <div>포장: ₩{cashPayments.takeout.toLocaleString()}</div>
                  <div>배달: ₩{cashPayments.delivery.toLocaleString()}</div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* 카드 */}
          <TabsContent value="card">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-lg font-semibold">
                  <CreditCard className="w-5 h-5 text-blue-600" />
                  카드 거래
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-3 text-center">
                  <div>방문: ₩{cardPayments.visit.toLocaleString()}</div>
                  <div>포장: ₩{cardPayments.takeout.toLocaleString()}</div>
                  <div>배달: ₩{cardPayments.delivery.toLocaleString()}</div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* 상품권 */}
          <TabsContent value="voucher">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-lg font-semibold">
                  <Gift className="w-5 h-5 text-purple-600" />
                  상품권 거래
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p>총액: ₩{voucherTotal.toLocaleString()}</p>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </section>  

      {/* 현금 지출 내역 */}
      <section className="mt-4">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg font-semibold">
              <TrendingDown className="w-5 h-5 text-kpi-red" />
              현금 지출 내역
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-12 gap-2">
              <Input
                placeholder="지출 내역"
                value={newExpense.description}
                onChange={(e) =>
                  setNewExpense({ ...newExpense, description: e.target.value })
                }
                className="col-span-6"
              />
              <Input
                type="number"
                placeholder="금액"
                value={newExpense.amount}
                onChange={(e) =>
                  setNewExpense({ ...newExpense, amount: e.target.value })
                }
                className="col-span-4"
              />
              <Button
                onClick={handleAddExpense}
                className="col-span-2 bg-kpi-red text-white"
              >
                추가
              </Button>
            </div>

            {expenses.map((e) => (
              <div
                key={e.id}
                className="flex justify-between items-center bg-gray-50 p-3 rounded"
              >
                <span>{e.description}</span>
                <div className="flex items-center gap-2">
                  <span>₩{e.amount.toLocaleString()}</span>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleRemoveExpense(e.id)}
                  >
                    삭제
                  </Button>
                </div>
              </div>
            ))}

            <div className="flex justify-between border-t pt-3 font-semibold">
              <span>총 지출</span>
              <span>₩{totalExpenses.toLocaleString()}</span>
            </div>
          </CardContent>
        </Card>
      </section>

      {/* 시재 요약 */}
      <section className="mt-4">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg font-semibold">
              <Calculator className="w-5 h-5 text-kpi-orange" />
              시작금(준비금) 입력
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-3 gap-6">
              <div className="space-y-2">
                <Label className="font-semibold">시작금(준비금)</Label>
                <Input
                  type="number"
                  value={startingCash}
                  onChange={(e) => setStartingCash(parseFloat(e.target.value) || 0)}
                  disabled={isClosed || startingCashLocked}
                />
              </div>

              <div className="space-y-2 text-gray-500">
                <Label>현금 매출</Label>
                <Input type="number" value={totalCash} readOnly />
              </div>

              <div className="space-y-2 text-gray-500">
                <Label>총 현금 지출</Label>
                <Input type="number" value={totalExpenses} readOnly />
              </div>
            </div>
          </CardContent>
        </Card>
      </section>
      

      {/* 권종별 시재 입력 + 마감 요약 */}
      <section className="mt-4">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg font-semibold">
              <Banknote className="w-5 h-5 text-kpi-green" />
              권종별 시재 입력
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* 지폐 */}
            <div>
              <h4 className="font-medium mb-3">지폐</h4>
              <div className="grid grid-cols-2 gap-4">
                {denominations.filter(d => d.type === "note").map(denom => (
                  <div key={denom.value} className="flex justify-between p-3 bg-gray-50 rounded-lg">
                    <span className={`font-medium ${denom.color}`}>{denom.name}</span>
                    <div className="flex items-center gap-2">
                      <Input
                        type="number"
                        value={denomCounts[denom.value] || ""}
                        onChange={(e) =>
                          handleDenomCountChange(denom.value, parseInt(e.target.value) || 0)
                        }
                        className="w-16 h-8"
                        disabled={isClosed}
                      />
                      <span className="text-xs text-gray-500">장</span>
                      <span className="text-xs text-gray-600 w-20 text-right">
                        ₩{((denomCounts[denom.value] || 0) * denom.value).toLocaleString()}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* 동전 */}
            <div>
              <h4 className="font-medium mb-3">동전</h4>
              <div className="grid grid-cols-2 gap-4">
                {denominations.filter(d => d.type === "coin").map(denom => (
                  <div key={denom.value} className="flex justify-between p-3 bg-gray-50 rounded-lg">
                    <span className={`font-medium ${denom.color}`}>{denom.name}</span>
                    <div className="flex items-center gap-2">
                      <Input
                        type="number"
                        value={denomCounts[denom.value] || ""}
                        onChange={(e) =>
                          handleDenomCountChange(denom.value, parseInt(e.target.value) || 0)
                        }
                        className="w-16 h-8"
                        disabled={isClosed}
                      />
                      <span className="text-xs text-gray-500">개</span>
                      <span className="text-xs text-gray-600 w-20 text-right">
                        ₩{((denomCounts[denom.value] || 0) * denom.value).toLocaleString()}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* 실제 시재 총액 */}
            <div className="bg-blue-50 p-4 rounded-lg flex justify-between items-center">
              <span className="font-medium text-blue-900">실제 시재 총액</span>
              <span className="font-bold text-lg text-blue-600">
                ₩{actualCashFromCounts.toLocaleString()}
              </span>
            </div>
          </CardContent>
        </Card>

        {/* 시재 마감 요약 */}
        <section className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-lg font-semibold">
                <Calculator className="w-5 h-5 text-kpi-orange" />
                시재 마감 요약
              </CardTitle>
            </CardHeader>

            <CardContent className="divide-y divide-gray-100">
              <div className="flex justify-between items-center py-2">
                <span className="text-sm text-gray-600">시작금(준비금)</span>
                <span className="font-semibold text-gray-800">
                  ₩{startingCash.toLocaleString()}
                </span>
              </div>

              <div className="flex justify-between items-center py-2">
                <span className="text-sm text-gray-600">현금 매출</span>
                <span className="font-semibold text-green-600">
                  ₩{totalCash.toLocaleString()}
                </span>
              </div>

              <div className="flex justify-between items-center py-2">
                <span className="text-sm text-gray-600">현금 지출</span>
                <span className="font-semibold text-gray-800">
                  ₩{totalExpenses.toLocaleString()}
                </span>
              </div>

              <div className="flex justify-between items-center py-2">
                <span className="text-sm text-gray-600">입금액</span>
                <Input
                  type="number"
                  className="w-28 text-right"
                  value={depositAmount}
                  onChange={(e) =>
                    setDepositAmount(parseFloat(e.target.value) || 0)
                  }
                  disabled={isClosed}
                />
              </div>

              <div className="flex justify-between items-center py-2">
                <span className="text-sm text-gray-600">실제 시재금</span>
                <span className="font-semibold text-blue-600">
                  ₩{actualCashFromCounts.toLocaleString()}
                </span>
              </div>

              <div className="flex justify-between items-center py-3 border-t mt-3">
                <span className="font-semibold text-gray-700">차액</span>
                <span
                  className={`font-bold ${
                    difference === 0
                      ? "text-kpi-green"
                      : difference > 0
                      ? "text-blue-600"
                      : "text-kpi-red"
                  }`}
                >
                  {difference === 0
                    ? "일치"
                    : difference > 0
                    ? `+₩${difference.toLocaleString()}`
                    : `-₩${Math.abs(difference).toLocaleString()}`}
                </span>
              </div>

              {/* 차액 사유 메모 */}
              {difference !== 0 && (
                <div className="pt-3">
                  <Label className="text-sm text-gray-700 flex items-center gap-1">
                    <AlertTriangle className="w-4 h-4 text-kpi-red" />
                    차액 사유 메모
                  </Label>
                  <textarea
                    className="mt-1 w-full border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-kpi-orange"
                    rows={2}
                    value={differenceMemo}
                    onChange={(e) => setDifferenceMemo(e.target.value)}
                    placeholder="예: 현금 계산 실수로 보임, 오후 교대 시 재확인 예정"
                    disabled={isClosed}
                  />
                  <p className="mt-1 text-xs text-gray-500">
                    차액이 발생한 경우 사유를 간단히 기록해 주세요.
                  </p>
                </div>
              )}
            </CardContent>
          </Card>
        </section>  

      </section>
    </div>
  );
}
