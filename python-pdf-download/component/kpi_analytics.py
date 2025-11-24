# component/kpi_analytics.py
from io import BytesIO
from typing import Dict, Any, List

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle

from .pdf_generator import PdfGenerator

_GEN = PdfGenerator()


def _headers(view_by: str) -> List[str]:
    if view_by == "MONTH":
        first = "월"
    else:
        first = "날짜"
    return [first, "매출", "주문수", "UPT", "ADS", "AUR"]


def _col_widths() -> List[float]:
    return [
        26 * mm,  # 날짜/월
        30 * mm,  # 매출
        20 * mm,  # 주문수
        16 * mm,  # UPT
        22 * mm,  # ADS
        22 * mm,  # AUR
    ]


def generate_kpi_pdf(payload: Dict[str, Any]) -> bytes:
    styles = _GEN.styles

    criteria = payload.get("criteria") or {}
    rows: List[Dict[str, Any]] = payload.get("data") or []

    store_name = criteria.get("storeName", "")
    start = criteria.get("startDate", "")
    end = criteria.get("endDate", "")
    view_by = (criteria.get("viewBy") or "DAY").upper()
    gen_at = criteria.get("generatedAt", "")

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
    story.append(Paragraph("[KPI 분석 리포트] " + store_name, styles["TitleKR"]))
    story.append(Spacer(1, 3 * mm))

    info = f"""기간: {start} ~ {end} / ViewBy: {view_by}<br/>
점포: {store_name}<br/>
생성일시: {gen_at}"""
    story.append(Paragraph(info, styles["BodyKR"]))
    story.append(Spacer(1, 6 * mm))

    headers = _headers(view_by)
    table_data: List[List[str]] = [headers]

    for r in rows:
        date_label = r.get("date", "")
        sales = int(r.get("sales", 0))
        tx = int(r.get("transaction", 0))
        upt = float(r.get("upt", 0.0))
        ads = int(r.get("ads", 0))
        aur = int(r.get("aur", 0))

        table_data.append([
            str(date_label),
            f"{sales:,}",
            f"{tx:,}",
            f"{upt:.2f}",
            f"{ads:,}",
            f"{aur:,}",
        ])

    if len(table_data) == 1:
        table_data.append([""] * len(headers))

    table = Table(table_data, colWidths=_col_widths(), repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#F3F3F3")),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#CED4DA")),
        ("FONTNAME", (0, 0), (-1, 0), "KR-Bold"),
        ("FONTNAME", (0, 1), (-1, -1), "KR-Regular"),
        ("FONTSIZE", (0, 0), (-1, -1), 9),
        ("ALIGN", (0, 0), (-1, 0), "CENTER"),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ALIGN", (0, 1), (0, -1), "LEFT"),
        ("ALIGN", (1, 1), (5, -1), "RIGHT"),
    ]))

    story.append(table)
    doc.build(story)
    return buf.getvalue()
