import React, { useState } from 'react';
import { Package, Plus, AlertTriangle, TrendingDown, TrendingUp, Search, Filter, Eye, Edit3, ShoppingCart, Download, Calendar, MapPin, DollarSign, Activity, ArrowUp, ArrowDown, BarChart3 } from 'lucide-react';
import { Card } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Badge } from '../ui/badge';
import { DataTable, Column } from '../Common/DataTable';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '../ui/dialog';
import { Label } from '../ui/label';
import { Checkbox } from '../ui/checkbox';
import { DownloadToggle } from '../Common/DownloadToggle';
import { toast } from 'sonner';

export function InventoryStatus() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [modalType, setModalType] = useState<'restock' | 'adjust' | 'register'>('restock');
  const [selectedItem, setSelectedItem] = useState<any>(null);
  const [selectedItems, setSelectedItems] = useState<number[]>([]);
  const [isCartModalOpen, setIsCartModalOpen] = useState(false);
  const [cartItems, setCartItems] = useState<any[]>([]);

  const [priority, setPriority] = useState("NORMAL");
  const [remark, setRemark] = useState("");

  // 샘플 재고 데이터
  const [inventory, setInventory] = useState([
    {
      id: 1,
      name: '양파',
      category: '채소',
      currentStock: 45,
      minStock: 20,
      maxStock: 100,
      unit: 'kg',
      location: 'A-01',
      supplier: '신선마트',
      lastUpdated: '2024-01-15',
      unitPrice: 3000,
      status: 'normal'
    },
    {
      id: 2,
      name: '토마토',
      category: '채소',
      currentStock: 15,
      minStock: 30,
      maxStock: 80,
      unit: 'kg',
      location: 'A-02',
      supplier: '신선마트',
      lastUpdated: '2024-01-14',
      unitPrice: 5000,
      status: 'low'
    },
    {
      id: 3,
      name: '소고기 등심',
      category: '육류',
      currentStock: 8,
      minStock: 15,
      maxStock: 50,
      unit: 'kg',
      location: 'B-01',
      supplier: '프리미엄 정육점',
      lastUpdated: '2024-01-14',
      unitPrice: 35000,
      status: 'critical'
    },
    {
      id: 4,
      name: '치즈',
      category: '유제품',
      currentStock: 0,
      minStock: 10,
      maxStock: 30,
      unit: 'kg',
      location: 'C-01',
      supplier: '델리카트',
      lastUpdated: '2024-01-13',
      unitPrice: 12000,
      status: 'out'
    },
    {
      id: 5,
      name: '밀가루',
      category: '제빵재료',
      currentStock: 75,
      minStock: 20,
      maxStock: 100,
      unit: 'kg',
      location: 'D-01',
      supplier: '베이커리 서플라이',
      lastUpdated: '2024-01-15',
      unitPrice: 2500,
      status: 'normal'
    }
  ]);

  const getStockStatus = (item: any) => {
    if (item.currentStock === 0) return 'out';
    if (item.currentStock <= item.minStock * 0.5) return 'critical';
    if (item.currentStock <= item.minStock) return 'low';
    return 'normal';
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'out':
        return <Badge variant="destructive">재고없음</Badge>;
      case 'critical':
        return <Badge className="bg-red-100 text-red-800 border-red-200">매우부족</Badge>;
      case 'low':
        return <Badge className="bg-orange-100 text-orange-800 border-orange-200">부족</Badge>;
      case 'normal':
        return <Badge className="bg-green-100 text-green-800 border-green-200">정상</Badge>;
      default:
        return <Badge variant="secondary">알수없음</Badge>;
    }
  };

  const filteredInventory = inventory.map(item => ({
    ...item,
    status: getStockStatus(item)
  }));

  const handleItemSelect = (itemId: number, checked: boolean) => {
    setSelectedItems(prev => 
      checked 
        ? [...prev, itemId]
        : prev.filter(id => id !== itemId)
    );
  };

  const handleSelectAll = (checked: boolean) => {
    setSelectedItems(checked ? inventory.map(item => item.id) : []);
  };

  const handleBulkOrder = () => {
    if (selectedItems.length === 0) {
      toast.error('발주할 품목을 선택해주세요.');
      return;
    }
    
    const selectedInventoryItems = inventory.filter(item => selectedItems.includes(item.id));
    const cartData = selectedInventoryItems.map(item => ({
      ...item,
      orderQuantity: Math.max(item.maxStock - item.currentStock, item.minStock),
      totalPrice: (Math.max(item.maxStock - item.currentStock, item.minStock)) * item.unitPrice
    }));
    
    setCartItems(cartData);
    setIsCartModalOpen(true);
  };

  const [status, setStatus] = useState<string | undefined>(undefined); // 'PENDING' | 'RECEIVED' | ...
  const [q, setQ] = useState('');       

  // 다운로드 기능
  const handleDownload = async (format: 'excel' | 'pdf') => {
    try {
      // 파일 생성 시뮬레이션을 위한 지연
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      // 데이터가 없는 경우 처리
      if (!inventory || inventory.length === 0) {
        throw new Error('다운로드할 재고 데이터가 없습니다.');
      }

      const exportData = inventory.map(item => ({
        품목명: item.name || '-',
        카테고리: item.category || '-',
        현재재고: `${item.currentStock || 0}${item.unit || ''}`,
        최소재고: `${item.minStock || 0}${item.unit || ''}`,
        최대재고: `${item.maxStock || 0}${item.unit || ''}`,
        공급업체: item.supplier || '-',
        단가: `${(item.unitPrice || 0).toLocaleString()}원`,
        재고상태: getStatusText(item.status),
        마지막업데이트: item.lastUpdated || '-'
      }));

      if (format === 'excel') {
        const csvContent = [
          Object.keys(exportData[0]).join(','),
          ...exportData.map(row => Object.values(row).map(v => `"${v}"`).join(','))
        ].join('\n');
        
        const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `재고현황_${new Date().toISOString().split('T')[0]}.csv`;
        link.click();
      } else {
        // HTML 보고서 생성 (인쇄용)
        const reportWindow = window.open('', '_blank');
        const htmlContent = `
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>재고 현황 보고서</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700&display=swap');
        
        * { margin: 0; padding: 0; box-sizing: border-box; }
        
        body { 
            font-family: 'Noto Sans KR', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            line-height: 1.6;
            color: #333;
            background: #fff;
            padding: 40px;
        }
        
        .header {
            text-align: center;
            margin-bottom: 40px;
            border-bottom: 3px solid #14213D;
            padding-bottom: 20px;
        }
        
        .header h1 {
            color: #14213D;
            font-size: 28px;
            font-weight: 700;
            margin-bottom: 10px;
        }
        
        .header-info {
            color: #666;
            font-size: 14px;
        }
        
        .summary {
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            padding: 20px;
            border-radius: 12px;
            margin-bottom: 30px;
            border-left: 5px solid #06D6A0;
        }
        
        .summary h2 {
            color: #14213D;
            font-size: 18px;
            margin-bottom: 10px;
            font-weight: 600;
        }
        
        .summary-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
        }
        
        .summary-item {
            background: white;
            padding: 15px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        
        .summary-label {
            font-size: 12px;
            color: #666;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 5px;
        }
        
        .summary-value {
            font-size: 18px;
            font-weight: 600;
            color: #14213D;
        }
        
        .item-grid {
            display: grid;
            gap: 20px;
        }
        
        .item-card {
            border: 1px solid #e0e0e0;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 4px 6px rgba(0,0,0,0.05);
        }
        
        .item-header {
            background: linear-gradient(135deg, #14213D 0%, #1a2b4d 100%);
            color: white;
            padding: 16px 20px;
            font-weight: 600;
            font-size: 16px;
        }
        
        .item-content {
            padding: 20px;
        }
        
        .item-details {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 15px;
        }
        
        .detail-item {
            display: flex;
            flex-direction: column;
        }
        
        .detail-label {
            font-size: 11px;
            color: #666;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 4px;
            font-weight: 500;
        }
        
        .detail-value {
            font-size: 14px;
            color: #333;
            font-weight: 500;
        }
        
        .status-badge {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        
        .status-normal { background: #d4edda; color: #155724; }
        .status-low { background: #fff3cd; color: #856404; }
        .status-out { background: #f8d7da; color: #721c24; }
        
        .footer {
            margin-top: 40px;
            text-align: center;
            color: #666;
            font-size: 12px;
            border-top: 1px solid #eee;
            padding-top: 20px;
        }
        
        @media print {
            body { padding: 20px; }
            .item-card { break-inside: avoid; }
            .header { break-after: avoid; }
        }
        
        @page {
            margin: 2cm;
            size: A4;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>📦 재고 현황 보고서</h1>
        <div class="header-info">
            <div>생성일시: ${new Date().toLocaleString('ko-KR')}</div>
        </div>
    </div>
    
    <div class="summary">
        <h2>📊 보고서 요약</h2>
        <div class="summary-grid">
            <div class="summary-item">
                <div class="summary-label">총 재고 품목</div>
                <div class="summary-value">${exportData.length}개</div>
            </div>
            <div class="summary-item">
                <div class="summary-label">보고서 생성</div>
                <div class="summary-value">${new Date().toLocaleDateString('ko-KR')}</div>
            </div>
            <div class="summary-item">
                <div class="summary-label">생성 시간</div>
                <div class="summary-value">${new Date().toLocaleTimeString('ko-KR')}</div>
            </div>
        </div>
    </div>

    <div class="item-grid">
        ${exportData.map((item, index) => `
        <div class="item-card">
            <div class="item-header">
                #${index + 1} ${item.품목명}
            </div>
            <div class="item-content">
                <div class="item-details">
                    <div class="detail-item">
                        <div class="detail-label">카테고리</div>
                        <div class="detail-value">${item.카테고리}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">현재재고</div>
                        <div class="detail-value">${item.현재재고}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">최소재고</div>
                        <div class="detail-value">${item.최소재고}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">최대재고</div>
                        <div class="detail-value">${item.최대재고}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">공급업체</div>
                        <div class="detail-value">${item.공급업체}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">단가</div>
                        <div class="detail-value">${item.단가}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">재고상태</div>
                        <div class="detail-value">
                            <span class="status-badge ${item.재고상태 === '정상' ? 'status-normal' : item.재고상태 === '부족' ? 'status-low' : 'status-out'}">${item.재고상태}</span>
                        </div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">마지막업데이트</div>
                        <div class="detail-value">${item.마지막업데이트}</div>
                    </div>
                </div>
            </div>
        </div>
        `).join('')}
    </div>
    
    <div class="footer">
        <div>FranFriend ERP System - 재고 관리 보고서</div>
        <div>본 보고서는 ${new Date().toLocaleString('ko-KR')}에 자동 생성되었습니다.</div>
    </div>
    
    <script>
        window.onload = function() {
            setTimeout(() => {
                window.print();
            }, 500);
        };
    </script>
</body>
</html>`;
        
        if (reportWindow) {
          reportWindow.document.write(htmlContent);
          reportWindow.document.close();
        }
      }
    } catch (error) {
      console.error('Download error:', error);
      throw error;
    }
  };

  // 재고 상태 텍스트 변환
  const getStatusText = (status: string) => {
    switch (status) {
      case 'low': return '부족';
      case 'critical': return '심각';
      case 'normal': return '정상';
      case 'overstock': return '과재고';
      default: return '정상';
    }
  };

  const handleRegisterItem = () => {
    setSelectedItem(null);
    setModalType('register');
    setIsModalOpen(true);
  };

  const handleItemDetail = (item: any) => {
    setSelectedItem(item);
    setIsDetailModalOpen(true);
  };

  const handleRestock = (item: any) => {
    setSelectedItem(item);
    setModalType('restock');
    setIsModalOpen(true);
  };

  const handleAdjust = (item: any) => {
    setSelectedItem(item);
    setModalType('adjust');
    setIsModalOpen(true);
  };

  const handleUpdateMinStock = (minStock: number) => {
    if (selectedItem) {
      setInventory(prev => prev.map(item => 
        item.id === selectedItem.id 
          ? { ...item, minStock, status: getStockStatus({ ...item, minStock }) }
          : item
      ));
      toast.success('최소 재고량이 업데이트되었습니다.');
    }
  };

  const inventoryColumns: Column[] = [
    {
      key: 'select',
      label: (
        <Checkbox
          checked={selectedItems.length === inventory.length && inventory.length > 0}
          onCheckedChange={handleSelectAll}
          className="border-gray-300"
        />
      ),
      render: (_, row) => (
        <Checkbox
          checked={selectedItems.includes(row.id)}
          onCheckedChange={(checked:any) => handleItemSelect(row.id, checked as boolean)}
          className="border-gray-300"
        />
      ),
      width: '60px'
    },
    { 
      key: 'name', 
      label: '품목정보', 
      sortable: true,
      render: (value, row) => (
        <div>
          <div 
            className="font-medium text-gray-900 cursor-pointer hover:text-kpi-red transition-colors"
            onClick={() => handleItemDetail(row)}
          >
            {value}
          </div>
          <div className="text-sm text-dark-gray">{row.category}</div>
        </div>
      )
    },
    { 
      key: 'currentStock', 
      label: '현재재고', 
      sortable: true,
      render: (value, row) => (
        <div className="text-left">
          <div className="font-medium">{value}{row.unit}</div>
          <div className="text-xs text-dark-gray">최소: {row.minStock}{row.unit}</div>
        </div>
      )
    },
    { 
      key: 'status', 
      label: '상태', 
      sortable: true,
      render: (_, row) => getStatusBadge(getStockStatus(row))
    },

    { 
      key: 'lastUpdated', 
      label: '최종업데이트', 
      sortable: true,
      render: (value) => <span className="text-sm text-dark-gray">{value}</span>
    },
    {
      key: 'actions',
      label: '작업',
      render: (_, row) => (
        <div className="flex gap-2">
          <Button 
            size="sm" 
            variant="outline" 
            onClick={() => {
              setSelectedItem(row);
              setModalType('restock');
              setIsModalOpen(true);
            }}
            className="border-kpi-green text-kpi-green hover:bg-green-50"
          >
            입고
          </Button>
          <Button 
            size="sm" 
            variant="outline" 
            onClick={() => {
              setSelectedItem(row);
              setModalType('adjust');
              setIsModalOpen(true);
            }}
          >
            조정
          </Button>
        </div>
      )
    }
  ];

  // 재고 요약 통계
  const totalItems = inventory.length;
  const lowStockItems = inventory.filter(item => getStockStatus(item) === 'low').length;
  const criticalStockItems = inventory.filter(item => getStockStatus(item) === 'critical').length;
  const outOfStockItems = inventory.filter(item => getStockStatus(item) === 'out').length;

  return (
    <div className="space-y-6">
      {/* 재고 요약 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <Card className="p-6 bg-kpi-green text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-green-100">총 품목수</p>
              <p className="text-2xl font-bold">{totalItems}</p>
            </div>
            <Package className="w-8 h-8 text-green-200" />
          </div>
        </Card>
        
        <Card className="p-6 bg-kpi-orange text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-orange-100">부족 품목</p>
              <p className="text-2xl font-bold">{lowStockItems}</p>
            </div>
            <TrendingDown className="w-8 h-8 text-orange-200" />
          </div>
        </Card>
        
        <Card className="p-6 bg-kpi-red text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-red-100">매우부족</p>
              <p className="text-2xl font-bold">{criticalStockItems}</p>
            </div>
            <AlertTriangle className="w-8 h-8 text-red-200" />
          </div>
        </Card>
        
        <Card className="p-6 bg-kpi-purple text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-purple-100">재고없음</p>
              <p className="text-2xl font-bold">{outOfStockItems}</p>
            </div>
            <Package className="w-8 h-8 text-purple-200" />
          </div>
        </Card>
      </div>

      {/* 재고 현황 목록 */}
      <Card className="p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
            <Package className="w-5 h-5" />
            재고 현황 목록
          </h3>
          <div className="flex gap-3">
            <DownloadToggle
              onDownload={handleDownload}
              filename={`재고현황_${new Date().toISOString().split('T')[0]}`}
            />
            <Button 
              onClick={handleRegisterItem}
              variant="outline"
              className="border-kpi-green text-kpi-green hover:bg-green-50"
            >
              <Plus className="w-4 h-4 mr-2" />
              자재 등록
            </Button>
            <Button 
              onClick={handleBulkOrder}
              className="bg-kpi-red hover:bg-red-600 text-white relative"
            >
              <ShoppingCart className="w-4 h-4 mr-2" />
              발주 등록 {selectedItems.length > 0 && `(${selectedItems.length})`}
            </Button>
          </div>
        </div>

        <DataTable
          columns={inventoryColumns}
          data={filteredInventory}
          title=""
          searchPlaceholder="품목명 또는 카테고리 검색"
          showActions={false}
          filters={[
            { label: '정상', value: 'normal' },
            { label: '부족', value: 'low' },
            { label: '매우부족', value: 'critical' },
            { label: '재고없음', value: 'out' }
          ]}
        />
      </Card>

      {/* 자재 등록/조정 모달 */}
      {isModalOpen && (
        <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>
                {modalType === 'register' ? '새 자재 등록' : 
                 modalType === 'restock' ? '재고 입고' : '재고 조정'}
              </DialogTitle>
              <DialogDescription>
                {modalType === 'register' ? '새로운 자재를 등록합니다.' : 
                 modalType === 'restock' ? '재고를 입고 처리합니다.' : '재고 수량을 조정합니다.'}
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4">
              {modalType === 'register' ? (
                <>
                  <div>
                    <Label htmlFor="itemName">품목명 *</Label>
                    <Input id="itemName" placeholder="품목명을 입력하세요" className="mt-1" />
                  </div>
                  <div>
                    <Label htmlFor="category">카테고리 *</Label>
                    <Input id="category" placeholder="카테고리를 입력하세요" className="mt-1" />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="unit">단위</Label>
                      <select className="mt-1 w-full p-2 border border-gray-300 rounded-md">
                        <option value="kg">kg</option>
                        <option value="g">g</option>
                        <option value="개">개</option>
                        <option value="박스">박스</option>
                      </select>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="minStock">최소재고 *</Label>
                      <Input id="minStock" type="number" placeholder="20" className="mt-1" />
                    </div>
                    <div>
                      <Label htmlFor="maxStock">최대재고 *</Label>
                      <Input id="maxStock" type="number" placeholder="100" className="mt-1" />
                    </div>
                  </div>
                  <div>
                    <Label htmlFor="supplier">공급업체</Label>
                    <Input id="supplier" placeholder="공급업체명" className="mt-1" />
                  </div>
                  <div>
                    <Label htmlFor="unitPrice">단가</Label>
                    <Input id="unitPrice" type="number" placeholder="단가를 입력하세요" className="mt-1" />
                  </div>
                </>
              ) : (
                <>
                  <div>
                    <Label>품목명</Label>
                    <p className="mt-1 p-2 bg-gray-50 rounded">{selectedItem?.name}</p>
                  </div>
                  <div>
                    <Label htmlFor="quantity">
                      {modalType === 'restock' ? '입고 수량' : '조정 수량'} *
                    </Label>
                    <Input 
                      id="quantity" 
                      type="number" 
                      placeholder={modalType === 'restock' ? '입고할 수량' : '조정할 수량'}
                      className="mt-1" 
                    />
                  </div>
                  {modalType === 'adjust' && (
                    <div>
                      <Label htmlFor="reason">조정 사유</Label>
                      <select className="mt-1 w-full p-2 border border-gray-300 rounded-md">
                        <option value="">사유 선택</option>
                        <option value="damage">손상</option>
                        <option value="expired">유통기한 만료</option>
                        <option value="loss">분실</option>
                        <option value="inventory">재고조사</option>
                        <option value="other">기타</option>
                      </select>
                    </div>
                  )}
                  <div>
                    <Label htmlFor="notes">비고</Label>
                    <Input id="notes" placeholder="특이사항이 있다면 입력하세요" className="mt-1" />
                  </div>
                </>
              )}
            </div>
            <div className="flex gap-3 pt-4">
              <Button 
                onClick={() => {
                  toast.success(
                    modalType === 'register' ? '새 자재가 등록되었습니다.' :
                    modalType === 'restock' ? '재고 입고가 완료되었습니다.' : '재고가 조정되었습니다.'
                  );
                  setIsModalOpen(false);
                }}
                className="flex-1 bg-kpi-green hover:bg-green-600 text-white"
              >
                {modalType === 'register' ? '등록' : modalType === 'restock' ? '입고' : '조정'}
              </Button>
              <Button variant="outline" onClick={() => setIsModalOpen(false)} className="flex-1">
                취소
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      )}

      {/* 발주 장바구니 모달 */}
      {isCartModalOpen && (
        <Dialog open={isCartModalOpen} onOpenChange={setIsCartModalOpen}>
          <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <ShoppingCart className="w-5 h-5" />
                발주 장바구니 ({cartItems.length}개 품목)
              </DialogTitle>
              <DialogDescription>
                선택한 품목들의 발주 정보를 확인하고 수정할 수 있습니다.
              </DialogDescription>
            </DialogHeader>
            
            {cartItems.length > 0 && (
              <div className="space-y-4">
                <div className="border rounded-lg overflow-hidden">
                  <table className="w-full">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-3 text-left">품목명</th>
                        <th className="px-4 py-3 text-center">현재재고</th>
                        <th className="px-4 py-3 text-center">발주수량</th>
                        <th className="px-4 py-3 text-right">단가</th>
                        <th className="px-4 py-3 text-right">금액</th>
                      </tr>
                    </thead>
                    <tbody>
                      {cartItems.map((item, index) => (
                        <tr key={index} className="border-t">
                          <td className="px-4 py-3">
                            <div>
                              <div className="font-medium">{item.name}</div>
                              <div className="text-sm text-gray-600">{item.category}</div>
                            </div>
                          </td>
                          <td className="px-4 py-3 text-center">{item.currentStock}{item.unit}</td>
                          <td className="px-4 py-3 text-center">
                            <Input 
                              type="number" 
                              value={item.orderQuantity}
                              onChange={(e) => {
                                const newQuantity = parseInt(e.target.value) || 0;
                                setCartItems(prev => prev.map(cartItem => 
                                  cartItem.id === item.id 
                                    ? { ...cartItem, orderQuantity: newQuantity, totalPrice: newQuantity * cartItem.unitPrice }
                                    : cartItem
                                ));
                              }}
                              className="w-20 text-center"
                              min="1"
                            />
                          </td>
                          <td className="px-4 py-3 text-right">₩{item.unitPrice.toLocaleString()}</td>
                          <td className="px-4 py-3 text-right font-medium">₩{item.totalPrice.toLocaleString()}</td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot className="bg-gray-50">
                      <tr>
                        <td colSpan={4} className="px-4 py-3 text-right font-semibold">총 발주 금액:</td>
                        <td className="px-4 py-3 text-right font-bold text-lg">
                          ₩{cartItems.reduce((sum, item) => sum + item.totalPrice, 0).toLocaleString()}
                        </td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
                
                {/* 추가 입력 영역 */}
                <div className="space-y-4">
                  {/* 우선순위 선택 */}
                  <div>
                    <h3 className="font-semibold mb-2">우선순위</h3>
                    <div className="flex items-center gap-6">
                      <label className="flex items-center gap-2">
                        <input 
                          type="radio" 
                          name="priority" 
                          value="NORMAL" 
                          defaultChecked 
                          onChange={() => setPriority("NORMAL")} 
                        />
                        <span>일반</span>
                      </label>
                      <label className="flex items-center gap-2">
                        <input 
                          type="radio" 
                          name="priority" 
                          value="URGENT" 
                          onChange={() => setPriority("URGENT")} 
                        />
                        <span className="text-red-600 font-medium">우선</span>
                      </label>
                    </div>
                  </div>

                  {/* 특이사항 입력 */}
                  <div>
                    <h3 className="font-semibold mb-2">특이사항</h3>
                    <textarea
                      value={remark}
                      onChange={(e) => setRemark(e.target.value)}
                      placeholder="특이사항을 입력하세요."
                      className="w-full border rounded-md p-2 text-sm focus:outline-none focus:ring-2 focus:ring-kpi-red resize-none"
                      rows={3}
                    />
                  </div>
                </div>      

                <div className="flex gap-3 pt-4">
                  <Button 
                    onClick={() => {
                      toast.success(`${cartItems.length}개 품목의 발주가 등록되었습니다.`);
                      setIsCartModalOpen(false);
                      setSelectedItems([]);
                      setCartItems([]);
                    }}
                    className="flex-1 bg-kpi-red hover:bg-red-600 text-white"
                  >
                    발주 등록
                  </Button>
                  <Button variant="outline" onClick={() => setIsCartModalOpen(false)} className="flex-1">
                    닫기
                  </Button>
                </div>
              </div>
            )}
          </DialogContent>
        </Dialog>
      )}

      {/* 품목 상세 모달 */}
      {isDetailModalOpen && selectedItem && (
        <Dialog open={isDetailModalOpen} onOpenChange={setIsDetailModalOpen}>
          <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-3">
                <Package className="w-6 h-6 text-kpi-green" />
                <div>
                  <div className="text-xl font-bold">{selectedItem.name}</div>
                  <div className="text-sm font-normal text-gray-600">{selectedItem.category}</div>
                </div>
              </DialogTitle>
              <DialogDescription>
                품목의 상세 정보와 재고 현황을 확인할 수 있습니다.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-6">
              {/* 기본 정보 카드들 */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Card className="p-4 bg-gradient-to-br from-blue-50 to-blue-100 border-blue-200">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="text-sm text-blue-600 font-medium">현재 재고</div>
                      <div className="text-2xl font-bold text-blue-800">
                        {selectedItem.currentStock}<span className="text-lg">{selectedItem.unit}</span>
                      </div>
                    </div>
                    <Package className="w-8 h-8 text-blue-400" />
                  </div>
                </Card>

                <Card className="p-4 bg-gradient-to-br from-green-50 to-green-100 border-green-200">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="text-sm text-green-600 font-medium">재고 가치</div>
                      <div className="text-2xl font-bold text-green-800">
                        ₩{(selectedItem.currentStock * selectedItem.unitPrice).toLocaleString()}
                      </div>
                    </div>
                    <DollarSign className="w-8 h-8 text-green-400" />
                  </div>
                </Card>

                <Card className="p-4 bg-gradient-to-br from-purple-50 to-purple-100 border-purple-200">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="text-sm text-purple-600 font-medium">재고 상태</div>
                      <div className="text-lg font-bold text-purple-800">
                        {getStatusBadge(selectedItem.status)}
                      </div>
                    </div>
                    <Activity className="w-8 h-8 text-purple-400" />
                  </div>
                </Card>
              </div>

              {/* 상세 정보 섹션 */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* 기본 정보 */}
                <Card className="p-6">
                  <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                    <Package className="w-5 h-5 text-kpi-green" />
                    기본 정보
                  </h3>
                  <div className="space-y-4">
                    <div className="flex justify-between">
                      <span className="text-gray-600">품목명</span>
                      <span className="font-medium">{selectedItem.name}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">카테고리</span>
                      <span className="font-medium">{selectedItem.category}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">단위</span>
                      <span className="font-medium">{selectedItem.unit}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">공급업체</span>
                      <span className="font-medium">{selectedItem.supplier}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">단가</span>
                      <span className="font-medium">₩{selectedItem.unitPrice.toLocaleString()}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600 flex items-center gap-1">
                        <Calendar className="w-4 h-4" />
                        최종 업데이트
                      </span>
                      <span className="font-medium">{selectedItem.lastUpdated}</span>
                    </div>
                  </div>
                </Card>

                {/* 재고 설정 */}
                <Card className="p-6">
                  <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                    <BarChart3 className="w-5 h-5 text-kpi-orange" />
                    재고 설정
                  </h3>
                  <div className="space-y-4">
                    <div className="flex justify-between items-center">
                      <span className="text-gray-600">현재 재고</span>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-xl">{selectedItem.currentStock}</span>
                        <span className="text-gray-500">{selectedItem.unit}</span>
                      </div>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-gray-600">최소 재고</span>
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-orange-600">{selectedItem.minStock}</span>
                        <span className="text-gray-500">{selectedItem.unit}</span>
                      </div>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-gray-600">최대 재고</span>
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-green-600">{selectedItem.maxStock}</span>
                        <span className="text-gray-500">{selectedItem.unit}</span>
                      </div>
                    </div>
                    
                    {/* 재고 상태 바 */}
                    <div className="mt-4">
                      <div className="text-sm text-gray-600 mb-2">재고 상태</div>
                      <div className="relative w-full bg-gray-200 rounded-full h-3">
                        <div 
                          className={`absolute left-0 top-0 h-full rounded-full transition-all ${
                            selectedItem.currentStock <= selectedItem.minStock * 0.5 ? 'bg-red-500' :
                            selectedItem.currentStock <= selectedItem.minStock ? 'bg-orange-500' :
                            'bg-green-500'
                          }`}
                          style={{ 
                            width: `${Math.min((selectedItem.currentStock / selectedItem.maxStock) * 100, 100)}%` 
                          }}
                        />
                        {/* 최소 재고 표시선 */}
                        <div 
                          className="absolute top-0 w-0.5 h-full bg-red-300"
                          style={{ 
                            left: `${(selectedItem.minStock / selectedItem.maxStock) * 100}%` 
                          }}
                        />
                      </div>
                      <div className="flex justify-between text-xs text-gray-500 mt-1">
                        <span>0</span>
                        <span>최소: {selectedItem.minStock}{selectedItem.unit}</span>
                        <span>최대: {selectedItem.maxStock}{selectedItem.unit}</span>
                      </div>
                    </div>
                  </div>
                </Card>
              </div>

              {/* 최근 입출고 내역 */}
              <Card className="p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                  <Activity className="w-5 h-5 text-kpi-purple" />
                  최근 입출고 내역 (7일)
                </h3>
                <div className="space-y-3">
                  {[
                    { date: '2024-01-15', type: '입고', quantity: 50, unit: selectedItem.unit, reason: '정기 발주', balance: selectedItem.currentStock },
                    { date: '2024-01-14', type: '출고', quantity: -15, unit: selectedItem.unit, reason: '생산 사용', balance: selectedItem.currentStock - 50 },
                    { date: '2024-01-13', type: '출고', quantity: -8, unit: selectedItem.unit, reason: '생산 사용', balance: selectedItem.currentStock - 35 },
                    { date: '2024-01-12', type: '입고', quantity: 25, unit: selectedItem.unit, reason: '긴급 발주', balance: selectedItem.currentStock - 27 },
                    { date: '2024-01-11', type: '출고', quantity: -12, unit: selectedItem.unit, reason: '생산 사용', balance: selectedItem.currentStock - 52 }
                  ].map((record, index) => (
                    <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center gap-3">
                        <div className={`p-2 rounded-full ${record.type === '입고' ? 'bg-green-100' : 'bg-red-100'}`}>
                          {record.type === '입고' ? 
                            <ArrowUp className="w-4 h-4 text-green-600" /> : 
                            <ArrowDown className="w-4 h-4 text-red-600" />
                          }
                        </div>
                        <div>
                          <div className="font-medium">{record.reason}</div>
                          <div className="text-sm text-gray-500">{record.date}</div>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className={`font-medium ${record.type === '입고' ? 'text-green-600' : 'text-red-600'}`}>
                          {record.type === '입고' ? '+' : ''}{record.quantity}{record.unit}
                        </div>
                        <div className="text-sm text-gray-500">잔량: {record.balance}{record.unit}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>

              {/* 액션 버튼들 */}
              <div className="flex gap-3 pt-4 border-t">
                <Button 
                  onClick={() => {
                    setIsDetailModalOpen(false);
                    handleRestock(selectedItem);
                  }}
                  className="flex-1 bg-kpi-green hover:bg-green-600 text-white"
                >
                  <ArrowUp className="w-4 h-4 mr-2" />
                  재고 입고
                </Button>
                <Button 
                  onClick={() => {
                    setIsDetailModalOpen(false);
                    handleAdjust(selectedItem);
                  }}
                  variant="outline"
                  className="flex-1"
                >
                  <Edit3 className="w-4 h-4 mr-2" />
                  재고 조정
                </Button>
                <Button 
                  onClick={() => {
                    const cartData = [{
                      ...selectedItem,
                      orderQuantity: Math.max(selectedItem.maxStock - selectedItem.currentStock, selectedItem.minStock),
                      totalPrice: (Math.max(selectedItem.maxStock - selectedItem.currentStock, selectedItem.minStock)) * selectedItem.unitPrice
                    }];
                    setCartItems(cartData);
                    setIsDetailModalOpen(false);
                    setIsCartModalOpen(true);
                  }}
                  variant="outline"
                  className="flex-1 border-kpi-red text-kpi-red hover:bg-red-50"
                >
                  <ShoppingCart className="w-4 h-4 mr-2" />
                  발주 등록
                </Button>
                <Button 
                  variant="outline" 
                  onClick={() => setIsDetailModalOpen(false)}
                  className="flex-1"
                >
                  닫기
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}