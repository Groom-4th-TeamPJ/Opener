# 선착순 쿠폰 발급 파이프라인 코드 분석

- 대상 저장소: `/Users/mskim/Desktop/PJ/groom-shopping`
- 조사일: 2026-07-30
- 범위: 읽기 전용 조사 (코드 수정 없음)
- 패키지 구조: `interfaces` → `application` → `domain` → `infrastructure` (DDD 계층형, 전통적 Controller/Service/Repository 평면 구조가 아님)

모든 경로는 `backend/src/main/java/groom/backend/` 기준으로 축약 표기한다.

---

## 1. 발급 진입점

**결론 한 줄**: 발급 경로는 **동기(Redisson 락 + Lua)와 비동기(Kafka) 두 갈래**가 공존하며, 두 경로 모두 `CouponIssueService` 로 수렴한다.

### 동기 경로

| 계층 | 클래스 | 파일 |
|---|---|---|
| interfaces | `CouponController` | `interfaces/coupon/CouponController.java` |
| application | `CouponIssueService` | `application/coupon/CouponIssueService.java` |
| application (Redis) | `CouponStockRedisRepository` | `application/coupon/CouponStockRedisRepository.java` |
| domain | `CouponRepository`, `CouponIssueRepository` | `domain/coupon/repository/` |

**근거**: `interfaces/coupon/CouponController.java:117-155`

```java
@PostMapping("/issue/{coupon_id}")
public ResponseEntity<CouponIssueResponse> issueCoupon(
        @AuthenticationPrincipal(expression = "user") User user,
        @RequestHeader("Request-Date") Instant clientInstant,
        @PathVariable("coupon_id") Long couponId) {
  Duration diff = Duration.between(clientInstant, Instant.now()).abs();
  if (diff.toMinutes() >= 1) { ... throw new BusinessException(ErrorCode.INVALID_PARAMETER, "잘못된 요청입니다."); }
  CouponIssueResponse response = couponIssueService.issueCoupon(couponId, user);
```

전체 호출 경로:

```
POST /v1/coupon/issue/{coupon_id}
  → CouponController.issueCoupon                       (CouponController.java:118)
  → CouponIssueService.issueCoupon                     (CouponIssueService.java:105)
  → CouponStockRedisRepository.tryIssue                (CouponStockRedisRepository.java:74)  [Lua]
  → CouponIssueService.persistIssuedCoupon             (CouponIssueService.java:161)         [DB]
  → CouponIssueRepository.save / CouponRepository.save (CouponIssueService.java:171,177)
```

**부가 사항**: 컨트롤러 진입 조건으로 `Request-Date` 헤더와 서버 시각의 차이가 1분 이상이면 발급을 거부한다 (`CouponController.java:141-144`). 클래스 레벨 `@CheckPermission(roles = {"USER","ADMIN"}, ...)` 로 인가를 건다 (`CouponController.java:52`).

### 비동기 경로

```
POST /v1/coupon/issue-async/{coupon_id}
  → CouponController.issueCouponAsync                  (CouponController.java:170)
  → CouponAsyncIssueService.enqueue                    (CouponAsyncIssueService.java:52)
  → Kafka topic "coupon-issue-requests"
  → CouponIssueRequestConsumer.consume                 (infrastructure/kafka/CouponIssueRequestConsumer.java:26)
  → CouponAsyncIssueService.process                    (CouponAsyncIssueService.java:70)
  → CouponIssueService.issueCouponInDbOnly             (CouponIssueService.java:194)         [DB 비관적 락]
```

관리자용 CRUD 는 별도 컨트롤러다: `CouponCommonController` (`POST/PUT/DELETE /v1/coupon`, 파일 라인 38·79·102) → `CouponCommonService`.

---

## 2. 분산 락

**결론 한 줄**: `CouponIssueService.issueCoupon` 한 메서드 안에서 잡고 `finally` 에서 풀며, **락 구간 안에 Lua 호출뿐 아니라 DB 트랜잭션(`persistIssuedCoupon`)까지 통째로 들어 있다.**

- 락 키 포맷: `coupon:lock:{couponId}` (`CouponIssueService.java:68` 의 `COUPON_LOCK_KEY_PREFIX = "coupon:lock:"` + couponId)
- `waitTime = 3초`, `leaseTime = 5초` (`CouponIssueService.java:69-70`)

**근거**: `CouponIssueService.java:107-151`

```java
RLock lock = redissonClient.getLock(COUPON_LOCK_KEY_PREFIX + couponId);
boolean acquired = false;
try {
    acquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
    if (!acquired) { ... throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK); }
    CouponStockRedisRepository.IssueResult issueResult =
            couponStockRedisRepository.tryIssue(couponId, user.getId());   // ← Lua
    ...
    try {
        return selfProvider.getObject().persistIssuedCoupon(couponId, user);  // ← DB 쓰기 트랜잭션
    } catch (RuntimeException dbError) { ... }
} finally {
    if (acquired && lock.isHeldByCurrentThread()) { lock.unlock(); }
}
```

**락 구간에 포함되는 것 (중요)**:

1. `tryIssue` Lua 호출 (`:119-120`)
2. **`persistIssuedCoupon` 의 DB 트랜잭션 전체** — 쿠폰 조회 + `decreaseQuantity` + `CouponIssue` insert + `Coupon` update (`:137`)
3. `NOT_INITIALIZED` 폴백 시 **`issueCouponInDbOnly` 의 DB 비관적 락 트랜잭션까지** (`:129`)
4. 실패 시 `rollbackIssue` 의 Redis 왕복 2회 (`:140`)

**유의점 2건**:

- `CouponIssueService.java:95` 주석은 "`findByIdForUpdate` (JPA 비관적 락) 방식 대비 DB 커넥션 점유 시간이 사라져"라고 서술하지만, 실제로는 락 구간 안에서 DB 커넥션을 잡는다. `NOT_SUPPORTED` 가 없앤 것은 *바깥 메서드의* 트랜잭션이지, 안쪽 `REQUIRES_NEW` 의 커넥션 점유가 아니다. 주석과 코드가 어긋난다.
- `leaseTime = 5초`인데 그 안에서 DB 트랜잭션이 돈다. DB 지연으로 5초를 넘기면 **락이 자동 해제된 채 임계 구역이 계속 실행**되어 두 스레드가 동시에 들어갈 수 있다. 다만 Lua 의 `SISMEMBER` 중복 체크와 `DECR` 가 원자적이라 재고 초과 자체는 Redis 선에서 방어된다.

---

## 3. Lua 스크립트

**결론 한 줄**: `CouponStockRedisRepository` 에 Java 텍스트 블록 상수로 인라인돼 있으며(`.lua` 파일은 저장소 전체에 없음), **재고 확인 → 중복 확인 → 차감 + 발급자 등록**을 하나의 원자 연산으로 묶는다.

**근거**: `application/coupon/CouponStockRedisRepository.java:34-51` (원문 그대로)

```lua
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then
    return -2
end
if stock <= 0 then
    return 0
end
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -1
end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 1
```

**KEYS/ARGV 구성** (`CouponStockRedisRepository.java:75-79`):

| 슬롯 | 값 | 키 상수 |
|---|---|---|
| `KEYS[1]` | `coupon:stock:{couponId}` — 남은 재고 (INT) | `STOCK_KEY_PREFIX` (`:28`) |
| `KEYS[2]` | `coupon:issued_users:{couponId}` — 발급받은 userId SET | `ISSUED_USERS_KEY_PREFIX` (`:29`) |
| `ARGV[1]` | `userId` (문자열) | — |

**반환 코드 의미** (`CouponStockRedisRepository.java:83-88`):

| 코드 | 의미 | 매핑 enum |
|---|---|---|
| `1` | 발급 성공 (DECR + SADD 완료) | `SUCCESS` |
| `0` | 재고 소진 | `OUT_OF_STOCK` |
| `-1` | 이미 발급받은 사용자 | `ALREADY_ISSUED` |
| `-2` | 재고 키 자체가 없음(미초기화) | `NOT_INITIALIZED` (`default` 분기로 흡수) |

```java
Long result = redisTemplate.execute(ISSUE_SCRIPT, keys, userId.toString());
if (result == null) { return IssueResult.NOT_INITIALIZED; }
return switch (result.intValue()) {
    case 1 -> IssueResult.SUCCESS;
    case 0 -> IssueResult.OUT_OF_STOCK;
    case -1 -> IssueResult.ALREADY_ISSUED;
    default -> IssueResult.NOT_INITIALIZED;
};
```

`-2` 는 명시적 `case` 가 아니라 `default` 로 떨어진다. 결과는 같지만 의도치 않은 다른 반환값도 전부 `NOT_INITIALIZED` 로 뭉뚱그려진다.

**원자로 묶이는 연산**: 재고 조회(GET) + 존재 검증 + 소진 검증 + 중복 검증(SISMEMBER) + 차감(DECR) + 발급자 등록(SADD). 즉 **락이 없어도 Redis 레벨에서는 초과발급이 방어되는 구조**다.

---

## 4. 트랜잭션 경계

**결론 한 줄**: 클래스 기본값이 `readOnly = true` 이고, 발급 진입 메서드는 `NOT_SUPPORTED`, 실제 DB 쓰기 메서드 2개는 `REQUIRES_NEW` 다.

| 메서드 | 어노테이션 | 라인 |
|---|---|---|
| `CouponIssueService` (클래스) | `@Transactional(readOnly = true)` | `:53` |
| `issueCoupon` | `@Transactional(propagation = Propagation.NOT_SUPPORTED)` | `:104` |
| `persistIssuedCoupon` | `@Transactional(propagation = Propagation.REQUIRES_NEW)` | `:160` |
| `issueCouponInDbOnly` | `@Transactional(propagation = Propagation.REQUIRES_NEW)` | `:193` |
| `useCoupon` | `@Transactional` (기본 REQUIRED, 쓰기) | `:392` |
| `CouponCommonService` (클래스) | `@Transactional(readOnly = true)` | `CouponCommonService.java:25` |

**근거**: `CouponIssueService.java:103-105`, `:160-161`, `:193-194`

```java
@CacheEvict(cacheNames = COUPON_LIST_CACHE_NAME, key = "#user.id")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public CouponIssueResponse issueCoupon(Long couponId, User user) {
```

**자기호출(self-invocation) 우회**: `issueCoupon` 이 같은 빈의 `@Transactional` 메서드를 직접 부르면 프록시를 안 타서 트랜잭션이 무효화된다. 이를 `ObjectProvider<CouponIssueService> selfProvider` 로 우회한다.

```java
private final ObjectProvider<CouponIssueService> selfProvider;   // :83
...
return selfProvider.getObject().issueCouponInDbOnly(couponId, user);   // :129
return selfProvider.getObject().persistIssuedCoupon(couponId, user);   // :137
```

비동기 경로의 `CouponAsyncIssueService.process` 는 **주입받은 다른 빈**을 통해 호출하므로(`CouponAsyncIssueService.java:76`) 이미 프록시를 탄다 — `REQUIRES_NEW` 가 정상 적용된다.

---

## 5. 미초기화 폴백 경로

**결론 한 줄**: 존재한다. `CouponIssueService.issueCouponInDbOnly` 가 DB 비관적 락(`SELECT ... FOR UPDATE`) 폴백이다.

**근거 — 분기**: `CouponIssueService.java:125-130`

```java
case NOT_INITIALIZED -> {
    log.warn("[COUPON_STOCK_FALLBACK] Redis stock not initialized, falling back to DB. couponId={}", couponId);
    return selfProvider.getObject().issueCouponInDbOnly(couponId, user);
}
```

**근거 — 폴백 본문**: `CouponIssueService.java:193-219`

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public CouponIssueResponse issueCouponInDbOnly(Long couponId, User user) {
    Coupon coupon = couponRepository.findByIdForUpdate(couponId).orElseThrow(...);
    if (!coupon.getIsActive()) { throw ... COUPON_NOT_FOUND; }
    if (coupon.getQuantity() <= 0) { throw ... COUPON_OUT_OF_STOCK; }
    if (!couponIssueRepository.findByCouponIdAndUserId(couponId, user.getId()).isEmpty()) {
        throw ... COUPON_ALREADY_ISSUED; }
    coupon.decreaseQuantity();
    ...
    couponStockRedisRepository.initStock(couponId, coupon.getQuantity());  // :219
```

**비관적 락 정의**: `domain/coupon/repository/CouponRepository.java:23-25`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Coupon c WHERE c.id = :couponId")
Optional<Coupon> findByIdForUpdate(@Param("couponId") Long couponId);
```

**중요 — 재고 워밍업이 없다**: `initStock` 의 프로덕션 호출자는 `CouponIssueService.java:219` **한 곳뿐**이다. `CouponCommonService.createCoupon` (`CouponCommonService.java:32-55`) 은 `initStock` 을 부르지 않고, 상품 쪽에 있는 `StockWarmUpRunner` (`infrastructure/product/StockWarmUpRunner.java:46`) 는 `ProductStockRedisRepository` 만 워밍업하며 쿠폰은 다루지 않는다.

→ 결과적으로 **쿠폰 생성 직후 첫 발급 요청은 항상 `NOT_INITIALIZED` 로 DB 폴백을 탄다.** 트래픽이 몰린 상태로 이벤트를 시작하면 Redis 키가 채워지기 전까지 다수 요청이 동시에 비관적 락 경로로 몰릴 수 있다(락 대기는 `coupon:lock:{id}` 가 직렬화하지만, 그만큼 3초 `waitTime` 초과로 `COUPON_OUT_OF_STOCK` 오탐이 나올 수 있음).

---

## 6. 보상 로직 (Redis 롤백)

**결론 한 줄**: 존재한다. DB 저장 실패 시 `INCR` + `SREM` 으로 되돌린다.

**근거 — 호출부**: `CouponIssueService.java:136-142`

```java
try {
    return selfProvider.getObject().persistIssuedCoupon(couponId, user);
} catch (RuntimeException dbError) {
    // DB 저장 실패 → Redis 재고/발급자 SET 롤백으로 상태 일치 유지
    couponStockRedisRepository.rollbackIssue(couponId, user.getId());
    throw dbError;
}
```

**근거 — 구현부**: `CouponStockRedisRepository.java:94-97`

```java
public void rollbackIssue(Long couponId, Long userId) {
    redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + couponId);
    redisTemplate.opsForSet().remove(ISSUED_USERS_KEY_PREFIX + couponId, userId.toString());
}
```

**한계**: 두 명령이 Lua 로 묶여 있지 않아 원자적이지 않다(발급 경로와 달리 파이프라인/스크립트 미사용). 또한 `catch (RuntimeException)` 이므로 `Error` 계열이나 롤백 자체가 실패하면 Redis 재고가 실제보다 적게 남는 방향으로 어긋난다(과소 발급 쪽이라 초과발급보다는 안전한 방향).

---

## 7. ZSet 사용 여부

**결론 한 줄**: 사용한다. **대기 순번(waiting position) 표시 용도**이며 랭킹이 아니다. 비동기 발급 경로에서만 쓰인다.

**근거**: `application/coupon/CouponQueueRedisRepository.java:27-59`

```java
private static final String SEQ_KEY_PREFIX = "coupon:seq:";
private static final String QUEUE_KEY_PREFIX = "coupon:queue:";
private static final Duration KEY_TTL = Duration.ofHours(2);

public void enqueue(Long couponId, Long userId) {
    Long seq = redisTemplate.opsForValue().increment(seqKey);
    redisTemplate.opsForZSet().addIfAbsent(queueKey, userId.toString(), seq != null ? seq : 0);
    redisTemplate.expire(seqKey, KEY_TTL);
    redisTemplate.expire(queueKey, KEY_TTL);
}
```

| 키 | 자료구조 | 내용 |
|---|---|---|
| `coupon:seq:{couponId}` | String (INCR 카운터) | 접수 순번 발급기. 타임스탬프 대신 원자적 카운터를 score 로 씀 (동일 ms 충돌 방지) |
| `coupon:queue:{couponId}` | ZSet | `member = userId`, `score = seq`. 대기열 |

**쓰는 쪽**: `CouponAsyncIssueService.enqueue` (`:58`) 가 등록, `process` 의 `finally` 에서 `remove` (`:91`).
**읽는 쪽**: `CouponAsyncIssueService.getPosition` (`:99-103`) → `rank`(내 앞 대기 인원, 0-based) + `size`(전체 대기 인원). 컨트롤러 `GET /v1/coupon/issue-async/{coupon_id}/position` (`CouponController.java:189-199`) 및 `enqueue` 응답(`:176-182`)이 노출.
**정리**: TTL 2시간 idle 만료 + `CouponDelayConsumer.java:64` 에서 쿠폰 비활성화 시 `clear(couponId)` 즉시 삭제.

`addIfAbsent`(ZADD NX)라 같은 사용자가 재요청해도 순번이 뒤로 밀리지 않는다. 코드 주석(`CouponQueueRedisRepository.java:14`)이 명시하듯 실제 처리 순서는 Kafka 파티션이 결정하므로 **보장 순번이 아니라 추정 순번**이다.

---

## 8. 비동기 발급 경로

**결론 한 줄**: 구현돼 있다. **Kafka** 기반이며(RabbitMQ 아님), 큐 이름은 `coupon-issue-requests` / `coupon-issue-results` 다.

**근거 — 프로듀서**: `application/coupon/CouponAsyncIssueService.java:30-33, 52-65`

```java
private static final String REQUEST_TOPIC = "coupon-issue-requests";
private static final String RESULT_TOPIC = "coupon-issue-results";
private static final String STATUS_KEY_PREFIX = "coupon:issue:status:";
private static final Duration STATUS_TTL = Duration.ofHours(1);
...
public String enqueue(Long couponId, Long userId) {
    String requestId = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(STATUS_KEY_PREFIX + requestId, STATUS_WAITING, STATUS_TTL);
    couponQueueRedisRepository.enqueue(couponId, userId);
    send(REQUEST_TOPIC, couponId.toString(), event);   // key = couponId → 동일 파티션
```

**근거 — 컨슈머**: `infrastructure/kafka/CouponIssueRequestConsumer.java:22-34`

```java
@KafkaListener(
        topics = "coupon-issue-requests",
        groupId = "coupon-issue-group",
        containerFactory = "paymentEventKafkaListenerContainerFactory")
public void consume(ConsumerRecord<String, String> record) {
    try {
        CouponIssueRequestEvent event = objectMapper.readValue(record.value(), CouponIssueRequestEvent.class);
        couponAsyncIssueService.process(event);
    } catch (Exception e) {
        log.error("[COUPON_ASYNC_CONSUME_ERROR] payload={}, error={}", record.value(), e.getMessage());
    }
}
```

**결과 컨슈머**: `infrastructure/kafka/CouponIssueResultConsumer.java` (별도 존재).

**핵심 설계**: 메시지 key 를 `couponId` 로 지정 → 같은 쿠폰은 같은 파티션 → 단일 컨슈머가 직렬 처리 → **분산 락 없이 동시성 해소**. 그래서 `process` 는 락 경로가 아닌 DB 경로를 직접 부른다.

**근거 — 처리부**: `CouponAsyncIssueService.java:70-93`

```java
public void process(CouponIssueRequestEvent event) {
    try {
        User user = userRepository.findById(event.userId()).orElseThrow(...);
        couponIssueService.issueCouponInDbOnly(event.couponId(), user);   // ← Redis 재고 경로를 안 탐
        redisTemplate.opsForValue().set(statusKey, STATUS_SUCCESS, STATUS_TTL);
        send(RESULT_TOPIC, ...);
    } catch (BusinessException e) {
        redisTemplate.opsForValue().set(statusKey, STATUS_FAILED_PREFIX + reason, STATUS_TTL);
        ...
    } finally {
        couponQueueRedisRepository.remove(event.couponId(), event.userId());
    }
}
```

**동시성 전제 확인**: `infrastructure/config/KafkaConfig.java:270-276` 의 `paymentEventKafkaListenerContainerFactory` 에는 `setConcurrency(...)` 호출이 **없다**. 따라서 Spring Kafka 기본값 1이 적용되어 컨테이너당 단일 컨슈머 스레드로 동작한다 — 직렬 처리 전제는 성립한다. 다만 애플리케이션을 여러 인스턴스로 스케일아웃하면 파티션이 인스턴스별로 배분되므로, 파티션 수가 1보다 크면 같은 쿠폰이라도 key 해싱이 한 파티션에 고정되는 것에 의존한다(동일 key → 동일 파티션이므로 여전히 직렬).

**상태 조회**: `GET /v1/coupon/issue-async/{request_id}/status` (`CouponController.java:205-211`) → `coupon:issue:status:{requestId}` 를 polling. 값은 `WAITING` / `SUCCESS` / `FAILED:{ErrorCode}` / `UNKNOWN`(TTL 1시간 만료 시).

**한계**: 컨슈머가 예외를 `catch` 후 로깅만 하고 삼킨다(`CouponIssueRequestConsumer.java:30-33`). DLQ 나 재시도가 없어 역직렬화 실패 메시지는 유실되고, 해당 `requestId` 는 영원히 `WAITING` 으로 남는다. (결제 쪽에는 `PaymentCompensationDlqConsumer` 가 있지만 쿠폰 경로에는 없음.)

---

## 9. 결제 시 쿠폰 사용 / 재고 선점

**결론 한 줄**: **결제 계층에는 쿠폰 참조가 전혀 없다.** 할인은 *주문 생성* 시점에 계산될 뿐이고, **쿠폰을 소진 처리하는 `useCoupon` 은 프로덕션 코드에서 아무도 호출하지 않는다 — 같은 쿠폰 중복 사용을 막는 장치가 없다.**

**근거 — 결제 계층 검색 결과**: `application/payment`, `domain/payment`, `interfaces/payment` 세 패키지 전체에 대소문자 무시 `coupon` 검색 시 **매칭 0건**.

**근거 — 할인 적용은 주문 생성 시점**: `application/order/OrderApplicationService.java:121-127`

```java
// 쿠폰 할인 적용 (쿠폰이 있는 경우)
if (couponId != null) {
    Integer discountAmount = couponIssueService.calculateDiscount(couponId, userId, order.getSubTotal());
    System.out.println("discountAmount : " + discountAmount);
    order.setDiscountAmount(discountAmount);
    log.info("Coupon applied - couponId: {}, discountAmount: {}", couponId, discountAmount);
}
```

**근거 — `useCoupon` 호출자 부재**: 저장소 전체(`backend/src`) 검색 결과, `useCoupon` 의 호출자는 **테스트 파일 단 하나뿐**이다.

```
./main/java/groom/backend/application/coupon/CouponIssueService.java:393:  public Boolean useCoupon(...)   ← 정의
./test/java/groom/backend/application/coupon/CouponIssueServiceIntegrationTest.java:340,490,510,546,562,565,599  ← 테스트만
```

`CouponController` / `CouponCommonController` 어디에도 쿠폰 사용 확정 엔드포인트가 없다 (`CouponCommonController` 매핑은 `@PostMapping`, `@PutMapping("/{coupon_id}")`, `@DeleteMapping("/{coupon_id}")` 3개뿐).

**검증은 하지만 소진은 안 한다**: `calculateDiscount` 는 `checkCouponUsable(couponDto, userId)` 로 소유자·활성 상태·만료일을 검증한다(`CouponIssueService.java:341-356`, 검증 본문은 `:431-444`). 하지만 어느 경로에서도 `isActive = false` 로 바꾸지 않는다.

**귀결 (오버셀링 갭)**: 발급 단계의 1인 1매 제약은 Lua `SISMEMBER`(`CouponStockRedisRepository.java:43`)와 DB 폴백의 `findByCouponIdAndUserId`(`CouponIssueService.java:204`)로 이중 방어된다. 그러나 **사용 단계에는 방어가 없다** — 발급받은 쿠폰 1장으로 주문을 반복 생성하면 매번 할인이 적용된다. 재고 선점(hold/reserve) 개념도 코드에 없다.

**참고**: `useCoupon` 자체도 낙관/비관 락 없는 read-modify-write 라(`:395-404`), 호출자가 생기더라도 동시 요청 시 중복 사용 여지가 있다.

---

## 10. 샤딩 흔적

**결론 한 줄**: **없음.**

**근거**: `backend/src/main/java` 전체에 대소문자 무시 `shard` 검색 결과 **매칭 0건**. 재고 키는 `coupon:stock:{couponId}` 단일 키이며(`CouponStockRedisRepository.java:28`), 쿠폰 하나당 하나의 문자열 카운터를 `DECR` 로 차감하는 구조다. 재고를 N개 키로 쪼개거나, 요청을 키별로 분산시키는 로직은 코드에 없다.

락 키 역시 `coupon:lock:{couponId}` 로 쿠폰 단위까지만 분리된다(`:68`). 즉 **동일 쿠폰에 대한 모든 요청은 단일 락 + 단일 Redis 키로 직렬화**된다 — 이것이 현재 구조의 처리량 상한이다.

---

## 11. 부하 테스트

**결론 한 줄**: 존재한다. k6 스크립트가 `k6/scripts/` 에 6종 있고, 쿠폰 전용은 `coupon-issue-test.js` 다.

**경로**: `/Users/mskim/Desktop/PJ/groom-shopping/k6/scripts/`

| 파일 | 대상 |
|---|---|
| `coupon-issue-test.js` | **선착순 쿠폰 발급 동시성** |
| `stock-concurrency-test.js` | 상품 재고 동시성 |
| `order-test.js` | 주문 |
| `product-test.js` | 상품 |
| `payment` 관련 `circuit-breaker-test.js` | 서킷 브레이커 |
| `integrated-test.js` | 통합 (쿠폰 참조 없음 — 검색 0건) |

**시나리오 요약** (`k6/scripts/coupon-issue-test.js`):

```js
export const options = {
  scenarios: {
    coupon_rush: {
      executor: 'shared-iterations',
      vus: VUS,              // 기본 200
      iterations: ITERATIONS, // 기본 5000
      maxDuration: '10m',
    },
  },
  thresholds: {
    issue_latency: ['p(95)<200', 'p(99)<300'],
  },
};
```

- 한 쿠폰(`COUPON_ID`)에 기본 200 VU / 5000 iteration 을 `shared-iterations` 로 몰아 `POST /coupon/issue/{id}` 를 때린다.
- 부하 유저 `lt_1..N@test.com` (비번 `loadtest123!`)을 `setup()` 에서 signup 으로 보강하고, VU 별로 로그인 토큰을 캐싱한다(`:52-77`).
- `Request-Date` 헤더를 RFC1123 UTC 로 매 요청 세팅해 컨트롤러의 ±1분 시각 검증을 통과시킨다(`:82`).
- 응답을 4개 카운터로 분류: `issue_success`(201) / `issue_soldout` / `issue_dup` / `issue_other`. **분류는 HTTP 상태가 아니라 응답 본문 문자열 매칭**(`'SOLD_OUT'`, `'소진'`, `'이미'` 등)에 의존한다(`:88-96`) — 메시지 문구가 바뀌면 집계가 깨진다.
- 초과발급 검증은 스크립트가 아니라 **런북 SQL 로 수동 확인**한다(`:100`: `coupon_issue 행 수 <= quantity`).

**스크립트 자체가 남긴 주의**(`:10-11`): A/B 비교의 "before"(비관적 락) 경로가 코드에서 제거되어 이 스크립트는 "after" 절대값 측정용이다. `350ms→85ms` 비교를 내려면 비관적 락 경로를 feature flag 로 복원해 재측정해야 한다고 명시. 임계값 주석(`:44`)도 `p99 85ms` 를 "이력서 가정치(참고), 실측 후 갱신"이라고 적어 두었다 — **현재 저장소에 실측 결과 파일은 없다** (`--summary-export` 대상인 `backend/docs/measurements/coupon.json` 미존재).

---

## 현재 파이프라인 구조도

### A. 동기 발급 (기본 경로)

- **요청** — `POST /v1/coupon/issue/{coupon_id}` + `Authorization: Bearer`, `Request-Date` 헤더
- → `@CheckPermission` AOP 인가 검사 (USER 또는 ADMIN)
- → `CouponController.issueCoupon` — 서버/클라 시각 오차 1분 이상이면 `INVALID_PARAMETER` 로 거부
- → `CouponIssueService.issueCoupon` — `@Transactional(NOT_SUPPORTED)` 로 바깥 트랜잭션 없이 진입
- → **`RLock("coupon:lock:{couponId}").tryLock(3s, 5s)`** — 실패 시 `COUPON_OUT_OF_STOCK` 로 빠른 실패
  - → `CouponStockRedisRepository.tryIssue` — **Lua 원자 실행**: `GET stock` → 존재/소진 검사 → `SISMEMBER` 중복 검사 → `DECR` + `SADD`
    - `0`(품절) → `COUPON_OUT_OF_STOCK` 예외
    - `-1`(중복) → `COUPON_ALREADY_ISSUED` 예외
    - `-2`(미초기화) → **폴백 분기** (아래 C)
    - `1`(성공) → 계속
  - → `persistIssuedCoupon` — `@Transactional(REQUIRES_NEW)`, `selfProvider` 로 프록시 경유
    - `Coupon` 조회 → `isActive` 검사 → `decreaseQuantity()` → `CouponIssue` insert → `Coupon` update
    - → 단건 캐시(`coupon-item-cache`)에 응답 DTO put
  - → DB 실패 시 `rollbackIssue` — Redis `INCR stock` + `SREM issued_users` 후 예외 재던짐
- → `finally` 에서 `lock.unlock()` (보유 중일 때만)
- → `@CacheEvict` 로 해당 사용자의 `user-coupons-list-cache` 무효화
- **응답** — `201 Created` + `CouponIssueResponse`

### B. 비동기 발급 (Strangler 추가 경로)

- **요청** — `POST /v1/coupon/issue-async/{coupon_id}`
- → `CouponAsyncIssueService.enqueue`
  - `requestId = UUID` 생성 → Redis `coupon:issue:status:{requestId} = WAITING` (TTL 1h)
  - → `CouponQueueRedisRepository.enqueue` — `INCR coupon:seq:{id}` 로 순번 획득 → `ZADD NX coupon:queue:{id} {seq} {userId}` → 두 키 TTL 2h 갱신
  - → Kafka `coupon-issue-requests` 발행 (**key = couponId** → 동일 파티션 → 직렬 처리)
- **즉시 응답** — `202 Accepted` + `{requestId, status: WAITING, position, waiting}`
- → *(비동기)* `CouponIssueRequestConsumer` (groupId `coupon-issue-group`, concurrency 기본 1)
  - → `CouponAsyncIssueService.process` → **`issueCouponInDbOnly`** (Redis 재고 경로가 아닌 DB 비관적 락 경로)
  - → 성공/실패를 `coupon:issue:status:{requestId}` 에 기록 + `coupon-issue-results` 토픽 발행
  - → `finally` 에서 ZSet 대기열에서 `remove`
- **폴링** — `GET /v1/coupon/issue-async/{request_id}/status` → `WAITING`/`SUCCESS`/`FAILED:{code}`/`UNKNOWN`
- **순번 조회** — `GET /v1/coupon/issue-async/{coupon_id}/position` → ZSet `rank` + `zCard`

### C. DB 폴백 (Redis 재고 미초기화 시)

- → `issueCouponInDbOnly` — `@Transactional(REQUIRES_NEW)`, **여전히 Redisson 락 구간 안**
- → `findByIdForUpdate` (`SELECT c FROM Coupon c WHERE c.id = :couponId` + `PESSIMISTIC_WRITE`)
- → `isActive` / `quantity > 0` / `findByCouponIdAndUserId` 중복 검사
- → `decreaseQuantity()` → `CouponIssue` insert → `Coupon` update
- → **`initStock(couponId, coupon.getQuantity())`** — 차감 후 수량으로 Redis 재고 시딩 → 다음 요청부터 A 경로 사용
- 쿠폰 생성 시 워밍업이 없으므로 **모든 쿠폰의 첫 발급은 반드시 이 경로를 탄다**

### D. 사용(소진) 단계 — 미연결

- 주문 생성 `OrderApplicationService.createOrder` → `calculateDiscount(couponId, userId, subTotal)` — 캐시 MGET → 미스분 DB IN 조회 → 소유자/활성/만료 검증 → 정책(`DiscountPolicyFactory`)으로 할인액 산출
- → `order.setDiscountAmount(...)` 후 주문·결제(PENDING) 저장
- → **`useCoupon` 은 호출되지 않음** — 쿠폰이 `isActive = false` 로 바뀌지 않아 같은 쿠폰을 반복 사용 가능
- 결제 계층(`application/payment`, `domain/payment`, `interfaces/payment`)에는 쿠폰 참조가 전혀 없음

### E. 이벤트 종료 (부수 경로)

- `CouponCommonService.createCoupon(request, expirationTime)` → `CouponDelayProducer` 로 지연 이벤트 발행
- → Kafka Streams 지연 처리 → `coupon-activate-events` 토픽
- → `CouponDelayConsumer.consume` → `CouponService.disableCoupon` + `CouponQueueRedisRepository.clear(couponId)` (ZSet·seq 키 즉시 삭제)

---

## 요약 — 코드에 없는 것

| 항목 | 상태 |
|---|---|
| 재고 키 샤딩 | **없음** (`shard` 검색 0건) |
| 쿠폰 재고 워밍업 러너 | **없음** (`initStock` 호출자는 DB 폴백 1곳뿐) |
| 쿠폰 사용 확정 엔드포인트 / `useCoupon` 프로덕션 호출자 | **없음** (테스트에서만 호출) |
| 결제 계층의 쿠폰 검증·소진 | **없음** (패키지 3곳 검색 0건) |
| 비동기 발급 DLQ / 재시도 | **없음** (컨슈머가 예외를 로깅 후 삼킴) |
| `rollbackIssue` 의 원자성 보장 | **없음** (INCR·SREM 이 개별 명령) |
| 부하 테스트 실측 결과 파일 | **없음** (`backend/docs/measurements/coupon.json` 미존재) |
