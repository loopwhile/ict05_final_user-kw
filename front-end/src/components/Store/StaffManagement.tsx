import React, { useState } from 'react';
import { KPICard } from '../Common/KPICard';
import { DataTable } from '../Common/DataTable';
import { FormModal } from '../Common/FormModal';
import { StatusBadge } from '../Common/StatusBadge';
import { ConfirmDialog } from '../Common/ConfirmDialog';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Badge } from '../ui/badge';
import { Avatar, AvatarFallback } from '../ui/avatar';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { 
  Users, 
  UserPlus, 
  UserCheck, 
  UserX, 
  Search,
  Phone,
  Mail,
  Calendar,
  Clock,
  Edit3,
  Trash2,
  DollarSign,
  TrendingUp,
  CalendarDays
} from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, BarChart, Bar } from 'recharts';
import { toast } from 'sonner';

// 직원 데이터 (임시)
const storeStaffData = [
  {
    id: 1,
    name: '김민준',
    phone: '010-1111-2222',
    email: 'kim.mj@store.com',
    position: '매니저',
    hourlyRate: 12000,
    workType: '정규직',
    status: '근무중',
    joinDate: '2023-03-15',
    totalHours: 180,
    thisMonthHours: 42,
    avatar: 'KM',
    lastWorkDate: '2024-01-05',
  },
  {
    id: 2,
    name: '이서연',
    phone: '010-2222-3333',
    email: 'lee.sy@store.com',
    position: '서빙',
    hourlyRate: 9500,
    workType: '파트타임',
    status: '근무중',
    joinDate: '2023-08-20',
    totalHours: 120,
    thisMonthHours: 28,
    avatar: 'LS',
    lastWorkDate: '2024-01-05',
  },
  {
    id: 3,
    name: '박준혁',
    phone: '010-3333-4444',
    email: 'park.jh@store.com',
    position: '주방',
    hourlyRate: 11000,
    workType: '파트타임',
    status: '근무중',
    joinDate: '2023-11-10',
    totalHours: 95,
    thisMonthHours: 35,
    avatar: 'PJ',
    lastWorkDate: '2024-01-04',
  },
  {
    id: 4,
    name: '최유진',
    phone: '010-4444-5555',
    email: 'choi.yj@store.com',
    position: '서빙',
    hourlyRate: 9500,
    workType: '파트타임',
    status: '휴가',
    joinDate: '2023-12-01',
    totalHours: 45,
    thisMonthHours: 0,
    avatar: 'CY',
    lastWorkDate: '2023-12-28',
  },
  {
    id: 5,
    name: '정도현',
    phone: '010-5555-6666',
    email: 'jung.dh@store.com',
    position: '캐셔',
    hourlyRate: 10000,
    workType: '파트타임',
    status: '퇴사',
    joinDate: '2023-05-20',
    totalHours: 280,
    thisMonthHours: 0,
    avatar: 'JD',
    lastWorkDate: '2023-12-15',
  },
];

// 근무 시간 데이터 (주별)
const weeklyHoursData = [
  { week: '1주차', totalHours: 156, staffCount: 4 },
  { week: '2주차', totalHours: 172, staffCount: 4 },
  { week: '3주차', totalHours: 145, staffCount: 4 },
  { week: '4주차', totalHours: 168, staffCount: 4 },
];

// 포지션별 통계
const positionStats = [
  { name: '매니저', count: 1, avgRate: 12000, color: '#FF6B6B' },
  { name: '주방', count: 1, avgRate: 11000, color: '#F77F00' },
  { name: '캐셔', count: 1, avgRate: 10000, color: '#06D6A0' },
  { name: '서빙', count: 2, avgRate: 9500, color: '#9D4EDD' },
];

export function StoreStaffManagement() {
  const [staff, setStaff] = useState(storeStaffData);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [selectedStaff, setSelectedStaff] = useState<any>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterPosition, setFilterPosition] = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');

  const staffColumns = [
    {
      key: 'name',
      label: '이름',
      render: (value: any, staff: any) => (
        <div className="flex items-center gap-3">
          <Avatar className="w-8 h-8">
            <AvatarFallback className="bg-kpi-green text-white text-xs">
              {staff.avatar}
            </AvatarFallback>
          </Avatar>
          <div>
            <div className="font-medium">{staff.name}</div>
            <div className="text-xs text-gray-500">{staff.email}</div>
          </div>
        </div>
      ),
    },
    {
      key: 'position',
      label: '포지션',
      render: (value: any, staff: any) => (
        <div>
          <div className="font-medium">{staff.position}</div>
          <div className="text-xs text-gray-500">{staff.workType}</div>
        </div>
      ),
    },
    {
      key: 'hourlyRate',
      label: '시급',
      render: (value: any, staff: any) => (
        <div className="font-medium">
          ₩{(staff.hourlyRate || 0).toLocaleString()}/시간
        </div>
      ),
    },
    {
      key: 'status',
      label: '상태',
      render: (value: any, staff: any) => {
        const originalStatus = staff?.status || '일반';
        // 한글 상태를 StatusBadge에서 인식할 수 있는 영문 상태로 매핑
        const statusMapping: { [key: string]: string } = {
          '근무중': 'active',
          '휴가': 'warning',
          '퇴사': 'closed',
          '일반': 'normal'
        };
        
        const status = statusMapping[originalStatus] || 'normal';
        
        return <StatusBadge status={status} text={originalStatus} />;
      },
    },
    {
      key: 'contact',
      label: '연락처',
      render: (value: any, staff: any) => (
        <div className="text-xs">
          <div className="flex items-center gap-1">
            <Phone className="w-3 h-3" />
            {staff?.phone || '-'}
          </div>
        </div>
      ),
    },
    {
      key: 'workHours',
      label: '근무시간',
      render: (value: any, staff: any) => (
        <div className="text-xs">
          <div>이번달: {staff?.thisMonthHours || 0}h</div>
          <div className="text-gray-500">총 {staff?.totalHours || 0}h</div>
        </div>
      ),
    },
    {
      key: 'salary',
      label: '이번달 급여',
      render: (value: any, staff: any) => (
        <div className="font-medium text-kpi-green">
          ₩{((staff?.thisMonthHours || 0) * (staff?.hourlyRate || 0)).toLocaleString()}
        </div>
      ),
    },
    {
      key: 'lastWorkDate',
      label: '최근 근무',
      render: (value: any, staff: any) => (
        <div className="text-xs flex items-center gap-1">
          <Clock className="w-3 h-3" />
          {staff.lastWorkDate}
        </div>
      ),
    },
    {
      key: 'actions',
      label: '관리',
      render: (value: any, staff: any) => (
        <div className="flex gap-1">
          <Button
            size="sm"
            variant="ghost"
            onClick={() => handleEditStaff(staff)}
          >
            <Edit3 className="w-3 h-3" />
          </Button>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => handleDeleteStaff(staff)}
            className="text-red-600 hover:text-red-700"
          >
            <Trash2 className="w-3 h-3" />
          </Button>
        </div>
      ),
    },
  ];

  const staffFormFields = [
    {
      name: 'name',
      label: '이름',
      type: 'text' as const,
      required: true,
    },
    {
      name: 'phone',
      label: '연락처',
      type: 'tel' as const,
      required: true,
    },
    {
      name: 'email',
      label: '이메일',
      type: 'email' as const,
      required: true,
    },
    {
      name: 'position',
      label: '포지션',
      type: 'select' as const,
      options: [
        { value: '매니저', label: '매니저' },
        { value: '주방', label: '주방' },
        { value: '캐셔', label: '캐셔' },
        { value: '서빙', label: '서빙' },
        { value: '청소', label: '청소' },
      ],
      required: true,
    },
    {
      name: 'workType',
      label: '근무형태',
      type: 'select' as const,
      options: [
        { value: '정규직', label: '정규직' },
        { value: '파트타임', label: '파트타임' },
        { value: '계약직', label: '계약직' },
      ],
      required: true,
    },
    {
      name: 'hourlyRate',
      label: '시급',
      type: 'number' as const,
      required: true,
    },
    {
      name: 'joinDate',
      label: '입사일',
      type: 'date' as const,
      required: true,
    },
  ];

  const handleAddStaff = (formData: any) => {
    const newStaff = {
      id: staff.length + 1,
      ...formData,
      hourlyRate: Number(formData.hourlyRate),
      avatar: formData.name.substring(0, 2).toUpperCase(),
      status: '근무중',
      totalHours: 0,
      thisMonthHours: 0,
      lastWorkDate: new Date().toISOString().split('T')[0],
    };
    
    setStaff([...staff, newStaff]);
    setShowAddModal(false);
    toast.success('직원이 추가되었습니다.');
  };

  const handleEditStaff = (staffMember: any) => {
    setSelectedStaff(staffMember);
    setShowEditModal(true);
  };

  const handleUpdateStaff = (formData: any) => {
    setStaff(staff.map(s => 
      s.id === selectedStaff.id 
        ? { ...s, ...formData, hourlyRate: Number(formData.hourlyRate) }
        : s
    ));
    setShowEditModal(false);
    setSelectedStaff(null);
    toast.success('직원 정보가 수정되었습니다.');
  };

  const handleDeleteStaff = (staffMember: any) => {
    setSelectedStaff(staffMember);
    setShowDeleteDialog(true);
  };

  const confirmDelete = () => {
    setStaff(staff.filter(s => s.id !== selectedStaff.id));
    setShowDeleteDialog(false);
    setSelectedStaff(null);
    toast.success('직원이 삭제되었습니다.');
  };

  // 필터링된 직원 데이터
  const filteredStaff = staff.filter(s => {
    const matchesSearch = s.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         s.phone.includes(searchTerm) ||
                         s.position.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesPosition = filterPosition === 'all' || s.position === filterPosition;
    const matchesStatus = filterStatus === 'all' || s.status === filterStatus;
    
    return matchesSearch && matchesPosition && matchesStatus;
  });



  // 통계 계산
  const totalStaff = staff.length;
  const activeStaff = staff.filter(s => s.status === '근무중').length;
  const thisMonthTotalHours = staff.reduce((sum, s) => sum + (s?.thisMonthHours || 0), 0);
  const thisMonthTotalSalary = staff.reduce((sum, s) => sum + ((s?.thisMonthHours || 0) * (s?.hourlyRate || 0)), 0);

  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">직원 관리</h1>
          <p className="text-sm text-gray-600 mt-1">매장 직원들의 정보와 근무시간을 관리합니다</p>
        </div>
        <Button onClick={() => setShowAddModal(true)}>
          <UserPlus className="w-4 h-4 mr-2" />
          직원 추가
        </Button>
      </div>

      {/* KPI 카드들 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <KPICard
          title="전체 직원"
          value={`${totalStaff}명`}
          icon={Users}
          color="red"
          trend={0}
        />
        <KPICard
          title="근무 중"
          value={`${activeStaff}명`}
          icon={UserCheck}
          color="orange"
          trend={+5.3}
        />
        <KPICard
          title="이번달 총 근무시간"
          value={`${thisMonthTotalHours}시간`}
          icon={Clock}
          color="green"
          trend={+8.7}
        />
        <KPICard
          title="이번달 인건비"
          value={`₩${Math.round(thisMonthTotalSalary / 10000)}만원`}
          icon={DollarSign}
          color="purple"
          trend={+12.3}
        />
      </div>

      <Tabs defaultValue="list" className="space-y-6">
        <TabsList>
          <TabsTrigger value="list">직원 목록</TabsTrigger>
          <TabsTrigger value="schedule">근무 일정</TabsTrigger>
          <TabsTrigger value="salary">급여 관리</TabsTrigger>
          <TabsTrigger value="reports">근무 리포트</TabsTrigger>
        </TabsList>

        <TabsContent value="list" className="space-y-4">
          {/* 검색 및 필터 */}
          <Card>
            <CardContent className="p-4">
              <div className="flex flex-col sm:flex-row gap-4">
                <div className="flex-1">
                  <div className="relative">
                    <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                    <Input
                      placeholder="이름, 연락처, 포지션으로 검색..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="pl-10"
                    />
                  </div>
                </div>
                <div className="flex gap-2">
                  <select
                    value={filterPosition}
                    onChange={(e) => setFilterPosition(e.target.value)}
                    className="px-3 py-2 border rounded-md text-sm"
                  >
                    <option value="all">전체 포지션</option>
                    {positionStats.map(pos => (
                      <option key={pos.name} value={pos.name}>{pos.name}</option>
                    ))}
                  </select>
                  <select
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                    className="px-3 py-2 border rounded-md text-sm"
                  >
                    <option value="all">전체 상태</option>
                    <option value="근무중">근무중</option>
                    <option value="휴가">휴가</option>
                    <option value="퇴사">퇴사</option>
                  </select>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* 직원 목록 테이블 */}
          <DataTable
            data={filteredStaff}
            columns={staffColumns}
            title={`직원 목록 (${filteredStaff.length}명)`}
            hideSearch={true}
          />
        </TabsContent>

        <TabsContent value="schedule" className="space-y-6">
          {/* 근무 일정 관리 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CalendarDays className="w-5 h-5" />
                이번주 근무 일정
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="grid grid-cols-7 gap-2 text-center text-sm font-medium">
                  <div>월</div>
                  <div>화</div>
                  <div>수</div>
                  <div>목</div>
                  <div>금</div>
                  <div>토</div>
                  <div>일</div>
                </div>
                <div className="grid grid-cols-7 gap-2">
                  {Array.from({ length: 7 }, (_, i) => (
                    <div key={i} className="border rounded-lg p-2 min-h-[100px]">
                      <div className="text-xs font-medium mb-2">
                        {new Date(2024, 0, i + 1).getDate()}일
                      </div>
                      <div className="space-y-1">
                        {staff.filter(s => s.status === '근무중').slice(0, 2).map(member => (
                          <div key={member.id} className="text-xs bg-gray-100 rounded px-1 py-0.5">
                            {member.name}
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>

          {/* 포지션별 현황 */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {positionStats.map((pos, index) => (
              <Card key={pos.name}>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="font-semibold">{pos.name}</h3>
                      <p className="text-2xl font-bold mt-1">{pos.count}명</p>
                      <p className="text-sm text-gray-600">
                        평균 ₩{(pos.avgRate || 0).toLocaleString()}/h
                      </p>
                    </div>
                    <div 
                      className="w-12 h-12 rounded-full flex items-center justify-center"
                      style={{ backgroundColor: pos.color }}
                    >
                      <Users className="w-6 h-6 text-white" />
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="salary" className="space-y-6">
          {/* 급여 관리 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="w-5 h-5" />
                이번달 급여 현황
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {staff.filter(s => s.status !== '퇴사').map(member => (
                  <div key={member.id} className="flex items-center justify-between p-3 border rounded-lg">
                    <div className="flex items-center gap-3">
                      <Avatar className="w-8 h-8">
                        <AvatarFallback className="bg-kpi-green text-white text-xs">
                          {member.avatar}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <h4 className="font-medium">{member.name}</h4>
                        <p className="text-sm text-gray-600">
                          {member.position} • {member.thisMonthHours}시간 근무
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-semibold">
                        ₩{((member.thisMonthHours || 0) * (member.hourlyRate || 0)).toLocaleString()}
                      </div>
                      <div className="text-xs text-gray-500">
                        ₩{(member.hourlyRate || 0).toLocaleString()}/시간
                      </div>
                    </div>
                  </div>
                ))}
                
                <div className="border-t pt-4">
                  <div className="flex justify-between items-center text-lg font-semibold">
                    <span>총 인건비</span>
                    <span className="text-kpi-red">₩{(thisMonthTotalSalary || 0).toLocaleString()}</span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="reports" className="space-y-6">
          {/* 주별 근무시간 차트 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrendingUp className="w-5 h-5" />
                주별 근무시간 현황
              </CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={weeklyHoursData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="week" />
                  <YAxis />
                  <Tooltip 
                    formatter={(value: any, name) => [
                      name === 'totalHours' ? `${value}시간` : `${value}명`,
                      name === 'totalHours' ? '총 근무시간' : '근무 인원'
                    ]}
                  />
                  <Legend />
                  <Bar dataKey="totalHours" fill="#FF6B6B" name="총 근무시간" />
                  <Bar dataKey="staffCount" fill="#06D6A0" name="근무 인원" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          {/* 근무 통계 */}
          <Card>
            <CardHeader>
              <CardTitle>근무 통계</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-gray-50 p-4 rounded-lg">
                  <h4 className="font-semibold mb-2">평균 주간 근무시간</h4>
                  <p className="text-2xl font-bold text-kpi-green">38시간</p>
                  <p className="text-sm text-gray-600">직원 1인당</p>
                </div>
                <div className="bg-gray-50 p-4 rounded-lg">
                  <h4 className="font-semibold mb-2">최고 근무시간</h4>
                  <p className="text-2xl font-bold text-kpi-orange">45시간</p>
                  <p className="text-sm text-gray-600">김민준 (매니저)</p>
                </div>
                <div className="bg-gray-50 p-4 rounded-lg">
                  <h4 className="font-semibold mb-2">인건비 비율</h4>
                  <p className="text-2xl font-bold text-kpi-red">28%</p>
                  <p className="text-sm text-gray-600">총 매출 대비</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* 직원 추가 모달 */}
      <FormModal
        isOpen={showAddModal}
        onClose={() => setShowAddModal(false)}
        onSubmit={handleAddStaff}
        title="직원 추가"
        fields={staffFormFields}
      />

      {/* 직원 수정 모달 */}
      <FormModal
        isOpen={showEditModal}
        onClose={() => {
          setShowEditModal(false);
          setSelectedStaff(null);
        }}
        onSubmit={handleUpdateStaff}
        title="직원 정보 수정"
        fields={staffFormFields}
        initialData={selectedStaff || {}}
      />

      {/* 삭제 확인 다이얼로그 */}
      <ConfirmDialog
        isOpen={showDeleteDialog}
        onClose={() => {
          setShowDeleteDialog(false);
          setSelectedStaff(null);
        }}
        onConfirm={confirmDelete}
        title="직원 삭제"
        description={`정말로 ${selectedStaff?.name} 직원을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`}
      />
    </div>
  );
}