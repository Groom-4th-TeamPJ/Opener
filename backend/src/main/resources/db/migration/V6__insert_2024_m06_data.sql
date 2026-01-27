insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2024
       , 'M06'
       , '6월 모의평가'
       , 30
       , 6000);


WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2024 AND exam_type = 'M06' LIMIT 1
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
      {"order":1,"type":"TEXT","content":"\\(\\sqrt[3]{27} \\times 4^{-\\frac{1}{2}}\\) 의 값은?"}
    ]'::jsonb,
    1,
    '[
      {"order":1,"content":"\\(\\frac{1}{2}\\)"},
      {"order":2,"content":"\\(\\frac{3}{4}\\)"},
      {"order":3,"content":"1"},
      {"order":4,"content":"\\(\\frac{5}{4}\\)"},
      {"order":5,"content":"\\(\\frac{3}{2}\\)"}
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
      {"order":1,"type":"TEXT","content":"함수 \\(f(x)=x^2-2x+3\\) 에 대하여 \\(\\lim_{h \\to 0} \\frac{f(3+h)-f(3)}{h}\\) 의 값은?"}
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
      {"order":1,"type":"TEXT","content":"수열 \\(\\{a_n\\}\\) 에 대하여 \\(\\sum_{k=1}^{10}(2a_k+3)=60\\) 일 때, \\(\\sum_{k=1}^{10} a_k\\) 의 값은?"}
    ]'::jsonb,
    3,
    '[
      {"order":1,"content":"10"},
      {"order":2,"content":"15"},
      {"order":3,"content":"20"},
      {"order":4,"content":"25"},
      {"order":5,"content":"30"}
    ]'::jsonb,
    2,
    'ALG',
    3,
    'MCQ'
)
     , (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"실수 전체의 집합에서 연속인 함수 \\(f(x)\\) 가"},
      {"order":2,"type":"TEXT","content":"\\[ \\lim_{x \\to 1} f(x)=4-f(1) \\]"},
      {"order":3,"type":"TEXT","content":"을 만족시킬 때, \\(f(1)\\) 의 값은?"}
    ]'::jsonb,
    4,
    '[
      {"order":1,"content":"1"},
      {"order":2,"content":"2"},
      {"order":3,"content":"3"},
      {"order":4,"content":"4"},
      {"order":5,"content":"5"}
    ]'::jsonb,
    2,
    'ALG',
    3,
    'MCQ'
)
     , (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"다항함수 \\(f(x)\\) 에 대하여 함수 \\(g(x)\\) 를"},
      {"order":2,"type":"TEXT","content":"\\[ g(x)=(x^3+1)f(x) \\]"},
      {"order":3,"type":"TEXT","content":"라 하자. \\(f(1)=2,\\ f^{\\prime}(1)=3\\) 일 때, \\(g^{\\prime}(1)\\) 의 값은?"}
    ]'::jsonb,
    5,
    '[
      {"order":1,"content":"12"},
      {"order":2,"content":"14"},
      {"order":3,"content":"16"},
      {"order":4,"content":"18"},
      {"order":5,"content":"20"}
    ]'::jsonb,
    1,
    'ALG',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"\\(\\cos\\theta<0\\) 이고 \\(\\sin(-\\theta)=\\frac{1}{7}\\cos\\theta\\) 일 때, \\(\\sin\\theta\\) 의 값은?"}
    ]'::jsonb,
    6,
    '[
      {"order":1,"content":"\\(-\\frac{3\\sqrt{2}}{10}\\)"},
      {"order":2,"content":"\\(-\\frac{\\sqrt{2}}{10}\\)"},
      {"order":3,"content":"0"},
      {"order":4,"content":"\\(\\frac{\\sqrt{2}}{10}\\)"},
      {"order":5,"content":"\\(\\frac{3\\sqrt{2}}{10}\\)"}
    ]'::jsonb,
    4,
    'ALG',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"상수 \\(a(a>2)\\) 에 대하여 함수 \\(y=\\log_2(x-a)\\) 의 그래프의 점근선이 두 곡선 \\(y=\\log_2\\frac{x}{4},\\ y=\\log_{\\frac{1}{2}}x\\) 와 만나는 점을 각각 \\(\\mathrm{A},\\ \\mathrm{B}\\) 라 하자. \\(\\overline{\\mathrm{AB}}=4\\) 일 때, \\(a\\) 의 값은?"}
    ]'::jsonb,
    7,
    '[
      {"order":1,"content":"4"},
      {"order":2,"content":"6"},
      {"order":3,"content":"8"},
      {"order":4,"content":"10"},
      {"order":5,"content":"12"}
    ]'::jsonb,
    3,
    'ALG',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"두 곡선 \\(y=2x^2-1,\\ y=x^3-x^2+k\\) 가 만나는 점의 개수가 2 가 되도록 하는 양수 \\(k\\) 의 값은?"}
    ]'::jsonb,
    8,
    '[
      {"order":1,"content":"1"},
      {"order":2,"content":"2"},
      {"order":3,"content":"3"},
      {"order":4,"content":"4"},
      {"order":5,"content":"5"}
    ]'::jsonb,
    3,
    'ALG',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"수열 \\(\\{a_n\\}\\) 이 모든 자연수 \\(n\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ \\sum_{k=1}^{n} \\frac{1}{(2k-1)a_k}=n^2+2n \\]"},
      {"order":3,"type":"TEXT","content":"을 만족시킬 때, \\(\\sum_{n=1}^{10} a_n\\) 의 값은?"}
    ]'::jsonb,
    9,
    '[
      {"order":1,"content":"\\(\\frac{10}{21}\\)"},
      {"order":2,"content":"\\(\\frac{4}{7}\\)"},
      {"order":3,"content":"\\(\\frac{2}{3}\\)"},
      {"order":4,"content":"\\(\\frac{16}{21}\\)"},
      {"order":5,"content":"\\(\\frac{6}{7}\\)"}
    ]'::jsonb,
    1,
    'ALG',
    4,
    'MCQ'
)
     ,(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"양수 \\(k\\) 에 대하여 함수 \\(f(x)\\) 는"},
      {"order":2,"type":"TEXT","content":"\\[ f(x)=kx(x-2)(x-3) \\]"},
      {"order":3,"type":"TEXT","content":"이다. 곡선 \\(y=f(x)\\) 와 \\(x\\) 축이 원점 O 와 두 점 \\(\\mathrm{P},\\ \\mathrm{Q}(\\overline{\\mathrm{OP}}<\\overline{\\mathrm{OQ}})\\) 에서 만난다. 곡선 \\(y=f(x)\\) 와 선분 OP 로 둘러싸인 영역을 A, 곡선 \\(y=f(x)\\) 와 선분 PQ 로 둘러싸인 영역을 B 라 하자."},
      {"order":4,"type":"TEXT","content":"\\[ (A\\text{ 의 넓이})-(B\\text{ 의 넓이})=3 \\]"},
      {"order":5,"type":"TEXT","content":"일 때, \\(k\\) 의 값은?"},
      {"order":6, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/alg10p.JPG"}
    ]'::jsonb,
    10,
    '[
      {"order":1,"content":"\\(\\frac{7}{6}\\)"},
      {"order":2,"content":"\\(\\frac{4}{3}\\)"},
      {"order":3,"content":"\\(\\frac{3}{2}\\)"},
      {"order":4,"content":"\\(\\frac{5}{3}\\)"},
      {"order":5,"content":"\\(\\frac{11}{6}\\)"}
    ]'::jsonb,
    2,
    'ALG',
    4,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"그림과 같이 실수 \\(t(0<t<1)\\) 에 대하여 곡선 \\(y=x^2\\) 위의 점 중에서 직선 \\(y=2tx-1\\) 과의 거리가 최소인 점을 P 라 하고, 직선 OP 가 직선 \\(y=2tx-1\\) 과 만나는 점을 Q 라 할 때, \\(\\lim_{t \\to 1-} \\frac{\\overline{\\mathrm{PQ}}}{1-t}\\) 의 값은? (단, O 는 원점이다.)"},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/alg11p.JPG"}
    ]'::jsonb,
    11,
    '[
      {"order":1,"content":"\\(\\sqrt{6}\\)"},
      {"order":2,"content":"\\(\\sqrt{7}\\)"},
      {"order":3,"content":"\\(2\\sqrt{2}\\)"},
      {"order":4,"content":"3"},
      {"order":5,"content":"\\(\\sqrt{10}\\)"}
    ]'::jsonb,
    3,
    'ALG',
    4,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"\\(a_2=-4\\) 이고 공차가 0 이 아닌 등차수열 \\(\\{a_n\\}\\) 에 대하여 수열 \\(\\{b_n\\}\\) 을 \\(b_n=a_n+a_{n+1}(n \\geq 1)\\) 이라 하고, 두 집합 A, B 를"},
      {"order":2,"type":"TEXT","content":"\\[ A=\\{a_1,a_2,a_3,a_4,a_5\\}, \\quad B=\\{b_1,b_2,b_3,b_4,b_5\\} \\]"},
      {"order":3,"type":"TEXT","content":"라 하자. \\(n(A \\cap B)=3\\) 이 되도록 하는 모든 수열 \\(\\{a_n\\}\\) 에 대하여 \\(a_{20}\\) 의 값의 합은?"}
    ]'::jsonb,
    12,
    '[
      {"order":1,"content":"30"},
      {"order":2,"content":"34"},
      {"order":3,"content":"38"},
      {"order":4,"content":"42"},
      {"order":5,"content":"46"}
    ]'::jsonb,
    5,
    'ALG',
    4,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"그림과 같이"},
      {"order":2,"type":"TEXT","content":"\\[ \\overline{\\mathrm{BC}}=3,\\ \\overline{\\mathrm{CD}}=2,\\ \\cos(\\angle \\mathrm{BCD})=-\\frac{1}{3},\\ \\angle \\mathrm{DAB}>\\frac{\\pi}{2} \\]"},
      {"order":3,"type":"TEXT","content":"인 사각형 ABCD 에서 두 삼각형 ABC 와 ACD 는 모두 예각삼각형이다. 선분 AC 를 \\(1:2\\) 로 내분하는 점 E 에 대하여 선분 AE 를 지름으로 하는 원이 두 선분 \\(\\mathrm{AB},\\ \\mathrm{AD}\\) 와 만나는 점 중 A 가 아닌 점을 각각 \\(\\mathrm{P}_1,\\ \\mathrm{P}_2\\) 라 하고, 선분 CE 를 지름으로 하는 원이 두 선분 \\(\\mathrm{BC},\\ \\mathrm{CD}\\) 와 만나는 점 중 C 가 아닌 점을 각각 \\(\\mathrm{Q}_1,\\ \\mathrm{Q}_2\\) 라 하자."},
      {"order":4,"type":"TEXT","content":"\\(\\overline{\\mathrm{P}_1\\mathrm{P}_2}:\\overline{\\mathrm{Q}_1\\mathrm{Q}_2}=3:5\\sqrt{2}\\) 이고 삼각형 ABD 의 넓이가 2 일 때, \\(\\overline{\\mathrm{AB}}+\\overline{\\mathrm{AD}}\\) 의 값은? (단, \\(\\overline{\\mathrm{AB}}>\\overline{\\mathrm{AD}}\\))"},
      {"order":5, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/alg13p.JPG"}
    ]'::jsonb,
    13,
    '[
      {"order":1,"content":"\\(\\sqrt{21}\\)"},
      {"order":2,"content":"\\(\\sqrt{22}\\)"},
      {"order":3,"content":"\\(\\sqrt{23}\\)"},
      {"order":4,"content":"\\(2\\sqrt{6}\\)"},
      {"order":5,"content":"5"}
    ]'::jsonb,
    1,
    'ALG',
    4,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"실수 \\(a(a \\geq 0)\\) 에 대하여 수직선 위를 움직이는 점 P 의 시각 \\(t(t \\geq 0)\\) 에서의 속도 \\(v(t)\\) 를"},
      {"order":2,"type":"TEXT","content":"\\[ v(t)=-t(t-1)(t-a)(t-2a) \\]"},
      {"order":3,"type":"TEXT","content":"라 하자. 점 P 가 시각 \\(t=0\\) 일 때 출발한 후 운동 방향을 한 번만 바꾸도록 하는 \\(a\\) 에 대하여, 시각 \\(t=0\\) 에서 \\(t=2\\) 까지 점 P 의 위치의 변화량의 최댓값은?"}
    ]'::jsonb,
    14,
    '[
      {"order":1,"content":"\\(\\frac{1}{5}\\)"},
      {"order":2,"content":"\\(\\frac{7}{30}\\)"},
      {"order":3,"content":"\\(\\frac{4}{15}\\)"},
      {"order":4,"content":"\\(\\frac{3}{10}\\)"},
      {"order":5,"content":"\\(\\frac{1}{3}\\)"}
    ]'::jsonb,
    3,
    'ALG',
    4,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"자연수 \\(k\\) 에 대하여 다음 조건을 만족시키는 수열 \\(\\{a_n\\}\\) 이 있다."},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/alg13p.JPG"},
      {"order":3,"type":"TEXT","content":"\\(a_3\\times a_4\\times a_5\\times a_6<0\\) 이 되도록 하는 모든 \\(k\\) 의 값의 합은?"}
    ]'::jsonb,
    15,
    '[
      {"order":1,"content":"10"},
      {"order":2,"content":"14"},
      {"order":3,"content":"18"},
      {"order":4,"content":"22"},
      {"order":5,"content":"26"}
    ]'::jsonb,
    2,
    'ALG',
    4,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"부등식 \\(2^{x-6} \\leq \\left(\\frac{1}{4}\\right)^x\\) 을 만족시키는 모든 자연수 \\(x\\) 의 값의 합을 구하시오."}
    ]'::jsonb,
    16,
    null,
    3,
    'ALG',
    3,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"함수 \\(f(x)\\) 에 대하여 \\(f^{\\prime}(x)=8x^3-1\\) 이고 \\(f(0)=3\\) 일 때, \\(f(2)\\) 의 값을 구하시오."}
    ]'::jsonb,
    17,
    null,
    33,
    'ALG',
    3,
    'FRQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"두 상수 \\(a,b\\) 에 대하여 삼차함수 \\(f(x)=ax^3+bx+a\\) 는 \\(x=1\\) 에서 극소이다. 함수 \\(f(x)\\) 의 극솟값이 -2 일 때, 함수 \\(f(x)\\) 의 극댓값을 구하시오."}
    ]'::jsonb,
    18,
    null,
    6,
    'ALG',
    3,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"두 자연수 \\(a,b\\) 에 대하여 함수"},
      {"order":2,"type":"TEXT","content":"\\[ f(x)=a\\sin bx+8-a \\]"},
      {"order":3,"type":"TEXT","content":"가 다음 조건을 만족시킬 때, \\(a+b\\) 의 값을 구하시오."},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/alg19p.JPG"}
    ]'::jsonb,
    19,
    null,
    8,
    'ALG',
    3,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"최고차항의 계수가 1 인 이차함수 \\(f(x)\\) 에 대하여 함수"},
      {"order":2,"type":"TEXT","content":"\\[ g(x)=\\int_{0}^{x} f(t)\\,dt \\]"},
      {"order":3,"type":"TEXT","content":"가 다음 조건을 만족시킬 때, \\(f(9)\\) 의 값을 구하시오."},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/alg20p.JPG"}
    ]'::jsonb,
    20,
    null,
    39,
    'ALG',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"실수 \\(t\\) 에 대하여 두 곡선 \\(y=t-\\log_2 x\\) 와 \\(y=2^{x-t}\\) 이 만나는 점의 \\(x\\) 좌표를 \\(f(t)\\) 라 하자."},
      {"order":2,"type":"TEXT","content":"<보기>의 각 명제에 대하여 다음 규칙에 따라 \\(A, B, C\\) 의 값을 정할 때, \\(A+B+C\\) 의 값을 구하시오. (단, \\(A+B+C \\neq 0\\))"},
      {"order":3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/alg21p.JPG"}
    ]'::jsonb,
    21,
    null,
    110,
    'ALG',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"정수 \\(a(a \\neq 0)\\) 에 대하여 함수 \\(f(x)\\) 를"},
      {"order":2,"type":"TEXT","content":"\\[ f(x)=x^3-2ax^2 \\]"},
      {"order":3,"type":"TEXT","content":"이라 하자. 다음 조건을 만족시키는 모든 정수 \\(k\\) 의 값의 곱이 -12 가 되도록 하는 \\(a\\) 에 대하여 \\(f^{\\prime}(10)\\) 의 값을 구하시오."},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/alg22p.JPG"}
    ]'::jsonb,
    22,
    null,
    380,
    'ALG',
    4,
    'FRQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"5 개의 문자 \\(a, a, b, c, d\\) 를 모두 일렬로 나열하는 경우의 수는?"}
    ]'::jsonb,
    23,
    '[
      {"order":1,"content":"50"},
      {"order":2,"content":"55"},
      {"order":3,"content":"60"},
      {"order":4,"content":"65"},
      {"order":5,"content":"70"}
    ]'::jsonb,
    3,
    'PROB',
    2,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"두 사건 \\(A, B\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ \\mathrm{P}(A \\cap B^C)=\\frac{1}{9}, \\quad \\mathrm{P}(B^C)=\\frac{7}{18} \\]"},
      {"order":3,"type":"TEXT","content":"일 때, \\(\\mathrm{P}(A \\cup B)\\) 의 값은? (단, \\(B^C\\) 은 \\(B\\) 의 여사건이다.)"}
    ]'::jsonb,
    24,
    '[
      {"order":1,"content":"\\(\\frac{5}{9}\\)"},
      {"order":2,"content":"\\(\\frac{11}{18}\\)"},
      {"order":3,"content":"\\(\\frac{2}{3}\\)"},
      {"order":4,"content":"\\(\\frac{13}{18}\\)"},
      {"order":5,"content":"\\(\\frac{7}{9}\\)"}
    ]'::jsonb,
    4,
    'PROB',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"흰색 손수건 4장, 검은색 손수건 5장이 들어 있는 상자가 있다. 이 상자에서 임의로 4 장의 손수건을 동시에 꺼낼 때, 꺼낸 4 장의 손수건 중에서 흰색 손수건이 2 장 이상일 확률은?"}
    ]'::jsonb,
    25,
    '[
      {"order":1,"content":"\\(\\frac{1}{2}\\)"},
      {"order":2,"content":"\\(\\frac{4}{7}\\)"},
      {"order":3,"content":"\\(\\frac{9}{14}\\)"},
      {"order":4,"content":"\\(\\frac{5}{7}\\)"},
      {"order":5,"content":"\\(\\frac{11}{14}\\)"}
    ]'::jsonb,
    3,
    'PROB',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"다항식 \\((x-1)^6(2x+1)^7\\) 의 전개식에서 \\(x^2\\) 의 계수는?"}
    ]'::jsonb,
    26,
    '[
      {"order":1,"content":"15"},
      {"order":2,"content":"20"},
      {"order":3,"content":"25"},
      {"order":4,"content":"30"},
      {"order":5,"content":"35"}
    ]'::jsonb,
    1,
    'CALC',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"한 개의 주사위를 두 번 던질 때 나오는 눈의 수를 차례로 \\(a, b\\) 라 하자. \\(a \\times b\\) 가 4 의 배수일 때, \\(a+b \\leq 7\\) 일 확률은?"}
    ]'::jsonb,
    27,
    '[
      {"order":1,"content":"\\(\\frac{2}{5}\\)"},
      {"order":2,"content":"\\(\\frac{7}{15}\\)"},
      {"order":3,"content":"\\(\\frac{8}{15}\\)"},
      {"order":4,"content":"\\(\\frac{3}{5}\\)"},
      {"order":5,"content":"\\(\\frac{2}{3}\\)"}
    ]'::jsonb,
    2,
    'PROB',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"집합 \\(X=\\{1,2,3,4,5\\}\\) 에 대하여 다음 조건을 만족시키는 함수 \\(f: X \\rightarrow X\\) 의 개수는?"},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/prob28p.JPG"}
    ]'::jsonb,
    28,
    '[
      {"order":1,"content":"128"},
      {"order":2,"content":"132"},
      {"order":3,"content":"136"},
      {"order":4,"content":"140"},
      {"order":5,"content":"144"}
    ]'::jsonb,
    5,
    'PROB',
    4,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"그림과 같이 2 장의 검은색 카드와 1 부터 8 까지의 자연수가 하나씩 적혀 있는 8 장의 흰색 카드가 있다. 이 카드를 모두 한 번씩 사용하여 왼쪽에서 오른쪽으로 일렬로 배열할 때, 다음 조건을 만족시키는 경우의 수를 구하시오. (단, 검은색 카드는 서로 구별하지 않는다.)"},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/prob29p.JPG"}
    ]'::jsonb,
    29,
    null,
    25,
    'PROB',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"주머니에 숫자 \\(1,2,3,4\\) 가 하나씩 적혀 있는 흰 공 4 개와 숫자 \\(4,5,6,7\\) 이 하나씩 적혀 있는 검은 공 4 개가 들어 있다. 이 주머니를 사용하여 다음 규칙에 따라 점수를 얻는 시행을 한다."},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/prob30p.JPG"},
      {"order":3,"type":"TEXT","content":"이 시행을 한 번 하여 얻은 점수가 24 이하의 짝수일 확률이 \\(\\frac{q}{p}\\) 일 때, \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
    ]'::jsonb,
    30,
    null,
    51,
    'PROB',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"\\(\\lim_{n \\to \\infty}\\left(\\sqrt{n^2+9n}-\\sqrt{n^2+4n}\\right)\\) 의 값은?"}
    ]'::jsonb,
    23,
    '[
      {"order":1,"content":"\\(\\frac{1}{2}\\)"},
      {"order":2,"content":"1"},
      {"order":3,"content":"\\(\\frac{3}{2}\\)"},
      {"order":4,"content":"2"},
      {"order":5,"content":"\\(\\frac{5}{2}\\)"}
    ]'::jsonb,
    5,
    'CALC',
    2,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"매개변수 \\(t\\) 로 나타내어진 곡선"},
      {"order":2,"type":"TEXT","content":"\\[ x=\\frac{5t}{t^2+1}, \\quad y=3\\ln(t^2+1) \\]"},
      {"order":3,"type":"TEXT","content":"에서 \\(t=2\\) 일 때, \\(\\frac{dy}{dx}\\) 의 값은?"}
    ]'::jsonb,
    24,
    '[
      {"order":1,"content":"-1"},
      {"order":2,"content":"-2"},
      {"order":3,"content":"-3"},
      {"order":4,"content":"-4"},
      {"order":5,"content":"-5"}
    ]'::jsonb,
    4,
    'CALC',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"\\(\\lim_{x \\to 0} \\frac{2^{ax+b}-8}{2^{bx}-1}=16\\) 일 때, \\(a+b\\) 의 값은? (단, \\(a\\) 와 \\(b\\) 는 0 이 아닌 상수이다.)"}
    ]'::jsonb,
    25,
    '[
      {"order":1,"content":"9"},
      {"order":2,"content":"10"},
      {"order":3,"content":"11"},
      {"order":4,"content":"12"},
      {"order":5,"content":"13"}
    ]'::jsonb,
    1,
    'CALC',
    3,
    'MCQ'
)
     ,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"\\(x\\) 에 대한 방정식 \\(x^2-5x+2\\ln x=t\\) 의 서로 다른 실근의 개수가 2 가 되도록 하는 모든 실수 \\(t\\) 의 값의 합은?"}
    ]'::jsonb,
    26,
    '[
      {"order":1,"content":"\\(-\\frac{17}{2}\\)"},
      {"order":2,"content":"\\(-\\frac{33}{4}\\)"},
      {"order":3,"content":"-8"},
      {"order":4,"content":"\\(-\\frac{31}{4}\\)"},
      {"order":5,"content":"\\(-\\frac{15}{2}\\)"}
    ]'::jsonb,
    2,
    'CALC',
    3,
    'MCQ'
    )
        ,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"실수 \\(t(0<t<\\pi)\\) 에 대하여 곡선 \\(y=\\sin x\\) 위의 점 \\(\\mathrm{P}(t,\\sin t)\\) 에서의 접선과 점 P 를 지나고 기울기가 -1 인 직선이 이루는 예각의 크기를 \\(\\theta\\) 라 할 때,"},
      {"order":2,"type":"TEXT","content":"\\(\\lim_{t \\to \\pi-} \\frac{\\tan\\theta}{(\\pi-t)^2}\\) 의 값은?"}
    ]'::jsonb,
    27,
    '[
      {"order":1,"content":"\\(\\frac{1}{16}\\)"},
      {"order":2,"content":"\\(\\frac{1}{8}\\)"},
      {"order":3,"content":"\\(\\frac{1}{4}\\)"},
      {"order":4,"content":"\\(\\frac{1}{2}\\)"},
      {"order":5,"content":"1"}
    ]'::jsonb,
    3,
    'CALC',
    3,
    'MCQ'
    )
        ,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"두 상수 \\(a(a>0),\\ b\\) 에 대하여 실수 전체의 집합에서 연속인 함수 \\(f(x)\\) 가 다음 조건을 만족시킬 때, \\(a \\times b\\) 의 값은?"},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/calc28p.JPG"}
    ]'::jsonb,
    28,
    '[
      {"order":1,"content":"\\(-\\frac{1}{16}\\)"},
      {"order":2,"content":"\\(-\\frac{7}{64}\\)"},
      {"order":3,"content":"\\(-\\frac{5}{32}\\)"},
      {"order":4,"content":"\\(-\\frac{13}{64}\\)"},
      {"order":5,"content":"\\(-\\frac{1}{4}\\)"}
    ]'::jsonb,
    2,
    'CALC',
    4,
    'MCQ'
    )
        ,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"세 실수 \\(a,b,k\\) 에 대하여 두 점 \\(\\mathrm{A}(a,a+k),\\ \\mathrm{B}(b,b+k)\\) 가 곡선 \\(C: x^2-2xy+2y^2=15\\) 위에 있다."},
      {"order":2,"type":"TEXT","content":"곡선 \\(C\\) 위의 점 A 에서의 접선과 곡선 \\(C\\) 위의 점 B 에서의 접선이 서로 수직일 때, \\(k^2\\) 의 값을 구하시오. (단, \\(a+2k \\neq 0,\\ b+2k \\neq 0\\))"}
    ]'::jsonb,
    29,
    null,
    5,
    'CALC',
    4,
    'FRQ'
    )

        ,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"수열 \\(\\{a_n\\}\\) 은 등비수열이고, 수열 \\(\\{b_n\\}\\) 을 모든 자연수 \\(n\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ b_n=\\begin{cases} -1 & (a_n \\leq -1) \\\\ a_n & (a_n>-1) \\end{cases} \\]"},
      {"order":3,"type":"TEXT","content":"이라 할 때, 수열 \\(\\{b_n\\}\\) 은 다음 조건을 만족시킨다."},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/calc30p.JPG"},
      {"order":5,"type":"TEXT","content":"\\(b_3=-1\\) 일 때, \\(\\sum_{n=1}^{\\infty} |a_n|\\) 의 값을 구하시오."}
    ]'::jsonb,
    30,
    null,
    24,
    'CALC',
    4,
    'FRQ'
    )
        ,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"포물선 \\(y^2=-12(x-1)\\) 의 준선을 \\(x=k\\) 라 할 때, 상수 \\(k\\) 의 값은?"}
    ]'::jsonb,
    23,
    '[
      {"order":1,"content":"4"},
      {"order":2,"content":"7"},
      {"order":3,"content":"10"},
      {"order":4,"content":"13"},
      {"order":5,"content":"16"}
    ]'::jsonb,
    1,
    'GEO',
    2,
    'MCQ'
    )
        ,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"한 직선 위에 있지 않은 서로 다른 세 점 \\(\\mathrm{A},\\ \\mathrm{B},\\ \\mathrm{C}\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ 2\\overrightarrow{\\mathrm{AB}}+p\\overrightarrow{\\mathrm{BC}}=q\\overrightarrow{\\mathrm{CA}} \\]"},
      {"order":3,"type":"TEXT","content":"일 때, \\(p-q\\) 의 값은? (단, \\(p\\) 와 \\(q\\) 는 실수이다.)"}
    ]'::jsonb,
    24,
    '[
      {"order":1,"content":"1"},
      {"order":2,"content":"2"},
      {"order":3,"content":"3"},
      {"order":4,"content":"4"},
      {"order":5,"content":"5"}
    ]'::jsonb,
    4,
    'GEO',
    3,
    'MCQ'
    )
        ,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"그림과 같이 한 변의 길이가 1 인 정사각형 ABCD 에서"},
      {"order":2,"type":"TEXT","content":"\\[ (\\overrightarrow{\\mathrm{AB}}+k\\overrightarrow{\\mathrm{BC}})\\cdot(\\overrightarrow{\\mathrm{AC}}+3k\\overrightarrow{\\mathrm{CD}})=0 \\]"},
      {"order":3,"type":"TEXT","content":"일 때, 실수 \\(k\\) 의 값은?"},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/geo25p.JPG"}
    ]'::jsonb,
    25,
    '[
      {"order":1,"content":"1"},
      {"order":2,"content":"\\(\\frac{1}{2}\\)"},
      {"order":3,"content":"\\(\\frac{1}{3}\\)"},
      {"order":4,"content":"\\(\\frac{1}{4}\\)"},
      {"order":5,"content":"\\(\\frac{1}{5}\\)"}
    ]'::jsonb,
    2,
    'GEO',
    3,
    'MCQ'
    )
        , (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"두 초점이 \\(\\mathrm{F}(12,0),\\ \\mathrm{F}^{\\prime}(-4,0)\\) 이고, 장축의 길이가 24 인 타원 \\(C\\) 가 있다."},
      {"order":2,"type":"TEXT","content":"\\(\\overline{\\mathrm{F}^{\\prime}\\mathrm{F}}=\\overline{\\mathrm{F}^{\\prime}\\mathrm{P}}\\) 인 타원 \\(C\\) 위의 점 P 에 대하여 선분 \\(\\mathrm{F}^{\\prime}\\mathrm{P}\\) 의 중점을 Q 라 하자."},
      {"order":3,"type":"TEXT","content":"한 초점이 \\(\\mathrm{F}^{\\prime}\\) 인 타원 \\(\\frac{x^2}{a^2}+\\frac{y^2}{b^2}=1\\) 이 점 Q 를 지날 때, \\(\\overline{\\mathrm{PF}}+a^2+b^2\\) 의 값은? (단, \\(a\\) 와 \\(b\\) 는 양수이다.)"}
    ]'::jsonb,
    26,
    '[
      {"order":1,"content":"46"},
      {"order":2,"content":"52"},
      {"order":3,"content":"58"},
      {"order":4,"content":"64"},
      {"order":5,"content":"70"}
    ]'::jsonb,
    4,
    'GEO',
    3,
    'MCQ'
    )
        ,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"포물선 \\((y-2)^2=8(x+2)\\) 위의 점 P 와 점 \\(\\mathrm{A}(0,2)\\) 에 대하여 \\(\\overline{\\mathrm{OP}}+\\overline{\\mathrm{PA}}\\) 의 값이 최소가 되도록 하는 점 P 를 \\(\\mathrm{P}_0\\) 이라 하자."},
      {"order":2,"type":"TEXT","content":"\\(\\overline{\\mathrm{OQ}}+\\overline{\\mathrm{QA}}=\\overline{\\mathrm{OP}_0}+\\overline{\\mathrm{P}_0\\mathrm{A}}\\) 를 만족시키는 점 Q 에 대하여 점 Q 의 \\(y\\) 좌표의 최댓값과 최솟값을 각각 \\(M,m\\) 이라 할 때, \\(M^2+m^2\\) 의 값은? (단, O 는 원점이다.)"}
    ]'::jsonb,
    27,
    '[
      {"order":1,"content":"8"},
      {"order":2,"content":"9"},
      {"order":3,"content":"10"},
      {"order":4,"content":"11"},
      {"order":5,"content":"12"}
    ]'::jsonb,
    3,
    'GEO',
    3,
    'MCQ'
    )
,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"좌표평면의 네 점 \\(\\mathrm{A}(2,6),\\ \\mathrm{B}(6,2),\\ \\mathrm{C}(4,4),\\ \\mathrm{D}(8,6)\\) 에 대하여 다음 조건을 만족시키는 모든 점 X 의 집합을 \\(S\\) 라 하자."},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/geo28p.JPG"},
      {"order":3,"type":"TEXT","content":"집합 \\(S\\) 에 속하는 점 중에서 \\(y\\) 좌표가 최대인 점을 Q, \\(y\\) 좌표가 최소인 점을 R 이라 할 때, \\(\\overrightarrow{\\mathrm{OQ}}\\cdot\\overrightarrow{\\mathrm{OR}}\\) 의 값은? (단, O 는 원점이다.)"}
    ]'::jsonb,
    28,
    '[
      {"order":1,"content":"25"},
      {"order":2,"content":"26"},
      {"order":3,"content":"27"},
      {"order":4,"content":"28"},
      {"order":5,"content":"29"}
    ]'::jsonb,
    5,
    'GEO',
    4,
    'MCQ'
    )
,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"두 점 \\(\\mathrm{F}(c,0),\\ \\mathrm{F}^{\\prime}(-c,0)(c>0)\\) 을 초점으로 하는 두 쌍곡선"},
      {"order":2,"type":"TEXT","content":"\\[ C_1: x^2-\\frac{y^2}{24}=1, \\quad C_2: \\frac{x^2}{4}-\\frac{y^2}{21}=1 \\]"},
      {"order":3,"type":"TEXT","content":"이 있다. 쌍곡선 \\(C_1\\) 위에 있는 제 2 사분면 위의 점 P 에 대하여 선분 \\(\\mathrm{PF}^{\\prime}\\) 이 쌍곡선 \\(C_2\\) 와 만나는 점을 Q 라 하자."},
      {"order":4,"type":"TEXT","content":"\\(\\overline{\\mathrm{PQ}}+\\overline{\\mathrm{QF}},\\ 2\\overline{\\mathrm{PF}^{\\prime}},\\ \\overline{\\mathrm{PF}}+\\overline{\\mathrm{PF}^{\\prime}}\\) 이 이 순서대로 등차수열을 이룰 때, 직선 PQ 의 기울기는 \\(m\\) 이다. \\(60m\\) 의 값을 구하시오."},
      {"order":5, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/geo29p.JPG"}
    ]'::jsonb,
    29,
    null,
    80,
    'GEO',
    4,
    'FRQ'
    )
,
    (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"직선 \\(2x+y=0\\) 위를 움직이는 점 P 와 타원 \\(2x^2+y^2=3\\) 위를 움직이는 점 Q 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ \\overrightarrow{\\mathrm{OX}}=\\overrightarrow{\\mathrm{OP}}+\\overrightarrow{\\mathrm{OQ}} \\]"},
      {"order":3,"type":"TEXT","content":"를 만족시키고, \\(x\\) 좌표와 \\(y\\) 좌표가 모두 0 이상인 모든 점 X 가 나타내는 영역의 넓이는 \\(\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, O 는 원점이고, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"},
      {"order":5, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/geo29p.JPG"}
    ]'::jsonb,
    30,
    null,
    13,
    'GEO',
    4,
    'FRQ'
    )

;




