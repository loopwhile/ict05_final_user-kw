// 허용 문자열 집합
export const CHANGE_TYPES = ['increase','decrease','neutral'] as const;
export type ChangeType = typeof CHANGE_TYPES[number];

// 서버가 대문자로 줄 수도 있으면(백엔드 enum = INCREASE 등)
export type ChangeTypeAny = ChangeType | Uppercase<ChangeType>;

// 정상화 유틸
export function normalizeChangeType(v?: string): ChangeType {
  const s = (v ?? 'neutral').toLowerCase();
  return (CHANGE_TYPES as readonly string[]).includes(s) ? (s as ChangeType) : 'neutral';
}

// DTO도 한곳에서
export interface KpiCardDTO {
  key: 'sales_today' | 'orders_today' | 'visitors_today' | 'top_menu' | string;
  value: string;
  change?: string;
  changeType?: ChangeTypeAny;
}

export interface KpiCardsResponse {
  date: string;
  storeId?: number;
  cards: KpiCardDTO[];
}
