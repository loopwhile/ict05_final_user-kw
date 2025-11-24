export const nf = new Intl.NumberFormat('ko-KR');

export const fmtMoneyInt = (v?: number) => nf.format(Math.round(v ?? 0));      // ADS/AUR, 정수
export const fmtUPT = (v?: number) => (v ?? 0).toFixed(2);                      // UPT, 소수 둘째
export const fmtPercent1 = (v?: number) => `${(v ?? 0).toFixed(1)}%`;           // Comp, 소수 첫째

export const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';

export function displayInbound(label?: string | null) {
  return label && label.trim().length > 0 ? label : '입고 이력 없음';
}