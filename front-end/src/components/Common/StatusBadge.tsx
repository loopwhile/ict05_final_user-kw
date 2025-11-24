import React from 'react';
import { Badge } from '../ui/badge';

interface StatusBadgeProps {
  status?: 'active' | 'preparing' | 'closed' | 'warning' | 'normal' | 'pending' | 'approved' | 'rejected' | 'completed' | string;
  text?: string;
  className?: string;
}

const statusConfig: Record<string, { className: string; label: string }> = {
  active: {
    className: 'bg-kpi-green text-white border-kpi-green',
    label: '운영중'
  },
  preparing: {
    className: 'bg-kpi-orange text-white border-kpi-orange',
    label: '개점 준비'
  },
  closed: {
    className: 'bg-gray-500 text-white border-gray-500',
    label: '폐점'
  },
  warning: {
    className: 'bg-kpi-red text-white border-kpi-red',
    label: '경고'
  },
  normal: {
    className: 'bg-blue-500 text-white border-blue-500',
    label: '일반'
  },
  pending: {
    className: 'bg-yellow-500 text-white border-yellow-500',
    label: '대기중'
  },
  approved: {
    className: 'bg-kpi-green text-white border-kpi-green',
    label: '승인'
  },
  rejected: {
    className: 'bg-kpi-red text-white border-kpi-red',
    label: '거부'
  },
  completed: {
    className: 'bg-blue-600 text-white border-blue-600',
    label: '완료'
  }
};

export function StatusBadge({ status = 'normal', text = '', className = '' }: StatusBadgeProps) {
  // 모든 매개변수가 안전한지 확인
  if (!status && !text) {
    console.warn('StatusBadge: Both status and text are missing');
    return (
      <Badge variant="outline" className="bg-gray-500 text-white border-gray-500 rounded-full px-3 py-1">
        알 수 없음
      </Badge>
    );
  }

  // status가 undefined이거나 유효하지 않은 경우 기본값 사용
  const safeStatus = (status && typeof status === 'string' && status in statusConfig) ? status : 'normal';
  const config = statusConfig[safeStatus];
  
  // config가 없는 경우 fallback
  if (!config || typeof config !== 'object' || !config.className) {
    console.warn('StatusBadge: Invalid config for status:', safeStatus);
    return (
      <Badge variant="outline" className="bg-gray-500 text-white border-gray-500 rounded-full px-3 py-1">
        {text || status || '알 수 없음'}
      </Badge>
    );
  }
  
  return (
    <Badge 
      variant="outline" 
      className={`${config.className} ${className || ''} rounded-full px-3 py-1`}
    >
      {text || config.label || status}
    </Badge>
  );
}