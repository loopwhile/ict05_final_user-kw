FROM python:3.11-slim

WORKDIR /app

# 한글 폰트 설치 (reportlab에서 KR-Regular 쓰는 부분 때문에)
RUN apt-get update && \
    apt-get install -y --no-install-recommends fonts-nanum && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# uvicorn으로 FastAPI 실행
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001"]
