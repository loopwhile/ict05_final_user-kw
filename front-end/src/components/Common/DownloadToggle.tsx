import React, { useState } from 'react';
import { Button } from '../ui/button';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '../ui/dropdown-menu';
import { Download, FileSpreadsheet, FileText } from 'lucide-react';
import { toast } from 'sonner';

interface DownloadToggleProps {
  onDownload: (format: 'excel' | 'pdf') => void;
  filename?: string;
  disabled?: boolean;
}

export function DownloadToggle({ onDownload, filename = 'data', disabled = false }: DownloadToggleProps) {
  const [isDownloading, setIsDownloading] = useState(false);

  const handleDownload = async (format: 'excel' | 'pdf') => {
    if (disabled || isDownloading) return;
    
    setIsDownloading(true);
    
    // 로딩 토스트 생성 및 ID 저장
    const loadingToast = toast.loading(`${format === 'excel' ? '엑셀' : 'pdf'} 파일 생성중입니다...`);
    
    try {
      // 실제 다운로드 함수 호출
      await onDownload(format);
      
      // 로딩 토스트를 성공 메시지로 업데이트
      toast.success(`${format === 'excel' ? '엑셀 파일 다운로드가 완료되었습니다' : 'pdf 파일 생성이 완료되었습니다'}`, {
        id: loadingToast
      });
      
    } catch (error) {
      console.error('Download error:', error);
      // 로딩 토스트를 에러 메시지로 업데이트
      toast.error(`다운로드 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.`, {
        id: loadingToast
      });
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button 
          variant="outline" 
          size="sm" 
          disabled={disabled || isDownloading}
          className="gap-2"
        >
          <Download className="h-4 w-4" />
          {isDownloading ? '다운로드 중...' : '내려받기'}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-48">
        <DropdownMenuItem 
          onClick={() => handleDownload('excel')}
          className="gap-2 cursor-pointer"
        >
          <FileSpreadsheet className="h-4 w-4 text-kpi-green" />
          <span>엑셀 파일로 내려받기</span>
        </DropdownMenuItem>
        <DropdownMenuItem 
          onClick={() => handleDownload('pdf')}
          className="gap-2 cursor-pointer"
        >
          <FileText className="h-4 w-4 text-kpi-red" />
          <span>pdf 파일 내려받기</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}