import React, { useState } from 'react';
import { KPICard } from '../Common/KPICard';
import { DataTable } from '../Common/DataTable';
import { FormModal } from '../Common/FormModal';
import { StatusBadge } from '../Common/StatusBadge';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { Calendar } from '../ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '../ui/popover';
import { Badge } from '../ui/badge';
import { CalendarIcon, Plus, TrendingUp, TrendingDown, CreditCard, Wallet, FileText, Calculator, Banknote } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, BarChart, Bar, PieChart, Pie, Cell } from 'recharts';
import { toast } from 'sonner';

// 매출 데이터 (임시)
const salesData = [
  { date: '2024-01-01', cash: 150000, card: 300000, total: 450000, orders: 45 },
  { date: '2024-01-02', cash: 200000, card: 350000, total: 550000, orders: 52 },
  { date: '2024-01-03', cash: 180000, card: 320000, total: 500000, orders: 48 },
  { date: '2024-01-04', cash: 220000, card: 380000, total: 600000, orders: 58 },
  { date: '2024-01-05', cash: 190000, card: 340000, total: 530000, orders: 51 },
];

// 지출 데이터 (임시)
const expenseData = [
  { id: 1, date: '2024-01-01', category: '재료비', description: '농산물 구매', amount: 80000, method: '카드', status: '승인' },
  { id: 2, date: '2024-01-01', category: '인건비', description: '알바생 급여', amount: 120000, method: '현금', status: '승인' },
  { id: 3, date: '2024-01-02', category: '임대료', description: '1월 임대료', amount: 500000, method: '계좌이체', status: '승인' },
  { id: 4, date: '2024-01-02', category: '유틸리티', description: '전기세', amount: 45000, method: '카드', status: '대기' },
  { id: 5, date: '2024-01-03', category: '기타', description: '청소용품', amount: 25000, method: '현금', status: '승인' },
  { id: 6, date: '2024-01-03', category: '재료비', description: '육류 구매', amount: 150000, method: '카드', status: '승인' },
  { id: 7, date: '2024-01-04', category: '마케팅', description: '전단지 제작', amount: 35000, method: '카드', status: '승인' },
  { id: 8, date: '2024-01-04', category: '유틸리티', description: '가스비', amount: 52000, method: '계좌이체', status: '승인' },
  { id: 9, date: '2024-01-05', category: '인건비', description: '정규직 급여', amount: 280000, method: '계좌이체', status: '승인' },
  { id: 10, date: '2024-01-05', category: '재료비', description: '조미료 구매', amount: 45000, method: '현금', status: '승인' },
  { id: 11, date: '2024-01-06', category: '기타', description: '사무용품', amount: 18000, method: '카드', status: '대기' },
  { id: 12, date: '2024-01-06', category: '유지보수', description: '주방기기 수리', amount: 85000, method: '카드', status: '승인' },
  { id: 13, date: '2024-01-07', category: '재료비', description: '유제품 구매', amount: 95000, method: '카드', status: '승인' },
  { id: 14, date: '2024-01-07', category: '마케팅', description: '소셜미디어 광고', amount: 120000, method: '카드', status: '승인' },
  { id: 15, date: '2024-01-08', category: '기타', description: '포장재 구매', amount: 32000, method: '현금', status: '승인' },
  { id: 16, date: '2024-01-08', category: '교육', description: '위생교육비', amount: 60000, method: '계좌이체', status: '대기' },
  { id: 17, date: '2024-01-09', category: '재료비', description: '음료 구매', amount: 75000, method: '카드', status: '승인' },
  { id: 18, date: '2024-01-09', category: '유틸리티', description: '수도세', amount: 38000, method: '계좌이체', status: '승인' },
  { id: 19, date: '2024-01-10', category: '인건비', description: '주말 수당', amount: 65000, method: '현금', status: '승인' },
  { id: 20, date: '2024-01-10', category: '기타', description: '보험료', amount: 125000, method: '계좌이체', status: '승인' },
];

// 월별 매출 추이 데이터
const monthlyData = [
  { month: '2023-07', revenue: 12500000, expense: 8200000, profit: 4300000 },
  { month: '2023-08', revenue: 13200000, expense: 8500000, profit: 4700000 },
  { month: '2023-09', revenue: 12800000, expense: 8300000, profit: 4500000 },
  { month: '2023-10', revenue: 14100000, expense: 9100000, profit: 5000000 },
  { month: '2023-11', revenue: 13700000, expense: 8800000, profit: 4900000 },
  { month: '2023-12', revenue: 15200000, expense: 9800000, profit: 5400000 },
];

// 카테고리별 지출 데이터
const expenseByCategoryData = [
  { name: '재료비', value: 35, color: '#FF6B6B' },
  { name: '인건비', value: 28, color: '#F77F00' },
  { name: '임대료', value: 18, color: '#06D6A0' },
  { name: '유틸리티', value: 8, color: '#9D4EDD' },
  { name: '마케팅', value: 5, color: '#17A2B8' },
  { name: '유지보수', value: 3, color: '#FFC107' },
  { name: '기타', value: 3, color: '#6C757D' },
];

export function FinanceManagement() {
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [showExpenseModal, setShowExpenseModal] = useState(false);
  const [expenses, setExpenses] = useState(expenseData);

  const expenseColumns = [
    {
      key: 'date',
      label: '날짜',
      render: (expense: any) => new Date(expense.date).toLocaleDateString('ko-KR'),
    },
    {
      key: 'category',
      label: '분류',
      render: (expense: any) => (
        <Badge variant="outline" className="text-xs">
          {expense.category}
        </Badge>
      ),
    },
    {
      key: 'description',
      label: '내용',
    },
    {
      key: 'amount',
      label: '금액',
      render: (expense: any) => `₩${(expense.amount || 0).toLocaleString()}`,
    },
    {
      key: 'method',
      label: '결제방법',
      render: (expense: any) => (
        <div className="flex items-center gap-1">
          {expense.method === '현금' ? <Wallet className="w-3 h-3" /> : <CreditCard className="w-3 h-3" />}
          <span className="text-xs">{expense.method}</span>
        </div>
      ),
    },
    {
      key: 'status',
      label: '상태',
      render: (expense: any) => <StatusBadge status={expense.status} />,
    },
  ];

  const expenseFormFields = [
    {
      name: 'date',
      label: '날짜',
      type: 'date' as const,
      required: true,
    },
    {
      name: 'category',
      label: '분류',
      type: 'select' as const,
      options: [
        { value: '재료비', label: '재료비' },
        { value: '인건비', label: '인건비' },
        { value: '임대료', label: '임대료' },
        { value: '유틸리티', label: '유틸리티' },
        { value: '마케팅', label: '마케팅' },
        { value: '유지보수', label: '유지보수' },
        { value: '교육', label: '교육' },
        { value: '기타', label: '기타' },
      ],
      required: true,
    },
    {
      name: 'description',
      label: '내용',
      type: 'text' as const,
      required: true,
    },
    {
      name: 'amount',
      label: '금액',
      type: 'number' as const,
      required: true,
    },
    {
      name: 'method',
      label: '결제방법',
      type: 'select' as const,
      options: [
        { value: '현금', label: '현금' },
        { value: '카드', label: '카드' },
        { value: '계좌이체', label: '계좌이체' },
      ],
      required: true,
    },
  ];

  const handleAddExpense = (formData: any) => {
    const newExpense = {
      id: expenses.length + 1,
      ...formData,
      amount: Number(formData.amount) || 0,
      status: '대기',
    };
    
    setExpenses([...expenses, newExpense]);
    setShowExpenseModal(false);
    toast.success('지출이 등록되었습니다.');
  };

  const todaySales = salesData[salesData.length - 1];
  const totalMonthlyRevenue = monthlyData[monthlyData.length - 1]?.revenue || 0;
  const totalMonthlyExpense = monthlyData[monthlyData.length - 1]?.expense || 0;
  const totalMonthlyProfit = monthlyData[monthlyData.length - 1]?.profit || 0;

  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">회계 관리</h1>
          <p className="text-sm text-gray-600 mt-1">매출, 지출, 손익을 관리합니다</p>
        </div>
        <div className="flex gap-2">
          <Popover>
            <PopoverTrigger asChild>
              <Button variant="outline">
                <CalendarIcon className="w-4 h-4 mr-2" />
                {selectedDate.toLocaleDateString('ko-KR')}
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-auto p-0">
              <Calendar
                mode="single"
                selected={selectedDate}
                onSelect={(date: any) => date && setSelectedDate(date)}
                initialFocus
              />
            </PopoverContent>
          </Popover>
          <Button onClick={() => setShowExpenseModal(true)}>
            <Plus className="w-4 h-4 mr-2" />
            지출 등록
          </Button>
        </div>
      </div>

      {/* KPI 카드들 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <KPICard
          title="오늘 매출"
          value={`₩${(todaySales?.total || 0).toLocaleString()}`}
          icon={Banknote}
          color="red"
          trend={+8.2}
        />
        <KPICard
          title="이번달 매출"
          value={`₩${(totalMonthlyRevenue / 10000).toFixed(0)}만원`}
          icon={TrendingUp}
          color="orange"
          trend={+12.5}
        />
        <KPICard
          title="이번달 지출"
          value={`₩${(totalMonthlyExpense / 10000).toFixed(0)}만원`}
          icon={TrendingDown}
          color="green"
          trend={-3.1}
        />
        <KPICard
          title="이번달 순이익"
          value={`₩${(totalMonthlyProfit / 10000).toFixed(0)}만원`}
          icon={Calculator}
          color="purple"
          trend={+15.3}
        />
      </div>

      {/* 탭 컨텐츠 */}
      <Tabs defaultValue="daily" className="space-y-6">
        <TabsList>
          <TabsTrigger value="daily">일별 매출</TabsTrigger>
          <TabsTrigger value="expense">지출 관리</TabsTrigger>
          <TabsTrigger value="profit">손익 분석</TabsTrigger>
          <TabsTrigger value="report">재무 리포트</TabsTrigger>
        </TabsList>

        <TabsContent value="daily" className="space-y-6">
          {/* 일별 매출 차트 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrendingUp className="w-5 h-5" />
                일별 매출 추이
              </CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={salesData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" tickFormatter={(value) => new Date(value).getDate().toString()} />
                  <YAxis tickFormatter={(value) => `₩${(value / 1000).toFixed(0)}K`} />
                  <Tooltip 
                    formatter={(value: any) => [`₩${(value || 0).toLocaleString()}`, '']}
                    labelFormatter={(label) => new Date(label).toLocaleDateString('ko-KR')}
                  />
                  <Legend />
                  <Line 
                    type="monotone" 
                    dataKey="total" 
                    stroke="#FF6B6B" 
                    strokeWidth={2}
                    name="총 매출"
                  />
                  <Line 
                    type="monotone" 
                    dataKey="card" 
                    stroke="#F77F00" 
                    strokeWidth={2}
                    name="카드 결제"
                  />
                  <Line 
                    type="monotone" 
                    dataKey="cash" 
                    stroke="#06D6A0" 
                    strokeWidth={2}
                    name="현금 결제"
                  />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          {/* 결제 수단별 매출 */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <CreditCard className="w-5 h-5" />
                  오늘 결제 수단별 매출
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex justify-between items-center">
                    <div className="flex items-center gap-2">
                      <div className="w-3 h-3 bg-kpi-orange rounded-full"></div>
                      <span>카드 결제</span>
                    </div>
                    <span className="font-semibold">₩{(todaySales?.card || 0).toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <div className="flex items-center gap-2">
                      <div className="w-3 h-3 bg-kpi-green rounded-full"></div>
                      <span>현금 결제</span>
                    </div>
                    <span className="font-semibold">₩{(todaySales?.cash || 0).toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between items-center pt-2 border-t">
                    <span className="font-semibold">총 매출</span>
                    <span className="font-semibold text-lg">₩{(todaySales?.total || 0).toLocaleString()}</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <FileText className="w-5 h-5" />
                  오늘 주문 현황
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex justify-between items-center">
                    <span>총 주문 수</span>
                    <span className="font-semibold">{todaySales?.orders}건</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span>평균 주문 금액</span>
                    <span className="font-semibold">₩{(todaySales?.total && todaySales?.orders) ? Math.round(todaySales.total / todaySales.orders).toLocaleString() : '0'}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span>시간당 평균 주문</span>
                    <span className="font-semibold">{Math.round(todaySales?.orders / 12)}건</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="expense" className="space-y-6">
          <DataTable
            data={expenses}
            columns={expenseColumns}
            title="지출 내역"
            searchPlaceholder="지출 내역 검색"
            onAdd={() => setShowExpenseModal(true)}
            addButtonText="지출 등록"
            filters={[
              { label: '승인', value: '승인' },
              { label: '대기', value: '대기' },
              { label: '거부', value: '거부' }
            ]}
          />
        </TabsContent>

        <TabsContent value="profit" className="space-y-6">
          {/* 월별 손익 차트 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Calculator className="w-5 h-5" />
                월별 손익 분석
              </CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={monthlyData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="month" tickFormatter={(value) => value.slice(-2) + '월'} />
                  <YAxis tickFormatter={(value) => `₩${(value / 1000000).toFixed(0)}M`} />
                  <Tooltip 
                    formatter={(value: any) => [`₩${(value / 10000).toFixed(0)}만원`, '']}
                    labelFormatter={(label) => `${label.slice(-2)}월`}
                  />
                  <Legend />
                  <Bar dataKey="revenue" fill="#FF6B6B" name="매출" />
                  <Bar dataKey="expense" fill="#F77F00" name="지출" />
                  <Bar dataKey="profit" fill="#06D6A0" name="순이익" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          {/* 카테고리별 지출 분석 */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>카테고리별 지출 비율</CardTitle>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={250}>
                  <PieChart>
                    <Pie
                      data={expenseByCategoryData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {expenseByCategoryData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>이번달 손익 요약</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex justify-between items-center">
                    <span>총 매출</span>
                    <span className="font-semibold text-kpi-red">₩{(totalMonthlyRevenue / 10000).toFixed(0)}만원</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span>총 지출</span>
                    <span className="font-semibold text-kpi-orange">₩{(totalMonthlyExpense / 10000).toFixed(0)}만원</span>
                  </div>
                  <div className="flex justify-between items-center pt-2 border-t">
                    <span className="font-semibold">순이익</span>
                    <span className="font-semibold text-lg text-kpi-green">₩{(totalMonthlyProfit / 10000).toFixed(0)}만원</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span>순이익률</span>
                    <span className="font-semibold">{((totalMonthlyProfit / totalMonthlyRevenue) * 100).toFixed(1)}%</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="report" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>재무 리포트</CardTitle>
              <p className="text-sm text-gray-600">월별 재무 현황을 요약합니다</p>
            </CardHeader>
            <CardContent>
              <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <h4 className="font-semibold mb-2">매출 성장률</h4>
                    <p className="text-2xl font-bold text-kpi-green">+12.5%</p>
                    <p className="text-sm text-gray-600">전월 대비</p>
                  </div>
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <h4 className="font-semibold mb-2">지출 절감률</h4>
                    <p className="text-2xl font-bold text-kpi-red">-3.1%</p>
                    <p className="text-sm text-gray-600">전월 대비</p>
                  </div>
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <h4 className="font-semibold mb-2">순이익 증가율</h4>
                    <p className="text-2xl font-bold text-kpi-purple">+15.3%</p>
                    <p className="text-sm text-gray-600">전월 대비</p>
                  </div>
                </div>

                <div className="space-y-4">
                  <h4 className="font-semibold">주요 분석 포인트</h4>
                  <ul className="space-y-2 text-sm text-gray-700">
                    <li className="flex items-start gap-2">
                      <div className="w-2 h-2 bg-kpi-green rounded-full mt-2"></div>
                      <span>이번달 매출이 전월 대비 12.5% 증가했습니다. 주말 매출 증가가 주요 원인입니다.</span>
                    </li>
                    <li className="flex items-start gap-2">
                      <div className="w-2 h-2 bg-kpi-orange rounded-full mt-2"></div>
                      <span>재료비 비중이 45%로 적정 수준을 유지하고 있습니다.</span>
                    </li>
                    <li className="flex items-start gap-2">
                      <div className="w-2 h-2 bg-kpi-red rounded-full mt-2"></div>
                      <span>인건비 절감을 통해 전체 지출이 3.1% 감소했습니다.</span>
                    </li>
                    <li className="flex items-start gap-2">
                      <div className="w-2 h-2 bg-kpi-purple rounded-full mt-2"></div>
                      <span>순이익률 35.5%로 업계 평균보다 높은 수준을 기록했습니다.</span>
                    </li>
                  </ul>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* 지출 등록 모달 */}
      <FormModal
        isOpen={showExpenseModal}
        onClose={() => setShowExpenseModal(false)}
        onSubmit={handleAddExpense}
        title="지출 등록"
        fields={expenseFormFields}
      />
    </div>
  );
}