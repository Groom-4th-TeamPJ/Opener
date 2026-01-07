
# Flyway 사용 가이드

이 프로젝트는 **DB 스키마 버전 관리**를 위해 **Flyway**를 사용합니다.

---

## 1️⃣ Flyway 적용 방법

### 1. application-dev.yml 설정 변경

`application-dev.yml`에서 **Flyway 활성화**가 필요합니다.

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration


  jpa:
    hibernate:
      ddl-auto: none
```

> 📌 `baseline-on-migrate: true`
> 기존 DB가 존재하는 상태에서도 Flyway 적용 가능하도록 설정

---

### 2. migration 파일 위치

```
src/main/resources/db/migration/
```

### 3. 파일 네이밍 규칙

```text
V{버전}__{설명}.sql
```

예시:

```
V1__init_schema.sql
V2__add_user_results.sql
V3__add_questions.sql
```

⚠️ 이미 실행된 migration 파일은 **절대 수정 금지**

---

## 2️⃣ Flyway 실행 방식

Spring Boot 애플리케이션 실행 시:

* Flyway가 자동으로 실행됨
* 실행 이력은 `flyway_schema_history` 테이블에 기록됨
* 실행된 migration은 **다시 실행되지 않음**

---

## 3️⃣ DB 초기화 (완전 초기화 후 다시 시작)

> **주의:** 개발 환경(dev)에서만 수행
> 운영 환경(prod)에서는 절대 금지

### 1. 기존 테이블 삭제

```sql
DROP TABLE IF EXISTS "users" CASCADE;
DROP TABLE IF EXISTS "exam_results" CASCADE;
DROP TABLE IF EXISTS "user_cans" CASCADE;
DROP TABLE IF EXISTS "questions" CASCADE;
DROP TABLE IF EXISTS "question_new" CASCADE;
DROP TABLE IF EXISTS "can_usage_logs" CASCADE;
DROP TABLE IF EXISTS "credentials" CASCADE;
DROP TABLE IF EXISTS "exams" CASCADE;
DROP TABLE IF EXISTS "question_results" CASCADE;
DROP TABLE IF EXISTS "chat_session" CASCADE;
DROP TABLE IF EXISTS "chat_message" CASCADE;
```

---

### 2. Flyway 메타데이터 테이블 삭제

```sql
DROP TABLE IF EXISTS flyway_schema_history;
```

📌 이 테이블은 Flyway가 **어떤 migration이 실행되었는지** 기록하는 테이블입니다.
삭제 시 Flyway는 **처음 실행된 것처럼 동작**합니다.

---

### 3. 애플리케이션 재실행

```bash
./gradlew bootRun
```

또는 IDE에서 Spring Boot 실행

➡️ Flyway가 `V1__*.sql`부터 **모든 migration을 다시 실행**

---

## 4️⃣ 개발 시 주의사항 (중요)

### ❌ 하면 안 되는 것

* 이미 실행된 migration 파일 수정
* DB에 직접 테이블 생성/수정
* Flyway 없이 스키마 변경

### ⭕ 반드시 지킬 것

* 모든 스키마 변경은 새 migration 파일로
* 버전은 **항상 증가**
* dev 환경에서만 DB 초기화

---

## 5️⃣ 자주 발생하는 이슈

### ❓ migration 수정이 필요할 때

👉 **새 migration 파일 생성**

예:

```
V4__alter_questions_add_column.sql
```

---

### ❓ migration 순서가 꼬였을 때 (dev)

👉 **DB 초기화 후 재실행**

1. 테이블 삭제
2. `flyway_schema_history` 삭제
3. 애플리케이션 재실행

---

## 6️⃣ 권장 운영 전략

| 환경    | 전략            |
| ----- | ------------- |
| dev   | 초기화 가능        |
| stage | migration만 허용 |
| prod  | 절대 초기화 금지     |

---

## 🔚 요약

* Flyway는 **DB 변경 이력의 단일 진실 소스**
* migration 파일이 곧 스키마
* 초기화는 **개발 환경에서만**
