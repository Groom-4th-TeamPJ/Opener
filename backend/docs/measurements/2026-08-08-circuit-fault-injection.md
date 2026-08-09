# 서킷 실패 주입 검증 (llm-chat)

- **일자**: 2026-08-08
- **대상**: `OpenAiLlmService.chatStream` 의 `@CircuitBreaker(name = "llm-chat")`
- **목적**: 3-1(`resilience4j-reactor` 추가)이 **스트리밍 실패를 실제로 집계**하는지를 로그가 아니라 수치로 확인
- **비용**: OpenAI 실호출 **0회**. API 키는 더미로 덮어 실제 키가 컨테이너 밖으로 나갈 경로를 없앴다

---

## 1. 왜 계획서 명령을 그대로 쓰지 않았나

P3 계획서 Step 10 은 `SPRING_AI_OPENAI_BASE_URL=http://127.0.0.1:59999` 로 기동해
`bootRun` 하라고 적었다. **세 군데가 실물과 어긋난다.**

| 계획서 | 실제 | 결과 |
|---|---|---|
| 부모 키 하나로 채팅을 죽은 포트에 붙인다 | `application-dev.yml:80` 이 **자식 키** `spring.ai.openai.chat.base-url` 을 명시 | 부모만 덮으면 채팅은 **진짜 OpenAI 로 나간다** |
| 죽은 포트면 서킷이 실패를 센다 | RAG 켜짐 상태에선 `chatStream` 본문의 `similaritySearch` 가 **먼저** 죽는다 | **조립 시점** 예외라 리액터 서킷(구독 시점 집계)이 못 센다 |
| 호스트에서 `bootRun` | chat 클러스터가 `172.20.0.x` 를 광고 | macOS 호스트에서 도달 불가 → 앱을 같은 도커 네트워크에서 돌려야 한다 |

### 교정 설계 — 부모와 자식을 갈랐다

부모 키는 **로컬 임베딩 스텁**, 자식(chat) 키만 **죽은 포트**로 둔다.

```
spring.ai.openai.base-url            = http://openai-stub:59998   (스텁)
spring.ai.openai.embedding.base-url  = http://openai-stub:59998   (스텁)
spring.ai.openai.chat.base-url       = http://127.0.0.1:59999     (죽은 포트)
```

이렇게 하면 임베딩은 성공해 `similaritySearch` 가 조립 시점에 죽지 않고,
실패가 **구독 시점에만** 발생해 리액터 서킷이 집계할 수 있다.
동시에 *"임베딩은 되는데 채팅만 실패한다"* 는 관찰 자체가
**자식 키가 채팅 경로를 지배한다**는 증거가 되어 위 1행을 근거화한다.

### 부수 설정과 이유

| 설정 | 이유 |
|---|---|
| `SPRING_AI_RETRY_MAX_ATTEMPTS=1` | 기본 10회 백오프가 측정 시간을 지배한다 |
| `OPENAI_API_KEY` 더미 | 실제 키가 컨테이너 밖으로 나갈 경로를 없앤다 |
| `--app.documents.directory` 빈 디렉터리 | PDF 로더가 **스텁의 가짜 임베딩으로 벡터 스토어를 오염**시키는 것을 막는다 |
| `--logging.config` 콘솔 전용 | 기본 logback 의 Loki 어펜더가 `localhost:3100` 에 못 닿아 로깅 파이프라인이 막힌다 |

---

## 2. 절차

1. Opener 인프라 기동 (db · redis-auth · chat 6노드 · rabbitmq), `cluster_state:ok` 확인
2. 임베딩 스텁을 `opener_local-network` 위에 올린다 (`/v1/embeddings` 만 응답)
3. 앱을 같은 네트워크의 컨테이너로 기동
4. `POST /api/auth/form-signup` → 쿠키 인증
5. `GET /api/chat/connect?sessionId=N` 을 **열어 둔 채**
6. `POST /api/chat/message` **6회** (3초 간격)
7. `GET /api/actuator/prometheus` 에서 `resilience4j_circuitbreaker_*{name="llm-chat"}` 수집

> 5번을 건너뛰면 Sink 가 없어 `processMessage` 가 `SESSION_EXPIRED` 로 먼저 막힌다.

---

## 3. 실측 결과 (`resilience4j-reactor` 있음)

HTTP 응답: **6회 전부 200**(접수 확인). 실패는 SSE `event:error` 로 전달됐다 —
설계대로 HTTP 는 접수만 알리고 실제 결과는 스트림으로 간다.

| 지표 | 값 |
|---|---|
| `resilience4j_circuitbreaker_calls_seconds_count{kind="failed"}` | **5** |
| `resilience4j_circuitbreaker_calls_seconds_count{kind="successful"}` | 0 |
| `resilience4j_circuitbreaker_calls_seconds_count{kind="ignored"}` | 0 |
| `resilience4j_circuitbreaker_buffered_calls{kind="failed"}` | 5 |
| `resilience4j_circuitbreaker_not_permitted_calls_total` | **1** |
| `resilience4j_circuitbreaker_state{state="open"}` | **1.0** |
| `resilience4j_circuitbreaker_failure_rate` | **100.0** |
| `calls_seconds_max{kind="failed"}` | 0.192s |

**설정과 정확히 일치한다.** `minimumNumberOfCalls: 5` · `failureRateThreshold: 50` 이므로
5번째 실패에서 실패율 100% 가 확정돼 OPEN 으로 전이하고,
**6번째 호출은 아예 실행되지 않아** `not_permitted` 로 1건 계상된다.
`ignored=0` 은 3-13 의 `BusinessException` 무시 설정이 **인프라 실패까지 삼키지는 않는다**는 확인이다.

→ `PORTFOLIO.html` 의 *"서킷이 LLM 장애를 차단한다"* 는 이 수치로 **참**이다.
6번째 요청이 상류에 나가지 않았다는 것이 차단의 실체다.

---

## 4. 측정이 드러낸 프로덕션 결함 2건 (커밋 `0e8f341`)

**1회차 주입은 위 표와 전혀 다른 결과를 냈다.** 그 자체가 결함의 증거였다.

| 요청 | 1회차 결과 |
|---|---|
| msg1 | **500** — `I/O error on POST … /v1/embeddings` |
| msg2~6 | **401 `C_003` 만료된 세션입니다** (5회 연속) |

### ① 조립 시점 실패는 서킷이 한 건도 세지 못한다

`chatStream` 이 본문에서 히스토리 조회와 `similaritySearch` 를 **동기로** 수행하므로,
상류가 죽으면 `Flux` 를 만들기도 전에 예외가 나간다.
리액터 서킷은 **구독 시점** 오류만 집계한다.
→ 3-1 로 어스펙트를 살려도 **보호 구간이 스트리밍 호출 이후로만 한정**돼 있었다.

### ② 그 실패가 세션을 영구히 막는다

`processMessage` 는 이미 `SEND_MESSAGE` 로 `PROCESSING` 에 들어간 뒤인데,
동기 예외는 `doOnError` 를 타지 않아 상태가 회수되지 않는다.
3-16 이 `PROCESSING → STREAM_ERROR` 전이를 *"없으면 세션이 갇힌다"* 며 열어 뒀으나
**그 전이를 보내는 코드가 이 경로에 없었다.** msg2~6 의 401 이 그 증상이다.

**수정** — 두 `LlmService` 구현의 조회·조립을 `Flux.defer` 로 구독 시점에 옮기고(①·② 동시 해소),
호출부에도 회수 절차를 둬 어떤 구현이 동기로 던져도 세션이 갇히지 않게 했다(②의 구조적 방어).
테스트 3종을 붙였고 **수정을 되돌리면 셋 다 실패**함을 확인했다.

> 1회차의 임베딩 I/O 오류 자체는 스텁 결함(HTTP/1.0 로 매 응답 연결 종료 → reactor-netty
> 커넥션 풀 재사용 실패)과 Docker 네트워크 장애가 겹친 것이었다.
> **그러나 pgvector·임베딩 상류 장애는 운영에서 실제로 일어나는 사건**이므로
> 결함은 그 원인과 무관하게 유효하다. 재현 조건은 *"검색이 실패한다"* 하나뿐이다.

---

## 5. 한계 — 이 문서가 뒷받침하지 않는 것

- **`resilience4j-reactor` 없는 대조군은 미측정이다.** 호스트 CPU 포화로 완주하지 못했다.
  따라서 *"추가 전에는 0건이었다"* 를 **수치로 주장하지 않는다.**
  근거는 3-1 의 단위 테스트(클래스패스 조건 + 연산자 집계)와 위 ①의 메커니즘까지다.
- **계획서의 판정 기준 *"기동 로그에 Reactor Aspect 경고가 없다"* 는 이 설정에서 무의미하다.**
  그 문구는 Resilience4j 가 **DEBUG** 로 남기는데 측정용 logback 루트가 INFO 다.
  의존성을 뺀 빌드에서도 **0건**으로 나왔다 — 즉 이 기준은 양쪽을 구분하지 못한다.
  판정은 **카운터로만** 한다.
- **`OpenAiLlmServiceWithoutRag.chatStream` 에는 서킷이 없다.** RAG 를 끄면 채팅 경로가
  무보호다. 이 측정은 RAG 켜짐 구성만 다룬다.
- TTFT(3-9)·RAG 스윕(3-10)은 별건이며 **여전히 미측정**이다.

---

## 6. 재현

세션 스크래치패드의 스크립트(`stub_openai.py` · `run_app.sh` · `inject.sh`)로 수행했다.
스크래치패드는 세션과 함께 사라지므로, 재작성에 필요한 정보는 **§1 의 환경변수 표와 §2 의 절차**가 전부다.

주의할 점 둘:
- 임베딩 스텁은 **HTTP/1.1** 로 응답해야 한다. Python `BaseHTTPRequestHandler` 기본값(HTTP/1.0)은
  매 응답마다 연결을 닫아 reactor-netty 커넥션 풀에서 `Connection prematurely closed BEFORE response` 를 만든다
- `sessionId` 를 JSON 에 실을 때 **앞자리 0** 이 붙으면 숫자 리터럴이 깨져 500 이 난다
