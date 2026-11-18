"""RAG 평가 보조 함수 시험 (r129 EV-1·EV-2·IX-1·EV-3) - 질문 복사 검사·층화 표본·Recall/MRR·임계값 규칙·분당 집계·\\boxed 정규화"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import evalkit  # noqa: E402


class CopyCheckTest(unittest.TestCase):
    def test_r129_30자_이상_이어진_구간이_원문에_있으면_복사로_본다(self):
        gold = "함수 f(x)=x^2+3x+2 의 그래프와 직선 y=2x+5 가 만나는 점의 개수를 구하시오 " * 2
        q = "선생님 " + gold[3:60] + " 이 문제 도와주세요"
        self.assertTrue(evalkit.shares_run(q, gold, 30))

    def test_r129_29자까지만_겹치면_복사가_아니다(self):
        gold = "가나다라마바사아자차카타파하" * 4          # 56자, 같은 14자 주기
        q = gold[:29] + "XYZ" + gold[:5]
        self.assertFalse(evalkit.shares_run(q, gold, 30))

    def test_r129_공백_차이는_무시하고_비교한다(self):
        gold = "직각삼각형의  두 변의 길이가 3 과 4 일 때 빗변의 길이를 구하고 넓이도 함께 구하시오"
        q = "직각삼각형의 두 변의 길이가 3 과 4 일 때 빗변의 길이를 구하고 넓이도 함께 구하시오"
        self.assertTrue(evalkit.shares_run(q, gold, 30))

    def test_r129_질문이_30자보다_짧으면_복사가_아니다(self):
        self.assertFalse(evalkit.shares_run("짧은 질문", "짧은 질문 그리고 더 긴 원문", 30))


class StratifiedSampleTest(unittest.TestCase):
    ITEMS = [{"id": f"{t}-{l}-{i}", "type": t, "level": l}
             for t in ("A", "B", "C") for l in ("Level 1", "Level 2") for i in range(10)]

    def test_r129_모든_층에서_고르게_뽑고_개수를_채운다(self):
        s = evalkit.stratified_sample(self.ITEMS, 12, seed=1)
        self.assertEqual(12, len(s))
        strata = {(x["type"], x["level"]) for x in s}
        self.assertEqual(6, len(strata))
        for st in strata:
            self.assertEqual(2, sum(1 for x in s if (x["type"], x["level"]) == st))

    def test_r129_같은_씨앗이면_같은_결과(self):
        a = evalkit.stratified_sample(self.ITEMS, 15, seed=7)
        b = evalkit.stratified_sample(self.ITEMS, 15, seed=7)
        self.assertEqual([x["id"] for x in a], [x["id"] for x in b])

    def test_r129_작은_층은_있는_만큼만_주고_남은_자리는_다른_층이_채운다(self):
        items = [{"id": "s1", "type": "S", "level": "L"}] + [{"id": f"b{i}", "type": "B", "level": "L"} for i in range(9)]
        s = evalkit.stratified_sample(items, 6, seed=1)
        self.assertEqual(6, len(s))
        self.assertIn("s1", [x["id"] for x in s])
        self.assertEqual(len({x["id"] for x in s}), 6)

    def test_r129_요청_수가_전체보다_크면_전체(self):
        self.assertEqual(len(self.ITEMS), len(evalkit.stratified_sample(self.ITEMS, 999, seed=1)))


def run(gold, ranked):
    return {"gold": set(gold), "ranked": ranked}


class RetrievalMetricsTest(unittest.TestCase):
    RUNS = [
        run({"a"}, [("a", 0.9), ("x", 0.5)]),                  # 1위
        run({"b"}, [("x", 0.8), ("y", 0.7), ("b", 0.6)]),      # 3위
        run({"c1", "c2"}, [("x", 0.8), ("c2", 0.4)]),          # 2위 (금 정답 둘 중 하나만 있어도 맞힘)
        run({"d"}, [("x", 0.8), ("y", 0.7), ("z", 0.6), ("w", 0.5), ("v", 0.4), ("d", 0.3)]),  # 6위
    ]

    def test_r129_Recall_5_는_상위_5건에_금_정답이_하나라도_있는_질문_비율(self):
        self.assertAlmostEqual(3 / 4, evalkit.recall_at_k(self.RUNS, 5, 0.0))

    def test_r129_MRR_10_은_첫_금_정답_순위의_역수_평균(self):
        self.assertAlmostEqual((1 + 1 / 3 + 1 / 2 + 1 / 6) / 4, evalkit.mrr_at_k(self.RUNS, 10, 0.0))

    def test_r129_MRR_은_k_밖의_순위를_0으로_센다(self):
        self.assertAlmostEqual((1 + 1 / 3 + 1 / 2) / 4, evalkit.mrr_at_k(self.RUNS, 5, 0.0))

    def test_r129_임계값_밑의_행은_순위에서_빠진다(self):
        # 임계 0.55 -> 둘째 질문은 b(0.6)가 3위 그대로, 셋째 질문은 c2(0.4)가 걸러져 빗나감
        self.assertAlmostEqual(2 / 4, evalkit.recall_at_k(self.RUNS, 5, 0.55))

    def test_r129_임계값과_같은_유사도는_통과한다(self):
        self.assertAlmostEqual(1.0, evalkit.recall_at_k([run({"a"}, [("a", 0.2)])], 5, 0.2))

    def test_r129_결과가_0건인_질문을_센다(self):
        self.assertEqual(0, evalkit.empty_count(self.RUNS, 0.0))
        self.assertEqual(3, evalkit.empty_count(self.RUNS, 0.85))  # 0.9 가 첫 질문에만 있고 나머지 셋은 0.8 이하

    def test_r129_질문이_없으면_None(self):
        self.assertIsNone(evalkit.recall_at_k([], 5, 0.0))


class PickThresholdTest(unittest.TestCase):
    def runs(self):
        # 정답 유사도 0.5/0.45/0.4/0.3, 오답이 0.35 까지 섞임 - 임계값을 올리면 정답이 먼저 떨어진다
        return [run({"a"}, [("a", 0.5)]), run({"b"}, [("b", 0.45)]), run({"c"}, [("c", 0.4)]), run({"d"}, [("d", 0.3)])]

    def test_r129_Recall_이_1점_안에서_유지되고_빈_질문이_없는_가장_높은_임계값(self):
        t = evalkit.pick_threshold(self.runs(), [0.0, 0.1, 0.2, 0.3, 0.4, 0.5], k=5, tolerance=0.01)
        self.assertEqual(0.3, t)

    def test_r129_허용_오차를_키우면_더_높은_임계값을_고른다(self):
        # 앞 셋은 0.2 짜리 정답 위에 0.9 짜리 오답이 있다 - 임계값 0.5 이상이면 Recall 이 1.0 -> 0.25 로 떨어지지만 빈 질문은 없다
        runs = [run({"a"}, [("x", 0.9), ("a", 0.2)]), run({"b"}, [("x", 0.9), ("b", 0.2)]),
                run({"c"}, [("x", 0.9), ("c", 0.2)]), run({"d"}, [("d", 0.9)])]
        cands = [0.0, 0.2, 0.5, 0.9]
        self.assertEqual(0.2, evalkit.pick_threshold(runs, cands, k=5, tolerance=0.01))
        self.assertEqual(0.9, evalkit.pick_threshold(runs, cands, k=5, tolerance=0.80))

    def test_r129_빈_질문이_생기면_Recall_이_허용_안이어도_그_임계값은_버린다(self):
        # 임계값 0.4 에서 넷째 질문(정답 0.3)이 0건 -> 허용 오차 0.30 이어도 0.4 는 못 고른다
        t = evalkit.pick_threshold(self.runs(), [0.0, 0.1, 0.2, 0.3, 0.4, 0.5], k=5, tolerance=0.30)
        self.assertEqual(0.3, t)

    def test_r129_0_0_밖에_조건을_만족하는_값이_없으면_0_0(self):
        runs = [run({"a"}, [("x", 0.5), ("a", 0.1)]), run({"b"}, [("b", 0.2)])]
        self.assertEqual(0.0, evalkit.pick_threshold(runs, [0.0, 0.2, 0.4], k=5, tolerance=0.0))


class BatchingTest(unittest.TestCase):
    def test_r129_입력_수와_토큰_상한으로_묶는다(self):
        self.assertEqual([[0, 1], [2, 3], [4]], evalkit.batches([10] * 5, max_inputs=2, max_tokens=1000))
        self.assertEqual([[0, 1, 2], [3, 4]], evalkit.batches([10, 10, 10, 25, 15], max_inputs=10, max_tokens=40))

    def test_r129_한_건이_상한보다_커도_혼자_한_묶음으로_보낸다(self):
        self.assertEqual([[0], [1], [2]], evalkit.batches([5, 500, 5], max_inputs=10, max_tokens=100))


class PerMinuteTest(unittest.TestCase):
    def test_r129_분당_요청_토큰_429_대기를_센다(self):
        recs = [
            {"t": 0.0, "tokens": 1000, "status": 200, "wait_s": 0.0},
            {"t": 30.0, "tokens": 2000, "status": 200, "wait_s": 0.0},
            {"t": 59.9, "tokens": 0, "status": 429, "wait_s": 20.0},
            {"t": 61.0, "tokens": 500, "status": 200, "wait_s": 0.0},
        ]
        m = evalkit.per_minute(recs)
        self.assertEqual({0, 1}, set(m))
        self.assertEqual({"requests": 3, "tokens": 3000, "n429": 1, "wait_s": 20.0}, m[0])
        self.assertEqual({"requests": 1, "tokens": 500, "n429": 0, "wait_s": 0.0}, m[1])

    def test_r129_색인_1회_소요_시간은_토큰_한도와_요청_한도_중_느린_쪽(self):
        # 120만 토큰, 분당 1만 토큰 -> 120분 / 분당 3회 요청에 요청당 5천 토큰 -> 최대 1.5만 토큰/분이라 토큰 한도가 더 느림
        self.assertAlmostEqual(120.0, evalkit.estimate_index_minutes(1_200_000, tpm=10_000, rpm=3, request_tokens=5_000))
        # 분당 토큰 한도가 넉넉하고 요청 한도가 3회뿐이면 요청 수가 병목: 120만/2천 = 600회 / 3 = 200분
        self.assertAlmostEqual(200.0, evalkit.estimate_index_minutes(1_200_000, tpm=10_000_000, rpm=3, request_tokens=2_000))

    def test_r129_한도를_모르면_None(self):
        self.assertIsNone(evalkit.estimate_index_minutes(1_000, tpm=None, rpm=None, request_tokens=100))


class BoxedTest(unittest.TestCase):
    def test_r129_마지막_boxed_를_중첩_중괄호까지_꺼낸다(self):
        self.assertEqual("\\frac{14}{3}", evalkit.extract_boxed("앞 \\boxed{1} 뒤 \\boxed{\\frac{14}{3}} 끝"))
        self.assertEqual("\\left( 3, \\frac{\\pi}{2} \\right)", evalkit.extract_boxed("답 $\\boxed{\\left( 3, \\frac{\\pi}{2} \\right)}$이다"))

    def test_r129_boxed_가_없거나_닫히지_않으면_None(self):
        self.assertIsNone(evalkit.extract_boxed("답은 3 이다"))
        self.assertIsNone(evalkit.extract_boxed("\\boxed{\\frac{1}{2"))
        self.assertIsNone(evalkit.extract_boxed(None))

    def test_r129_정규화는_공백_left_right_dfrac_을_맞춘다(self):
        a = evalkit.normalize_answer("\\left( 3, \\dfrac{\\pi}{2} \\right)")
        b = evalkit.normalize_answer("(3,\\frac{\\pi}{2})")
        self.assertEqual(b, a)

    def test_r129_값이_다르면_일치하지_않는다(self):
        self.assertFalse(evalkit.answers_match("\\frac{1}{2}", "\\frac{1}{3}"))
        self.assertTrue(evalkit.answers_match("\\dfrac{1}{2}", "\\frac{1}{2}"))
        self.assertFalse(evalkit.answers_match(None, "3"))
        self.assertFalse(evalkit.answers_match("3", None))

    def test_r129_비용은_입력_출력_토큰_단가의_합(self):
        self.assertAlmostEqual(3.0 + 7.5, evalkit.cost_usd(1_000_000, 500_000, 3.0, 15.0))


class LeakTest(unittest.TestCase):
    def test_r129_같은_문제의_다른_번역은_수식_조각이_겹쳐_누출로_본다(self):
        held = "$y=\\frac{2}{x^2+x-6}$의 그래프는 수직 점근선을 몇 개 가집니까? $x=2$ 와 $x=-3$"
        chunk = "[Algebra] Level 3\n\n## 문제\n함수 $y=\\frac{2}{x^2+x-6}$ 의 그래프가 갖는 수직 점근선 수는? $x=2$ , $x=-3$\n\n## 풀이\n..."
        self.assertTrue(evalkit.leaks(held, chunk, 0.6))

    def test_r129_수식이_다른_문제는_누출이_아니다(self):
        held = "$y=\\frac{2}{x^2+x-6}$의 점근선 수"
        chunk = "## 문제\n$\\sqrt{50}+\\sqrt{18}$ 을 간단히 하시오\n## 풀이\n$8\\sqrt{2}$"
        self.assertFalse(evalkit.leaks(held, chunk, 0.6))

    def test_r129_수식이_하나도_없으면_누출로_세지_않는다(self):
        self.assertFalse(evalkit.leaks("그냥 글만 있는 문제", "그냥 글만 있는 문제와 풀이", 0.6))


class PercentileTest(unittest.TestCase):
    def test_r129_백분위는_가까운_순위_방식(self):
        v = list(range(1, 101))
        self.assertEqual(50, evalkit.percentile(v, 50))
        self.assertEqual(95, evalkit.percentile(v, 95))
        self.assertEqual(7, evalkit.percentile([7], 95))
        self.assertIsNone(evalkit.percentile([], 50))

    def test_r129_정렬되지_않은_입력도_받는다(self):
        self.assertEqual(3, evalkit.percentile([5, 1, 3, 2, 4], 50))


class OverlapTest(unittest.TestCase):
    def test_r129_상위_5건_겹침_비율은_정확_탐색_5건_중_근사_탐색이_찾은_수(self):
        self.assertAlmostEqual(0.8, evalkit.overlap_at_k(["a", "b", "c", "d", "x"], ["a", "b", "c", "d", "e"], 5))
        self.assertAlmostEqual(1.0, evalkit.overlap_at_k(["e", "d", "c", "b", "a"], ["a", "b", "c", "d", "e"], 5))

    def test_r129_근사_탐색이_5건보다_적게_돌려주면_그만큼_못_찾은_것(self):
        self.assertAlmostEqual(0.4, evalkit.overlap_at_k(["a", "b"], ["a", "b", "c", "d", "e"], 5))

    def test_r129_정확_탐색_결과가_없으면_None(self):
        self.assertIsNone(evalkit.overlap_at_k(["a"], [], 5))


if __name__ == "__main__":
    unittest.main()
