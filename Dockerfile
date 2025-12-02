# 1단계: 빌드용 이미지
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# build.gradle, settings.gradle 등 빌드에 필요한 파일만 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# gradlew 실행 권한 부여
RUN chmod +x gradlew

# 의존성을 먼저 다운로드 (이 단계는 build.gradle이 변경될 때만 실행됨)
RUN ./gradlew dependencies --no-daemon

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