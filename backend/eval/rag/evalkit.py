"""RAG 평가 보조 함수 (r129 EV-1·EV-2·IX-1·EV-3) - 순수 함수만 둔다, API·DB 호출은 각 스크립트가 맡는다"""
import math
import random
import re

# ---------- 질문 생성 (EV-1) ----------
_WS = re.compile(r"\s+")


def shares_run(query, text, n=30):
    """질문이 원문과 공백을 뺀 n 글자 이상 이어진 구간을 공유하면 True (복사한 질문을 거른다)"""
    q, t = _WS.sub("", query or ""), _WS.sub("", text or "")
    return len(q) >= n and any(q[i:i + n] in t for i in range(len(q) - n + 1))


def stratified_sample(items, n, seed):
    """(type, level) 층마다 돌려 가며 하나씩 뽑아 n 건을 채운다 - 작은 층이 비면 남은 자리는 다른 층이 채운다"""
    groups = {}
    for it in items:
        groups.setdefault((it["type"], it["level"]), []).append(it)
    rng = random.Random(seed)
    for key in groups:
        rng.shuffle(groups[key])
    keys = sorted(groups)
    out = []
    while len(out) < min(n, len(items)):
        for key in keys:
            if groups[key] and len(out) < n:
                out.append(groups[key].pop())
    return out


# ---------- 검색 지표 (EV-2·IX-1) ----------
# run = {"gold": {청크 id}, "ranked": [(id, 유사도), ...]}  유사도 내림차순이 아니어도 된다
def _rows(run, threshold):
    return sorted((r for r in run["ranked"] if r[1] >= threshold), key=lambda r: -r[1])


def recall_at_k(runs, k, threshold):
    """상위 k 건(임계값 이상인 행만)에 금 정답 청크가 하나라도 있는 질문의 비율"""
    if not runs:
        return None
    return sum(1 for r in runs if any(i in r["gold"] for i, _ in _rows(r, threshold)[:k])) / len(runs)


def mrr_at_k(runs, k, threshold):
    if not runs:
        return None
    total = 0.0
    for r in runs:
        for rank, (i, _) in enumerate(_rows(r, threshold)[:k], start=1):
            if i in r["gold"]:
                total += 1 / rank
                break
    return total / len(runs)


def empty_count(runs, threshold):
    return sum(1 for r in runs if not _rows(r, threshold))


def pick_threshold(runs, candidates, k=5, tolerance=0.01):
    """Recall@k 가 0.0 일 때보다 tolerance 넘게 떨어지지 않고 0건 질문이 없는 가장 높은 임계값, 없으면 0.0"""
    base = recall_at_k(runs, k, 0.0)
    best = 0.0
    for t in sorted(candidates):
        if recall_at_k(runs, k, t) >= base - tolerance - 1e-9 and empty_count(runs, t) == 0:
            best = t
    return best


# ---------- Voyage 한도 탐색 (색인 소요 시간 추정) ----------
def batches(token_counts, max_inputs, max_tokens):
    """항목 번호를 입력 수·토큰 상한 안에서 앞에서부터 묶는다 - 상한보다 큰 한 건은 혼자 한 묶음"""
    out, cur, cur_tokens = [], [], 0
    for idx, tok in enumerate(token_counts):
        if cur and (len(cur) >= max_inputs or cur_tokens + tok > max_tokens):
            out.append(cur)
            cur, cur_tokens = [], 0
        cur.append(idx)
        cur_tokens += tok
    if cur:
        out.append(cur)
    return out


def per_minute(records):
    """records: {t(초), tokens, status, wait_s} -> {분: {requests, tokens, n429, wait_s}}"""
    out = {}
    for r in records:
        m = out.setdefault(int(r["t"] // 60), {"requests": 0, "tokens": 0, "n429": 0, "wait_s": 0.0})
        m["requests"] += 1
        m["tokens"] += r["tokens"]
        m["n429"] += 1 if r["status"] == 429 else 0
        m["wait_s"] += r["wait_s"]
    return out


def estimate_index_minutes(total_tokens, tpm, rpm, request_tokens):
    """분당 토큰 한도와 분당 요청 한도 중 느린 쪽으로 계산한 색인 소요 분, 한도를 모르면 None"""
    mins = []
    if tpm:
        mins.append(total_tokens / tpm)
    if rpm:
        mins.append(math.ceil(total_tokens / request_tokens) / rpm)
    return max(mins) if mins else None


# ---------- 답 평가 (EV-3) ----------
def extract_boxed(text):
    """마지막 \\boxed{...} 안쪽을 중첩 중괄호까지 꺼낸다 - 없거나 닫히지 않으면 None"""
    if not text:
        return None
    start = text.rfind("\\boxed{")
    if start < 0:
        return None
    depth, begin = 0, start + len("\\boxed")
    for i in range(begin, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[begin + 1:i]
    return None


def normalize_answer(s):
    """공백 제거, \\left \\right 제거, \\dfrac -> \\frac (브리프가 정한 세 가지만)"""
    s = s.replace("\\left", "").replace("\\right", "").replace("\\dfrac", "\\frac")
    return _WS.sub("", s)


def answers_match(got, gold):
    return got is not None and gold is not None and normalize_answer(got) == normalize_answer(gold)


def cost_usd(input_tokens, output_tokens, price_in_per_m, price_out_per_m):
    return input_tokens / 1e6 * price_in_per_m + output_tokens / 1e6 * price_out_per_m


# ---------- 누출 판정 (EV-3: MATH-500 은 색인된 MATH 테스트 5,000건의 부분집합일 수 있다) ----------
_MATH = re.compile(r"\$([^$]+)\$|\\\((.+?)\\\)|\\\[(.+?)\\\]", re.DOTALL)
MIN_MATH_CHARS = 12  # shortcut: 수식이 이보다 짧은 문제는 우연 일치와 구분이 안 돼 누출로 세지 않는다, 그런 문제가 많아지면 문장 유사도를 함께 본다


def _fragments(text):
    return {_WS.sub("", a or b or c) for a, b, c in _MATH.findall(text or "")}


def leaks(held_problem, chunk_text, threshold):
    """held 문제의 수식 조각(길이 가중) 중 threshold 이상이 청크에도 있으면 같은 문제로 본다 (번역이 달라도 수식은 같다)"""
    held, chunk = _fragments(held_problem), _fragments(chunk_text)
    total = sum(len(f) for f in held)
    if total < MIN_MATH_CHARS:
        return False
    return sum(len(f) for f in held & chunk) / total >= threshold
