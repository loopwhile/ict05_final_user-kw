# component/menu_analytics.py
from io import BytesIO
from typing import Dict, Any, List

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle

from .pdf_generator import PdfGenerator

_GEN = PdfGenerator()


def generate_menus_pdf(payload: Dict[str, Any]) -> bytes:
    styles = _GEN.styles

    criteria = payload.get("criteria") or {}
    rows: List[Dict[str, Any]] = payload.get("data") or []

    store_name = criteria.get("storeName", "")
    period = criteria.get("periodLabel", "")
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

    # ===== 제목 =====
    story.append(Paragraph("메뉴 분석 리포트", styles["TitleKR"]))
    story.append(Spacer(1, 3 * mm))

    # ===== 기본 정보 =====
    info = f"""점포: {store_name}<br/>
기간: {period}<br/>
보기: {('일별' if view_by == 'DAY' else '월별')}<br/>
생성일시: {gen_at}"""
    story.append(Paragraph(info, styles["BodyKR"]))
    story.append(Spacer(1, 6 * mm))

    # ===== 테이블 헤더 =====
    if view_by == "DAY":
        headers = ["날짜", "카테고리", "메뉴", "판매수량", "매출액", "주문수"]
    else:
        headers = ["월", "카테고리", "메뉴", "판매수량", "매출액", "주문수"]

    table_data: List[List[str]] = [headers]

    for r in rows:
        date_label = r.get("date", "")
        category = r.get("category", "")
        menu = r.get("menu", "")
        qty = int(r.get("quantity", 0))
        sales = int(r.get("sales", 0))
        order_cnt = int(r.get("orderCount", 0))

        table_data.append([
            date_label,
            category,
            menu,
            f"{qty:,}",
            f"{sales:,}",
            f"{order_cnt:,}",
        ])

    if len(table_data) == 1:
        table_data.append([""] * len(headers))

    table = Table(table_data, colWidths=[
        30 * mm,  # 날짜/월
        40 * mm,  # 카테고리
        60 * mm,  # 메뉴
        20 * mm,  # 판매수량
        25 * mm,  # 매출액
        20 * mm,  # 주문수
    ], repeatRows=1)

    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#F3F3F3")),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#CED4DA")),
        ("FONTNAME", (0, 0), (-1, 0), "KR-Bold"),
        ("FONTNAME", (0, 1), (-1, -1), "KR-Regular"),
        ("FONTSIZE", (0, 0), (-1, -1), 9),
        ("ALIGN", (0, 0), (-1, 0), "CENTER"),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ALIGN", (0, 1), (0, -1), "LEFT"),   # 날짜/월
        ("ALIGN", (1, 1), (2, -1), "LEFT"),   # 카테고리, 메뉴
        ("ALIGN", (3, 1), (5, -1), "RIGHT"),  # 수량/매출/주문
    ]))

    story.append(table)
    doc.build(story)
    return buf.getvalue()
