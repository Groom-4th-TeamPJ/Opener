#!/usr/bin/env python3
"""결과 불릿의 유보를 details.caveat 으로 물리 분리

단언부만 li 본문에 남기고 유보부를 접는다. 유보를 지우지 않는다 - 위치만 옮긴다
"""
import re

P = 'backend/docs/portfolio/PORTFOLIO_v2.html'


def cav(body):
    return ('\n                <details class="caveat"><summary>범위와 한계</summary>'
            '<div>%s</div></details>\n            ' % body)


# (앵커 = 해당 li 안에서 유일한 문자열, 새 li 내부 HTML)
REWRITE = [
    # ── ai-1 ─────────────────────────────────────────
    ('전달 구조가 완료 대기에서 도착 즉시로',
     '<b>전달 구조가 완료 대기에서 도착 즉시로 바뀜</b> — <span class="mono">blockLast()</span> 제거로 첫 토큰 생성 즉시 SSE로 흐름.\n'
     '                첫 글자까지 <span class="metric">중앙값 1.09s · p95 1.73s</span> <span class="src src-m">실측</span>'
     '(20회 · 일반 채팅 <span class="mono">POST /chat/message</span> · RAG on)'
     + cav('before인 <span class="metric">완료 대기 15s</span> <span class="src src-e">추정</span>은 구조가 이미 교체돼 '
           '<b>재측정 불가</b>이므로 배수를 미주장. 측정 세부 — <span class="mono">chat.sse.ttft</span> Timer 누적값 델타 방식, '
           '워밍업 2회 제외, 매 회 새 세션이라 히스토리 누적 부재')),

    ('유실 경로 3종을 계측 가능하게 만듦',
     '<b>유실 경로 3종을 서로 다른 카운터로 분리해 사유별 추적이 성립</b> — 버퍼 오버플로(<span class="mono">chat.sse.overflow</span>),\n'
     '                스트림 중단 시 부분 응답(<span class="mono">chat.stream.partial.saved</span>), 영속화 시 메시지 공백(<span\n'
     '                        class="mono">reason=expired|lost</span>)'
     + cav('유실 자체의 제거가 아니라 <b>관측 가능화</b>가 이 절의 범위. '
           '<span class="mono">expired</span>/<span class="mono">lost</span> 판정 설계는 <a href="#ai-6">6장</a>이 정본')),

    # ── ai-2 ─────────────────────────────────────────
    ('Master 강제 종료 통제 실험에서',
     'Master 강제 종료 통제 실험에서 복제본 자동 승격으로 <b>약 7초 자동 복구</b> <span class="src src-d">통제 실험</span> — '
     '<b>이 7초는 설정값의 결과</b>이고 <span class="mono">cluster-node-timeout 5000</span>(장애 판정 5초) + 선출·승격 시간'
     + cav('before인 수동 재시작 <span class="metric">5~10분</span> <span class="src src-e">추정</span>은 '
           '<b>분모가 사람이 개입하는 시간</b>이라 배수와 정밀도를 미주장. '
           '더 줄이려면 오탐 위험을 대가로 <span class="mono">cluster-node-timeout</span>을 낮춰야 함')),

    ('Redis 커넥션 풀(Lettuce · commons-pool2)',
     '<b>Redis 커넥션 풀(Lettuce · commons-pool2)</b>은 <span class="mono">maxTotal=20 / maxIdle=10 / minIdle=5</span>로 상한이 명확하고,\n'
     '                <b>20개 풀로 100 VU를 p95 133ms에 수용</b> <span class="src src-m">실측</span>'
     + cav('확인된 것은 "제한 부재"가 아니라 <b>이 수용량까지</b>. '
           'DB 커넥션 풀과 같은 수여도 수용량이 다른 이유는 <b>점유 시간의 성질</b> — Redis 명령은 왕복이 밀리초 단위라 커넥션이 즉시 회전하고, '
           'DB 풀은 트랜잭션이 끝날 때까지 붙잡음. <span class="mono">eco</span> 절의 풀 수치와 직접 비교 불가')),

    # ── ai-4 ─────────────────────────────────────────
    ('RAG On/Off 양방향으로',
     'RAG On/Off 양방향으로 <b>동일 질문셋을 통과시켜 근거 없는 응답이 감소하는 것을 확인</b> <span class="src src-e">정성 관찰</span>'
     + cav('채점 기준표와 질문셋이 부재해 <b>규모는 미주장</b>. 이 절이 주장하는 범위는 <b>검색 0건이던 상태의 해소</b>까지')),

    ('다만 0.2는 0.1·0.0과 결과가 완전히 동일',
     '<b>임계값은 품질 손잡이가 아니라 켜기/끄기 스위치였다는 것이 이 스윕의 결론</b> — 유효한 상한은 <span class="metric">0.3</span>부터 시작'
     + cav('현재 값 <span class="metric">0.2</span>는 0.1·0.0과 결과가 완전히 동일 — <span class="mono">topK=5</span>가 먼저 차서 '
           '<b>임계값이 실질적으로 무작동</b>. 정밀도도 전 구간 <span class="metric">37% 언저리</span>에서 임계값에 거의 무반응이라, '
           '정밀도를 올리려면 임계값이 아니라 청킹·리랭킹 축이 필요')),

    # ── ai-6 ─────────────────────────────────────────
    ('유실이 DLQ에 남아 추적 가능',
     '<b>유실이 DLQ에 남아 추적 가능</b> — DLQ 적재 자체가 <b>"고칠 수 있는 사고"의 신호</b>로 동작'
     + cav('이전에는 유실과 만료가 모두 정상 ACK로 사라져 사후 확인 수단이 부재')),

    # ── eco-1 ────────────────────────────────────────
    ('기존 구조의 실측 중 근거 자격이 있는 것만 존치',
     '<b>기존 구조의 실측 중 근거 자격이 있는 것만 존치</b> — 락 구성에서 수량 100개 쿠폰에 VU 200 · 5,000 요청으로 <b>100건 발급</b>,\n'
     '                총량 상한이 지켜짐을 확인 <span class="src src-p">대리 부하</span>'
     + cav('함께 적었던 "중복 시도 N건 거부"는 제외 — 응답 본문 부분일치로 세어져 <b>거짓 품절·저장소 불일치·진짜 품절이 한 칸에 혼재</b>. '
           '지금은 사유별 에러코드로 갈라 뒀으나 재측정 전까지 수치 미사용')),

    # ── eco-2 ────────────────────────────────────────
    ('Redis 선점 게이트는',
     '<b>Redis 선점 게이트는 재고가 요청보다 적을 때 PG 호출 자체를 차단</b> — 어차피 실패할 요청에 PG 왕복 비용과 승인 후 환불 미발생'
     + cav('이번 측정 조건(재고 1만 / 요청 1만)은 <b>전량이 게이트를 통과하는 구간</b>이라 이 효과가 수치로 미노출')),

    # ── eco-3 ────────────────────────────────────────
    ('외부 PG 장애(5s 지연 설정',
     '외부 PG 장애(5s 지연 설정 + 500 주입) 시 서킷브레이커 fast-fail 응답 <b>평균 632.9ms</b> <span class="src src-d">통제 실험</span>,\n'
     '                장애 구간 호출의 <span class="metric">97.8%</span>(667/682)를 <b>PG 미도달로 차단</b>'
     + cav('이 비율은 <b>서킷이 OPEN인 시간 비율에 전적으로 의존</b>하므로 실패율 50%를 넘기는 주입 조건에서의 값. '
           '이전 판본의 "5.6s → 0.6s(약 9배)"는 삭제 — 서킷 off 런의 산출물이 저장소에 부재해 before를 뒷받침 불가이고, '
           '5.6s는 같은 런의 최댓값과 소수점까지 일치해 대조군으로 사용 불가')),

    # ── eco-4 ────────────────────────────────────────
    ('처리 지연 최악값 기준',
     '<b>처리 지연 최악값 <span class="metric">1.1s</span></b> <span class="src src-m">실측</span>(이벤트 발행~비활성 전환 시각 차 직접 측정)이고,\n'
     '                punctuate 주기 1s + 처리시간이라는 <b>이론 최악값에 수렴</b>'
     + cav('함께 적힌 12s와 나란히 읽을 때 주의 필요 — 12s는 <b>채택하지 않은 폴링 대안의 설정에서 도출</b>한 값 '
           '<span class="src src-d">도출</span>(주기 10s + 스캔 2s)이라 실제로 돌려 본 구현이 아님. '
           '즉 이 비교는 측정 대 측정이 아니라 <b>설계 계산 대 측정</b>')),

    # ── eco-5 ────────────────────────────────────────
    ('발행·처리 실패가 모두 재시도로 수렴',
     '<b>발행·처리 실패가 모두 재시도로 수렴하는 at-least-once</b> — 실패가 유실이 아니라 재시도로 흡수됨'
     + cav('대가는 중복 실행 가능성이고, <b>컨슈머의 멱등이 전제로 잔존</b>')),

    ('지연 상한이 폴링 주기라는 것이',
     '<b>지연 상한이 폴링 주기라는 것을 설계 성질로 확정</b> — 값이 아니라 구조에서 나오므로 주기를 줄이면 상한이 줄 뿐 성질은 불변'
     + cav('즉시성이 요구되면 <a href="#eco-4">§4</a> 방식으로 옮겨야 하고, 그 전까지는 "예약이 사라지지 않는다"까지가 이 구조가 보장하는 범위')),

    # ── wf-2 ─────────────────────────────────────────
    ('여기서 드러난 키 설계의 한계가',
     '<b>여기서 드러난 키 설계의 한계가 다음 프로젝트의 설계를 전환</b> — Refresh Token을 <span class="mono">refresh_token:{token}</span>으로\n'
     '                저장한 탓에 사용자 단위 열거·폐기가 불가능했고, AI 튜터링에서는 <span class="mono">refreshToken:{userId}</span> 기준으로 잡고\n'
     '                블랙리스트 키도 토큰 전문이 아닌 <span class="mono">jti</span>로 옮겨 <b>키 설계 쪽 제약은 제거</b>'
     + cav('같이 잃은 것이 존재 — 휠 파인더에 있던 <span class="mono">blacklist:user:{id}:version</span>'
           '(사용자 단위 access 일괄 무효화)을 함께 가져오지 않아 <b>키 설계는 나아졌으나 폐기 단위는 후퇴</b>. '
           '"전체 로그아웃"의 실제 범위도 이 프로젝트에서는 Access Token까지')),
]


def main():
    s = open(P, encoding='utf-8').read()
    done = 0
    for anchor, new in REWRITE:
        i = s.find(anchor)
        if i < 0:
            print('앵커 미발견:', anchor[:30]); continue
        a = s.rfind('<li>', 0, i)
        b = s.find('</li>', i)
        if a < 0 or b < 0:
            print('li 경계 실패:', anchor[:30]); continue
        # 앵커가 여러 li 에 걸치지 않는지 확인
        if s.count(anchor) != 1:
            print('앵커 중복(%d건): %s' % (s.count(anchor), anchor[:30])); continue
        s = s[:a] + '<li>' + new + s[b:]
        done += 1
    open(P, 'w', encoding='utf-8').write(s)
    n = s.count('<details class="caveat">')
    print('분해 %d/%d건 · details.caveat %d개' % (done, len(REWRITE), n))


if __name__ == '__main__':
    main()
