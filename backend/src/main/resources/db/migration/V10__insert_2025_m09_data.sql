insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2025
       , 'M09'
       , '9월 모의평가'
       , 30
       , 6000);


WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2025 AND exam_type = 'M09' LIMIT 1
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
  {"order": 1, "type": "TEXT", "content": "\\(\\frac{\\sqrt[4]{32}}{\\sqrt[8]{4}}\\) 의 값은?"}
]'::jsonb
       , 1
       , '[
  {"order": 1, "content": "\\(\\sqrt{2}\\)"}
, {"order": 2, "content": "2"}
, {"order": 3, "content": "\\(2 \\sqrt{2}\\)"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "\\(4 \\sqrt{2}\\)"}
]'::jsonb
       , 2
       , 'ALG'
       , 2
       , 'MCQ'
)
     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=x^3+3 x^2-5\\) 에 대하여 \\(\\lim _{h \\rightarrow 0} \\frac{f(1+h)-f(1)}{h}\\) 의 값은?"}
]'::jsonb
, 2
, '[
  {"order": 1, "content": "5"}
, {"order": 2, "content": "6"}
, {"order": 3, "content": "7"}
, {"order": 4, "content": "8"}
, {"order": 5, "content": "9"}
]'::jsonb
, 5
, 'ALG'
, 2
, 'MCQ'
)
, (
    (SELECT id FROM exam)
  , '[
  {"order": 1, "type": "TEXT", "content": "모든 항이 실수인 등비수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[a_2 a_3=2, \\quad a_4=4\\]"}
, {"order": 3, "type": "TEXT", "content": "일 때, \\(a_6\\) 의 값은?"}
]'::jsonb
  , 3
  , '[
  {"order": 1, "content": "10"}
, {"order": 2, "content": "12"}
, {"order": 3, "content": "14"}
, {"order": 4, "content": "16"}
, {"order": 5, "content": "18"}
]'::jsonb
  , 4
  , 'ALG'
  , 3
  , 'MCQ'
)

     , (
         (SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(y=f(x)\\) 의 그래프가 그림과 같다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/alg4p.JPG"},
  {"order": 3, "type": "TEXT", "content": "\\(\\lim _{x \\rightarrow 0-} f(x)+\\lim _{x \\rightarrow 1+} f(x)\\) 의 값은?"}
]'::jsonb
       , 4
       , '[
  {"order": 1, "content": "-2"}
, {"order": 2, "content": "-1"}
, {"order": 3, "content": "0"}
, {"order": 4, "content": "1"}
, {"order": 5, "content": "2"}
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
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=(x+1)\\left(x^2+x-5\\right)\\) 에 대하여 \\(f^{\\prime}(2)\\) 의 값은?"}
]'::jsonb
, 5
, '[
  {"order": 1, "content": "15"}
, {"order": 2, "content": "16"}
, {"order": 3, "content": "17"}
, {"order": 4, "content": "18"}
, {"order": 5, "content": "19"}
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
  {"order": 1, "type": "TEXT", "content": "\\(\\frac{\\pi}{2}<\\theta<\\pi\\) 인 \\(\\theta\\) 에 대하여 \\(\\cos (\\pi+\\theta)=\\frac{2 \\sqrt{5}}{5}\\) 일 때, \\(\\sin \\theta+\\cos \\theta\\) 의 값은?"}
]'::jsonb
, 6
, '[
  {"order": 1, "content": "\\(-\\frac{2 \\sqrt{5}}{5}\\)"}
, {"order": 2, "content": "\\(-\\frac{\\sqrt{5}}{5}\\)"}
, {"order": 3, "content": "0"}
, {"order": 4, "content": "\\(\\frac{\\sqrt{5}}{5}\\)"}
, {"order": 5, "content": "\\(\\frac{2 \\sqrt{5}}{5}\\)"}
]'::jsonb
, 2
, 'ALG'
, 3
, 'MCQ'
)
,(
   (SELECT id FROM exam)
 , '[
  {"order": 1, "type": "TEXT", "content": "함수"}
, {"order": 2, "type": "TEXT", "content": "\\[f(x)= \\begin{cases}(x-a)^2 & (x<4) \\\\ 2 x-4 & (x \\geq 4)\\end{cases}\\]"}
, {"order": 3, "type": "TEXT", "content": "가 실수 전체의 집합에서 연속이 되도록 하는 모든 상수 \\(a\\) 의 값의 곱은?"}
]'::jsonb
 , 7
 , '[
  {"order": 1, "content": "6"}
, {"order": 2, "content": "9"}
, {"order": 3, "content": "12"}
, {"order": 4, "content": "15"}
, {"order": 5, "content": "18"}
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
  {"order": 1, "type": "TEXT", "content": "\\(a>2\\) 인 상수 \\(a\\) 에 대하여 두 수 \\(\\log _2 a, \\log _a 8\\) 의 합과 곱이 각각 \\(4, k\\) 일 때, \\(a+k\\) 의 값은?"}
]'::jsonb
, 8
, '[
  {"order": 1, "content": "11"}
, {"order": 2, "content": "12"}
, {"order": 3, "content": "13"}
, {"order": 4, "content": "14"}
, {"order": 5, "content": "15"}
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
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=x^2+x\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[5 \\int_0^1 f(x) d x-\\int_0^1(5 x+f(x)) d x\\]"}
, {"order": 3, "type": "TEXT", "content": "의 값은?"}
]'::jsonb
, 9
, '[
  {"order": 1, "content": "\\(\\frac{1}{6}\\)"}
, {"order": 2, "content": "\\(\\frac{1}{3}\\)"}
, {"order": 3, "content": "\\(\\frac{1}{2}\\)"}
, {"order": 4, "content": "\\(\\frac{2}{3}\\)"}
, {"order": 5, "content": "\\(\\frac{5}{6}\\)"}
]'::jsonb
, 5
, 'ALG'
, 4
, 'MCQ'
)
,(
   (SELECT id FROM exam)
 , '[
  {"order": 1, "type": "TEXT", "content": "\\(\\angle \\mathrm{A}>\\frac{\\pi}{2}\\) 인 삼각형 ABC 의 꼭짓점 A 에서 선분 BC 에 내린 수선의 발을 H 라 하자."}
, {"order": 2, "type": "TEXT", "content": "\\[\\overline{\\mathrm{AB}}: \\overline{\\mathrm{AC}}=\\sqrt{2}: 1, \\quad \\overline{\\mathrm{AH}}=2\\]"}
, {"order": 3, "type": "TEXT", "content": "이고, 삼각형 ABC 의 외접원의 넓이가 \\(50 \\pi\\) 일 때, 선분 BH 의 길이는?"}
]'::jsonb
 , 10
 , '[
  {"order": 1, "content": "6"}
, {"order": 2, "content": "\\(\\frac{25}{4}\\)"}
, {"order": 3, "content": "\\(\\frac{13}{2}\\)"}
, {"order": 4, "content": "\\(\\frac{27}{4}\\)"}
, {"order": 5, "content": "7"}
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
  {"order": 1, "type": "TEXT", "content": "수직선 위를 움직이는 두 점 \\(\\mathrm{P}, \\mathrm{Q}\\) 의 시각 \\(t(t \\geq 0)\\) 에서의 위치가 각각"}
, {"order": 2, "type": "TEXT", "content": "\\[x_1=t^2+t-6, \\quad x_2=-t^3+7 t^2\\]"}
, {"order": 3, "type": "TEXT", "content": "이다. 두 점 \\(\\mathrm{P}, \\mathrm{Q}\\) 의 위치가 같아지는 순간 두 점 \\(\\mathrm{P}, \\mathrm{Q}\\) 의 가속도를 각각 \\(p, q\\) 라 할 때, \\(p-q\\) 의 값은?"}
]'::jsonb
, 11
, '[
  {"order": 1, "content": "24"}
, {"order": 2, "content": "27"}
, {"order": 3, "content": "30"}
, {"order": 4, "content": "33"}
, {"order": 5, "content": "36"}
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
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 은 등차수열이고, 수열 \\(\\left\\{b_n\\right\\}\\) 은 모든 자연수 \\(n\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[b_n=\\sum_{k=1}^n(-1)^{k+1} a_k\\]"}
, {"order": 3, "type": "TEXT", "content": "를 만족시킨다. \\(b_2=-2, b_3+b_7=0\\) 일 때, 수열 \\(\\left\\{b_n\\right\\}\\) 의 첫째항부터 제9항까지의 합은?"}
]'::jsonb
, 12
, '[
  {"order": 1, "content": "-22"}
, {"order": 2, "content": "-20"}
, {"order": 3, "content": "-18"}
, {"order": 4, "content": "-16"}
, {"order": 5, "content": "-14"}
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
  {"order": 1, "type": "TEXT", "content": "함수"}
, {"order": 2, "type": "TEXT", "content": "\\[f(x)= \\begin{cases}-x^2-2 x+6 & (x<0) \\\\ -x^2+2 x+6 & (x \\geq 0)\\end{cases}\\]"}
, {"order": 3, "type": "TEXT", "content": "의 그래프가 \\(x\\) 축과 만나는 서로 다른 두 점을 \\(\\mathrm{P}, \\mathrm{Q}\\) 라 하고, 상수 \\(k(k>4)\\) 에 대하여 직선 \\(x=k\\) 가 \\(x\\) 축과 만나는 점을 R 이라 하자. 곡선 \\(y=f(x)\\) 와 선분 PQ 로 둘러싸인 부분의 넓이를 \\(A\\), 곡선 \\(y=f(x)\\) 와 직선 \\(x=k\\) 및 선분 QR 로 둘러싸인 부분의 넓이를 \\(B\\) 라 하자. \\(A=2 B\\) 일 때, \\(k\\) 의 값은? (단, 점 P 의 \\(x\\) 좌표는 음수이다.)"}
]'::jsonb
, 13
, '[
  {"order": 1, "content": "\\(\\frac{9}{2}\\)"}
, {"order": 2, "content": "5"}
, {"order": 3, "content": "\\(\\frac{11}{2}\\)"}
, {"order": 4, "content": "6"}
, {"order": 5, "content": "\\(\\frac{13}{2}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "자연수 \\(n\\) 에 대하여 곡선 \\(y=2^x\\) 위의 두 점 \\(\\mathrm{A}_n, \\mathrm{~B}_n\\) 이 다음 조건을 만족시킨다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/alg14p.JPG"}
, {"order": 3, "type": "TEXT", "content": "중심이 직선 \\(y=x\\) 위에 있고 두 점 \\(\\mathrm{A}_n, \\mathrm{~B}_n\\) 을 지나는 원이 곡선 \\(y=\\log _2 x\\) 와 만나는 두 점의 \\(x\\) 좌표 중 큰 값을 \\(x_n\\) 이라 하자. \\(x_1+x_2+x_3\\) 의 값은?"}
]'::jsonb
, 14
, '[
  {"order": 1, "content": "\\(\\frac{150}{7}\\)"}
, {"order": 2, "content": "\\(\\frac{155}{7}\\)"}
, {"order": 3, "content": "\\(\\frac{160}{7}\\)"}
, {"order": 4, "content": "\\(\\frac{165}{7}\\)"}
, {"order": 5, "content": "\\(\\frac{170}{7}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "두 다항함수 \\(f(x), g(x)\\) 는 모든 실수 \\(x\\) 에 대하여 다음 조건을 만족시킨다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/alg15p.JPG"}
, {"order": 3, "type": "TEXT", "content": "\\[\\int_0^3 g(x) d x \\text { 의 값은? }\\]"}
]'::jsonb
, 15
, '[
  {"order": 1, "content": "72"}
, {"order": 2, "content": "76"}
, {"order": 3, "content": "80"}
, {"order": 4, "content": "84"}
, {"order": 5, "content": "88"}
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
  {"order": 1, "type": "TEXT", "content": "방정식"}
, {"order": 2, "type": "TEXT", "content": "\\[\\log _3(x+2)-\\log _{\\frac{1}{3}}(x-4)=3\\]"}
, {"order": 3, "type": "TEXT", "content": "을 만족시키는 실수 \\(x\\) 의 값을 구하시오."}
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
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)\\) 에 대하여 \\(f^{\\prime}(x)=6 x^2+2 x+1\\) 이고 \\(f(0)=1\\) 일 때, \\(f(1)\\) 의 값을 구하시오."}
]'::jsonb
, 17
, null
, 5
, 'ALG'
, 3
, 'FRQ'
)

     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[\\sum_{k=1}^{10} k a_k=36, \\quad \\sum_{k=1}^9 k a_{k+1}=7\\]"}
, {"order": 3, "type": "TEXT", "content": "일 때, \\(\\sum_{k=1}^{10} a_k\\) 의 값을 구하시오."}
]'::jsonb
, 18
, null
, 29
, 'ALG'
, 3
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=x^3+a x^2-9 x+b\\) 는 \\(x=1\\) 에서 극소이다. 함수 \\(f(x)\\) 의 극댓값이 28 일 때, \\(a+b\\) 의 값을 구하시오. (단, \\(a\\) 와 \\(b\\) 는 상수이다.)"}
]'::jsonb
, 19
, null
, 4
, 'ALG'
, 3
, 'FRQ'
)
 ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "닫힌구간 \\([0,2 \\pi]\\) 에서 정의된 함수"}
, {"order": 2, "type": "TEXT", "content": "\\[f(x)= \\begin{cases}\\sin x-1 & (0 \\leq x<\\pi) \\\\ -\\sqrt{2} \\sin x-1 & (\\pi \\leq x \\leq 2 \\pi)\\end{cases}\\]"}
, {"order": 3, "type": "TEXT", "content": "가 있다. \\(0 \\leq t \\leq 2 \\pi\\) 인 실수 \\(t\\) 에 대하여 \\(x\\) 에 대한 방정식 \\(f(x)=f(t)\\) 의 서로 다른 실근의 개수가 3 이 되도록 하는 모든 \\(t\\) 의 값의 합은 \\(\\frac{q}{p} \\pi\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
]'::jsonb
, 20
, null
, 15
, 'ALG'
, 4
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "최고차항의 계수가 1 인 삼차함수 \\(f(x)\\) 가 모든 정수 \\(k\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[2 k-8 \\leq \\frac{f(k+2)-f(k)}{2} \\leq 4 k^2+14 k\\]"}
, {"order": 3, "type": "TEXT", "content": "를 만족시킬 때, \\(f^{\\prime}(3)\\) 의 값을 구하시오."}
]'::jsonb
, 21
, null
, 31
, 'ALG'
, 4
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "양수 \\(k\\) 에 대하여 \\(a_1=k\\) 인 수열 \\(\\left\\{a_n\\right\\}\\) 이 다음 조건을 만족시킨다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/alg22p.JPG"}
, {"order": 3, "type": "TEXT", "content": "\\(a_5=0\\) 이 되도록 하는 서로 다른 모든 양수 \\(k\\) 에 대하여 \\(k^2\\) 의 값의 합을 구하시오."}
]'::jsonb
, 22
, null
, 8
, 'ALG'
, 4
, 'FRQ'
)
,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "다섯 개의 숫자 \\(1,2,2,3,3\\) 을 모두 일렬로 나열하는 경우의 수는?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content": "10"}
, {"order": 2, "content": "15"}
, {"order": 3, "content": "20"}
, {"order": 4, "content": "25"}
, {"order": 5, "content": "30"}
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
  {"order": 1, "type": "TEXT", "content": "두 사건 \\(A, B\\) 는 서로 독립이고"}
, {"order": 2, "type": "TEXT", "content": "\\[\\mathrm{P}(A)=\\frac{2}{3}, \\quad \\mathrm{P}(A \\cap B)=\\frac{1}{6}\\]"}
, {"order": 3, "type": "TEXT", "content": "일 때, \\(\\mathrm{P}(A \\cup B)\\) 의 값은?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content": "\\(\\frac{3}{4}\\)"}
, {"order": 2, "content": "\\(\\frac{19}{24}\\)"}
, {"order": 3, "content": "\\(\\frac{5}{6}\\)"}
, {"order": 4, "content": "\\(\\frac{7}{8}\\)"}
, {"order": 5, "content": "\\(\\frac{11}{12}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "1 부터 11 까지의 자연수 중에서 임의로 서로 다른 2 개의 수를 선택한다. 선택한 2 개의 수 중 적어도 하나가 7 이상의 홀수일 확률은?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content": "\\(\\frac{23}{55}\\)"}
, {"order": 2, "content": "\\(\\frac{24}{55}\\)"}
, {"order": 3, "content": "\\(\\frac{5}{11}\\)"}
, {"order": 4, "content": "\\(\\frac{26}{55}\\)"}
, {"order": 5, "content": "\\(\\frac{27}{55}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "정규분포 \\(\\mathrm{N}\\left(m, 6^2\\right)\\) 을 따르는 모집단에서 크기가 9 인 표본을 임의추출하여 구한 표본평균을 \\(\\bar{X}\\), 정규분포 \\(\\mathrm{N}\\left(6,2^2\\right)\\) 을 따르는 모집단에서 크기가 4 인 표본을 임의추출하여 구한 표본평균을 \\(\\bar{Y}\\) 라 하자. \\(\\mathrm{P}(\\bar{X} \\leq 12)+\\mathrm{P}(\\bar{Y} \\geq 8)=1\\) 이 되도록 하는 \\(m\\) 의 값은?"}
]'::jsonb
, 26
, '[
  {"order": 1, "content": "5"}
, {"order": 2, "content": "\\(\\frac{13}{2}\\)"}
, {"order": 3, "content": "8"}
, {"order": 4, "content": "\\(\\frac{19}{2}\\)"}
, {"order": 5, "content": "11"}
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
  {"order": 1, "type": "TEXT", "content": "이산확률변수 \\(X\\) 가 가지는 값이 0 부터 4 까지의 정수이고"}
, {"order": 2, "type": "TEXT", "content": "\\[\\mathrm{P}(X=k)=\\mathrm{P}(X=k+2)(k=0,1,2)\\]"}
, {"order": 3, "type": "TEXT", "content": "이다. \\(\\mathrm{E}\\left(X^2\\right)=\\frac{35}{6}\\) 일 때, \\(\\mathrm{P}(X=0)\\) 의 값은?"}
]'::jsonb
, 27
, '[
  {"order": 1, "content": "\\(\\frac{1}{24}\\)"}
, {"order": 2, "content": "\\(\\frac{1}{12}\\)"}
, {"order": 3, "content": "\\(\\frac{1}{8}\\)"}
, {"order": 4, "content": "\\(\\frac{1}{6}\\)"}
, {"order": 5, "content": "\\(\\frac{5}{24}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "집합 \\(X=\\{1,2,3,4\\}\\) 에 대하여 \\(f: X \\rightarrow X\\) 인 모든 함수 \\(f\\) 중에서 임의로 하나를 선택하는 시행을 한다."}
, {"order": 2, "type": "TEXT", "content": "이 시행에서 선택한 함수 \\(f\\) 가 다음 조건을 만족시킬 때, \\(f(4)\\) 가 짝수일 확률은?"}
, {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/prob28p.JPG"}
]'::jsonb
, 28
, '[
  {"order": 1, "content": "\\(\\frac{9}{19}\\)"}
, {"order": 2, "content": "\\(\\frac{8}{15}\\)"}
, {"order": 3, "content": "\\(\\frac{3}{5}\\)"}
, {"order": 4, "content": "\\(\\frac{27}{40}\\)"}
, {"order": 5, "content": "\\(\\frac{19}{25}\\)"}
]'::jsonb
, 4
, 'PROB'
, 4
, 'MCQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "수직선의 원점에 점 A 가 있다. 한 개의 주사위를 사용하여 다음 시행을 한다."}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/prob29p.JPG"}
, {"order": 3, "type": "TEXT", "content": "이 시행을 16200 번 반복하여 이동된 점 A 의 위치가 5700 이하일 확률을 오른쪽 표준정규분포표를 이용하여 구한 값을 \\(k\\) 라 하자. \\(1000 \\times k\\) 의 값을 구하시오."}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/prob29p2.JPG"}
]'::jsonb
, 29
, null
, 994
, 'PROB'
, 4
, 'FRQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "흰 공 4 개와 검은 공 4 개를 세 명의 학생 \\(\\mathrm{A}, \\mathrm{B}, \\mathrm{C}\\) 에게 다음 규칙에 따라 남김없이 나누어 주는 경우의 수를 구하시오."}
, {"order": 2, "type": "TEXT", "content": "(단, 같은 색 공끼리는 서로 구별하지 않고, 공을 받지 못하는 학생이 있을 수 있다.)"}
, {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/prob30p.JPG"}
]'::jsonb
, 30
, null
, 93
, 'PROB'
, 4
, 'FRQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "\\(\\lim _{x \\rightarrow 0} \\frac{\\sin 5 x}{x}\\) 의 값은?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "2"}
, {"order": 3, "content": "3"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "5"}
]'::jsonb
, 5
, 'CALC'
, 2
, 'MCQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "양의 실수 전체의 집합에서 정의된 미분가능한 함수 \\(f(x)\\) 가 있다. 양수 \\(t\\) 에 대하여 곡선 \\(y=f(x)\\) 위의 점 \\((t, f(t))\\) 에서의 접선의 기울기는 \\(\\frac{1}{t}+4 e^{2 t}\\) 이다. \\(f(1)=2 e^2+1\\) 일 때, \\(f(e)\\) 의 값은?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content": "\\(2 e^{2 e}-1\\)"}
, {"order": 2, "content": "\\(2 e^{2 e}\\)"}
, {"order": 3, "content": "\\(2 e^{2 e}+1\\)"}
, {"order": 4, "content": "\\(2 e^{2 e}+2\\)"}
, {"order": 5, "content": "\\(2 e^{2 e}+3\\)"}
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
  {"order": 1, "type": "TEXT", "content": "등비수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[\\lim _{n \\rightarrow \\infty} \\frac{4^n \\times a_n-1}{3 \\times 2^{n+1}}=1\\]"}
, {"order": 3, "type": "TEXT", "content": "일 때, \\(a_1+a_2\\) 의 값은?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content": "\\(\\frac{3}{2}\\)"}
, {"order": 2, "content": "\\(\\frac{5}{2}\\)"}
, {"order": 3, "content": "\\(\\frac{7}{2}\\)"}
, {"order": 4, "content": "\\(\\frac{9}{2}\\)"}
, {"order": 5, "content": "\\(\\frac{11}{2}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "그림과 같이 곡선 \\(y=2 x \\sqrt{x \\sin x^2}(0 \\leq x \\leq \\sqrt{\\pi})\\) 와 \\(x\\) 축 및 두 직선 \\(x=\\sqrt{\\frac{\\pi}{6}}, x=\\sqrt{\\frac{\\pi}{2}}\\) 로 둘러싸인 부분을 밑면으로 하는 입체도형이 있다. 이 입체도형을 \\(x\\) 축에 수직인 평면으로 자른 단면이 모두 반원일 때, 이 입체도형의 부피는?"}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/calc26p.JPG"}
]'::jsonb
, 26
, '[
  {"order": 1, "content": "\\(\\frac{\\pi^2+6 \\pi}{48}\\)"}
, {"order": 2, "content": "\\(\\frac{\\sqrt{2} \\pi^2+6 \\pi}{48}\\)"}
, {"order": 3, "content": "\\(\\frac{\\sqrt{3} \\pi^2+6 \\pi}{48}\\)"}
, {"order": 4, "content": "\\(\\frac{\\sqrt{2} \\pi^2+12 \\pi}{48}\\)"}
, {"order": 5, "content": "\\(\\frac{\\sqrt{3} \\pi^2+12 \\pi}{48}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "실수 전체의 집합에서 미분가능한 함수 \\(f(x)\\) 가 모든 실수 \\(x\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[f(x)+f\\left(\\frac{1}{2} \\sin x\\right)=\\sin x\\]"}
, {"order": 3, "type": "TEXT", "content": "를 만족시킬 때, \\(f^{\\prime}(\\pi)\\) 의 값은?"}
]'::jsonb
, 27
, '[
  {"order": 1, "content": "\\(-\\frac{5}{6}\\)"}
, {"order": 2, "content": "\\(-\\frac{2}{3}\\)"}
, {"order": 3, "content": "\\(-\\frac{1}{2}\\)"}
, {"order": 4, "content": "\\(-\\frac{1}{3}\\)"}
, {"order": 5, "content": "\\(-\\frac{1}{6}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)\\) 는 실수 전체의 집합에서 연속인 이계도함수를 갖고, 실수 전체의 집합에서 정의된 함수 \\(g(x)\\) 를"}
, {"order": 2, "type": "TEXT", "content": "\\[g(x)=f^{\\prime}(2 x) \\sin \\pi x+x\\]"}
, {"order": 3, "type": "TEXT", "content": "라 하자. 함수 \\(g(x)\\) 는 역함수 \\(g^{-1}(x)\\) 를 갖고,"}
, {"order": 4, "type": "TEXT", "content": "\\[\\int_0^1 g^{-1}(x) d x=2 \\int_0^1 f^{\\prime}(2 x) \\sin \\pi x d x+\\frac{1}{4}\\]"}
, {"order": 5, "type": "TEXT", "content": "을 만족시킬 때, \\(\\int_0^2 f(x) \\cos \\frac{\\pi}{2} x d x\\) 의 값은?"}
]'::jsonb
, 28
, '[
  {"order": 1, "content": "\\(-\\frac{1}{\\pi}\\)"}
, {"order": 2, "content": "\\(-\\frac{1}{2 \\pi}\\)"}
, {"order": 3, "content": "\\(-\\frac{1}{3 \\pi}\\)"}
, {"order": 4, "content": "\\(-\\frac{1}{4 \\pi}\\)"}
, {"order": 5, "content": "\\(-\\frac{1}{5 \\pi}\\)"}
]'::jsonb
, 3
, 'CALC'
, 4
, 'MCQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 의 첫째항부터 제 \\(m\\) 항까지의 합을 \\(S_m\\) 이라 하자. 모든 자연수 \\(m\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[S_m=\\sum_{n=1}^{\\infty} \\frac{m+1}{n(n+m+1)}\\]"}
, {"order": 3, "type": "TEXT", "content": "일 때, \\(a_1+a_{10}=\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오."}
, {"order": 4, "type": "TEXT", "content": "(단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
]'::jsonb
, 29
, null
, 57
, 'CALC'
, 4
, 'FRQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "양수 \\(k\\) 에 대하여 함수 \\(f(x)\\) 를"}
, {"order": 2, "type": "TEXT", "content": "\\[f(x)=(k-|x|) e^{-x}\\]"}
, {"order": 3, "type": "TEXT", "content": "이라 하자. 실수 전체의 집합에서 미분가능하고 다음 조건을 만족시키는 모든 함수 \\(F(x)\\) 에 대하여 \\(F(0)\\) 의 최솟값을 \\(g(k)\\) 라 하자."}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/calc30p.JPG"}
, {"order": 5, "type": "TEXT", "content": "\\(g\\left(\\frac{1}{4}\\right)+g\\left(\\frac{3}{2}\\right)=p e+q\\) 일 때, \\(100(p+q)\\) 의 값을 구하시오. (단, \\(\\lim _{x \\rightarrow \\infty} x e^{-x}=0\\) 이고, \\(p\\) 와 \\(q\\) 는 유리수이다.)"}
]'::jsonb
, 30
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
  {"order": 1, "type": "TEXT", "content": "두 벡터 \\(\\vec{a}=(4,0), \\vec{b}=(1,3)\\) 에 대하여 \\(2 \\vec{a}+\\vec{b}=(9, k)\\) 일 때, \\(k\\) 의 값은?"}
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
  {"order": 1, "type": "TEXT", "content": "타원 \\(\\frac{x^2}{4^2}+\\frac{y^2}{b^2}=1\\) 의 두 초점 사이의 거리가 6 일 때, \\(b^2\\) 의 값은? (단, \\(0<b<4\\) )"}
]'::jsonb
, 24
, '[
  {"order": 1, "content": "4"}
, {"order": 2, "content": "5"}
, {"order": 3, "content": "6"}
, {"order": 4, "content": "7"}
, {"order": 5, "content": "8"}
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
  {"order": 1, "type": "TEXT", "content": "좌표공간의 서로 다른 두 점 \\(\\mathrm{A}(a, b,-5), \\mathrm{B}(-8,6, c)\\) 에 대하여 선분 AB 의 중점이 \\(z x\\) 평면 위에 있고, 선분 AB 를 \\(1: 2\\) 로 내분하는 점이 \\(y\\) 축 위에 있을 때, \\(a+b+c\\) 의 값은?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content": "-8"}
, {"order": 2, "content": "-4"}
, {"order": 3, "content": "0"}
, {"order": 4, "content": "4"}
, {"order": 5, "content": "8"}
]'::jsonb
, 5
, 'GEO'
, 3
, 'MCQ'
)

, (
    (SELECT id FROM exam)
  , '[
  {"order": 1, "type": "TEXT", "content": "좌표평면에서 점 \\((1,0)\\) 을 중심으로 하고 반지름의 길이가 6 인 원을 \\(C\\) 라 하자. 포물선 \\(y^2=4 x\\) 위의 점 \\(\\left(n^2, 2 n\\right)\\) 에서의 접선이 원 \\(C\\) 와 만나도록 하는 자연수 \\(n\\) 의 개수는?"}
]'::jsonb
  , 26
  , '[
  {"order": 1, "content": "1"}
, {"order": 2, "content": "3"}
, {"order": 3, "content": "5"}
, {"order": 4, "content": "7"}
, {"order": 5, "content": "9"}
]'::jsonb
  , 3
  , 'GEO'
  , 3
  , 'MCQ'
)
     ,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "그림과 같이 한 변의 길이가 각각 4,6 인 두 정사각형 \\(\\mathrm{ABCD}, \\mathrm{EFGH}\\) 를 밑면으로 하고"}
, {"order": 2, "type": "TEXT", "content": "\\[\\overline{\\mathrm{AE}}=\\overline{\\mathrm{BF}}=\\overline{\\mathrm{CG}}=\\overline{\\mathrm{DH}}\\]"}
, {"order": 3, "type": "TEXT", "content": "인 사각뿔대 \\(\\mathrm{ABCD}-\\mathrm{EFGH}\\) 가 있다. 사각뿔대 \\(\\mathrm{ABCD}-\\mathrm{EFGH}\\) 의 높이가 \\(\\sqrt{14}\\) 일 때, 사각형 AEHD 의 평면 BFGC 위로의 정사영의 넓이는?"}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/geo27p.JPG"}
]'::jsonb
, 27
, '[
  {"order": 1, "content": "\\(\\frac{10}{3} \\sqrt{15}\\)"}
, {"order": 2, "content": "\\(\\frac{11}{3} \\sqrt{15}\\)"}
, {"order": 3, "content": "\\(4 \\sqrt{15}\\)"}
, {"order": 4, "content": "\\(\\frac{13}{3} \\sqrt{15}\\)"}
, {"order": 5, "content": "\\(\\frac{14}{3} \\sqrt{15}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "좌표공간에 두 점 \\(\\mathrm{A}(a, 0,0), \\mathrm{B}(0,10 \\sqrt{2}, 0)\\) 과 구 \\(S: x^2+y^2+z^2=100\\) 이 있다. \\(\\angle \\mathrm{APO}=\\frac{\\pi}{2}\\) 인 구 \\(S\\) 위의 모든 점 P 가 나타내는 도형을 \\(C_1, \\angle \\mathrm{BQO}=\\frac{\\pi}{2}\\) 인 구 \\(S\\) 위의 모든 점 Q 가 나타내는 도형을 \\(C_2\\) 라 하자. \\(C_1\\) 과 \\(C_2\\) 가 서로 다른 두 점 \\(\\mathrm{N}_1, \\mathrm{~N}_2\\) 에서 만나고 \\(\\cos \\left(\\angle \\mathrm{N}_1 \\mathrm{ON}_2\\right)=\\frac{3}{5}\\) 일 때, \\(a\\) 의 값은? (단, \\(a>10 \\sqrt{2}\\) 이고, O 는 원점이다.)"}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/geo28p.JPG"}
]'::jsonb
, 28
, '[
  {"order": 1, "content": "\\(\\frac{10}{3} \\sqrt{30}\\)"}
, {"order": 2, "content": "\\(\\frac{15}{4} \\sqrt{30}\\)"}
, {"order": 3, "content": "\\(\\frac{25}{6} \\sqrt{30}\\)"}
, {"order": 4, "content": "\\(\\frac{55}{12} \\sqrt{30}\\)"}
, {"order": 5, "content": "\\(5 \\sqrt{30}\\)"}
]'::jsonb
, 1
, 'GEO'
, 4
, 'MCQ'
)

,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "그림과 같이 두 점 \\(\\mathrm{F}(4,0), \\mathrm{F}^{\\prime}(-4,0)\\) 을 초점으로 하는 쌍곡선 \\(C: \\frac{x^2}{a^2}-\\frac{y^2}{b^2}=1\\) 이 있다. 점 F 를 초점으로 하고 \\(y\\) 축을 준선으로 하는 포물선이 쌍곡선 \\(C\\) 와 만나는 점 중 제 1 사분면 위의 점을 P 라 하자. 점 P 에서 \\(y\\) 축에 내린 수선의 발을 H 라 할 때, \\(\\overline{\\mathrm{PH}}: \\overline{\\mathrm{HF}}=3: 2 \\sqrt{2}\\) 이다. \\(a^2 \\times b^2\\) 의 값을 구하시오. (단, \\(a>b>0\\) )"}
, {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/geo29p.JPG"}
]'::jsonb
, 29
, null
, 63
, 'GEO'
, 4
, 'FRQ'
)



,
(
  (SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content": "좌표평면 위에 다섯 점"}
, {"order": 2, "type": "TEXT", "content": "\\[\\mathrm{A}(0,8), \\mathrm{B}(8,0), \\mathrm{C}(7,1), \\mathrm{D}(7,0), \\mathrm{E}(-4,2)\\]"}
, {"order": 3, "type": "TEXT", "content": "가 있다. 삼각형 AOB 의 변 위를 움직이는 점 P 와 삼각형 CDB 의 변 위를 움직이는 점 Q 에 대하여 \\(|\\overrightarrow{\\mathrm{PQ}}+\\overrightarrow{\\mathrm{OE}}|^2\\) 의 최댓값을 \\(M\\), 최솟값을 \\(m\\) 이라 할 때, \\(M+m\\) 의 값을 구하시오. (단, O 는 원점이다.)"}
, {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m09/geo30p.JPG"}
]'::jsonb
, 30
, null
, 54
, 'GEO'
, 4
, 'FRQ'
)


;






