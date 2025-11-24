import React, { useState, useMemo } from 'react';
import { Card } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Download, MoreHorizontal, ChevronUp, ChevronDown, Edit, Trash2, Eye, Search } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/dropdown-menu';

export interface Column<Row = any> {
  key: string;
  label: string | React.ReactNode;
  sortable?: boolean;
  width?: string;
  render?: (value: any, row: Row) => React.ReactNode;
}

export interface DataTableProps {
  data: any[];
  columns: Column[];
  title: string;
  searchPlaceholder?: string;
  onAdd?: () => void;
  onEdit?: (row: any) => void;
  onDelete?: (row: any) => void;
  onView?: (row: any) => void;
  addButtonText?: string;
  showActions?: boolean;
  filters?: { label: string; value: string; count?: number }[];
  onExport?: () => void;
  hideSearch?: boolean;
  hideHeaderSummary?: boolean;

  // ✅ 추가된 서버 페이징 관련 props
  serverSidePagination?: boolean;
  currentPage?: number; // 외부 제어
  totalPageCount?: number;
  onPageChange?: (page: number) => void;
  totalElements?: number;
  totalDisplayCount?: number; 

  pageSize?: number;          // 표시에만 사용(서버모드), 기본 10
  pageBlockSize?: number;     // 블록 크기, 기본 10

  serverFilterEnabled?: boolean;
  onFilterChange?: (value: string) => void;
}

export function DataTable({
  data = [],
  columns = [],
  title = '데이터 테이블',
  searchPlaceholder = '검색어를 입력하세요',
  onAdd,
  onEdit,
  onDelete,
  onView,
  addButtonText = '등록',
  showActions = true,
  filters = [],
  onExport,
  hideSearch = false,
  hideHeaderSummary = false,

  // ✅ 추가된 props 기본값
  serverSidePagination = false,
  currentPage: externalPage = 1,
  totalPageCount: externaltotalPageCount = 1,
  onPageChange,
  totalElements = 0,
  totalDisplayCount,

  pageSize = 10,
  pageBlockSize = 10,

  serverFilterEnabled = false,
  onFilterChange,
}: DataTableProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [sortColumn, setSortColumn] = useState<string | null>(null);
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [activeFilter, setActiveFilter] = useState<string>('all');

  // ✅ 내부 페이지 상태 (클라이언트 모드에서만 사용)
  const [internalPage, setInternalPage] = useState(1);
  const [itemsPerPage] = useState(10);

  const isServerFilter = serverSidePagination && serverFilterEnabled;

  // ✅ 서버모드일 때 외부 페이지, 아니면 내부 페이지
  const currentPage = serverSidePagination ? externalPage : internalPage;

  // 헤더 표시용 총개수 선택
  const headerTotal = totalDisplayCount ?? (serverSidePagination ? totalElements : data.length);

  // 필터링
  const filteredData = useMemo(() => {
    const safe = Array.isArray(data)
      ? data.filter((row) => row && typeof row === "object")
      : [];

    let out = safe;

    // 로컬 검색(서버가 검색 처리하지 않는 경우만 유지)
    if (!isServerFilter && !hideSearch && searchTerm) {
      const q = searchTerm.toLowerCase();
      out = out.filter((row) =>
        Object.values(row).some((v) =>
          String(v ?? "").toLowerCase().includes(q)
        )
      );
    }

    // 서버 필터 모드에선 상태 필터를 로컬에서 적용하지 않음
    if (!isServerFilter && activeFilter !== "all") {
      out = out.filter((row: any) => row.status === activeFilter);
    }

    return out;
  }, [
    data,
    searchTerm,
    activeFilter,
    hideSearch,
    serverSidePagination,
    serverFilterEnabled,
  ]);

  // 정렬
  const sortedData = useMemo(() => {
    if (!sortColumn) return filteredData;
    return [...filteredData].sort((a, b) => {
      const aValue = a[sortColumn];
      const bValue = b[sortColumn];
      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredData, sortColumn, sortDirection]);

  // ✅ 페이지 데이터 계산
  const paginatedData = useMemo(() => {
    if (serverSidePagination) return sortedData;
    const startIndex = (currentPage - 1) * itemsPerPage;
    return sortedData.slice(startIndex, startIndex + itemsPerPage);
  }, [sortedData, currentPage, itemsPerPage, serverSidePagination]);

  const totalPageCount = serverSidePagination
    ? externaltotalPageCount
    : Math.ceil(sortedData.length / itemsPerPage);

  const rangeStart = serverSidePagination
    ? (currentPage - 1) * pageSize + 1
    : (currentPage - 1) * itemsPerPage + 1;

  const rangeEnd = serverSidePagination
    ? Math.min(currentPage * pageSize, totalElements)
    : Math.min(currentPage * itemsPerPage, sortedData.length);

  const totalCountForDisplay = serverSidePagination ? totalElements : sortedData.length;

  const blockStart = Math.floor((currentPage - 1) / pageBlockSize) * pageBlockSize + 1;
  const blockEnd = Math.min(blockStart + pageBlockSize - 1, totalPageCount);
  const hasPrevBlock = blockStart > 1;
  const hasNextBlock = blockEnd < totalPageCount;

  const handleSort = (columnKey: string) => {
    if (sortColumn === columnKey) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortColumn(columnKey);
      setSortDirection('asc');
    }
  };

  const renderSortIcon = (columnKey: string) => {
    if (sortColumn !== columnKey) return null;
    return sortDirection === 'asc' ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      {!hideHeaderSummary && (
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">{title}</h2>
            <p className="text-sm text-dark-gray">
              총 {headerTotal}개 항목
            </p>
          </div>
          <div className="flex items-center gap-3">
            {onExport && (
              <Button variant="outline" onClick={onExport} className="gap-2">
                <Download className="w-4 h-4" />
                내보내기
              </Button>
            )}
            {onAdd && (
              <Button onClick={onAdd} className="bg-kpi-red hover:bg-red-600 text-white gap-2">
                <span>+ {addButtonText}</span>
              </Button>
            )}
          </div>
        </div>
      )}

      {/* Filters & Search */}
      {(filters.length > 0 || !hideSearch) && (
        <Card className="p-4 bg-white rounded-xl shadow-sm">
          <div className="flex flex-col lg:flex-row gap-4">
            {/* 검색창: hideSearch 가 false 일 때만 보여줌 */}
            {!hideSearch && (
              <div className="flex-1 relative">
                <Search className="w-5 h-5 text-dark-gray absolute left-3 top-1/2 transform -translate-y-1/2" />
                <Input
                  placeholder={searchPlaceholder}
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            )}

            {/* 상태 필터 버튼은 hideSearch 와 상관없이 항상 표시 */}
            {filters.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {/* 전체 버튼 */}
                <button
                  onClick={() => {
                    setActiveFilter("all");
                    if (isServerFilter) {
                      onFilterChange && onFilterChange("all");
                      onPageChange && onPageChange(1);
                    } else {
                      setInternalPage(1);
                    }
                  }}
                  className={`px-4 py-2 rounded-lg text-sm font-medium ${
                    activeFilter === "all"
                      ? "bg-kpi-red text-white"
                      : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                  }`}
                >
                  전체
                </button>

                {(filters ?? []).map((filter) => {
                  const zero =
                    typeof filter.count === "number" &&
                    filter.count === 0 &&
                    filter.value !== "all";
                  const isActive = activeFilter === filter.value;

                  const base =
                    "px-4 py-2 rounded-lg text-sm font-medium transition-colors";
                  const style = zero
                    ? "bg-gray-100 text-gray-300 cursor-not-allowed"
                    : isActive
                    ? "bg-kpi-red text-white"
                    : "bg-gray-100 text-gray-700 hover:bg-gray-200";

                  return (
                    <button
                      key={filter.value}
                      disabled={zero}
                      onClick={() => {
                        if (zero) return;
                        setActiveFilter(filter.value);
                        if (isServerFilter) {
                          onFilterChange && onFilterChange(filter.value);
                          onPageChange && onPageChange(1);
                        } else {
                          setInternalPage(1);
                        }
                      }}
                      className={`${base} ${style}`}
                      aria-disabled={zero}
                    >
                      {filter.label}
                      {filter.value !== "all" &&
                      typeof filter.count === "number"
                        ? ` (${filter.count})`
                        : ""}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </Card>
      )}

      {/* Table */}
      <Card className="bg-white rounded-xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-light-gray border-b">
              <tr>
                {columns.map((col) => (
                  <th
                    key={col.key}
                    className={`px-6 py-4 text-left text-sm font-semibold text-gray-900 ${
                      col.sortable ? 'cursor-pointer hover:bg-gray-100' : ''
                    }`}
                    onClick={() => col.sortable && handleSort(col.key)}
                  >
                    <div className="flex items-center gap-2">
                      {col.label}
                      {col.sortable && renderSortIcon(col.key)}
                    </div>
                  </th>
                ))}
                {showActions && <th className="px-6 py-4 text-sm font-semibold text-gray-900 w-24">액션</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {paginatedData.map((row, i) => (
                <tr key={i} className="hover:bg-gray-50">
                  {columns.map((col) => (
                    <td key={col.key} className="px-6 py-4 text-sm text-gray-900">
                      {col.render ? col.render(row[col.key], row) : row[col.key] ?? '-'}
                    </td>
                  ))}
                  {showActions && (
                    <td className="px-6 py-4">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="sm">
                            <MoreHorizontal className="w-4 h-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          {onView && (
                            <DropdownMenuItem onClick={() => onView(row)}>
                              <Eye className="w-4 h-4 mr-2" /> 상세보기
                            </DropdownMenuItem>
                          )}
                          {onEdit && (
                            <DropdownMenuItem onClick={() => onEdit(row)}>
                              <Edit className="w-4 h-4 mr-2" /> 수정
                            </DropdownMenuItem>
                          )}
                          {onDelete && (
                            <DropdownMenuItem onClick={() => onDelete(row)} className="text-red-600">
                              <Trash2 className="w-4 h-4 mr-2" /> 삭제
                            </DropdownMenuItem>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {paginatedData.length === 0 && (
          <div className="text-center py-12">
            <p className="text-dark-gray">데이터가 없습니다.</p>
          </div>
        )}

        {/* ✅ Pagination */}
        {totalPageCount > 1 && (
          <div className="px-6 py-4 border-t bg-light-gray flex items-center justify-between">
            <p className="text-sm text-dark-gray">
              {rangeStart} - {rangeEnd} / {totalCountForDisplay}개
            </p>

            <div className="flex items-center gap-1">
              {/* 첫 페이지, 이전 블록 */}
              <Button
                variant="outline" size="sm"
                disabled={currentPage === 1}
                onClick={() => onPageChange && onPageChange(1)}
              >
                «
              </Button>
              <Button
                variant="outline" size="sm"
                disabled={!hasPrevBlock}
                onClick={() => onPageChange && onPageChange(blockStart - 1)}
              >
                ‹
              </Button>

              {/* 블록 내 페이지들 */}
              {Array.from({ length: blockEnd - blockStart + 1 }, (_, i) => blockStart + i).map((page) => (
                <Button
                  key={page}
                  variant={currentPage === page ? "default" : "outline"}
                  size="sm"
                  onClick={() => onPageChange && onPageChange(page)}
                  className={currentPage === page ? "bg-kpi-red text-white" : ""}
                >
                  {page}
                </Button>
              ))}

              {/* 다음 블록, 마지막 페이지 */}
              <Button
                variant="outline" size="sm"
                disabled={!hasNextBlock}
                onClick={() => onPageChange && onPageChange(blockEnd + 1)}
              >
                ›
              </Button>
              <Button
                variant="outline" size="sm"
                disabled={currentPage === totalPageCount}
                onClick={() => onPageChange && onPageChange(totalPageCount)}
              >
                »
              </Button>
            </div>
          </div>
        )}

      </Card>
    </div>
  );
}
