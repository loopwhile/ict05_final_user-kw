# 1단계: 빌드용 이미지 (Node.js)
FROM node:20-alpine AS build

WORKDIR /app

# package.json, package-lock.json 복사
COPY package.json package-lock.json* ./

# 의존성 설치
RUN npm install

# 소스 코드 복사
COPY . .

# React 앱 빌드
RUN npm run build

# 2단계: 실행용 이미지 (Nginx)
FROM nginx:stable-alpine

# Nginx의 기본 설정을 삭제하고, 우리 앱을 위한 커스텀 설정을 복사
COPY --from=build /app/nginx.conf /etc/nginx/conf.d/default.conf

# 빌드된 정적 파일들을 Nginx의 웹 루트 디렉토리로 복사
COPY --from=build /app/dist /usr/share/nginx/html

# Nginx 포트
EXPOSE 80

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]
