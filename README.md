<img width="2600" height="1300" alt="image" src="https://github.com/user-attachments/assets/952e3083-26e9-48a2-b94b-ae0f62a156a8" />

## 📜 서비스 소개

---

**하루제주**는 "나무를 보지 말고 숲을 보자"는 철학에서 출발한 **미션형 걷기 기반 지역관광 플랫폼**입니다.

**오버투어리즘과 국민 건강 문제**라는 두 가지 사회적 과제를 동시에 해결하기 위해 설계되었으며, 렌터카 중심의 획일적 관광 패턴에서 벗어나 걷기를 통해 관광객 동선을 분산시키고 지역 상권 활성화와 지속 가능한 관광 구조를 만들어냅니다.

## 🧩 주요 기능

---

### 걸음수 측정 & 포인트 시스템

10걸음당 1포인트, 일일 최대 2,000포인트 적립 및 누적 걸음수 기반 5단계 등급 시스템 (2만보 단위)

### 위치 기반 챌린지

개인화 추천 미션 및 사진/위치 인증으로 지역 탐방 유도

### UGC 커뮤니티

여행 후기 공유 → 인기글은 새로운 챌린지 장소로 자동 등록되는 자생적 콘텐츠 생태계

### 리워드 상점

포인트로 제주 굿즈 교환 (공항 수령)

## 🛠️ 기술 스택

---

**Backend**

- Java, Spring Boot, JPA, Spring Security(OAuth2/JWT), Thymeleaf, WebSocket
- PostgreSQL(AWS RDS MySQL → Render PostgreSQL 전환), Redis, FCM

**Frontend**

- TypeScript, React (Cursor)

**Infra**

- AWS EC2, S3, RDS, Route 53, Nginx, Docker, Render
- Slack, Jira, Git, Swagger, Github Actions

## 💻 개발 내용

---

### 인증 시스템

- JWT + OAuth 2.0 통합 인증 시스템 구현 (일반/카카오 로그인)
- 이메일 인증 기반 회원가입 프로세스 설계 및 임시 회원 자동 정리 스케줄러 구현
- 커스텀 어노테이션(`@ValidNickname`)을 통한 닉네임 유효성 검사 로직 분리

### 걸음수 & 등급 시스템

- 걸음수-포인트 변환 시스템 및 일일 교환 제한 로직 구현
- 누적 걸음수 기반 5단계 등급 시스템 및 등급 달성 시 자동 보상 지급
- 전날 걸음수 기반 시작 보너스 시스템으로 사용자 참여 유도

### 출석 체크 시스템

- 연속 출석 보상 및 7일 보너스 시스템 구현
- Redis 기반 캐싱으로 출석 리마인더 발송 최적화
- DB 제약 조건과 캐시 동기화로 중복 출석 방지

### 결제/상점 시스템

- 포인트 기반 상품 교환 및 등급별 구매 제한 기능 구현
- `@Version` 낙관적 락으로 동시성 제어 및 재고 정합성 보장
- Spring Cache와 Fetch Join을 활용한 조회 성능 최적화

### 알림 시스템

- 7가지 알림 타입 지원 및 사용자별 알림 설정 관리
- Reddit 알고리즘 기반 게시글 자동 승격 시스템 구현
- 비동기 FCM 처리 및 Redis 중복 방지로 알림 안정성 확보
- 스케줄링 기반 자동 알림 (출석 리마인더, 게시글 승격)

### 프로젝트 관리 (PM)

- JIRA 기반 이슈 트래킹 및 스프린트 관리, Slack 연동 실시간 커뮤니케이션
- Git 컨벤션, DDD 적용 등 팀 개발 표준 수립

### 프론트엔드

- Cursor–Figma MCP 연동으로 커뮤니티 UI 컴포넌트 구현
- 걸음수 API 및 상점 장바구니 API 연동
- 걸음수 → 한라봉 포인트 컨버터 API 연동
- 다양한 진입 경로에서도 동작하는 공통 BackHeader 네비게이션 구현
- ProductDetailPage에서 제주티콘 중복 구매 시 에러 토스트 처리

## 📺 서비스 화면

---

<img width="1300" height="1300" alt="image" src="https://github.com/user-attachments/assets/256de8d3-702f-490c-b4fe-15551cbf8d05" />

<img width="1300" height="1300" alt="image" src="https://github.com/user-attachments/assets/af252fb6-2a76-4430-882f-d0a7c5fdba77" />

<img width="1300" height="1300" alt="image" src="https://github.com/user-attachments/assets/a8ae1e08-1acf-44f4-aed0-4434ab40254c" />

<img width="1300" height="1300" alt="image" src="https://github.com/user-attachments/assets/e788997b-6aa6-4505-91e1-e37bd1959954" />

