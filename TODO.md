# Deployment Debugging Summary

This document summarizes all configurations related to the deployment for analysis by a GPT model.

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
        # 최종 수정: curl에 X-Forwarded-Proto 헤더를 추가하여 HTTPS 요청인 것처럼 시뮬레이션
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

    # 2. 현재 활성 포트(Blue/Green) 확인
    local CURRENT_PORT=$(grep -Po "${SERVICE_NAME}\s*{\s*server\s*127.0.0.1:\K\d+" ${UPSTREAM_CONF_PATH} || echo "")
    log "현재 활성 포트: ${CURRENT_PORT}"

    local TARGET_PORT
    local OLD_PORT
    if [ "$CURRENT_PORT" == "$BLUE_PORT" ]; then
        TARGET_PORT=$GREEN_PORT
        OLD_PORT=$BLUE_PORT
        log "Blue -> Green 으로 배포합니다. (타겟 포트: ${TARGET_PORT})"
    else
        TARGET_PORT=$BLUE_PORT
        OLD_PORT=$GREEN_PORT
        log "Green -> Blue 로 배포합니다. (타겟 포트: ${TARGET_PORT})"
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

    # 5. Nginx 트래픽 전환
    log "Nginx Upstream 트래픽 전환 -> ${TARGET_PORT}"
    sudo sed -i "s/${SERVICE_NAME}\s*{\s*server\s*127.0.0.1:.*;/${SERVICE_NAME} { server 127.0.0.1:${TARGET_PORT};/g" ${UPSTREAM_CONF_PATH}

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
    deploy_service "admin_backend" 8081 9081 "/admin/health" "admin-backend" "ADMIN_BACKEND_PORT"

    log "admin-pdf 서비스 재시작..."
    ${DOCKER_COMPOSE_CMD} pull admin-pdf
    ${DOCKER_COMPOSE_CMD} up -d --no-deps admin-pdf

elif [ "$1" == "user" ]; then
    log ">>>>>> USER 서비스 배포를 시작합니다. <<<<<<"
    deploy_service "user_frontend" 3000 9080 "/" "user-frontend" "USER_FRONTEND_PORT"
    deploy_service "user_backend" 8082 9082 "/user/health" "user-backend" "USER_BACKEND_PORT"

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
      - MARIADB_USER=${DB_USER}
      - MARIADB_PASSWORD=${DB_PASSWORD}
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
    server 127.0.0.1:8081;
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

---

## 3. Admin Project Files (`ict05_final_admin-kw`)

### `Jenkinsfile`

```groovy
// Jenkinsfile for the Admin Project

pipeline {
    // Run on any available agent
    agent any

    tools {
        // Use the 'docker' tool configured in Jenkins Global Tool Configuration
        dockerTool 'docker'
    }

    // Environment variables used throughout the pipeline
    environment {
        // [수정된 부분] Credentials ID 문자열을 환경 변수에 직접 할당
        DOCKERHUB_CREDENTIALS_ID = 'dockerhub-credentials'
        DOCKERHUB_USERNAME       = 'loopwhile' // As per DevOps.md
        ADMIN_BACKEND_IMAGE      = "${DOCKERHUB_USERNAME}/ict05-final-admin-backend"
        ADMIN_PDF_IMAGE          = "${DOCKERHUB_USERNAME}/ict05-final-admin-pdf"
    }

    stages {
        // Stage 1: Checkout source code from Git
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                git branch: 'main', url: 'https://github.com/loopwhile/ict05_final_admin-kw.git'
            }
        }

        // Stage 2: Build and push the Spring Boot backend image
        stage('Build & Push Admin Backend') {
            steps {
                // Use withCredentials to access the secret file
                withCredentials([file(credentialsId: 'firebase-admin-key', variable: 'FIREBASE_ADMIN_KEY_FILE')]) {
                    script {
                        // Use a try-finally block to ensure the secret file is cleaned up
                        try {
                            echo "Preparing secret file for Docker build..."
                            // Create the directory and copy the secret file to the location expected by the Dockerfile
                            sh 'mkdir -p fcm-secret'
                            sh 'cp $FIREBASE_ADMIN_KEY_FILE fcm-secret/firebase-admin.json'

                            echo "Building Admin Backend Docker image..."
                            def customImage = docker.build(ADMIN_BACKEND_IMAGE, ".")
                            
                            echo "Pushing Admin Backend image to Docker Hub..."
                            // [수정된 부분] DOCKERHUB_CREDENTIALS_ID 변수 사용
                            docker.withRegistry('https://registry.hub.docker.com', DOCKERHUB_CREDENTIALS_ID) {
                                customImage.push("latest")
                            }
                        } finally {
                            // Clean up the secret file and directory from the workspace
                            echo "Cleaning up secret file..."
                            sh 'rm -rf fcm-secret'
                        }
                    }
                }
            }
        }

        // Stage 3: Build and push the Python PDF service image
        stage('Build & Push Admin PDF Service') {
            steps {
                script {
                    echo "Building Admin PDF Service Docker image..."
                    // PDF 서비스용 Dockerfile이 'python-pdf-download' 하위 폴더에 있다고 가정
                    def customImage = docker.build(ADMIN_PDF_IMAGE, "python-pdf-download")
                    
                    echo "Pushing Admin PDF Service image to Docker Hub..."
                    // [수정된 부분] DOCKERHUB_CREDENTIALS_ID 변수 사용
                    docker.withRegistry('https://registry.hub.docker.com', DOCKERHUB_CREDENTIALS_ID) {
                        customImage.push("latest")
                    }
                }
            }
        }

        // Stage 4: Trigger the deployment on the EC2 server
        stage('Deploy to EC2') {
            steps {
                echo "Executing deployment script on EC2 via SSH..."
                // 등록한 SSH 키 파일을 사용해 EC2에 접속하여 스크립트 실행
                withCredentials([file(credentialsId: 'ec2-ssh-key', variable: 'SSH_KEY_FILE')]) {
                    script {
                        // 1. 키 파일 권한 설정 (필수)
                        sh 'chmod 600 $SSH_KEY_FILE'
                        
                        // 2. SSH로 접속해서 스크립트 실행 (StrictHostKeyChecking=no 옵션으로 접속 확인 무시)
                        // 주의: ubuntu@172.31.37.197 부분은 사용자님의 EC2 사설 IP입니다.
                        sh "ssh -o StrictHostKeyChecking=no -i $SSH_KEY_FILE ubuntu@172.31.37.197 '/home/ubuntu/deploy/deploy.sh admin'"
                    }
                }
            }
        }
    }

    // Post-build actions
    post {
        always {
            echo 'Admin pipeline finished.'
        }
    }
}
```

### `Dockerfile`

```dockerfile
# 1단계: 빌드용 이미지
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# build.gradle, settings.gradle 등 빌드에 필요한 파일만 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# gradlew 실행 권한 부여
RUN chmod +x gradlew

# 의존성을 먼저 다운로드 (이 단계는 build.gradle이 변경될 때만 실행됨)
RUN ./gradlew dependencies

# 소스 코드 복사
COPY src ./src

# 스프링 부트 jar 빌드 (테스트 제외)
RUN ./gradlew clean bootJar -x test --no-daemon

# 2단계: 실행용 이미지
FROM eclipse-temurin:17-jre

WORKDIR /app

# 빌드된 jar 복사
COPY --from=build /app/build/libs/*.jar app.jar

# Firebase 서비스 계정 파일 복사
RUN mkdir -p fcm-secret
COPY fcm-secret/firebase-admin.json fcm-secret/firebase-admin.json

# 포트 설정
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### `src/main/resources/application.properties`

```properties
spring.application.name=ict05_final_admin
spring.config.import=optional:application-receive.properties

# db connection (MariaDB + P6Spy)
spring.datasource.driver-class-name=com.p6spy.engine.spy.P6SpyDriver
spring.datasource.url=jdbc:p6spy:mariadb://ict05final.wwwbiz.kr:3306/ict05final
spring.datasource.username=ict05final
spring.datasource.password=team*1472

# http port number
server.port=8081
server.servlet.context-path=/admin
server.forward-headers-strategy=FRAMEWORK

# Tomcat-specific settings to handle forwarded headers directly
server.tomcat.remoteip.protocol-header=x-forwarded-proto
server.tomcat.remoteip.scheme-header-value=https

# === HQ ? User API ?? ===
#hq.user-api-base-url=http://localhost:8082/user
#hq.cookie-domain=localhost      # ?? ??? ?? ????? ??
#hq.cookie-secure=false

# JPA 설정
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.database-platform=org.hibernate.dialect.MariaDBDialect

# --- Python PDF Service ---
pdf.python.base-url=http://localhost:8000

# ===== FCM =====
fcm.enabled=true
toastlab.user-api.base-url=http://localhost:8082/user
# 서비스 계정 JSON (외부 경로)
fcm.service-account=file:fcm-secret/firebase-admin.json
# 웹푸시 VAPID 공개키 (head의 meta[name="vapid-key"]로 주입)
firebase.web.vapid-key=BDtFDyXg24QDjAMXaWm2ZJ112EfdSYq4oTo6m46eKW8aFMdKti8oQ4pPYx8eVeMm8MRR8VCDppoVbq0duGgztDw
# (선택) 토픽 이름을 프로퍼티로 쓰고 싶다면 이렇게 (우리 코드에서 안쓰면 없어도 됨)
fcm.hq-topic-all=hq-all

# --- FCM WebPush 하드닝 ---
fcm.webpush.icon=/admin/images/fcm/toastlab.png
fcm.webpush.badge=/admin/images/fcm/badge-72.png
fcm.webpush.ttl-seconds=3600
fcm.webpush.urgency=high
fcm.webpush.default-link=/admin
# dev엔 켜두고, prod는 필요 시만 켜세요.
fcm.seed.templates=false
# HQ 토픽 화이트리스트 사용 (권장: dev/prod 모두 true)
fcm.topic.restrict=true

# === HQ 스캐너 ===
fcm.scanner.enabled=true
fcm.scanner.expire-soon-days-default=3
fcm.scanner.stock-low-max=50
fcm.scanner.expire-soon-max=50
# 기본 30분 마다
#fcm.scanner.cron=0 0/30 * * * *
# 테스트할 때 (아래 둘 중 하나로 교체)
# 1) 1분마다
fcm.scanner.cron=0 * * * * *
# 2) 30초마다
# fcm.scanner.cron=*/30 * * * * *

# Hibernate SQL 로그는 끄기 (중복 방지)
logging.level.org.hibernate.SQL=off
logging.level.org.hibernate.type.descriptor.sql=off

# P6Spy 로그 레벨 (INFO면 충분)
logging.level.com.p6spy=INFO

# 기본 활성 프로필 (이미 dev 쓰고 있으면 유지)
spring.profiles.active=dev
# 프로필 이미지 기본 저장 경로 (로컬)
file.upload-dir.profile=D:/ict05_uploads/profile

# Firebase Web App Configuration (for frontend JS)
firebase.api-key=AIzaSyA7m5jVdo-w7TBG6h6wW4h6mc5gbNjqYlU
firebase.auth-domain=ict05-final.firebaseapp.com
firebase.project-id=ict05-final
firebase.storage-bucket=ict05-final.firebasestorage.app
firebase.messaging-sender-id=382264607725
firebase.app-id=1:382264607725:web:da28516c4a49f92e045de4
firebase.measurement-id=G-YEHZ8996H8
```

### `src/main/java/com/boot/ict05_final_admin/config/security/SecurityConfig.java`

```java
package com.boot.ict05_final_admin.config.security;

import com.boot.ict05_final_admin.config.security.filter.SyncAuthFilter;
import com.boot.ict05_final_admin.domain.auth.service.MemberUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


// ADD >>
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.List;
// << ADD

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final MemberUserDetailsService memberUserDetailsService;
    private static final RequestMatcher ADMIN_API = new AntPathRequestMatcher("/admin/API/**");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(memberUserDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    // 1) API 전용 체인
    @Bean
    @Order(0)
    public SecurityFilterChain apiChain(HttpSecurity http,
                                        ObjectProvider<SyncAuthFilter> syncAuthFilterProvider) throws Exception {
        http
                // ★ 여기! context-path 포함 매처로 강제
                .securityMatcher(ADMIN_API)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().permitAll()   // API는 전부 통과
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable) // ★ savedRequest로 로그인 리다이렉트 방지
                .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> res.sendError(401)));
        SyncAuthFilter filter = syncAuthFilterProvider.getIfAvailable();
        if (filter != null) {
            http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }


    @Bean
    @Order(1) // 체인 하나만 사용
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(ADMIN_API) // API는 CSRF 미적용
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공개 경로
                        .requestMatchers(
                                "/admin/health",
                                "/login", "/login/**",
                                "/register",
                                "/css/**", "/js/**", "/images/**", "/assets/**",
                                "/api/**", "/API/**", // ★ API 전부 허용
                                "/uploads/**", "/uploads/profile/**"
                        ).permitAll()

                        // ★ 백업 안전장치: 혹시라도 웹 체인이 잡아도 /admin/API/**는 통과
                        .requestMatchers("/admin/API/**").permitAll()

                        // 역할별 보호 경로
                        .requestMatchers("/store/**").hasAnyRole("FRANCHISE","ADMIN")
                        .requestMatchers("/menu/**").hasAnyRole("OPS","ADMIN")
                        .requestMatchers("/receive/**").hasAnyRole("OPS","ADMIN")
                        .requestMatchers("/inventory/**").hasAnyRole("OPS","ADMIN")
                        .requestMatchers("/material/**").hasAnyRole("OPS","ADMIN")
                        .requestMatchers("/store/material/**").hasAnyRole("OPS","ADMIN")
                        .requestMatchers("/staff/**").hasAnyRole("HR","ADMIN")
                        .requestMatchers("/analytics/**").hasAnyRole("ANALYTICS","ADMIN")
                        .requestMatchers("/member/**").hasRole("ADMIN")

                        // 나머지
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .authenticationProvider(daoAuthProvider());

        return http.build();
    }


    // ADD >>
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 개발 도메인
        cfg.setAllowedOrigins(List.of(
                "https://toastlabadmin.duckdns.org",  // ← Admin 도메인 추가
                "http://toastlabadmin.duckdns.org",   // HTTP도 추가
                "https://toastlab.duckdns.org",       // User 도메인도 추가 (필요하면)
                "http://localhost:8082", // 가맹점 프런트/게이트웨이
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setExposedHeaders(List.of("Authorization", "Content-Type", "Location"));
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        src.registerCorsConfiguration("/admin/**", cfg);
        return src;
    }
    // << ADD

    @Value("${file.upload-dir.profile}")
    private String profileUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + profileUploadDir + "/";

        registry.addResourceHandler("/uploads/profile/**")
                .addResourceLocations(location);
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

}
```

---

## 4. User Project Files (`ict05_final_user-kw`)

### `Jenkinsfile`

```groovy
// Jenkinsfile for the User Project

pipeline {
    // Run on any available agent
    agent any

    tools {
        // Use the 'docker' tool configured in Jenkins Global Tool Configuration
        dockerTool 'docker'
    }

    // Environment variables used throughout the pipeline
    environment {
        DOCKERHUB_CREDENTIALS_ID = 'dockerhub-credentials' // Corrected
        DOCKERHUB_USERNAME       = 'loopwhile' // As per DevOps.md
        USER_BACKEND_IMAGE       = "${DOCKERHUB_USERNAME}/ict05-final-user-backend"
        USER_PDF_IMAGE           = "${DOCKERHUB_USERNAME}/ict05-final-user-pdf"
        USER_FRONTEND_IMAGE      = "${DOCKERHUB_USERNAME}/ict05-final-user-frontend"
    }

    stages {
        // Stage 1: Checkout source code from Git
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                git branch: 'main', url: 'https://github.com/loopwhile/ict05_final_user-kw.git'
            }
        }

        // Stage 2: Build and push the Spring Boot backend image
        stage('Build & Push User Backend') {
            steps {
                // Use withCredentials to access the secret file
                withCredentials([file(credentialsId: 'firebase-admin-key', variable: 'FIREBASE_ADMIN_KEY_FILE')]) {
                    script {
                        // Use a try-finally block to ensure the secret file is cleaned up
                        try {
                            echo "Preparing secret file for Docker build..."
                            // Create the directory and copy the secret file to the location expected by the Dockerfile
                            sh 'mkdir -p fcm-secret'
                            sh 'cp $FIREBASE_ADMIN_KEY_FILE fcm-secret/firebase-admin.json'

                            echo "Building User Backend Docker image..."
                            // Assumes the Dockerfile for the backend is in the project root
                            def customImage = docker.build(USER_BACKEND_IMAGE, ".")
                            
                            echo "Pushing User Backend image to Docker Hub..."
                            docker.withRegistry('https://registry.hub.docker.com', DOCKERHUB_CREDENTIALS_ID) { // Corrected
                                customImage.push("latest")
                            }
                        } finally {
                            // Clean up the secret file and directory from the workspace
                            echo "Cleaning up secret file..."
                            sh 'rm -rf fcm-secret'
                        }
                    }
                }
            }
        }

        // Stage 3: Build and push the Python PDF service image
        stage('Build & Push User PDF Service') {
            steps {
                script {
                    echo "Building User PDF Service Docker image..."
                    // Assumes the Dockerfile for the PDF service is in the 'python-pdf-download' subdirectory
                    def customImage = docker.build(USER_PDF_IMAGE, "python-pdf-download")
                    
                    echo "Pushing User PDF Service image to Docker Hub..."
                    docker.withRegistry('https://registry.hub.docker.com', DOCKERHUB_CREDENTIALS_ID) { // Corrected
                        customImage.push("latest")
                    }
                }
            }
        }
        
        // Stage 4: Build and push the React frontend image
        stage('Build & Push User Frontend') {
            steps {
                script {
                    echo "Building User Frontend Docker image..."
                    // Assumes the Dockerfile for the frontend is in the 'front-end' subdirectory
                    def customImage = docker.build(USER_FRONTEND_IMAGE, "front-end")
                    
                    echo "Pushing User Frontend image to Docker Hub..."
                    docker.withRegistry('https://registry.hub.docker.com', DOCKERHUB_CREDENTIALS_ID) { // Corrected
                        customImage.push("latest")
                    }
                }
            }
        }

        // Stage 5: Trigger the deployment on the EC2 server
        stage('Deploy to EC2') {
            steps {
                echo "Executing deployment script on EC2 via SSH..."
                // 등록한 SSH 키 파일을 사용해 EC2에 접속하여 스크립트 실행
                withCredentials([file(credentialsId: 'ec2-ssh-key', variable: 'SSH_KEY_FILE')]) {
                    script {
                        // 1. 키 파일 권한 설정 (필수)
                        sh 'chmod 600 $SSH_KEY_FILE'
                        
                        // 2. SSH로 접속해서 스크립트 실행 (StrictHostKeyChecking=no 옵션으로 접속 확인 무시)
                        sh "ssh -o StrictHostKeyChecking=no -i $SSH_KEY_FILE ubuntu@172.31.37.197 '/home/ubuntu/deploy/deploy.sh user'"
                    }
                }
            }
        }
    }

    // Post-build actions
    post {
        always {
            echo 'User pipeline finished.'
        }
    }
}
```

### `Dockerfile`

```dockerfile
# 1단계: 빌드용 이미지
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# build.gradle, settings.gradle 등 빌드에 필요한 파일만 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# gradlew 실행 권한 부여
RUN chmod +x gradlew

# 의존성을 먼저 다운로드 (이 단계는 build.gradle이 변경될 때만 실행됨)
RUN ./gradlew dependencies

# 소스 코드 복사
COPY src ./src

# 스프링 부트 jar 빌드 (테스트 제외)
RUN ./gradlew clean bootJar -x test --no-daemon

# 2단계: 실행용 이미지
FROM eclipse-temurin:17-jre

WORKDIR /app

# 빌드된 jar 복사
COPY --from=build /app/build/libs/*.jar app.jar

# Firebase 서비스 계정 파일 복사
RUN mkdir -p fcm-secret
COPY fcm-secret/firebase-admin.json fcm-secret/firebase-admin.json

# 포트 설정
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### `src/main/resources/application.properties`

```properties
spring.application.name=ict05_final_user

# spring.profiles.active=purchase
# --- ??? ???(?? ???) ---
app.profile-image-dir=./uploads/profile
app.profile-image-url-prefix=/uploads/profile

# db connection (MariaDB + P6Spy)
spring.datasource.driver-class-name=com.p6spy.engine.spy.P6SpyDriver
spring.datasource.url=jdbc:p6spy:mariadb://ict05final.wwwbiz.kr:3306/ict05final
spring.datasource.username=ict05final
spring.datasource.password=team*1472

# http port number
server.port=8082
# 0.0.0.0\uC73C\uB85C \uC8FC\uC18C \uC124\uC815\uD558\uC5EC \uC678\uBD80 \uC811\uC18D \uD5C8\uC6A9
server.address=0.0.0.0
server.servlet.context-path=/user
server.forward-headers-strategy=FRAMEWORK

# Tomcat-specific settings to handle forwarded headers directly
server.tomcat.remoteip.protocol-header=x-forwarded-proto
server.tomcat.remoteip.scheme-header-value=https

# --- Python PDF Service ---
pdf.python.base-url=http://localhost:8001

# JPA \uC124\uC815
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.database-platform=org.hibernate.dialect.MariaDBDialect

# Hibernate SQL \uB85C\uADF8\uB294 \uB044\uAE30 (\uC911\uBCF5 \uBC29\uC9C0)
logging.level.org.hibernate.SQL=off
logging.level.org.hibernate.type.descriptor.sql=off

# P6Spy \uB85C\uADF8 \uB808\uBCA8 (INFO\uBA74 \uCDA9\uBD84)
logging.level.com.p6spy=INFO

# ===== FCM =====
fcm.enabled=true
# \uC11C\uBE44\uC2A4 \uACC4\uC815 JSON (\uC678\uBD80 \uACBD\uB85C)
fcm.service-account=file:fcm-secret/firebase-admin.json
firebase.web.vapid-key=BDtFDyXg24QDjAMXaWm2ZJ112EfdSYq4oTo6m46eKW8aFMdKti8oQ4pPYx8eVeMm8MRR8VCDppoVbq0duGgztDw
# --- FCM WebPush \uD558\uB4DC\uB2DD ---
fcm.webpush.icon=/user/images/fcm/toastlab.png
fcm.webpush.badge=/user/images/fcm/badge-72.png
fcm.webpush.default-link=/user
# \uC2A4\uCE90\uB108
# \uC7AC\uACE0\uBD80\uC871: 20\uBD84\uB9C8\uB2E4
fcm.scanner.low-cron=0 0/20 * * * *
# \uC720\uD1B5\uC784\uBC15: \uB9E4\uC77C 09:10
fcm.scanner.expire-cron=0 10 9 * * *

# \uC784\uACC4\uCE58/\uC77C\uC218/\uC0C1\uD55C (StoreFcmScannerProperties\uC5D0\uC11C \uC0AC\uC6A9)
fcm.scanner.low-threshold=1
fcm.scanner.expire-soon-days-default=3
fcm.scanner.stock-low-max=100
fcm.scanner.expire-soon-max=100

# \uD1A0\uD070 \uD074\uB9B0\uC5C5 \uC2A4\uCF00\uC904/\uC815\uCC45
fcm.cleanup.cron=0 0 3 * * *
fcm.cleanup.days-inactive=90



# Profile Image
file.upload-dir.profile=D:/ict05_uploads/profile
```

### `src/main/java/com/boot/ict05_final_user/config/security/config/SecurityConfig.java`

```java
package com.boot.ict05_final_user.config.security.config;

import com.boot.ict05_final_user.config.security.jwt.service.JwtService;
import com.boot.ict05_final_user.domain.user.entity.UserRoleType;
import com.boot.ict05_final_user.config.security.filter.JWTFilter;
import com.boot.ict05_final_user.config.security.filter.LoginFilter;
import com.boot.ict05_final_user.config.security.handler.RefreshTokenLogoutHandler;
import com.boot.ict05_final_user.config.security.auth.EmailAuthenticationProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;

    @Qualifier("LoginSuccessHandler")
    private final AuthenticationSuccessHandler loginSuccessHandler;

    // AuthenticationManager (커스텀 LoginFilter 용)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 권한 계층 (ADMIN > USER)
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withRolePrefix("ROLE_")
                .role(UserRoleType.ADMIN.name()).implies(UserRoleType.USER.name())
                .build();
    }

    // CORS (credentials 허용)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 개발/운영 도메인 추가
        cfg.setAllowedOrigins(List.of(
                "capacitor://localhost",              // 안드로이드 앱 기본 주소
                "http://localhost",                   // 안드로이드 앱 대체 주소
                "https://toastlab.duckdns.org",       // ← 추가
                "http://toastlab.duckdns.org",        // ← 추가
                "https://toastlabadmin.duckdns.org",  // ← 추가
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:8082"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With","X-Refresh-Token"));
        cfg.setExposedHeaders(List.of("Authorization","Set-Cookie"));
        cfg.setAllowCredentials(true);
        // 쿠키 수신 시 브라우저가 확인 가능한 헤더
        cfg.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    // 보안 필터 체인
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, EmailAuthenticationProvider provider) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        // 인가 규칙
        http.authorizeHttpRequests(auth -> auth
                // CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers("/user/health").permitAll()
                .requestMatchers("/api/auth/**").permitAll()

                // 공개 엔드포인트
                .requestMatchers("/login").permitAll()
                .requestMatchers("/jwt/exchange", "/jwt/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/exist", "/me","/API/**", "/dashboard/**", "/join", "/member/exist-email", "/member","/API/menu/**", "/api/customer-orders/**", "/register").permitAll()

                .requestMatchers(HttpMethod.POST, "/fcm/notice/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/fcm/token", "/fcm/topic/**", "/fcm/send/**").authenticated()

                // 인증 필요
                .requestMatchers(HttpMethod.PATCH, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/**").hasRole(UserRoleType.USER.name())
                .requestMatchers(HttpMethod.DELETE, "/**").hasRole(UserRoleType.USER.name())

                .anyRequest().authenticated()
        );

        // 예외 처리
        http.exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
        );

        // 무상태 세션
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // JWT 필터: UsernamePasswordAuthenticationFilter 보다 앞에서 토큰 검증
        http.addFilterBefore(new JWTFilter(), UsernamePasswordAuthenticationFilter.class);

        // 커스텀 로그인 필터: /login 엔드포인트에서 인증 처리 + 성공시 핸들러
        http.addFilterAt(
                new LoginFilter(authenticationManager(authenticationConfiguration), loginSuccessHandler),
                UsernamePasswordAuthenticationFilter.class
        );

        // 로그아웃 (Refresh Token 정리)
        http.logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler(new RefreshTokenLogoutHandler(jwtService))
        );

        // 필요 시, H2 콘솔/iframe 등 사용할 때만
        // http.headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        http.authenticationProvider(provider);

        return http.build();
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}
```
