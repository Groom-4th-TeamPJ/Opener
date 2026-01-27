insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2026
       , 'M09'
       , '9월 모의평가'
       , 30
       , 6000);


WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2026 AND exam_type = 'M09' LIMIT 1
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
  {"order": 1, "type": "TEXT", "content": "\\(5^{\\sqrt{2}+1} \\times\\left(\\frac{1}{5}\\right)^{\\sqrt{2}}\\) 의 값은?"}
]'::jsonb
       , 1
       , '[
  {"order": 1, "content": "\\(\\frac{1}{25}\\)"}
, {"order": 2, "content": "\\(\\frac{1}{5}\\)"}
, {"order": 3, "content": "1"}
, {"order": 4, "content": "5"}
, {"order": 5, "content": "25"}
]'::jsonb
       , 4
       , 'ALG'
       , 2
       , 'MCQ'
)
     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=x^2-4 x+2\\) 에 대하여 \\(\\lim _{h \\rightarrow 0} \\frac{f(4+h)-f(4)}{h}\\) 의 값은?"}
]'::jsonb
, 2
, '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "2"}
, {"order": 3, "content": "3"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "5"}
]'::jsonb
, 4
, 'ALG'
, 2
, 'MCQ'
)

     , (
         (SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 \\(\\sum_{k=1}^6\\left(2 a_k-1\\right)=30\\) 일 때, \\(\\sum_{k=1}^6 a_k\\) 의 값은?"}
]'::jsonb
       , 3
       , '[
  {"order": 1, "content": "2"}
, {"order": 2, "content": "6"}
, {"order": 3, "content": "10"}
, {"order": 4, "content": "14"}
, {"order": 5, "content": "18"}
]'::jsonb
       , 5
       , 'ALG'
       , 3
       , 'MCQ'
)

     , (
         (SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content": "닫힌구간 \\([-2,2]\\) 에서 정의된 함수 \\(y=f(x)\\) 의 그래프가 그림과 같다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/alg4p.JPG"}
, {"order": 3, "type": "TEXT", "content": "\\(\\lim _{x \\rightarrow 0-} f(x)+\\lim _{x \\rightarrow 1+} f(x)\\) 의 값은?"}
]'::jsonb
       , 4
       , '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "2"}
, {"order": 3, "content": "3"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "5"}
]'::jsonb
       , 1
       , 'ALG'
       , 3
       , 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=\\left(x^2+2\\right)\\left(x^2+x-3\\right)\\) 에 대하여 \\(f^{\\prime}(1)\\) 의 값은?"}
]'::jsonb
, 5
, '[
  {"order": 1, "content": "6"}
, {"order": 2, "content": "7"}
, {"order": 3, "content": "8"}
, {"order": 4, "content": "9"}
, {"order": 5, "content": "10"}
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
  {"order": 1, "type": "TEXT", "content": "\\(\\cos (\\theta-\\pi)=\\frac{3}{5}\\) 이고 \\(\\tan \\theta<0\\) 일 때, \\(\\sin \\theta\\) 의 값은?"}
]'::jsonb
, 6
, '[
  {"order": 1, "content": "\\(-\\frac{4}{5}\\)"}
, {"order": 2, "content": "\\(-\\frac{3}{5}\\)"}
, {"order": 3, "content": "\\(\\frac{1}{5}\\)"}
, {"order": 4, "content": "\\(\\frac{3}{5}\\)"}
, {"order": 5, "content": "\\(\\frac{4}{5}\\)"}
]'::jsonb
, 5
, 'ALG'
, 3
, 'MCQ'
)

     ,(
        (SELECT id FROM exam)
      , '[
  {"order": 1, "type": "TEXT", "content": "곡선 \\(y=x^3-5 x^2+6 x\\) 위의 점 \\((3,0)\\) 에서의 접선이 점 \\((5, a)\\) 를 지날 때, \\(a\\) 의 값은?"}
]'::jsonb
      , 7
      , '[
  {"order": 1, "content": "6"}
, {"order": 2, "content": "7"}
, {"order": 3, "content": "8"}
, {"order": 4, "content": "9"}
, {"order": 5, "content": "10"}
]'::jsonb
      , 1
      , 'ALG'
      , 3
      , 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "두 양수 \\(a, b\\) 가"}
, {"order": 2, "type": "TEXT", "content": "\\[\\log _{\\sqrt{2}} a+\\log _2 b=2, \\quad \\log _2 a+\\log _2 b^2=7\\]"}
, {"order": 3, "type": "TEXT", "content": "을 만족시킬 때, \\(a \\times b\\) 의 값은?"}
]'::jsonb
, 8
, '[
  {"order": 1, "content": "2"}
, {"order": 2, "content": "4"}
, {"order": 3, "content": "8"}
, {"order": 4, "content": "16"}
, {"order": 5, "content": "32"}
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
  {"order": 1, "type": "TEXT", "content": "다항함수 \\(f(x)\\) 의 한 부정적분을 \\(F(x)\\) 라 하고, 함수 \\(2 f(x)+1\\) 의 한 부정적분을 \\(G(x)\\) 라 하자."}
, {"order": 2, "type": "TEXT", "content": "\\(G(3)=2 F(3)\\) 일 때, \\(G(5)-2 F(5)\\) 의 값은?"}
]'::jsonb
, 9
, '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "2"}
, {"order": 3, "content": "3"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "5"}
]'::jsonb
, 2
, 'ALG'
, 4
, 'MCQ'
)

     ,(
        (SELECT id FROM exam)
      , '[
  {"order": 1, "type": "TEXT", "content": "모든 항이 양수인 등비수열 \\(\\left\\{a_n\\right\\}\\) 의 첫째항부터 제 \\(n\\) 항까지의 합을 \\(S_n\\) 이라 하자."}
, {"order": 2, "type": "TEXT", "content": "\\[a_2=1, \\quad \\sum_{k=1}^6(-1)^k S_k=21\\]"}
, {"order": 3, "type": "TEXT", "content": "일 때, \\(S_2+S_7\\) 의 값은?"}
]'::jsonb
      , 10
      , '[
  {"order": 1, "content": "61"}
, {"order": 2, "content": "63"}
, {"order": 3, "content": "65"}
, {"order": 4, "content": "67"}
, {"order": 5, "content": "69"}
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
  {"order": 1, "type": "TEXT", "content": "시각 \\(t=0\\) 일 때 원점에서 출발하여 수직선 위를 움직이는 점 P 가 있다. 시각이 \\(t(t \\geq 0)\\) 일 때 점 P 의 속도 \\(v(t)\\) 가"}
, {"order": 2, "type": "TEXT", "content": "\\[v(t)=3 t^2-10 t+7\\]"}
, {"order": 3, "type": "TEXT", "content": "이다. <보기>에서 옳은 것만을 있는 대로 고른 것은?"}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/alg11p.JPG"}
]'::jsonb
, 11
, '[
  {"order": 1, "content": "ᄀ"}
, {"order": 2, "content": "ㄱ,ㄴ"}
, {"order": 3, "content": "ᄀ, ᄃ"}
, {"order": 4, "content": "ᄂ, ᄃ"}
, {"order": 5, "content": "ᄀ, ᄂ, ᄃ"}
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
  {"order": 1, "type": "TEXT", "content": "상수 \\(a(a>1)\\) 과 양수 \\(t\\) 에 대하여 곡선 \\(y=a^x\\) 과 두 직선 \\(x=t, x=2 t\\) 가 만나는 점을 각각 \\(\\mathrm{A}, \\mathrm{B}\\) 라 하고, 점 B 에서 \\(x\\) 축에 내린 수선의 발을 C 라 하자."}
, {"order": 2, "type": "TEXT", "content": "\\(\\overline{\\mathrm{AB}}=\\overline{\\mathrm{AC}}\\) 이고 삼각형 ACB 의 넓이가 8 일 때, \\(a \\times t\\) 의 값은?"}
]'::jsonb
, 12
, '[
  {"order": 1, "content": "\\(2^{\\frac{9}{4}}\\)"}
, {"order": 2, "content": "\\(2^{\\frac{23}{8}}\\)"}
, {"order": 3, "content": "\\(2^{\\frac{7}{2}}\\)"}
, {"order": 4, "content": "\\(2^{\\frac{33}{8}}\\)"}
, {"order": 5, "content": "\\(2^{\\frac{19}{4}}\\)"}
]'::jsonb
, 1
, 'ALG'
, 4
, 'MCQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=x^2+6 x+12\\) 에 대하여 다음 조건을 만족시키는 모든 정수 \\(k\\) 의 개수는?"}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/alg13p.JPG"}
]'::jsonb
, 13
, '[
  {"order": 1, "content": "5"}
, {"order": 2, "content": "6"}
, {"order": 3, "content": "7"}
, {"order": 4, "content": "8"}
, {"order": 5, "content": "9"}
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
  {"order": 1, "type": "TEXT", "content": "양수 \\(k\\) 에 대하여 집합 \\(\\left\\{x \\left\\lvert\\, 0 \\leq x<\\frac{3 k \\pi}{2}\\right., x \\neq \\frac{k \\pi}{2}\\right\\}\\) 에서 정의된 함수 \\(f(x)=\\tan \\frac{x}{k}\\) 가 있다."}
, {"order": 2, "type": "TEXT", "content": "점 \\(\\mathrm{P}(0, p)(p>0)\\) 을 지나며 \\(x\\) 축에 평행한 직선이 함수 \\(y=f(x)\\) 의 그래프와 만나는 두 점을 \\(\\mathrm{A}, \\mathrm{B}(\\overline{\\mathrm{PA}}<\\overline{\\mathrm{PB}})\\) 라 하고, 직선 \\(y=-p\\) 가 함수 \\(y=f(x)\\) 의 그래프와 만나는 점을 C 라 하자."}
, {"order": 3, "type": "TEXT", "content": "\\(\\overline{\\mathrm{AB}}=3 \\overline{\\mathrm{PA}}\\) 이고 삼각형 OCB 의 넓이가 \\(\\frac{5 \\pi}{3}\\) 일 때, \\(k+p\\) 의 값은? (단, O 는 원점이다.)"}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/alg14p.JPG"}
]'::jsonb
, 14
, '[
  {"order": 1, "content": "\\(\\frac{4 \\sqrt{3}}{3}\\)"}
, {"order": 2, "content": "\\(\\frac{13 \\sqrt{3}}{9}\\)"}
, {"order": 3, "content": "\\(\\frac{14 \\sqrt{3}}{9}\\)"}
, {"order": 4, "content": "\\(\\frac{5 \\sqrt{3}}{3}\\)"}
, {"order": 5, "content": "\\(\\frac{16 \\sqrt{3}}{9}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "최고차항의 계수가 양수이고 \\(f(0)=0\\) 인 삼차함수 \\(f(x)\\) 에 대하여 함수"}
, {"order": 2, "type": "TEXT", "content": "\\[g(x)=\\int_0^x(|f(t)|-|t|) d t\\]"}
, {"order": 3, "type": "TEXT", "content": "가 다음 조건을 만족시킨다."}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/alg15p.JPG"}
, {"order": 5, "type": "TEXT", "content": "\\(f(6) \\times g(2)<0\\) 일 때, \\(f(8)\\) 의 값은?"}
]'::jsonb
, 15
, '[
  {"order": 1, "content": "16"}
, {"order": 2, "content": "22"}
, {"order": 3, "content": "28"}
, {"order": 4, "content": "34"}
, {"order": 5, "content": "40"}
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
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 은 \\(a_1=1\\) 이고, 모든 자연수 \\(n\\) 에 대하여 \\[a_{n+1}=n a_n+2\\] 를 만족시킨다. \\(a_3\\) 의 값을 구하시오."}
]'::jsonb
, 16
, null
, 8
, 'ALG'
, 3
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "다항함수 \\(f(x)\\) 에 대하여 \\(f^{\\prime}(x)=3 x^2+2 x+1\\) 이고 \\(f(1)=6\\) 일 때, \\(f(2)\\) 의 값을 구하시오."}
]'::jsonb
, 17
, null
, 17
, 'ALG'
, 3
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "등차수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[a_3=6, \\quad 2 a_5-a_4=15\\]"}
, {"order": 3, "type": "TEXT", "content": "일 때, \\(a_{11}\\) 의 값을 구하시오."}
]'::jsonb
, 18
, null
, 30
, 'ALG'
, 3
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=2 x^3-3 a x^2+5 a\\) 의 극솟값이 \\(a\\) 일 때, 함수 \\(f(x)\\) 의 극댓값을 구하시오. (단, \\(a\\) 는 상수이다.)"}
]'::jsonb
, 19
, null
, 10
, 'ALG'
, 3
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "그림과 같이 사각형 ABCD 가 한 원에 내접하고 \\(\\overline{\\mathrm{AB}}: \\overline{\\mathrm{CD}}=1: 3, \\overline{\\mathrm{BC}}<\\overline{\\mathrm{AD}}\\) 일 때, 직선 AB 와 직선 CD 가 만나는 점을 P 라 하자."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/alg20p.JPG"}
, {"order": 3, "type": "TEXT", "content": "다음은 \\(\\overline{\\mathrm{PB}}: \\overline{\\mathrm{PC}}: \\overline{\\mathrm{BC}}=7: 5: \\sqrt{14}\\) 이고 \\(\\overline{\\mathrm{AD}}=4 \\sqrt{13}\\) 일 때, 삼각형 BPC 의 외접원의 반지름의 길이를 구하는 과정이다."}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/alg20p2.JPG"}
, {"order": 5, "type": "TEXT", "content": "위의 (가), (나), (다)에 알맞은 수를 각각 \\(p, q, r\\) 이라 할 때, \\(p+q+r\\) 의 값을 구하시오."}
]'::jsonb
, 20
, null
, 12
, 'ALG'
, 4
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "최고차항의 계수가 1 인 삼차함수 \\(f(x)\\) 가 다음 조건을 만족시킬 때, \\(f^{\\prime}(10)\\) 의 값을 구하시오."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/alg21p.JPG"}
]'::jsonb
, 21
, null
, 296
, 'ALG'
, 4
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "곡선 \\(y=\\log _2 x\\) 위에 서로 다른 두 점 \\(\\mathrm{A}, \\mathrm{B}\\) 가 있다. 점 A 에서 직선 \\(y=x\\) 에 내린 수선의 발을 P 라 하고, 점 B 를 직선 \\(y=x\\) 에 대하여 대칭이동한 점을 Q 라 할 때, 네 점 \\(\\mathrm{A}, \\mathrm{B}, \\mathrm{P}, \\mathrm{Q}\\) 가 다음 조건을 만족시킨다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/alg22p.JPG"}
, {"order": 3, "type": "TEXT", "content": "사각형 APQB 의 넓이가 \\(\\frac{q}{p}\\) 일 때, \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
]'::jsonb
, 22
, null
, 73
, 'ALG'
, 4
, 'FRQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "세 문자 \\(a, b, c\\) 중에서 중복을 허락하여 4 개를 택해 일렬로 나열하는 경우의 수는?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content": "72"}
, {"order": 2, "content": "75"}
, {"order": 3, "content": "78"}
, {"order": 4, "content": "81"}
, {"order": 5, "content": "84"}
]'::jsonb
, 4
, 'PROB'
, 2
, 'MCQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "두 사건 \\(A, B\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[\\mathrm{P}(A \\cup B)=\\frac{5}{6}, \\quad \\mathrm{P}\\left(A^C \\cap B\\right)=\\frac{1}{4}\\]"}
, {"order": 3, "type": "TEXT", "content": "일 때, \\(\\mathrm{P}\\left(A^C\\right)\\) 의 값은?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content": "\\(\\frac{1}{3}\\)"}
, {"order": 2, "content": "\\(\\frac{3}{8}\\)"}
, {"order": 3, "content": "\\(\\frac{5}{12}\\)"}
, {"order": 4, "content": "\\(\\frac{11}{24}\\)"}
, {"order": 5, "content": "\\(\\frac{1}{2}\\)"}
]'::jsonb
, 3
, 'PROB'
, 3
, 'MCQ'
)

    ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "1 학년 학생 1 명, 2 학년 학생 3 명, 3 학년 학생 4 명이 있다. 이 8 명의 학생 중 임의로 5 명의 학생을 선택할 때, 선택된 2 학년 학생 수와 선택된 3 학년 학생 수가 서로 같을 확률은?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content": "\\(\\frac{1}{4}\\)"}
, {"order": 2, "content": "\\(\\frac{15}{56}\\)"}
, {"order": 3, "content": "\\(\\frac{2}{7}\\)"}
, {"order": 4, "content": "\\(\\frac{17}{56}\\)"}
, {"order": 5, "content": "\\(\\frac{9}{28}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "평균이 \\(m\\) 이고 표준편차가 \\(2 \\sqrt{2}\\) 인 정규분포를 따르는 모집단에서 크기가 128 인 표본을 임의추출하여 얻은 표본평균의 값이 \\(\\bar{x}\\) 일 때, 이를 이용하여 구한 모평균 \\(m\\) 에 대한 신뢰도 \\(95 \\%\\) 의 신뢰구간이 \\(\\bar{x}-c \\leq m \\leq \\bar{x}+c\\) 이다."}
, {"order": 2, "type": "TEXT", "content": "\\(c\\) 의 값은? (단, \\(Z\\) 가 표준정규분포를 따르는 확률변수일 때, \\(\\mathrm{P}(|Z| \\leq 1.96)=0.95\\) 로 계산한다.)"}
]'::jsonb
, 26
, '[
  {"order": 1, "content": "0.47"}
, {"order": 2, "content": "0.49"}
, {"order": 3, "content": "0.51"}
, {"order": 4, "content": "0.53"}
, {"order": 5, "content": "0.55"}
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
  {"order": 1, "type": "TEXT", "content": "각 면에 숫자 \\(1,2,2,3\\) 이 하나씩 적혀 있는 정사면체 모양의 서로 다른 상자 2 개가 있다. 이 두 상자를 동시에 던져서 바닥에 닿은 면에 적혀 있는 두 수의 차를 확률변수 \\(X\\) 라 할 때, \\(\\mathrm{V}(X)\\) 의 값은?"}
]'::jsonb
, 27
, '[
  {"order": 1, "content": "\\(\\frac{1}{4}\\)"}
, {"order": 2, "content": "\\(\\frac{5}{16}\\)"}
, {"order": 3, "content": "\\(\\frac{3}{8}\\)"}
, {"order": 4, "content": "\\(\\frac{7}{16}\\)"}
, {"order": 5, "content": "\\(\\frac{1}{2}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "빨간색 카드 1 장, 파란색 카드 1 장, 노란색 카드 3 장, 보라색 카드 3 장이 있다. 이 8 장의 카드를 세 학생 \\(\\mathrm{A}, \\mathrm{B}, \\mathrm{C}\\) 에게 다음 규칙에 따라 남김없이 나누어 주는 경우의 수는? (단, 같은 색 카드끼리는 서로 구별하지 않는다.)"}
, {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/prob28p.JPG"}
]'::jsonb
, 28
, '[
  {"order": 1, "content": "730"}
, {"order": 2, "content": "746"}
, {"order": 3, "content": "762"}
, {"order": 4, "content": "778"}
, {"order": 5, "content": "794"}
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
  {"order": 1, "type": "TEXT", "content": "두 집합 \\(A=\\{2,3,4\\}, B=\\{2,3\\}\\) 에 대하여 다음 시행을 한다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/prob29p.JPG"}
, {"order": 3, "type": "TEXT", "content": "이 시행을 15360 번 반복하여 기록한 수가 1 인 횟수가 5880 이상일 확률을 오른쪽 표준정규분포표를 이용하여 구한 값이 \\(k\\) 일 때, \\(1000 \\times k\\) 의 값을 구하시오."}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/prob29p2.JPG"}
]'::jsonb
, 29
, null
, 23
, 'PROB'
, 4
, 'FRQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "학생 A 는 숫자 1,8 이 각각 하나씩 적혀 있는 2 장의 카드 중 임의로 한 장의 카드를 선택하여 선택한 카드에 적힌 수가 8 일 때만 선택한 카드를 바닥에 내려놓고, 학생 B 는 숫자 \\(2,3,4,5,6,7\\) 이 각각 하나씩 적혀 있는 6 장의 카드 중 임의로 한 장의 카드를 선택하여 선택한 카드에 적힌 수가 자연수 \\(n\\) 보다 작거나 같을 때만 선택한 카드를 바닥에 내려놓는다."}
, {"order": 2, "type": "TEXT", "content": "다음 규칙에 따라 학생 A 가 귤을 받을 확률을 \\(p\\), 학생 B 가 귤을 받을 확률을 \\(q\\) 라 하자."}
, {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/prob30p.JPG"}
, {"order": 4, "type": "TEXT", "content": "\\(p=q\\) 일 때, \\(24(n+p)\\) 의 값을 구하시오. (단, \\(n\\) 은 7 이하의 자연수이다.)"}
]'::jsonb
, 30
, null
, 80
, 'PROB'
, 4
, 'FRQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "\\(\\lim _{x \\rightarrow 1} \\frac{e^x-e}{x-1}\\) 의 값은?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content": "\\(e\\)"}
, {"order": 2, "content": "\\(2 e\\)"}
, {"order": 3, "content": "\\(3 e\\)"}
, {"order": 4, "content": "\\(4 e\\)"}
, {"order": 5, "content": "\\(5 e\\)"}
]'::jsonb
, 1
, 'CALC'
, 2
, 'MCQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "\\(\\int_{\\frac{\\pi}{4}}^{\\frac{3 \\pi}{4}} \\cos \\left(x-\\frac{\\pi}{4}\\right) e^{\\sin \\left(x-\\frac{\\pi}{4}\\right)} d x\\) 의 값은?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content": "\\(e-2\\)"}
, {"order": 2, "content": "\\(\\frac{e-1}{2}\\)"}
, {"order": 3, "content": "\\(\\frac{e}{2}\\)"}
, {"order": 4, "content": "\\(e-1\\)"}
, {"order": 5, "content": "\\(\\frac{e+1}{2}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "두 실수 \\(a, b\\) 에 대하여 \\(\\lim _{n \\rightarrow \\infty} \\frac{a n^b}{\\sqrt{n^4+4 n}-\\sqrt{n^4+n}}=6\\) 일 때, \\(a+b\\) 의 값은?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content": "6"}
, {"order": 2, "content": "8"}
, {"order": 3, "content": "10"}
, {"order": 4, "content": "12"}
, {"order": 5, "content": "14"}
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
  {"order": 1, "type": "TEXT", "content": "곡선 \\(y=\\frac{3}{x-1}(x>1)\\) 이 두 직선 \\(y=1, y=3\\) 과 만나는 점을 각각 \\(\\mathrm{A}, \\mathrm{B}\\) 라 하자. 곡선 \\(y=\\frac{3}{x-1}(x>1)\\) 과 직선 AB 로 둘러싸인 부분의 넓이는?"}
]'::jsonb
, 26
, '[
  {"order": 1, "content": "\\(4-3 \\ln 3\\)"}
, {"order": 2, "content": "\\(3-3 \\ln 2\\)"}
, {"order": 3, "content": "\\(4-2 \\ln 3\\)"}
, {"order": 4, "content": "\\(3+3 \\ln 2\\)"}
, {"order": 5, "content": "\\(3+3 \\ln 3\\)"}
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
  {"order": 1, "type": "TEXT", "content": "실수 전체의 집합에서 미분가능한 함수 \\(f(x)\\) 가 모든 실수 \\(x\\) 에 대하여 \\(f^{\\prime}(x)>0\\) 이다."}
, {"order": 2, "type": "TEXT", "content": "함수 \\(f\\left(x^3+x\\right)\\) 의 역함수를 \\(g(x)\\) 라 할 때, \\(f(2)=1, f^{\\prime}(2)=8 g^{\\prime}(1)-1\\) 이다. \\(g(1)+g^{\\prime}(1)\\) 의 값은?"}
]'::jsonb
, 27
, '[
  {"order": 1, "content": "\\(\\frac{5}{4}\\)"}
, {"order": 2, "content": "\\(\\frac{11}{8}\\)"}
, {"order": 3, "content": "\\(\\frac{3}{2}\\)"}
, {"order": 4, "content": "\\(\\frac{13}{8}\\)"}
, {"order": 5, "content": "\\(\\frac{7}{4}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "삼차함수 \\(f(x)\\) 와 실수 전체의 집합에서 미분가능한 함수 \\(g(x)\\) 가 모든 실수 \\(x\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[f(x)=g(x)-\\tan g(x)\\]"}
, {"order": 3, "type": "TEXT", "content": "이고 다음 조건을 만족시킬 때, \\(g^{\\prime}(0) \\times(g(0))^2\\) 의 값은?"}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/calc28p.JPG"}
]'::jsonb
, 28
, '[
  {"order": 1, "content": "-12"}
, {"order": 2, "content": "-6"}
, {"order": 3, "content": "-1"}
, {"order": 4, "content": "3"}
, {"order": 5, "content": "9"}
]'::jsonb
, 2
, 'CALC'
, 4
, 'MCQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "첫째항이 양수이고 공비가 유리수인 등비수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 급수 \\(\\sum_{n=1}^{\\infty} a_n\\) 이 수렴하고, 수열 \\(\\left\\{a_n\\right\\}\\) 이 다음 조건을 만족시킨다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/calc29p.JPG"}
, {"order": 3, "type": "TEXT", "content": "\\(\\sum_{n=1}^{\\infty} a_n=\\frac{q}{p}\\) 일 때, \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
]'::jsonb
, 29
, null
, 91
, 'CALC'
, 4
, 'FRQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "실수 전체의 집합에서 미분가능한 함수 \\(f(x)\\) 와 실수 전체의 집합에서 연속인 함수 \\(g(x)\\) 는 모든 실수 \\(x\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[f(x)=\\ln \\left(\\frac{g(x)}{1+x f^{\\prime}(x)}\\right)\\]"}
, {"order": 3, "type": "TEXT", "content": "를 만족시킨다. \\(f(1)=4 \\ln 2\\) 이고"}
, {"order": 4, "type": "TEXT", "content": "\\[\\int_1^2 g(x) d x=34, \\quad \\int_1^2 x g(x) d x=53\\]"}
, {"order": 5, "type": "TEXT", "content": "일 때, \\(\\int_1^2 x e^{f(x)} d x\\) 의 값을 구하시오."}
]'::jsonb
, 30
, null
, 31
, 'CALC'
, 4
, 'FRQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "포물선 \\(y^2=8 x\\) 의 초점의 좌표가 \\((p, 0)\\) 일 때, \\(p\\) 의 값은?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "2"}
, {"order": 3, "content": "3"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "5"}
]'::jsonb
, 2
, 'GEO'
, 2
, 'MCQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "좌표평면에서 두 직선"}
, {"order": 2, "type": "TEXT", "content": "\\[\\frac{x-1}{2}=y-4, \\quad \\frac{x+2}{8}=\\frac{y+5}{a}\\]"}
, {"order": 3, "type": "TEXT", "content": "가 서로 평행할 때, 상수 \\(a\\) 의 값은? (단, \\(a \\neq 0\\) )"}
]'::jsonb
, 24
, '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "2"}
, {"order": 3, "content": "3"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "5"}
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
  {"order": 1, "type": "TEXT", "content": "좌표공간의 점 \\(\\mathrm{A}(4,3,-9)\\) 를 \\(x y\\) 평면에 대하여 대칭이동한 점을 B , 점 A 를 원점에 대하여 대칭이동한 점을 C 라 할 때, 선분 BC 의 길이는?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content": "10"}
, {"order": 2, "content": "12"}
, {"order": 3, "content": "14"}
, {"order": 4, "content": "16"}
, {"order": 5, "content": "18"}
]'::jsonb
, 1
, 'GEO'
, 3
, 'MCQ'
)

, (
    (SELECT id FROM exam)
  , '[
  {"order": 1, "type": "TEXT", "content": "그림과 같이 \\(\\overline{\\mathrm{AB}}=10, \\overline{\\mathrm{AD}}=5, \\overline{\\mathrm{AE}}=1\\) 인 직육면체 \\(\\mathrm{ABCD}-\\mathrm{EFGH}\\) 가 있다. 점 A 와 직선 FH 사이의 거리는?"}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/geo26p.JPG"}
]'::jsonb
  , 26
  , '[
  {"order": 1, "content": "\\(\\sqrt{21}\\)"}
, {"order": 2, "content": "\\(\\sqrt{22}\\)"}
, {"order": 3, "content": "\\(\\sqrt{23}\\)"}
, {"order": 4, "content": "\\(2 \\sqrt{6}\\)"}
, {"order": 5, "content": "5"}
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
  {"order": 1, "type": "TEXT", "content": "두 초점이 \\(\\mathrm{F}(0, c), \\mathrm{F}^{\\prime}(0,-c)(c>0)\\) 인 쌍곡선 \\(\\frac{x^2}{9}-\\frac{y^2}{16}=-1\\) 위의 점 P 가 제 2 사분면에 있다. 삼각형 \\(\\mathrm{PF}^{\\prime} \\mathrm{F}\\) 의 둘레의 길이가 30 일 때, 이 쌍곡선 위의 점 P 에서의 접선의 기울기는?"}
]'::jsonb
, 27
, '[
  {"order": 1, "content": "\\(-\\frac{7 \\sqrt{3}}{9}\\)"}
, {"order": 2, "content": "\\(-\\frac{2 \\sqrt{3}}{3}\\)"}
, {"order": 3, "content": "\\(-\\frac{5 \\sqrt{3}}{9}\\)"}
, {"order": 4, "content": "\\(-\\frac{4 \\sqrt{3}}{9}\\)"}
, {"order": 5, "content": "\\(-\\frac{\\sqrt{3}}{3}\\)"}
]'::jsonb
, 2
, 'GEO'
, 3
, 'MCQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "좌표공간의 구 \\(S: x^2+y^2+z^2=36\\) 위의 점 A 에 대하여 구 \\(S\\) 위의 점 B 가 다음 조건을 만족시킨다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/geo28p.JPG"}
, {"order": 3, "type": "TEXT", "content": "삼각형 OAB 의 \\(x y\\) 평면 위로의 정사영이 직각삼각형일 때, 평면 OAB 와 \\(x y\\) 평면이 이루는 예각의 크기를 \\(\\theta\\) 라 하자. \\(\\cos \\theta\\) 의 값은? (단, O 는 원점이고, 점 A 의 \\(z\\) 좌표는 6 이 아닌 양수이다.)"}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/geo28p2.JPG"}
]'::jsonb
, 28
, '[
  {"order": 1, "content": "\\(\\frac{\\sqrt{2}}{6}\\)"}
, {"order": 2, "content": "\\(\\frac{\\sqrt{2}}{5}\\)"}
, {"order": 3, "content": "\\(\\frac{\\sqrt{2}}{4}\\)"}
, {"order": 4, "content": "\\(\\frac{\\sqrt{2}}{3}\\)"}
, {"order": 5, "content": "\\(\\frac{\\sqrt{2}}{2}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "두 점 \\(\\mathrm{F}(0,6), \\mathrm{F}^{\\prime}(0,-6)\\) 을 초점으로 하는 타원 \\(C_1\\) 에 대하여 점 F 를 지나고 \\(x\\) 축과 평행한 직선이 타원 \\(C_1\\) 과 만나는 점 중 제 1 사분면 위에 있는 점을 P , 선분 \\(\\mathrm{PF}^{\\prime}\\) 과 \\(x\\) 축이 만나는 점을 Q 라 하자."}
, {"order": 2, "type": "TEXT", "content": "두 점 \\(\\mathrm{P}, \\mathrm{F}\\) 를 초점으로 하고 점 Q 가 꼭짓점인 타원 \\(C_2\\) 에 대하여 두 타원 \\(C_1, C_2\\) 가 만나는 점 중 \\(x\\) 축에 가까운 점을 R 이라 하자."}
, {"order": 3, "type": "TEXT", "content": "\\(\\overline{\\mathrm{F}^{\\prime} \\mathrm{R}}-\\overline{\\mathrm{PR}}=7 \\sqrt{2}\\) 일 때, 두 타원 \\(C_1, C_2\\) 의 장축의 길이의 곱을 구하시오."}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/geo29p.JPG"}
]'::jsonb
, 29
, null
, 396
, 'GEO'
, 4
, 'FRQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "좌표평면에 \\(\\overline{\\mathrm{AB}}=\\overline{\\mathrm{AC}}=8 \\sqrt{5}, \\overline{\\mathrm{BC}}=16\\) 인 삼각형 ABC 가 있다. 선분 AB 위의 점 P , 선분 BC 위의 점 Q , 선분 CA 위의 점 R 이 다음 조건을 만족시킨다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m09/geo30p.JPG"}
, {"order": 3, "type": "TEXT", "content": "\\(|3 \\overrightarrow{\\mathrm{XP}}+\\overrightarrow{\\mathrm{XR}}|=|\\overrightarrow{\\mathrm{PR}}|\\) 을 만족시키는 점 X 에 대하여"}
, {"order": 4, "type": "TEXT", "content": "\\(|\\overrightarrow{\\mathrm{BX}}|\\) 의 최댓값과 최솟값을 각각 \\(M, m\\) 이라 할 때,"}
, {"order": 5, "type": "TEXT", "content": "\\(M \\times m\\) 의 값을 구하시오. (단, \\(|\\overrightarrow{\\mathrm{PQ}}|>0\\) )"}
]'::jsonb
, 30
, null
, 69
, 'GEO'
, 4
, 'FRQ'
);









