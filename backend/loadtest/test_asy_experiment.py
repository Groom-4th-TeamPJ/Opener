"""[asy] 실험 보조 함수 시험 - 블록 제거(로더와 같은 규칙)·로더와 같은 본문 조립·Hit@5·최고 유사도·상위 5건의 [asy] 비율"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import asy_experiment  # noqa: E402


class StripAsyTest(unittest.TestCase):
    def test_여러_블록을_한_본문에서_모두_걷어낸다(self):
        text = "문제 가\n[asy]\ndraw(a);\n[/asy]\n중간 글\n[asy]draw(b);[/asy]\n끝 글"
        out, unclosed = asy_experiment.strip_asy(text)
        self.assertNotIn("[asy]", out)
        self.assertNotIn("draw", out)
        self.assertEqual(0, unclosed)

    def test_블록_앞뒤_글은_남긴다(self):
        out, _ = asy_experiment.strip_asy("앞 글\n[asy]x[/asy]\n뒤 글")
        self.assertIn("앞 글", out)
        self.assertIn("뒤 글", out)

    def test_블록을_지우고_남은_빈_줄_셋_이상은_둘로_접고_양끝_공백을_자른다(self):
        out, _ = asy_experiment.strip_asy("앞\n\n[asy]x[/asy]\n\n\n뒤\n")
        self.assertEqual("앞\n\n뒤", out)

    def test_닫히지_않은_블록은_지우지_않고_개수로_보고한다(self):
        text = "앞 글 [asy] draw(a); 뒤 글 전부"
        out, unclosed = asy_experiment.strip_asy(text)
        self.assertEqual(text, out)
        self.assertEqual(1, unclosed)

    def test_닫힌_블록과_닫히지_않은_블록이_섞이면_닫힌_것만_지우고_나머지를_보고한다(self):
        out, unclosed = asy_experiment.strip_asy("가 [asy]a[/asy] 나 [asy] b 다")
        self.assertEqual("가  나 [asy] b 다", out)
        self.assertEqual(1, unclosed)

    def test_asy_가_없으면_그대로(self):
        self.assertEqual(("식 $x^2$", 0), asy_experiment.strip_asy("식 $x^2$"))
        self.assertEqual(("", 0), asy_experiment.strip_asy(None))


class BuildContentTest(unittest.TestCase):
    NODE = {"problem_ko": "문제 [asy]p[/asy] 끝", "solution_ko": "풀이", "level": "Level 2", "type": "Geometry"}

    def test_로더와_같은_형식으로_type_level_문제_풀이를_잇는다(self):
        c = asy_experiment.build_content({"problem_ko": "P", "solution_ko": "S", "level": "Level 1", "type": "Algebra"}, strip=True)
        self.assertEqual("[Algebra] Level 1\n\n## 문제\nP\n\n## 풀이\nS", c)

    def test_strip_False_는_블록을_남기고_True_는_걷어낸다(self):
        self.assertIn("[asy]p[/asy]", asy_experiment.build_content(self.NODE, strip=False))
        stripped = asy_experiment.build_content(self.NODE, strip=True)
        self.assertNotIn("[asy]", stripped)
        self.assertIn("문제  끝", stripped)

    def test_problem_ko_가_비면_problem_으로_subject_는_type_이_없을_때(self):
        c = asy_experiment.build_content({"problem_ko": " ", "problem": "영문", "solution": "sol", "level": "L", "subject": "Algebra"}, strip=True)
        self.assertEqual("[Algebra] L\n\n## 문제\n영문\n\n## 풀이\nsol", c)

    def test_본문이_둘_다_비면_None(self):
        self.assertIsNone(asy_experiment.build_content({"problem_ko": "[asy]x[/asy]", "solution_ko": "", "type": "A", "level": "L"}, strip=True))

    def test_asy_로_표시한다(self):
        self.assertTrue(asy_experiment.has_asy(self.NODE))
        self.assertFalse(asy_experiment.has_asy({"problem_ko": "p", "solution_ko": "s"}))


def rows(*sims_types):
    """(유사도, type, asy) 목록 - 유사도 내림차순 상위 n건 그대로"""
    return [(s, t, a) for s, t, a in sims_types]


class HitAt5Test(unittest.TestCase):
    ROWS = rows((0.50, "Algebra", False), (0.40, "Geometry", False), (0.30, "Geometry", True),
                (0.25, "Prealgebra", False), (0.10, "Algebra", False), (0.05, "Algebra", False))

    def test_상위_5건_안에_기대_type_이_있으면_맞힘(self):
        self.assertTrue(asy_experiment.hit_at_5(self.ROWS, "Prealgebra", 0.0))

    def test_6번째_행은_세지_않는다(self):
        rs = rows((0.9, "A", False), (0.8, "A", False), (0.7, "A", False), (0.6, "A", False), (0.5, "A", False), (0.4, "Algebra", False))
        self.assertFalse(asy_experiment.hit_at_5(rs, "Algebra", 0.0))

    def test_임계값_밑의_행은_세지_않는다(self):
        self.assertFalse(asy_experiment.hit_at_5(self.ROWS, "Geometry", 0.45))
        self.assertTrue(asy_experiment.hit_at_5(self.ROWS, "Algebra", 0.0))
        self.assertFalse(asy_experiment.hit_at_5(rows((0.5, "Geometry", False), (0.1, "Algebra", False)), "Algebra", 0.2))

    def test_임계값과_같은_유사도는_통과(self):
        self.assertTrue(asy_experiment.hit_at_5(rows((0.2, "Algebra", False)), "Algebra", 0.2))

    def test_임계값을_넘는_행이_없으면_맞히지_못함(self):
        self.assertFalse(asy_experiment.hit_at_5(rows((0.1, "Algebra", False)), "Algebra", 0.2))
        self.assertFalse(asy_experiment.hit_at_5([], "Algebra", 0.0))

    def test_임계값은_통과한_행에서_5건을_자른_뒤에_본다(self):
        # 0.3 이상은 6건, 상위 5건 밖의 Algebra 는 세지 않는다
        rs = rows(*[(0.9 - i * 0.1, "A", False) for i in range(5)], (0.35, "Algebra", False))
        self.assertFalse(asy_experiment.hit_at_5(rs, "Algebra", 0.3))


class AggregateTest(unittest.TestCase):
    PER_Q = [
        {"want": "Algebra", "rows": rows((0.6, "Algebra", False), (0.5, "Geometry", True))},
        {"want": "Geometry", "rows": rows((0.4, "Algebra", True), (0.3, "Algebra", False))},
        {"want": "Number Theory", "rows": rows((0.2, "Geometry", False))},
    ]

    def test_Hit_5_는_질문_수를_센다(self):
        self.assertEqual(1, asy_experiment.hit_count(self.PER_Q, 0.0))  # Algebra 만 맞힘 (Geometry 질문은 2위가 아니라 type 불일치)
        self.assertEqual(1, asy_experiment.hit_count(self.PER_Q, 0.2))

    def test_질문별_최고_유사도의_중앙값과_최댓값(self):
        s = asy_experiment.best_similarity_stats(self.PER_Q)
        self.assertEqual(0.4, s["median"])
        self.assertEqual(0.6, s["max"])
        self.assertEqual(0.2, s["min"])

    def test_짝수_개_질문의_중앙값은_가운데_둘의_평균(self):
        per_q = [{"want": "A", "rows": rows((v, "A", False))} for v in (0.2, 0.4, 0.6, 0.8)]
        self.assertAlmostEqual(0.5, asy_experiment.best_similarity_stats(per_q)["median"])

    def test_상위_5건_중_asy_비율(self):
        share = asy_experiment.asy_share_top5(self.PER_Q)
        self.assertEqual(2, share["asy"])
        self.assertEqual(5, share["rows"])
        self.assertAlmostEqual(0.4, share["share"])

    def test_상위_5건_비율은_임계값을_따르지_않고_상위_5건_전체(self):
        per_q = [{"want": "A", "rows": rows(*[(0.9 - i * 0.1, "A", i == 5) for i in range(7)])}]
        share = asy_experiment.asy_share_top5(per_q)
        self.assertEqual((0, 5), (share["asy"], share["rows"]))

    def test_질문이_없으면_비율은_None(self):
        self.assertIsNone(asy_experiment.asy_share_top5([])["share"])


if __name__ == "__main__":
    unittest.main()
