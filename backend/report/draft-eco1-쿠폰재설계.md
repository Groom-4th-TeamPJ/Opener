# eco-1 쿠폰 재설계 — PORTFOLIO.html 교체용 초안

- 작성일: 2026-07-30
- 대상: `backend/docs/PORTFOLIO.html` `id="eco-1"` 섹션
- 상태: **초안 — PORTFOLIO.html 미편집.** 승인 후 반영
- 입력: `2026-07-30-쿠폰-파이프라인-재설계안.md`(설계 4축), `agent-out-쿠폰-파이프라인-현황.md`(코드 근거)

---

## 1. 교체 대상 범위

### (A) 회고 문단 삭제 — `해결 과정` `<ol class="bul-num">` 의 8번째 `<li>` (원문 L647)

**삭제할 원문**

```html
<li>지금 다시 만든다면 <b>락 범위를 폴백 경로로 좁히겠습니다</b> — 정상 경로는 Lua만으로 충분한데 현재는 전 구간에 락이 걸려 락 보유 시간에 DB I/O가 포함되고, 직렬 구간이 길어지면 그만큼 처리량 상한이 내려갑니다. 교체 대상이던 <b>비관적 락은 커넥션과 행 잠금을 함께 점유해 커넥션 풀 크기가 곧 동시성 상한</b>이 되는데, 이 구조는 직렬 구간이 Lua 실행 시간으로 줄어든 것이 개선의 메커니즘입니다</li>
```

**삭제 이유**: 현재 구현이 과하다는 자백으로 읽히고, 바로 앞 항목(L643)의 "정상 경로에서 락은 정확성에 기여하지 않습니다"와 겹쳐 "그럼 락은 왜 넣었나"라는 역질문을 자초한다.

**대체 위치**: 삭제한 자리가 아니라 **`<ol>` 의 마지막**(현재 L648 "확인한 한계와 후속 조치" 항목 **다음**, `</ol>` 직전)에 신규 `<li>` 2개를 넣는다. 확장 로드맵은 "확인한 한계"보다 뒤에 오는 편이 서사가 자연스럽다.

> ⚠️ 삭제된 원문의 마지막 문장 — "직렬 구간이 DB I/O에서 Lua 실행 시간으로 줄어든 것이 개선의 메커니즘"이라는 **성능 개선의 인과 설명**은 `결과` 섹션 2번째 `<li>`(p99 항목)에 이미 같은 문장으로 남아 있다. 따라서 삭제해도 인과 설명은 유실되지 않는다.

항목 수 변화: 9개 → 10개 (1개 삭제, 2개 추가)

### (B) SVG 다이어그램

**권장안(기본)**: 상단 `전체적인 아키텍처` viz-frame(원문 L588~L628)은 **그대로 둔다.** 그것은 *구현된* 현재 구조를 그리며, 해결 과정 1~7번 항목이 전부 그 구조를 설명한다. 대신 **`해결 과정` `</ol>` 직후에 두 번째 viz-frame 을 추가**해 확장 설계를 그린다(아래 §2-C).

> 근거: 재설계는 **미구현**이다. 섹션 최상단 "전체적인 아키텍처" 자리에 샤딩 구조를 그리면 그것이 현재 아키텍처로 읽혀, 본문의 "확장 설계" 서술과 어긋난다. 다이어그램이 서술보다 강하게 각인되므로 이 위치는 사실 왜곡 위험이 크다.

**대안(지시대로 전면 교체할 경우)**: 원문 L588~L628 의 `<div class="viz-frame">…</div>` 전체를 §2-C 블록으로 치환하고, `<p class="sec-label">전체적인 아키텍처</p>` 를 `<p class="sec-label">확장 설계</p>` 로 바꾼다. 이 경우 현재 구현의 시퀀스 다이어그램은 문서에서 사라진다.

### (C) `결과` 섹션 — `<ul class="bul-sub">` (원문 L682~L686)

| 원문 위치 | 처리 |
|---|---|
| 1번째 `<li>` (VU 200 · 100건 발급) | **그대로 유지** |
| 2번째 `<li>` (p99 350ms → 85ms) | **손대지 않음** — 재측정 중, 다른 담당 처리 |
| 3번째 `<li>` (락은 폴백 구간만 직렬화…) | **문장 교체** — 아래 §2-D |
| — | **4번째 `<li>` 신규 추가** — 아래 §2-D |

3번째 `<li>` 를 고치는 이유: 현재 문장은 "락은 두 저장소에 걸친 폴백 구간만 직렬화하는 구조 확보"라고 단정하지만, 코드상 락 구간에는 `persistIssuedCoupon` 의 DB 트랜잭션 전체가 들어 있다(`CouponIssueService.java:107-151`). 회고 문단(L647)이 이 간극을 완충하고 있었는데 그 문단을 지우면 남는 문장이 **사실과 어긋난 단정**이 된다. 보호 범위 서술로 바꿔 사실에 맞춘다.

---

## 2. 완성된 HTML 블록

### (A) `해결 과정` `</ol>` 직전에 추가할 `<li>` 2개

```html
  <li>지금 구조의 처리량 상한은 <b>쿠폰 하나당 락 하나·카운터 하나</b>라는 점에 있습니다 — 서버를 늘려도 같은 쿠폰의 요청은 결국 같은 키에서 직렬화됩니다. 트래픽이 한 단계 더 올라가는 규모를 가정하고 확장 설계를 세 축으로 정리했습니다. ① <b>재고 워밍업</b> — 지금은 쿠폰 생성 시 Redis 재고를 시딩하지 않아 첫 발급 요청이 반드시 폴백을 타는데, 하필 이벤트 시작 순간이 최대 트래픽 시점입니다. 쿠폰 생성 시점과 이벤트 시작 전 배치에서 재고를 미리 채우면 폴백은 <b>워밍업 누락·Redis 재기동에 대비한 최후 방어선</b>으로만 남습니다. 락을 걷어내는 방향이 아니라 <b>락이 DB I/O를 안고 있는 상황 자체를 없애는</b> 방향이며, 그 결과 락의 역할이 "정상 경로 직렬화"가 아니라 "두 저장소에 걸친 예외 구간의 배타성"으로 확정됩니다. ② <b>재고 샤딩</b> — 수량 Q를 N개 샤드로 나눠 <span class="mono">coupon:{couponId}:stock:{shard}</span>에 두고 <span class="mono">shard = hash(userId) % N</span>으로 배정하면 락 경합이 이론상 1/N로 내려갑니다. 배정 기준을 <b>userId로 잡아야</b> 같은 사용자의 재시도가 항상 같은 샤드로 가 샤드별 상태를 신뢰할 수 있고, 키를 <span class="mono">{couponId}</span> <b>해시 태그</b>로 묶어야 Redis Cluster에서 Lua의 multi-key 연산이 같은 슬롯에 떨어져 <span class="mono">CROSSSLOT</span> 실패를 피합니다. 반대로 <b>발급자 SET은 샤딩하지 않습니다</b> — 1인 1매 판정은 전역이어야 하고, 쪼개면 한 사용자가 샤드마다 한 장씩 받습니다. 샤드 편차가 만드는 "전체 재고는 남았는데 내 샤드만 빈" 오탐은 Lua 안에서 다음 샤드를 순회하는 <b>샤드 스틸</b>로 흡수하고(스크립트 한 번의 실행 안에서 탐색과 차감이 끝나므로 check-then-act 경합이 생기지 않습니다), 소진 말기에 탐색이 길어지는 구간은 남은 샤드를 하나로 합치는 <b>잔여 회수</b>로 상한을 잡습니다. 초과발급 방어는 여전히 Lua 원자성이 담당하고 <b>샤딩은 처리량 축만</b> 건드리는 것이 이 분리의 핵심입니다. ③ <b>기존 ZSet 대기열과의 결합</b> — 비동기 경로에 이미 쓰고 있는 <span class="mono">coupon:seq</span>(INCR)·<span class="mono">coupon:queue</span>(ZADD NX)는 <b>순번을 전역으로 유지</b>한 채 샤딩과 결합합니다. "3번 샤드의 12등"은 사용자에게 정보가 아니고, 샤드 번호가 응답에 새면 내부 분산 전략이 API 계약이 되어 나중에 N을 바꿀 수 없게 되기 때문입니다. 여기에 컨슈머 DLQ와 상태 TTL 만료의 <b>타임아웃 확정 실패 전이</b>를 함께 두어, 예외가 삼켜졌을 때 요청이 영원히 <span class="mono">WAITING</span>으로 남는 구멍을 닫습니다</li>
  <li>같은 원칙을 <b>자원의 수명주기 전체로 확장</b>하는 것이 그다음 축입니다 — 발급 단계는 Lua <span class="mono">SISMEMBER</span>와 DB 중복 조회로 이중 방어를 걸어 뒀지만, 발급된 쿠폰이 <b>주문·결제에서 소진되는 구간</b>에는 같은 밀도의 방어가 걸려 있지 않습니다. 입구만 잠근 창고는 잠그지 않은 창고와 같으므로, 쿠폰을 <span class="mono">ISSUED → HELD → USED</span> 세 상태로 두고 주문 생성 시점에 <span class="mono">SET coupon:hold:{couponId}:{userId} {orderId} NX EX 600</span>으로 <b>선점</b>합니다. <span class="mono">NX</span>가 중복 선점을 원자적으로 차단하고, <span class="mono">EX</span> 10분이 결제가 중단돼도 선점분이 영구히 묶이지 않게 하며, 값에 <span class="mono">orderId</span>를 담아 어느 주문이 잡았는지 추적합니다. 확정 단계에서는 <span class="mono">orderId</span>를 <b>멱등키</b>로 삼아 PG 웹훅이 중복 도착해도 두 번 소진되지 않게 하고, <span class="mono">(coupon_issue_id, order_id)</span> 유니크 제약을 최종 방어로 둡니다. 결제 실패·타임아웃 시의 해제는 명시적 <span class="mono">DEL</span>과 TTL 자연 만료의 이중 회수인데, 이는 <b>다음 항목의 Saga 보상 트랜잭션과 동일한 패턴</b>입니다 — 새 메커니즘을 들이는 대신 프로젝트 안에서 이미 검증된 회수 경로에 대상 하나를 얹는 쪽을 택했습니다</li>
```

### (B) 삭제할 `<li>` (재확인용, 원문 그대로)

```html
  <li>지금 다시 만든다면 <b>락 범위를 폴백 경로로 좁히겠습니다</b> — 정상 경로는 Lua만으로 충분한데 현재는 전 구간에 락이 걸려 락 보유 시간에 DB I/O가 포함되고, 직렬 구간이 길어지면 그만큼 처리량 상한이 내려갑니다. 교체 대상이던 <b>비관적 락은 커넥션과 행 잠금을 함께 점유해 커넥션 풀 크기가 곧 동시성 상한</b>이 되는데, 이 구조는 직렬 구간이 Lua 실행 시간으로 줄어든 것이 개선의 메커니즘입니다</li>
```

### (C) 확장 설계 SVG — `해결 과정` `</ol>` 직후에 삽입

```html
<p class="sec-label">확장 설계</p>
<div class="viz-frame">
  <div class="viz-title">확장 설계 — 워밍업으로 폴백을 예외로 밀어내고, 재고를 샤딩해 경합을 1/N로, 방어를 수명주기 전체로 (미구현 · 설계 단계)</div>
  <svg viewBox="0 0 1100 480" xmlns="http://www.w3.org/2000/svg" width="100%">
    <style>
      .cx-box{fill:rgba(67,97,184,0.10); stroke:#4361b8; stroke-width:1.5}
      .cx-box-y{fill:rgba(180,83,9,0.10); stroke:#b45309; stroke-width:1.5}
      .cx-box-g{fill:rgba(0,122,85,0.10); stroke:#007a55; stroke-width:1.5}
      .cx-tt{fill:#1a1a2e; font:bold 14px sans-serif; text-anchor:middle}
      .cx-mu{fill:#6b7280; font:12.5px sans-serif; text-anchor:middle}
      .cx-line{stroke:#9ca3af; stroke-width:1.5; fill:none}
    </style>
    <defs>
      <marker id="cx-arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0,0 L10,5 L0,10 z" fill="#9ca3af"/></marker>
      <marker id="cx-arry" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0,0 L10,5 L0,10 z" fill="#b45309"/></marker>
    </defs>

    <!-- ① 재고 워밍업 -->
    <text class="v-tag" x="20" y="30">① 재고 워밍업 — 폴백 진입 자체를 없애 락을 예외 경로 전용 안전장치로 확정</text>
    <rect class="cx-box" x="20" y="42" width="215" height="52" rx="8"/>
    <text class="cx-tt" x="127" y="64">쿠폰 생성 · 이벤트 전 배치</text>
    <text class="cx-mu" x="127" y="82">재고 Q를 샤드별로 시딩</text>
    <path class="cx-line" d="M235,68 L280,68" marker-end="url(#cx-arr)"/>
    <rect class="cx-box-g" x="280" y="42" width="230" height="52" rx="8"/>
    <text class="cx-tt" x="395" y="64">Redis 재고 초기화 완료</text>
    <text class="cx-mu" x="395" y="82">coupon:{couponId}:stock:{n}</text>
    <text class="cx-mu" x="585" y="58" style="fill:#b45309">워밍업 누락 · Redis 재기동 시에만</text>
    <path class="cx-line" d="M510,68 L655,68" marker-end="url(#cx-arry)" stroke-dasharray="4,3" style="stroke:#b45309"/>
    <rect class="cx-box-y" x="660" y="42" width="250" height="52" rx="8"/>
    <text class="cx-tt" x="785" y="64">DB 폴백 + 분산 락</text>
    <text class="cx-mu" x="785" y="82">예외 경로 전용 안전장치</text>
    <text class="cx-mu" x="660" y="112" style="text-anchor:start">락은 그대로 둔다 — 역할이 정상 경로 직렬화가 아니라 두 저장소에 걸친 구간의 배타성으로 한정된다</text>

    <!-- ② 재고 샤딩 -->
    <text class="v-tag" x="20" y="150">② 재고 샤딩 — shard = hash(userId) % N, 락 경합 이론상 1/N (정확성은 Lua 원자성이 유지)</text>
    <rect class="cx-box" x="20" y="162" width="175" height="52" rx="8"/>
    <text class="cx-tt" x="107" y="184">발급 요청</text>
    <text class="cx-mu" x="107" y="202">userId 해싱으로 배정</text>
    <path class="cx-line" d="M195,188 L228,188" marker-end="url(#cx-arr)"/>
    <rect class="cx-box-g" x="230" y="162" width="175" height="52" rx="8"/>
    <text class="cx-tt" x="317" y="184">shard 0</text>
    <text class="cx-mu" x="317" y="202">stock + lock 쌍</text>
    <rect class="cx-box-g" x="425" y="162" width="175" height="52" rx="8"/>
    <text class="cx-tt" x="512" y="184">shard 1</text>
    <text class="cx-mu" x="512" y="202">stock + lock 쌍</text>
    <text class="cx-tt" x="632" y="194" style="fill:#9ca3af">···</text>
    <rect class="cx-box-g" x="665" y="162" width="175" height="52" rx="8"/>
    <text class="cx-tt" x="752" y="184">shard N-1</text>
    <text class="cx-mu" x="752" y="202">stock + lock 쌍</text>
    <path class="cx-line" d="M840,188 L878,188" marker-end="url(#cx-arr)" stroke-dasharray="4,3"/>
    <rect class="cx-box" x="880" y="162" width="200" height="52" rx="8"/>
    <text class="cx-tt" x="980" y="184">발급자 SET (전역)</text>
    <text class="cx-mu" x="980" y="202">샤딩하지 않음 · 1인 1매</text>
    <path class="cx-line" d="M317,214 L317,232 L752,232 L752,216" marker-end="url(#cx-arr)" stroke-dasharray="4,3"/>
    <text class="cx-mu" x="535" y="252">샤드 스틸 — 자기 샤드가 0이면 Lua 한 번의 실행 안에서 다음 샤드를 순회해 탐색·차감이 끝난다</text>
    <text class="cx-mu" x="535" y="270">잔여 회수 — 소진 말기에 남은 샤드를 하나로 합쳐 탐색 비용의 상한을 잡는다</text>

    <!-- ③ ZSet 대기열 결합 -->
    <text class="v-tag" x="20" y="300">③ 기존 ZSet 대기열과 결합 — 순번은 전역 유지, 샤드 번호는 API에 비노출</text>
    <rect class="cx-box" x="20" y="312" width="240" height="52" rx="8"/>
    <text class="cx-tt" x="140" y="334">coupon:seq · coupon:queue</text>
    <text class="cx-mu" x="140" y="352">INCR + ZADD NX (구현 완료)</text>
    <path class="cx-line" d="M260,338 L298,338" marker-end="url(#cx-arr)"/>
    <rect class="cx-box" x="300" y="312" width="210" height="52" rx="8"/>
    <text class="cx-tt" x="405" y="334">추정 대기 순번</text>
    <text class="cx-mu" x="405" y="352">rank + size 조회 전용</text>
    <text class="cx-mu" x="535" y="332" style="text-anchor:start">샤드 번호를 응답에 담지 않는다 — 내부 분산 전략이 API 계약에 새면 N을 다시 못 바꾼다</text>
    <text class="cx-mu" x="535" y="354" style="text-anchor:start">컨슈머 DLQ + 상태 TTL 만료를 타임아웃 확정 실패로 전이 — WAITING 잔존을 닫는다</text>

    <!-- ④ 수명주기 확장 -->
    <text class="v-tag" x="20" y="392">④ 동시성 방어를 자원의 수명주기 전체로 확장 — 발급뿐 아니라 소진까지</text>
    <rect class="cx-box-g" x="20" y="404" width="150" height="48" rx="8"/>
    <text class="cx-tt" x="95" y="426">ISSUED</text>
    <text class="cx-mu" x="95" y="443">발급 완료</text>
    <text class="cx-mu" x="257" y="420">주문 생성 · SET NX EX 600</text>
    <path class="cx-line" d="M170,428 L343,428" marker-end="url(#cx-arr)"/>
    <rect class="cx-box-y" x="345" y="404" width="205" height="48" rx="8"/>
    <text class="cx-tt" x="447" y="426">HELD</text>
    <text class="cx-mu" x="447" y="443">coupon:hold:{id}:{userId}</text>
    <text class="cx-mu" x="665" y="420">결제 승인 · 멱등키 orderId</text>
    <path class="cx-line" d="M550,428 L778,428" marker-end="url(#cx-arr)"/>
    <rect class="cx-box-g" x="780" y="404" width="150" height="48" rx="8"/>
    <text class="cx-tt" x="855" y="426">USED</text>
    <text class="cx-mu" x="855" y="443">usedAt 확정</text>
    <path class="cx-line" d="M447,452 L447,468 L95,468 L95,454" marker-end="url(#cx-arr)" stroke-dasharray="4,3"/>
    <text class="cx-mu" x="600" y="470" style="text-anchor:start">결제 실패 · TTL 만료 → DEL + 자연 만료의 이중 회수로 ISSUED 복귀 (Saga 보상과 동일 패턴)</text>
  </svg>
</div>
```

### (D) `결과` 섹션 교체분

**교체할 원문 (3번째 `<li>`)**

```html
  <li>정상 경로는 Lua로 완결되고 락은 두 저장소에 걸친 폴백 구간만 직렬화하는 구조 확보 — 도구를 보호 범위 기준으로 나눈 결과이며, 같은 기준이 재고 도메인에서 락을 쓰지 않은 판단도 설명합니다</li>
```

**대체문 + 신규 4번째 `<li>`**

```html
  <li>정상 경로의 초과발급 방어는 Lua 원자 연산이, 두 저장소에 걸친 폴백 구간의 배타성은 분산 락이 담당하도록 <b>보호 범위를 기준으로 도구를 나눈 구조</b> 확보 — 같은 기준이 재고 도메인에서 락을 쓰지 않은 판단도 설명합니다</li>
  <li>현재 구조의 처리량 상한을 <b>쿠폰당 단일 락·단일 카운터</b>로 코드 수준에서 특정하고, 그 위 규모를 위한 <b>확장 설계</b>를 워밍업·재고 샤딩·기존 대기열 결합·수명주기 선점 네 축으로 정리 — 초과발급 방어는 Lua 원자성에 남기고 샤딩은 처리량 축만 건드리도록 <b>정확성과 성능의 책임을 분리</b>한 설계입니다</li>
```

> 유지: 1번째 `<li>`(VU 200 · 100건), 2번째 `<li>`(p99 350ms → 85ms). 2번째는 재측정 중이므로 **손대지 않는다.**

---

## 3. 검증 체크리스트

### SVG 클래스 접두사 중복

문서 전체에서 이미 쓰이는 접두사(추출 결과): `c1-`, `c5-`, `cp-`, `ea-`, `ks-`, `pg-`, `rc-`, `rg-`, `sk-`, `sm-`, `wa-`, `wb-`, `wc-`, `wd-`, `wf-`

- 신규 접두사 **`cx-`** 선택 → 위 목록에 **없음. 충돌 없음** ✅
- 신규 클래스 5종: `.cx-box`, `.cx-box-y`, `.cx-box-g`, `.cx-tt`, `.cx-mu`, `.cx-line` — 모두 `cx-` 접두 ✅
- 전역 클래스 `.v-tag`(`fill:#db2777; text-anchor:start`)는 기존 eco-1 SVG 도 쓰던 공용 클래스이므로 **의도적으로 재사용** ✅

### marker id 중복

문서 내 기존 marker id: `c1-arr`, `c5-arr`, `cp-arr`, `cp-arrx`, `ea-arr`, `pg-arr`, `rc-arr`, `rg-arr`, `sk-arr`, `sm-arr`, `sm-arrx`, `wa-arr`, `wb-arr`, `wc-arr`, `wd-arr`, `wf-arr`

- 신규: `cx-arr`, `cx-arry` → **둘 다 미사용. 충돌 없음** ✅

### 태그 균형

- `<li>` 신규 2개 · 결과 섹션 2개 — 모두 여닫음 쌍 ✅
- `<div class="viz-frame">` 1개 ↔ `</div>` 2개(`viz-title` 포함) ✅
- `<svg>`/`</svg>`, `<style>`/`</style>`, `<defs>`/`</defs>` 각 1쌍 ✅
- `<rect>`/`<path>`/`<marker>` 는 self-closing 또는 즉시 닫음 ✅
- `<span class="mono">` 총 15개 ↔ `</span>` 15개 ✅ / `<b>` 총 18개 ↔ `</b>` 18개 ✅

### 금지 표현 미사용

| 금지 | 검사 결과 |
|---|---|
| "도입했습니다" / "적용했습니다" | **0건** ✅ |
| "해야 합니다" / "필요합니다" | **0건** ✅ (`~여야 합니다`도 미사용, `~로 잡아야`·`~로 묶어야` 는 기술적 제약을 설명하는 종속절이며 미완 시제 아님) |
| "~하겠습니다" (미래·회고 시제) | **0건** ✅ |
| ZSet "도입" 서술 | **0건** ✅ — "이미 쓰고 있는 … 을 샤딩과 결합합니다", SVG 라벨도 "(구현 완료)" |

### 톤·사실 확인

- 축 4는 "갭"이 아니라 "**동시성 방어를 자원의 수명주기 전체로 확장**"이라는 원칙 확장으로 서술 ✅
- 축 1은 "락을 좁힌다"(부정)가 아니라 "**락이 DB I/O를 안고 있는 상황 자체를 없앤다**"(추가)로 프레이밍 ✅
- 문체: 전 문장 "~습니다" 체, 본문 한글 + 영문 코드 식별자만 영문 ✅
- SVG 캡션은 기존 eco-1 SVG 관례(`되돌린다`/`보호된다`)에 맞춘 평서형 ✅
- SVG 색상: 기본 `#4361b8` / 주의 `#b45309` / 성공 `#007a55` — 지정된 팔레트 그대로, 문제(`#e86969`)는 미사용(결함 자백 톤 회피) ✅
- `viewBox="0 0 1100 480"` + `width="100%"`, 절대 width 없음 ✅
- 좌측 캡션은 전부 `class="v-tag"`(전역 `text-anchor:start`) 또는 인라인 `style="text-anchor:start"` — 가운데 정렬 텍스트를 x<40 에 둔 곳 없음 ✅
- `<pre>` ASCII 다이어그램 없음 ✅

### 반영 시 주의

- 이 초안은 **PORTFOLIO.html 을 편집하지 않았다.** 위 블록을 적용하는 것은 별도 승인 사항이다
- §1-(B) 는 권장안(추가)과 대안(전면 교체) 두 가지다. **선택이 필요하다**
