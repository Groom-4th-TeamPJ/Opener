insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2026
       , 'CSAT'
       , '수학능력시험'
       , 30
       , 100);

WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2026 AND exam_type = 'CSAT' LIMIT 1
    )
insert into questions (exam_id
                      , passages
                      , question_no
                      , options
                      , answer
                      , category
                      , point
                      , question_type)
values ((SELECT id FROM exam)
        , '[
            {"order": 1, "type": "TEXT", "content":"$9^{\\frac{1}{4}} \\times 3^{-\\frac{1}{2}}$ 의 값은?"}
        ]'::jsonb
        ,1
        ,'[
            {"order": 1, "content":"1"},
            {"order": 2, "content":"$\\sqrt{3}$"},
            {"order": 3, "content":"3"},
            {"order": 4, "content":"$3 \\sqrt{3}$"},
            {"order": 5, "content":"9"}
        ]'::jsonb
        ,1
        , 'ALG'
        , 2
        , 'MCQ'
    ),
    ((select id from exams)
        , '[
      {"order": 1, "type": "TEXT", "content":"함수 $f(x)=3 x^3+4 x+1$ 에 대하여 $\\lim _{h \\rightarrow 0} \\frac{f(1+h)-f(1)}{h}$ 의 값은?"}
    ]'::jsonb
        ,2
        ,'[
      {"order": 1, "content":"7"},
      {"order": 2, "content":"9"},
      {"order": 3, "content":"11"},
      {"order": 4, "content":"13"},
      {"order": 5, "content":"15"}
    ]'::jsonb
        ,4
        , 'ALG'
        , 2
        , 'MCQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"$$ \\text {수열} \\left\\{a_n\\right\\} \\text{에 대하여} \\sum_{k=1}^4\\left(2 a_k-k\\right)=0 \\text{일 때,} \\sum_{k=1}^4 a_k \\text{의 값은}?$$"}
]'::jsonb
        ,3
        ,'[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
]'::jsonb
        ,5
        , 'ALG'
        , 3
        , 'MCQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"함수"},
  {"order": 2, "type": "TEXT", "content":"$$ f(x)= \\begin{cases}3 x-2 & (x<1) \\\\ x^2-3 x+a & (x \\geq 1)\\end{cases} $$"},
  {"order": 3, "type":  "TEXT", "content":"이 실수 전체의 집합에서 연속일 때, 상수 $a$ 의 값은?"}
]'::jsonb
        ,4
        ,'[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
]'::jsonb
        ,3
        , 'ALG'
        , 3
        , 'MCQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"함수 $f(x)=(x+2)\\left(2 x^2-x-2\\right)$ 에 대하여 $f^{\\prime}(1)$ 의 값은?"}
]'::jsonb
        ,5
        ,'[
  {"order": 1, "content":"6"},
  {"order": 2, "content":"7"},
  {"order": 3, "content":"8"},
  {"order": 4, "content":"9"},
  {"order": 5, "content":"10"}
]'::jsonb
        ,3
        , 'ALG'
        , 3
        , 'MCQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"1 보다 큰 두 실수 $a, b$ 가"},
  {"order": 2, "type": "TEXT", "content":"$$ \\log _a b=3, \\quad \\log _3 \\frac{b}{a}=\\frac{1}{2} $$"},
  {"order": 3, "type": "TEXT", "content":"을 만족시킬 때, $\\log _9 a b$ 의 값은? "}
]'::jsonb
        ,6
        ,'[
  {"order": 1, "content":"$\\frac{3}{8}$"},
  {"order": 2, "content":"$\\frac{1}{2}$"},
  {"order": 3, "content":"$\\frac{5}{8}$"},
  {"order": 4, "content":"$\\frac{3}{4}$"},
  {"order": 5, "content":"$\\frac{7}{8}$"}
]'::jsonb
        ,2
        , 'ALG'
        , 3
        , 'MCQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"시각 $t=0$ 일 때 원점을 출발하여 수직선 위를 움직이는 점 P 가 있다. 실수 $k$ 에 대하여 시각이 $t(t \\geq 0)$ 일 때 점 P 의 속도 $v(t)$ 가"},
  {"order": 2, "type": "TEXT", "content":"$$ v(t)=t^2-k t+4 $$"},
  {"order": 3, "type": "TEXT", "content":"이다. <보기>에서 옳은 것만을 있는 대로 고른 것은? "},
  {"order": 4, "type": "IMAGE", "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/agl11p.jpg"}
]'::jsonb
        ,11
        ,'[
  {"order": 1, "content":"ᄀ"},
  {"order": 2, "content":"ᄀ, ᄂ"},
  {"order": 3, "content":"ᄀ, ᄃ"},
  {"order": 4, "content":"ᄂ, ᄃ"},
  {"order": 5, "content":"ᄀ, ᄂ, ᄃ"}
]'::jsonb
        ,3
        , 'ALG'
        , 4
        , 'MCQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"함수 $f(x)$ 가"},
  {"order": 2, "type": "TEXT", "content":"$$ f(x)= \\begin{cases}-x^2 & (x<0) \\\\ x^2-x & (x \\geq 0)\\end{cases} $$"},
  {"order": 3, "type": "TEXT", "content":"이고, 양수 $a$ 에 대하여 함수 $g(x)$ 를"},
  {"order": 4, "type": "TEXT", "content":"$$ g(x)=\\left\\{\\begin{array}{cl}  a x+a & (x<-1) \\\\   0 & (-1 \\leq x<1) \\\\  a x-a & (x \\geq 1)   \\end{array}\\right.   $$"},
  {"order": 5, "type": "TEXT", "content":"이라 하자. 함수 $h(x)=\\int_0^x(g(t)-f(t)) d t$ 가 오직 하나의 극값을 갖도록 하는 $a$ 의 최댓값을 $k$ 라 하자. $a=k$ 일 때, $k+h(3)$ 의 값은?"}
]'::jsonb
        ,15
        ,'[
  {"order": 1, "content":"$\\frac{9}{2}$"},
  {"order": 2, "content":"$\\frac{11}{2}$"},
  {"order": 3, "content":"$\\frac{13}{2}$"},
  {"order": 4, "content":"$\\frac{15}{2}$"},
  {"order": 5, "content":"$\\frac{17}{2}$"}
]'::jsonb
        ,4
        , 'ALG'
        , 4
        , 'MCQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"함수 $f(x)=4 x^3-2 x$ 의 한 부정적분 $F(x)$ 에 대하여 $F(0)=4$ 일 때, $F(2)$ 의 값을 구하시오."}
]'::jsonb
        ,17
        ,null
        ,16
        , 'ALG'
        , 3
        , 'FRQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"수열 $\\left\\{a_n\\right\\}$ 이 다음 조건을 만족시킨다."},
  {"order": 2, "type": "IMAGE", "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/agl20p.JPG"},
  {"order": 3, "type": "TEXT", "content":"$$ \\text {다음은 } \\sum_{k=1}^{12} a_k+\\sum_{k=1}^5 a_{2 k+1} \\text { 의 값을 구하는 과정이다.} $$"},
  {"order": 4, "type": "IMAGE", "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/agl20p2.JPG"},
  {"order": 5, "type": "TEXT", "content":"위의 (가)에 알맞은 식을 $f(n)$ 이라 하고, (나), (다)에 알맞은 수를 각각 $p, q$ 라 할 때, $\\frac{p \\times q}{f(12)}$ 의 값을 구하시오. "}
]'::jsonb
        ,20
        ,null
        ,130
        , 'ALG'
        , 4
        , 'FRQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"주머니에 숫자 $1,2,3,4,5$ 가 하나씩 적혀 있는 흰 공 5 개와 숫자 $2,3,4,5,6$ 이 하나씩 적혀 있는 검은 공 5 개가 들어 있다. 이 주머니에서 임의로 2 개의 공을 동시에 꺼낼 때, 꺼낸 2 개의 공이 서로 같은 색이거나 꺼낸 2 개의 공에 적힌 수가 서로 같을 확률은? "},
  {"order": 2, "type": "IMAGE", "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/prob25p.JPG"}
]'::jsonb
        ,25
        ,'[
  {"order": 1, "content":"$\\frac{7}{15}$"},
  {"order": 2, "content":"$\\frac{8}{15}$"},
  {"order": 3, "content":"$\\frac{3}{5}$"},
  {"order": 4, "content":"$\\frac{2}{3}$"},
  {"order": 5, "content":"$\\frac{11}{15}$"}
]'::jsonb
        ,2
        , 'PROB'
        , 3
        , 'MCQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"비어 있는 주머니 10 개가 일렬로 놓여 있고, 공 8 개가 있다.\\n각 주머니에 들어 있는 공의 개수가 2 이하가 되도록 공을 주머니에 남김없이 나누어 넣을 때, 다음 조건을 만족시키는 경우의 수를 구하시오. (단, 공끼리는 서로 구별하지 않는다.)"},
  {"order": 2, "type": "IMAGE", "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/prob30p.JPG"}
]'::jsonb
        ,30
        ,null
        ,262
        , 'PROB'
        , 4
        , 'FRQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"$\\int_0^{\\frac{\\pi}{2}} \\sqrt{\\sin x-\\sin ^3 x} d x$ 의 값은?"}
]'::jsonb
        ,24
        ,'[
  {"order": 1, "content":"$\\frac{1}{6}$"},
  {"order": 2, "content":"$\\frac{1}{3}$"},
  {"order": 3, "content":"$\\frac{1}{2}$"},
  {"order": 4, "content":"$\\frac{2}{3}$"},
  {"order": 5, "content":"$\\frac{5}{6}$"}
]'::jsonb
        ,4
        , 'CALC'
        , 3
        , 'MCQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"실수 전체의 집합에서 증가하는 연속함수 $f(x)$ 의 역함수 $f^{-1}(x)$ 가 다음 조건을 만족시킨다."},
  {"order": 2, "type": "IMAGE", "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/calc30p.JPG"},
  {"order": 3, "type": "TEXT", "content":"실수 $m$ 에 대하여 기울기가 $m$ 이고 점 $(1,0)$ 을 지나는 직선이 곡선 $y=f(x)$ 와 만나는 점의 개수를 $g(m)$ 이라 하자. 함수 $g(m)$ 이 $m=a, m=b(a<b)$ 에서 불연속일 때, $g(a) \\times\\left(\\lim _{m \\rightarrow a+} g(m)\\right)+g(b) \\times\\left(\\frac{\\ln b}{b}\\right)^2$ 의 값을 구하시오. (단, $\\lim _{x \\rightarrow \\infty} \\frac{\\ln x}{x}=0$ ) "}
]'::jsonb
        ,30
        ,null
        ,11
        , 'CALC'
        , 4
        , 'FRQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"그림과 같이 $\\overline{\\mathrm{AB}}=\\overline{\\mathrm{CD}}=4, \\overline{\\mathrm{BC}}=\\overline{\\mathrm{BD}}=2 \\sqrt{5}$ 인 사면체 ABCD 가 있고, 점 A 에서 직선 CD 에 내린 수선의 발 H 에 대하여 두 평면 ABH 와 BCD 는 서로 수직이고 $\\overline{\\mathrm{AH}}=4$ 이다. 삼각형 ABH 의 무게중심을 G 라 하고, 점 G 를 중심으로 하고 평면 ACD 에 접하는 구를 $S$ 라 하자. $\\angle \\mathrm{APG}=\\frac{\\pi}{2}$ 인 구 $S$ 위의 모든 점 P 가 나타내는 도형을 $T$ 라 할 때, 도형 $T$ 의 평면 ABC 위로의 정사영의 넓이는?"},
  {"order": 2, "type": "IMAGE", "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/geo28p.JPG"}
]'::jsonb
        ,28
        ,'[
  {"order": 1, "content":"$\\frac{\\pi}{7}$"},
  {"order": 2, "content":"$\\frac{\\pi}{6}$"},
  {"order": 3, "content":"$\\frac{\\pi}{5}$"},
  {"order": 4, "content":"$\\frac{\\pi}{4}$"},
  {"order": 5, "content":"$\\frac{\\pi}{3}$"}
]'::jsonb
        ,4
        , 'GEO'
        , 3
        , 'MCQ'
    )
        , ((select id from exams)
        , '[
  {"order": 1, "type": "TEXT", "content":"그림과 같이 초점이 $\\mathrm{F}(p, 0)(p>0)$ 이고 준선이 $x=-p$ 인 포물선 위의 점 중 제 1 사분면에 있는 점 A 에서 포물선의 준선에 내린 수선의 발을 H 라 하고, 두 초점이 $x$ 축 위에 있고 세 점 $\\mathrm{F}, \\mathrm{A}, \\mathrm{H}$ 를 지나는 타원의 $x$ 좌표가 양수인 초점을 B 라 하자. 삼각형 AHB 의 둘레의 길이가 $p+27$, 넓이가 $2 p+12$ 일 때, 선분 HF 의 길이를 $k$ 라 하자. $k^2$ 의 값을 구하시오."},
  {"order": 2, "type": "IMAGE", "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/geo29p.JPG"}
]'::jsonb
        ,29
        ,null
        ,360
        , 'GEO'
        , 4
        , 'FRQ'
    );



