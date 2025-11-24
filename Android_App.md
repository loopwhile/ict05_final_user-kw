# Android APK 빌드 및 설치 가이드

Capacitor 기반의 Android 앱을 빌드하고 핸드폰에 설치하는 절차를 안내합니다.

## 중요: 빌드 전 API 주소 변경

핸드폰에서 앱이 서버와 정상적으로 통신하려면, 개발용 API 주소(`10.0.2.2`)를 실제 배포된 서버의 공개 주소로 변경해야 합니다.

1.  `front-end/.env.android` 파일을 엽니다.
2.  `VITE_BACKEND_API_BASE_URL` 값을 실제 서버 주소로 수정합니다.

-   **변경 전:**
    ```
    VITE_BACKEND_API_BASE_URL=http://10.0.2.2:8082/user
    ```

-   **변경 후 (예시):**
    ```
    VITE_BACKEND_API_BASE_URL=https://your-deployed-server.com/user
    ```

## APK 빌드 절차

### 1. 프론트엔드 프로젝트 빌드 및 동기화

React 프로젝트를 빌드하고, 빌드된 파일을 Android 프로젝트에 동기화합니다.

```bash
# /front-end 디렉터리에서 실행
cd front-end

# React 프로젝트 빌드
npm run build

# Capacitor 동기화
npx capacitor sync android
```

### 2. APK 파일 생성

Android 프로젝트 디렉터리로 이동하여 테스트용 `debug` 버전 APK를 생성합니다.

```bash
# /front-end/android 디렉터리에서 실행
cd front-end/android

# Gradle wrapper 실행 권한 부여 (필요시)
chmod +x ./gradlew

# Debug용 APK 빌드
./gradlew assembleDebug
```

### 3. 생성된 APK 파일 확인 및 설치

빌드가 성공적으로 완료되면 아래 경로에서 `app-debug.apk` 파일을 찾을 수 있습니다.

-   **APK 파일 위치:** `front-end/android/app/build/outputs/apk/debug/app-debug.apk`

이 파일을 핸드폰으로 복사하여 설치합니다. 핸드폰 설정에서 "알 수 없는 출처의 앱 설치" 권한이 필요할 수 있습니다.

## 명령어 요약

```bash
# 1. 프로젝트 루트의 front-end 디렉터리로 이동
cd /mnt/data/Workspace_IntelliJ/ict05_final/ict05_final_user/front-end

# 2. 웹 빌드
npm run build

# 3. 변경사항 동기화
npx capacitor sync android

# 4. android 디렉터리로 이동
cd ./android

# 5. APK 빌드
./gradlew assembleDebug
```
