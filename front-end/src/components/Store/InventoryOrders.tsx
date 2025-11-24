import { useState, useEffect } from 'react';
import api from "../../lib/authApi";
import { Truck, Eye, CheckCircle, Clock, Package, Trash } from 'lucide-react';
import { Card } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Badge } from '../ui/badge';
import { DataTable, Column } from '../Common/DataTable';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '../ui/dialog';
import { toast } from 'sonner';

export function InventoryOrders() {
  const [orders, setOrders] = useState<any[]>([]);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [originalOrder, setOriginalOrder] = useState<any>(null);
  const [selectedOrder, setSelectedOrder] = useState<any>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);  // 필터 적용된 총건수
  const [totalAllElements, setTotalAllElements] = useState(0); // 전체 총건수(필터 미적용)
  const [statusFilter, setStatusFilter] = useState<'all'|'PENDING'|'RECEIVED'|'SHIPPING'|'DELIVERED'|'CANCELED'>('all');
  const [activeFilter, setActiveFilter] = useState<string>("all");
  const [statusTotals, setStatusTotals] = useState({
    PENDING: 0,
    RECEIVED: 0,
    SHIPPING: 0,
    DELIVERED: 0,
    CANCELED: 0,
  });
  
  // 🔎 검색어 상태 (입력 중 / 실제 적용된 검색어 분리)
  const [searchTerm, setSearchTerm] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");   // 이 값이 바뀔 때만 서버 재조회

  useEffect(() => {
    fetchOrders(currentPage, statusFilter, appliedSearch);
  }, [currentPage, statusFilter, appliedSearch]);

  const fetchOrders = async (page = 0, status = statusFilter, keyword = appliedSearch) => {
      try {
    const res = await api.get("/api/purchase/list", {
      params: {
        page,
        size: 10,
        status: status !== 'all' ? status : undefined, // 서버 필터
        s: keyword && keyword.trim() !== "" ? keyword.trim() : undefined,
        type: "all",
      },
      withCredentials: false,
    });

      const data = res.data;
      const list = Array.isArray(data.content) ? data.content : [];

      const fetchedOrders = list.map((po: any) => ({
        id: po.id,
        orderCode: po.orderCode,
        supplier: po.supplier,
        orderDate: po.orderDate,
        actualDate: po.actualDeliveryDate,
        totalPrice: (() => {
          const v = (po.totalPrice ?? 0);
          return typeof v === 'number' ? v : Number(v);
        })(),
        status: po.status,
        priority: po.priority,
        notes: po.notes || '',
        mainItemName: po.mainItemName ?? '-',
        itemCount: po.itemCount ?? 0,
      }));

      console.log("✅ 전체 건수:", data.totalElements);
      setOrders(fetchedOrders);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (error) {
      console.error("🚨 발주 목록 조회 실패:", error);
    }
  };

  // 전체 건수(1회 호출)
  const fetchTotalAll = async () => {
    const res = await api.get("/api/purchase/list", { params: { page: 0, size: 1 } }); // status 생략
    setTotalAllElements(res.data.totalElements ?? 0);
  };

  const fetchStatusTotals = async () => {
    try {
      const [p, r, s, d, c] = await Promise.all([
        api.get("/api/purchase/list", { params: { page: 0, size: 1, status: "PENDING"  }, withCredentials: false }),
        api.get("/api/purchase/list", { params: { page: 0, size: 1, status: "RECEIVED" }, withCredentials: false }),
        api.get("/api/purchase/list", { params: { page: 0, size: 1, status: "SHIPPING" }, withCredentials: false }),
        api.get("/api/purchase/list", { params: { page: 0, size: 1, status: "DELIVERED"}, withCredentials: false }),
        api.get("/api/purchase/list", { params: { page: 0, size: 1, status: "CANCELED" }, withCredentials: false }),
      ]);
      setStatusTotals({
        PENDING:   p.data?.totalElements ?? 0,
        RECEIVED:  r.data?.totalElements ?? 0,
        SHIPPING:  s.data?.totalElements ?? 0,
        DELIVERED: d.data?.totalElements ?? 0,
        CANCELED:  c.data?.totalElements ?? 0,
      });
    } catch (e) {
      console.error("🚨 상태별 총계 조회 실패:", e);
    }
  };

  useEffect(() => {
    fetchTotalAll();
    fetchStatusTotals();
  }, []);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Badge className="bg-yellow-100 text-yellow-800 border-yellow-200">대기중</Badge>;
      case 'RECEIVED':
        return <Badge className="bg-green-100 text-green-800 border-green-200">접수됨</Badge>;
      case 'SHIPPING':
        return <Badge className="bg-blue-100 text-blue-800 border-blue-200">배송중</Badge>;
      case 'DELIVERED':
        return <Badge className="bg-purple-100 text-purple-800 border-purple-200">검수완료</Badge>;
      case 'CANCELED':
        return <Badge className="bg-red-100 text-red-800 border-red-100">취소됨</Badge>;
      default:
        return <Badge variant="secondary">알수없음</Badge>;
    }
  };

  const getPriorityBadge = (priority: string) => {
    switch (priority) {
      case 'URGENT':
        return <Badge variant="destructive">우선</Badge>;
      case 'NORMAL':
        return <Badge className="bg-gray-100 text-gray-800 border-gray-200">일반</Badge>;
      default:
        return <Badge variant="secondary">알수없음</Badge>;
    }
  };

  const normalizeDetail = (data: any) => {
    const items = Array.isArray(data.items) ? data.items : [];

    const normItems = items.map((it: any) => {
      const count = Number(it.count ?? 0);
      const unitPrice = Number(it.unitPrice ?? 0);

      return {
        // 화면 표시용
        materialName: it.materialName ?? "-",
        unitPrice,
        totalPrice: Number(it.totalPrice ?? unitPrice * count),
        count,

        // 서버가 내려준 키들 그대로 보존
        id: it.id ?? null,                    // pod.id (상세행 id)
        storeMaterialId: it.storeMaterialId ?? null, // 가맹점 재고 id
        materialId: it.materialId ?? null,    // 본사 재료 id (읽기전용, 디버깅용)
      };
    });

    const computedTotal =
      normItems.reduce((s: number, x: any) => s + Number(x.totalPrice ?? 0), 0) || 0;

    return {
      ...data,
      items: normItems,
      totalPrice: Number(data.totalPrice ?? computedTotal),
      priority: data.priority ?? "NORMAL",
      notes: data.notes ?? data.remark ?? "",
    };
  };

  const handleOrderDetail = async (order: any) => {
    try {
      const res = await api.get(`/api/purchase/detail/${order.id}`);
      const normalized = normalizeDetail(res.data);
      setSelectedOrder(normalized);
      setIsDetailModalOpen(true);
    } catch (err) {
      console.error("❌ 발주 상세 조회 실패:", err);
      toast.error("상세 정보를 불러올 수 없습니다.");
    }
  };
  
  const handleSelectOrder = (id: number) => {
    setSelectedIds(prev =>
      prev.includes(id)
        ? prev.filter(i => i !== id)
        : [...prev, id]
    );
  };

  const handleBulkDelete = async () => {
    if (selectedIds.length === 0) return;
    if (!window.confirm(`${selectedIds.length}건의 발주서를 삭제하시겠습니까?`)) return;

    try {
      await Promise.all(selectedIds.map(id => api.delete(`/api/purchase/${id}`)));
      toast.success(`${selectedIds.length}건의 발주서가 삭제되었습니다.`);
      setOrders(prev => prev.filter(order => !selectedIds.includes(order.id)));
      setSelectedIds([]);
      await fetchOrders(currentPage, statusFilter, appliedSearch);  // 삭제 후 재조회
      await fetchTotalAll();
      await fetchStatusTotals();
      fetchOrders(currentPage);
    } catch (error) {
      console.error("🚨 발주 삭제 실패:", error);
      toast.error("삭제 중 오류가 발생했습니다.");
    }
  };

    const handleStatusChange = async (orderId: number, newStatus: string) => {
    try {
      const order = orders.find(o => o.id === orderId);
      if (!order) return;

      // optimistic UI
      setOrders(prev => prev.map(o => o.id === orderId
        ? { ...o, status: newStatus,
            actualDate: newStatus === "DELIVERED"
              ? new Date().toISOString().split("T")[0]
              : o.actualDate }     
        : o));

      // 3️⃣ 가맹점(내 서버) 상태 변경
      await api.put(`/api/purchase/status/${orderId}`, null, {
        params: { status: newStatus  },
      }); 

      // 4️⃣ 탭을 자동으로 "검수완료"로 전환
      setActiveFilter(newStatus );

      toast.success("검수 완료 및 본사 동기화 요청 완료");

      // 상태 변경 후 재조회
      await fetchOrders(currentPage, statusFilter, appliedSearch);  
      await fetchTotalAll();
      await fetchStatusTotals(); 
    } catch (error) {
      console.error("🚨 상태 변경 및 동기화 실패:", error);
      toast.error("상태 변경에 실패했습니다.");
    }
  };

  const orderColumns: Column[] = [
    {
      key: "select",
      label: "",
      render: (_, row) => (
        <input
          type="checkbox"
          checked={selectedIds.includes(row.id)}
          onChange={() => handleSelectOrder(row.id)}
          className="w-4 h-4 accent-kpi-green"
        />
      ),
    },
    { 
      key: 'orderCode', 
      label: '발주번호', 
      sortable: true,
      render: (value, row) => (
        <div>
          <div 
            className="font-medium text-gray-900 cursor-pointer hover:text-kpi-red transition-colors"
            onClick={() => handleOrderDetail(row)}
          >
            {value}
          </div>
        </div>
      )
    },
    { 
      key: 'supplier', 
      label: '공급업체', 
      sortable: true,
      render: (value) => <span className="font-medium">{value}</span>
    },
    {
      key: 'mainItemName',
      label: '발주품목',
      sortable: true,
      render: (value, row) => (
        <div>
          <div className="font-medium">{value ?? '-'}</div>
          {row.itemCount > 1 && (
            <div className="text-xs text-dark-gray">
              외 {row.itemCount - 1}개
            </div>
          )}
        </div>
      ),
    },
    { 
      key: 'totalPrice', 
      label: '발주금액', 
      sortable: true,
      render: (value) => (
        <span className="font-medium">₩{(value || 0).toLocaleString()}</span>
      )
    },
    { 
      key: 'orderDate', 
      label: '발주주문일', 
      sortable: true,
      render: (value) => (
        <div>
          <div className="text-sm">{value}</div>
        </div>
      )
    },
    { 
      key: 'actualDate', 
      label: '실제납기일', 
      sortable: true,
      render: (value) => (
        <div>
          <div className="text-sm">{value ? value : '-'}</div>
        </div>
      )
    },
    { 
      key: 'priority', 
      label: '우선순위', 
      sortable: true,
      render: (value) => getPriorityBadge(value)
    },
    { 
      key: 'status', 
      label: '상태', 
      sortable: true,
      render: (value) => getStatusBadge(value)
    },
    {
      key: 'actions',
      label: '작업',
      render: (_, row) => (
        <div className="flex gap-2">
          <Button 
            size="sm" 
            variant="outline" 
            onClick={() => handleOrderDetail(row)}
          >
            <Eye className="w-3 h-3 mr-1" />
            상세
          </Button>
          {row.status === 'PENDING' && (
            <Button 
              size="sm" 
              className="bg-kpi-green hover:bg-green-600 text-white"
              onClick={() => handleStatusChange(row.id, 'RECEIVED')}
            >
              <CheckCircle className="w-3 h-3 mr-1" />
              접수
            </Button>
          )}
          {row.status === 'SHIPPING' && (
            <Button
              size="sm"
              className="bg-kpi-green hover:bg-green-600 text-white"
              onClick={() => handleStatusChange(row.id, 'DELIVERED')}
            >
              <CheckCircle className="w-3 h-3 mr-1" />
              검수완료
            </Button>
          )}
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      {/* 발주 요약 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <Card className="p-6 bg-kpi-green text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-green-100">총 발주</p>
              <p className="text-2xl font-bold">{totalAllElements}</p>
            </div>
            <Truck className="w-8 h-8 text-green-200" />
          </div>
        </Card>
        
        <Card className="p-6 bg-kpi-orange text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-orange-100">대기중</p>
              <p className="text-2xl font-bold">{statusTotals.PENDING}</p>
            </div>
            <Clock className="w-8 h-8 text-orange-200" />
          </div>
        </Card>
        
        <Card className="p-6 bg-kpi-purple text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-purple-100">접수됨</p>
              <p className="text-2xl font-bold">{statusTotals.RECEIVED}</p>
            </div>
            <CheckCircle className="w-8 h-8 text-purple-200" />
          </div>
        </Card>
        
        <Card className="p-6 bg-kpi-red text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-red-100">배송중</p>
              <p className="text-2xl font-bold">{statusTotals.SHIPPING}</p>
            </div>
            <Package className="w-8 h-8 text-red-200" />
          </div>
        </Card>
      </div>

      {/* 발주 내역 관리 */}
      <Card className="p-6">
        {/* 첫 줄: 제목, 삭제 버튼, 힌트 */}
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
            <Truck className="w-5 h-5" />
            발주 내역 관리
          </h3>
          <div className="flex items-center gap-3">
            <Button
              size="sm"
              variant="destructive"
              onClick={handleBulkDelete}
              disabled={selectedIds.length === 0}
            >
              <Trash className="w-3 h-3 mr-1" /> 삭제
            </Button>
            <p className="text-sm text-dark-gray">
              발주번호를 클릭하면 상세 정보를 확인할 수 있습니다
            </p>
          </div>
        </div>

        {/* 둘째 줄: 총 n개 항목 + 검색창 */}
        <div className="mb-4">
          <p className="text-sm text-dark-gray mb-2">
            총 {totalAllElements}개 항목
          </p>

          <div className="flex items-center justify-between gap-4">
            {/* 왼쪽: 검색창 */}
            <div className="flex items-center gap-2">
              <Input
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="발주번호, 공급업체, 품목명 검색"
                className="h-9 w-56"
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    setCurrentPage(0);
                    setAppliedSearch(searchTerm);
                  }
                }}
              />
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  setCurrentPage(0);
                  setAppliedSearch(searchTerm);
                }}
              >
                검색
              </Button>
            </div>
            
            {/* 오른쪽 : 상태 필터 버튼들 */}
            <div className="flex flex-wrap gap-2">
              <button
                onClick={() => {
                  setStatusFilter('all');
                  setCurrentPage(0);
                  fetchOrders(0, 'all', appliedSearch);
                }}
                className={`px-4 py-2 rounded-lg text-sm font-medium ${
                  statusFilter === 'all'
                    ? 'bg-kpi-red text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                전체
              </button>

              <button
                onClick={() => {
                  setStatusFilter('PENDING');
                  setCurrentPage(0);
                  fetchOrders(0, 'PENDING', appliedSearch);
                }}
                className={`px-4 py-2 rounded-lg text-sm font-medium ${
                  statusFilter === 'PENDING'
                    ? 'bg-kpi-red text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                대기중 ({statusTotals.PENDING})
              </button>

              <button
                onClick={() => {
                  setStatusFilter('RECEIVED');
                  setCurrentPage(0);
                  fetchOrders(0, 'RECEIVED', appliedSearch);
                }}
                className={`px-4 py-2 rounded-lg text-sm font-medium ${
                  statusFilter === 'RECEIVED'
                    ? 'bg-kpi-red text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                접수됨 ({statusTotals.RECEIVED})
              </button>

              <button
                onClick={() => {
                  setStatusFilter('SHIPPING');
                  setCurrentPage(0);
                  fetchOrders(0, 'SHIPPING', appliedSearch);
                }}
                className={`px-4 py-2 rounded-lg text-sm font-medium ${
                  statusFilter === 'SHIPPING'
                    ? 'bg-kpi-red text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                배송중 ({statusTotals.SHIPPING})
              </button>

              <button
                onClick={() => {
                  setStatusFilter('DELIVERED');
                  setCurrentPage(0);
                  fetchOrders(0, 'DELIVERED', appliedSearch);
                }}
                className={`px-4 py-2 rounded-lg text-sm font-medium ${
                  statusFilter === 'DELIVERED'
                    ? 'bg-kpi-red text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                검수완료 ({statusTotals.DELIVERED})
              </button>

              <button
                onClick={() => {
                  setStatusFilter('CANCELED');
                  setCurrentPage(0);
                  fetchOrders(0, 'CANCELED', appliedSearch);
                }}
                className={`px-4 py-2 rounded-lg text-sm font-medium ${
                  statusFilter === 'CANCELED'
                    ? 'bg-kpi-red text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                취소됨 ({statusTotals.CANCELED})
              </button>
            </div>
          </div>
        </div>

       <DataTable
          columns={orderColumns}
          data={orders}
          title=""
          showActions={false}
          hideSearch={true}          // DataTable 안 검색바는 안 씀
          hideHeaderSummary={true}
          serverSidePagination
          currentPage={currentPage + 1}
          totalPageCount={totalPages}
          totalElements={totalElements}
          totalDisplayCount={totalAllElements}
          pageSize={10}
          pageBlockSize={10}
          onPageChange={(page) => {
            const zeroBased = page - 1;
            setCurrentPage(zeroBased);
            fetchOrders(zeroBased, statusFilter, appliedSearch);
          }}
        />

      </Card>

      {/* 발주 상세 모달 */}
      <Dialog
        open={isDetailModalOpen}
        onOpenChange={(open) => {
          setIsDetailModalOpen(open);
          if (!open) {
            // 모달 닫힐 때 초기화
            setSelectedOrder(null);
            setOriginalOrder(null);
            setIsEditMode(false);
          }
        }}>
        {/* ⬇️ 스크롤 가능 영역: 최대 높이 제한 + 내부 스크롤 */}
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Truck className="w-5 h-5" />
              발주 상세 정보
            </DialogTitle>
            <DialogDescription>
              선택한 품목들의 발주 정보를 확인하고 수정할 수 있습니다.
            </DialogDescription>
          </DialogHeader>

          {selectedOrder && Array.isArray(selectedOrder.items) && (
            <div className="space-y-6">
              {/* 기본 정보 */}
              <div className="grid grid-cols-2 gap-4 p-4 bg-gray-50 rounded-lg">
                <div>
                  <span className="text-sm text-dark-gray">발주번호</span>
                  <p className="font-medium">{selectedOrder.orderCode}</p>
                </div>
                <div>
                  <span className="text-sm text-dark-gray">공급업체</span>
                  <p className="font-medium">{selectedOrder.supplier}</p>
                </div>
                <div>
                  <span className="text-sm text-dark-gray">발주일</span>
                  <p className="font-medium">{selectedOrder.orderDate}</p>
                </div>
                <div>
                  <span className="text-sm text-dark-gray">실제납기일</span>
                  <p className="font-medium">
                    {selectedOrder?.actualDeliveryDate && selectedOrder.actualDeliveryDate.trim() !== '' ? selectedOrder.actualDeliveryDate : '-'}
                  </p>
                </div>
                <div>
                  <span className="text-sm text-dark-gray">우선순위</span>
                  <div>{getPriorityBadge(selectedOrder.priority)}</div>
                </div>
                <div>
                  <span className="text-sm text-dark-gray">상태</span>
                  <div>{getStatusBadge(selectedOrder.status)}</div>
                </div>
              </div>

              {/* 발주 품목 */}
              <div>
                <h3 className="font-semibold mb-3">발주 품목</h3>

                {/* ⬇️ 테이블 영역만 별도 스크롤. 헤더 고정 */}
                <div className="border rounded-lg overflow-hidden max-h-[45vh] overflow-y-auto">
                  <table className="w-full">
                    <thead className="bg-gray-50 sticky top-0 z-10">
                      <tr>
                        <th className="px-4 py-2 text-left">품목명</th>
                        <th className="px-4 py-2 text-center">수량</th>
                        <th className="px-4 py-2 text-right">단가</th>
                        {isEditMode && <th className="px-4 py-2 text-right">작업</th>}
                      </tr>
                    </thead>
                    <tbody>
                      {(selectedOrder.items ?? []).map((item: any, index: number) => (
                        <tr key={index} className="border-t">
                          <td className="px-4 py-3 font-medium">{item.materialName}</td>

                          <td className="px-4 py-3 text-center">
                            {isEditMode ? (
                              <Input
                                type="number"
                                value={item.count}
                                onChange={(e) => {
                                  const newCount = Number(e.target.value) || 0;
                                  setSelectedOrder((prev: any) => {
                                    const nextItems = prev.items.map((it: any, i: number) =>
                                      i === index
                                        ? {
                                            ...it,
                                            count: newCount,
                                            totalPrice: Number(it.unitPrice ?? 0) * newCount,
                                          }
                                        : it
                                    );
                                    const nextTotal = nextItems.reduce((s: number, x: any) => s + Number(x.totalPrice ?? 0), 0);
                                    return { ...prev, items: nextItems, totalPrice: nextTotal };
                                  });
                                }}
                                className="w-20 text-center"
                                min="1"
                              />
                            ) : (
                              <span>{item.count}</span>
                            )}
                          </td>

                          <td className="px-4 py-3 text-right">
                            ₩{(item.unitPrice || 0).toLocaleString()}
                          </td>
                          <td className="px-4 py-3 text-right font-medium">
                            ₩{(item.totalPrice || 0).toLocaleString()}
                          </td>

                          {isEditMode && (
                            <td className="px-4 py-3 text-right">
                              <Button
                                size="sm"
                                variant="destructive"
                                onClick={async () => {
                                  if (!window.confirm('이 품목을 삭제하시겠습니까?')) return;
                                  try {
                                    await api.delete(`/api/purchase/detail/item/${item.id}`);
                                    toast.success('품목이 삭제되었습니다.');
                                    setSelectedOrder((prev: any) => {
                                      const updatedItems = prev.items.filter((i: any) => i.id !== item.id);
                                      if (updatedItems.length === 0) {
                                        setIsDetailModalOpen(false);
                                        fetchOrders(currentPage);
                                      }
                                      return { ...prev, items: updatedItems };
                                    });
                                  } catch (error) {
                                    console.error('🚨 품목 삭제 실패:', error);
                                    toast.error('품목 삭제에 실패했습니다.');
                                  }
                                }}
                              >
                                삭제
                              </Button>
                            </td>
                          )}
                        </tr>
                      ))}
                    </tbody>

                    <tfoot className="bg-gray-50">
                      <tr>
                        <td colSpan={3} className="px-4 py-3 text-right">
                          총 발주 금액:
                        </td>
                        <td className="px-4 py-3 text-right text-lg">
                          ₩{selectedOrder.totalPrice.toLocaleString()}
                        </td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              </div>

              <div>
                {/* 우선순위 */}
                <div>
                  <span className="text-sm text-dark-gray">우선순위</span>
                  {isEditMode ? (
                    <select
                      value={selectedOrder.priority}
                      onChange={(e) => setSelectedOrder({ ...selectedOrder, priority: e.target.value })}
                      className="border rounded-md px-2 py-1 text-sm"
                    >
                      <option value="NORMAL">일반</option>
                      <option value="URGENT">우선</option>
                    </select>
                  ) : (
                    <div>{getPriorityBadge(selectedOrder.priority)}</div>
                  )}
                </div>

                {/* 특이사항 */}
                <div className="mt-4">
                  <h3 className="font-semibold mb-2">특이사항</h3>
                  {isEditMode ? (
                    <textarea
                      value={selectedOrder.notes}
                      onChange={(e) => setSelectedOrder({ ...selectedOrder, notes: e.target.value })}
                      className="w-full border rounded-lg p-2 text-sm"
                      rows={3}
                    />
                  ) : (
                    <p className="p-3 bg-gray-50 rounded-lg">{selectedOrder.notes || '-'}</p>
                  )}
                </div>
              </div>

              {/* 수정 / 저장 / 취소 버튼 */}
              <div className="flex justify-end gap-3 pt-4 border-t mt-6">
                {isEditMode ? (
                  <>
                    <Button
                      onClick={async () => {
                        try {
                          if (selectedOrder?.status !== 'PENDING') {
                            toast.error('대기중 상태에서만 수정할 수 있습니다.');
                            return;
                          }
                          const payload = {
                            priority: selectedOrder.priority,
                            notes: selectedOrder.notes ?? "",
                            items: (selectedOrder.items ?? [])
                              .map((row: any) => ({
                                storeMaterialId: row.storeMaterialId,        // 무조건 이 값만 신뢰
                                count: Number(row.count ?? 0),
                              }))
                              .filter((it: any) => it.storeMaterialId && it.count > 0),
                          };

                          await api.put(`/api/purchase/${selectedOrder.id}`, payload, {
                            withCredentials: false,
                            headers: { 'Content-Type': 'application/json' },
                          });

                          toast.success('발주 정보가 수정되었습니다.');
                          setIsEditMode(false);
                          // 저장 후 원본 백업은 의미가 없으므로 비움
                          setOriginalOrder(null);

                          // 목록/상단카드 동기화 + 상세 다시 읽어 최신값 반영
                          await Promise.all([
                            fetchOrders(currentPage, statusFilter),
                            fetchTotalAll(),
                            fetchStatusTotals(),
                          ]);

                          // 모달 열어둔 상태라면 상세도 최신으로 갱신
                          if (selectedOrder?.id) {
                          const detail = await api.get(`/api/purchase/detail/${selectedOrder.id}`);
                          setSelectedOrder(normalizeDetail(detail.data));
                        }
                        } catch (error) {
                          console.error('🚨 발주 수정 실패:', error);
                          toast.error('발주 수정에 실패했습니다.');
                        }
                      }}
                      className="bg-kpi-green text-white hover:bg-green-600"
                    >
                      저장
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => {
                        if (originalOrder) setSelectedOrder(JSON.parse(JSON.stringify(originalOrder)));
                        setIsEditMode(false);
                      }}
                    >
                      취소
                    </Button>
                  </>
                ) : (
                  <Button
                    variant="outline"
                    onClick={() => setIsEditMode(true)}
                    disabled={selectedOrder?.status !== 'PENDING'}   // 🔒 PENDING만 수정 진입
                    title={selectedOrder?.status !== 'PENDING' ? '대기중 상태에서만 수정 가능합니다' : undefined}
                  >
                    수정
                  </Button>
                )}
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

    </div>
  );
}