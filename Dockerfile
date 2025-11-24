# 1단계: 빌드용 이미지
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# 프로젝트 전체 복사
COPY . .

# gradlew 실행 권한 부여
RUN chmod +x gradlew

# 스프링 부트 jar 빌드 (테스트는 일단 제외)
RUN ./gradlew clean bootJar -x test

# 2단계: 실행용 이미지
FROM eclipse-temurin:17-jre

WORKDIR /app

# 빌드된 jar 복사 (이름 바뀌어도 * 로 처리)
COPY --from=build /app/build/libs/*.jar app.jar

# Firebase 서비스 계정 파일 복사
RUN mkdir -p fcm-secret
COPY fcm-secret/firebase-admin.json fcm-secret/firebase-admin.json

# 스프링 부트 포트 (user는 8082)
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
