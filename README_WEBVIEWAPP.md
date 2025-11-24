# 프로젝트 분석 및 배포 가이드 (웹뷰 앱 포함)

이 문서는 Spring Boot 백엔드와 React 프론트엔드(Capacitor 웹뷰 앱)로 구성된 프로젝트의 구조, 빌드 방식, 그리고 실제 배포 시 필요한 설정 변경 사항을 정리한 가이드입니다.

---

## 1. 프로젝트 구조 및 기술 스택 분석

### 1.1. 전체 아키텍처

- **백엔드:** Spring Boot 기반의 API 서버
- **프론트엔드:** React(Vite) 기반의 웹 애플리케이션
- **웹뷰 앱:** Capacitor를 사용하여 프론트엔드 웹을 Android 앱으로 패키징
- **배포 방식:** 프론트엔드를 빌드하여 생성된 정적 파일(HTML, JS, CSS)을 백엔드 Spring Boot의 `static` 리소스에 포함시켜, 최종적으로 하나의 `.jar` 파일로 패키징하여 배포합니다.

### 1.2. 백엔드 (Spring Boot)

- **기술 스택:** Java 17, Spring Boot 3.5.6, Gradle, JPA(QueryDSL), Spring Security
- **데이터베이스:** MariaDB
- **서버 설정 (`application.properties`):**
    - 포트: `8082`
    - 컨텍스트 경로: `/user` (모든 경로는 `http://서버주소:8082/user` 로 시작)
- **인증 방식:**
    - 이메일/비밀번호 기반의 로그인을 `LoginFilter`로 처리합니다.
    - 인증 성공 시, `JWTUtil`을 통해 Access Token과 Refresh Token을 발급합니다.
    - API 요청은 `JWTFilter`에서 Access Token을 검증하며, 만료 시 `/jwt/refresh`를 통해 재발급받는 흐름을 가집니다.
- **SPA 지원:** `WebController`가 프론트엔드 라우팅 경로(`login`, `dashboard` 등)에 대한 요청을 `index.html`로 포워딩하여 React Router가 동작할 수 있도록 지원합니다.

### 1.3. 프론트엔드 (React)

- **기술 스택:** React 18, TypeScript, Vite
- **UI 라이브러리:** shadcn/ui, Radix UI, Lucide Icons
- **API 통신:** `axios` 인터셉터를 사용하여 모든 요청에 Access Token을 자동으로 주입하고, 401 에러 발생 시 토큰을 자동 재발급합니다 (`src/lib/authApi.ts`).
- **라우팅:** `react-router-dom`을 사용하며, 빌드 시 `basename`이 `/user`로 설정되어 백엔드 컨텍스트 경로와 일치시킵니다.
- **빌드:** `npm run android-build` 명령어를 사용하면 `front-end/build` 디렉터리에 프로덕션용 정적 파일이 생성됩니다.

### 1.4. 웹뷰 앱 (Capacitor)

- **역할:** 빌드된 React 웹 앱을 감싸는 Android 껍데기 역할을 합니다.
- **개발 환경 설정 (`capacitor.config.ts`):**
    - `server.url`이 `http://10.0.2.2:8082/user`로 설정되어 있습니다. 이는 Android 에뮬레이터에서 개발용 백엔드 서버와 직접 통신하며 실시간 리로딩을 하기 위함입니다.
    - **실제 앱 배포 시에는 이 설정이 비활성화되어야 합니다.**

---

## 2. 빌드 및 실행 절차

### 2.1. 개발 환경 실행 순서

1.  **백엔드 실행:** IntelliJ 등에서 `Ict05FinalUserApplication`을 실행하거나, 터미널에서 `./gradlew bootRun` 명령어로 Spring Boot 서버를 시작합니다.
2.  **프론트엔드 실행:** `front-end` 디렉터리에서 `npm install` 후 `npm run dev` 명령어로 Vite 개발 서버를 시작합니다. (브라우저에서 `http://localhost:3000`으로 접속)

### 2.2. 프로덕션 `.jar` 패키징 순서

1.  **프론트엔드 빌드:** `front-end` 디렉터리에서 `npm run android-build`를 실행합니다.
2.  **빌드 결과물 복사:** `front-end/build` 디렉터리의 모든 내용을 백엔드의 `src/main/resources/static` 폴더로 복사합니다. (기존 파일은 삭제 후 복사)
3.  **백엔드 빌드:** 프로젝트 루트 디렉터리에서 `./gradlew build`를 실행합니다.
4.  **결과물 확인:** `build/libs` 폴더에 생성된 `ict05_final_user-0.0.1-SNAPSHOT.jar` 파일이 모든 것이 포함된 최종 배포 파일입니다. 이 파일을 `java -jar <파일명>.jar`으로 실행할 수 있습니다.

---

## 3. 실제 배포 시 변경 필요 설정

실제 운영 서버에 배포할 때는 보안 및 환경 구성을 위해 아래 파일들의 설정값을 반드시 변경해야 합니다.

### 3.1. 백엔드 데이터베이스 연결 정보

-   **파일:** `src/main/resources/application.properties`
-   **변경 대상:**
    -   `spring.datasource.url`: 운영 DB의 JDBC URL로 변경해야 합니다.
    -   `spring.datasource.username`: 운영 DB의 사용자 이름으로 변경해야 합니다.
    -   `spring.datasource.password`: 운영 DB의 비밀번호로 변경해야 합니다.

    ```properties
    # 예시
    spring.datasource.url=jdbc:p6spy:mariadb://<운영DB_IP>:<포트>/<DB명>
    spring.datasource.username=<운영DB_사용자>
    spring.datasource.password=<운영DB_비밀번호>
    ```

### 3.2. (매우 중요) JWT 비밀 키

-   **파일:** `src/main/java/com/boot/ict05_final_user/config/security/jwt/JWTUtil.java`
-   **문제점:** 현재 JWT 토큰을 서명하는 비밀 키가 소스 코드에 하드코딩되어 있습니다. **이 키가 유출되면 누구나 유효한 토큰을 만들어 시스템에 접근할 수 있습니다.**
-   **해결 방안:** 이 값을 소스 코드에서 제거하고, OS 환경 변수나 Spring Boot의 외부 설정 파일(`application.yml` 등)을 통해 주입받도록 변경해야 합니다.

    ```java
    // 변경 전
    // private static final String secretKeyString = "himynameiskimjihunmyyoutubechann";

    // 변경 후 (예시: @Value 어노테이션 사용)
    // @Value("${jwt.secret}")
    // private String secretKeyString;
    ```

### 3.3. 프론트엔드 API 서버 주소

-   **파일:** `front-end/.env.production` (없으면 생성)
-   **변경 대상:** `VITE_BACKEND_API_BASE_URL`
-   **설명:** 프론트엔드가 API를 호출할 때 사용하는 기본 URL입니다. 개발 시에는 `http://localhost:8082`를 사용하지만, 운영 환경에서는 실제 서버의 도메인 주소를 사용해야 합니다.

    ```
    # front-end/.env.production 파일 내용 예시
    VITE_BACKEND_API_BASE_URL=https://api.yourdomain.com
    ```

### 3.4. 백엔드 CORS 설정

-   **파일:** `src/main/java/com/boot/ict05_final_user/config/security/config/SecurityConfig.java`
-   **변경 대상:** `corsConfigurationSource()` 메소드 내부의 `setAllowedOrigins`
-   **설명:** 현재는 `localhost`에서의 요청만 허용하고 있습니다. 실제 서비스 도메인에서 오는 요청을 허용하도록 주소를 추가하거나 변경해야 합니다.

    ```java
    // 변경 전
    // cfg.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173", "http://localhost"));

    // 변경 후 (예시)
    cfg.setAllowedOrigins(List.of("https://your-app-domain.com"));
    ```

### 3.5. Capacitor 웹뷰 앱 설정 (앱 배포 시)

-   **파일:** `front-end/capacitor.config.ts`
-   **변경 대상:** `server` 객체
-   **설명:** 웹뷰 앱을 정식으로 빌드하여 스토어에 배포할 때는, 개발용 라이브 리로드 서버 설정을 제거해야 합니다. 이 설정을 제거하면 앱은 내장된 `build` 폴더의 `index.html`을 직접 로드하게 됩니다. 앱 내부의 웹은 3.3에서 설정한 API 서버 주소로 통신합니다.

    ```typescript
    // 변경 전
    // const config: CapacitorConfig = {
    //   ...
    //   server: {
    //     url: 'http://10.0.2.2:8082/user',
    //     cleartext: true,
    //   },
    // };

    // 변경 후 (server 객체 주석 처리 또는 삭제)
    const config: CapacitorConfig = {
      appId: 'com.example.webviewapp',
      appName: 'Toast Lab App',
      webDir: 'build',
      // server: { ... } // 이 부분을 제거하거나 주석 처리
    };
    ```

---

## 4. 다른 팀원과 웹뷰 앱 테스트 공유 방법

Android Studio가 설치되지 않은 팀원이 웹뷰 앱을 테스트하는 가장 간단한 방법은, 개발 환경이 구성된 담당자가 **앱 설치 파일(.apk)을 생성하여 공유**하는 것입니다.

### 4.1. (담당자) 테스트용 APK 파일 생성 및 공유

1.  **웹 소스 빌드 및 동기화**
    - 최신 웹 코드를 반영하기 위해 아래 명령어를 순서대로 실행합니다.
    ```bash
    # front-end 디렉터리에서 실행
    npm run android-build
    npx cap sync android
    ```

2.  **APK 파일 빌드**
    - `front-end/android` 디렉터리로 이동하여 Gradle 빌드를 실행합니다.
    ```bash
    cd front-end/android
    ./gradlew assembleDebug
    ```

3.  **APK 파일 공유**
    - 빌드가 성공하면 아래 경로에 `app-debug.apk` 파일이 생성됩니다.
    - **경로:** `front-end/android/app/build/outputs/apk/debug/app-debug.apk`
    - 이 파일을 메신저, 이메일 등으로 다른 팀원에게 전달합니다.

### 4.2. (테스트 팀원) 공유받은 APK 파일 설치

1.  **"출처를 알 수 없는 앱 설치" 허용**
    - Android 기기의 `설정` > `보안` 메뉴로 이동하여, "출처를 알 수 없는 앱 설치"를 허용해 주어야 합니다. (기기 및 OS 버전에 따라 메뉴 위치는 다를 수 있습니다.)

2.  **파일 설치**
    - 공유받은 `app-debug.apk` 파일을 스마트폰으로 옮긴 후, 파일 탐색기 앱에서 해당 파일을 찾아 실행하면 앱이 설치됩니다.
