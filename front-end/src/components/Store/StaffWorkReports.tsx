import React, { useState } from 'react';
import { BarChart3, Clock, Calendar, User, TrendingUp, Download, Filter } from 'lucide-react';
import { Button } from '../ui/button';
import { Card } from '../ui/card';
import { Badge } from '../ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { ChartWrapper } from '../Common/ChartWrapper';
// import { DatePicker } from '../ui/calendar';

interface WorkReport {
  staffId: string;
  staffName: string;
  position: string;
  date: string;
  clockIn: string;
  clockOut: string;
  workHours: number;
  breakTime: number;
  overtimeHours: number;
  efficiency: number;
  status: 'present' | 'late' | 'absent' | 'early_leave';
}

const mockWorkReports: WorkReport[] = [
  {
    staffId: '1',
    staffName: '김철수',
    position: '매장 매니저',
    date: '2024-01-15',
    clockIn: '09:00',
    clockOut: '18:00',
    workHours: 8,
    breakTime: 1,
    overtimeHours: 0,
    efficiency: 95,
    status: 'present'
  },
  {
    staffId: '2',
    staffName: '이영희',
    position: '주방장',
    date: '2024-01-15',
    clockIn: '10:15',
    clockOut: '19:00',
    workHours: 7.75,
    breakTime: 1,
    overtimeHours: 0,
    efficiency: 88,
    status: 'late'
  },
  {
    staffId: '3',
    staffName: '박민수',
    position: '홀 서빙',
    date: '2024-01-15',
    clockIn: '14:00',
    clockOut: '23:00',
    workHours: 8,
    breakTime: 1,
    overtimeHours: 1,
    efficiency: 92,
    status: 'present'
  },
  {
    staffId: '4',
    staffName: '최지은',
    position: '캐셔',
    date: '2024-01-15',
    clockIn: '18:00',
    clockOut: '01:30',
    workHours: 6.5,
    breakTime: 1,
    overtimeHours: 0,
    efficiency: 90,
    status: 'early_leave'
  }
];

const mockStaff = [
  { id: '1', name: '김철수', position: '매장 매니저' },
  { id: '2', name: '이영희', position: '주방장' },
  { id: '3', name: '박민수', position: '홀 서빙' },
  { id: '4', name: '최지은', position: '캐셔' }
];

// 차트 데이터 타입 정의
interface ChartData {
  name: string;
  [key: string]: number | string;  // 추가적인 값들이 있을 수 있으므로 이를 포함
}

export function StaffWorkReports() {
  const [reports, setReports] = useState<WorkReport[]>(mockWorkReports);
  const [selectedPeriod, setSelectedPeriod] = useState<'day' | 'week' | 'month'>('week');
  const [selectedStaff, setSelectedStaff] = useState<string>('all');
  const [selectedDate, setSelectedDate] = useState('2024-01-15');

  // 필터링된 리포트
  const filteredReports = reports.filter(report => {
    const matchesStaff = selectedStaff === 'all' || report.staffId === selectedStaff;
    const matchesDate = report.date === selectedDate;
    return matchesStaff && matchesDate;
  });

  // 차트 데이터 (타입 맞추기)
  const workHoursChartData: ChartData[] = filteredReports.map(report => ({
    name: report.staffName,
    '정규근무': report.workHours - report.overtimeHours,
    '연장근무': report.overtimeHours,
    '휴게시간': report.breakTime
  }));

  const efficiencyChartData: ChartData[] = filteredReports.map(report => ({
    name: report.staffName,
    efficiency: report.efficiency
  }));

  const attendanceData: ChartData[] = [
    { name: '정상출근', value: filteredReports.filter(r => r.status === 'present').length },
    { name: '지각', value: filteredReports.filter(r => r.status === 'late').length },
    { name: '조퇴', value: filteredReports.filter(r => r.status === 'early_leave').length },
    { name: '결근', value: filteredReports.filter(r => r.status === 'absent').length }
  ];

 function getStatusBadge(status: WorkReport['status']): React.ReactNode {
  const meta: Record<WorkReport['status'], { text: string; cls: string }> = {
    present:     { text: '정상출근', cls: 'bg-green-100 text-green-700 border-green-200' },
    late:        { text: '지각',     cls: 'bg-amber-100 text-amber-700 border-amber-200' },
    early_leave: { text: '조퇴',     cls: 'bg-purple-100 text-purple-700 border-purple-200' },
    absent:      { text: '결근',     cls: 'bg-red-100 text-red-700 border-red-200' },
  };

  const m = meta[status] ?? { text: status, cls: 'bg-gray-100 text-gray-700 border-gray-200' };
  return <Badge className={`border ${m.cls}`}>{m.text}</Badge>;
}

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <BarChart3 className="w-6 h-6 text-kpi-red" />
          <h1>근무리포트</h1>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="outline" className="gap-2">
            <Download className="w-4 h-4" />
            리포트 다운로드
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card className="p-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-kpi-red/10 rounded-lg flex items-center justify-center">
              <Clock className="w-6 h-6 text-kpi-red" />
            </div>
            <div>
              <p className="text-sm text-dark-gray">총 근무시간</p>
              <p className="text-2xl font-semibold">{filteredReports.reduce((sum, r) => sum + r.workHours, 0)}시간</p>
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-kpi-orange/10 rounded-lg flex items-center justify-center">
              <TrendingUp className="w-6 h-6 text-kpi-orange" />
            </div>
            <div>
              <p className="text-sm text-dark-gray">연장근무</p>
              <p className="text-2xl font-semibold">{filteredReports.reduce((sum, r) => sum + r.overtimeHours, 0)}시간</p>
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-kpi-green/10 rounded-lg flex items-center justify-center">
              <BarChart3 className="w-6 h-6 text-kpi-green" />
            </div>
            <div>
              <p className="text-sm text-dark-gray">평균 효율성</p>
              <p className="text-2xl font-semibold">
                {Math.round(filteredReports.reduce((sum, r) => sum + r.efficiency, 0) / filteredReports.length) || 0}%
              </p>
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-kpi-purple/10 rounded-lg flex items-center justify-center">
              <User className="w-6 h-6 text-kpi-purple" />
            </div>
            <div>
              <p className="text-sm text-dark-gray">출근율</p>
              <p className="text-2xl font-semibold">
                {Math.round((filteredReports.filter(r => r.status === 'present').length / filteredReports.length) * 100) || 0}%
              </p>
            </div>
          </div>
        </Card>
      </div>

      {/* Filters */}
      <Card className="p-4">
        <div className="flex flex-col md:flex-row gap-4">
          <Select value={selectedPeriod} onValueChange={(value: any) => setSelectedPeriod(value)}>
            <SelectTrigger className="w-full md:w-48">
              <SelectValue placeholder="기간 선택" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="day">일간</SelectItem>
              <SelectItem value="week">주간</SelectItem>
              <SelectItem value="month">월간</SelectItem>
            </SelectContent>
          </Select>

          <Select value={selectedStaff} onValueChange={setSelectedStaff}>
            <SelectTrigger className="w-full md:w-48">
              <SelectValue placeholder="직원 선택" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">전체 직원</SelectItem>
              {mockStaff.map(staff => (
                <SelectItem key={staff.id} value={staff.id}>
                  {staff.name} ({staff.position})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select value={selectedDate} onValueChange={setSelectedDate}>
            <SelectTrigger className="w-full md:w-48">
              <SelectValue placeholder="날짜 선택" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="2024-01-15">2024-01-15</SelectItem>
              <SelectItem value="2024-01-14">2024-01-14</SelectItem>
              <SelectItem value="2024-01-13">2024-01-13</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </Card>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Work Hours Chart */}
        {/* <Card className="p-6">
          <h3 className="font-semibold mb-4">근무시간 분석</h3>
          <ChartWrapper
            data={workHoursChartData}
            type="bar"
            xKey="name"
            yKeys={['정규근무', '연장근무']}
            colors={['#06D6A0', '#F77F00']}
          />
        </Card> */}

        {/* Efficiency Chart */}
        {/* <Card className="p-6">
          <h3 className="font-semibold mb-4">업무 효율성</h3>
          <ChartWrapper
            data={efficiencyChartData}
            type="bar"
            xKey="name"
            yKeys={['efficiency']}
            colors={['#9D4EDD']}
          />
        </Card> */}
      </div>

      {/* Attendance Chart */}
      {/* <Card className="p-6">
        <h3 className="font-semibold mb-4">출근 현황</h3>
        <ChartWrapper
          data={attendanceData}
          type="pie"
          xKey="name"
          yKeys={['value']}
          colors={['#06D6A0', '#F77F00', '#FF6B6B', '#9D4EDD']}
        />
      </Card> */}

      {/* Work Report Details */}
      <Card>
        <div className="p-6">
          <h3 className="font-semibold mb-4">근무 상세 내역</h3>
          <div className="space-y-4">
            {filteredReports.map((report, index) => (
              <Card key={index} className="p-4 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-4 mb-3">
                      <div>
                        <h4 className="font-semibold">{report.staffName}</h4>
                        <p className="text-sm text-dark-gray">{report.position}</p>
                      </div>
                      {getStatusBadge(report.status)}
                    </div>

                    <div className="grid grid-cols-2 md:grid-cols-6 gap-4 text-sm">
                      <div>
                        <p className="text-dark-gray">출근시간</p>
                        <p className="font-semibold">{report.clockIn}</p>
                      </div>
                      <div>
                        <p className="text-dark-gray">퇴근시간</p>
                        <p className="font-semibold">{report.clockOut}</p>
                      </div>
                      <div>
                        <p className="text-dark-gray">근무시간</p>
                        <p className="font-semibold">{report.workHours}시간</p>
                      </div>
                      <div>
                        <p className="text-dark-gray">연장근무</p>
                        <p className="font-semibold text-kpi-orange">{report.overtimeHours}시간</p>
                      </div>
                      <div>
                        <p className="text-dark-gray">휴게시간</p>
                        <p className="font-semibold">{report.breakTime}시간</p>
                      </div>
                      <div>
                        <p className="text-dark-gray">업무효율성</p>
                        <p className="font-semibold text-kpi-green">{report.efficiency}%</p>
                      </div>
                    </div>
                  </div>
                </div>
              </Card>
            ))}
          </div>

          {filteredReports.length === 0 && (
            <div className="text-center py-8 text-dark-gray">
              해당 조건에 맞는 근무 기록이 없습니다.
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
