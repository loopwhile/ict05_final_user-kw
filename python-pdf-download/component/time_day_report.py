from io import BytesIO
from typing import Dict, Any, List

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle

from .pdf_generator import PdfGenerator

_GEN = PdfGenerator()


def _headers(view_by: str) -> List[str]:
    """
    일별 / 월별에 따라 첫 컬럼 라벨만 다르게.
    """
    if view_by == "MONTH":
        return ["날짜(월)", "요일", "시간대", "주문수", "매출액", "매장/포장/배달"]
    return ["날짜(일)", "요일", "시간대", "주문수", "매출액", "매장/포장/배달"]


def _col_widths() -> List[float]:
    """
    컬럼 폭 고정 (A4 가로 기준)
    """
    return [
        26 * mm,  # 날짜/월
        12 * mm,  # 요일
        18 * mm,  # 시간대
        16 * mm,  # 주문수
        20 * mm,  # 매출액
        62 * mm,  # 매장/포장/배달
    ]


def generate_time_day_pdf(payload: Dict[str, Any]) -> bytes:
    styles = _GEN.styles

    summary = payload.get("summary") or {}
    # hourly = payload.get("hourlyPoints", [])  # 지금은 사용 안 함
    view_by = (payload.get("viewBy") or "DAY").upper()

    daily_rows: List[Dict[str, Any]] = payload.get("dailyRows") or []
    monthly_rows: List[Dict[str, Any]] = payload.get("monthlyRows") or []

    # 🔹 어떤 모드인지에 따라 사용할 rows 선택
    if view_by == "MONTH":
        rows = monthly_rows
    else:
        rows = daily_rows

    store_name = payload.get("storeName", "")
    period = payload.get("periodLabel", "")
    gen_at = payload.get("generatedAt", "")

    buf = BytesIO()
    doc = SimpleDocTemplate(
        buf,
        pagesize=landscape(A4),
        leftMargin=10 * mm,
        rightMargin=10 * mm,
        topMargin=15 * mm,
        bottomMargin=15 * mm,
    )

    story = []

    # ===== 제목 =====
    story.append(Paragraph("시간/요일 분석 리포트", styles["TitleKR"]))
    story.append(Spacer(1, 3 * mm))

    # ===== 기본 정보 =====
    info = f"""점포: {store_name}<br/>
기간: {period}<br/>
생성일시: {gen_at}"""
    story.append(Paragraph(info, styles["BodyKR"]))
    story.append(Spacer(1, 6 * mm))

    # ===== 요약 =====
    story.append(Paragraph("[요약]", styles["HeaderKR"]))
    story.append(Spacer(1, 2 * mm))

    summary_text = f"""
피크 시간대: {summary.get("peakHour","-")}시 / 매출 {summary.get("peakHourSales",0):,}원<br/>
비수 시간대: {summary.get("offpeakHour","-")}시 / 매출 {summary.get("offpeakHourSales",0):,}원<br/>
최고 매출 요일: {summary.get("topWeekday","-")}요일 / 매출 {summary.get("topWeekdaySales",0):,}원<br/>
주중 매출: {summary.get("weekdaySales",0):,}원 / 주말 매출: {summary.get("weekendSales",0):,}원
"""
    story.append(Paragraph(summary_text, styles["BodyKR"]))
    story.append(Spacer(1, 8 * mm))

    # ===== 테이블 =====
    headers = _headers(view_by)
    table_data: List[List[str]] = [headers]

    weekday_map = ["", "월", "화", "수", "목", "금", "토", "일"]

    for r in rows:
        w = (r.get("weekday") or 0)
        weekday_label = weekday_map[w] if 0 <= w < len(weekday_map) else "-"

        if view_by == "MONTH":
            date_label = r.get("yearMonth", "")
        else:
            date_label = r.get("orderDate", "")

        hour_val = int(r.get("hour", 0))
        hour_label = f"{hour_val:02d}시"

        type_str = (
            f"VISIT {r.get('visitCount',0)}, "
            f"TAKEOUT {r.get('takeoutCount',0)}, "
            f"DELIVERY {r.get('deliveryCount',0)}"
        )

        table_data.append(
            [
                date_label,
                weekday_label,
                hour_label,
                f"{int(r.get('orderCount', 0)):,}",
                f"{int(r.get('sales', 0)):,}",
                type_str,
            ]
        )

    # 데이터가 전혀 없으면 한 줄 짜리 빈 행 추가
    if len(table_data) == 1:
        table_data.append([""] * len(headers))

    table = Table(table_data, colWidths=_col_widths(), repeatRows=1)

    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#F3F3F3")),
                ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#CED4DA")),
                ("FONTNAME", (0, 0), (-1, 0), "KR-Bold"),
                ("FONTNAME", (0, 1), (-1, -1), "KR-Regular"),
                ("FONTSIZE", (0, 0), (-1, -1), 9),
                ("ALIGN", (0, 0), (-1, 0), "CENTER"),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),

                ("ALIGN", (0, 1), (0, -1), "LEFT"),   # 날짜/월
                ("ALIGN", (1, 1), (2, -1), "LEFT"),   # 요일, 시간
                ("ALIGN", (3, 1), (4, -1), "RIGHT"),  # 주문수, 매출액
                ("ALIGN", (5, 1), (5, -1), "LEFT"),   # 주문유형
            ]
        )
    )

    story.append(table)
    doc.build(story)
    return buf.getvalue()
