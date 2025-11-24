import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { KPICard } from '../Common/KPICard';
import { DataTable } from '../Common/DataTable';
import { Card } from '../ui/card';
import { Input } from '../ui/input';
import { Badge } from '../ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '../ui/dialog';
import { Button } from '../ui/button';
import {
  MessageSquare,
  AlertTriangle,
  Search,
  Paperclip,
  Star,
  User,
  Calendar,
  Download,
  Eye
} from 'lucide-react';
import { toast } from 'sonner';
import {
  getNoticeList,
  getNoticeDetail,
  Notice,
  NoticeDetail,
  Attachment,
  NoticeListResponseDTO
} from '../../lib/noticeApi';

// 라벨 매핑 (타입 안전)
const NoticePriorityLabel: Record<'NORMAL' | 'IMPORTANT' | 'EMERGENCY', string> = {
  NORMAL: '일반',
  IMPORTANT: '중요',
  EMERGENCY: '긴급',
};
type NoticePriorityKey = keyof typeof NoticePriorityLabel;

export function NoticeEducation() {
  // 공지사항 목록 / 상세
  const [notices, setNotices] = useState<Notice[]>([]);
  const [selectedNotice, setSelectedNotice] = useState<NoticeDetail | null>(null);
  const [showNoticeDetail, setShowNoticeDetail] = useState(false);

  // 상태
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 검색/필터
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  const [priorityFilter, setPriorityFilter] = useState<NoticePriorityKey | 'ALL'>('ALL');

  // 페이지네이션
  const [pagination, setPagination] = useState({
    page: 1,
    size: 10, // DataTable 기본 10에 맞춤
    totalPages: 1,
    totalElements: 0,
  });

  // KPI
  const [kpiCounts, setKpiCounts] = useState({
    totalCount: 0,
    urgentCount: 0,
    importantCount: 0,
    unreadCount: 0,
  });

  // 검색어 디바운싱
  useEffect(() => {
    const t = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm);
      setPagination((prev) => ({ ...prev, page: 1 }));
    }, 500);
    return () => clearTimeout(t);
  }, [searchTerm]);

  // 목록 로드
  useEffect(() => {
    let canceled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const type = 'all';
        const search = debouncedSearchTerm.trim() === '' ? undefined : debouncedSearchTerm;
        const priority = priorityFilter === 'ALL' ? undefined : priorityFilter;

        const data: NoticeListResponseDTO = await getNoticeList(
          pagination.page,
          pagination.size,
          type,
          search,
          priority
        );

        if (canceled) return;

        setNotices(data.pageData.content);
        setPagination((prev) => ({
          ...prev,
          totalPages: data.pageData.totalPages,
          totalElements: data.pageData.totalElements,
        }));
        setKpiCounts(data.countData);
      } catch (e) {
        if (canceled) return;
        setError('공지사항을 불러오는 데 실패했습니다.');
        toast.error('데이터 로딩 실패');
      } finally {
        if (!canceled) setLoading(false);
      }
    })();
    return () => {
      canceled = true;
    };
  }, [pagination.page, pagination.size, debouncedSearchTerm, priorityFilter]);

  // 상세 보기
  const handleViewNotice = useCallback(async (notice: Notice) => {
    try {
      const detail = await getNoticeDetail(notice.id);
      setSelectedNotice(detail);
      setShowNoticeDetail(true);
    } catch {
      toast.error('공지사항 상세 정보를 불러오는 데 실패했습니다.');
    }
  }, []);

  const handlePageChange = useCallback((newPage: number) => {
    setPagination((prev) => {
      if (newPage > 0 && newPage <= prev.totalPages) {
        return { ...prev, page: newPage };
      }
      return prev;
    });
  }, []);

  // 테이블 컬럼 (메모이제이션)
  const noticeColumns = useMemo(
    () => [
      {
        key: 'noticePriority',
        label: '중요도',
        render: (_: any, notice: Notice) => (
          <div className="flex items-center gap-2">
            {notice.noticePriority === 'EMERGENCY' && <AlertTriangle className="w-4 h-4 text-red-500" />}
            {notice.noticePriority === 'IMPORTANT' && <Star className="w-4 h-4 text-yellow-500" />}
            <span>{NoticePriorityLabel[notice.noticePriority as NoticePriorityKey]}</span>
          </div>
        ),
      },
      {
        key: 'title',
        label: '제목',
        render: (_: any, notice: Notice) => (
          <div className="max-w-[20rem]">
            <div className="flex items-center gap-2 mb-1">
              <span
                className="font-medium truncate cursor-pointer hover:text-blue-600"
                onClick={() => handleViewNotice(notice)}
                title={notice.title}
              >
                {notice.title}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <Badge variant="outline" className="text-xs">
                {notice.noticeCategory}
              </Badge>
            </div>
          </div>
        ),
      },
      {
        key: 'writer',
        label: '작성자',
        render: (_: any, notice: Notice) => (
          <div className="text-xs">
            <div className="font-medium flex items-center gap-1">
              <User className="w-3 h-3" />
              {notice.writer}
            </div>
          </div>
        ),
      },
      {
        key: 'registeredAt',
        label: '등록일',
        render: (_: any, notice: Notice) => (
          <div className="text-xs flex items-center gap-1">
            <Calendar className="w-3 h-3" />
            {new Date(notice.registeredAt).toLocaleDateString('ko-KR')}
          </div>
        ),
      },
      {
        key: 'attachment',
        label: '첨부파일',
        render: (_: any, notice: Notice) => (
          <div className="flex items-center justify-center">
            {notice.hasAttachment && notice.firstAttachmentUrl ? (
              <a
                href={notice.firstAttachmentUrl}
                target="_blank"
                rel="noopener noreferrer"
                onClick={(e) => e.stopPropagation()}
                className="text-gray-500 hover:text-blue-600"
                title="첨부파일 다운로드"
                // 가능한 경우 브라우저 기본 다운로드 유도
                download
              >
                <Download className="w-5 h-5" />
              </a>
            ) : (
              <span className="text-gray-400">-</span>
            )}
          </div>
        ),
      },
    ],
    [handleViewNotice]
  );

  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">공지사항</h1>
          <p className="text-sm text-gray-600 mt-1">본사에서 발송한 공지사항을 확인합니다.</p>
        </div>
      </div>

      {/* KPI 카드 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <KPICard title="전체 공지" value={`${kpiCounts.totalCount}건`} icon={MessageSquare} color="green" />
        <KPICard title="긴급 공지" value={`${kpiCounts.urgentCount}건`} icon={AlertTriangle} color="red" />
        <KPICard title="중요 공지" value={`${kpiCounts.importantCount}건`} icon={Star} color="orange" />
      </div>

      <div className="space-y-4">
        {/* 검색 & 필터 */}
        <Card className="p-4">
          <div className="flex flex-wrap items-center gap-4">
            <div className="flex-1 min-w-[200px]">
              <div className="relative">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                <Input
                  aria-label="공지사항 검색"
                  placeholder="제목, 내용, 작성자로 검색..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>

            <div className="flex items-center gap-2">
              {/* 중요도 필터 버튼 그룹 */}
              <div className="flex rounded-md border">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setPriorityFilter('ALL');
                    setPagination((prev) => ({ ...prev, page: 1 }));
                  }}
                  className={`h-auto px-3 py-2 text-sm rounded-l-md rounded-r-none ${
                    priorityFilter === 'ALL' ? 'bg-kpi-red text-white hover:bg-kpi-red' : 'hover:bg-gray-100'
                  }`}
                >
                  전체
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setPriorityFilter('NORMAL');
                    setPagination((prev) => ({ ...prev, page: 1 }));
                  }}
                  className={`h-auto px-3 py-2 text-sm rounded-none ${
                    priorityFilter === 'NORMAL' ? 'bg-kpi-red text-white hover:bg-kpi-red' : 'hover:bg-gray-100'
                  }`}
                >
                  일반
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setPriorityFilter('IMPORTANT');
                    setPagination((prev) => ({ ...prev, page: 1 }));
                  }}
                  className={`h-auto px-3 py-2 text-sm rounded-none ${
                    priorityFilter === 'IMPORTANT' ? 'bg-kpi-red text-white hover:bg-kpi-red' : 'hover:bg-gray-100'
                  }`}
                >
                  중요
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setPriorityFilter('EMERGENCY');
                    setPagination((prev) => ({ ...prev, page: 1 }));
                  }}
                  className={`h-auto px-3 py-2 text-sm rounded-r-md rounded-l-none ${
                    priorityFilter === 'EMERGENCY' ? 'bg-kpi-red text-white hover:bg-kpi-red' : 'hover:bg-gray-100'
                  }`}
                >
                  긴급
                </Button>
              </div>
            </div>
          </div>
        </Card>

        {/* 테이블 */}
        {loading ? (
          <p>로딩 중...</p>
        ) : error ? (
          <p className="text-red-600">{error}</p>
        ) : (
          <DataTable
            data={notices}
            columns={noticeColumns}
            title={`공지사항 목록 (${pagination.totalElements}건)`}
            hideSearch={true}
            showActions={false}
            serverSidePagination={true}
            currentPage={pagination.page}
            totalPageCount={pagination.totalPages}
            onPageChange={handlePageChange}
            totalElements={pagination.totalElements}
          />
        )}
      </div>

      {/* 상세 모달 */}
      <Dialog
        open={showNoticeDetail}
        onOpenChange={(open) => {
          setShowNoticeDetail(open);
          if (!open) setSelectedNotice(null); // 닫을 때 정리
        }}
      >
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          {selectedNotice ? (
            <>
              <DialogHeader>
                <DialogTitle className="text-xl pr-4">{selectedNotice.notice.title}</DialogTitle>
                <DialogDescription className="mt-1">
                  {selectedNotice.notice.writer}에서 작성한 공지사항입니다.
                </DialogDescription>
                <div className="flex items-center gap-2 mt-2">
                  <Badge variant="outline" className="text-xs">
                    {selectedNotice.notice.noticeCategory}
                  </Badge>
                  {selectedNotice.notice.noticePriority === 'EMERGENCY' && (
                    <Badge className="bg-red-100 text-red-700 text-xs">긴급</Badge>
                  )}
                  {selectedNotice.notice.noticePriority === 'IMPORTANT' && (
                    <Badge className="bg-yellow-100 text-yellow-700 text-xs">중요</Badge>
                  )}
                </div>
              </DialogHeader>

              <div className="space-y-4">
                <div className="border-b pb-4">
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="text-gray-600">작성자:</span>
                      <span className="ml-2 font-medium">{selectedNotice.notice.writer}</span>
                    </div>
                    <div>
                      <span className="text-gray-600">작성일:</span>
                      <span className="ml-2">
                        {new Date(selectedNotice.notice.registeredAt).toLocaleString('ko-KR')}
                      </span>
                    </div>
                    <div>
                      <span className="text-gray-600">조회수:</span>
                      <span className="ml-2 flex items-center gap-1">
                        <Eye className="w-3 h-3" />
                        {selectedNotice.notice.noticeCount}
                      </span>
                    </div>
                  </div>
                </div>

                <div
                  className="prose prose-sm max-w-none"
                  dangerouslySetInnerHTML={{ __html: selectedNotice.notice.body }}
                />

                {selectedNotice.attachments && selectedNotice.attachments.length > 0 && (
                  <div className="border-t pt-4">
                    <h4 className="font-medium mb-2 flex items-center gap-2">
                      <Paperclip className="w-4 h-4" />
                      첨부파일
                    </h4>
                    <div className="space-y-2">
                      {selectedNotice.attachments.map((file: Attachment) => (
                        <a
                          key={file.id}
                          href={file.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="flex items-center justify-between p-2 bg-gray-50 rounded hover:bg-gray-100"
                          download
                        >
                          <span className="text-sm text-blue-600 underline">{file.originalFilename}</span>
                          <Download className="w-4 h-4 text-gray-500" />
                        </a>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </>
          ) : (
            <p>상세 정보를 불러오는 중입니다...</p>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}