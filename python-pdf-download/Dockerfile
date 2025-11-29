FROM python:3.11-slim

WORKDIR /app

# 한글 폰트 설치
RUN apt-get update && \
    apt-get install -y --no-install-recommends fonts-nanum && \
    rm -rf /var/lib/apt/lists/*

# 보안을 위해 non-root 사용자 생성 및 전환
RUN useradd --create-home appuser
USER appuser
WORKDIR /home/appuser/app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# 포트 설정
EXPOSE 8001

# uvicorn으로 FastAPI 실행
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001"]