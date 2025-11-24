# component/pdf_generator.py
import os
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont


class PdfGenerator:
    """
    공통 PDF 리소스:
    - 한글 폰트 등록
    - 공용 Paragraph 스타일 제공
    """
    def __init__(self):
        self._register_fonts()
        self.styles = self._build_styles()

    def _register_fonts(self):
        font_path_reg = "/usr/share/fonts/truetype/nanum/NanumGothic.ttf"
        font_path_bold = "/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf"

        if os.name == "nt":  # Windows
            font_path_reg = "c:/Windows/Fonts/malgun.ttf"
            font_path_bold = "c:/Windows/Fonts/malgunbd.ttf"

        if os.path.exists(font_path_reg) and os.path.exists(font_path_bold):
            pdfmetrics.registerFont(TTFont("KR-Regular", font_path_reg))
            pdfmetrics.registerFont(TTFont("KR-Bold", font_path_bold))
            pdfmetrics.registerFontFamily("KR", normal="KR-Regular", bold="KR-Bold")

    def _build_styles(self):
        styles = getSampleStyleSheet()
        styles.add(ParagraphStyle(name="TitleKR",   fontName="KR-Bold",    fontSize=18, alignment=1))
        styles.add(ParagraphStyle(name="HeaderKR",  fontName="KR-Bold",    fontSize=10, alignment=1))
        styles.add(ParagraphStyle(name="BodyKR",    fontName="KR-Regular", fontSize=9))
        styles.add(ParagraphStyle(name="BodyRight", fontName="KR-Regular", fontSize=9, alignment=2))
        return styles
