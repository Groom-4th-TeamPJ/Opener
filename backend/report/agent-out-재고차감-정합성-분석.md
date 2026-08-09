# 재고 차감 정합성 분석 — 315건 실패의 원인 규명

대상 저장소: `/Users/mskim/Desktop/PJ/groom-shopping`
조사 범위: 동시 결제 재고 차감 경로 (읽기 전용, 코드 미수정)
작성일: 2026-07-30

---

## 0. 한 줄 결론

**315건 실패는 (a) 낙관적 락 재시도 3회 소진이 원인이며, 이것은 "추정"이 아니라 로그로 확정된다.**
다만 실패가 `confirm_conflict` 가 아니라 `confirm_other_fail` 로 집계된 이유는 계측이 없어서가 아니라,
`PaymentApplicationService.java:135` 의 `catch (Exception dbError)` 가 `BusinessException(PRODUCT_STOCK_CONFLICT)` 을
익명 `RuntimeException` 으로 **재포장하면서 에러 코드를 소실**시켜 HTTP 409 가 아닌 **HTTP 500** 으로 나갔기 때문이다.

즉 포트폴리오의 서술 "실패 사유를 분리 계측하지 않았으므로 원인은 추정" 은 **두 가지 점에서 사실과 다르다**:

1. k6 스크립트에는 실패 사유별 카운터가 **이미 분리되어 있었다** (`confirm_blocked` / `confirm_conflict` / `confirm_other_fail`).
2. 애플리케이션 로그에 원인이 **명시적으로 남아 있다** (`[STOCK_CONFLICT_GIVEUP]`).

---

## 1. 재고 차감 호출 경로 (전체 체인)

```
POST /api/v1/payment/confirm
  └─ PaymentController                                     interfaces/payment/PaymentController.java
     └─ PaymentApplicationService.confirmPayment()          application/payment/PaymentApplicationService.java:87
        │  @Transactional(propagation = NOT_SUPPORTED)   ← 비트랜잭션 실행
        │
        ├─[1]   멱등성 선조회 (paymentKey / orderId)                        :89-104
        ├─[1.5] preReserveStock(orderId)  ── Redis 선점 게이트              :113 → :153-174
        │         └─ ProductStockRedisRepository.preReserve()  Lua DECRBY   ProductStockRedisRepository.java:55
        │
        ├─[2]   tossPaymentClient.confirmPayment()  ── 외부 PG 승인          :120
        │         실패 시 → releaseStock() + markPaymentFailed() + throw     :123-129
        │
        └─[3]   applyApprovedPaymentInTx(orderId, response)                 :134
                 │  @Transactional(propagation = REQUIRES_NEW)              :190-191
                 │
                 └─ reduceProductStock(order)                               :464-502
                     └─ ProductStockService.decreaseWithOptimisticLock()    :475
                        │  @Retryable(maxAttempts=3)                        ProductStockService.java:40-46
                        └─ selfProvider.getObject().decreaseOnce()          ProductStockService.java:48
                           │  @Transactional(REQUIRES_NEW)                  ProductStockService.java:54
                           ├─ productRepository.findById()                  :56
                           ├─ product.decreaseStock(quantity)               :58
                           └─ productRepository.save()  → UPDATE ... WHERE id=? AND version=?  :60

                 실패 시 → [4] releaseStock() + compensate(환불) + markPaymentFailed() + throw  :135-148
```

핵심: **DB 재고 차감은 Toss 승인 이후에 일어난다.** 따라서 차감이 실패하면 이미 승인된 결제를 환불해야 한다.

---

## 2. 낙관적 락

| 항목 | 값 | 위치 |
|---|---|---|
| `@Version` 필드 | `private Long version;` | `interfaces/product/persistence/ProductJpaEntity.java:61-63` |
| 컬럼 | `@Column(name = "version")` | 같은 위치 |
| 충돌 예외 타입 | `org.springframework.orm.ObjectOptimisticLockingFailureException` | `ProductStockService.java:41` |

`save()` 시 `UPDATE ... WHERE id=? AND version=?` 가 나가고, 영향 행 수가 0이면 커밋 시점에 충돌 예외가 발생한다.

---

## 3. 재시도 설정 (`ProductStockService.java:40-45`)

```java
@Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2),
        listeners = "stockRetryListener"
)
```

| 항목 | 실제 값 |
|---|---|
| `maxAttempts` | **3** (최초 1회 + 재시도 2회) |
| `backoff` | `delay=100ms`, `multiplier=2` → 100ms, 200ms |
| **random 지터** | **없음** (`random = true` 미지정 → 고정 백오프) |
| `retryFor` | `ObjectOptimisticLockingFailureException` **단 하나** |
| `listeners` | `stockRetryListener` |

### `@Recover` — **존재한다**

```java
@Recover
public int recover(ObjectOptimisticLockingFailureException e, UUID productId, int quantity) {
    log.error("[STOCK_CONFLICT_GIVEUP] 재고 차감 재시도 실패 - productId={}, quantity={}", productId, quantity);
    throw new BusinessException(ErrorCode.PRODUCT_STOCK_CONFLICT);
}
```
`ProductStockService.java:68-72`

즉 "@Recover 없이 예외 전파" 는 **아니다**. `@Recover` 는 있고, 거기서 `PRODUCT_STOCK_CONFLICT` 로 변환한다.
문제는 **그 변환 결과가 상위에서 다시 뭉개진다**는 것이다 (→ 6절).

증가(복원) 경로에도 대칭으로 `@Retryable` + `@Recover recoverIncrease()` 가 있다 (`ProductStockService.java:80-109`).

---

## 4. 트랜잭션 경계와 프록시 중첩 순서

**결론: 순서는 올바르다. "재시도 바깥 / 트랜잭션 안쪽" 이 정확히 구현되어 있다.**

| 메서드 | 애노테이션 |
|---|---|
| `decreaseWithOptimisticLock()` | `@Retryable` **만** (트랜잭션 없음) — `ProductStockService.java:40-46` |
| `decreaseOnce()` | `@Transactional(REQUIRES_NEW)` **만** — `ProductStockService.java:54` |

두 애노테이션을 **같은 메서드에 겹치지 않고 두 메서드로 분리**했기 때문에 프록시 순서 문제가 원천적으로 발생하지 않는다.
낙관적 락 충돌은 **커밋 시점**에 터지므로, 재시도 프레임이 트랜잭션 바깥에 있어야 충돌을 잡을 수 있다. 이 구조가 그것을 만족한다.

### 자기호출(self-invocation) 우회

`decreaseWithOptimisticLock()` 이 `decreaseOnce()` 를 직접 부르면 프록시를 안 타서 `REQUIRES_NEW` 가 무시된다.
이를 `ObjectProvider<ProductStockService> selfProvider` 로 우회한다:

```java
return selfProvider.getObject().decreaseOnce(productId, quantity);   // ProductStockService.java:48
```

같은 패턴이 `PaymentApplicationService` 에도 적용되어 있다 (`:127`, `:134`, `:146`).
`@EnableRetry` 는 `BackendApplication.java:17` 에 선언되어 있다.

**따라서 "프록시 순서가 뒤집혀 같은 트랜잭션에서 재시도가 도는" 문제는 이 저장소에 없다.**

### 다만 주의할 부작용

`decreaseOnce()` 의 `REQUIRES_NEW` 는 바깥 `applyApprovedPaymentInTx()` 의 트랜잭션을 **일시 중단시키고 독립 커밋**한다.
그래서 `applyApprovedPaymentInTx` 주석(`:186-187`)의 "재고 차감 … 전부 롤백한다(All-or-Nothing)" 은 **재고에 대해서는 성립하지 않는다.**
코드도 이를 인지하고 `reduceProductStock()` 에 부분 차감 보상 로직을 따로 두고 있다 (`:467-501`).
즉 주석과 실제 동작이 어긋나 있다 (기능 결함은 아니고 문서 불일치).

---

## 5. Redis 선점 게이트

`ProductStockRedisRepository.java`

```lua
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then return -2 end          -- 미초기화 → DB 폴백
if stock < tonumber(ARGV[1]) then return -1 end  -- 재고 부족
return redis.call('DECRBY', KEYS[1], ARGV[1])    -- 선점 성공
```
`ProductStockRedisRepository.java:29-41`

- **원자성**: `GET → 검증 → DECRBY` 를 단일 Lua 로 묶어 음수 진입을 차단한다. 올바르다.
- **키**: `product:stock:{productId}` (`:26`)
- **선점 실패(-1) 시**: 이미 선점한 항목을 `releaseStock()` 로 되돌리고 `BusinessException(INSUFFICIENT_STOCK)` 즉시 throw → Toss 호출 안 함 (`PaymentApplicationService.java:165-170`)
- **미초기화(-2) 시**: 게이트를 건너뛰고 DB 낙관적 락이 최종 방어 (`:160-164`)

### TTL — **걸려 있지 않다**

```java
public void initStock(UUID productId, int quantity) {
    redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + productId, String.valueOf(quantity));
}
```
`ProductStockRedisRepository.java:46-48` — `set()` 에 만료 인자가 없다.

키는 부팅 시 `StockWarmUpRunner` (`infrastructure/product/StockWarmUpRunner.java`) 가 DB→Redis 로 복사하며 채운다.
TTL 이 없는 것은 이 설계에서는 **의도적으로 합리적**이다. 재고 키는 영속 상태이고, TTL 로 사라지면 `-2` 폴백으로 떨어져 게이트가 무력화되기 때문이다.
대신 **재기동 없이 Redis 가 유실되면 자동 복구 경로가 없다**는 것이 위험 요소다.

---

## 6. 선점분 회수(보상) — **존재한다**

`release()` 는 `INCR` 기반이다:
```java
public void release(UUID productId, int quantity) {
    redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + productId, quantity);
}
```
`ProductStockRedisRepository.java:64-66`

호출 지점 3곳 — **결제 실패 전 경로를 모두 덮는다**:

| 상황 | 위치 |
|---|---|
| Redis 선점 도중 재고 부족 → 앞서 선점분 롤백 | `PaymentApplicationService.java:167` |
| Toss 승인 실패 | `PaymentApplicationService.java:125` |
| **Toss 승인 성공 + DB 후처리 실패** | `PaymentApplicationService.java:137` |
| 결제 취소 시 DB·Redis 동시 복원 | `PaymentApplicationService.java:507-510` |

**→ (b) "선점분 미회수" 가설은 코드상 성립하지 않으며, 로그로도 반증된다 (9절).**
이것이 바로 실패 315건에도 DB·Redis 재고가 **둘 다 315로 일치**한 이유다.

---

## 7. 실패 계측 (Micrometer)

`infrastructure/product/StockRetryMetricsListener.java`

```java
@Component("stockRetryListener")
public class StockRetryMetricsListener implements RetryListener {
    private static final String SUMMARY_NAME = "stock_optimistic_retries";

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        String label = String.valueOf(context.getAttribute(RetryContext.NAME));
        String op = label.contains("increase") ? "increase" : "decrease";
        meterRegistry.summary(SUMMARY_NAME, "op", op).record(context.getRetryCount());
    }
}
```

- 지표: `stock_optimistic_retries` (DistributionSummary), 태그 `op = decrease | increase`
- 기록 값: **호출당 재시도 횟수** (`RetryContext.getRetryCount()`, 첫 시도 성공이면 0)
- 평균 재시도 = `stock_optimistic_retries_sum / stock_optimistic_retries_count`

### 계측의 공백 (중요)

이 리스너는 **재시도 횟수 분포만** 기록한다. 다음이 **분리되어 있지 않다**:

- 성공/포기 여부 (`close()` 의 `throwable` 파라미터를 받아놓고 쓰지 않는다 → 포기 건수를 지표로 셀 수 없다)
- 실패 사유별 카운터 (`PRODUCT_STOCK_CONFLICT` vs `INSUFFICIENT_STOCK` vs 기타)
- 결제 확정 실패 사유별 카운터 (서버 측에 없음)

즉 **서버 측 Micrometer 로는 315건의 사유를 나눌 수 없다**. 사유를 나눌 수 있는 근거는 (i) k6 커스텀 카운터와 (ii) 애플리케이션 로그 두 가지다.

---

## 8. 테스트 코드 — 두 개가 있으며, 1만 건 테스트는 **k6 쪽**이다

### 8-1. JUnit 통합 테스트 (1만 건 아님)

`backend/src/test/java/groom/backend/application/product/ProductStockConcurrencyIntegrationTest.java`

- **동시성 제어 방식**: `ExecutorService` (`Executors.newFixedThreadPool(16)`) + `CountDownLatch` + `AtomicInteger`
- **재고 초기값**: `100`  (`:58`)
- **스레드 수 / 반복 횟수**: `threadCount = 100`, 각 1건씩 차감 (`:59`, `:70`)
- **스레드풀 크기**: 16
- 대기: `latch.await(30, TimeUnit.SECONDS)`
- 검증: `finalStock == initialStock - success` (Lost Update 부재) + `finalStock >= 0`
- `@SpringBootTest` + `@AutoConfigureTestDatabase(replace = NONE)` → **실 PostgreSQL 필요**
- 클래스 레벨 `@Transactional` 없음 (각 스레드가 독립 커밋해야 하므로)

**이 테스트는 100건짜리다. 포트폴리오의 1만 건이 아니다.**

### 8-2. k6 부하 테스트 — **이것이 1만 건 테스트다**

`k6/scripts/stock-concurrency-test.js` (전문 147줄)

- **동시성 제어 방식**: k6 `executor: 'shared-iterations'` (ExecutorService/CountDownLatch 아님)
- **반복 횟수**: `ITERATIONS` 기본값 **10000** (`:26`) — 총 확정 시도 횟수
- **VU 수**: `VUS` 기본값 200 (`:25`) — 단, **실측 런은 10** (9절)
- **재고 초기값**: 스크립트가 아니라 사전 시드로 준비 (주석 `:14` — "재고 known 인 GENERAL 상품, Redis 재고 워밍업된 상태")
- `maxDuration: '10m'`
- 임계값: `confirm_latency: ['p(95)<300', 'p(99)<500']`

커스텀 카운터 (`:30-34`) — **실패 사유가 이미 분리되어 있다**:

| 카운터 | 의미 | 판정 조건 |
|---|---|---|
| `confirm_success` | 확정 성공 | `status === 200` |
| `confirm_blocked` | 선점 차단/품절 | 본문에 `INSUFFICIENT_STOCK` 포함 |
| `confirm_conflict` | 낙관적 락 최종 충돌 | 본문에 `PRODUCT_STOCK_CONFLICT` 포함 |
| `confirm_other_fail` | 기타 실패 | 그 외 전부 |

`confirm_other_fail` 은 **세 지점**에서 증가한다 — 이 점이 해석에 중요하다:
1. `:89` 로그인 실패
2. `:119` 주문 생성 5회 시도 모두 실패 (**confirm 을 아예 호출하지 않음**)
3. `:139` confirm 응답이 200 도 아니고 위 두 문자열도 없을 때

check (`:141`): `'confirm 처리됨(200/4xx)': (r) => r.status === 200 || (r.status >= 400 && r.status < 500)`
→ **5xx 이거나 연결 실패면 이 check 가 실패한다.** (원인 규명의 열쇠)

---

## 9. 실측 데이터 — 저장소 안에 원본이 남아 있다

**`backend/docs/measurements/stock_fixed.json`** 이 바로 포트폴리오가 인용한 그 런의 k6 요약이다.

```json
"confirm_success":    { "count": 9685, "rate": 75.775 },
"confirm_other_fail": { "count": 315,  "rate": 2.4646 },
"iterations":         { "count": 10000 },
"checks":             { "passes": 9685, "fails": 315, "value": 0.9685 },
"vus_max":            { "value": 10, "min": 10, "max": 10 },
"http_reqs":          { "count": 40562 },
"confirm_latency":    { "med": 29.43, "p(90)": 209.11, "p(95)": 352.84, "p(99)": 444.69, "max": 1127.21 },
"setup_data":         { "hotProductId": "550e8400-e29b-41d4-a716-446655440000" }
```

### 이 JSON 이 곧바로 확정해 주는 사실 4가지

**① `confirm_blocked` 와 `confirm_conflict` 는 JSON 에 아예 없다 = 둘 다 0건.**
k6 는 값이 기록된 지표만 요약에 넣는다. 두 카운터가 통째로 빠져 있다는 것은 한 번도 증가하지 않았다는 뜻이다.

**② 재고 소진이 아니다.** `confirm_blocked = 0` → `INSUFFICIENT_STOCK` 이 한 번도 안 났다.
초기 재고 10,000 에 9,685 성공 + 315 실패(선점 복원됨) → Redis 재고가 0에 도달한 적이 없다. 산술적으로 정합한다.
**포트폴리오의 "재고 소진이 아니다" 라는 서술은 옳다.**

**③ 315건은 전부 5xx 다.**
`confirm_success(9685) + confirm_other_fail(315) = 10000 = iterations` 이고,
`checks.passes + checks.fails = 10000` 이다.
주문 생성이 실패했다면 `:119` 에서 `return` 하므로 `:141` 의 check 에 도달하지 못해 check 총계가 10000 미만이어야 한다.
**check 총계가 정확히 10000 이므로 모든 반복이 confirm 을 호출했다** → 주문 생성 실패(테스트 타이밍 경합) 가설은 **배제**된다.
그리고 check 가 315회 실패했으므로 그 315건은 `200` 도 `4xx` 도 아니다 → **5xx**.

**④ 처리량 대비 지연**: p95 = 352.84ms 로 임계값 300ms 를 **초과**(threshold 위반), p99 = 444.69ms 는 500ms 이내로 통과.

### 그런데 왜 5xx 인가 — 여기가 핵심

`@Recover` 는 `BusinessException(PRODUCT_STOCK_CONFLICT)` 를 던진다. 이 코드의 HTTP 상태는:

```java
PRODUCT_STOCK_CONFLICT(HttpStatus.CONFLICT, "주문이 많아 처리에 실패했습니다. 잠시 후 다시 시도해주세요.");
```
`common/exception/ErrorCode.java:142` → **409 여야 한다.**

그러나 이 예외는 `applyApprovedPaymentInTx()` **안에서** 발생하므로, `PaymentApplicationService.java:135` 의 catch 에 걸린다:

```java
} catch (Exception dbError) {
    releaseStock(reservedItems);                       // :137  ← Redis 선점 복원 (정합성 유지의 이유)
    paymentCompensationService.compensate(...);        // :140  ← Toss 환불
    selfProvider.getObject().markPaymentFailed(...);   // :146
    throw new RuntimeException("결제 승인 후 처리 중 실패했습니다: " + dbError.getMessage(), dbError);  // :147
}
```

`RuntimeException` 은 `BusinessException` 이 아니므로 `@ExceptionHandler(BusinessException.class)` 를 타지 못하고
`@ExceptionHandler(Exception.class)` 로 떨어진다:

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("[UnhandledException]", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.from(ErrorCode.SERVER_ERROR));
}
```
`common/exception/GlobalExceptionHandler.java:52-57`

응답 본문은 `SERVER_ERROR` 로 **에러 코드 문자열이 전부 소실**된다.
게다가 `BusinessException.getMessage()` 는 코드명이 아니라 한글 메시지("주문이 많아 처리에 실패했습니다…")를 반환하므로
(`common/exception/BusinessException.java` 생성자 `super(errorCode.getMessage())`),
본문 어디에도 `"PRODUCT_STOCK_CONFLICT"` 문자열이 등장하지 않는다.

> **결론: k6 의 `confirm_conflict` 카운터는 confirm 경로에서 구조적으로 절대 증가할 수 없는 죽은 카운터였다.**
> 낙관적 락 충돌 포기는 반드시 `confirm_other_fail` + 500 으로 분류된다.

---

## 10. 로그에 의한 최종 확증

`logs/` 의 2026-06-07 부하테스트 로그 (paymentKey 형식 `lt_pk_{VU}_{ITER}_{ts}`, productId `550e8400-e29b-41d4-a716-446655440000` — 둘 다 k6 스크립트·`stock_fixed.json` 과 일치).

### 마커 총계 (2026-06-07 전체)

| 로그 마커 | 건수 |
|---|---|
| `[PAYMENT_CONFIRM_DB_FAILED]` | 2,871 |
| `[STOCK_REDUCE_PARTIAL_FAIL]` | 2,854 |
| `[STOCK_CONFLICT_GIVEUP]` | **2,852** |
| `[PAYMENT_COMPENSATION_START]` | 2,863 |
| `[PAYMENT_COMPENSATION_SUCCESS]` | **2,863** |
| `[STOCK_RESTORE_GIVEUP]` | **0** |
| `[STOCK_REDUCE_COMPENSATED]` | 0 |
| `[STOCK_RESERVE_OUT]` (재고 부족) | 38 |
| `[TOSS_API_ERROR]` | 206 (서킷브레이커 테스트 런) |

**`STOCK_CONFLICT_GIVEUP` 2,852 / `PAYMENT_CONFIRM_DB_FAILED` 2,871 = 99.3%.**
즉 DB 후처리 실패의 **99.3% 가 낙관적 락 재시도 소진**이다. 파일 단위로 보면 두 마커 건수가 대부분 **완전히 일치**한다
(예: `application-2026-06-07.10.log` 84 : 84, `.11.log` 80 : 80, `.12.log` 95 : 95, `.13.log` 98 : 98).

### 동일 스레드·동일 시각 페어링 (인과 직결 증거)

```
02:04:59.324 [http-nio-8080-exec-21] ERROR ProductStockService
    - [STOCK_CONFLICT_GIVEUP] 재고 차감 재시도 실패 - productId=550e8400-e29b-41d4-a716-446655440000, quantity=1

02:04:59.341 [http-nio-8080-exec-21] ERROR PaymentApplicationService
    - [PAYMENT_CONFIRM_DB_FAILED] OrderId: abb72800-..., PaymentKey: lt_pk_8_301_1780765498888,
      Error: 주문이 많아 처리에 실패했습니다. 잠시 후 다시 시도해주세요.
```

같은 워커 스레드에서 17ms 간격, 그리고 `PAYMENT_CONFIRM_DB_FAILED` 의 `Error:` 문자열이
`PRODUCT_STOCK_CONFLICT` 의 메시지와 **정확히 일치**한다. 인과가 확정된다.

### 런 구분 (분 단위 클러스터링)

| 시간대 | 차감 성공 `[STOCK_REDUCE]` | 포기 `[STOCK_CONFLICT_GIVEUP]` | 합계 |
|---|---|---|---|
| 02:03–02:06 | 8,982 | 1,019 | 10,001 |
| 17:48–17:51 | 8,675 | 1,326 | 10,001 |
| 17:55–17:56 | 9,786 | 214 | 10,000 |
| 17:58–18:00 | 9,051 | 293 | 9,344 |

각 런의 `성공 + 포기 ≈ 10,000` 으로 1만 건 시나리오임이 확인된다.
**9,685 / 315 조합의 런은 이 로그 파일들에 남아 있지 않다**(로그 로테이션으로 유실된 것으로 보인다).
다만 네 런 모두 실패 = 낙관적 락 포기라는 동일 패턴을 보이고, 실패율도 2.1%~13.3% 범위로 315건(3.15%)과 같은 대역이다.

### 정합성이 유지된 이유 (드리프트 0)

- `[STOCK_RESTORE_GIVEUP] 0` → Redis/DB 복원이 **한 번도 실패하지 않았다**
- `[PAYMENT_COMPENSATION_SUCCESS] 2,863 = START 2,863` → 환불 보상이 **전부 성공**
- `[STOCK_REDUCE_COMPENSATED] 0` → 주문당 상품 1종이라 부분 차감 상황 자체가 없었음
  (첫 상품에서 실패 → `decremented` 가 비어 있음 → 복원 대상 없음)

**→ (b) "선점분 미회수" 가설은 코드(6절)와 로그 양쪽에서 반증된다.**

---

## 11. 인프라 요구사항 & 재실행 방법

### 필요한 컨테이너

`docker-compose.dev.yml` (`/Users/mskim/Desktop/PJ/groom-shopping/docker-compose.dev.yml`)

| 서비스 | 이미지 | 포트 | 재고 테스트 필수 여부 |
|---|---|---|---|
| `db` | `postgres:15` (컨테이너 `dev-db`) | 5432 | **필수** |
| `redis` | `redis:latest` (컨테이너 `dev-redis`) | 6379 | **필수** (선점 게이트) |
| `zookeeper` | `confluentinc/cp-zookeeper:7.4.0` | — | 앱 기동에 필요 |
| `kafka` | `confluentinc/cp-kafka` | — | 앱 기동에 필요 |
| `frontend`, `nginx` | — | — | 불필요 |
| `loki`/`promtail`/`prometheus`/`grafana`/`*-exporter`/`cadvisor` | — | — | 지표 관측 시에만 |

`docker-compose.yml` (운영용) 은 `db / redis / zookeeper / kafka / backend / frontend / nginx` 로 모니터링 스택이 없다.

> 프로젝트 규칙에 따라 **컨테이너는 직접 기동하지 않았다.** 재현하려면 사용자가 먼저 띄워야 한다:
> `docker compose -f docker-compose.dev.yml up -d db redis zookeeper kafka`

추가 전제 (k6 스크립트 주석 `:12-15`):
- **WireMock healthy** 로 Toss API 대체 (`k6/wiremock/healthy/mappings/confirm.json`), `payment.toss.api-url` 오버라이드
- `HOT_PRODUCT_ID` 는 `AVAILABLE` 상태의 `GENERAL` 상품, Redis 재고 워밍업 완료 상태 (`StockWarmUpRunner` 가 부팅 시 수행)
- `VUS <= USERS` (VU 별 유저 고정으로 장바구니 경합 방지)

### 실행 명령

**JUnit 통합 테스트 (100건)**
```bash
cd /Users/mskim/Desktop/PJ/groom-shopping/backend
./gradlew test --tests 'groom.backend.application.product.ProductStockConcurrencyIntegrationTest'
```
(`backend/build.gradle:82-83` — `tasks.named('test') { useJUnitPlatform() }`)

**k6 부하 테스트 (1만 건)** — 스크립트 헤더 `:18-19` 의 예시 그대로
```bash
cd /Users/mskim/Desktop/PJ/groom-shopping
k6 run -e HOT_PRODUCT_ID=550e8400-e29b-41d4-a716-446655440000 \
       -e USERS=200 -e VUS=200 -e ITERATIONS=10000 \
       k6/scripts/stock-concurrency-test.js
```
단, 실측 런(`stock_fixed.json`)은 `vus_max = 10` 이었으므로 그 조건을 재현하려면 `-e USERS=10 -e VUS=10`.

정합성 검증은 런 종료 후 SQL 로: `초기재고 - confirm_success == 최종재고` (스크립트 `:10`, `:145`).

---

## 12. 최종 판정 — 315건 실패의 원인

### **(a) 재시도 3회 소진 — 확정. 단, "@Recover 없이" 는 틀림.**

- `@Recover` 는 **존재한다** (`ProductStockService.java:68-72`)
- 재시도 소진 → `@Recover` → `BusinessException(PRODUCT_STOCK_CONFLICT)` → **`PaymentApplicationService.java:147` 에서 `RuntimeException` 으로 재포장** → `GlobalExceptionHandler:52` → **HTTP 500 / 본문 `SERVER_ERROR`**
- k6 는 이를 `confirm_other_fail` 로 집계하고 check 도 실패시킨다 (5xx 이므로)
- 근거: `STOCK_CONFLICT_GIVEUP` 2,852 vs `PAYMENT_CONFIRM_DB_FAILED` 2,871 (**99.3% 일치**), 동일 스레드 페어링, 에러 메시지 문자열 일치

### **(b) Redis 선점 미회수 — 배제.**

`PaymentApplicationService.java:137` 에서 `releaseStock()` 이 DB 실패 경로를 덮고, `STOCK_RESTORE_GIVEUP = 0`.
이것이 DB·Redis 가 **둘 다 315** 로 일치한 직접적 이유다.

### **(c) 지터 없는 백오프(thundering herd) — 기여 요인이나 단독 원인은 아님.**

`@Backoff(delay=100, multiplier=2)` 에 `random` 이 없어 **모든 충돌 스레드가 정확히 100ms, 200ms 후 동시에 재진입**한다.
1만 건이 단일 상품(`550e8400-...`) 한 행에 집중되므로 재시도가 동일 타이밍에 정렬되어 충돌률을 높인다.
이것은 (a) 의 **발생 확률을 키운 증폭 요인**이지, 실패의 최종 형태는 (a) 다.

### **(d) 추가 발견 — 계측 자체의 결함 (가장 실무적으로 중요)**

1. **`confirm_conflict` 는 confirm 경로에서 절대 증가할 수 없는 죽은 카운터였다.** 예외 재포장이 에러 코드를 소실시키기 때문.
   → "실패 사유를 분리 계측하지 않았다" 가 아니라 **"분리 계측했으나 서버가 사유를 알려주지 않아 무력화됐다"** 가 정확한 서술이다.
2. **낙관적 락 충돌이 클라이언트에게 500(서버 오류)으로 보인다.** 실제로는 409(재시도 가능한 경합)이어야 한다.
   500 이면 클라이언트/게이트웨이가 재시도 대상으로 판단하지 않고, 알림·SLO 에서도 서버 장애로 오분류된다.
3. **가장 값비싼 부작용**: 315건 모두 **Toss 승인이 이미 끝난 뒤** 실패했다. 그래서 2,863건의 **환불 보상**이 실행됐다.
   즉 이 실패는 단순 거절이 아니라 "승인 후 환불" 이며, 실서비스라면 PG 수수료·정산·사용자 신뢰 비용이 발생한다.
4. `StockRetryMetricsListener` 가 `close()` 의 `throwable` 을 받고도 사용하지 않아 **포기 건수를 지표로 셀 수 없다** (로그 grep 에 의존하게 된 원인).

---

## 13. 1만 건 전부 성공시키면서 정합성을 유지하는 방안 (우선순위순)

> 아래는 **제안이며 코드는 수정하지 않았다.**

### 1순위 — 재고 차감을 원자적 조건부 UPDATE 로 교체 (근본 해결)

`decreaseOnce()` 의 `findById → decreaseStock → save` 를 단일 쿼리로 대체:
```sql
UPDATE product SET stock = stock - :qty, version = version + 1
WHERE id = :id AND stock >= :qty
```
영향 행 0 → 진짜 재고 부족(409/400), 1 → 성공. **읽고-쓰는 사이의 창이 사라져 낙관적 락 충돌 자체가 소멸**한다.

- **좋아지는 것**: 충돌·재시도가 원천 제거되어 315건이 0으로 수렴하고, p95 352ms → 대폭 감소. 초과판매도 DB 가 직접 막는다.
- **대가**: JPA 더티체킹/도메인 모델(`Product.decreaseStock()`)을 우회하는 네이티브 쿼리가 생기고, `@Version` 이 재고 외 필드 동시 수정을 감지하던 이점을 재고 경로에서는 잃는다.

### 2순위 — 예외 재포장 중단 (관측성 즉시 복구, 위험 거의 없음)

`PaymentApplicationService.java:147` 에서 `BusinessException` 은 **원본 그대로 재던지고**, 그 외만 `RuntimeException` 으로 감싼다.

- **좋아지는 것**: `PRODUCT_STOCK_CONFLICT` 가 409 로 정상 노출되어 `confirm_conflict` 카운터가 살아나고, 5xx 오분류가 사라진다. 클라이언트가 안전하게 재시도할 수 있다.
- **대가**: 없다시피 하다. 다만 4xx 로 바뀌면서 기존 5xx 기준 알림·대시보드 임계값을 함께 조정해야 한다.

### 3순위 — 백오프에 지터 추가 + 재시도 횟수 상향

`@Backoff(delay = 50, maxDelay = 500, multiplier = 2, random = true)`, `maxAttempts = 5~7`.

- **좋아지는 것**: 재시도가 시간축으로 흩어져 thundering herd 가 완화되고, 1순위를 적용하지 않아도 실패율이 눈에 띄게 떨어진다. 코드 변경이 한 줄이라 즉시 적용 가능하다.
- **대가**: 실패 확정까지 걸리는 시간이 길어져 p99 지연과 톰캣 스레드 점유가 증가한다. 부하가 더 높으면 스레드 고갈로 전이될 수 있다.

### 4순위 — 재고 차감을 Toss 승인 **이전**으로 이동

현재는 승인 후 차감이라 실패 시 환불이 필수다. Redis 선점처럼 DB 차감도 승인 전에 끝내고, Toss 실패 시 복원한다.

- **좋아지는 것**: "승인 후 환불" 2,863건이 사라진다. PG 수수료·정산 부담과 사용자 혼란이 제거된다.
- **대가**: 승인 전 차감분이 Toss 응답 지연 동안 묶여 실질 판매 가능 재고가 일시적으로 줄고, 복원 실패 시 재고가 과소 계상될 위험을 별도 관리해야 한다.

### 5순위 — 핫 상품에 한해 비관적 락 또는 분산 락

`SELECT ... FOR UPDATE` 또는 Redisson 분산 락을 상품 단위로 적용.

- **좋아지는 것**: 충돌이 0이 되고 동작이 결정적이라 추론과 검증이 쉽다.
- **대가**: 상품 단위 직렬화로 처리량 상한이 락 보유 시간에 묶이고, 커넥션 점유·데드락·락 타임아웃이라는 새로운 실패 모드가 생긴다. 1순위보다 대체로 열등하다.

### 6순위 — Redis 선점을 진실의 원천으로 두고 DB 반영을 비동기화

선점 성공 시 즉시 응답하고, DB 차감은 Outbox/Kafka 로 비동기 반영.

- **좋아지는 것**: 처리량과 지연이 최대로 개선되고 DB 경합이 사실상 사라진다.
- **대가**: 강한 일관성을 최종적 일관성으로 낮추게 되며, Redis 유실 시 복구 절차·정합성 감사 배치가 반드시 필요해진다. 현재 규모에는 과설계다.

### 부수 개선 (낮은 비용)

- `StockRetryMetricsListener.close()` 의 `throwable != null` 여부로 **포기 건수 카운터**(`stock_optimistic_giveup_total`)를 추가 → 로그 grep 없이 지표로 확인 가능. 대가: 지표 카디널리티 소폭 증가.
- 결제 확정 실패를 **사유 태그가 붙은 Micrometer 카운터**로 집계 (`payment_confirm_failed_total{reason=...}`) → 서버 단독으로 사유 분해 가능. 대가: 계측 코드가 비즈니스 로직에 섞인다.
- `applyApprovedPaymentInTx` 의 "All-or-Nothing" 주석(`:186-187`)을 실제 동작에 맞게 수정 — `REQUIRES_NEW` 차감은 독립 커밋되므로 전부 롤백되지 않는다.

---

## 부록 — 조사에 사용한 핵심 파일 경로

| 목적 | 절대 경로 |
|---|---|
| 재고 차감/재시도 | `/Users/mskim/Desktop/PJ/groom-shopping/backend/src/main/java/groom/backend/application/product/ProductStockService.java` |
| Redis 선점 게이트 | `/Users/mskim/Desktop/PJ/groom-shopping/backend/src/main/java/groom/backend/application/product/ProductStockRedisRepository.java` |
| 결제 오케스트레이션 | `/Users/mskim/Desktop/PJ/groom-shopping/backend/src/main/java/groom/backend/application/payment/PaymentApplicationService.java` |
| `@Version` 엔티티 | `/Users/mskim/Desktop/PJ/groom-shopping/backend/src/main/java/groom/backend/interfaces/product/persistence/ProductJpaEntity.java` |
| 예외 → HTTP 변환 | `/Users/mskim/Desktop/PJ/groom-shopping/backend/src/main/java/groom/backend/common/exception/GlobalExceptionHandler.java` |
| 에러 코드 정의 | `/Users/mskim/Desktop/PJ/groom-shopping/backend/src/main/java/groom/backend/common/exception/ErrorCode.java` |
| 재시도 지표 | `/Users/mskim/Desktop/PJ/groom-shopping/backend/src/main/java/groom/backend/infrastructure/product/StockRetryMetricsListener.java` |
| Redis 워밍업 | `/Users/mskim/Desktop/PJ/groom-shopping/backend/src/main/java/groom/backend/infrastructure/product/StockWarmUpRunner.java` |
| JUnit 동시성 테스트(100건) | `/Users/mskim/Desktop/PJ/groom-shopping/backend/src/test/java/groom/backend/application/product/ProductStockConcurrencyIntegrationTest.java` |
| k6 부하 테스트(1만건) | `/Users/mskim/Desktop/PJ/groom-shopping/k6/scripts/stock-concurrency-test.js` |
| **실측 원본 (9685/315)** | `/Users/mskim/Desktop/PJ/groom-shopping/backend/docs/measurements/stock_fixed.json` |
| 부하테스트 로그 | `/Users/mskim/Desktop/PJ/groom-shopping/logs/application-2026-06-07.*.log` |
| 인프라 | `/Users/mskim/Desktop/PJ/groom-shopping/docker-compose.dev.yml` |
