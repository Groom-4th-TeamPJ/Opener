#!/usr/bin/env python3
"""RAG 유사도 임계값 스윕 - 한국어 질문 20문항 x 임계값 6지점

Spring AI PgVectorStore 와 같은 기준으로 잰다
  similarity = 1 - cosine_distance,  통과 조건 = similarity >= threshold,  이후 topK 로 자름
"""
import json
import os
import re
import subprocess
import urllib.request

ENV = '/Users/mskim/Desktop/PJ/Opener/backend/.env'
MODEL = 'text-embedding-3-small'
TOPK = 5
THRESHOLDS = [0.0, 0.1, 0.2, 0.3, 0.5, 0.7]

# (질문, 기대 type) - type 은 벡터 스토어 metadata->>'type' 값과 대조할 정답 라벨
QUESTIONS = [
    ("이차방정식의 근의 공식을 알려줘", "Algebra"),
    ("일차부등식을 푸는 방법을 설명해줘", "Algebra"),
    ("함수의 그래프에서 점근선은 어떻게 찾나요", "Algebra"),
    ("로그의 성질을 정리해줘", "Intermediate Algebra"),
    ("복소수의 곱셈은 어떻게 계산하나요", "Intermediate Algebra"),
    ("등비수열의 합을 구하는 공식이 뭔가요", "Intermediate Algebra"),
    ("분수의 덧셈을 하는 순서를 알려줘", "Prealgebra"),
    ("백분율 계산 문제를 푸는 방법", "Prealgebra"),
    ("최소공배수를 구하는 방법을 알려줘", "Prealgebra"),
    ("삼각함수의 덧셈정리를 설명해줘", "Precalculus"),
    ("행렬의 곱셈은 어떻게 하나요", "Precalculus"),
    ("극좌표를 직교좌표로 바꾸는 방법", "Precalculus"),
    ("소수를 판별하는 방법을 알려줘", "Number Theory"),
    ("합동식의 성질을 설명해줘", "Number Theory"),
    ("원의 넓이를 구하는 공식이 뭐야", "Geometry"),
    ("삼각형의 닮음 조건을 알려줘", "Geometry"),
    ("정육면체의 부피와 겉넓이를 구하는 법", "Geometry"),
    ("순열과 조합의 차이가 뭔가요", "Counting & Probability"),
    ("확률의 덧셈정리를 설명해줘", "Counting & Probability"),
    ("경우의 수를 세는 기본 원리를 알려줘", "Counting & Probability"),
]


def api_key():
    for line in open(ENV, encoding='utf-8'):
        if line.startswith('OPENAI_API_KEY='):
            return line.split('=', 1)[1].strip()
    raise SystemExit('OPENAI_API_KEY 부재')


def embed(text, key):
    req = urllib.request.Request(
        'https://api.openai.com/v1/embeddings',
        data=json.dumps({'input': text, 'model': MODEL}).encode(),
        headers={'Authorization': f'Bearer {key}', 'Content-Type': 'application/json'},
    )
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.loads(r.read())['data'][0]['embedding']


def topn(vec, n=25):
    """거리 오름차순 상위 n건의 (similarity, type, asy) 반환

    asy - 청크에 Asymptote 그림 소스 코드가 섞였는지. 그림이 이미지가 아니라 코드 텍스트로
    들어가 있어 임베딩에 노이즈로 작용하는지를 보려는 축
    """
    lit = '[' + ','.join(f'{v:.6f}' for v in vec) + ']'
    sql = (
        "SELECT round((1 - (embedding <=> '%s'))::numeric, 4) AS sim, "
        "coalesce(metadata->>'type','-') AS t, "
        "(content LIKE '%%[asy]%%') AS asy "
        "FROM vector_store ORDER BY embedding <=> '%s' LIMIT %d;" % (lit, lit, n)
    )
    out = subprocess.run(
        ['docker', 'exec', '-i', 'db-local', 'psql', '-U', 'dev', '-d', 'postgres',
         '-t', '-A', '-F', '|', '-c', sql],
        capture_output=True, text=True,
    )
    rows = []
    for line in out.stdout.strip().split('\n'):
        if '|' not in line:
            continue
        sim, t, asy = line.split('|')
        rows.append((float(sim), t.strip(), asy.strip() == 't'))
    if not rows:
        raise SystemExit('psql 결과 0건: ' + out.stderr[:300])
    return rows


def main():
    key = api_key()
    per_q = []
    for q, want in QUESTIONS:
        rows = topn(embed(q, key))
        per_q.append({'q': q, 'want': want, 'rows': rows})
        print(f'· {q[:26]:28s} 최고유사도 {rows[0][0]:.4f}', flush=True)

    print('\n=== 임계값 스윕 (질문 20문항 · topK=5) ===')
    print(f"{'임계값':>6s} {'검색 0건 질문':>12s} {'평균 검색 건수':>13s} {'정답 type 포함':>14s}")
    table = []
    for th in THRESHOLDS:
        zero = 0
        total_hits = 0
        relevant = 0
        for e in per_q:
            passing = [r for r in e['rows'] if r[0] >= th][:TOPK]
            total_hits += len(passing)
            if not passing:
                zero += 1
            if any(t == e['want'] for _, t, _ in passing):
                relevant += 1
        n = len(per_q)
        table.append((th, zero, total_hits / n, relevant))
        print(f'{th:6.1f} {zero:9d}/{n} {total_hits/n:13.2f} {relevant:11d}/{n}')

    json.dump({'questions': per_q, 'table': table},
              open('sweep_raw.json', 'w'), ensure_ascii=False, indent=1)
    sims = sorted(e['rows'][0][0] for e in per_q)
    print(f'\n최고 유사도 분포 — min {sims[0]:.4f} / 중앙값 {sims[len(sims)//2]:.4f} / max {sims[-1]:.4f}')

    # 그림 소스 코드가 섞인 청크가 검색 상위에 오르는 비율과 그때의 유사도
    top5 = [r for e in per_q for r in e['rows'][:TOPK]]
    a = [r[0] for r in top5 if r[2]]
    b = [r[0] for r in top5 if not r[2]]
    corpus_ratio = 787 / 5799
    print(f'\n=== 그림 소스 코드([asy]) 축 ===')
    print(f'코퍼스 내 비율        {corpus_ratio*100:.1f}% (787/5799)')
    print(f'검색 상위 5건 내 비율 {len(a)/len(top5)*100:.1f}% ({len(a)}/{len(top5)})')
    if a and b:
        print(f'평균 유사도 — 그림 포함 {sum(a)/len(a):.4f} / 미포함 {sum(b)/len(b):.4f}')


if __name__ == '__main__':
    main()
