# Template Project

Docker Compose를 사용한 Full-Stack 프로젝트 템플릿입니다.

## 프로젝트 구조

```
template/
├── backend/                    # Spring Boot 백엔드
│   ├── .env.dev               # Backend 개발 환경 설정 (Docker 내부)
│   ├── .env.test              # Backend 테스트 환경 설정 (IntelliJ 로컬)
│   ├── .env.prod              # Backend 운영 환경 설정
│   ├── .env.dev.example
│   ├── .env.test.example
│   └── .env.prod.example
├── frontend/                   # Frontend 애플리케이션
│   ├── .env.dev               # Frontend 개발 환경 설정
│   ├── .env.prod              # Frontend 운영 환경 설정
│   ├── .env.dev.example
│   └── .env.prod.example
├── nginx/                      # Nginx 설정
│   ├── nginx.dev.conf         # 개발 환경용 (모든 서비스 Docker)
│   ├── nginx.test.conf        # 테스트 환경용 (Backend는 호스트)
│   ├── nginx.prod.conf        # 운영 환경용
│   └── ssl/                   # SSL 인증서 디렉토리
├── docker-compose.dev.yml     # 개발: 전체 서비스 Docker 실행
├── docker-compose.test.yml    # 테스트: Backend만 IntelliJ 실행
└── docker-compose.prod.yml    # 운영: 프로덕션 배포
```

## 시작하기

### 1. 환경 설정 파일 준비

```bash
# Backend 환경 설정
cp backend/.env.dev.example backend/.env.dev
cp backend/.env.test.example backend/.env.test
cp backend/.env.prod.example backend/.env.prod

# Frontend 환경 설정
cp frontend/.env.dev.example frontend/.env.dev
cp frontend/.env.prod.example frontend/.env.prod
```

### 2. 개발 환경 실행 (전체 Docker)

모든 서비스를 Docker로 실행합니다.

```bash
docker-compose -f docker-compose.dev.yml up -d
```

**실행 서비스:**

- PostgreSQL (DB)
- Redis
- Backend (Spring Boot)
- Frontend
- Nginx (Reverse Proxy)

**접근 방법:**

- **Nginx를 통한 접근** (권장): http://localhost
  - Frontend: http://localhost/
  - Backend API: http://localhost/api/
- Frontend (직접): http://localhost:3000
- Backend (직접): http://localhost:8080
- PostgreSQL: localhost:5432
- Redis: localhost:6379

### 3. 테스트 환경 실행 (IntelliJ에서 Backend 실행)

IntelliJ에서 Backend를 직접 실행하고, 나머지 서비스는 Docker로 실행합니다.

```bash
# 1. DB, Redis, Frontend, Nginx를 Docker로 실행
docker-compose -f docker-compose.test.yml up -d

# 2. IntelliJ에서 Spring Boot 실행
# - Run Configuration에서 환경 변수 파일 설정: backend/.env.test
# - 또는 .env.test 파일의 환경 변수를 직접 설정
```

**실행 서비스:**

- PostgreSQL (Docker)
- Redis (Docker)
- **Backend (IntelliJ)** ← 직접 실행
- Frontend (Docker)
- Nginx (Docker)

**접근 방법:**

- **Nginx를 통한 접근**: http://localhost
  - Frontend: http://localhost/
  - Backend API: http://localhost/api/ (IntelliJ Backend로 프록시)
- Frontend (직접): http://localhost:3000
- **Backend (IntelliJ)**: http://localhost:8080
- PostgreSQL: localhost:5432
- Redis: localhost:6379

**IntelliJ에서 Backend 실행 시 환경 변수:**

- `DB_URL=jdbc:postgresql://localhost:5432/postgres`
- `REDIS_HOST=localhost`
- backend/.env.test 파일 참조

### 4. 운영 환경 실행

```bash
# ⚠️ 운영 환경 실행 전 반드시 비밀번호들을 변경하세요!
docker-compose -f docker-compose.prod.yml up -d
```

**접근 방법 (Nginx를 통해서만 접근):**

- HTTP: http://your-domain.com (자동으로 HTTPS로 리다이렉트)
- HTTPS: https://your-domain.com
  - Frontend: https://your-domain.com/
  - Backend API: https://your-domain.com/api/

## 환경별 차이점

### Development (docker-compose.dev.yml)

**용도:** 전체 스택을 Docker로 실행하여 통합 개발

**특징:**

- 모든 서비스가 Docker 컨테이너로 실행
- 모든 서비스의 포트가 외부에 노출됨 (디버깅 용이)
- Nginx는 선택사항 (포트 80)
- Frontend, Backend 직접 접근 가능
- 로깅 레벨: DEBUG/INFO
- **Frontend Hot Reload 지원** - 코드 수정 시 즉시 반영
- Frontend는 개발 서버(pnpm dev) 실행

**Backend 환경 변수:** backend/.env.dev

- DB_URL: `jdbc:postgresql://db:5432/postgres`
- REDIS_HOST: `redis`

### Test (docker-compose.test.yml)

**용도:** IntelliJ에서 Backend를 직접 실행하며 개발 (디버깅 편리)

**특징:**

- **Backend는 IntelliJ에서 직접 실행** (디버깅 가능)
- DB, Redis, Frontend, Nginx는 Docker로 실행
- Nginx가 호스트 머신의 Backend를 프록시 (`host.docker.internal:8080`)
- Backend 코드 수정 후 즉시 재시작 가능
- **Frontend Hot Reload 지원** - 코드 수정 시 즉시 반영

**Backend 환경 변수:** backend/.env.test

- DB_URL: `jdbc:postgresql://localhost:5432/postgres`
- REDIS_HOST: `localhost`

**Nginx 설정:** nginx.test.conf

- Backend upstream: `host.docker.internal:8080` (호스트 머신)
- Frontend upstream: `frontend:3000` (Docker 컨테이너)

### Production (docker-compose.prod.yml)

**용도:** 실제 서비스 배포

**특징:**

- **Nginx를 통해서만 외부 접근 가능** (보안 강화)
- DB, Redis, Backend, Frontend는 외부 포트 노출 안함
- HTTPS 강제 (HTTP → HTTPS 자동 리다이렉트)
- Rate limiting 적용
- 보안 헤더 추가
- Gzip 압축 활성화
- 로깅 레벨: WARN
- 정적 파일 캐싱
- **Frontend 프로덕션 빌드** (Dockerfile 사용)

**Backend 환경 변수:** backend/.env.prod

## 포트 구성

### Development 환경

| 서비스     | 내부 포트 | 외부 포트 | 접근 URL              |
| ---------- | --------- | --------- | --------------------- |
| Frontend   | 3000      | 3000      | http://localhost:3000 |
| Backend    | 8080      | 8080      | http://localhost:8080 |
| Nginx      | 80        | 80        | http://localhost      |
| PostgreSQL | 5432      | 5432      | localhost:5432        |
| Redis      | 6379      | 6379      | localhost:6379        |

### Test 환경 (IntelliJ Backend)

| 서비스                 | 내부 포트 | 외부 포트 | 접근 URL              |
| ---------------------- | --------- | --------- | --------------------- |
| Frontend (Docker)      | 3000      | 3000      | http://localhost:3000 |
| **Backend (IntelliJ)** | 8080      | 8080      | http://localhost:8080 |
| Nginx (Docker)         | 80        | 80        | http://localhost      |
| PostgreSQL (Docker)    | 5432      | 5432      | localhost:5432        |
| Redis (Docker)         | 6379      | 6379      | localhost:6379        |

### Production 환경

| 서비스     | 내부 포트 | 외부 포트 | 접근 URL                |
| ---------- | --------- | --------- | ----------------------- |
| Frontend   | 3000      | -         | Nginx를 통해서만        |
| Backend    | 8080      | -         | Nginx를 통해서만        |
| Nginx      | 80/443    | 80/443    | https://your-domain.com |
| PostgreSQL | 5432      | -         | 외부 접근 불가          |
| Redis      | 6379      | -         | 외부 접근 불가          |

## 주요 명령어

```bash
# 개발 환경 (전체 Docker, Frontend Hot Reload)
docker-compose -f docker-compose.dev.yml up -d
docker-compose -f docker-compose.dev.yml down

# 테스트 환경 (IntelliJ Backend)
docker-compose -f docker-compose.test.yml up -d
docker-compose -f docker-compose.test.yml down
# + IntelliJ에서 Spring Boot 실행

# 운영 환경 (SSL 인증서 필요)
docker-compose -f docker-compose.prod.yml up -d --build
docker-compose -f docker-compose.prod.yml down

# 로그 확인
docker-compose -f docker-compose.dev.yml logs -f [service-name]

# Frontend 로그 실시간 확인 (Hot Reload 확인)
docker-compose -f docker-compose.dev.yml logs -f frontend

# 특정 서비스만 재시작
docker-compose -f docker-compose.dev.yml restart [service-name]

# Backend만 재빌드 (Frontend는 Hot Reload로 자동 반영)
docker-compose -f docker-compose.dev.yml up -d --build backend

# 볼륨 포함 완전 삭제
docker-compose -f docker-compose.dev.yml down -v
```

## Frontend Hot Reload

### 개발/테스트 환경 (dev, test)

**✅ Hot Reload 지원**

- Frontend 코드 수정 시 **즉시 반영**
- 브라우저 자동 새로고침
- volumes로 소스 코드 마운트
- Next.js 개발 서버(`pnpm dev`) 실행

```bash
# 코드 수정 후 별도 작업 불필요
# 저장만 하면 자동으로 반영됨
```

### 프로덕션 환경 (prod)

**❌ Hot Reload 없음**

- Dockerfile로 프로덕션 빌드
- 코드 수정 시 재빌드 필요

```bash
# 코드 수정 후 재빌드 필요
docker-compose -f docker-compose.prod.yml up -d --build frontend
```

## IntelliJ에서 Backend 실행하기

### 1. Run Configuration 설정

**방법 1: .env 파일 플러그인 사용**

1. IntelliJ에 EnvFile 플러그인 설치
2. Run Configuration 생성
3. EnvFile 탭에서 `backend/.env.test` 파일 추가

**방법 2: Environment Variables 직접 설정**

1. Run Configuration 생성
2. Environment Variables 섹션에 다음 값 입력:
   ```
   SPRING_PROFILES_ACTIVE=dev
   DB_URL=jdbc:postgresql://localhost:5432/postgres
   DB_USERNAME=dev
   DB_PASSWORD=dev123
   REDIS_HOST=localhost
   REDIS_PORT=6379
   JWT_SECRET=dev_jwt_secret_key_for_development_only
   JWT_EXPIRATION=86400000
   LOG_LEVEL=DEBUG
   ```

### 2. Docker 서비스 실행

```bash
docker-compose -f docker-compose.test.yml up -d
```

### 3. IntelliJ에서 Spring Boot 실행

Run Configuration을 실행하여 Backend를 시작합니다.

### 4. 접근 확인

- Backend 직접: http://localhost:8080
- Nginx 통해: http://localhost:8000/api/

## SSL 인증서 설정 (Production)

**⚠️ 프로덕션 환경(docker-compose.prod.yml)을 사용하려면 먼저 SSL 인증서가 필요합니다.**

개발/테스트 환경에서는 `docker-compose.dev.yml` 또는 `docker-compose.test.yml`을 사용하세요.

### 실제 배포 시 SSL 인증서 설정

#### 방법 1: Let's Encrypt (무료, 권장)

```bash
# Certbot 설치 (Ubuntu/Debian)
sudo apt-get update
sudo apt-get install certbot

# 인증서 발급
sudo certbot certonly --standalone -d your-domain.com

# 발급받은 인증서를 nginx/ssl/에 복사
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem nginx/ssl/key.pem
```

#### 방법 2: 자체 서명 인증서 (개발/테스트용)

```bash
# nginx/ssl 디렉토리 생성
mkdir -p nginx/ssl

# 자체 서명 인증서 생성
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/key.pem \
  -out nginx/ssl/cert.pem \
  -subj "/C=KR/ST=Seoul/L=Seoul/O=Development/OU=IT/CN=localhost"
```

**참고:** 자체 서명 인증서는 브라우저에서 보안 경고가 표시됩니다. 실제 배포 시에는 반드시 공인 인증서를 사용하세요.

## 환경 변수 관리

### 환경 변수 파일 구조

- **Backend:**
  - `backend/.env.dev` - Docker 내부 실행용 (db, redis 서비스명 사용)
  - `backend/.env.test` - IntelliJ 로컬 실행용 (localhost 사용)
  - `backend/.env.prod` - 프로덕션 배포용

- **Frontend:**
  - `frontend/.env.dev` - 개발 환경용
  - `frontend/.env.prod` - 프로덕션 환경용

### Git 관리

- `.env.*` 파일은 Git에 커밋되지 않음 (.gitignore에 등록됨)
- `.env.*.example` 파일은 커밋되어 팀원들이 참고할 수 있음
- **중요: 운영 환경에서는 반드시 비밀번호를 변경하세요!**

## 문제 해결

### 포트 충돌 시

docker-compose 파일에서 포트 번호를 직접 변경하세요.

```yaml
# docker-compose.dev.yml 예시
ports:
  - '8081:8080' # 8080 대신 8081 사용
```

### IntelliJ Backend가 DB/Redis 연결 실패

1. docker-compose.test.yml이 실행 중인지 확인

   ```bash
   docker ps
   ```

2. backend/.env.test의 호스트가 `localhost`인지 확인
   ```
   DB_URL=jdbc:postgresql://localhost:5432/postgres
   REDIS_HOST=localhost
   ```

### Nginx에서 Backend 연결 실패 (test 환경)

1. IntelliJ에서 Backend가 실행 중인지 확인 (http://localhost:8080)

2. Docker의 extra_hosts 설정 확인

   ```yaml
   extra_hosts:
     - 'host.docker.internal:host-gateway'
   ```

3. nginx.test.conf에서 upstream 확인
   ```nginx
   upstream backend {
       server host.docker.internal:8080;
   }
   ```

### 볼륨 초기화

```bash
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d
```

## 보안 권장사항

1. **운영 환경에서 반드시 변경해야 할 값:**
   - `DB_PASSWORD` (backend/.env.prod)
   - `REDIS_PASSWORD` (backend/.env.prod)
   - `JWT_SECRET` (backend/.env.prod)

2. **SSL 인증서:**
   - 운영 환경에서는 반드시 유효한 SSL 인증서 사용
   - Let's Encrypt 무료 인증서 권장

3. **방화벽:**
   - 운영 환경에서는 80, 443 포트만 외부에 개방
   - DB, Redis 포트는 절대 외부에 노출하지 마세요

4. **환경 변수 파일:**
   - .env 파일을 Git에 커밋하지 마세요
   - 민감한 정보는 암호화된 저장소나 환경 변수 관리 서비스 사용 권장

## 라이선스

MIT
