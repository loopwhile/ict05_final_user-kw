import React, { useState, useEffect } from 'react';

interface ChartWrapperProps {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export function ChartWrapper({ children, fallback }: ChartWrapperProps) {
  const [isClient, setIsClient] = useState(false);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    setIsClient(true);
  }, []);

  if (!isClient) {
    return fallback || (
      <div className="flex items-center justify-center h-64 bg-gray-50 rounded-lg">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-kpi-red mx-auto mb-2"></div>
          <p className="text-sm text-gray-600">차트를 로딩 중입니다...</p>
        </div>
      </div>
    );
  }

  if (hasError) {
    return fallback || (
      <div className="flex items-center justify-center h-64 bg-gray-50 rounded-lg">
        <div className="text-center">
          <p className="text-sm text-gray-600">차트를 불러올 수 없습니다.</p>
          <button 
            onClick={() => setHasError(false)}
            className="mt-2 text-sm text-kpi-red hover:underline"
          >
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  try {
    return <>{children}</>;
  } catch (error) {
    setHasError(true);
    return fallback || (
      <div className="flex items-center justify-center h-64 bg-gray-50 rounded-lg">
        <p className="text-sm text-gray-600">차트 렌더링 오류</p>
      </div>
    );
  }
}