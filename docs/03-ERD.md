# OpenRun ERD

> 상태: **영문 이름·타입 확정** (VARCHAR 길이는 임의로 잡은 값이라 구현하며 조정 가능).
> 원칙: 계산으로 구할 수 있는 값(내 앞 팀 수, 대기 시간)은 저장하지 않는다.
> 이름 규칙: DB는 snake_case, 상태값은 enum 문자열 저장(`@Enumerated(STRING)`, ORDINAL 금지).
> 2026-10-30 변경: 손님은 전화번호 계정(email 삭제, `phone` 추가, `login_id`·`password`는 STORE/ADMIN 전용), QR 폐지, 탈퇴 규칙 확정.

## 관계

```
users 1 ───── 0..1 stores      (가게 1곳당 가게측 계정 1개: stores.owner_user_id 유니크)
users 1 ───── 0..N waitings    (회원 1명이 여러 대기를 가질 수 있음, 활성은 팝업당 1개)
stores 1 ──── 0..N waitings    (가게 1곳에 여러 대기)
```

## 1. users (회원)

| 컬럼 | 타입 | 제약/비고 |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| login_id | VARCHAR(30) | **유니크**, **NULL 허용**. STORE·ADMIN의 로그인 아이디. 손님(USER)은 NULL. 탈퇴 시 `withdrawn_{id}`로 변경 (waitings.user_id와 이름이 겹치지 않게 분리) |
| password | VARCHAR(100) | **NULL 허용**. 해시된 비밀번호. STORE·ADMIN만 값이 있고 손님(USER)은 비밀번호가 없어 NULL |
| phone | VARCHAR(20) | **유니크**, **NULL 허용**. 손님(USER)은 필수(서비스에서 검사), STORE·ADMIN은 비워도 됨. 숫자만 저장. 탈퇴 시 `withdrawn_{id}`로 변경 (아래 탈퇴 규칙) |
| username | VARCHAR(30) | **NULL 허용**. 이름/닉네임. 태블릿에서 자동 가입한 손님은 NULL (화면에는 전화번호 뒷 4자리 등으로 표시) |
| provider | VARCHAR(10) | NOT NULL, 기본 `LOCAL`. 가입 경로 (LOCAL/소셜). 소셜 로그인은 범위 밖, 컬럼만 둠 |
| role | VARCHAR(10) | NOT NULL. USER / STORE / ADMIN |
| status | VARCHAR(10) | NOT NULL. ACTIVE / WITHDRAWN. 소프트 삭제 |
| consecutive_no_show_count | INT | NOT NULL, 기본 0. 연속 노쇼 횟수. 입장 시 0, 취소는 무관, 제재 종료 시 0 |
| banned_until | DATE | NULL. 제재 종료일. 비어 있으면 제재 없음. 5회째에 다음날~7일로 설정 |
| created_at | DATETIME | NOT NULL. 가입 시각 |
| withdrawn_at | DATETIME | NULL. 탈퇴 시각 |

**탈퇴 규칙**
- 탈퇴는 **users에서만** 관리한다. 가게 계정 탈퇴도 users.status = WITHDRAWN으로 처리하고, stores에는 탈퇴 값을 두지 않는다.
- 탈퇴 처리와 같은 트랜잭션에서 `phone`(손님) 또는 `login_id`(가게측)를 `withdrawn_{id}`로 바꿔 개인정보를 파기하고, 같은 값으로 재가입할 수 있게 한다. `{id}`가 들어가므로 재가입 후 재탈퇴해도 유니크가 깨지지 않는다.
- 재가입은 **새 행**으로 한다. 옛 대기·노쇼 기록은 옛 행에 남는다.
- 진행 중 대기가 **없으면** 바로 탈퇴한다.
- **WAITING만** 있으면 "대기 N건이 취소됩니다"를 안내하고 확인하면 `CANCELED`로 바꾼 뒤 탈퇴한다. 연속 노쇼 횟수는 바뀌지 않는다(B-1).
- **CALLED가** 있으면 탈퇴할 수 없다. 가게가 입장 확인 또는 노쇼 처리를 한 뒤에 탈퇴할 수 있다. (호출받고 탈퇴로 빠져나가 노쇼 제재를 피하는 것을 막기 위해)

## 2. stores (가게 = 팝업)

| 컬럼 | 타입 | 제약/비고 |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| owner_user_id | BIGINT | FK(users.id), **유니크**, NOT NULL. 가게측 계정 (1가게 1계정) |
| name | VARCHAR(50) | NOT NULL. 가게 이름 |
| address | VARCHAR(200) | NOT NULL. 상세 화면 표시용 |
| start_time | TIME | NULL. 하루 영업 시작. 표시용·확장용(자동 열기), Core에서는 비워 둠 |
| end_time | TIME | NULL. 하루 영업 종료. 표시용·확장용 |
| day_change_time | TIME | NOT NULL, 기본 `00:00:00`. 날짜 바뀌는 시각. 자정 넘겨 마감하는 가게는 마감 시각 |
| status | VARCHAR(10) | NOT NULL. PENDING / APPROVED / REJECTED. 가게측 가입 승인 흐름만 담당 (탈퇴는 users에서 관리) |
| accept_status | VARCHAR(10) | NOT NULL, 기본 `CLOSED`. OPEN / CLOSED. 수동 접수 열기/닫기. 영업 상태로 읽히지 않게 구분 |
| created_at | DATETIME | NOT NULL. 등록 시각 |

- 위치(위도/경도) 컬럼은 **두지 않는다**. (위치 검증 대신 가게 태블릿에서만 등록)
- 요일별 스케줄/날짜 예외는 Optional이라 지금은 테이블을 만들지 않는다.
- 가게가 목록에 보이는 조건: users.status = ACTIVE, stores.status = APPROVED, accept_status = OPEN.

## 3. waitings (대기)

| 컬럼 | 타입 | 제약/비고 |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | FK(users.id), NOT NULL. 대기한 회원 |
| store_id | BIGINT | FK(stores.id), NOT NULL. 대기한 가게 |
| business_date | DATE | NOT NULL. 영업일 = (등록 시각 - stores.day_change_time)의 날짜 |
| queue_number | INT | NOT NULL. 팝업·영업일 기준 1부터, 취소돼도 재사용 안 함 |
| party_size | TINYINT | NOT NULL. 인원 1~4 (서버 검증) |
| status | VARCHAR(10) | NOT NULL. WAITING / CALLED / ENTERED / CANCELED / NO_SHOW (대기중/호출됨/입장완료/취소됨/노쇼) |
| is_active | TINYINT | **생성 컬럼(STORED)**, 직접 넣지 않음. status가 WAITING/CALLED이면 1, 아니면 NULL. 활성 대기 1개 보장용 (아래 참고) |
| created_at | DATETIME | NOT NULL. 등록 시각 |
| called_at | DATETIME | NULL. 호출 시각. 호출 5분 경과 표시에 사용 |
| ended_at | DATETIME | NULL. 종료 시각. 입장·취소·노쇼 어느 쪽이든 끝난 시각. 종류는 status로 구분 |

**유니크 제약**: `(store_id, business_date, queue_number)` — 같은 팝업·같은 영업일에 같은 번호가 두 번 저장되지 않게 하는 마지막 안전장치. (번호만 유니크로 걸면 다른 팝업/다른 날의 같은 번호가 막힌다.)

**인덱스**: `(store_id, business_date, status)` — 가게 화면의 "오늘 대기 목록"용. 3~4주차 k6 실험 전에 추가한다.

### 활성 대기 1개 보장 (W-3, NF-2) — 결정 (2026-10-31): 생성 컬럼 유니크
- **문제**: `(user_id, store_id)`에 그냥 유니크를 걸면, 취소·입장·노쇼로 끝난 행이 남아 있어 재등록이 막힌다. MySQL에는 "활성일 때만" 거는 조건부 유니크 인덱스가 없다.
- **방법**: `is_active` 생성 컬럼(활성이면 1, 아니면 NULL)을 두고 `UNIQUE (user_id, store_id, is_active)`를 건다. MySQL 유니크는 NULL이 들어간 행끼리는 중복으로 보지 않으므로, 끝난 대기는 몇 개든 쌓이고 활성 대기는 (회원, 가게)당 1개만 저장된다.
- **정의 예**: `is_active TINYINT GENERATED ALWAYS AS (IF(status IN ('WAITING','CALLED'), 1, NULL)) STORED`
- **위반 시**: DB가 유니크 예외를 던지고, 서비스가 이를 409 `WAITING_ALREADY_ACTIVE`로 바꾼다. 서비스의 "이미 있나 확인"은 친절한 메시지용으로 두되, 최종 보장은 DB가 한다.
- **주의**: JPA에서는 읽기 전용으로 매핑한다(`insertable=false, updatable=false`). 생성 컬럼은 `ddl-auto`가 만들기 어려울 수 있어, 스키마를 SQL로 관리할지는 프로젝트 세팅 때 정한다.
- **탈락한 후보**: 회원 행 `FOR UPDATE` 락(다른 가게 등록까지 직렬화됨), 별도 active 테이블(테이블이 늘고 삭제를 빼먹으면 어긋남). Redisson 락은 필요하면 보조로 추가한다(Optional).
- **검증**: 4주차에 스레드 100개 동시 등록 테스트로 "1건만 성공"을 확인한다.

### 저장하지 않고 계산하는 것
| 값 | 계산 방법 |
|---|---|
| 내 앞 팀 수 | 같은 가게·영업일에서 활성 상태(대기중, 호출됨)이면서 내 번호보다 작은 건수 |
| 대기 시간 | `호출 시각 - 등록 시각` |
| 현재 대기 팀 수 | 같은 가게·영업일의 활성 상태 건수 |

### 아직 정하지 않은 것
- 순번 계산에 쓸 인덱스 설계 → 구현하며 측정 후

## 규칙과 컬럼 대응 (확인용)

| 요구사항 | 대응 |
|---|---|
| W-2 번호 중복 없음 | waitings 유니크 제약 + 채번 방식(02) |
| W-3 활성 대기 1개 | waitings.is_active + 유니크 `(user_id, store_id, is_active)` (결정 10-31, 4주차 검증) |
| W-9 노쇼 체크 | waitings.status = NO_SHOW, ended_at |
| W-9a 5분 경과 표시 | waitings.called_at |
| W-11 접수 열기/닫기 | stores.accept_status |
| P-6 날짜 바뀌는 시각 | stores.day_change_time → waitings.business_date |
| B-1~B-4 노쇼 제재 | users.consecutive_no_show_count, banned_until |
| U-1 손님 자동 가입 | users.phone, users.role = USER |
| U-6 회원 탈퇴 | users.status, withdrawn_at, phone·login_id 변경 |
| U-4 가게측 가입·승인 | users.role = STORE, stores.status |
| U-5 자기 가게만 | stores.owner_user_id로 소유 확인 |
