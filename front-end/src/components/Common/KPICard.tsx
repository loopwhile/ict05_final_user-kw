import React from "react";
import { Card } from "../ui/card";
import { ChangeTypeAny, normalizeChangeType } from "./kpi"

type KPIColor = "red" | "orange" | "green" | "purple";

interface KPICardProps {
  id?: string;                      // ← key 대신 사용 (테스트/추적용)
  title: string;
  value: string | number | React.ReactNode;
  change?: string;
  changeType?: ChangeTypeAny;       // ← 양쪽 케이스 허용
  icon: React.ComponentType<{ className?: string }>;
  color: KPIColor;
  className?: string;
  trend?: number;
}

const colorClasses: Record<KPIColor, string> = {
  red: "bg-kpi-red",
  orange: "bg-kpi-orange",
  green: "bg-kpi-green",
  purple: "bg-kpi-purple",
};

export function KPICard({
  title,
  value,
  change,
  changeType = "neutral",
  icon: Icon,
  color,
  className = "",
  trend,
}: KPICardProps) {
  // 대소문자 섞여 와도 내부는 소문자로 통일
  const ct = normalizeChangeType(changeType);

  return (
    <Card className={`${colorClasses[color]} text-white p-6 rounded-xl shadow-lg border-0 ${className}`}>
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <p className="text-white/80 text-sm mb-2">{title}</p>
          <div className="text-2xl font-bold leading-tight">{value}</div>

          {change && (
            <div className="flex items-center gap-1">
              <span
                className={`text-sm ${
                  ct === "increase" ? "text-white"
                  : ct === "decrease" ? "text-white/70"
                  : "text-white/80"
                }`}
              >
                {ct === "increase" && "↗"}
                {ct === "decrease" && "↘"}
                {change}
              </span>
            </div>
          )}

          {typeof trend === "number" && (
            <div className="flex items-center gap-1 mt-1">
              <span className={`text-sm ${trend > 0 ? "text-white" : trend < 0 ? "text-white/70" : "text-white/80"}`}>
                {trend > 0 ? "↗" : trend < 0 ? "↘" : "•"}
                {trend > 0 ? `+${trend}` : trend}%
              </span>
            </div>
          )}
        </div>

        <div className="w-12 h-12 bg-white/20 rounded-lg flex items-center justify-center">
          <Icon className="w-6 h-6 text-white" />
        </div>
      </div>
    </Card>
  );
}
