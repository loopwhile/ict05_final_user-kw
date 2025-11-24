import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  Plus, Search, Edit, Users,
  UserCheck, UserMinus
} from 'lucide-react';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Card } from '../ui/card';
import { Badge } from '../ui/badge';
import {
  Select, SelectContent, SelectItem,
  SelectTrigger, SelectValue
} from '../ui/select';
import { Avatar, AvatarFallback } from '../ui/avatar';
import { FormModal } from '../Common/FormModal';
import { toast } from 'sonner';

/* ---------- FormModal 필드 타입 ---------- */
type UIFieldType =
  | 'number' | 'text' | 'select' | 'email' | 'date'
  | 'textarea' | 'time' | 'password' | 'file' | 'tel' | 'month';

type BaseField = {
  name: string;
  label: string;
  required: boolean;
};

type ModalField =
  | (BaseField & {
      type: Exclude<UIFieldType, 'select'>;
      placeholder?: string;
      options?: never;
    })
  | (BaseField & {
      type: 'select';
      options: { value: string; label: string }[];
    });

/* ---------- Staff & Page 인터페이스 ---------- */
interface Staff {
  id: number;
  staffName: string;
  staffBirth: string;
  staffEmploymentType: string;
  staffStartDate: string;
  staffEndDate?: string | null;
  attendanceStatus?: string | null;
  staffPhone: string;
  staffEmail: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // 현재 페이지(0부터)
  size: number;
}

/* ---------- 컴포넌트 ---------- */
export function StaffList() {
  const [staff, setStaff] = useState<Staff[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterAttendance, setFilterAttendance] = useState<string>('all');
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [editingStaff, setEditingStaff] = useState<Staff | null>(null);

  // 페이징 상태 (현재 페이지용)
  const [page, setPage] = useState(0);          // 현재 페이지(0-based)
  const [size, setSize] = useState(8);         // 한 페이지 8명
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // 전체 직원 (카테고리 통계용)
  const [allStaff, setAllStaff] = useState<Staff[]>([]);

  // 날짜 포맷 변환 (LocalDateTime 대응)
  const toDateTime = (v: string) =>
    v && v.length === 10 ? `${v}T00:00:00` : v;

  /* 직원 목록 불러오기 (page/size 포함 - 현재 페이지) */
  const fetchStaffList = async (pageParam = page, sizeParam = size) => {
    try {
      const baseUrl = import.meta.env.VITE_BACKEND_API_BASE_URL;
      const token = localStorage.getItem('accessToken');

      const res = await axios.get<PageResponse<Staff>>(
        `${baseUrl}/api/staff/list`,
        {
          headers: { Authorization: `Bearer ${token}` },
          params: { page: pageParam, size: sizeParam },
        }
      );

      console.log('<<백엔드에서 받은 직원 데이터(현재 페이지)>>', res.data.content);

      setStaff(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
      setPage(res.data.number);
      setSize(res.data.size);
    } catch (err) {
      console.error(err);
      toast.error('직원 목록을 불러오지 못했습니다.');
    }
  };

  /* 전체 직원 목록 전부 가져오기 (카테고리 숫자 계산용) */
  const fetchAllStaff = async () => {
    try {
      const baseUrl = import.meta.env.VITE_BACKEND_API_BASE_URL;
      const token = localStorage.getItem('accessToken');

      const res = await axios.get<PageResponse<Staff>>(
        `${baseUrl}/api/staff/list`,
        {
          headers: { Authorization: `Bearer ${token}` },
          params: { page: 0, size: 9999 }, // 충분히 큰 값으로 전체 조회
        }
      );

      console.log('<<백엔드에서 받은 직원 데이터(전체)>>', res.data.content);
      setAllStaff(res.data.content);
    } catch (err) {
      console.error(err);
    }
  };

  // 최초 로딩 시 1페이지 조회 + 전체 통계용 조회
  useEffect(() => {
    fetchStaffList(0, 8);   // 현재 페이지
    fetchAllStaff();        // 전체 직원
  }, []);

  /* 등록 처리 */
  const handleAddStaff = async (data: any) => {
    try {
      const baseUrl = import.meta.env.VITE_BACKEND_API_BASE_URL;
      const token = localStorage.getItem('accessToken');

      const payload = {
        staffName: data.staffName,
        staffEmploymentType: data.staffEmploymentType,
        staffEmail: data.staffEmail,
        staffPhone: data.staffPhone,
        staffBirth: toDateTime(data.staffBirth),
        staffStartDate: toDateTime(data.staffStartDate),
      };

      const res = await axios.post<number>(`${baseUrl}/api/staff/add`, payload, {
        headers: { Authorization: `Bearer ${token}` },
      });

      // 현재 페이지 다시 조회 + 전체 통계 다시 조회
      await fetchStaffList(page, size);
      await fetchAllStaff();

      setIsAddModalOpen(false);
      toast.success(`직원이 등록되었습니다. (ID: ${res.data})`);
    } catch (err: any) {
      console.error(err?.response?.data ?? err);
      toast.error('직원 등록에 실패했습니다.');
    }
  };

  /* 수정 처리 */
  const handleEditStaff = async (data: any) => {
    if (!editingStaff) return;

    try {
      const baseUrl = import.meta.env.VITE_BACKEND_API_BASE_URL;
      const token = localStorage.getItem('accessToken');

      const payload = {
        staffName: data.staffName,
        staffEmploymentType: data.staffEmploymentType,
        staffEmail: data.staffEmail,
        staffPhone: data.staffPhone,
        staffBirth: toDateTime(data.staffBirth),
        staffStartDate: toDateTime(data.staffStartDate),
        staffEndDate: data.staffEndDate ? toDateTime(data.staffEndDate) : null,
      };

      await axios.put(
        `${baseUrl}/api/staff/modify/${editingStaff.id}`,
        payload,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      // 수정 후 현재 페이지 + 전체 통계 재조회
      await fetchStaffList(page, size);
      await fetchAllStaff();

      setEditingStaff(null);
      toast.success('직원 정보가 수정되었습니다.');
    } catch (err: any) {
      console.error(err?.response?.data ?? err);
      toast.error('직원 수정에 실패했습니다.');
    }
  };

  /* 퇴사 처리 */
  const handleResignStaff = async (target: Staff) => {
    const confirmed = window.confirm(
      `${target.staffName} 직원을 퇴사 처리하시겠습니까?`
    );
    if (!confirmed) return;

    try {
      const baseUrl = import.meta.env.VITE_BACKEND_API_BASE_URL;
      const token = localStorage.getItem('accessToken');

      // 오늘 날짜를 yyyy-MM-dd 로
      const todayDate = new Date().toISOString().slice(0, 10);

      const payload = {
        staffName: target.staffName,
        staffEmploymentType: target.staffEmploymentType,
        staffEmail: target.staffEmail,
        staffPhone: target.staffPhone,
        staffBirth: toDateTime(target.staffBirth.slice(0, 10)),
        staffStartDate: toDateTime(target.staffStartDate.slice(0, 10)),
        staffEndDate: toDateTime(todayDate),
      };

      await axios.put(
        `${baseUrl}/api/staff/modify/${target.id}`,
        payload,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      // 다시 목록 재조회 + 통계 재조회
      await fetchStaffList(page, size);
      await fetchAllStaff();

      toast.success(`${target.staffName}님이 퇴사 처리되었습니다.`);
    } catch (err: any) {
      console.error(err?.response?.data ?? err);
      toast.error('퇴사 처리에 실패했습니다.');
    }
  };

  /* ✅ 근무/퇴사 카테고리 매핑 */
  const getWorkCategoryFromStaff = (s: Staff): 'active' | 'resigned' => {
    // 1) 퇴사일이 있으면 무조건 퇴사
    if (s.staffEndDate) {
      return 'resigned';
    }

    // 2) (옵션) 근태 상태 enum도 같이 체크
    if (s.attendanceStatus) {
      const raw = s.attendanceStatus.toString().trim();
      const upper = raw.toUpperCase();

      if (upper === 'RESIGN' || raw === '퇴사') {
        return 'resigned';
      }
    }

    // 3) 나머지는 근무중
    return 'active';
  };

  /* ✅ 필터링 (현재 페이지 기준에서만 필터) */
  const filteredStaff = staff.filter(staffMember => {
    const matchesSearch =
      staffMember.staffName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      staffMember.staffEmploymentType
        .toLowerCase()
        .includes(searchTerm.toLowerCase());

    const matchesAttendance =
      filterAttendance === 'all' ||
      getWorkCategoryFromStaff(staffMember) === filterAttendance;

    return matchesSearch && matchesAttendance;
  });

  /* ✅ 근태 상태 뱃지 */
  const getAttendanceBadge = (s: Staff) => {
    const category = getWorkCategoryFromStaff(s);

    if (category === 'resigned') {
      return <Badge className="bg-red-100 text-red-800">퇴사</Badge>;
    }
    return <Badge className="bg-green-100 text-green-800">근무중</Badge>;
  };

  /* 등록 모달 필드 */
  const staffAddFormFields: ModalField[] = [
    { name: 'staffName', label: '이름', type: 'text', required: true },
    { name: 'staffBirth', label: '생년월일', type: 'date', required: true },
    {
      name: 'staffPhone',
      label: '연락처',
      type: 'tel',
      required: true,
      placeholder: '010-1234-5678',
    },
    {
      name: 'staffEmail',
      label: '이메일',
      type: 'email',
      required: true,
      placeholder: 'example@email.com',
    },
    { name: 'staffStartDate', label: '입사일', type: 'date', required: true },
    {
      name: 'staffEmploymentType',
      label: '직책',
      type: 'select',
      required: true,
      options: [
        { value: 'OWNER', label: '점주' },
        { value: 'WORKER', label: '직원' },
        { value: 'PART_TIMER', label: '알바' },
      ],
    },
  ];

  /* 수정 모달 필드 (퇴사일 추가 / 선택값) */
  const staffEditFormFields: ModalField[] = [
    ...staffAddFormFields,
    {
      name: 'staffEndDate',
      label: '퇴사일',
      type: 'date',
      required: false,
    },
  ];

  /* ✅ 렌더링 */
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Users className="w-6 h-6 text-kpi-purple" />
          <h1>직원 목록</h1>
        </div>
        <Button onClick={() => setIsAddModalOpen(true)} className="gap-2">
          <Plus className="w-4 h-4" />
          직원 추가
        </Button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="p-4">
          <div className="flex items-center gap-3">
            <Users className="w-6 h-6 text-kpi-purple" />
            <div>
              <p className="text-sm text-dark-gray">전체 직원</p>
              {/* 전체 기준 → allStaff.length */}
              <p className="text-2xl font-semibold">{allStaff.length}</p>
            </div>
          </div>
        </Card>
        <Card className="p-4">
          <div className="flex items-center gap-3">
            <UserCheck className="w-6 h-6 text-kpi-green" />
            <div>
              <p className="text-sm text-dark-gray">근무중</p>
              <p className="text-2xl font-semibold">
                {allStaff.filter(s => getWorkCategoryFromStaff(s) === 'active').length}
              </p>
            </div>
          </div>
        </Card>
        <Card className="p-4">
          <div className="flex items-center gap-3">
            <UserMinus className="w-6 h-6 text-kpi-red" />
            <div>
              <p className="text-sm text-dark-gray">퇴사</p>
              <p className="text-2xl font-semibold">
                {allStaff.filter(s => getWorkCategoryFromStaff(s) === 'resigned').length}
              </p>
            </div>
          </div>
        </Card>
      </div>

      {/* Filters */}
      <Card className="p-4">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="flex-1">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
              <Input
                placeholder="이름, 직책 검색..."
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>
          <Select value={filterAttendance} onValueChange={setFilterAttendance}>
            <SelectTrigger className="w-full md:w-48">
              <SelectValue placeholder="상태 필터" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">전체 상태</SelectItem>
              <SelectItem value="active">근무중</SelectItem>
              <SelectItem value="resigned">퇴사</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </Card>

      {/* Staff List + Pagination */}
      <Card>
        <div className="p-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {filteredStaff.map(staff => (
              <Card key={staff.id} className="p-4 hover:shadow-md transition-shadow">
                <div className="flex items-start gap-4">
                  <Avatar className="w-12 h-12">
                    <AvatarFallback>{staff.staffName[0]}</AvatarFallback>
                  </Avatar>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="font-semibold">{staff.staffName}</h3>
                        <p className="text-sm text-dark-gray">
                          {staff.staffEmploymentType}
                        </p>
                      </div>
                      {getAttendanceBadge(staff)}
                    </div>
                    <div className="mt-3 space-y-1 text-sm text-dark-gray">
                      <div>생년월일: {staff.staffBirth}</div>
                      <div>전화번호: {staff.staffPhone}</div>
                      <div>이메일: {staff.staffEmail}</div>
                      <div>입사일: {staff.staffStartDate}</div>
                      {staff.staffEndDate && (
                        <div>퇴사일: {staff.staffEndDate}</div>
                      )}
                    </div>

                    <div className="flex gap-2 mt-4">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setEditingStaff(staff)}
                        className="gap-1"
                      >
                        <Edit className="w-3 h-3" /> 수정
                      </Button>

                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleResignStaff(staff)}
                        className="gap-1 text-kpi-red"
                      >
                        퇴사 처리
                      </Button>
                    </div>
                  </div>
                </div>
              </Card>
            ))}
          </div>

          {filteredStaff.length === 0 && (
            <div className="text-center py-8 text-dark-gray">
              검색 조건에 맞는 직원이 없습니다.
            </div>
          )}

          {/* ✅ 페이징 네비게이션 */}
          <div className="mt-6 flex items-center justify-center gap-4">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => fetchStaffList(page - 1, size)}
            >
              이전
            </Button>
            <span className="text-sm text-dark-gray">
              {totalPages > 0 ? page + 1 : 0} / {totalPages} 페이지
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page + 1 >= totalPages}
              onClick={() => fetchStaffList(page + 1, size)}
            >
              다음
            </Button>
          </div>
        </div>
      </Card>

      {/* Add Staff Modal */}
      <FormModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onSubmit={handleAddStaff}
        title="직원 추가"
        fields={staffAddFormFields}
      />

      {/* Edit Staff Modal */}
      <FormModal
        isOpen={!!editingStaff}
        onClose={() => setEditingStaff(null)}
        onSubmit={handleEditStaff}
        title="직원 정보 수정"
        fields={staffEditFormFields}
        initialData={
          editingStaff
            ? {
                ...editingStaff,
                staffBirth: editingStaff.staffBirth?.slice(0, 10),
                staffStartDate: editingStaff.staffStartDate?.slice(0, 10),
                staffEndDate: editingStaff.staffEndDate
                  ? editingStaff.staffEndDate.slice(0, 10)
                  : undefined,
              }
            : undefined
        }
      />
    </div>
  );
}

export default StaffList;
