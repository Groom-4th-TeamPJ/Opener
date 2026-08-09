#!/usr/bin/env python3
"""PORTFOLIO.html STAR 섹션 분량 측정

경계는 다음 <h3 class="star-title"> 또는 다음 <h2 class="proj-title"> 중 먼저 오는 것
h3 만 경계로 쓰면 각 프로젝트의 마지막 STAR 가 뒤따르는 프로젝트 헤더·아키텍처 SVG 를 흡수한다
(2026-07-30 기준 ai-5 가 13,366자로 5,137자 부풀어 보였던 원인)

사용법: python3 portfolio_section_sizes.py [PORTFOLIO.html 경로]
"""
import re
import sys
from pathlib import Path

DEFAULT = Path(__file__).resolve().parent.parent / "docs" / "portfolio" / "PORTFOLIO.html"


def sections(html):
    marks = [
        (m.start(), m.group(0))
        for m in re.finditer(
            r'<h3 class="star-title" id="([^"]+)"|<h2 class="proj-title"', html
        )
    ]
    for i, (pos, tag) in enumerate(marks):
        if "h2" in tag:
            continue
        end = marks[i + 1][0] if i + 1 < len(marks) else len(html)
        yield re.search(r'id="([^"]+)"', tag).group(1), html[pos:end]


def strip_tags(s):
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", s)).strip()


def main():
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT
    html = path.read_text(encoding="utf-8")
    rows = []
    for sid, sec in sections(html):
        svgs = re.findall(r"<svg.*?</svg>", sec, flags=re.S)
        prose = strip_tags(re.sub(r"<svg.*?</svg>", "", sec, flags=re.S))
        ol = re.search(r'<ol class="bul-num">(.*?)</ol>', sec, flags=re.S)
        body = ol.group(1) if ol else ""
        rows.append((sid, len(sec), len(prose), len(svgs), body.count("<li>"), len(strip_tags(body))))

    rows.sort(key=lambda r: -r[1])
    print(f"문서 전체 {len(html):,}자 · 섹션 {len(rows)}개\n")
    print(f"{'id':8s}{'전체':>8s}{'산문':>8s}{'viz':>5s}{'항목':>6s}{'해결과정':>9s}")
    for r in rows:
        print(f"{r[0]:8s}{r[1]:8d}{r[2]:8d}{r[3]:5d}{r[4]:6d}{r[5]:9d}")
    print("\n기준: 전체 8,000자 내외 / 해결과정 6~7항목 · 1,100~1,400자 / viz-frame 1개")

    # 경어체 종결은 0개여야 한다 (개조식 문서)
    # star-pitch(15초 요약)는 면접에서 입으로 말할 문장이라 존댓말 구어체가 규약상 예외다
    # 이 블록을 빼지 않으면 정상 상태에서 매번 위반이 뜨고, 그러면 이 검사 자체가 무시된다
    pitches = re.findall(r'<p class="star-pitch">.*?</p>', html, flags=re.S)
    body = html
    for p in pitches:
        body = body.replace(p, "")
    honorific = len(re.findall(r"(습니다|합니다)", strip_tags(body)))
    print(
        f"경어체 종결: {honorific}개 (star-pitch {len(pitches)}건 제외)"
        + ("" if honorific == 0 else "  ← 개조식 위반")
    )


if __name__ == "__main__":
    main()
