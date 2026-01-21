insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2024
       , 'CSAT'
       , '수학능력시험'
       , 30
       , 6000);


WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2024 AND exam_type = 'CSAT' LIMIT 1
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
           {"order":1,"type":"TEXT","content":"\\(\\sqrt[3]{24} \\times 3^{\\frac{2}{3}}\\) 의 값은?"}
         ]'::jsonb,
         1,
         '[
           {"order":1,"content":"6"},
           {"order":2,"content":"7"},
           {"order":3,"content":"8"},
           {"order":4,"content":"9"},
           {"order":5,"content":"10"}
         ]'::jsonb,
         1,
         'ALG',
         2,
         'MCQ'
            )
       ,
        (
         ((SELECT id FROM exam)),
         '[
           {"order":1,"type":"TEXT","content":"함수 \\(f(x)=2x^3-5x^2+3\\) 에 대하여 \\(\\lim_{h \\to 0} \\frac{f(2+h)-f(2)}{h}\\) 의 값은?"}
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
            {"order":1,"type":"TEXT","content":"\\(\\frac{3}{2}\\pi<\\theta<2\\pi\\) 인 \\(\\theta\\) 에 대하여 \\(\\sin(-\\theta)=\\frac{1}{3}\\) 일 때, \\(\\tan\\theta\\) 의 값은?"}
          ]'::jsonb,
          3,
          '[
            {"order":1,"content":"\\(-\\frac{\\sqrt{2}}{2}\\)"},
            {"order":2,"content":"\\(-\\frac{\\sqrt{2}}{4}\\)"},
            {"order":3,"content":"\\(-\\frac{1}{4}\\)"},
            {"order":4,"content":"\\(\\frac{1}{4}\\)"},
            {"order":5,"content":"\\(\\frac{\\sqrt{2}}{4}\\)"}
          ]'::jsonb,
          2,
          'ALG',
          3,
          'MCQ'
            )
       , (
          ((SELECT id FROM exam)),
          '[
            {"order":1,"type":"TEXT","content":"함수"},
            {"order":2,"type":"TEXT","content":"\\[ f(x)= \\begin{cases} 3x-a & (x<2) \\\\ x^2+a & (x \\geq 2) \\end{cases} \\]"},
            {"order":3,"type":"TEXT","content":"가 실수 전체의 집합에서 연속일 때, 상수 \\(a\\) 의 값은?"}
          ]'::jsonb,
          4,
          '[
            {"order":1,"content":"1"},
            {"order":2,"content":"2"},
            {"order":3,"content":"3"},
            {"order":4,"content":"4"},
            {"order":5,"content":"5"}
          ]'::jsonb,
          1,
          'ALG',
          3,
          'MCQ'
            )
       , (
          ((SELECT id FROM exam)),
          '[
            {"order":1,"type":"TEXT","content":"다항함수 \\(f(x)\\) 가"},
            {"order":2,"type":"TEXT","content":"\\[ f^{\\prime}(x)=3x(x-2), \\quad f(1)=6 \\]"},
            {"order":3,"type":"TEXT","content":"을 만족시킬 때, \\(f(2)\\) 의 값은?"}
          ]'::jsonb,
          5,
          '[
            {"order":1,"content":"1"},
            {"order":2,"content":"2"},
            {"order":3,"content":"3"},
            {"order":4,"content":"4"},
            {"order":5,"content":"5"}
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
            {"order":1,"type":"TEXT","content":"등비수열 \\(\\{a_n\\}\\) 의 첫째항부터 제 \\(n\\) 항까지의 합을 \\(S_n\\) 이라 하자."},
            {"order":2,"type":"TEXT","content":"\\[ S_4-S_2=3a_4, \\quad a_5=\\frac{3}{4} \\]"},
            {"order":3,"type":"TEXT","content":"일 때, \\(a_1+a_2\\) 의 값은?"}
          ]'::jsonb,
          6,
          '[
            {"order":1,"content":"27"},
            {"order":2,"content":"24"},
            {"order":3,"content":"21"},
            {"order":4,"content":"18"},
            {"order":5,"content":"15"}
          ]'::jsonb,
          4,
          'ALG',
          3,
          'MCQ'
            )
       ,(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"함수 \\(f(x)=\\frac{1}{3}x^3-2x^2-12x+4\\) 가 \\(x=\\alpha\\) 에서 극대이고 \\(x=\\beta\\) 에서 극소일 때, \\(\\beta-\\alpha\\) 의 값은? (단, \\(\\alpha\\) 와 \\(\\beta\\) 는 상수이다.)"}
    ]'::jsonb,
    7,
    '[
      {"order":1,"content":"-4"},
      {"order":2,"content":"-1"},
      {"order":3,"content":"2"},
      {"order":4,"content":"5"},
      {"order":5,"content":"8"}
    ]'::jsonb,
    5,
    'ALG',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"삼차함수 \\(f(x)\\) 가 모든 실수 \\(x\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ xf(x)-f(x)=3x^4-3x \\]"},
      {"order":3,"type":"TEXT","content":"를 만족시킬 때, \\(\\int_{-2}^{2} f(x)\\,dx\\) 의 값은?"}
    ]'::jsonb,
    8,
    '[
      {"order":1,"content":"12"},
      {"order":2,"content":"16"},
      {"order":3,"content":"20"},
      {"order":4,"content":"24"},
      {"order":5,"content":"28"}
    ]'::jsonb,
    2,
    'ALG',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"수직선 위의 두 점 \\(\\mathrm{P}(\\log_5 3),\\ \\mathrm{Q}(\\log_5 12)\\) 에 대하여 선분 PQ 를 \\(m:(1-m)\\) 으로 내분하는 점의 좌표가 1 일 때, \\(4^m\\) 의 값은? (단, \\(m\\) 은 \\(0<m<1\\) 인 상수이다.)"}
    ]'::jsonb,
    9,
    '[
      {"order":1,"content":"\\(\\frac{7}{6}\\)"},
      {"order":2,"content":"\\(\\frac{4}{3}\\)"},
      {"order":3,"content":"\\(\\frac{3}{2}\\)"},
      {"order":4,"content":"\\(\\frac{5}{3}\\)"},
      {"order":5,"content":"\\(\\frac{11}{6}\\)"}
    ]'::jsonb,
    4,
    'ALG',
    4,
    'MCQ'
)
     ,(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"시각 \\(t=0\\) 일 때 동시에 원점을 출발하여 수직선 위를 움직이는 두 점 \\(\\mathrm{P},\\ \\mathrm{Q}\\) 의 시각 \\(t(t \\geq 0)\\) 에서의 속도가 각각"},
      {"order":2,"type":"TEXT","content":"\\[ v_1(t)=t^2-6t+5, \\quad v_2(t)=2t-7 \\]"},
      {"order":3,"type":"TEXT","content":"이다. 시각 \\(t\\) 에서의 두 점 \\(\\mathrm{P},\\ \\mathrm{Q}\\) 사이의 거리를 \\(f(t)\\) 라 할 때, 함수 \\(f(t)\\) 는 구간 \\([0,a]\\) 에서 증가하고, 구간 \\([a,b]\\) 에서 감소하고, 구간 \\([b,\\infty)\\) 에서 증가한다. 시각 \\(t=a\\) 에서 \\(t=b\\) 까지 점 Q 가 움직인 거리는? (단, \\(0<a<b\\))"}
    ]'::jsonb,
    10,
    '[
      {"order":1,"content":"\\(\\frac{15}{2}\\)"},
      {"order":2,"content":"\\(\\frac{17}{2}\\)"},
      {"order":3,"content":"\\(\\frac{19}{2}\\)"},
      {"order":4,"content":"\\(\\frac{21}{2}\\)"},
      {"order":5,"content":"\\(\\frac{23}{2}\\)"}
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
      {"order":1,"type":"TEXT","content":"공차가 0 이 아닌 등차수열 \\(\\{a_n\\}\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ |a_6|=a_8, \\quad \\sum_{k=1}^{5} \\frac{1}{a_k a_{k+1}}=\\frac{5}{96} \\]"},
      {"order":3,"type":"TEXT","content":"일 때, \\(\\sum_{k=1}^{15} a_k\\) 의 값은?"}
    ]'::jsonb,
    11,
    '[
      {"order":1,"content":"60"},
      {"order":2,"content":"65"},
      {"order":3,"content":"70"},
      {"order":4,"content":"75"},
      {"order":5,"content":"80"}
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
      {"order":1,"type":"TEXT","content":"함수 \\(f(x)=\\frac{1}{9}x(x-6)(x-9)\\) 와 실수 \\(t(0<t<6)\\) 에 대하여 함수 \\(g(x)\\) 는"},
      {"order":2,"type":"TEXT","content":"\\[ g(x)= \\begin{cases} f(x) & (x<t) \\\\ -(x-t)+f(t) & (x \\geq t) \\end{cases} \\]"},
      {"order":3,"type":"TEXT","content":"이다. 함수 \\(y=g(x)\\) 의 그래프와 \\(x\\) 축으로 둘러싸인 영역의 넓이의 최댓값은?"}
    ]'::jsonb,
    12,
    '[
      {"order":1,"content":"\\(\\frac{125}{4}\\)"},
      {"order":2,"content":"\\(\\frac{127}{4}\\)"},
      {"order":3,"content":"\\(\\frac{129}{4}\\)"},
      {"order":4,"content":"\\(\\frac{131}{4}\\)"},
      {"order":5,"content":"\\(\\frac{133}{4}\\)"}
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
      {"order":1,"type":"TEXT","content":"그림과 같이"},
      {"order":2,"type":"TEXT","content":"\\[ \\overline{\\mathrm{AB}}=3, \\quad \\overline{\\mathrm{BC}}=\\sqrt{13}, \\quad \\overline{\\mathrm{AD}} \\times \\overline{\\mathrm{CD}}=9, \\quad \\angle \\mathrm{BAC}=\\frac{\\pi}{3} \\]"},
      {"order":3,"type":"TEXT","content":"인 사각형 ABCD 가 있다. 삼각형 ABC 의 넓이를 \\(S_1\\), 삼각형 ACD 의 넓이를 \\(S_2\\) 라 하고, 삼각형 ACD 의 외접원의 반지름의 길이를 \\(R\\) 이라 하자."},
      {"order":4,"type":"TEXT","content":"\\(S_2=\\frac{5}{6}S_1\\) 일 때, \\(\\frac{R}{\\sin(\\angle \\mathrm{ADC})}\\) 의 값은?"},
      {"order":5, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/csat/alg13p.JPG"}
    ]'::jsonb,
    13,
    '[
      {"order":1,"content":"\\(\\frac{54}{25}\\)"},
      {"order":2,"content":"\\(\\frac{117}{50}\\)"},
      {"order":3,"content":"\\(\\frac{63}{25}\\)"},
      {"order":4,"content":"\\(\\frac{27}{10}\\)"},
      {"order":5,"content":"\\(\\frac{72}{25}\\)"}
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
      {"order":1,"type":"TEXT","content":"두 자연수 \\(a,b\\) 에 대하여 함수 \\(f(x)\\) 는"},
      {"order":2,"type":"TEXT","content":"\\[ f(x)= \\begin{cases} 2x^3-6x+1 & (x \\leq 2) \\\\ a(x-2)(x-b)+9 & (x>2) \\end{cases} \\]"},
      {"order":3,"type":"TEXT","content":"이다. 실수 \\(t\\) 에 대하여 함수 \\(y=f(x)\\) 의 그래프와 직선 \\(y=t\\) 가 만나는 점의 개수를 \\(g(t)\\) 라 하자."},
      {"order":4,"type":"TEXT","content":"\\[ g(k)+\\lim_{t \\to k-} g(t)+\\lim_{t \\to k+} g(t)=9 \\]"},
      {"order":5,"type":"TEXT","content":"를 만족시키는 실수 \\(k\\) 의 개수가 1 이 되도록 하는 두 자연수 \\(a,b\\) 의 순서쌍 \\((a,b)\\) 에 대하여 \\(a+b\\) 의 최댓값은?"}
    ]'::jsonb,
    14,
    '[
      {"order":1,"content":"51"},
      {"order":2,"content":"52"},
      {"order":3,"content":"53"},
      {"order":4,"content":"54"},
      {"order":5,"content":"55"}
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
      {"order":1,"type":"TEXT","content":"첫째항이 자연수인 수열 \\(\\{a_n\\}\\) 이 모든 자연수 \\(n\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ a_{n+1}= \\begin{cases} 2^{a_n} & (a_n \\text{ 이 홀수인 경우}) \\\\ \\frac{1}{2}a_n & (a_n \\text{ 이 짝수인 경우}) \\end{cases} \\]"},
      {"order":3,"type":"TEXT","content":"를 만족시킬 때, \\(a_6+a_7=3\\) 이 되도록 하는 모든 \\(a_1\\) 의 값의 합은?"}
    ]'::jsonb,
    15,
    '[
      {"order":1,"content":"139"},
      {"order":2,"content":"146"},
      {"order":3,"content":"153"},
      {"order":4,"content":"160"},
      {"order":5,"content":"167"}
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
      {"order":1,"type":"TEXT","content":"방정식 \\(3^{x-8}=\\left(\\frac{1}{27}\\right)^x\\) 을 만족시키는 실수 \\(x\\) 의 값을 구하시오."}
    ]'::jsonb,
    16,
    null,
    2,
    'ALG',
    3,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"함수 \\(f(x)=(x+1)(x^2+3)\\) 에 대하여 \\(f^{\\prime}(1)\\) 의 값을 구하시오."}
    ]'::jsonb,
    17,
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
      {"order":1,"type":"TEXT","content":"두 수열 \\(\\{a_n\\},\\ \\{b_n\\}\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ \\sum_{k=1}^{10} a_k=\\sum_{k=1}^{10}(2b_k-1), \\quad \\sum_{k=1}^{10}(3a_k+b_k)=33 \\]"},
      {"order":3,"type":"TEXT","content":"일 때, \\(\\sum_{k=1}^{10} b_k\\) 의 값을 구하시오."}
    ]'::jsonb,
    18,
    null,
    9,
    'ALG',
    3,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"함수 \\(f(x)=\\sin\\frac{\\pi}{4}x\\) 라 할 때, \\(0<x<16\\) 에서 부등식"},
      {"order":2,"type":"TEXT","content":"\\[ f(2+x)f(2-x)<\\frac{1}{4} \\]"},
      {"order":3,"type":"TEXT","content":"을 만족시키는 모든 자연수 \\(x\\) 의 값의 합을 구하시오."}
    ]'::jsonb,
    19,
    null,
    32,
    'ALG',
    3,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"\\(a>\\sqrt{2}\\) 인 실수 \\(a\\) 에 대하여 함수 \\(f(x)\\) 를"},
      {"order":2,"type":"TEXT","content":"\\[ f(x)=-x^3+ax^2+2x \\]"},
      {"order":3,"type":"TEXT","content":"라 하자. 곡선 \\(y=f(x)\\) 위의 점 \\(\\mathrm{O}(0,0)\\) 에서의 접선이 곡선 \\(y=f(x)\\) 와 만나는 점 중 O 가 아닌 점을 A 라 하고, 곡선 \\(y=f(x)\\) 위의 점 A 에서의 접선이 \\(x\\) 축과 만나는 점을 B 라 하자."},
      {"order":4,"type":"TEXT","content":"점 A 가 선분 OB 를 지름으로 하는 원 위의 점일 때, \\(\\overline{\\mathrm{OA}} \\times \\overline{\\mathrm{AB}}\\) 의 값을 구하시오."}
    ]'::jsonb,
    20,
    null,
    25,
    'ALG',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"양수 \\(a\\) 에 대하여 \\(x \\geq -1\\) 에서 정의된 함수 \\(f(x)\\) 는"},
      {"order":2,"type":"TEXT","content":"\\[ f(x)= \\begin{cases} -x^2+6x & (-1 \\leq x<6) \\\\ a\\log_4(x-5) & (x \\geq 6) \\end{cases} \\]"},
      {"order":3,"type":"TEXT","content":"이다. \\(t \\geq 0\\) 인 실수 \\(t\\) 에 대하여 닫힌구간 \\([t-1,t+1]\\) 에서의 \\(f(x)\\) 의 최댓값을 \\(g(t)\\) 라 하자."},
      {"order":4,"type":"TEXT","content":"구간 \\([0,\\infty)\\) 에서 함수 \\(g(t)\\) 의 최솟값이 5 가 되도록 하는 양수 \\(a\\) 의 최솟값을 구하시오."}
    ]'::jsonb,
    21,
    null,
    10,
    'ALG',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"최고차항의 계수가 1 인 삼차함수 \\(f(x)\\) 가 다음 조건을 만족시킨다."},
      {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/csat/alg22p.JPG"},
      {"order":3,"type":"TEXT","content":"\\(f^{\\prime}\\left(-\\frac{1}{4}\\right)=-\\frac{1}{4},\\ f^{\\prime}\\left(\\frac{1}{4}\\right)<0\\) 일 때, \\(f(8)\\) 의 값을 구하시오."}
    ]'::jsonb,
    22,
    null,
    483,
    'ALG',
    4,
    'FRQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"5 개의 문자 \\(x, x, y, y, z\\) 를 모두 일렬로 나열하는 경우의 수는?"}
    ]'::jsonb,
    23,
    '[
      {"order":1,"content":"10"},
      {"order":2,"content":"20"},
      {"order":3,"content":"30"},
      {"order":4,"content":"40"},
      {"order":5,"content":"50"}
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
      {"order":1,"type":"TEXT","content":"두 사건 \\(A, B\\) 는 서로 독립이고"},
      {"order":2,"type":"TEXT","content":"\\[ \\mathrm{P}(A \\cap B)=\\frac{1}{4}, \\quad \\mathrm{P}(A^C)=2\\mathrm{P}(A) \\]"},
      {"order":3,"type":"TEXT","content":"일 때, \\(\\mathrm{P}(B)\\) 의 값은? (단, \\(A^C\\) 은 \\(A\\) 의 여사건이다.)"}
    ]'::jsonb,
    24,
    '[
      {"order":1,"content":"\\(\\frac{3}{8}\\)"},
      {"order":2,"content":"\\(\\frac{1}{2}\\)"},
      {"order":3,"content":"\\(\\frac{5}{8}\\)"},
      {"order":4,"content":"\\(\\frac{3}{4}\\)"},
      {"order":5,"content":"\\(\\frac{7}{8}\\)"}
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
      {"order":1,"type":"TEXT","content":"숫자 \\(1,2,3,4,5,6\\) 이 하나씩 적혀 있는 6 장의 카드가 있다. 이 6 장의 카드를 모두 한 번씩 사용하여 일렬로 임의로 나열할 때, 양 끝에 놓인 카드에 적힌 두 수의 합이 10 이하가 되도록 카드가 놓일 확률은?"},
      {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/csat/prob25p.JPG"}
    ]'::jsonb,
    25,
    '[
      {"order":1,"content":"\\(\\frac{8}{15}\\)"},
      {"order":2,"content":"\\(\\frac{19}{30}\\)"},
      {"order":3,"content":"\\(\\frac{11}{15}\\)"},
      {"order":4,"content":"\\(\\frac{5}{6}\\)"},
      {"order":5,"content":"\\(\\frac{14}{15}\\)"}
    ]'::jsonb,
    5,
    'PROB',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"4 개의 동전을 동시에 던져서 앞면이 나오는 동전의 개수를 확률변수 \\(X\\) 라 하고, 이산확률변수 \\(Y\\) 를"},
      {"order":2,"type":"TEXT","content":"\\[ Y=\\begin{cases} X & (X \\text{ 가 } 0 \\text{ 또는 } 1 \\text{ 의 값을 가지는 경우}) \\\\ 2 & (X \\text{ 가 } 2 \\text{ 이상의 값을 가지는 경우}) \\end{cases} \\]"},
      {"order":3,"type":"TEXT","content":"라 하자. \\(\\mathrm{E}(Y)\\) 의 값은?"}
    ]'::jsonb,
    26,
    '[
      {"order":1,"content":"\\(\\frac{25}{16}\\)"},
      {"order":2,"content":"\\(\\frac{13}{8}\\)"},
      {"order":3,"content":"\\(\\frac{27}{16}\\)"},
      {"order":4,"content":"\\(\\frac{7}{4}\\)"},
      {"order":5,"content":"\\(\\frac{29}{16}\\)"}
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
      {"order":1,"type":"TEXT","content":"정규분포 \\(\\mathrm{N}(m,5^2)\\) 을 따르는 모집단에서 크기가 49 인 표본을 임의추출하여 얻은 표본평균이 \\\\bar{x}\\\\ 일 때, 모평균 \\(m\\) 에 대한 신뢰도 95\\% 의 신뢰구간이 \\(a \\leq m \\leq \\frac{6}{5}a\\) 이다."},
      {"order":2,"type":"TEXT","content":"\\(\\bar{x}\\) 의 값은? (단, \\(Z\\) 가 표준정규분포를 따르는 확률변수일 때, \\(\\mathrm{P}(|Z| \\leq 1.96)=0.95\\) 로 계산한다.)"}
    ]'::jsonb,
    27,
    '[
      {"order":1,"content":"15.2"},
      {"order":2,"content":"15.4"},
      {"order":3,"content":"15.6"},
      {"order":4,"content":"15.8"},
      {"order":5,"content":"16.0"}
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
      {"order":1,"type":"TEXT","content":"하나의 주머니와 두 상자 \\(\\mathrm{A},\\ \\mathrm{B}\\) 가 있다. 주머니에는 숫자 \\(1,2,3,4\\) 가 하나씩 적힌 4 장의 카드가 들어 있고, 상자 A 에는 흰 공과 검은 공이 각각 8 개 이상 들어 있고, 상자 B 는 비어 있다. 이 주머니와 두 상자 \\(\\mathrm{A},\\ \\mathrm{B}\\) 를 사용하여 다음 시행을 한다."},
      {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/csat/prob28p.JPG"},
      {"order":3,"type":"TEXT","content":"이 시행을 4 번 반복한 후 상자 B 에 들어 있는 공의 개수가 8 일 때, 상자 B 에 들어 있는 검은 공의 개수가 2 일 확률은?"},
      {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/csat/prob28p2.JPG"}
    ]'::jsonb,
    28,
    '[
      {"order":1,"content":"\\(\\frac{3}{70}\\)"},
      {"order":2,"content":"\\(\\frac{2}{35}\\)"},
      {"order":3,"content":"\\(\\frac{1}{14}\\)"},
      {"order":4,"content":"\\(\\frac{3}{35}\\)"},
      {"order":5,"content":"\\(\\frac{1}{10}\\)"}
    ]'::jsonb,
    4,
    'PROB',
    4,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"다음 조건을 만족시키는 6 이하의 자연수 \\(a,b,c,d\\) 의 모든 순서쌍 \\((a,b,c,d)\\) 의 개수를 구하시오."},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/csat/prob29p.JPG"}
    ]'::jsonb,
    29,
    null,
    196,
    'PROB',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"양수 \\(t\\) 에 대하여 확률변수 \\(X\\) 가 정규분포 \\(\\mathrm{N}(1,t^2)\\) 을 따른다."},
      {"order":2,"type":"TEXT","content":"\\[ \\mathrm{P}(X \\leq 5t) \\geq \\frac{1}{2} \\]"},
      {"order":3,"type":"TEXT","content":"이 되도록 하는 모든 양수 \\(t\\) 에 대하여 \\(\\mathrm{P}(t^2-t+1 \\leq X \\leq t^2+t+1)\\) 의 최댓값을 오른쪽 표준정규분포표를 이용하여 구한 값을 \\(k\\) 라 하자."},
      {"order":4,"type":"TEXT","content":"\\(1000 \\times k\\) 의 값을 구하시오."},
      {"order":5, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/csat/prob30p.JPG"}
    ]'::jsonb,
    30,
    null,
    673,
    'PROB',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"\\(\\lim_{x \\to 0} \\frac{\\ln(1+3x)}{\\ln(1+5x)}\\) 의 값은?"}
    ]'::jsonb,
    23,
    '[
      {"order":1,"content":"\\(\\frac{1}{5}\\)"},
      {"order":2,"content":"\\(\\frac{2}{5}\\)"},
      {"order":3,"content":"\\(\\frac{3}{5}\\)"},
      {"order":4,"content":"\\(\\frac{4}{5}\\)"},
      {"order":5,"content":"1"}
    ]'::jsonb,
    3,
    'CALC',
    2,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"매개변수 \\(t(t>0)\\) 로 나타내어진 곡선"},
      {"order":2,"type":"TEXT","content":"\\[ x=\\ln(t^3+1), \\quad y=\\sin \\pi t \\]"},
      {"order":3,"type":"TEXT","content":"에서 \\(t=1\\) 일 때, \\(\\frac{dy}{dx}\\) 의 값은?"}
    ]'::jsonb,
    24,
    '[
      {"order":1,"content":"\\(-\\frac{1}{3}\\pi\\)"},
      {"order":2,"content":"\\(-\\frac{2}{3}\\pi\\)"},
      {"order":3,"content":"\\(-\\pi\\)"},
      {"order":4,"content":"\\(-\\frac{4}{3}\\pi\\)"},
      {"order":5,"content":"\\(-\\frac{5}{3}\\pi\\)"}
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
      {"order":1,"type":"TEXT","content":"양의 실수 전체의 집합에서 정의되고 미분가능한 두 함수 \\(f(x), g(x)\\) 가 있다. \\(g(x)\\) 는 \\(f(x)\\) 의 역함수이고, \\(g^{\\prime}(x)\\) 는 양의 실수 전체의 집합에서 연속이다."},
      {"order":2,"type":"TEXT","content":"모든 양수 \\(a\\) 에 대하여"},
      {"order":3,"type":"TEXT","content":"\\[ \\int_{1}^{a} \\frac{1}{g^{\\prime}(f(x))f(x)}\\,dx = 2\\ln a+\\ln(a+1)-\\ln 2 \\]"},
      {"order":4,"type":"TEXT","content":"이고 \\(f(1)=8\\) 일 때, \\(f(2)\\) 의 값은?"}
    ]'::jsonb,
    25,
    '[
      {"order":1,"content":"36"},
      {"order":2,"content":"40"},
      {"order":3,"content":"44"},
      {"order":4,"content":"48"},
      {"order":5,"content":"52"}
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
      {"order":1,"type":"TEXT","content":"그림과 같이 곡선 \\(y=\\sqrt{(1-2x)\\cos x}\\left(\\frac{3}{4}\\pi \\leq x \\leq \\frac{5}{4}\\pi\\right)\\) 와 \\(x\\) 축 및 두 직선 \\(x=\\frac{3}{4}\\pi,\\ x=\\frac{5}{4}\\pi\\) 로 둘러싸인 부분을 밑면으로 하는 입체도형이 있다."},
      {"order":2,"type":"TEXT","content":"이 입체도형을 \\(x\\) 축에 수직인 평면으로 자른 단면이 모두 정사각형일 때, 이 입체도형의 부피는?"},
      {"order":3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/csat/calc26p.JPG"}
    ]'::jsonb,
    26,
    '[
      {"order":1,"content":"\\(\\sqrt{2}\\pi-\\sqrt{2}\\)"},
      {"order":2,"content":"\\(\\sqrt{2}\\pi-1\\)"},
      {"order":3,"content":"\\(2\\sqrt{2}\\pi-\\sqrt{2}\\)"},
      {"order":4,"content":"\\(2\\sqrt{2}\\pi-1\\)"},
      {"order":5,"content":"\\(2\\sqrt{2}\\pi\\)"}
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
      {"order":1,"type":"TEXT","content":"실수 \\(t\\) 에 대하여 원점을 지나고 곡선 \\(y=\\frac{1}{e^x}+e^t\\) 에 접하는 직선의 기울기를 \\(f(t)\\) 라 하자."},
      {"order":2,"type":"TEXT","content":"\\(f(a)=-e\\sqrt{e}\\) 를 만족시키는 상수 \\(a\\) 에 대하여 \\(f^{\\prime}(a)\\) 의 값은?"}
    ]'::jsonb,
    27,
    '[
      {"order":1,"content":"\\(-\\frac{1}{3}e\\sqrt{e}\\)"},
      {"order":2,"content":"\\(-\\frac{1}{2}e\\sqrt{e}\\)"},
      {"order":3,"content":"\\(-\\frac{2}{3}e\\sqrt{e}\\)"},
      {"order":4,"content":"\\(-\\frac{5}{6}e\\sqrt{e}\\)"},
      {"order":5,"content":"\\(-e\\sqrt{e}\\)"}
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
      {"order":1,"type":"TEXT","content":"실수 전체의 집합에서 연속인 함수 \\(f(x)\\) 가 모든 실수 \\(x\\) 에 대하여 \\(f(x)\\geq 0\\) 이고, \\(x<0\\) 일 때 \\(f(x)=-4xe^{4x^2}\\) 이다."},
      {"order":2,"type":"TEXT","content":"모든 양수 \\(t\\) 에 대하여 \\(x\\) 에 대한 방정식 \\(f(x)=t\\) 의 서로 다른 실근의 개수는 2 이고, 이 방정식의 두 실근 중 작은 값을 \\(g(t)\\), 큰 값을 \\(h(t)\\) 라 하자."},
      {"order":3,"type":"TEXT","content":"두 함수 \\(g(t),h(t)\\) 는 모든 양수 \\(t\\) 에 대하여 \\[ 2g(t)+h(t)=k \\,(k\\text{ 는 상수}) \\] 를 만족시킨다."},
      {"order":4,"type":"TEXT","content":"\\(\\int_{0}^{7} f(x)\\,dx=e^4-1\\) 일 때, \\(\\frac{f(9)}{f(8)}\\) 의 값은?"}
    ]'::jsonb,
    28,
    '[
      {"order":1,"content":"\\(\\frac{3}{2}e^5\\)"},
      {"order":2,"content":"\\(\\frac{4}{3}e^7\\)"},
      {"order":3,"content":"\\(\\frac{5}{4}e^9\\)"},
      {"order":4,"content":"\\(\\frac{6}{5}e^{11}\\)"},
      {"order":5,"content":"\\(\\frac{7}{6}e^{13}\\)"}
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
      {"order":1,"type":"TEXT","content":"첫째항과 공비가 각각 0 이 아닌 두 등비수열 \\(\\{a_n\\},\\ \\{b_n\\}\\) 에 대하여 두 급수 \\(\\sum_{n=1}^{\\infty} a_n,\\ \\sum_{n=1}^{\\infty} b_n\\) 이 각각 수렴하고"},
      {"order":2,"type":"TEXT","content":"\\[ \\sum_{n=1}^{\\infty} a_n b_n=\\left(\\sum_{n=1}^{\\infty} a_n\\right)\\times\\left(\\sum_{n=1}^{\\infty} b_n\\right), \\quad 3\\times\\sum_{n=1}^{\\infty}|a_{2n}|=7\\times\\sum_{n=1}^{\\infty}|a_{3n}| \\]"},
      {"order":3,"type":"TEXT","content":"이 성립한다. \\(\\sum_{n=1}^{\\infty} \\frac{b_{2n-1}+b_{3n+1}}{b_n}=S\\) 일 때, \\(120S\\) 의 값을 구하시오."}
    ]'::jsonb,
    29,
    null,
    162,
    'CALC',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"실수 전체의 집합에서 미분가능한 함수 \\(f(x)\\) 의 도함수 \\(f^{\\prime}(x)\\) 가"},
      {"order":2,"type":"TEXT","content":"\\[ f^{\\prime}(x)=|\\sin x|\\cos x \\]"},
      {"order":3,"type":"TEXT","content":"이다. 양수 \\(a\\) 에 대하여 곡선 \\(y=f(x)\\) 위의 점 \\((a,f(a))\\) 에서의 접선의 방정식을 \\(y=g(x)\\) 라 하자."},
      {"order":4,"type":"TEXT","content":"함수"},
      {"order":5,"type":"TEXT","content":"\\[ h(x)=\\int_{0}^{x}\\{f(t)-g(t)\\}\\,dt \\]"},
      {"order":6,"type":"TEXT","content":"가 \\(x=a\\) 에서 극대 또는 극소가 되도록 하는 모든 양수 \\(a\\) 를 작은 수부터 크기순으로 나열할 때, \\(n\\) 번째 수를 \\(a_n\\) 이라 하자. \\(\\frac{100}{\\pi}\\times(a_6-a_2)\\) 의 값을 구하시오."}
    ]'::jsonb,
    30,
    null,
    125,
    'CALC',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"좌표공간의 두 점 \\(\\mathrm{A}(a,-2,6),\\ \\mathrm{B}(9,2,b)\\) 에 대하여 선분 AB 의 중점의 좌표가 \\((4,0,7)\\) 일 때, \\(a+b\\) 의 값은?"}
    ]'::jsonb,
    23,
    '[
      {"order":1,"content":"1"},
      {"order":2,"content":"3"},
      {"order":3,"content":"5"},
      {"order":4,"content":"7"},
      {"order":5,"content":"9"}
    ]'::jsonb,
    4,
    'GEO',
    2,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"타원 \\(\\frac{x^2}{a^2}+\\frac{y^2}{6}=1\\) 위의 점 \\((\\sqrt{3},-2)\\) 에서의 접선의 기울기는? (단, \\(a\\) 는 양수이다.)"}
    ]'::jsonb,
    24,
    '[
      {"order":1,"content":"\\(\\sqrt{3}\\)"},
      {"order":2,"content":"\\(\\frac{\\sqrt{3}}{2}\\)"},
      {"order":3,"content":"\\(\\frac{\\sqrt{3}}{3}\\)"},
      {"order":4,"content":"\\(\\frac{\\sqrt{3}}{4}\\)"},
      {"order":5,"content":"\\(\\frac{\\sqrt{3}}{5}\\)"}
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
      {"order":1,"type":"TEXT","content":"두 벡터 \\(\\vec{a},\\ \\vec{b}\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ |\\vec{a}|=\\sqrt{11}, \\quad |\\vec{b}|=3, \\quad |2\\vec{a}-\\vec{b}|=\\sqrt{17} \\]"},
      {"order":3,"type":"TEXT","content":"일 때, \\(|\\vec{a}-\\vec{b}|\\) 의 값은?"}
    ]'::jsonb,
    25,
    '[
      {"order":1,"content":"\\(\\frac{\\sqrt{2}}{2}\\)"},
      {"order":2,"content":"\\(\\sqrt{2}\\)"},
      {"order":3,"content":"\\(\\frac{3\\sqrt{2}}{2}\\)"},
      {"order":4,"content":"\\(2\\sqrt{2}\\)"},
      {"order":5,"content":"\\(\\frac{5\\sqrt{2}}{2}\\)"}
    ]'::jsonb,
    2,
    'GEO',
    3,
    'MCQ'
)
     , (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"좌표공간에 평면 \\(\\alpha\\) 가 있다. 평면 \\(\\alpha\\) 위에 있지 않은 서로 다른 두 점 \\(\\mathrm{A},\\ \\mathrm{B}\\) 의 평면 \\(\\alpha\\) 위로의 정사영을 각각 \\(\\mathrm{A}^{\\prime},\\ \\mathrm{B}^{\\prime}\\) 이라 할 때,"},
      {"order":2,"type":"TEXT","content":"\\[ \\overline{\\mathrm{AB}}=\\overline{\\mathrm{A}^{\\prime}\\mathrm{B}^{\\prime}}=6 \\]"},
      {"order":3,"type":"TEXT","content":"이다. 선분 AB 의 중점 M 의 평면 \\(\\alpha\\) 위로의 정사영을 \\(\\mathrm{M}^{\\prime}\\) 이라 할 때,"},
      {"order":4,"type":"TEXT","content":"\\[ \\overline{\\mathrm{PM}^{\\prime}} \\perp \\overline{\\mathrm{A}^{\\prime}\\mathrm{B}^{\\prime}}, \\quad \\overline{\\mathrm{PM}^{\\prime}}=6 \\]"},
      {"order":5,"type":"TEXT","content":"이 되도록 평면 \\(\\alpha\\) 위에 점 P 를 잡는다."},
      {"order":6,"type":"TEXT","content":"삼각형 \\(\\mathrm{A}^{\\prime}\\mathrm{B}^{\\prime}\\mathrm{P}\\) 의 평면 ABP 위로의 정사영의 넓이가 \\(\\frac{9}{2}\\) 일 때, 선분 PM 의 길이는?"}
    ]'::jsonb,
    26,
    '[
      {"order":1,"content":"12"},
      {"order":2,"content":"15"},
      {"order":3,"content":"18"},
      {"order":4,"content":"21"},
      {"order":5,"content":"24"}
    ]'::jsonb,
    5,
    'GEO',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"초점이 F 인 포물선 \\(y^2=8x\\) 위의 한 점 A 에서 포물선의 준선에 내린 수선의 발을 B 라 하고, 직선 BF 와 포물선이 만나는 두 점을 각각 \\(\\mathrm{C},\\ \\mathrm{D}\\) 라 하자."},
      {"order":2,"type":"TEXT","content":"\\(\\overline{\\mathrm{BC}}=\\overline{\\mathrm{CD}}\\) 일 때, 삼각형 ABD 의 넓이는? (단, \\(\\overline{\\mathrm{CF}}<\\overline{\\mathrm{DF}}\\) 이고, 점 A 는 원점이 아니다.)"}
    ]'::jsonb,
    27,
    '[
      {"order":1,"content":"\\(100\\sqrt{2}\\)"},
      {"order":2,"content":"\\(104\\sqrt{2}\\)"},
      {"order":3,"content":"\\(108\\sqrt{2}\\)"},
      {"order":4,"content":"\\(112\\sqrt{2}\\)"},
      {"order":5,"content":"\\(116\\sqrt{2}\\)"}
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
      {"order":1,"type":"TEXT","content":"그림과 같이 서로 다른 두 평면 \\(\\alpha,\\ \\beta\\) 의 교선 위에 \\(\\overline{\\mathrm{AB}}=18\\) 인 두 점 \\(\\mathrm{A},\\ \\mathrm{B}\\) 가 있다. 선분 AB 를 지름으로 하는 원 \\(C_1\\) 이 평면 \\(\\alpha\\) 위에 있고, 선분 AB 를 장축으로 하고 두 점 \\(\\mathrm{F},\\ \\mathrm{F}^{\\prime}\\) 을 초점으로 하는 타원 \\(C_2\\) 가 평면 \\(\\beta\\) 위에 있다."},
      {"order":2,"type":"TEXT","content":"원 \\(C_1\\) 위의 한 점 P 에서 평면 \\(\\beta\\) 에 내린 수선의 발을 H 라 할 때, \\(\\overline{\\mathrm{HF}^{\\prime}}<\\overline{\\mathrm{HF}}\\) 이고 \\(\\angle \\mathrm{HFF}^{\\prime}=\\frac{\\pi}{6}\\) 이다. 직선 HF 와 타원 \\(C_2\\) 가 만나는 점 중 점 H 와 가까운 점을 Q 라 하면, \\(\\overline{\\mathrm{FH}}<\\overline{\\mathrm{FQ}}\\) 이다."},
      {"order":3,"type":"TEXT","content":"점 H 를 중심으로 하고 점 Q 를 지나는 평면 \\(\\beta\\) 위의 원은 반지름의 길이가 4 이고 직선 AB 에 접한다. 두 평면 \\(\\alpha,\\ \\beta\\) 가 이루는 각의 크기를 \\(\\theta\\) 라 할 때, \\(\\cos\\theta\\) 의 값은? (단, 점 P 는 평면 \\(\\beta\\) 위에 있지 않다.)"},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/csat/geo28p.JPG"}
    ]'::jsonb,
    28,
    '[
      {"order":1,"content":"\\(\\frac{2\\sqrt{66}}{33}\\)"},
      {"order":2,"content":"\\(\\frac{4\\sqrt{69}}{69}\\)"},
      {"order":3,"content":"\\(\\frac{\\sqrt{2}}{3}\\)"},
      {"order":4,"content":"\\(\\frac{4\\sqrt{3}}{15}\\)"},
      {"order":5,"content":"\\(\\frac{2\\sqrt{78}}{39}\\)"}
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
      {"order":1,"type":"TEXT","content":"양수 \\(c\\) 에 대하여 두 점 \\(\\mathrm{F}(c,0),\\ \\mathrm{F}^{\\prime}(-c,0)\\) 을 초점으로 하고, 주축의 길이가 6 인 쌍곡선이 있다."},
      {"order":2,"type":"TEXT","content":"이 쌍곡선 위에 다음 조건을 만족시키는 서로 다른 두 점 \\(\\mathrm{P},\\ \\mathrm{Q}\\) 가 존재하도록 하는 모든 \\(c\\) 의 값의 합을 구하시오."},
      {"order":3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/csat/geo29p.JPG"}
    ]'::jsonb,
    29,
    null,
    11,
    'GEO',
    4,
    'FRQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"좌표평면에 한 변의 길이가 4 인 정삼각형 ABC 가 있다. 선분 AB 를 \\(1:3\\) 으로 내분하는 점을 D, 선분 BC 를 \\(1:3\\) 으로 내분하는 점을 E, 선분 CA 를 \\(1:3\\) 으로 내분하는 점을 F 라 하자. 네 점 \\(\\mathrm{P},\\ \\mathrm{Q},\\ \\mathrm{R},\\ \\mathrm{X}\\) 가 다음 조건을 만족시킨다."},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/csat/geo30p.JPG"},
      {"order":3,"type":"TEXT","content":"\\(|\\overrightarrow{\\mathrm{AX}}|\\) 의 값이 최대일 때, 삼각형 PQR 의 넓이를 \\(S\\) 라 하자. \\(16S^2\\) 의 값을 구하시오."}
    ]'::jsonb,
    30,
    null,
    147,
    'GEO',
    4,
    'FRQ'
)

;




