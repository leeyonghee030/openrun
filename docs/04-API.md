# OpenRun API 명세 (초안)

> 상태: **초안**. 오늘은 엔드포인트 목록과 규칙까지만 정하고, 상세 요청/응답은 구현하며 채운다.
> 2026-10-30 수정: QR 폐지 → 가게 태블릿에서 전화번호로 등록, 손님은 자동 가입, 탈퇴 규칙 확정, ADMIN/STORE 분리 확정.
> 근거: `01-요구사항.md`의 요구사항 ID를 표에 함께 적는다.

## 1. 공통 규칙

| 항목 | 규칙 | 이유 |
|---|---|---|
| 경로 접두사 | JSON API는 `/api`로 시작. 화면(Mustache)은 접두사 없음 | 화면과 API의 인증 설정을 나누기 쉽다 |
| 역할별 구분 | 일반 사용자 `/api/...`, 가게측 `/api/owner/...`, 관리자 `/api/admin/...` | 권한 설정을 경로 접두사 하나로 걸 수 있다 |
| 이름 | 자원은 복수형 영어 (`stores`, `waitings`). `reservation` 용어는 쓰지 않는다 | 이 서비스는 예약이 아니라 줄서기 |
| 로그인 사용자 | **`userId`를 경로·본문에 받지 않는다.** JWT에서 꺼낸다 | 남의 ID를 넣는 공격(IDOR) 방지 |
| 대기 등록의 손님 | 가게 태블릿(STORE 로그인)이 호출하므로 손님을 본문의 `phone`으로 지정한다. 위 `userId` 규칙의 **유일한 예외** | 손님은 로그인하지 않은 채 등록한다. 현장 확인은 STORE 로그인이 대신한다 |
| 가게측의 가게 | `storeId`를 받지 않는다. 로그인 계정의 `stores.owner_user_id`로 찾는다 | 1계정 1가게라서 가능하고, U-5(자기 가게만)가 구조적으로 보장된다 |
| 조회 / 변경 | 조회는 GET, 생성·상태 변경은 POST | 상태 전이는 행을 지우지 않으므로 DELETE를 쓰지 않는다 |
| 상태 전이 | 동작마다 경로를 나눈다 (`/cancel`, `/enter`, `/no-show`) | 어떤 전이인지 경로에서 읽히고, 전이 규칙(CALLED일 때만 입장 등)을 메서드 단위로 검증하기 쉽다 |

## 2. 화면 (Mustache)

| 화면 | 경로 | 비고 |
|---|---|---|
| 가게측 가입 / 로그인 | GET `/join`(가게측만), GET `/login` | 손님은 가입 화면이 없다. 태블릿에서 처음 등록할 때 자동 가입된다 |
| 가게 목록 / 상세 | GET `/stores`, GET `/stores/{storeId}` | |
| 가게 태블릿 접수 화면 | GET `/owner/register` | STORE 로그인 필요. 손님이 전화번호와 인원을 입력한다 (현장 확인) |
| 내 대기 목록 / 상세 | GET `/my/waitings`, GET `/my/waitings/{waitingId}` | |
| 가게측 대기 관리 | GET `/owner/waitings` | |
| 관리자 가게 승인 | GET `/admin/stores` | |

## 3. API 목록

### 3-1. 회원 / 인증

| 기능 | 메서드 + 경로 | 권한 | ID |
|---|---|---|---|
| 가게측 회원가입 | POST `/api/auth/signup/store` | 누구나 | U-4 |
| 인증번호 요청 (손님) | POST `/api/auth/otp` | 누구나 | U-2 |
| 로그인 | POST `/api/auth/login` | 누구나 | U-2 |
| 회원 탈퇴 | POST `/api/users/me/withdraw` | 로그인 | U-6 |

- 손님(USER)은 가입 API가 없다. 태블릿에서 처음 대기를 등록하면 `phone`으로 자동 가입된다.
- 가게측 가입은 아이디, 비밀번호와 가게 정보(이름, 주소 등)를 받는다. 가입하면 `stores.status = PENDING`.
- 로그인은 한 경로에서 본문 형태로 구분한다. 손님은 `phone` + 인증번호, STORE·ADMIN은 `loginId` + 비밀번호. 인증번호는 데모에서 가짜 인증(로그 출력 또는 고정값)이다.
- 탈퇴(U-6, Core 맨 뒤 순서로 구현): 대기가 없으면 바로 탈퇴, **WAITING만** 있으면 취소 안내 후 `confirm=true`로 요청하면 취소하고 탈퇴, **CALLED가 있으면 409**로 거부한다. `confirm` 없이 WAITING이 있으면 대기 건수를 응답하고 탈퇴는 하지 않는다.
- 로그아웃 / 토큰 무효화는 3주차 이후 결정.

### 3-2. 가게 조회 (사용자)

| 기능 | 메서드 + 경로 | 권한 | ID |
|---|---|---|---|
| 가게 목록 | GET `/api/stores` | 누구나 | P-1 |
| 가게 상세 (현재 대기 팀 수 포함) | GET `/api/stores/{storeId}` | 누구나 | P-1, P-3 |

- 목록에는 `APPROVED` 가게만 나온다.

### 3-3. 대기 (사용자)

| 기능 | 메서드 + 경로 | 권한 | ID |
|---|---|---|---|
| 내 대기 목록 | GET `/api/waitings` | 로그인 사용자 본인 | W-4, R-9 |
| 내 대기 상세 | GET `/api/waitings/{waitingId}` | 본인 대기만 | W-4 |
| 대기 취소 | POST `/api/waitings/{waitingId}/cancel` | 본인 대기만 | W-5 |

- 목록은 내 활성 대기(WAITING / CALLED)만, 가게 이름·내 번호·상태 정도만 담는다.
- 상세는 대기번호, 내 앞 팀 수(계산), 상태를 담는다. `waitingId`가 내 것인지 서비스에서 검증한다.
- 남의 `waitingId`로 요청하면 **404**로 응답한다 (존재 여부를 알려주지 않기 위해).

### 3-4. 가게측 (STORE)

| 기능 | 메서드 + 경로 | 권한 | ID |
|---|---|---|---|
| 대기 등록 (손님 전화번호로) | POST `/api/owner/waitings` | STORE | W-1~W-3, U-1 |
| 오늘 대기 목록 + 상태별 개수 | GET `/api/owner/waitings` | STORE | W-9a, W-10 |
| 다음 팀 호출 | POST `/api/owner/waitings/call-next` | STORE | W-7 |
| 입장 확인 | POST `/api/owner/waitings/{waitingId}/enter` | STORE (자기 가게 대기) | W-8 |
| 노쇼 처리 (+ 다음 팀 자동 호출) | POST `/api/owner/waitings/{waitingId}/no-show` | STORE (자기 가게 대기) | W-9 |
| 접수 열기 | POST `/api/owner/store/open` | STORE | W-11 |
| 접수 마감 | POST `/api/owner/store/close` | STORE | W-11 |

- 호출은 대기자를 지정하지 않는다. 번호가 가장 작은 WAITING 팀이 CALLED로 바뀐다.
- 목록은 "오늘 영업일"(`day_change_time` 기준으로 계산)의 대기만 보여 준다.
- 입장/노쇼는 대상 대기가 **내 가게 것이고 CALLED 상태인지** 검증한다.
- 등록 본문: `phone`, `partySize`(1~4). 서버는 `phone`으로 회원을 찾고 없으면 USER로 자동 가입시킨 뒤, 접수중인지, 제재 중이 아닌지, 같은 가게에 활성 대기가 없는지 검증하고 번호를 발급한다. 현장 확인은 이 API를 STORE 계정만 호출할 수 있다는 점이 맡는다.
- 번호만 받고 인증하지 않아서 남의 번호 도용을 완전히 막지는 못한다. 한계로 문서에 둔다(`01-요구사항.md` 8장).

### 3-5. 관리자 (ADMIN)

| 기능 | 메서드 + 경로 | 권한 | ID |
|---|---|---|---|
| 승인 대기 가게 목록 | GET `/api/admin/stores?status=PENDING` | ADMIN | U-4 |
| 가게 승인 | POST `/api/admin/stores/{storeId}/approve` | ADMIN | U-4 |
| 가게 거절 | POST `/api/admin/stores/{storeId}/reject` | ADMIN | U-4 |

### 3-6. 알림 / 데모 (후순위)

| 기능 | 메서드 + 경로 | 시점 |
|---|---|---|
| 알림 구독 (SSE) | GET `/api/notifications/stream` | 8주차 (인증 방식은 그때 결정) |
| 데모 패널 | `/api/demo/...` | 구현 시 별도 정리. `demo` 프로파일 또는 데모 계정에서만 활성화 |

## 4. 에러 응답 (전역 처리)

`@RestControllerAdvice` 한 곳에서 예외를 아래 형식으로 변환한다. 스택트레이스와 SQL 오류는 응답에 노출하지 않는다.

```json
{ "code": "WAITING_ALREADY_ACTIVE", "message": "이미 이 가게에 대기 중입니다." }
```

| HTTP | code (예시) | 상황 |
|---|---|---|
| 400 | INVALID_INPUT | 인원 1~4 위반 등 입력 검증 실패 |
| 401 | UNAUTHORIZED | 토큰 없음/만료 |
| 403 | FORBIDDEN | 권한 없음 (역할 불일치) |
| 403 | USER_BANNED | 노쇼 제재 중 |
| 404 | NOT_FOUND | 대상 없음 / 남의 대기 접근 |
| 409 | WAITING_ALREADY_ACTIVE | 같은 가게에 이미 활성 대기 |
| 409 | STORE_CLOSED | 접수 마감 |
| 409 | INVALID_STATUS | 허용되지 않는 상태 전이 (예: ENTERED를 취소) |
| 409 | WITHDRAW_BLOCKED_CALLED | 호출된(CALLED) 대기가 있어 탈퇴 불가 |

- 401/403 중 **Security 필터 단계에서 나는 것**은 `@RestControllerAdvice`가 못 잡는다. 3주차에 `AuthenticationEntryPoint`, `AccessDeniedHandler`로 같은 형식에 맞춘다.

## 5. 열린 질문 / 결정된 것

**해결됨 (2026-10-30)**
- ADMIN은 가게측 API(`/api/owner/...`)를 쓰지 않는다. ADMIN은 가게 관리(승인·거절), STORE는 자기 가게 운영으로 분리한다. 데모의 운영자 계정은 STORE 계정으로 만든다.
- 진행 중 대기가 있는 회원의 탈퇴: WAITING만 있으면 취소 안내 후 탈퇴, CALLED가 있으면 탈퇴 불가.
- 회원 탈퇴는 Core에 넣되 맨 뒤 순서로 구현한다.
- QR은 폐지하고 가게 태블릿에서 전화번호로 등록한다. 손님은 자동 가입한다.

**남음**
- 남의 번호로 대기를 거는 도용 방지 (지금은 현장 입력 + 제재 5회부터로 완화. 문자 구현체를 붙일 때 인증번호 단계를 Optional로 추가)
- 손님이 자기 폰에서 내 대기 상태를 보는 방법 (3주차 JWT 설계 때 정함)
- 활성 대기 1개 보장 방식 (4주차)
