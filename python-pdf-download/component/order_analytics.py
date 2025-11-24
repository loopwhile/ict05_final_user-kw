# component/order_analytics.py
from io import BytesIO
from typing import Dict, Any, List

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle

from .pdf_generator import PdfGenerator

_GEN = PdfGenerator()

ORDER_TYPE_LABEL = {
    "DELIVERY": "배달",
    "TAKEOUT": "포장",
    "VISIT": "매장",
}

PAYMENT_TYPE_LABEL = {
    "CARD": "카드",
    "CASH": "현금",
    "VOUCHER": "상품권",
    "EXTERNAL": "외부 결제",
}

def _headers(view_by: str) -> List[str]:
    """
    일별 / 월별에 따라 컬럼 헤더 정의 (프론트 테이블과 동일)
    """
    view_by = (view_by or "DAY").upper()
    if view_by == "MONTH":
        # 프론트 월별 테이블 구조:
        # 월 / 총매출 / 주문수 / 평균주문금액 / 배달매출 / 포장매출 / 매장매출
        return ["월", "총매출", "주문수", "평균주문금액", "배달매출", "포장매출", "매장매출"]

    # DAY: 주문일자 기준 1행 = 1주문
    # 프론트 테이블 구조:
    # 주문일자 / 주문ID / 주문유형 / 총금액 / 메뉴수 / 결제수단 / 채널메모
    return ["주문일자", "주문ID", "주문유형", "총금액", "메뉴수", "결제수단", "채널메모"]


def _col_widths(view_by: str) -> List[float]:
    view_by = (view_by or "DAY").upper()
    if view_by == "MONTH":
        return [
            25 * mm,  # 월
            25 * mm,  # 총매출
            18 * mm,  # 주문수
            30 * mm,  # 평균주문금액
            25 * mm,  # 배달매출
            25 * mm,  # 포장매출
            25 * mm,  # 매장매출
        ]
    # DAY
    return [
        25 * mm,  # 주문일자
        20 * mm,  # 주문ID
        18 * mm,  # 주문유형
        25 * mm,  # 총금액
        18 * mm,  # 메뉴수
        22 * mm,  # 결제수단
        40 * mm,  # 채널메모
    ]

def _fmt_money(v) -> str:
    try:
        return f"{int(v or 0):,}"
    except Exception:
        return "0"

def generate_orders_pdf(payload: Dict[str, Any]) -> bytes:
    styles = _GEN.styles

    criteria = payload.get("criteria") or {}
    rows: List[Dict[str, Any]] = payload.get("data") or []

    store_name = criteria.get("storeName", "")
    start = criteria.get("startDate", "")
    end = criteria.get("endDate", "")
    view_by = (criteria.get("viewBy") or "DAY").upper()
    gen_at = criteria.get("generatedAt", "") or ""

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
    story.append(Paragraph("주문 분석 리포트", styles["TitleKR"]))
    story.append(Spacer(1, 3 * mm))

    # ===== 기본 정보 =====
    view_label = "일별" if view_by == "DAY" else "월별"
    info = f"""
기간: {start} ~ {end} / 기준: {view_label}<br/>
점포: {store_name}<br/>
생성일시: {gen_at}
"""
    story.append(Paragraph(info, styles["BodyKR"]))
    story.append(Spacer(1, 6 * mm))

    # ===== 테이블 데이터 구성 =====
    headers = _headers(view_by)
    table_data: List[List[str]] = [headers]

    if view_by == "MONTH":
        # date(or yearMonth), totalSales, orderCount, avgOrderAmount, deliverySales, takeoutSales, visitSales
        for r in rows:
            ym = r.get("yearMonth") or r.get("date") or ""
            total_sales = int(r.get("totalSales", 0) or 0)
            order_cnt = int(r.get("orderCount", 0) or 0)
            avg_amount = int(r.get("avgOrderAmount", 0) or 0)
            delivery = int(r.get("deliverySales", 0) or 0)
            takeout = int(r.get("takeoutSales", 0) or 0)
            visit = int(r.get("visitSales", 0) or 0)

            table_data.append([
                ym,
                _fmt_money(total_sales),    # 총매출
                f"{order_cnt:,}",           # 주문수
                _fmt_money(avg_amount),     # 평균주문금액
                _fmt_money(delivery),       # 배달매출
                _fmt_money(takeout),        # 포장매출
                _fmt_money(visit),          # 매장매출
            ])
    else:
        # DAY: date(orderDate), orderId, orderType, totalPrice, menuCount, paymentType, channelMemo
        for r in rows:
            date = r.get("date") or r.get("orderDate") or ""
            order_id = r.get("orderId", "")
            order_type_code = (r.get("orderType") or "").upper()
            total_price = int(r.get("totalPrice", 0) or 0)
            menu_cnt = int(r.get("menuCount", 0) or 0)
            pay_type_code = (r.get("paymentType") or "").upper()
            channel_memo = r.get("channelMemo", "") or "-"

            order_type = ORDER_TYPE_LABEL.get(order_type_code, order_type_code or "-")
            payment_type = PAYMENT_TYPE_LABEL.get(pay_type_code, pay_type_code or "-")

            table_data.append([
                date,                       # 주문일자
                str(order_id),              # 주문ID
                order_type,                 # 주문유형(한글)
                _fmt_money(total_price),    # 총금액
                f"{menu_cnt:,}",            # 메뉴수
                payment_type,               # 결제수단(한글)
                channel_memo,               # 채널메모
            ])

    # 데이터가 하나도 없으면 빈 행 추가
    if len(table_data) == 1:
        table_data.append([""] * len(headers))

    table = Table(
        table_data,
        colWidths=_col_widths(view_by),
        repeatRows=1,
    )

    # 스타일
    style_cmds = [
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#F3F3F3")),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#CED4DA")),
        ("FONTNAME", (0, 0), (-1, 0), "KR-Bold"),
        ("FONTNAME", (0, 1), (-1, -1), "KR-Regular"),
        ("FONTSIZE", (0, 0), (-1, -1), 9),
        ("ALIGN", (0, 0), (-1, 0), "CENTER"),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ]

    if view_by == "MONTH":
        # 숫자열 전체 우측 정렬
        style_cmds.append(("ALIGN", (1, 1), (-1, -1), "RIGHT"))
    else:
        # DAY: 주문ID, 금액/메뉴수/결제수단만 우측 정렬
        style_cmds.append(("ALIGN", (1, 1), (1, -1), "RIGHT"))  # 주문ID
        style_cmds.append(("ALIGN", (3, 1), (5, -1), "RIGHT"))  # 총금액, 메뉴수, 결제수단

    table.setStyle(TableStyle(style_cmds))

    story.append(table)
    doc.build(story)
    return buf.getvalue()
