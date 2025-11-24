import React, { useState, useEffect } from 'react';
import { Calculator, DollarSign, Clock, Plus, Edit, Trash2, Download, Users, TrendingUp } from 'lucide-react';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Card } from '../ui/card';
import { Badge } from '../ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { FormModal } from '../Common/FormModal';
// import type { FormField } from '../Common/FormModal'; // ❌ 필요 없음: 로컬 타입으로 대체
import { ConfirmDialog } from '../Common/ConfirmDialog';
import { ChartWrapper } from '../Common/ChartWrapper';
import { toast } from 'sonner';

/** =========================
 *  🔸 로컬 FormField 타입 정의
 *  (FormModal에서 타입을 내보내지 않아도 사용 가능)
 *  ========================= */
type BaseFieldType = 'text' | 'number' | 'email' | 'date' | 'month' | 'select';
type FieldOption = { value: string; label: string };

type CommonField = {
  name: string;
  label: string;
  type: BaseFieldType;
  required?: boolean;
  placeholder?: string;
};

type SelectField = CommonField & {
  type: 'select';
  options: FieldOption[];
};

export type FormField = SelectField | CommonField;

interface Payroll {
  id: string;
  staffName: string;
  staffId: string;
  position: string;
  employmentType: 'fulltime' | 'parttime';
  month: string;
  // 정규직 필드
  baseSalary?: number;
  overtime?: number;
  bonus?: number;
  // 파트타임 필드
  hourlyRate?: number;
  weeklyHolidayPay?: number;
  // 공통 필드
  deductions: number;
  totalPay: number;
  workHours: number;
  overtimeHours: number;
  actualPay?: number; // 파트타임용 실지급액
  status: 'draft' | 'approved' | 'paid';
}

// ✅ mockStaff에 유니온 타입 명시
interface StaffInfo {
  id: string;
  name: string;
  position: string;
  employmentType: 'fulltime' | 'parttime';
  baseSalary: number;
  hourlyRate: number;
}

const mockStaff: StaffInfo[] = [
  { id: '1', name: '김철수', position: '매장 매니저', employmentType: 'fulltime', baseSalary: 2800000, hourlyRate: 0 },
  { id: '2', name: '이영희', position: '주방장', employmentType: 'fulltime', baseSalary: 2500000, hourlyRate: 0 },
  { id: '3', name: '박민수', position: '홀 서빙', employmentType: 'parttime', baseSalary: 0, hourlyRate: 12000 },
  { id: '4', name: '최지은', position: '캐셔', employmentType: 'parttime', baseSalary: 0, hourlyRate: 10000 },
  { id: '5', name: '정수빈', position: '주방보조', employmentType: 'parttime', baseSalary: 0, hourlyRate: 9620 }
];

const mockPayrolls: Payroll[] = [
  {
    id: '1',
    staffName: '김철수',
    staffId: '1',
    position: '매장 매니저',
    employmentType: 'fulltime',
    month: '2024-01',
    baseSalary: 2800000,
    overtime: 300000,
    bonus: 200000,
    deductions: 180000,
    totalPay: 3120000,
    workHours: 160,
    overtimeHours: 20,
    status: 'paid'
  },
  {
    id: '2',
    staffName: '이영희',
    staffId: '2',
    position: '주방장',
    employmentType: 'fulltime',
    month: '2024-01',
    baseSalary: 2500000,
    overtime: 250000,
    bonus: 150000,
    deductions: 160000,
    totalPay: 2740000,
    workHours: 160,
    overtimeHours: 15,
    status: 'paid'
  },
  {
    id: '3',
    staffName: '박민수',
    staffId: '3',
    position: '홀 서빙',
    employmentType: 'parttime',
    month: '2024-01',
    hourlyRate: 12000,
    weeklyHolidayPay: 96000,
    overtime: 48000,
    deductions: 50000,
    workHours: 120,
    overtimeHours: 8,
    totalPay: 1534000,
    actualPay: 1484000,
    status: 'approved'
  },
  {
    id: '4',
    staffName: '최지은',
    staffId: '4',
    position: '캐셔',
    employmentType: 'parttime',
    month: '2024-01',
    hourlyRate: 10000,
    weeklyHolidayPay: 80000,
    overtime: 30000,
    deductions: 40000,
    workHours: 100,
    overtimeHours: 6,
    totalPay: 1170000,
    actualPay: 1130000,
    status: 'draft'
  }
];

export function StaffPayroll() {
  const [selectedStaffType, setSelectedStaffType] = useState<'fulltime' | 'parttime' | null>(null);

  // ✅ 폼 필드 생성 함수는 FormField[] 반환, 내부 배열도 모두 FormField[]
  const getPayrollFormFields = (
    staffId?: string,
    employmentType?: 'fulltime' | 'parttime'
  ): FormField[] => {
    const selected = staffId ? mockStaff.find(s => s.id === staffId) : null;
    const empType = employmentType || selected?.employmentType || selectedStaffType || undefined;

    const baseFields: FormField[] = [
      {
        name: 'staffId',
        label: '직원',
        type: 'select',
        required: true,
        options: mockStaff.map(staff => ({
          value: staff.id,
          label: `${staff.name} (${staff.position}) - ${staff.employmentType === 'fulltime' ? '정규직' : '파트타임'}`
        }))
      },
      { name: 'month', label: '급여월', type: 'month', required: true }
    ];

    let salaryFields: FormField[] = [];

    if (empType === 'fulltime') {
      salaryFields = [
        { name: 'baseSalary', label: '기본급', type: 'number', required: true, placeholder: '기본급을 입력하세요' },
        { name: 'overtime', label: '연장근무수당', type: 'number', placeholder: '연장근무수당을 입력하세요' },
        { name: 'bonus', label: '상여금', type: 'number', placeholder: '상여금을 입력하세요' }
      ];
    } else if (empType === 'parttime') {
      salaryFields = [
        { name: 'hourlyRate', label: '시급', type: 'number', required: true, placeholder: '시급을 입력하세요' },
        { name: 'workHours', label: '근무시간', type: 'number', required: true, placeholder: '근무시간을 입력하세요' },
        { name: 'overtime', label: '연장근무수당', type: 'number', placeholder: '연장근무수당을 입력하세요' },
        { name: 'weeklyHolidayPay', label: '주휴수당', type: 'number', placeholder: '주휴수당을 입력하세요' }
      ];
    }

    const commonFields: FormField[] = [
      { name: 'deductions', label: '공제액', type: 'number', placeholder: '공제액을 입력하세요' },
      ...(empType === 'fulltime'
        ? [{ name: 'workHours', label: '근무시간', type: 'number', required: true, placeholder: '근무시간을 입력하세요' } as FormField]
        : []),
      { name: 'overtimeHours', label: '연장근무시간', type: 'number', placeholder: '연장근무시간을 입력하세요' },
      ...(empType === 'parttime'
        ? [{ name: 'actualPay', label: '실지급액', type: 'number', placeholder: '실지급액을 입력하세요' } as FormField]
        : []),
      {
        name: 'status',
        label: '상태',
        type: 'select',
        required: true,
        options: [
          { value: 'draft', label: '임시저장' },
          { value: 'approved', label: '승인됨' },
          { value: 'paid', label: '지급완료' }
        ]
      }
    ];

    return [...baseFields, ...salaryFields, ...commonFields];
  };

  const [payrolls, setPayrolls] = useState<Payroll[]>(mockPayrolls);
  const [selectedMonth, setSelectedMonth] = useState('2024-01');
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [editingPayroll, setEditingPayroll] = useState<Payroll | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
  const [dynamicFormFields, setDynamicFormFields] = useState<FormField[]>(getPayrollFormFields());
  const [initialFormData, setInitialFormData] = useState<Record<string, unknown>>({}); // ✅ 타입 명시

  // 모달 열릴 때 폼 필드 초기화
  useEffect(() => {
    if (isAddModalOpen) {
      setDynamicFormFields(getPayrollFormFields());
      setSelectedStaffType(null);
      setInitialFormData({});
    }
  }, [isAddModalOpen]);

  // 수정 모달 열릴 때 폼 필드 설정
  useEffect(() => {
    if (editingPayroll) {
      const fields = getPayrollFormFields(editingPayroll.staffId, editingPayroll.employmentType);
      setDynamicFormFields(fields);
      setSelectedStaffType(editingPayroll.employmentType);
    }
  }, [editingPayroll]);

  // 필터링된 급여 목록
  const filteredPayrolls = payrolls.filter(payroll => {
    const matchesMonth = payroll.month === selectedMonth;
    const matchesSearch =
      payroll.staffName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      payroll.position.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = filterStatus === 'all' || payroll.status === filterStatus;

    return matchesMonth && matchesSearch && matchesStatus;
  });

  // 상태별 스타일
  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'draft':
        return <Badge variant="outline">임시저장</Badge>;
      case 'approved':
        return <Badge className="bg-blue-100 text-blue-800">승인됨</Badge>;
      case 'paid':
        return <Badge className="bg-green-100 text-green-800">지급완료</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  // 급여 추가
  const handleAddPayroll = (data: any) => {
    const staff = mockStaff.find(s => s.id === data.staffId);
    let totalPay = 0;

    if (staff?.employmentType === 'fulltime') {
      totalPay = (data.baseSalary || 0) + (data.overtime || 0) + (data.bonus || 0) - (data.deductions || 0);
    } else {
      // 파트타임
      const basicPay = (data.hourlyRate || 0) * (data.workHours || 0);
      totalPay = basicPay + (data.overtime || 0) + (data.weeklyHolidayPay || 0) - (data.deductions || 0);
    }

    const newPayroll: Payroll = {
      id: Date.now().toString(),
      staffName: staff?.name || '',
      staffId: data.staffId,
      position: staff?.position || '',
      employmentType: (staff?.employmentType ?? 'fulltime') as 'fulltime' | 'parttime', // ✅
      month: data.month,
      // 정규직 필드
      baseSalary: staff?.employmentType === 'fulltime' ? Number(data.baseSalary || 0) : undefined,
      overtime: Number(data.overtime || 0),
      bonus: staff?.employmentType === 'fulltime' ? Number(data.bonus || 0) : undefined,
      // 파트타임 필드
      hourlyRate: staff?.employmentType === 'parttime' ? Number(data.hourlyRate || 0) : undefined,
      weeklyHolidayPay: staff?.employmentType === 'parttime' ? Number(data.weeklyHolidayPay || 0) : undefined,
      // 공통 필드
      deductions: Number(data.deductions || 0),
      totalPay,
      workHours: Number(data.workHours || 0),
      overtimeHours: Number(data.overtimeHours || 0),
      actualPay: staff?.employmentType === 'parttime' ? Number(data.actualPay || totalPay) : undefined,
      status: data.status
    };
    setPayrolls([...payrolls, newPayroll]);
    setIsAddModalOpen(false);
    setSelectedStaffType(null);
    toast.success('급여 정보가 추가되었습니다.');
  };

  // 급여 수정
  const handleEditPayroll = (data: any) => {
    const staff = mockStaff.find(s => s.id === data.staffId);
    let totalPay = 0;

    if (staff?.employmentType === 'fulltime') {
      totalPay = (data.baseSalary || 0) + (data.overtime || 0) + (data.bonus || 0) - (data.deductions || 0);
    } else {
      const basicPay = (data.hourlyRate || 0) * (data.workHours || 0);
      totalPay = basicPay + (data.overtime || 0) + (data.weeklyHolidayPay || 0) - (data.deductions || 0);
    }

    setPayrolls(
      payrolls.map(payroll =>
        payroll.id === editingPayroll?.id
          ? {
              ...payroll,
              staffName: staff?.name || payroll.staffName,
              position: staff?.position || payroll.position,
              employmentType: (staff?.employmentType ?? payroll.employmentType) as 'fulltime' | 'parttime', // ✅
              month: data.month,
              // 정규직 필드
              baseSalary: staff?.employmentType === 'fulltime' ? Number(data.baseSalary || 0) : undefined,
              overtime: Number(data.overtime || 0),
              bonus: staff?.employmentType === 'fulltime' ? Number(data.bonus || 0) : undefined,
              // 파트타임 필드
              hourlyRate: staff?.employmentType === 'parttime' ? Number(data.hourlyRate || 0) : undefined,
              weeklyHolidayPay: staff?.employmentType === 'parttime' ? Number(data.weeklyHolidayPay || 0) : undefined,
              // 공통 필드
              deductions: Number(data.deductions || 0),
              totalPay,
              workHours: Number(data.workHours || 0),
              overtimeHours: Number(data.overtimeHours || 0),
              actualPay: staff?.employmentType === 'parttime' ? Number(data.actualPay || totalPay) : undefined,
              status: data.status
            }
          : payroll
      )
    );
    setEditingPayroll(null);
    toast.success('급여 정보가 수정되었습니다.');
  };

  // 급여 삭제
  const handleDeletePayroll = (id: string) => {
    setPayrolls(payrolls.filter(payroll => payroll.id !== id));
    setDeleteConfirm(null);
    toast.success('급여 정보가 삭제되었습니다.');
  };

  // 폼 필드 변경 핸들러
  const handleFormFieldChange = (fieldName: string, value: any) => {
    if (fieldName === 'staffId' && value) {
      const selected = mockStaff.find(s => s.id === value);
      if (selected) {
        setSelectedStaffType(selected.employmentType as 'fulltime' | 'parttime'); // ✅
        setDynamicFormFields(getPayrollFormFields(value, selected.employmentType));

        const currentMonth = new Date().toISOString().slice(0, 7);
        const defaultFormData: Record<string, unknown> = {
          staffId: value,
          month: currentMonth,
          ...(selected.employmentType === 'fulltime'
            ? { baseSalary: selected.baseSalary || 0, overtime: 0, bonus: 0, workHours: 160 }
            : { hourlyRate: selected.hourlyRate || 0, workHours: 80, overtime: 0, weeklyHolidayPay: 0, actualPay: 0 }),
          deductions: 0,
          overtimeHours: 0,
          status: 'draft'
        };
        setInitialFormData(defaultFormData);
      }
    }
  };

  const handleDownloadPayslip = (payroll: Payroll) => {
    toast.success(`${payroll.staffName}님의 급여명세서 다운로드를 시작합니다.`);
  };

  const totalPayroll = filteredPayrolls.reduce((sum, p) => sum + p.totalPay, 0);
  const avgPayroll = filteredPayrolls.length > 0 ? totalPayroll / filteredPayrolls.length : 0;

  const payrollChartData = filteredPayrolls.map(payroll => {
    if (payroll.employmentType === 'fulltime') {
      return {
        name: payroll.staffName,
        기본급: payroll.baseSalary || 0,
        연장수당: payroll.overtime || 0,
        상여금: payroll.bonus || 0,
        공제액: -(payroll.deductions || 0),
        실지급액: payroll.totalPay
      };
    } else {
      const basicPay = (payroll.hourlyRate || 0) * (payroll.workHours || 0);
      return {
        name: payroll.staffName,
        시간급여: basicPay,
        연장수당: payroll.overtime || 0,
        주휴수당: payroll.weeklyHolidayPay || 0,
        공제액: -(payroll.deductions || 0),
        실지급액: payroll.actualPay || payroll.totalPay
      };
    }
  });

  const yKeys =
    filteredPayrolls.length > 0 && filteredPayrolls.some(p => p.employmentType === 'parttime')
      ? ['시간급여', '연장수당', '주휴수당', '공제액']
      : ['기본급', '연장수당', '상여금', '공제액'];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Calculator className="w-6 h-6 text-kpi-orange" />
          <h1>급여 관리</h1>
        </div>

        <Button onClick={() => setIsAddModalOpen(true)} className="gap-2">
          <Plus className="w-4 h-4" />
          급여 추가
        </Button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card className="p-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-kpi-orange/10 rounded-lg flex items-center justify-center">
              <DollarSign className="w-6 h-6 text-kpi-orange" />
            </div>
            <div>
              <p className="text-sm text-dark-gray">총 급여 지출</p>
              <p className="text-2xl font-semibold">{totalPayroll.toLocaleString()}원</p>
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-kpi-purple/10 rounded-lg flex items-center justify-center">
              <Users className="w-6 h-6 text-kpi-purple" />
            </div>
            <div>
              <p className="text-sm text-dark-gray">평균 급여</p>
              <p className="text-2xl font-semibold">{Math.round(avgPayroll).toLocaleString()}원</p>
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-kpi-green/10 rounded-lg flex items-center justify-center">
              <TrendingUp className="w-6 h-6 text-kpi-green" />
            </div>
            <div>
              <p className="text-sm text-dark-gray">지급완료</p>
              <p className="text-2xl font-semibold">{filteredPayrolls.filter(p => p.status === 'paid').length}명</p>
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
              <Clock className="w-6 h-6 text-blue-600" />
            </div>
            <div>
              <p className="text-sm text-dark-gray">승인대기</p>
              <p className="text-2xl font-semibold">{filteredPayrolls.filter(p => p.status === 'draft').length}명</p>
            </div>
          </div>
        </Card>
      </div>

      {/* Filters */}
      <Card className="p-4">
        <div className="flex flex-col md:flex-row gap-4">
          <Select value={selectedMonth} onValueChange={setSelectedMonth}>
            <SelectTrigger className="w-full md:w-48">
              <SelectValue placeholder="급여월 선택" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="2024-01">2024년 1월</SelectItem>
              <SelectItem value="2023-12">2023년 12월</SelectItem>
              <SelectItem value="2023-11">2023년 11월</SelectItem>
            </SelectContent>
          </Select>

          <div className="flex-1">
            <Input
              placeholder="직원명, 직책으로 검색..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
            />
          </div>

          <Select value={filterStatus} onValueChange={setFilterStatus}>
            <SelectTrigger className="w-full md:w-48">
              <SelectValue placeholder="상태 필터" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">전체 상태</SelectItem>
              <SelectItem value="draft">임시저장</SelectItem>
              <SelectItem value="approved">승인됨</SelectItem>
              <SelectItem value="paid">지급완료</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </Card>

      {/* Payroll Chart */}
      <Card className="p-6">
        <h3 className="font-semibold mb-4">급여 구성 현황</h3>
        {/* ChartWrapper 타입은 변경하지 않고 최소 캐스팅만 */}
        <ChartWrapper
          {...({
            data: payrollChartData,
            type: 'bar',
            xKey: 'name',
            yKeys,
            colors: ['#06D6A0', '#F77F00', '#9D4EDD', '#FF6B6B']
          } as any)}
        />
      </Card>

      {/* Payroll List */}
      <Card>
        <div className="p-6">
          <div className="space-y-4">
            {filteredPayrolls.map(payroll => (
              <Card key={payroll.id} className="p-4 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-4 mb-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="font-semibold">{payroll.staffName}</h3>
                          <Badge
                            variant="outline"
                            className={`text-xs ${
                              payroll.employmentType === 'fulltime'
                                ? 'bg-blue-50 text-blue-700'
                                : 'bg-orange-50 text-orange-700'
                            }`}
                          >
                            {payroll.employmentType === 'fulltime' ? '정규직' : '파트타임'}
                          </Badge>
                        </div>
                        <p className="text-sm text-dark-gray">{payroll.position}</p>
                      </div>
                      {getStatusBadge(payroll.status)}
                    </div>

                    {payroll.employmentType === 'fulltime' ? (
                      <div className="grid grid-cols-2 md:grid-cols-6 gap-4 text-sm">
                        <div>
                          <p className="text-dark-gray">기본급</p>
                          <p className="font-semibold">{(payroll.baseSalary || 0).toLocaleString()}원</p>
                        </div>
                        <div>
                          <p className="text-dark-gray">연장수당</p>
                          <p className="font-semibold">{(payroll.overtime || 0).toLocaleString()}원</p>
                        </div>
                        <div>
                          <p className="text-dark-gray">상여금</p>
                          <p className="font-semibold">{(payroll.bonus || 0).toLocaleString()}원</p>
                        </div>
                        <div>
                          <p className="text-dark-gray">공제액</p>
                          <p className="font-semibold text-red-600">-{payroll.deductions.toLocaleString()}원</p>
                        </div>
                        <div>
                          <p className="text-dark-gray">근무시간</p>
                          <p className="font-semibold">
                            {payroll.workHours}h (+{payroll.overtimeHours}h)
                          </p>
                        </div>
                        <div>
                          <p className="text-dark-gray">실지급액</p>
                          <p className="font-semibold text-kpi-green">
                            {payroll.totalPay.toLocaleString()}원
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div className="grid grid-cols-2 md:grid-cols-6 gap-4 text-sm">
                        <div>
                          <p className="text-dark-gray">시급</p>
                          <p className="font-semibold">{(payroll.hourlyRate || 0).toLocaleString()}원</p>
                        </div>
                        <div>
                          <p className="text-dark-gray">근무시간</p>
                          <p className="font-semibold">
                            {payroll.workHours}h (+{payroll.overtimeHours}h)
                          </p>
                        </div>
                        <div>
                          <p className="text-dark-gray">연장근무수당</p>
                          <p className="font-semibold">{(payroll.overtime || 0).toLocaleString()}원</p>
                        </div>
                        <div>
                          <p className="text-dark-gray">주휴수당</p>
                          <p className="font-semibold">{(payroll.weeklyHolidayPay || 0).toLocaleString()}원</p>
                        </div>
                        <div>
                          <p className="text-dark-gray">공제액</p>
                          <p className="font-semibold text-red-600">-{payroll.deductions.toLocaleString()}원</p>
                        </div>
                        <div>
                          <p className="text-dark-gray">실지급액</p>
                          <p className="font-semibold text-kpi-green">
                            {(payroll.actualPay || payroll.totalPay).toLocaleString()}원
                          </p>
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="flex gap-2 ml-4">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleDownloadPayslip(payroll)}
                      className="gap-1"
                    >
                      <Download className="w-3 h-3" />
                      명세서
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setEditingPayroll(payroll)}
                      className="gap-1"
                    >
                      <Edit className="w-3 h-3" />
                      수정
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setDeleteConfirm(payroll.id)}
                      className="gap-1 text-red-600 hover:text-red-700"
                    >
                      <Trash2 className="w-3 h-3" />
                      삭제
                    </Button>
                  </div>
                </div>
              </Card>
            ))}
          </div>

          {filteredPayrolls.length === 0 && (
            <div className="text-center py-8 text-dark-gray">해당 조건에 맞는 급여 정보가 없습니다.</div>
          )}
        </div>
      </Card>

      {/* Add Payroll Modal */}
      <FormModal
        key={`add-payroll-${selectedStaffType || 'default'}`}
        isOpen={isAddModalOpen}
        onClose={() => {
          setIsAddModalOpen(false);
          setSelectedStaffType(null);
          setDynamicFormFields(getPayrollFormFields());
          setInitialFormData({});
        }}
        onSubmit={handleAddPayroll}
        title="급여 추가"
        fields={dynamicFormFields}
        initialData={initialFormData}
        onChange={handleFormFieldChange}
      />

      {/* Edit Payroll Modal */}
      <FormModal
        isOpen={!!editingPayroll}
        onClose={() => setEditingPayroll(null)}
        onSubmit={handleEditPayroll}
        title="급여 정보 수정"
        fields={dynamicFormFields}
        initialData={editingPayroll ? (editingPayroll as unknown as Record<string, unknown>) : undefined} // ✅ 캐스팅
        onChange={handleFormFieldChange}
      />

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={!!deleteConfirm}
        onClose={() => setDeleteConfirm(null)}
        onConfirm={() => deleteConfirm && handleDeletePayroll(deleteConfirm)}
        title="급여 정보 삭제"
        description="이 급여 정보를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다."
        confirmText="삭제"
        cancelText="취소"
      />
    </div>
  );
}
