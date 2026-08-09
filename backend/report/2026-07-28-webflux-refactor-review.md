# WebFlux 리팩터링 리뷰 보고서

- **대상 커밋**: `5449953` (refactor : llm, 채팅 기능 webflux 기반 리펙토링)
- **브랜치**: `be/setting/branch-update` (기준 `stage`)
- **작성일**: 2026-07-28
- **리뷰어**: convention-auditor, diff-reviewer, security-reviewer (병렬 실행)
- **범위**: 정적 분석만. 테스트·런타임 실행 없음
- **조치**: 보고 전용. 이 보고서에 따른 코드 수정은 수행하지 않음

---

## 요약

WebFlux 전환 과정에서 **예외 처리 경로가 끊긴 결함 1건(high)** 과
**인가 검증 누락 1건(high)** 이 발견됐다.
전자는 `Flux` 조립 전 동기 실행 코드가 `doOnError` 를 우회하는 문제,
후자는 `questionResultId` 소유권 미검증으로 인한 IDOR 이다.

| 심각도 | 건수 |
|---|---|
| high | 2 |
| medium | 4 |
| low | 3 |

---

## 지적 사항

| 파일:라인 | 담당 | 유형 | 심각도 | 근거·제안 |
|---|---|---|---|---|
| `ChatServiceImpl.java:300-318`, `:360-405` | security-reviewer | 인가 | **high** | IDOR. `validateSessionOwner(sessionId, userId)` 만 검사하고 body 의 `questionResultId` 는 미검증 → 자기 세션 + 타인의 `questionResultId` 조합으로 `markOpener()` / `recordOpenerUsage()` 가 남의 기록에 적용된다. `saveMessages` → `ChatMessageProducer` → `ChatMessageConsumer` 경로도 sessionId 만 확인하므로 공격자 채팅이 피해자 QuestionResult 에 저장되고 이후 `ScrapbookServiceImpl.getScrapbookDetail` 로 노출된다. **두 메서드 모두에서 QuestionResult → `ExamResult.getUserId()` 가 호출자 userId 와 일치하는지 검증** |
| `OpenAiLlmServiceWithoutRag.java:64`<br>(호출부 `ChatServiceImpl.java:264`)<br>동일 패턴 `ChatServiceImpl.java:247`, `:390` | diff-reviewer, convention-auditor | 예외 처리 | **high** | `chatMemory.get(sessionId)` 이 `Flux` 조립 **전에** 동기 실행되어 예외가 `.doOnError()` / `.subscribe()` 에 도달하지 못한다. 이 클래스는 `@CircuitBreaker` 가 없다(`app.rag.enabled` 미설정 시 `matchIfMissing=true` 로 선택되는 기본 빈). 결과적으로 `emitError` 미발행, `STREAM_ERROR` 미전송 → 상태 머신이 `PROCESSING` 고착 → 다음 요청이 `SESSION_EXPIRED` 로 거부되고 SSE 클라이언트는 async 타임아웃(30분)까지 무응답. RAG 경로(`OpenAiLlmService.chatStream`)는 `@CircuitBreaker(fallbackMethod=...)` 덕에 `resilience4j-spring6` 의 `ReactorFallbackDecorator` 가 동기 예외를 `Flux.error` 로 변환해 살아남는 **비대칭 구조**. `Mono.defer` / `Flux.defer` 로 조립부를 지연 실행하거나 동일한 CircuitBreaker 적용 |
| `ChatServiceImpl.java:290`<br>(동일 패턴 `:408`) | diff-reviewer | 로직 결함 | medium | `doOnComplete` 가 `streamStarted` / `ragStreamStarted` 를 무시하고 무조건 `STREAM_COMPLETE` 를 전송한다. `ChatStateMachineConfig.java:82-98` 상 이 이벤트는 `STREAMING` 상태에서만 유효하다. `.filter(chunk -> chunk != null && !chunk.isEmpty())` 로 청크가 전부 걸러지면 `STREAM_START` 가 발화되지 않아 상태가 `PROCESSING` 에 머문다. 게다가 이 지점은 `sendEvent` 반환값을 검사하지 않아(호출부 `:248` 과 불일치) 거부가 조용히 삼켜진다. 프론트는 `emitComplete` 를 받지만 백엔드는 고착 |
| `ChatServiceImpl.java:292-296` | security-reviewer | 정보 노출 | medium | `emitError(sessionId, error.getMessage(), null)` 로 원본 예외 문자열이 SSE 에 그대로 실린다. 형제 메서드 `openerAnalysis`(`:410-421`)는 이미 BusinessException / 일반 예외를 분기해 `INTERNAL_SERVER_ERROR` 로 치환하고 있다. `doOnError` 는 `GlobalExceptionHandler` 를 우회하므로 여기서 직접 분기해야 한다 |
| `OpenAiLlmService.java:143`<br>`OpenAiLlmServiceWithoutRag.java:81` | convention-auditor | 컨벤션 위반 | medium | `BusinessException(ErrorCode.LLM_RESPONSE_FAIL)` 로 매핑하던 try/catch 가 리팩터링에서 제거됐다. 남은 `doOnError` 는 로깅만 하고 원본 예외(API 키 오류 등)가 변환 없이 하류로 흐른다 |
| `OpenAiLlmService.java:70` (`validateApiKeyBinding`)<br>`OpenAiLlmServiceWithoutRag.java:55` | security-reviewer | 시크릿 | medium | `@PostConstruct` 에서 `openaiApiKey.substring(0, 10)` 을 로깅한다. logstash-logback-encoder 가 JSON 로그를 수집·색인하므로 `application-dev.yml` 의 평문 키보다 노출면이 넓어진다 |
| `ChatController.java:67-74`, `:82-89`, `:112-119`<br>`ChatSendRequest`, `ChatSaveRequest` | security-reviewer | 입력 검증 | low | `@RequestBody` 에 `@Valid` 가 없어 DTO 의 `@NotBlank` 가 전혀 동작하지 않는다. 추가로 `Long` 타입 필드(`sessionId`, `questionId`)의 `@NotBlank` 는 의미가 없고(→ `@NotNull`), `message` 에 길이 상한이 없다(→ `@Size`) |
| `ChatServiceImpl.java:277` | diff-reviewer | 회귀 위험 | low | `.doOnError` 는 시그널을 소비하지 않고 흘려보내는데 뒤이은 `.subscribe()` 가 인자 없이 호출된다. Reactor 가 `Exceptions.errorCallbackNotImplemented` 로 `onErrorDropped` 경고를 추가 발생시켜 운영 로그에 같은 에러가 중복 적재된다. 기능상 문제는 없음(비즈니스 처리는 doOnError 에서 완료). `.subscribe(chunk -> {}, err -> {})` 형태로 빈 에러 컨슈머 전달 권장 |
| `RagServiceImpl.java:90` | convention-auditor | 컨벤션 위반 | low | 위 medium 항목과 같은 패턴이나, CircuitBreaker fallback(`generateSimilarProblemStreamFallback`)이 BusinessException 을 유지해 영향 범위가 좁다 |

---

## 핵심 원인 분석

상위 2건 중 예외 처리 결함은 **WebFlux 의 실행 시점** 에서 비롯된다.

1. `Flux` 를 반환하는 메서드의 본문 코드는 구독 시점이 아니라 **호출 즉시** 실행된다
2. `.doOnError` 는 조립이 끝난 파이프라인 내부의 예외만 포착한다
3. 조립 전에 던져진 예외는 호출자의 `try/catch` 로도, `doOnError` 로도 잡히지 않는다
4. 예외가 어디에도 잡히지 않으니 `emitError` 가 호출되지 않는다
5. 상태 머신이 `PROCESSING` 에 머물러 다음 요청이 `SESSION_EXPIRED` 로 거부된다

RAG 경로만 살아남은 이유는 설계 의도가 아니라 **AOP 의 부수효과** 다.
`@CircuitBreaker` 의 리액터 확장(`ReactorCircuitBreakerAspectExt`)이 `proceed()` 에서
발생한 동기 예외를 fallback 으로 라우팅하면서 `Flux.error()` 로 변환해준다.
서킷브레이커를 제거하는 순간 이 안전망이 사라진다.

---

## 리뷰 방법 메모

- `tokensave_callers` / `tokensave_implementations` 가 0건을 반환해 Grep 으로 교차검증했다.
  원인은 두 가지가 겹쳤다 — 인터페이스 타입 주입(기존에 알려진 함정),
  그리고 **인덱스가 `stage` 기준이라 현재 브랜치의 신규 심볼이 그래프에 없음**
- `resilience4j-spring6` 의 `ReactorFallbackDecorator` 존재는 로컬 gradle 캐시 jar 에서 실물 확인했다
- 테스트·Docker 는 사용하지 않았다. 위 결함은 전부 코드 정적 분석 결과이며 런타임 재현은 별도 작업이다
