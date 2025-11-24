# Android 앱 FCM 기능 구현 TODO 리스트

## 1. Firebase 프로젝트 설정
- [ ] Firebase 콘솔에서 `com.toastlab.pos` 앱 ID로 안드로이드 앱 추가
- [ ] `google-services.json` 파일 다운로드 후 `front-end/android/app/` 디렉토리에 추가

## 2. Capacitor Push Notifications 플러그인 설치
- [x] `npm install @capacitor/push-notifications` 명령어 실행
- [x] `npx cap sync` 명령어로 안드로이드 프로젝트에 변경사항 동기화

## 3. 안드로이드 네이티브 설정 확인
- [x] `front-end/android/build.gradle` 및 `front-end/android/app/build.gradle`에 Firebase 의존성 추가되었는지 확인 (`npx cap sync` 이후)
- [x] `AndroidManifest.xml`에 푸시 알림 관련 권한 및 서비스가 추가되었는지 확인 (수동 추가 완료)

## 4. 웹 앱(React) 코드 수정
- [x] 앱 시작 시 푸시 알림 권한 요청 로직 추가
- [x] FCM 디바이스 토큰을 가져와 백엔드 서버로 전송하는 함수 구현
- [x] 포그라운드에서 푸시 알림을 수신하고 화면에 표시하는 리스너 추가
- [x] 사용자가 알림을 탭했을 때 특정 동작을 처리하는 리스너 추가

## 5. 테스트
- [ ] Firebase 콘솔 또는 백엔드 API를 사용하여 테스트 푸시 메시지 발송
- [ ] 앱 상태(포그라운드, 백그라운드, 종료)에 따른 알림 수신 확인
