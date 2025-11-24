import React, { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { Bell, AlertTriangle, Clock3, ShieldCheck, ShieldAlert, RefreshCw, Smartphone, BellRing, BellOff } from 'lucide-react';
import { Button } from '../ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { toast } from 'sonner';
import { KPICard } from '../Common/KPICard';
import { Switch } from '../ui/switch';
import { Label } from '../ui/label';
import { Input } from '../ui/input';
import { Capacitor } from '@capacitor/core';
import { initializePushNotifications, cleanupPushNotifications } from '../../lib/fcm';
import { PushNotifications } from '@capacitor/push-notifications';
import api from '../../lib/authApi';

type Pref = {
  catNotice: boolean;
  catStockLow: boolean;
  catExpireSoon: boolean;
  thresholdDays?: number;   // ← 서버가 내려주므로 반영
  storeId?: number;
};

export default function NotificationSettings() {
  const [pref, setPref] = useState<Pref | null>(null);
  const [applySubs, setApplySubs] = useState(true);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [perm, setPerm] = useState<NotificationPermission>(Notification.permission);
  const [tokenBusy, setTokenBusy] = useState(false);
  const [currentToken, setCurrentToken] = useState<string | null>(localStorage.getItem('fcm_token'));

  // 초기 선호도 로드
  useEffect(() => {
    (async () => {
      try {
        // ✅ 문제 해결: authApi 대신 수동으로 axios 호출
        const token = localStorage.getItem('accessToken');
        if (!token) throw new Error('로그인이 필요합니다.');

        const { data } = await axios.get('/fcm/pref/me', {
          baseURL: import.meta.env.VITE_BACKEND_API_BASE_URL,
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        
        setPref({
          catNotice: !!data.catNotice,
          catStockLow: !!data.catStockLow,
          catExpireSoon: !!data.catExpireSoon,
          thresholdDays: data.thresholdDays ?? 3,
          storeId: data.storeId ?? undefined,
        });
      } catch (e: any) {
        console.error('[FCM] pref load error', e);
        toast.error('알림 설정을 불러오지 못했습니다.');
        setPref({ catNotice: true, catStockLow: true, catExpireSoon: true, thresholdDays: 3 });
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  // 권한 요청
  const requestPerm = async () => {
    try {
      let newPerm: NotificationPermission;
      if (Capacitor.getPlatform() !== 'web') {
        const permStatus = await PushNotifications.requestPermissions();
        newPerm = permStatus.receive === 'granted' ? 'granted' : permStatus.receive === 'denied' ? 'denied' : 'default';
      } else {
        newPerm = await Notification.requestPermission();
      }
      
      setPerm(newPerm);
      if (newPerm === 'granted') toast.success('알림 권한이 허용되었습니다.');
      else if (newPerm === 'denied') toast.warning('알림 권한이 차단되었습니다. 브라우저/앱 설정에서 변경할 수 있습니다.');

    } catch {
      toast.error('알림 권한을 요청하는 중 오류가 발생했습니다.');
    }
  };

  // 토큰 등록(업서트 + 선호 토픽 동기화)
  const registerToken = async () => {
    setTokenBusy(true);
    try {
      // 1) 토큰 발급(플랫폼별 분기 처리 위임)
      const token = await initializePushNotifications();
      if (!token) {
        toast.warning('FCM 토큰을 가져오지 못했습니다. 브라우저/앱의 알림 권한을 확인하세요.');
        return;
      }

      // 2) 서버 업서트 (fcm.ts의 registerTokenWithServer와 유사하나, 여기서는 UI 옵션(applySubs)을 고려해야 하므로 직접 호출)
      await api.post('/fcm/token', {
        token,
        platform: Capacitor.getPlatform() !== 'web' ? Capacitor.getPlatform().toUpperCase() : 'WEB',
        deviceId: 'browser', // 네이티브의 경우 실제 ID를 가져와야 하지만, 여기서는 단순화
      });

      // 3) 선호도 즉시 반영
      if (applySubs && pref?.storeId) {
        await api.put('/fcm/pref/me', {
          ...pref,
          applySubscriptions: true,
        });
      }

      setCurrentToken(token);
      toast.success('디바이스가 알림 수신에 등록되었습니다.');
    } catch (e: any) {
      console.error('[FCM] token register failed', e);
      toast.error('디바이스 등록에 실패했습니다.');
    } finally {
      setTokenBusy(false);
    }
  };

  // 토큰 해제(서버 revoke + 클라이언트 삭제)
  const revokeToken = async () => {
    setTokenBusy(true);
    try {
      await cleanupPushNotifications();
      setCurrentToken(null);
      toast.success('디바이스 알림 등록이 해제되었습니다.');
    } catch (e: any) {
      console.error('[FCM] token revoke failed', e);
      toast.error('디바이스 해제에 실패했습니다.');
    } finally {
      setTokenBusy(false);
    }
  };

  // 설정 저장
  const save = async () => {
    if (!pref) return;
    if (saving) return;
    setSaving(true);
    try {
      await api.put('/fcm/pref/me', {
        catNotice: pref.catNotice,
        catStockLow: pref.catStockLow,
        catExpireSoon: pref.catExpireSoon,
        thresholdDays: pref.thresholdDays ?? 3,
        applySubscriptions: applySubs,
      });
      toast.success('저장되었습니다.');
    } catch (e: any) {
      console.error('[FCM] pref save error', e);
      toast.error('저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const storeIdText = useMemo(() => (pref?.storeId ? String(pref.storeId) : '-'), [pref?.storeId]);

  if (loading || !pref) {
    return (
      <div className="p-6 text-center">
        <p>Loading...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">알림 설정</h1>
          <p className="text-sm text-gray-600 mt-1">공지/재고부족/유통임박 알림 구독과 수신 상태를 관리합니다.</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={requestPerm}>
            {perm === 'granted' ? <ShieldCheck className="w-4 h-4 mr-1" /> : <ShieldAlert className="w-4 h-4 mr-1" />}
            권한 재요청
          </Button>
        </div>
      </div>

      {/* KPI 카드 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <KPICard
          title="공지 수신"
          value={pref.catNotice ? 'ON' : 'OFF'}
          icon={Bell}
          color="green"
          footerText={`Topic: store-${storeIdText}`}
        />
        <KPICard
          title="재고부족 수신"
          value={pref.catStockLow ? 'ON' : 'OFF'}
          icon={AlertTriangle}
          color="orange"
          footerText={`Topic: inv-low-${storeIdText}`}
        />
        <KPICard
          title="유통임박 수신"
          value={pref.catExpireSoon ? 'ON' : 'OFF'}
          icon={Clock3}
          color="red"
          footerText={`Topic: expire-soon-${storeIdText}`}
        />
        <KPICard
          title="디바이스 등록"
          value={currentToken ? '등록됨' : '미등록'}
          icon={Smartphone}
          color="purple"
          footerText={currentToken ? '수신 가능' : '수신 불가'}
        />
      </div>

      {/* 카테고리 구독 설정 */}
      <Card>
        <CardHeader>
          <CardTitle>카테고리별 구독 설정</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="divide-y divide-gray-200">
            <SettingRow
              title="공지 수신"
              description={`본사 공지 토픽을 구독합니다. (store-${storeIdText})`}
              checked={pref.catNotice}
              onCheckedChange={(v) => setPref({ ...pref, catNotice: v })}
            />
            <SettingRow
              title="재고부족 수신"
              description={`재고 임계치 미만 상황을 수신합니다. (inv-low-${storeIdText})`}
              checked={pref.catStockLow}
              onCheckedChange={(v) => setPref({ ...pref, catStockLow: v })}
            />
            <SettingRow
              title="유통임박 수신"
              description={`유통기한 임박 알림을 수신합니다. (expire-soon-${storeIdText})`}
              checked={pref.catExpireSoon}
              onCheckedChange={(v) => setPref({ ...pref, catExpireSoon: v })}
            />
          </div>
        </CardContent>
      </Card>

      {/* 임계 일수(옵션) */}
      <Card>
        <CardHeader>
          <CardTitle>유통임박 임계 일수</CardTitle>
        </CardHeader>
        <CardContent className="flex items-center gap-3">
          <Input
            type="number"
            min={1}
            max={30}
            className="w-28"
            value={pref.thresholdDays ?? 3}
            onChange={(e) => setPref({ ...pref, thresholdDays: Math.max(1, Math.min(30, Number(e.target.value) || 1)) })}
          />
          <span className="text-sm text-gray-600">일 (1~30)</span>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setPref({ ...pref, thresholdDays: 3 })}
            title="기본값으로"
          >
            <RefreshCw className="w-4 h-4 mr-1" /> 기본 3일
          </Button>
        </CardContent>
      </Card>

      {/* 디바이스(토큰) 제어 */}
      <Card>
        <CardHeader>
          <CardTitle>디바이스 등록 상태</CardTitle>
        </CardHeader>
        <CardContent className="flex items-center justify-between">
          <div className="text-sm text-gray-700">
            현재 상태: <strong>{currentToken ? '등록됨' : '미등록'}</strong>
          </div>
          <div className="flex gap-2">
            <Button disabled={tokenBusy} variant="outline" onClick={registerToken}>
              <BellRing className="w-4 h-4 mr-1" />
              디바이스 등록
            </Button>
            <Button disabled={tokenBusy || !currentToken} variant="destructive" onClick={revokeToken}>
              <BellOff className="w-4 h-4 mr-1" />
              등록 해제
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 저장 */}
      <div className="flex justify-end items-center gap-4 p-4 bg-gray-50 rounded-lg">
        <div className="flex items-center space-x-2">
          <Switch
            id="apply-subs-immediately"
            checked={applySubs}
            onCheckedChange={setApplySubs}
          />
          <Label htmlFor="apply-subs-immediately" className="text-sm text-gray-700">
            저장 시 “즉시 구독/해제 반영”
          </Label>
        </div>
        <div className="flex gap-2">
          <Button variant="ghost" onClick={() => window.history.back()} disabled={saving}>
            취소
          </Button>
          <Button onClick={save} disabled={saving}>
            {saving ? '저장 중…' : '설정 저장'}
          </Button>
        </div>
      </div>
    </div>
  );
}

/** 설정 항목 한 줄 */
function SettingRow({
  title,
  description,
  checked,
  onCheckedChange,
}: {
  title: string;
  description: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
}) {
  const id = `setting-${title.replace(/\s+/g, '-')}`;
  return (
    <div className="py-4 flex items-center justify-between">
      <div className="flex flex-col">
        <Label htmlFor={id} className="font-medium text-gray-900">
          {title}
        </Label>
        <p className="text-sm text-gray-500">{description}</p>
      </div>
      <Switch id={id} checked={checked} onCheckedChange={onCheckedChange} />
    </div>
  );
}
