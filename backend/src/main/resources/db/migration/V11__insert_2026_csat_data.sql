insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2026
       , 'CSAT'
       , '수학능력시험'
       , 30
       , 6000);

DELETE FROM questions WHERE exam_id = (
    SELECT id FROM exams WHERE exam_year = 2026 AND exam_type = 'CSAT'
    );

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
values (
         (SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content": "\\(9^{\\frac{1}{4}} \\times 3^{-\\frac{1}{2}}\\) 의 값은?"}
]'::jsonb
       , 1
       , '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "\\(\\sqrt{3}\\)"}
, {"order": 3, "content": "3"}
, {"order": 4, "content": "\\(3 \\sqrt{3}\\)"}
, {"order": 5, "content": "9"}
]'::jsonb
       , 1
       , 'ALG'
       , 2
       , 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=3 x^3+4 x+1\\) 에 대하여 \\(\\lim _{h \\rightarrow 0} \\frac{f(1+h)-f(1)}{h}\\) 의 값은?"}
]'::jsonb
, 2
, '[
  {"order": 1, "content": "7"}
, {"order": 2, "content": "9"}
, {"order": 3, "content": "11"}
, {"order": 4, "content": "13"}
, {"order": 5, "content": "15"}
]'::jsonb
, 4
, 'ALG'
, 2
, 'MCQ'
)

     , (
         (SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 \\(\\sum_{k=1}^4\\left(2 a_k-k\\right)=0\\) 일 때, \\(\\sum_{k=1}^4 a_k\\) 의 값은?"}
]'::jsonb
       , 3
       , '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "2"}
, {"order": 3, "content": "3"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "5"}
]'::jsonb
       , 5
       , 'ALG'
       , 3
       , 'MCQ'
)

     , (
         (SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content": "함수"}
, {"order": 2, "type": "TEXT", "content": "\\[f(x)= \\begin{cases}3 x-2 & (x<1) \\\\ x^2-3 x+a & (x \\geq 1)\\end{cases}\\]"}
, {"order": 3, "type": "TEXT", "content": "이 실수 전체의 집합에서 연속일 때, 상수 \\(a\\) 의 값은?"}
]'::jsonb
       , 4
       , '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "2"}
, {"order": 3, "content": "3"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "5"}
]'::jsonb
       , 3
       , 'ALG'
       , 3
       , 'MCQ'
)

     , (
         (SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=(x+2)\\left(2 x^2-x-2\\right)\\) 에 대하여 \\(f^{\\prime}(1)\\) 의 값은?"}
]'::jsonb
       , 5
       , '[
  {"order": 1, "content": "6"}
, {"order": 2, "content": "7"}
, {"order": 3, "content": "8"}
, {"order": 4, "content": "9"}
, {"order": 5, "content": "10"}
]'::jsonb
       , 3
       , 'ALG'
       , 3
       , 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "1 보다 큰 두 실수 \\(a, b\\) 가"}
, {"order": 2, "type": "TEXT", "content": "\\[\\log _a b=3, \\quad \\log _3 \\frac{b}{a}=\\frac{1}{2}\\]"}
, {"order": 3, "type": "TEXT", "content": "을 만족시킬 때, \\(\\log _9 a b\\) 의 값은?"}
]'::jsonb
, 6
, '[
  {"order": 1, "content": "\\(\\frac{3}{8}\\)"}
, {"order": 2, "content": "\\(\\frac{1}{2}\\)"}
, {"order": 3, "content": "\\(\\frac{5}{8}\\)"}
, {"order": 4, "content": "\\(\\frac{3}{4}\\)"}
, {"order": 5, "content": "\\(\\frac{7}{8}\\)"}
]'::jsonb
, 2
, 'ALG'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "두 곡선 \\(y=x^2+3, y=-\\frac{1}{5} x^2+3\\) 과 직선 \\(x=2\\) 로 둘러싸인 부분의 넓이는?"}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/alg7p.JPG"}
]'::jsonb
, 7
, '[
  {"order": 1, "content": "\\(\\frac{18}{5}\\)"}
, {"order": 2, "content": "\\(\\frac{7}{2}\\)"}
, {"order": 3, "content": "\\(\\frac{17}{5}\\)"}
, {"order": 4, "content": "\\(\\frac{33}{10}\\)"}
, {"order": 5, "content": "\\(\\frac{16}{5}\\)"}
]'::jsonb
, 5
, 'ALG'
, 3
, 'MCQ'
)
  ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "\\(\\sin \\theta+3 \\cos \\theta=0\\) 이고 \\(\\cos (\\pi-\\theta)>0\\) 일 때, \\(\\sin \\theta\\) 의 값은?"}
]'::jsonb
, 8
, '[
  {"order": 1, "content": "\\(\\frac{3 \\sqrt{10}}{10}\\)"}
, {"order": 2, "content": "\\(\\frac{\\sqrt{10}}{5}\\)"}
, {"order": 3, "content": "0"}
, {"order": 4, "content": "\\(-\\frac{\\sqrt{10}}{5}\\)"}
, {"order": 5, "content": "\\(-\\frac{3 \\sqrt{10}}{10}\\)"}
]'::jsonb
, 1
, 'ALG'
, 3
, 'MCQ'
)  ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "양수 \\(a\\) 에 대하여 함수 \\(f(x)\\) 를"}
, {"order": 2, "type": "TEXT", "content": "\\[f(x)=x^3+3 a x^2-9 a^2 x+4\\]"}
, {"order": 3, "type": "TEXT", "content": "라 하자. 직선 \\(y=5\\) 가 곡선 \\(y=f(x)\\) 에 접할 때, \\(f(2)\\) 의 값은?"}
]'::jsonb
, 9
, '[
  {"order": 1, "content": "11"}
, {"order": 2, "content": "12"}
, {"order": 3, "content": "13"}
, {"order": 4, "content": "14"}
, {"order": 5, "content": "15"}
]'::jsonb
, 4
, 'ALG'
, 4
, 'MCQ'
)


     ,(
        (SELECT id FROM exam)
      , '[
  {"order": 1, "type": "TEXT", "content": "상수 \\(a(a>1)\\) 에 대하여 곡선 \\(y=a^x-2\\) 위의 점 중 제 1 사분면에 있는 점 A 를 지나고 \\(y\\) 축에 평행한 직선이 \\(x\\) 축과 만나는 점을 B , 곡선 \\(y=a^x-2\\) 의 점근선과 만나는 점을 C 라 하자. \\(\\overline{\\mathrm{AB}}=\\overline{\\mathrm{BC}}\\) 이고 삼각형 AOC 의 넓이가 8 일 때, \\(a \\times \\overline{\\mathrm{OB}}\\) 의 값은? (단, O 는 원점이다.)"}
]'::jsonb
      , 10
      , '[
  {"order": 1, "content": "\\(2^{\\frac{13}{6}}\\)"}
, {"order": 2, "content": "\\(2^{\\frac{7}{3}}\\)"}
, {"order": 3, "content": "\\(2^{\\frac{5}{2}}\\)"}
, {"order": 4, "content": "\\(2^{\\frac{8}{3}}\\)"}
, {"order": 5, "content": "\\(2^{\\frac{17}{6}}\\)"}
]'::jsonb
      , 3
      , 'ALG'
      , 4
      , 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "시각 \\(t=0\\) 일 때 원점을 출발하여 수직선 위를 움직이는 점 P 가 있다. 실수 \\(k\\) 에 대하여 시각이 \\(t(t \\geq 0)\\) 일 때 점 P 의 속도 \\(v(t)\\) 가"}
, {"order": 2, "type": "TEXT", "content": "\\[v(t)=t^2-k t+4\\]"}
, {"order": 3, "type": "TEXT", "content": "이다. <보기>에서 옳은 것만을 있는 대로 고른 것은?"}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/alg11p.JPG"}
]'::jsonb
, 11
, '[
  {"order": 1, "content": "ᄀ"}
, {"order": 2, "content": "ᄀ, ᄂ"}
, {"order": 3, "content": "ᄀ, ᄃ"}
, {"order": 4, "content": "ᄂ, ᄃ"}
, {"order": 5, "content": "ᄀ, ᄂ, ᄃ"}
]'::jsonb
, 3
, 'ALG'
, 4
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "등비수열 \\(\\left\\{a_n\\right\\}\\) 이"}
, {"order": 2, "type": "TEXT", "content": "\\[2\\left(a_1+a_4+a_7\\right)=a_4+a_7+a_{10}=6\\]"}
, {"order": 3, "type": "TEXT", "content": "을 만족시킬 때, \\(a_{10}\\) 의 값은?"}
]'::jsonb
, 12
, '[
  {"order": 1, "content": "\\(\\frac{22}{7}\\)"}
, {"order": 2, "content": "\\(\\frac{24}{7}\\)"}
, {"order": 3, "content": "\\(\\frac{26}{7}\\)"}
, {"order": 4, "content": "\\(\\frac{30}{7}\\)"}
, {"order": 5, "content": "\\(\\frac{32}{7}\\)"}
]'::jsonb
, 2
, 'ALG'
, 4
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=x^2-4 x-3\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "곡선 \\(y=f(x)\\) 위의 점 \\((1,-6)\\) 에서의 접선을 \\(l\\) 이라 하고, 함수 \\(g(x)=\\left(x^3-2 x\\right) f(x)\\) 에 대하여 곡선 \\(y=g(x)\\) 위의 점 \\((1,6)\\) 에서의 접선을 \\(m\\) 이라 하자. 두 직선 \\(l, m\\) 과 \\(y\\) 축으로 둘러싸인 도형의 넓이는?"}
]'::jsonb
, 13
, '[
  {"order": 1, "content": "21"}
, {"order": 2, "content": "28"}
, {"order": 3, "content": "35"}
, {"order": 4, "content": "42"}
, {"order": 5, "content": "49"}
]'::jsonb
, 5
, 'ALG'
, 4
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "그림과 같이 \\(\\overline{\\mathrm{AB}}=3, \\overline{\\mathrm{BC}}=4\\) 이고 \\(\\angle \\mathrm{B}=\\frac{\\pi}{2}\\) 인 직각삼각형 ABC 가 있다. 선분 AB 를 \\(2: 1\\) 로 내분하는 점을 D , 점 A 를 중심으로 하고 반지름의 길이가 \\(\\overline{\\mathrm{AD}}\\) 인 원이 선분 AC 와 만나는 점을 E , 직선 AB 가 이 원과 만나는 점 중 D 가 아닌 점을 F 라 하고, 호 EF 위의 점 G 를 \\(\\overline{\\mathrm{CG}}=2 \\sqrt{6}\\) 이 되도록 잡는다. 세 점 \\(\\mathrm{C}, \\mathrm{E}, \\mathrm{G}\\) 를 지나는 원 위의 점 H 가 \\(\\angle \\mathrm{HCG}=\\angle \\mathrm{BAC}\\) 를 만족시킬 때, 선분 GH 의 길이는?"}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/alg14p.JPG"}
]'::jsonb
, 14
, '[
  {"order": 1, "content": "\\(\\frac{6 \\sqrt{15}}{5}\\)"}
, {"order": 2, "content": "\\(\\frac{38 \\sqrt{10}}{25}\\)"}
, {"order": 3, "content": "\\(\\frac{14 \\sqrt{3}}{5}\\)"}
, {"order": 4, "content": "\\(\\frac{32 \\sqrt{15}}{25}\\)"}
, {"order": 5, "content": "\\(\\frac{8 \\sqrt{10}}{5}\\)"}
]'::jsonb
, 4
, 'ALG'
, 4
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)\\) 가"}
, {"order": 2, "type": "TEXT", "content": "\\[f(x)= \\begin{cases}-x^2 & (x<0) \\\\ x^2-x & (x \\geq 0)\\end{cases}\\]"}
, {"order": 3, "type": "TEXT", "content": "이고, 양수 \\(a\\) 에 대하여 함수 \\(g(x)\\) 를"}
, {"order": 4, "type": "TEXT", "content": "\\[g(x)=\\left\\{\\begin{array}{cl} a x+a & (x<-1) \\\\ 0 & (-1 \\leq x<1) \\\\ a x-a & (x \\geq 1)\\end{array}\\right.\\]"}
, {"order": 5, "type": "TEXT", "content": "이라 하자. 함수 \\(h(x)=\\int_0^x(g(t)-f(t)) d t\\) 가 오직 하나의 극값을 갖도록 하는 \\(a\\) 의 최댓값을 \\(k\\) 라 하자. \\(a=k\\) 일 때, \\(k+h(3)\\) 의 값은?"}
]'::jsonb
, 15
, '[
  {"order": 1, "content": "\\(\\frac{9}{2}\\)"}
, {"order": 2, "content": "\\(\\frac{11}{2}\\)"}
, {"order": 3, "content": "\\(\\frac{13}{2}\\)"}
, {"order": 4, "content": "\\(\\frac{15}{2}\\)"}
, {"order": 5, "content": "\\(\\frac{17}{2}\\)"}
]'::jsonb
, 4
, 'ALG'
, 4
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 은 \\(a_1=1\\) 이고, 모든 자연수 \\(n\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[a_{n+1}=n^2 a_n+1\\]"}
, {"order": 3, "type": "TEXT", "content": "을 만족시킨다. \\(a_3\\) 의 값을 구하시오."}
]'::jsonb
, 16
, null
, 9
, 'ALG'
, 3
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=4 x^3-2 x\\) 의 한 부정적분 \\(F(x)\\) 에 대하여 \\(F(0)=4\\) 일 때, \\(F(2)\\) 의 값을 구하시오."}
]'::jsonb
, 17
, null
, 16
, 'ALG'
, 3
, 'FRQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "\\(\\overline{\\mathrm{AB}}=5, \\overline{\\mathrm{AC}}=6\\) 이고 \\(\\cos (\\angle \\mathrm{BAC})=-\\frac{3}{5}\\) 인 삼각형 ABC 의 넓이를 구하시오."}
]'::jsonb
, 18
, null
, 12
, 'ALG'
, 3
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "\\(-2 \\leq x \\leq 2\\) 인 모든 실수 \\(x\\) 에 대하여 부등식"}
, {"order": 2, "type": "TEXT", "content": "\\[-k \\leq 2 x^3+3 x^2-12 x-8 \\leq k\\]"}
, {"order": 3, "type": "TEXT", "content": "가 성립하도록 하는 양수 \\(k\\) 의 최솟값을 구하시오."}
]'::jsonb
, 19
, null
, 15
, 'ALG'
, 3
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 이 다음 조건을 만족시킨다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/alg20p.JPG"}
, {"order": 3, "type": "TEXT", "content": "다음은 \\(\\sum_{k=1}^{12} a_k+\\sum_{k=1}^5 a_{2 k+1}\\) 의 값을 구하는 과정이다."}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/alg20p2.JPG"}
, {"order": 5, "type": "TEXT", "content": "위의 (가)에 알맞은 식을 \\(f(n)\\) 이라 하고, (나), (다)에 알맞은 수를 각각 \\(p, q\\) 라 할 때, \\(\\frac{p \\times q}{f(12)}\\) 의 값을 구하시오."}
]'::jsonb
, 20
, null
, 130
, 'ALG'
, 4
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "최고차항의 계수가 양수인 삼차함수 \\(f(x)\\) 와 실수 \\(t\\) 에 대하여 함수"}
, {"order": 2, "type": "TEXT", "content": "\\[g(x)=\\left\\{\\begin{array}{rr}-f(x) & (x<t) \\\\ f(x) & (x \\geq t)\\end{array}\\right.\\]"}
, {"order": 3, "type": "TEXT", "content": "는 실수 전체의 집합에서 연속이고 다음 조건을 만족시킨다."}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/alg21p.JPG"}
, {"order": 5, "type": "TEXT", "content": "\\(g(-5)\\) 의 값을 구하시오. (단, \\(g(-1) \\neq-\\frac{7}{2} g(1)\\) )"}
]'::jsonb
, 21
, null
, 65
, 'ALG'
, 4
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "곡선 \\(y=\\log _{16}(8 x+2)\\) 위의 점 \\(\\mathrm{A}(a, b)\\) 와 곡선 \\(y=4^{x-1}-\\frac{1}{2}\\) 위의 점 B 가 제 1 사분면에 있다."}
, {"order": 2, "type": "TEXT", "content": "점 A 를 직선 \\(y=x\\) 에 대하여 대칭이동한 점이 직선 OB 위에 있고 선분 AB 의 중점의 좌표가 \\(\\left(\\frac{77}{8}, \\frac{133}{8}\\right)\\) 일 때, \\(a \\times b=\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, O 는 원점이고, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
]'::jsonb
, 22
, null
, 457
, 'ALG'
, 4
, 'FRQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "네 문자 \\(a, b, c, d\\) 중에서 중복을 허락하여 3 개를 택해 일렬로 나열하는 경우의 수는?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content": "56"}
, {"order": 2, "content": "60"}
, {"order": 3, "content": "64"}
, {"order": 4, "content": "68"}
, {"order": 5, "content": "72"}
]'::jsonb
, 3
, 'PROB'
, 2
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "두 사건 \\(A, B\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[\\mathrm{P}(A)=\\frac{2}{5}, \\quad \\mathrm{P}(B \\mid A)=\\frac{1}{4}, \\quad \\mathrm{P}(A \\cup B)=1\\]"}
, {"order": 3, "type": "TEXT", "content": "일 때, \\(\\mathrm{P}(B)\\) 의 값은?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content": "\\(\\frac{7}{10}\\)"}
, {"order": 2, "content": "\\(\\frac{3}{4}\\)"}
, {"order": 3, "content": "\\(\\frac{4}{5}\\)"}
, {"order": 4, "content": "\\(\\frac{17}{20}\\)"}
, {"order": 5, "content": "\\(\\frac{9}{10}\\)"}
]'::jsonb
, 1
, 'PROB'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "주머니에 숫자 \\(1,2,3,4,5\\) 가 하나씩 적혀 있는 흰 공 5 개와 숫자 \\(2,3,4,5,6\\) 이 하나씩 적혀 있는 검은 공 5 개가 들어 있다. 이 주머니에서 임의로 2 개의 공을 동시에 꺼낼 때, 꺼낸 2 개의 공이 서로 같은 색이거나 꺼낸 2 개의 공에 적힌 수가 서로 같을 확률은?"}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/prob25p.JPG"}
]'::jsonb
, 25
, '[
  {"order": 1, "content": "\\(\\frac{7}{15}\\)"}
, {"order": 2, "content": "\\(\\frac{8}{15}\\)"}
, {"order": 3, "content": "\\(\\frac{3}{5}\\)"}
, {"order": 4, "content": "\\(\\frac{2}{3}\\)"}
, {"order": 5, "content": "\\(\\frac{11}{15}\\)"}
]'::jsonb
, 2
, 'PROB'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "평균이 \\(m\\) 이고 표준편차가 5 인 정규분포를 따르는 모집단에서 크기가 36 인 표본을 임의추출하여 얻은 표본평균을 이용하여 구한 모평균 \\(m\\) 에 대한 신뢰도 \\(99 \\%\\) 의 신뢰구간이 \\(1.2 \\leq m \\leq a\\) 이다. \\(a\\) 의 값은? (단, \\(Z\\) 가 표준정규분포를 따르는 확률변수일 때, \\(\\mathrm{P}(|Z| \\leq 2.58)=0.99\\) 로 계산한다.)"}
]'::jsonb
, 26
, '[
  {"order": 1, "content": "5.1"}
, {"order": 2, "content": "5.2"}
, {"order": 3, "content": "5.3"}
, {"order": 4, "content": "5.4"}
, {"order": 5, "content": "5.5"}
]'::jsonb
, 5
, 'PROB'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "이산확률변수 \\(X\\) 가 가지는 값이 0 부터 4 까지의 정수이고"}
, {"order": 2, "type": "TEXT", "content": "\\[\\mathrm{P}(X=x)=\\left\\{\\begin{array}{cl} \\frac{|2 x-1|}{12} & (x=0,1,2,3) \\\\ a & (x=4) \\end{array}\\right.\\]"}
, {"order": 3, "type": "TEXT", "content": "일 때, \\(\\mathrm{V}\\left(\\frac{1}{a} X\\right)\\) 의 값은? (단, \\(a\\) 는 0 이 아닌 상수이다.)"}
]'::jsonb
, 27
, '[
  {"order": 1, "content": "36"}
, {"order": 2, "content": "39"}
, {"order": 3, "content": "42"}
, {"order": 4, "content": "45"}
, {"order": 5, "content": "48"}
]'::jsonb
, 4
, 'PROB'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "16 개의 공과 1 부터 6 까지의 자연수가 하나씩 적혀 있는 여섯 개의 빈 상자가 있다. 한 개의 주사위를 사용하여 다음 시행을 한다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/prob28p.JPG"}
, {"order": 3, "type": "TEXT", "content": "이 시행을 4 번 반복한 후 여섯 개의 상자에 들어 있는 모든 공의 개수의 합이 홀수일 때, 3 이 적힌 상자에 들어 있는 공의 개수가 2 가 적힌 상자에 들어 있는 공의 개수보다 1 개 더 많을 확률은?"}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/prob28p2.JPG"}
]'::jsonb
, 28
, '[
  {"order": 1, "content": "\\(\\frac{1}{8}\\)"}
, {"order": 2, "content": "\\(\\frac{3}{16}\\)"}
, {"order": 3, "content": "\\(\\frac{1}{4}\\)"}
, {"order": 4, "content": "\\(\\frac{5}{16}\\)"}
, {"order": 5, "content": "\\(\\frac{3}{8}\\)"}
]'::jsonb
, 2
, 'PROB'
, 4
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "6 이하의 자연수 \\(a\\) 에 대하여 한 개의 주사위와 한 개의 동전을 사용하여 다음 시행을 한다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/prob29p.JPG"}
, {"order": 3, "type": "TEXT", "content": "이 시행을 19200 번 반복하여 기록한 수가 3 인 횟수를 확률변수 \\(X\\) 라 하자."}
, {"order": 4, "type": "TEXT", "content": "\\(\\mathrm{E}(X)=4800\\) 일 때, \\(\\mathrm{P}(X \\leq 4800+30 a)\\) 의 값을 오른쪽 표준정규분포표를 이용하여 구한 값이 \\(k\\) 이다."}
, {"order": 5, "type": "TEXT", "content": "\\(1000 \\times k\\) 의 값을 구하시오."}
,  {"order":6, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/prob29p2.JPG"}
]'::jsonb
, 29
, null
, 977
, 'PROB'
, 4
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "비어 있는 주머니 10 개가 일렬로 놓여 있고, 공 8 개가 있다."}
, {"order": 2, "type": "TEXT", "content": "각 주머니에 들어 있는 공의 개수가 2 이하가 되도록 공을 주머니에 남김없이 나누어 넣을 때, 다음 조건을 만족시키는 경우의 수를 구하시오. (단, 공끼리는 서로 구별하지 않는다.)"}
, {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/prob30p.JPG"}
]'::jsonb
, 30
, null
, 262
, 'PROB'
, 4
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "\\(\\lim _{x \\rightarrow 0} \\frac{\\tan 6 x}{2 x}\\) 의 값은?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "2"}
, {"order": 3, "content": "3"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "5"}
]'::jsonb
, 3
, 'CALC'
, 2
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "\\(\\int_0^{\\frac{\\pi}{2}} \\sqrt{\\sin x-\\sin ^3 x} \\, d x\\) 의 값은?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content": "\\(\\frac{1}{6}\\)"}
, {"order": 2, "content": "\\(\\frac{1}{3}\\)"}
, {"order": 3, "content": "\\(\\frac{1}{2}\\)"}
, {"order": 4, "content": "\\(\\frac{2}{3}\\)"}
, {"order": 5, "content": "\\(\\frac{5}{6}\\)"}
]'::jsonb
, 4
, 'CALC'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 이 모든 자연수 \\(n\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[\\sqrt{9 n^2-5}+2 n<a_n<5 n+1\\]"}
, {"order": 3, "type": "TEXT", "content": "을 만족시킬 때, \\(\\lim _{n \\rightarrow \\infty} \\frac{\\left(a_n+2\\right)^2}{n a_n+5 n^2-2}\\) 의 값은?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content": "\\(\\frac{1}{2}\\)"}
, {"order": 2, "content": "\\(\\frac{3}{2}\\)"}
, {"order": 3, "content": "\\(\\frac{5}{2}\\)"}
, {"order": 4, "content": "\\(\\frac{7}{2}\\)"}
, {"order": 5, "content": "\\(\\frac{9}{2}\\)"}
]'::jsonb
, 3
, 'CALC'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "그림과 같이 곡선 \\(y=\\sqrt{x+x \\ln x}\\) 와 \\(x\\) 축 및 두 직선 \\(x=1, x=2\\) 로 둘러싸인 부분을 밑면으로 하는 입체도형이 있다. 이 입체도형을 \\(x\\) 축에 수직인 평면으로 자른 단면이 모두 정삼각형일 때, 이 입체도형의 부피는?"}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/calc26p.JPG"}
]'::jsonb
, 26
, '[
  {"order": 1, "content": "\\(\\frac{\\sqrt{3}(3+8 \\ln 2)}{16}\\)"}
, {"order": 2, "content": "\\(\\frac{\\sqrt{3}(5+12 \\ln 2)}{24}\\)"}
, {"order": 3, "content": "\\(\\frac{\\sqrt{3}(1+12 \\ln 2)}{16}\\)"}
, {"order": 4, "content": "\\(\\frac{\\sqrt{3}(1+2 \\ln 2)}{4}\\)"}
, {"order": 5, "content": "\\(\\frac{\\sqrt{3}(1+9 \\ln 2)}{12}\\)"}
]'::jsonb
, 1
, 'CALC'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "매개변수 \\(t\\) 로 나타내어진 곡선"}
, {"order": 2, "type": "TEXT", "content": "\\[x=e^{4 t}\\left(1+\\sin ^2 \\pi t\\right), \\quad y=e^{4 t}\\left(1-3 \\cos ^2 \\pi t\\right)\\]"}
, {"order": 3, "type": "TEXT", "content": "를 \\(C\\) 라 하자. 곡선 \\(C\\) 가 직선 \\(y=3 x-5 e\\) 와 만나는 점을 P 라 할 때, 곡선 \\(C\\) 위의 점 P 에서의 접선의 기울기는?"}
]'::jsonb
, 27
, '[
  {"order": 1, "content": "\\(\\frac{3 \\pi-4}{\\pi+4}\\)"}
, {"order": 2, "content": "\\(\\frac{3 \\pi-2}{\\pi+6}\\)"}
, {"order": 3, "content": "\\(\\frac{3 \\pi}{\\pi+8}\\)"}
, {"order": 4, "content": "\\(\\frac{3 \\pi+2}{\\pi+10}\\)"}
, {"order": 5, "content": "\\(\\frac{3 \\pi+4}{\\pi+12}\\)"}
]'::jsonb
, 2
, 'CALC'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수"}
, {"order": 2, "type": "TEXT", "content": "\\[f(x)=\\frac{1}{2} x^2-x+\\ln (1+x)\\]"}
, {"order": 3, "type": "TEXT", "content": "와 양수 \\(t\\) 에 대하여 점 \\((s, f(s))(s>0)\\) 에서 \\(y\\) 축에 내린 수선의 발과 곡선 \\(y=f(x)\\) 위의 점 \\((s, f(s))\\) 에서의 접선이 \\(y\\) 축과 만나는 점 사이의 거리가 \\(t\\) 가 되도록 하는 \\(s\\) 의 값을 \\(g(t)\\) 라 하자. \\(\\int_{\\frac{1}{2}}^{\\frac{27}{4}} g(t) \\, d t\\) 의 값은?"}
]'::jsonb
, 28
, '[
  {"order": 1, "content": "\\(\\frac{161}{12}+\\ln 3\\)"}
, {"order": 2, "content": "\\(\\frac{40}{3}+\\ln 3\\)"}
, {"order": 3, "content": "\\(\\frac{53}{4}+\\ln 2\\)"}
, {"order": 4, "content": "\\(\\frac{79}{6}+\\ln 2\\)"}
, {"order": 5, "content": "\\(\\frac{157}{12}+\\ln 2\\)"}
]'::jsonb
, 5
, 'CALC'
, 4
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "첫째항과 공차가 같은 등차수열 \\(\\left\\{a_n\\right\\}\\) 과 등비수열 \\(\\left\\{b_n\\right\\}\\) 이 다음 조건을 만족시킨다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/calc29p.JPG"}
, {"order": 3, "type": "TEXT", "content": "부등식"}
, {"order": 4, "type": "TEXT", "content": "\\[0<\\sum_{n=1}^{\\infty}\\left(b_n-\\frac{1}{a_n a_{n+1}}\\right)<30\\]"}
, {"order": 5, "type": "TEXT", "content": "이 성립할 때, \\(a_2 \\times \\sum_{n=1}^{\\infty} b_{2 n}=\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(a_1 \\neq 0\\) 이고, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
]'::jsonb
, 29
, null
, 97
, 'CALC'
, 4
, 'FRQ'
)


     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "실수 전체의 집합에서 증가하는 연속함수 \\(f(x)\\) 의 역함수 \\(f^{-1}(x)\\) 가 다음 조건을 만족시킨다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/calc30p.JPG"}
, {"order": 3, "type": "TEXT", "content": "실수 \\(m\\) 에 대하여 기울기가 \\(m\\) 이고 점 \\((1,0)\\) 을 지나는 직선이 곡선 \\(y=f(x)\\) 와 만나는 점의 개수를 \\(g(m)\\) 이라 하자. 함수 \\(g(m)\\) 이 \\(m=a, m=b(a<b)\\) 에서 불연속일 때, \\(g(a) \\times\\left(\\lim _{m \\rightarrow a+} g(m)\\right)+g(b) \\times\\left(\\left(\\frac{\\ln b}{b}\\right)^2\\right)\\) 의 값을 구하시오. (단, \\(\\lim _{x \\rightarrow \\infty} \\frac{\\ln x}{x}=0\\) )"}
]'::jsonb
, 30
, null
, 11
, 'CALC'
, 4
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "두 벡터 \\(\\vec{a}=(4,1), \\\\vec{b}=(-1,-1)\\) 에 대하여 \\(\\vec{a}+\\vec{b}\\) 의 모든 성분의 합은?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "2"}
, {"order": 3, "content": "3"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "5"}
]'::jsonb
, 3
, 'GEO'
, 2
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "포물선 \\(y^2=12(x-2)\\) 의 초점과 준선 사이의 거리는?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content": "6"}
, {"order": 2, "content": "7"}
, {"order": 3, "content": "8"}
, {"order": 4, "content": "9"}
, {"order": 5, "content": "10"}
]'::jsonb
, 1
, 'GEO'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "좌표공간의 점 \\(\\mathrm{A}\\left(3,-\\frac{3}{2},-2\\right)\\) 를 \\(y z\\) 평면에 대하여 대칭이동한 점을 B , 점 A 를 원점에 대하여 대칭이동한 점을 C 라 할 때, 선분 BC 의 길이는?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content": "\\(\\sqrt{21}\\)"}
, {"order": 2, "content": "\\(\\sqrt{22}\\)"}
, {"order": 3, "content": "\\(\\sqrt{23}\\)"}
, {"order": 4, "content": "\\(2 \\sqrt{6}\\)"}
, {"order": 5, "content": "5"}
]'::jsonb
, 5
, 'GEO'
, 3
, 'MCQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "양수 \\(a\\) 에 대하여 두 초점이 \\(\\mathrm{F}, \\, \\mathrm{F}^{\\prime}\\) 인 쌍곡선 \\(\\frac{x^2}{a^2}-\\frac{y^2}{a^2}=-1\\) 위의 점 \\((a, \\sqrt{2} a)\\) 에서의 접선이 \\(y\\) 축과 만나는 점을 P 라 하자. \\(\\overline{\\mathrm{PF}} \\times \\overline{\\mathrm{PF}^{\\prime}}=8\\) 일 때, \\(a\\) 의 값은?"}
]'::jsonb
, 26
, '[
  {"order": 1, "content": "\\(\\sqrt{3}\\)"}
, {"order": 2, "content": "\\(\\frac{4 \\sqrt{3}}{3}\\)"}
, {"order": 3, "content": "\\(\\frac{5 \\sqrt{3}}{3}\\)"}
, {"order": 4, "content": "\\(2 \\sqrt{3}\\)"}
, {"order": 5, "content": "\\(\\frac{7 \\sqrt{3}}{3}\\)"}
]'::jsonb
, 4
, 'GEO'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "그림과 같이 지름의 길이가 5 인 두 원 \\(C_1, C_2\\) 를 두 밑면으로 하는 원기둥이 있고, 원 \\(C_1\\) 위의 \\(\\overline{\\mathrm{AB}}=5\\) 인 두 점 \\(\\mathrm{A}, \\, \\mathrm{B}\\) 와 원 \\(C_2\\) 위의 \\(\\overline{\\mathrm{CD}}=3\\) 인 두 점 \\(\\mathrm{C}, \\, \\mathrm{D}\\) 에 대하여 \\(\\overline{\\mathrm{AD}}=\\overline{\\mathrm{BC}}\\) 이다. 점 D 에서 원 \\(C_1\\) 을 포함하는 평면에 내린 수선의 발을 H 라 하자. 사각형 ABCD 의 넓이가 삼각형 ABH 의 넓이의 4 배일 때, 이 원기둥의 높이는?"}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/geo27p.JPG"}
]'::jsonb
, 27
, '[
  {"order": 1, "content": "\\(3 \\sqrt{2}\\)"}
, {"order": 2, "content": "\\(\\sqrt{19}\\)"}
, {"order": 3, "content": "\\(2 \\sqrt{5}\\)"}
, {"order": 4, "content": "\\(\\sqrt{21}\\)"}
, {"order": 5, "content": "\\(\\sqrt{22}\\)"}
]'::jsonb
, 4
, 'GEO'
, 3
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "그림과 같이 \\(\\overline{\\mathrm{AB}}=\\overline{\\mathrm{CD}}=4, \\, \\overline{\\mathrm{BC}}=\\overline{\\mathrm{BD}}=2 \\sqrt{5}\\) 인 사면체 ABCD 가 있고, 점 A 에서 직선 CD 에 내린 수선의 발 H 에 대하여 두 평면 ABH 와 BCD 는 서로 수직이고 \\(\\overline{\\mathrm{AH}}=4\\) 이다. 삼각형 ABH 의 무게중심을 G 라 하고, 점 G 를 중심으로 하고 평면 ACD 에 접하는 구를 \\(S\\) 라 하자. \\(\\angle \\mathrm{APG}=\\frac{\\pi}{2}\\) 인 구 \\(S\\) 위의 모든 점 P 가 나타내는 도형을 \\(T\\) 라 할 때, 도형 \\(T\\) 의 평면 ABC 위로의 정사영의 넓이는?"}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/geo28p.JPG"}

]'::jsonb
, 28
, '[
  {"order": 1, "content": "\\(\\frac{\\pi}{7}\\)"}
, {"order": 2, "content": "\\(\\frac{\\pi}{6}\\)"}
, {"order": 3, "content": "\\(\\frac{\\pi}{5}\\)"}
, {"order": 4, "content": "\\(\\frac{\\pi}{4}\\)"}
, {"order": 5, "content": "\\(\\frac{\\pi}{3}\\)"}
]'::jsonb
, 4
, 'GEO'
, 4
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "그림과 같이 초점이 \\(\\mathrm{F}(p, 0)(p>0)\\) 이고 준선이 \\(x=-p\\) 인 포물선 위의 점 중 제 1 사분면에 있는 점 A 에서 포물선의 준선에 내린 수선의 발을 H 라 하고, 두 초점이 \\(x\\) 축 위에 있고 세 점 \\(\\mathrm{F}, \\, \\mathrm{A}, \\, \\mathrm{H}\\) 를 지나는 타원의 \\(x\\) 좌표가 양수인 초점을 B 라 하자. 삼각형 AHB 의 둘레의 길이가 \\(p+27\\), 넓이가 \\(2 p+12\\) 일 때, 선분 HF 의 길이를 \\(k\\) 라 하자. \\(k^2\\) 의 값을 구하시오."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/csat/geo29p.JPG"}
]'::jsonb
, 29
, null
, 360
, 'GEO'
, 4
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "좌표평면에서 길이가 \\(10 \\sqrt{2}\\) 인 선분 AB 를 지름으로 하는 원 위의 두 점 \\(\\mathrm{P}, \\, \\mathrm{Q}\\) 가"}
, {"order": 2, "type": "TEXT", "content": "\\[(\\overrightarrow{\\mathrm{PA}}+\\overrightarrow{\\mathrm{PB}}) \\cdot(\\overrightarrow{\\mathrm{PQ}}+\\overrightarrow{\\mathrm{PB}})=2|\\overrightarrow{\\mathrm{PQ}}|^2\\]"}
, {"order": 3, "type": "TEXT", "content": "을 만족시킨다. \\(|\\overrightarrow{\\mathrm{PB}}|=14\\) 일 때, \\(|\\overrightarrow{\\mathrm{PA}} \\cdot \\overrightarrow{\\mathrm{QB}}|=\\frac{q}{p}\\) 이다."}
, {"order": 4, "type": "TEXT", "content": "\\(p+q\\) 의 값을 구하시오."}
, {"order": 5, "type": "TEXT", "content": "(단, \\(|\\overrightarrow{\\mathrm{QB}}|>0\\) 이고, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
]'::jsonb
, 30
, null
, 221
, 'GEO'
, 4
, 'FRQ'
)



;




