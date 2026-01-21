insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2025
       , 'CSAT'
       , '수학능력시험'
       , 30
       , 6000);


WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2025 AND exam_type = 'CSAT' LIMIT 1
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
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"\\(\\sqrt[3]{5} \\times 25^{\\frac{1}{3}}\\) 의 값은?"}
    ]'::jsonb,
    1,
    '[
      {"order":1,"content":"1"},
      {"order":2,"content":"2"},
      {"order":3,"content":"3"},
      {"order":4,"content":"4"},
      {"order":5,"content":"5"}
    ]'::jsonb,
    5,
    'ALG',
    2,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"함수 \\(f(x)=x^3-8x+7\\) 에 대하여 \\(\\lim_{h\\rightarrow 0}\\frac{f(2+h)-f(2)}{h}\\) 의 값은?"}
    ]'::jsonb,
    2,
    '[
      {"order":1,"content":"1"},
      {"order":2,"content":"2"},
      {"order":3,"content":"3"},
      {"order":4,"content":"4"},
      {"order":5,"content":"5"}
    ]'::jsonb,
    4,
    'ALG',
    2,
    'MCQ'
)
, (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"첫째항과 공비가 모두 양수 \\(k\\) 인 등비수열 \\(\\{a_n\\}\\) 이"},
      {"order":2,"type":"TEXT","content":"\\[ \\frac{a_4}{a_2}+\\frac{a_2}{a_1}=30 \\]"},
      {"order":3,"type":"TEXT","content":"을 만족시킬 때, \\(k\\) 의 값은?"}
    ]'::jsonb,
    3,
    '[
      {"order":1,"content":"1"},
      {"order":2,"content":"2"},
      {"order":3,"content":"3"},
      {"order":4,"content":"4"},
      {"order":5,"content":"5"}
    ]'::jsonb,
    5,
    'ALG',
    3,
    'MCQ'
)
     , (
         (SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content": "함수"},
  {"order": 2, "type": "TEXT", "content": "\\[ f(x)= \\begin{cases}5 x+a & (x<-2) \\\\ x^2-a & (x \\geq-2)\\end{cases} \\]"},
  {"order": 3, "type": "TEXT", "content": "가 실수 전체의 집합에서 연속일 때, 상수 \\(a\\) 의 값은?"}
]'::jsonb
       , 4
       , '[
  {"order": 1, "content":"6"},
  {"order": 2, "content":"7"},
  {"order": 3, "content":"8"},
  {"order": 4, "content":"9"},
  {"order": 5, "content":"10"}
]'::jsonb
       , 2
       , 'ALG'
       , 3
       , 'MCQ'
)
     , (
         (SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=\\left(x^2+1\\right)\\left(3 x^2-x\\right)\\) 에 대하여 \\(f^{\\prime}(1)\\) 의 값은?"}
]'::jsonb
       , 5
       , '[
  {"order": 1, "content":"8"},
  {"order": 2, "content":"10"},
  {"order": 3, "content":"12"},
  {"order": 4, "content":"14"},
  {"order": 5, "content":"16"}
]'::jsonb
       , 4
       , 'ALG'
       , 3
       , 'MCQ'
)
     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "\\(\\cos \\left(\\frac{\\pi}{2}+\\theta\\right)=-\\frac{1}{5}\\) 일 때, \\(\\frac{\\sin \\theta}{1-\\cos ^2 \\theta}\\) 의 값은?"}
]'::jsonb
, 6
, '[
  {"order": 1, "content":"-5"},
  {"order": 2, "content":"-\\sqrt{5}"},
  {"order": 3, "content":"0"},
  {"order": 4, "content":"\\sqrt{5}"},
  {"order": 5, "content":"5"}
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
  {"order": 1, "type": "TEXT", "content": "다항함수 \\(f(x)\\) 가 모든 실수 \\(x\\) 에 대하여"},
  {"order": 2, "type": "TEXT", "content": "\\[ \\int_0^x f(t) d t=3 x^3+2 x \\]"},
  {"order": 3, "type": "TEXT", "content": "를 만족시킬 때, \\(f(1)\\) 의 값은?"}
]'::jsonb
, 7
, '[
  {"order": 1, "content":"7"},
  {"order": 2, "content":"9"},
  {"order": 3, "content":"11"},
  {"order": 4, "content":"13"},
  {"order": 5, "content":"15"}
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
  {"order": 1, "type": "TEXT", "content": "두 실수 \\(a=2 \\log \\frac{1}{\\sqrt{10}}+\\log _2 20, b=\\log 2\\) 에 대하여 \\(a \\times b\\) 의 값은?"}
]'::jsonb
, 8
, '[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
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
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=3 x^2-16 x-20\\) 에 대하여"},
  {"order": 2, "type": "TEXT", "content": "\\[ \\int_{-2}^a f(x) d x=\\int_{-2}^0 f(x) d x \\]"},
  {"order": 3, "type": "TEXT", "content": "일 때, 양수 \\(a\\) 의 값은?"}
]'::jsonb
, 9
, '[
  {"order": 1, "content":"8"},
  {"order": 2, "content":"10"},
  {"order": 3, "content":"12"},
  {"order": 4, "content":"14"},
  {"order": 5, "content":"16"}
]'::jsonb
, 2
, 'ALG'
, 4
, 'MCQ'
)
,(
   (SELECT id FROM exam)
 , '[
  {"order": 1, "type": "TEXT", "content": "닫힌구간 \\([0,2 \\pi]\\) 에서 정의된 함수 \\(f(x)=a \\cos b x+3\\) 이 \\(x=\\frac{\\pi}{3}\\) 에서 최댓값 13 을 갖도록 하는 두 자연수 \\(a, b\\) 의 순서쌍 \\((a, b)\\) 에 대하여 \\(a+b\\) 의 최솟값은?"}
]'::jsonb
 , 10
 , '[
  {"order": 1, "content":"12"},
  {"order": 2, "content":"14"},
  {"order": 3, "content":"16"},
  {"order": 4, "content":"18"},
  {"order": 5, "content":"20"}
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
  {"order": 1, "type": "TEXT", "content": "시각 \\(t=0\\) 일 때 출발하여 수직선 위를 움직이는 점 P 의 시각 \\(t(t \\geq 0)\\) 에서의 위치 \\(x\\) 가"},
  {"order": 2, "type": "TEXT", "content": "\\[ x=t^3-\\frac{3}{2} t^2-6 t \\]"},
  {"order": 3, "type": "TEXT", "content": "이다. 출발한 후 점 P 의 운동 방향이 바뀌는 시각에서의 점 P 의 가속도는?"}
]'::jsonb
, 11
, '[
  {"order": 1, "content":"18"},
  {"order": 2, "content":"15"},
  {"order": 3, "content":"12"},
  {"order": 4, "content":"9"},
  {"order": 5, "content":"6"}
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
  {"order": 1, "type": "TEXT", "content": "\\(a_1=2\\) 인 수열 \\(\\left\\{a_n\\right\\}\\) 과 \\(b_1=2\\) 인 등차수열 \\(\\left\\{b_n\\right\\}\\) 이 모든 자연수 \\(n\\) 에 대하여"},
  {"order": 2, "type": "TEXT", "content": "\\[ \\sum_{k=1}^n \\frac{a_k}{b_{k+1}}=\\frac{1}{2} n^2 \\]"},
  {"order": 3, "type": "TEXT", "content": "을 만족시킬 때, \\(\\sum_{k=1}^5 a_k\\) 의 값은?"}
]'::jsonb
, 12
, '[
  {"order": 1, "content":"120"},
  {"order": 2, "content":"125"},
  {"order": 3, "content":"130"},
  {"order": 4, "content":"135"},
  {"order": 5, "content":"140"}
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
  {"order": 1, "type": "TEXT", "content": "최고차항의 계수가 1 인 삼차함수 \\(f(x)\\) 가"},
  {"order": 2, "type": "TEXT", "content": "\\[ f(1)=f(2)=0, \\quad f^{\\prime}(0)=-7 \\]"},
  {"order": 3, "type": "TEXT", "content": "을 만족시킨다. 원점 O 와 점 \\(\\mathrm{P}(3, f(3))\\) 에 대하여 선분 OP 가 곡선 \\(y=f(x)\\) 와 만나는 점 중 P 가 아닌 점을 Q 라 하자."},
  {"order": 4, "type": "TEXT", "content": "곡선 \\(y=f(x)\\) 와 \\(y\\) 축 및 선분 OQ 로 둘러싸인 부분의 넓이를 \\(A\\), 곡선 \\(y=f(x)\\) 와 선분 PQ 로 둘러싸인 부분의 넓이를 \\(B\\) 라 할 때, \\(B-A\\) 의 값은?"},
  {"order":5, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/alg13p.JPG"}
]'::jsonb
, 13
, '[
  {"order": 1, "content":"(1) \\(\\frac{37}{4}\\)"},
  {"order": 2, "content":"(2) \\(\\frac{39}{4}\\)"},
  {"order": 3, "content":"(3) \\(\\frac{41}{4}\\)"},
  {"order": 4, "content":"(4) \\(\\frac{43}{4}\\)"},
  {"order": 5, "content":"(5) \\(\\frac{45}{4}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "그림과 같이 삼각형 ABC 에서 선분 AB 위에 \\(\\overline{\\mathrm{AD}}: \\overline{\\mathrm{DB}}=3: 2\\) 인 점 D 를 잡고, 점 A 를 중심으로 하고 점 D 를 지나는 원을 \\(O\\), 원 \\(O\\) 와 선분 AC 가 만나는 점을 E 라 하자."},
  {"order": 2, "type": "TEXT", "content": "\\(\\sin A: \\sin C=8: 5\\) 이고, 삼각형 ADE 와 삼각형 ABC 의 넓이의 비가 \\(9: 35\\) 이다. 삼각형 ABC 의 외접원의 반지름의 길이가 7 일 때, 원 \\(O\\) 위의 점 P 에 대하여 삼각형 PBC 의 넓이의 최댓값은? (단, \\(\\overline{\\mathrm{AB}}<\\overline{\\mathrm{AC}}\\) )"},
  {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/alg14p.JPG"}
]'::jsonb
, 14
, '[
  {"order": 1, "content":"(1) \\(18+15 \\sqrt{3}\\)"},
  {"order": 2, "content":"(2) \\(24+20 \\sqrt{3}\\)"},
  {"order": 3, "content":"(3) \\(30+25 \\sqrt{3}\\)"},
  {"order": 4, "content":"(4) \\(36+30 \\sqrt{3}\\)"},
  {"order": 5, "content":"(5) \\(42+35 \\sqrt{3}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "상수 \\(a(a \\neq 3 \\sqrt{5})\\) 와 최고차항의 계수가 음수인 이차함수 \\(f(x)\\) 에 대하여 함수"},
  {"order": 2, "type": "TEXT", "content": "\\[ g(x)= \\begin{cases}x^3+a x^2+15 x+7 & (x \\leq 0) \\\\ f(x) & (x>0)\\end{cases} \\]"},
  {"order": 3, "type": "TEXT", "content": "이 다음 조건을 만족시킨다."},
  {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/alg15p.JPG"},
  {"order": 5, "type": "TEXT", "content": "\\(g(-2)+g(2)\\) 의 값은?"}
]'::jsonb
, 15
, '[
  {"order": 1, "content":"30"},
  {"order": 2, "content":"32"},
  {"order": 3, "content":"34"},
  {"order": 4, "content":"36"},
  {"order": 5, "content":"38"}
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
  {"order": 1, "type": "TEXT", "content": "방정식"},
  {"order": 2, "type": "TEXT", "content": "\\[ \\log _2(x-3)=\\log _4(3 x-5) \\]"},
  {"order": 3, "type": "TEXT", "content": "를 만족시키는 실수 \\(x\\) 의 값을 구하시오."}
]'::jsonb
, 16
, null
, 7
, 'ALG'
, 3
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "다항함수 \\(f(x)\\) 에 대하여 \\(f^{\\prime}(x)=9 x^2+4 x\\) 이고 \\(f(1)=6\\) 일 때, \\(f(2)\\) 의 값을 구하시오."}
]'::jsonb
, 17
, null
, 33
, 'ALG'
, 3
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 이 모든 자연수 \\(n\\) 에 대하여"},
  {"order": 2, "type": "TEXT", "content": "\\[ a_n+a_{n+4}=12 \\]"},
  {"order": 3, "type": "TEXT", "content": "를 만족시킬 때, \\(\\sum_{n=1}^{16} a_n\\) 의 값을 구하시오."}
]'::jsonb
, 18
, null
, 96
, 'ALG'
, 3
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "양수 \\(a\\) 에 대하여 함수 \\(f(x)\\) 를"},
  {"order": 2, "type": "TEXT", "content": "\\[ f(x)=2 x^3-3 a x^2-12 a^2 x \\]"},
  {"order": 3, "type": "TEXT", "content": "라 하자. 함수 \\(f(x)\\) 의 극댓값이 \\(\\frac{7}{27}\\) 일 때, \\(f(3)\\) 의 값을 구하시오."}
]'::jsonb
, 19
, null
, 41
, 'ALG'
, 3
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "곡선 \\(y=\\left(\\frac{1}{5}\\right)^{x-3}\\) 과 직선 \\(y=x\\) 가 만나는 점의 \\(x\\) 좌표를 \\(k\\) 라 하자. 실수 전체의 집합에서 정의된 함수 \\(f(x)\\) 가 다음 조건을 만족시킨다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/alg20p.JPG"},
  {"order": 3, "type": "TEXT", "content": "\\(f\\left(\\frac{1}{k^3 \\times 5^{3 k}}\\right)\\) 의 값을 구하시오."}
]'::jsonb
, 20
, null
, 36
, 'ALG'
, 4
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=x^3+a x^2+b x+4\\) 가 다음 조건을 만족시키도록 하는 두 정수 \\(a, b\\) 에 대하여 \\(f(1)\\) 의 최댓값을 구하시오."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/alg21p.JPG"}
]'::jsonb
, 21
, null
, 16
, 'ALG'
, 4
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "모든 항이 정수이고 다음 조건을 만족시키는 모든 수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 \\(\\left|a_1\\right|\\) 의 값의 합을 구하시오."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/alg22p.JPG"}
]'::jsonb
, 22
, null
, 64
, 'ALG'
, 4
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "다항식 \\(\\left(x^3+2\\right)^5\\) 의 전개식에서 \\(x^6\\) 의 계수는?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content":"40"},
  {"order": 2, "content":"50"},
  {"order": 3, "content":"60"},
  {"order": 4, "content":"70"},
  {"order": 5, "content":"80"}
]'::jsonb
, 5
, 'PROB'
, 2
, 'MCQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "두 사건 \\(A, B\\) 에 대하여"},
  {"order": 2, "type": "TEXT", "content": "\\[ \\mathrm{P}(A \\mid B)=\\mathrm{P}(A)=\\frac{1}{2}, \\quad \\mathrm{P}(A \\cap B)=\\frac{1}{5} \\]"},
  {"order": 3, "type": "TEXT", "content": "일 때, \\(\\mathrm{P}(A \\cup B)\\) 의 값은?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content":"\\(\\frac{1}{2}\\)"},
  {"order": 2, "content":"\\(\\frac{3}{5}\\)"},
  {"order": 3, "content":"\\(\\frac{7}{10}\\)"},
  {"order": 4, "content":"\\(\\frac{4}{5}\\)"},
  {"order": 5, "content":"\\(\\frac{9}{10}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "정규분포 \\(\\mathrm{N}\\left(m, 2^2\\right)\\) 을 따르는 모집단에서 크기가 256 인 표본을 임의추출하여 얻은 표본평균을 이용하여 구한 \\(m\\) 에 대한 신뢰도 \\(95 \\%\\) 의 신뢰구간이 \\(a \\leq m \\leq b\\) 이다."},
  {"order": 2, "type": "TEXT", "content": "\\(b-a\\) 의 값은? (단, \\(Z\\) 가 표준정규분포를 따르는 확률변수일 때, \\(\\mathrm{P}(|Z| \\leq 1.96)=0.95\\) 로 계산한다.)"}
]'::jsonb
, 25
, '[
  {"order": 1, "content":"0.49"},
  {"order": 2, "content":"0.52"},
  {"order": 3, "content":"0.55"},
  {"order": 4, "content":"0.58"},
  {"order": 5, "content":"0.61"}
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
  {"order": 1, "type": "TEXT", "content": "어느 학급의 학생 16 명을 대상으로 과목 A 와 과목 B 에 대한 선호도를 조사하였다. 이 조사에 참여한 학생은 과목 A 와 과목 B 중 하나를 선택하였고, 과목 A 를 선택한 학생은 9 명, 과목 B 를 선택한 학생은 7 명이다. 이 조사에 참여한 학생 16 명 중에서 임의로 3 명을 선택할 때, 선택한 3 명의 학생 중에서 적어도 한 명이 과목 B 를 선택한 학생일 확률은?"}
]'::jsonb
, 26
, '[
  {"order": 1, "content":"\\(\\frac{3}{4}\\)"},
  {"order": 2, "content":"\\(\\frac{4}{5}\\)"},
  {"order": 3, "content":"\\(\\frac{17}{20}\\)"},
  {"order": 4, "content":"\\(\\frac{9}{10}\\)"},
  {"order": 5, "content":"\\(\\frac{19}{20}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "숫자 \\(1,3,5,7,9\\) 가 각각 하나씩 적혀 있는 5 장의 카드가 들어 있는 주머니가 있다. 이 주머니에서 임의로 1 장의 카드를 꺼내어 카드에 적혀 있는 수를 확인한 후 다시 넣는 시행을 한다."},
  {"order": 2, "type": "TEXT", "content": "이 시행을 3 번 반복하여 확인한 세 개의 수의 평균을 \\(\\bar{X}\\) 라 하자. \\(\\mathrm{V}(a \\bar{X}+6)=24\\) 일 때, 양수 \\(a\\) 의 값은?"},
  {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/prob27p.JPG"}
]'::jsonb
, 27
, '[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
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
  {"order": 1, "type": "TEXT", "content": "집합 \\(X=\\{1,2,3,4,5,6\\}\\) 에 대하여 다음 조건을 만족시키는 함수 \\(f: X \\rightarrow X\\) 의 개수는?"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/prob28p.JPG"}
]'::jsonb
, 28
, '[
  {"order": 1, "content":"166"},
  {"order": 2, "content":"171"},
  {"order": 3, "content":"176"},
  {"order": 4, "content":"181"},
  {"order": 5, "content":"186"}
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
  {"order": 1, "type": "TEXT", "content": "정규분포 \\(\\mathrm{N}\\left(m_1, \\sigma_1^2\\right)\\) 을 따르는 확률변수 \\(X\\) 와 정규분포 \\(\\mathrm{N}\\left(m_2, \\sigma_2^2\\right)\\) 을 따르는 확률변수 \\(Y\\) 가 다음 조건을 만족시킨다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/prob29p.JPG"},
  {"order": 3, "type": "TEXT", "content": "\\[ \\mathrm{P}(15 \\leq X \\leq 20)+\\mathrm{P}(15 \\leq Y \\leq 20) \\text { 의 } \\]"},
  {"order": 4, "type": "TEXT", "content": "값을 오른쪽 표준정규분포표를 이용하여 구한 것이 0.4772 일 때, \\(m_1+\\sigma_2\\) 의 값을 구하시오."},
  {"order": 5, "type": "TEXT", "content": "(단, \\(\\sigma_1\\) 과 \\(\\sigma_2\\) 는 양수이다.)"},
  {"order": 6, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/prob29p2.JPG"}
]'::jsonb
, 29
, null
, 25
, 'PROB'
, 4
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "탁자 위에 5 개의 동전이 일렬로 놓여 있다. 이 5 개의 동전 중 1 번째 자리와 2 번째 자리의 동전은 앞면이 보이도록 놓여 있고, 나머지 자리의 3 개의 동전은 뒷면이 보이도록 놓여 있다. 이 5 개의 동전과 한 개의 주사위를 사용하여 다음 시행을 한다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/prob30p.JPG"},
  {"order": 3, "type": "TEXT", "content": "위의 시행을 3 번 반복한 후 이 5 개의 동전이 모두 앞면이 보이도록 놓여 있을 확률은 \\(\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"},
  {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/prob30p2.JPG"}
]'::jsonb
, 30
, null
, 19
, 'PROB'
, 4
, 'FRQ'
)
 ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "\\(\\lim _{x \\rightarrow 0} \\frac{3 x^2}{\\sin ^2 x}\\) 의 값은?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
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
  {"order": 1, "type": "TEXT", "content": "\\(\\int_0^{10} \\frac{x+2}{x+1} d x\\) 의 값은?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content":"\\(10+\\ln 5\\)"},
  {"order": 2, "content":"\\(10+\\ln 7\\)"},
  {"order": 3, "content":"\\(10+2 \\ln 3\\)"},
  {"order": 4, "content":"\\(10+\\ln 11\\)"},
  {"order": 5, "content":"\\(10+\\ln 13\\)"}
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
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 \\(\\lim _{n \\rightarrow \\infty} \\frac{n a_n}{n^2+3}=1\\) 일 때, \\(\\lim _{n \\rightarrow \\infty}\\left(\\sqrt{a_n{ }^2+n}-a_n\\right)\\) 의 값은?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content":"\\(\\frac{1}{3}\\)"},
  {"order": 2, "content":"\\(\\frac{1}{2}\\)"},
  {"order": 3, "content":"1"},
  {"order": 4, "content":"2"},
  {"order": 5, "content":"3"}
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
  {"order": 1, "type": "TEXT", "content": "그림과 같이 곡선 \\(y=\\sqrt{\\frac{x+1}{x(x+\\ln x)}}\\) 과 \\(x\\) 축 및 두 직선 \\(x=1, x=e\\) 로 둘러싸인 부분을 밑면으로 하는 입체도형이 있다. 이 입체도형을 \\(x\\) 축에 수직인 평면으로 자른 단면이 모두 정사각형일 때, 이 입체도형의 부피는?"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/calc26p.JPG"}
]'::jsonb
, 26
, '[
  {"order": 1, "content":"\\(\\ln (e+1)\\)"},
  {"order": 2, "content":"\\(\\ln (e+2)\\)"},
  {"order": 3, "content":"\\(\\ln (e+3)\\)"},
  {"order": 4, "content":"\\(\\ln (2 e+1)\\)"},
  {"order": 5, "content":"\\(\\ln (2 e+2)\\)"}
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
  {"order": 1, "type": "TEXT", "content": "최고차항의 계수가 1 인 삼차함수 \\(f(x)\\) 에 대하여 함수 \\(g(x)\\) 를"},
  {"order": 2, "type": "TEXT", "content": "\\[ g(x)=f\\left(e^x\\right)+e^x \\]"},
  {"order": 3, "type": "TEXT", "content": "이라 하자. 곡선 \\(y=g(x)\\) 위의 점 \\((0, g(0))\\) 에서의 접선이 \\(x\\) 축이고 함수 \\(g(x)\\) 가 역함수 \\(h(x)\\) 를 가질 때, \\(h^{\\prime}(8)\\) 의 값은?"}
]'::jsonb
, 27
, '[
  {"order": 1, "content":"\\(\\frac{1}{36}\\)"},
  {"order": 2, "content":"\\(\\frac{1}{18}\\)"},
  {"order": 3, "content":"\\(\\frac{1}{12}\\)"},
  {"order": 4, "content":"\\(\\frac{1}{9}\\)"},
  {"order": 5, "content":"\\(\\frac{5}{36}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "실수 전체의 집합에서 미분가능한 함수 \\(f(x)\\) 의 도함수 \\(f^{\\prime}(x)\\) 가"},
  {"order": 2, "type": "TEXT", "content": "\\[ f^{\\prime}(x)=-x+e^{1-x^2} \\]"},
  {"order": 3, "type": "TEXT", "content": "이다. 양수 \\(t\\) 에 대하여 곡선 \\(y=f(x)\\) 위의 점 \\((t, f(t))\\) 에서의 접선과 곡선 \\(y=f(x)\\) 및 \\(y\\) 축으로 둘러싸인 부분의 넓이를 \\(g(t)\\) 라 하자. \\(g(1)+g^{\\prime}(1)\\) 의 값은?"}
]'::jsonb
, 28
, '[
  {"order": 1, "content":"\\(\\frac{1}{2} e+\\frac{1}{2}\\)"},
  {"order": 2, "content":"\\(\\frac{1}{2} e+\\frac{2}{3}\\)"},
  {"order": 3, "content":"\\(\\frac{1}{2} e+\\frac{5}{6}\\)"},
  {"order": 4, "content":"\\(\\frac{2}{3} e+\\frac{1}{2}\\)"},
  {"order": 5, "content":"\\(\\frac{2}{3} e+\\frac{2}{3}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "등비수열 \\(\\left\\{a_n\\right\\}\\) 이"},
  {"order": 2, "type": "TEXT", "content": "\\[ \\sum_{n=1}^{\\infty}\\left(\\left|a_n\\right|+a_n\\right)=\\frac{40}{3}, \\quad \\sum_{n=1}^{\\infty}\\left(\\left|a_n\\right|-a_n\\right)=\\frac{20}{3} \\]"},
  {"order": 3, "type": "TEXT", "content": "을 만족시킨다. 부등식"},
  {"order": 4, "type": "TEXT", "content": "\\[ \\lim _{n \\rightarrow \\infty} \\sum_{k=1}^{2 n}\\left((-1)^{\\frac{k(k+1)}{2}} \\times a_{m+k}\\right)>\\frac{1}{700} \\]"},
  {"order": 5, "type": "TEXT", "content": "을 만족시키는 모든 자연수 \\(m\\) 의 값의 합을 구하시오."}
]'::jsonb
, 29
, null
, 25
, 'CALC'
, 4
, 'FRQ'
)
     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "두 상수 \\(a(1 \\leq a \\leq 2), b\\) 에 대하여 함수 \\(f(x)=\\sin (a x+b+\\sin x)\\) 가 다음 조건을 만족시킨다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/calc30p.JPG"},
  {"order": 3, "type": "TEXT", "content": "함수 \\(f(x)\\) 가 \\(x=\\alpha\\) 에서 극대인 \\(\\alpha\\) 의 값 중 열린구간 \\((0,4 \\pi)\\) 에 속하는 모든 값의 집합을 \\(A\\) 라 하자."},
  {"order": 4, "type": "TEXT", "content": "집합 \\(A\\) 의 원소의 개수를 \\(n\\), 집합 \\(A\\) 의 원소 중 가장 작은 값을 \\(\\alpha_1\\) 이라 하면, \\(n \\alpha_1-a b=\\frac{q}{p} \\pi\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
]'::jsonb
, 30
, null
, 17
, 'CALC'
, 4
, 'FRQ'
)
     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "두 벡터 \\(\\vec{a}=(k, 3), \\vec{b}=(1,2)\\) 에 대하여 \\(\\vec{a}+3 \\vec{b}=(6,9)\\) 일 때, \\(k\\) 의 값은?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
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
  {"order": 1, "type": "TEXT", "content": "꼭짓점의 좌표가 \\((1,0)\\) 이고, 준선이 \\(x=-1\\) 인 포물선이 점 \\((3, a)\\) 를 지날 때, 양수 \\(a\\) 의 값은?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
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
  {"order": 1, "type": "TEXT", "content": "좌표공간의 두 점 \\(\\mathrm{A}(a, b, 6), \\mathrm{B}(-4,-2, c)\\) 에 대하여 선분 AB 를 \\(3: 2\\) 로 내분하는 점이 \\(z\\) 축 위에 있고, 선분 AB 를 \\(3: 2\\) 로 외분하는 점이 \\(x y\\) 평면 위에 있을 때, \\(a+b+c\\) 의 값은?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content":"11"},
  {"order": 2, "content":"12"},
  {"order": 3, "content":"13"},
  {"order": 4, "content":"14"},
  {"order": 5, "content":"15"}
]'::jsonb
, 3
, 'GEO'
, 3
, 'MCQ'
)
     , (
         (SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content": "자연수 \\(n(n \\geq 2)\\) 에 대하여 직선 \\(x=\\frac{1}{n}\\) 이 두 타원"},
  {"order": 2, "type": "TEXT", "content": "\\[ C_1: \\frac{x^2}{2}+y^2=1, \\quad C_2: 2 x^2+\\frac{y^2}{2}=1 \\]"},
  {"order": 3, "type": "TEXT", "content": "과 만나는 제 1 사분면 위의 점을 각각 \\(\\mathrm{P}, \\mathrm{Q}\\) 라 하자. 타원 \\(C_1\\) 위의 점 P 에서의 접선의 \\(x\\) 절편을 \\(\\alpha\\), 타원 \\(C_2\\) 위의 점 Q 에서의 접선의 \\(x\\) 절편을 \\(\\beta\\) 라 할 때, \\(6 \\leq \\alpha-\\beta \\leq 15\\) 가 되도록 하는 모든 \\(n\\) 의 개수는?"}
]'::jsonb
       , 26
       , '[
  {"order": 1, "content":"7"},
  {"order": 2, "content":"9"},
  {"order": 3, "content":"11"},
  {"order": 4, "content":"13"},
  {"order": 5, "content":"15"}
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
  {"order": 1, "type": "TEXT", "content": "그림과 같이 \\(\\overline{\\mathrm{AB}}=6, \\overline{\\mathrm{BC}}=4 \\sqrt{5}\\) 인 사면체 ABCD 에 대하여 선분 BC 의 중점을 M 이라 하자. 삼각형 AMD 가 정삼각형이고 직선 BC 는 평면 AMD 와 수직일 때, 삼각형 ACD 에 내접하는 원의 평면 BCD 위로의 정사영의 넓이는?"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/geo27p.JPG"}
]'::jsonb
, 27
, '[
  {"order": 1, "content":"\\(\\frac{\\sqrt{10}}{4} \\pi\\)"},
  {"order": 2, "content":"\\(\\frac{\\sqrt{10}}{6} \\pi\\)"},
  {"order": 3, "content":"\\(\\frac{\\sqrt{10}}{8} \\pi\\)"},
  {"order": 4, "content":"\\(\\frac{\\sqrt{10}}{10} \\pi\\)"},
  {"order": 5, "content":"\\(\\frac{\\sqrt{10}}{12} \\pi\\)"}
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
  {"order": 1, "type": "TEXT", "content": "좌표공간에 \\(\\overline{\\mathrm{AB}}=8, \\overline{\\mathrm{BC}}=6, \\angle \\mathrm{ABC}=\\frac{\\pi}{2}\\) 인 직각삼각형 ABC 와 선분 AC 를 지름으로 하는 구 \\(S\\) 가 있다. 직선 AB 를 포함하고 평면 ABC 에 수직인 평면이 구 \\(S\\) 와 만나서 생기는 원을 \\(O\\) 라 하자."},
  {"order": 2, "type": "TEXT", "content": "원 \\(O\\) 위의 점 중에서 직선 AC 까지의 거리가 4 인 서로 다른 두 점을 \\(\\mathrm{P}, \\mathrm{Q}\\) 라 할 때, 선분 PQ 의 길이는?"},
  {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/geo28p.JPG"}
]'::jsonb
, 28
, '[
  {"order": 1, "content":"\\(\\sqrt{43}\\)"},
  {"order": 2, "content":"\\(\\sqrt{47}\\)"},
  {"order": 3, "content":"\\(\\sqrt{51}\\)"},
  {"order": 4, "content":"\\(\\sqrt{55}\\)"},
  {"order": 5, "content":"\\(\\sqrt{59}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "두 초점이 \\(\\mathrm{F}(c, 0), \\mathrm{F}^{\\prime}(-c, 0)(c>0)\\) 인 쌍곡선 \\(x^2-\\frac{y^2}{35}=1\\) 이 있다. 이 쌍곡선 위에 있는 제 1 사분면 위의 점 P 에 대하여 직선 \\(\\mathrm{PF}^{\\prime}\\) 위에 \\(\\overline{\\mathrm{PQ}}=\\overline{\\mathrm{PF}}\\) 인 점 Q 를 잡자. 삼각형 \\(\\mathrm{QF}^{\\prime} \\mathrm{F}\\) 와 삼각형 \\(\\mathrm{FF}^{\\prime} \\mathrm{P}\\) 가 서로 닮음일 때, 삼각형 PFQ 의 넓이는 \\(\\frac{q}{p} \\sqrt{5}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(\\overline{\\mathrm{PF}^{\\prime}}<\\overline{\\mathrm{QF}^{\\prime}}\\) 이고, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/geo29p.JPG"}
]'::jsonb
, 29
, null
, 107
, 'GEO'
, 4
, 'FRQ'
)
     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "좌표평면에 한 변의 길이가 4 인 정사각형 ABCD 가 있다."},
  {"order": 2, "type": "TEXT", "content": "\\[ |\\overrightarrow{\\mathrm{XB}}+\\overrightarrow{\\mathrm{XC}}|=|\\overrightarrow{\\mathrm{XB}}-\\overrightarrow{\\mathrm{XC}}| \\]"},
  {"order": 3, "type": "TEXT", "content": "를 만족시키는 점 X 가 나타내는 도형을 \\(S\\) 라 하자. 도형 \\(S\\) 위의 점 P 에 대하여"},
  {"order": 4, "type": "TEXT", "content": "\\[ 4 \\overrightarrow{\\mathrm{PQ}}=\\overrightarrow{\\mathrm{PB}}+2 \\overrightarrow{\\mathrm{PD}} \\]"},
  {"order": 5, "type": "TEXT", "content": "를 만족시키는 점을 Q 라 할 때, \\(\\overrightarrow{\\mathrm{AC}} \\cdot \\overrightarrow{\\mathrm{AQ}}\\) 의 최댓값과 최솟값을 각각 \\(M, m\\) 이라 하자. \\(M \\times m\\) 의 값을 구하시오."},
  {"order": 6, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/csat/geo30p.JPG"}
]'::jsonb
, 30
, null
, 316
, 'GEO'
, 4
, 'FRQ'
);




