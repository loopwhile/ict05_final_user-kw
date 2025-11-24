# component/material_analytics.py
from io import BytesIO
from typing import Dict, Any, List

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak

from .pdf_generator import PdfGenerator

_GEN = PdfGenerator()


def _fmt_money(v) -> str:
    """금액 포맷 (원화)"""
    try:
        return f"{int(v or 0):,}"
    except Exception:
        return "0"


def _fmt_qty(v) -> str:
    """수량 포맷 (소수점 2자리)"""
    try:
        return f"{float(v or 0):,.2f}"
    except Exception:
        return "0.00"


def _fmt_percent(v) -> str:
    """퍼센트 포맷 (소수점 1자리)"""
    try:
        return f"{float(v or 0):.1f}%"
    except Exception:
        return "0.0%"


def _build_summary_section(summary: Dict[str, Any]) -> List[Any]:
    """상단 요약 카드 섹션 생성"""
    styles = _GEN.styles
    story = []

    story.append(Paragraph("📊 재료 분석 요약", styles["HeaderKR"]))
    story.append(Spacer(1, 3 * mm))

    # --- Card 1: Top 5 재료 (사용량 / 원가) ---
    top_usage = summary.get("topByUsage", [])
    top_cost = summary.get("topByCost", [])

    # 사용량 Top 5
    story.append(Paragraph("▶ 사용량 Top 5", styles["BodyKR"]))
    if top_usage:
        usage_data = [["순위", "재료명", "사용량", "단위", "원가"]]
        for idx, item in enumerate(top_usage, start=1):
            usage_data.append([
                str(idx),
                item.get("materialName", "-"),
                _fmt_qty(item.get("usedQuantity", 0)),
                item.get("unitName", "-"),
                _fmt_money(item.get("cost", 0)),
            ])
        
        usage_table = Table(usage_data, colWidths=[15*mm, 40*mm, 25*mm, 20*mm, 25*mm])
        usage_table.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E9ECEF")),
            ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#CED4DA")),
            ("FONTNAME", (0, 0), (-1, 0), "KR-Bold"),
            ("FONTNAME", (0, 1), (-1, -1), "KR-Regular"),
            ("FONTSIZE", (0, 0), (-1, -1), 8),
            ("ALIGN", (0, 0), (-1, 0), "CENTER"),
            ("ALIGN", (2, 1), (4, -1), "RIGHT"),
        ]))
        story.append(usage_table)
    else:
        story.append(Paragraph("데이터 없음", styles["BodyKR"]))
    
    story.append(Spacer(1, 5 * mm))

    # 원가 Top 5
    story.append(Paragraph("▶ 원가 Top 5", styles["BodyKR"]))
    if top_cost:
        cost_data = [["순위", "재료명", "사용량", "단위", "원가"]]
        for idx, item in enumerate(top_cost, start=1):
            cost_data.append([
                str(idx),
                item.get("materialName", "-"),
                _fmt_qty(item.get("usedQuantity", 0)),
                item.get("unitName", "-"),
                _fmt_money(item.get("cost", 0)),
            ])
        
        cost_table = Table(cost_data, colWidths=[15*mm, 40*mm, 25*mm, 20*mm, 25*mm])
        cost_table.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E9ECEF")),
            ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#CED4DA")),
            ("FONTNAME", (0, 0), (-1, 0), "KR-Bold"),
            ("FONTNAME", (0, 1), (-1, -1), "KR-Regular"),
            ("FONTSIZE", (0, 0), (-1, -1), 8),
            ("ALIGN", (0, 0), (-1, 0), "CENTER"),
            ("ALIGN", (2, 1), (4, -1), "RIGHT"),
        ]))
        story.append(cost_table)
    else:
        story.append(Paragraph("데이터 없음", styles["BodyKR"]))

    story.append(Spacer(1, 5 * mm))

    # --- Card 2: 원가율 비교 ---
    current_rate = summary.get("currentCostRate", 0.0)
    prev_rate = summary.get("prevCostRate", 0.0)
    diff = summary.get("costRateDiff", 0.0)
    
    story.append(Paragraph("▶ 재료 원가율", styles["BodyKR"]))
    cost_rate_info = f"""
이번달 원가율: {_fmt_percent(current_rate)}<br/>
전월 동기간 원가율: {_fmt_percent(prev_rate)}<br/>
증감: {diff:+.1f}%p
"""
    story.append(Paragraph(cost_rate_info, styles["BodyKR"]))
    story.append(Spacer(1, 5 * mm))

    # --- Card 3 & 4: 재고 위험 ---
    low_stock = summary.get("lowStockCount", 0)
    expire_soon = summary.get("expireSoonCount", 0)
    
    story.append(Paragraph("▶ 재고 위험 알림", styles["BodyKR"]))
    risk_info = f"""
재고 부족: {low_stock}개 재료<br/>
유통기한 임박: {expire_soon}개 재료
"""
    story.append(Paragraph(risk_info, styles["BodyKR"]))
    story.append(Spacer(1, 8 * mm))

    return story


def _build_daily_table(rows: List[Dict[str, Any]]) -> List[Any]:
    """일별 테이블 생성"""
    styles = _GEN.styles
    story = []

    story.append(Paragraph("📅 일별 재료 사용 내역", styles["HeaderKR"]))
    story.append(Spacer(1, 3 * mm))

    if not rows:
        story.append(Paragraph("데이터 없음", styles["BodyKR"]))
        return story

    # 헤더
    headers = ["사용일자", "재료명", "사용량", "단위", "원가", "매출비중", "최근입고일"]
    table_data = [headers]

    for r in rows:
        table_data.append([
            r.get("useDate", "-"),
            r.get("materialName", "-"),
            _fmt_qty(r.get("usedQuantity", 0)),
            r.get("unitName", "-"),
            _fmt_money(r.get("cost", 0)),
            _fmt_percent(r.get("salesShare", 0)),
            r.get("lastInboundDate") or "-",
        ])

    table = Table(
        table_data,
        colWidths=[25*mm, 35*mm, 22*mm, 18*mm, 22*mm, 22*mm, 25*mm],
        repeatRows=1,
    )

    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#F3F3F3")),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#CED4DA")),
        ("FONTNAME", (0, 0), (-1, 0), "KR-Bold"),
        ("FONTNAME", (0, 1), (-1, -1), "KR-Regular"),
        ("FONTSIZE", (0, 0), (-1, -1), 8),
        ("ALIGN", (0, 0), (-1, 0), "CENTER"),
        ("ALIGN", (2, 1), (5, -1), "RIGHT"),  # 수량/원가/비중 우측정렬
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ]))

    story.append(table)
    return story


def _build_monthly_table(rows: List[Dict[str, Any]]) -> List[Any]:
    """월별 테이블 생성"""
    styles = _GEN.styles
    story = []

    story.append(Paragraph("📅 월별 재료 사용 내역", styles["HeaderKR"]))
    story.append(Spacer(1, 3 * mm))

    if not rows:
        story.append(Paragraph("데이터 없음", styles["BodyKR"]))
        return story

    # 헤더
    headers = ["월", "재료명", "사용량", "원가", "원가율", "최근입고월"]
    table_data = [headers]

    for r in rows:
        table_data.append([
            r.get("yearMonth", "-"),
            r.get("materialName", "-"),
            _fmt_qty(r.get("usedQuantity", 0)),
            _fmt_money(r.get("cost", 0)),
            _fmt_percent(r.get("costRate", 0)),
            r.get("lastInboundMonth") or "-",
        ])

    table = Table(
        table_data,
        colWidths=[25*mm, 45*mm, 30*mm, 30*mm, 25*mm, 30*mm],
        repeatRows=1,
    )

    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#F3F3F3")),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#CED4DA")),
        ("FONTNAME", (0, 0), (-1, 0), "KR-Bold"),
        ("FONTNAME", (0, 1), (-1, -1), "KR-Regular"),
        ("FONTSIZE", (0, 0), (-1, -1), 8),
        ("ALIGN", (0, 0), (-1, 0), "CENTER"),
        ("ALIGN", (2, 1), (4, -1), "RIGHT"),  # 수량/원가/원가율 우측정렬
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ]))

    story.append(table)
    return story


def generate_material_pdf(payload: Dict[str, Any]) -> bytes:
    """
    재료 분석 PDF 생성 메인 함수.
    
    Args:
        payload: MaterialReportPayload 딕셔너리
        
    Returns:
        PDF 바이트 배열
    """
    styles = _GEN.styles

    store_name = payload.get("storeName", "")
    period = payload.get("periodLabel", "")
    view_by = (payload.get("viewBy") or "DAY").upper()
    gen_at = payload.get("generatedAt", "")

    summary = payload.get("summary") or {}
    daily_rows = payload.get("dailyRows") or []
    monthly_rows = payload.get("monthlyRows") or []

    buf = BytesIO()
    doc = SimpleDocTemplate(
        buf,
        pagesize=landscape(A4),
        leftMargin=10 * mm,
        rightMargin=10 * mm,
        topMargin=15 * mm,
        bottomMargin=15 * mm,
    )

    story: List[Any] = []

    # ===== 제목 =====
    story.append(Paragraph("재료 분석 리포트", styles["TitleKR"]))
    story.append(Spacer(1, 3 * mm))

    # ===== 기본 정보 =====
    view_label = "일별" if view_by == "DAY" else "월별"
    info = f"""
점포: {store_name}<br/>
기간: {period} / 기준: {view_label}<br/>
생성일시: {gen_at}
"""
    story.append(Paragraph(info, styles["BodyKR"]))
    story.append(Spacer(1, 5 * mm))

    # ===== 상단 요약 카드 =====
    story.extend(_build_summary_section(summary))

    # ===== 페이지 브레이크 =====
    story.append(PageBreak())

    # ===== 일별/월별 테이블 =====
    if view_by == "DAY":
        story.extend(_build_daily_table(daily_rows))
    else:
        story.extend(_build_monthly_table(monthly_rows))

    # ===== PDF 빌드 =====
    doc.build(story)
    return buf.getvalue()