# User Frontend Debugging Summary

This document summarizes all configurations related to the `ict05_final_user-kw` project's frontend deployment, specifically for diagnosing the persistent white screen and 502 errors.

## 1. Environment Details

- **AWS EC2 Instance Type**: `t3.micro`
- **EC2 Public IP**: `43.202.226.171`
- **EC2 Private IP**: `172.31.37.197`
- **DNS Addresses**:
  - Admin: `ict05adminkw.duckdns.org`
  - User: `ict05userkw.duckdns.org`
- **Docker Hub Username**: `loopwhile`
- **Docker Hub Repositories**:
  - `loopwhile/ict05-final-admin-backend`
  - `loopwhile/ict05-final-admin-pdf`
  - `loopwhile/ict05-final-user-backend`
  - `loopwhile/ict05-final-user-frontend`
  - `loopwhile/ict05-final-user-pdf`

---

## 2. Shared EC2 Server Files

### `/home/ubuntu/deploy/deploy.sh`

```bash
#!/bin/bash

# Blue/Green 무중단 배포 스크립트
# 사용법: ./deploy.sh [admin|user]

# 스크립트 실행 중 오류 발생 시 즉시 중단
set -e

# --- 기본 설정 ---
DEPLOY_PATH="/home/ubuntu/deploy"
UPSTREAM_CONF_PATH="/etc/nginx/service-upstream.conf"
DOCKER_COMPOSE_CMD="docker compose -f ${DEPLOY_PATH}/docker-compose.yml --env-file ${DEPLOY_PATH}/.env"

# --- 로그 함수 ---
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# --- 헬스 체크 함수 ---
# $1: 헬스 체크할 URL, $2: 서비스 이름
health_check() {
    URL=$1
    SERVICE_NAME=$2
    log ">> ${SERVICE_NAME} 헬스 체크 시작: ${URL}"
    for i in {1..15}; do
        STATUS_CODE=$(curl -H "X-Forwarded-Proto: https" -s -o /dev/null -w "%{http_code}" ${URL})

        if [ "$STATUS_CODE" -eq 200 ]; then
            log ">> ${SERVICE_NAME} 헬스 체크 통과 (상태 코드: ${STATUS_CODE})"
            return 0
        fi
        log ">> ${SERVICE_NAME} 헬스 체크 실패 (상태 코드: ${STATUS_CODE}). 5초 후 재시도... ($i/15)"
        sleep 5
    done
    log ">> [ERROR] ${SERVICE_NAME} 헬스 체크 최종 실패."
    return 1
}

# --- 메인 배포 함수 ---
# $1: Nginx upstream 이름, $2: Blue 포트, $3: Green 포트, $4: 헬스체크 경로, $5: Docker-compose 서비스 이름, $6: 포트 설정용 환경변수 이름
deploy_service() {
    local SERVICE_NAME=$1
    local BLUE_PORT=$2
    local GREEN_PORT=$3
    local HEALTH_CHECK_PATH=$4
    local DOCKER_SERVICE_NAME=$5
    local PORT_ENV_VAR=$6

    log "===== ${DOCKER_SERVICE_NAME} 서비스 배포 시작 ====="

    # 1. 최신 이미지 받기
    log "최신 이미지 다운로드: ${DOCKER_SERVICE_NAME}"
    ${DOCKER_COMPOSE_CMD} pull ${DOCKER_SERVICE_NAME}

    # 2. 현재 활성 포트(Blue/Green) 확인 (awk로 수정)
    log "현재 활성 포트 확인 중..."
    local CURRENT_PORT=$(awk -v svc="${SERVICE_NAME}" '
      $1=="upstream" && $2==svc {inblk=1; next}
      inblk && $1=="server" {
        match($0, /127\.0\.0\.1:([0-9]+)/, m);
        if (m[1]!="") { print m[1]; exit }
      }
    ' ${UPSTREAM_CONF_PATH} || echo "")
    log "현재 활성 포트: ${CURRENT_PORT}"

    local TARGET_PORT
    local OLD_PORT
    if [ -z "$CURRENT_PORT" ] || [ "$CURRENT_PORT" == "$GREEN_PORT" ]; then
        TARGET_PORT=$BLUE_PORT
        OLD_PORT=$GREEN_PORT
        log "Green -> Blue 로 배포합니다. (타겟 포트: ${TARGET_PORT})"
    else
        TARGET_PORT=$GREEN_PORT
        OLD_PORT=$BLUE_PORT
        log "Blue -> Green 으로 배포합니다. (타겟 포트: ${TARGET_PORT})"
    fi

    # 3. 새로운 버전(Green)의 컨테이너 실행
    log "새 버전 컨테이너 실행: ${DOCKER_SERVICE_NAME} (포트: ${TARGET_PORT})"
    export ${PORT_ENV_VAR}=${TARGET_PORT}
    ${DOCKER_COMPOSE_CMD} up -d --no-deps ${DOCKER_SERVICE_NAME}

    # 4. 헬스 체크
    sleep 10
    if ! health_check "http://127.0.0.1:${TARGET_PORT}${HEALTH_CHECK_PATH}" ${DOCKER_SERVICE_NAME}; then
        log "[배포 실패] ${DOCKER_SERVICE_NAME} 롤백을 시작합니다."
        ${DOCKER_COMPOSE_CMD} stop ${DOCKER_SERVICE_NAME}
        ${DOCKER_COMPOSE_CMD} rm -f ${DOCKER_SERVICE_NAME}
        log "롤백 완료. 기존 버전(포트: ${OLD_PORT})이 계속 서비스됩니다."
        exit 1
    fi

    # 5. Nginx 트래픽 전환 (awk로 수정)
    log "Nginx Upstream 트래픽 전환 -> ${TARGET_PORT}"
    sudo awk -v svc="${SERVICE_NAME}" -v port="${TARGET_PORT}" '
      $1=="upstream" && $2==svc {print; inblk=1; next}
      inblk && $1=="server" {print "    server 127.0.0.1:" port ";"; inblk=0; next}
      {print}
    ' ${UPSTREAM_CONF_PATH} | sudo tee ${UPSTREAM_CONF_PATH}.tmp >/dev/null && sudo mv ${UPSTREAM_CONF_PATH}.tmp ${UPSTREAM_CONF_PATH}

    log "Nginx 설정 리로드"
    sudo nginx -s reload

    # 6. 이전 버전(Blue) 컨테이너 중지 및 삭제
    log "이전 버전 컨테이너 중지 (포트: ${OLD_PORT})"
    OLD_CONTAINER_ID=$(docker ps -q --filter "publish=${OLD_PORT}")
    if [ -n "$OLD_CONTAINER_ID" ]; then
        docker stop ${OLD_CONTAINER_ID}
        docker rm ${OLD_CONTAINER_ID}
        log "이전 버전 컨테이너(${OLD_CONTAINER_ID}) 중지 및 삭제 완료."
    else
        log "이전 버전 컨테이너를 찾을 수 없습니다."
    fi

    log "===== ${DOCKER_SERVICE_NAME} 서비스 배포 성공! ====="
}

# --- 스크립트 시작점 ---
if [ "$1" == "admin" ]; then
    log ">>>>>> ADMIN 서비스 배포를 시작합니다. <<<<<<"
    deploy_service "admin_backend" 8081 9081 "/admin/actuator/health" "admin-backend" "ADMIN_BACKEND_PORT"

    log "admin-pdf 서비스 재시작..."
    ${DOCKER_COMPOSE_CMD} pull admin-pdf
    ${DOCKER_COMPOSE_CMD} up -d --no-deps admin-pdf

elif [ "$1" == "user" ]; then
    log ">>>>>> USER 서비스 배포를 시작합니다. <<<<<<"
    deploy_service "user_frontend" 3000 9080 "/index.html" "user-frontend" "USER_FRONTEND_PORT"
    deploy_service "user_backend" 8082 9082 "/user/actuator/health" "user-backend" "USER_BACKEND_PORT"

    log "user-pdf 서비스 재시작..."
    ${DOCKER_COMPOSE_CMD} pull user-pdf
    ${DOCKER_COMPOSE_CMD} up -d --no-deps user-pdf

else
    log "[ERROR] 잘못된 인자입니다. 사용법: ./deploy.sh [admin|user]"
    exit 1
fi

log "모든 배포 작업이 완료되었습니다."
```

### `/home/ubuntu/deploy/docker-compose.yml`

```yaml
version: '3.8'

services:
  # ADMIN SERVICES
  admin-backend:
    image: ${DOCKERHUB_USERNAME}/ict05-final-admin-backend:latest
    container_name: admin-backend
    ports:
      - "${ADMIN_BACKEND_PORT:-8081}:8081"
    environment:
      - DB_HOST=mariadb
      - DB_NAME=${DB_NAME}
      - DB_USER=${DB_USER}
      - DB_PASSWORD=${DB_PASSWORD}
    volumes:
      - ./fcm-secrets/firebase-admin.json:/app/fcm-secret/firebase-admin.json
    depends_on:
      - mariadb
    networks:
      - ict_network

  admin-pdf:
    image: ${DOCKERHUB_USERNAME}/ict05-final-admin-pdf:latest
    container_name: admin-pdf
    ports:
      - "8000:8000"
    networks:
      - ict_network

  # USER SERVICES
  user-backend:
    image: ${DOCKERHUB_USERNAME}/ict05-final-user-backend:latest
    container_name: user-backend
    ports:
      - "${USER_BACKEND_PORT:-8082}:8082"
    environment:
      - DB_HOST=mariadb
      - DB_NAME=${DB_NAME}
      - DB_USER=${DB_USER}
      - DB_PASSWORD=${DB_PASSWORD}
    volumes:
      - ./fcm-secrets/firebase-admin.json:/app/fcm-secret/firebase-admin.json
    depends_on:
      - mariadb
    networks:
      - ict_network

  user-pdf:
    image: ${DOCKERHUB_USERNAME}/ict05-final-user-pdf:latest
    container_name: user-pdf
    ports:
      - "8001:8001"
    networks:
      - ict_network

  user-frontend:
    image: ${DOCKERHUB_USERNAME}/ict05-final-user-frontend:latest
    container_name: user-frontend
    ports:
      - "${USER_FRONTEND_PORT:-3000}:80"
    networks:
      - ict_network

  # DATABASE
  mariadb:
    image: mariadb:10.5
    container_name: mariadb
    environment:
      - MARIADB_ROOT_PASSWORD=${MARIADB_ROOT_PASSWORD}
      - MARIADB_DATABASE=${DB_NAME}
      - DB_USER=${DB_USER}
      - DB_PASSWORD=${DB_PASSWORD}
    volumes:
      - db_data:/var/lib/mysql
      - ./init-db.sql.gz:/docker-entrypoint-initdb.d/init-db.sql.gz
    ports:
      - "3306:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    networks:
      - ict_network

volumes:
  db_data:

networks:
  ict_network:
```

### `/etc/nginx/sites-enabled/default`

```nginx
include /etc/nginx/service-upstream.conf;
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    root /var/www/html;
    index index.html index.htm index.nginx-debian.html;
    server_name _;
    location / {
        try_files $uri $uri/ =404;
    }
}

# Admin Server (ict05adminkw.duckdns.org) - HTTPS (443)
server {
    server_name ict05adminkw.duckdns.org;

    # 루트(/)로 접속 시 /admin/으로 리디렉션
    location = / {
        return 301 /admin/;
    }

    # /admin/ 경로의 모든 요청을 백엔드로 전달
    location /admin/ {
        proxy_pass http://admin_backend;

        # --- 헤더 설정 (정리된 버전) ---
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    listen [::]:443 ssl ipv6only=on;
    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/ict05adminkw.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ict05adminkw.duckdns.org/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}


# Admin Server (ict05adminkw.duckdns.org) - HTTP (80)
server {
    if ($host = ict05adminkw.duckdns.org) {
        return 301 https://$host$request_uri;
    }
    listen 80;
    listen [::]:80;
    server_name ict05adminkw.duckdns.org;
    return 404;
}

# User Server (ict05userkw.duckdns.org) - HTTPS (443)
server {
    server_name ict05userkw.duckdns.org;

    # 루트 경로는 프론트엔드로 전달
    location / {
        proxy_pass http://user_frontend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # /user/ 경로는 백엔드로 전달
    location /user/ {
        proxy_pass http://user_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    listen [::]:443 ssl;
    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/ict05userkw.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ict05userkw.duckdns.org/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}

# User Server (ict05userkw.duckdns.org) - HTTP (80)
server {
    if ($host = ict05userkw.duckdns.org) {
        return 301 https://$host$request_uri;
    }
    listen 80;
    listen [::]:80;
    server_name ict05userkw.duckdns.org;
    return 404;
}
```

### `/etc/nginx/service-upstream.conf`

```nginx
# /etc/nginx/conf.d/service-upstream.conf

# Admin Backend Service를 위한 Upstream
# 배포 스크립트가 blue(8081)와 green(9081) 포트를 전환할 예정
upstream admin_backend {
    server 127.0.0.1:9081;
}

# User Backend Service를 위한 Upstream
# 배포 스크립트가 blue(8082)와 green(9082) 포트를 전환할 예정
upstream user_backend {
    server 127.0.0.1:8082;
}

# User Frontend Service를 위한 Upstream
# 배포 스크립트가 blue(3000)와 green(9080) 포트를 전환할 예정
upstream user_frontend {
    server 127.0.0.1:3000;
}
```
