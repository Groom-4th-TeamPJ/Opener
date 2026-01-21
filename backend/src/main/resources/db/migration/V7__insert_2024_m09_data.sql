insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2024
       , 'M09'
       , '9월 모의평가'
       , 30
       , 6000);


WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2024 AND exam_type = 'M09' LIMIT 1
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
      {"order":1,"type":"TEXT","content":"\\(3^{1-\\sqrt{5}} \\times 3^{1+\\sqrt{5}}\\) 의 값은?"}
    ]'::jsonb,
    1,
    '[
      {"order":1,"content":"\\(\\frac{1}{9}\\)"},
      {"order":2,"content":"\\(\\frac{1}{3}\\)"},
      {"order":3,"content":"1"},
      {"order":4,"content":"3"},
      {"order":5,"content":"9"}
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
      {"order":1,"type":"TEXT","content":"함수 \\(f(x)=2x^2-x\\) 에 대하여 \\(\\lim_{x \\to 1} \\frac{f(x)-1}{x-1}\\) 의 값은?"}
    ]'::jsonb,
    2,
    '[
      {"order":1,"content":"1"},
      {"order":2,"content":"2"},
      {"order":3,"content":"3"},
      {"order":4,"content":"4"},
      {"order":5,"content":"5"}
    ]'::jsonb,
    3,
    'ALG',
    2,
    'MCQ'
)
     , (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"\\(\\frac{3}{2}\\pi<\\theta<2\\pi\\) 인 \\(\\theta\\) 에 대하여 \\(\\cos\\theta=\\frac{\\sqrt{6}}{3}\\) 일 때, \\(\\tan\\theta\\) 의 값은?"}
    ]'::jsonb,
    3,
    '[
      {"order":1,"content":"\\(-\\sqrt{2}\\)"},
      {"order":2,"content":"\\(-\\frac{\\sqrt{2}}{2}\\)"},
      {"order":3,"content":"0"},
      {"order":4,"content":"\\(\\frac{\\sqrt{2}}{2}\\)"},
      {"order":5,"content":"\\(\\sqrt{2}\\)"}
    ]'::jsonb,
    2,
    'ALG',
    3,
    'MCQ'
)
     , (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"함수 \\(y=f(x)\\) 의 그래프가 그림과 같다."},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/alg4p.JPG"},
      {"order":3,"type":"TEXT","content":"\\(\\lim_{x \\to -2+} f(x)+\\lim_{x \\to 1-} f(x)\\) 의 값은?"}
    ]'::jsonb,
    4,
    '[
      {"order":1,"content":"-2"},
      {"order":2,"content":"-1"},
      {"order":3,"content":"0"},
      {"order":4,"content":"1"},
      {"order":5,"content":"2"}
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
      {"order":1,"type":"TEXT","content":"모든 항이 양수인 등비수열 \\(\\{a_n\\}\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ \\frac{a_3 a_8}{a_6}=12, \\quad a_5+a_7=36 \\]"},
      {"order":3,"type":"TEXT","content":"일 때, \\(a_{11}\\) 의 값은?"}
    ]'::jsonb,
    5,
    '[
      {"order":1,"content":"72"},
      {"order":2,"content":"78"},
      {"order":3,"content":"84"},
      {"order":4,"content":"90"},
      {"order":5,"content":"96"}
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
      {"order":1,"type":"TEXT","content":"함수 \\(f(x)=x^3+ax^2+bx+1\\) 은 \\(x=-1\\) 에서 극대이고, \\(x=3\\) 에서 극소이다. 함수 \\(f(x)\\) 의 극댓값은? (단, \\(a,b\\) 는 상수이다.)"}
    ]'::jsonb,
    6,
    '[
      {"order":1,"content":"0"},
      {"order":2,"content":"3"},
      {"order":3,"content":"6"},
      {"order":4,"content":"9"},
      {"order":5,"content":"12"}
    ]'::jsonb,
    3,
    'ALG',
    3,
    'MCQ'
)
     ,(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"두 실수 \\(a,b\\) 가"},
      {"order":2,"type":"TEXT","content":"\\[ 3a+2b=\\log_3 32, \\quad ab=\\log_9 2 \\]"},
      {"order":3,"type":"TEXT","content":"를 만족시킬 때, \\(\\frac{1}{3a}+\\frac{1}{2b}\\) 의 값은?"}
    ]'::jsonb,
    7,
    '[
      {"order":1,"content":"\\(\\frac{5}{12}\\)"},
      {"order":2,"content":"\\(\\frac{5}{6}\\)"},
      {"order":3,"content":"\\(\\frac{5}{4}\\)"},
      {"order":4,"content":"\\(\\frac{5}{3}\\)"},
      {"order":5,"content":"\\(\\frac{25}{12}\\)"}
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
      {"order":1,"type":"TEXT","content":"다항함수 \\(f(x)\\) 가"},
      {"order":2,"type":"TEXT","content":"\\[ f^{\\prime}(x)=6x^2-2f(1)x, \\quad f(0)=4 \\]"},
      {"order":3,"type":"TEXT","content":"를 만족시킬 때, \\(f(2)\\) 의 값은?"}
    ]'::jsonb,
    8,
    '[
      {"order":1,"content":"5"},
      {"order":2,"content":"6"},
      {"order":3,"content":"7"},
      {"order":4,"content":"8"},
      {"order":5,"content":"9"}
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
      {"order":1,"type":"TEXT","content":"\\(0 \\leq x \\leq 2\\pi\\) 일 때, 부등식"},
      {"order":2,"type":"TEXT","content":"\\[ \\cos x \\leq \\sin \\frac{\\pi}{7} \\]"},
      {"order":3,"type":"TEXT","content":"를 만족시키는 모든 \\(x\\) 의 값의 범위는 \\(\\alpha \\leq x \\leq \\beta\\) 이다. \\(\\beta-\\alpha\\) 의 값은?"}
    ]'::jsonb,
    9,
    '[
      {"order":1,"content":"\\(\\frac{8}{7}\\pi\\)"},
      {"order":2,"content":"\\(\\frac{17}{14}\\pi\\)"},
      {"order":3,"content":"\\(\\frac{9}{7}\\pi\\)"},
      {"order":4,"content":"\\(\\frac{19}{14}\\pi\\)"},
      {"order":5,"content":"\\(\\frac{10}{7}\\pi\\)"}
    ]'::jsonb,
    3,
    'ALG',
    4,
    'MCQ'
)
     ,(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"최고차항의 계수가 1 인 삼차함수 \\(f(x)\\) 에 대하여 곡선 \\(y=f(x)\\) 위의 점 \\((-2,f(-2))\\) 에서의 접선과 곡선 \\(y=f(x)\\) 위의 점 \\((2,3)\\) 에서의 접선이 점 \\((1,3)\\) 에서 만날 때, \\(f(0)\\) 의 값은?"}
    ]'::jsonb,
    10,
    '[
      {"order":1,"content":"31"},
      {"order":2,"content":"33"},
      {"order":3,"content":"35"},
      {"order":4,"content":"37"},
      {"order":5,"content":"39"}
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
      {"order":1,"type":"TEXT","content":"두 점 P 와 Q 는 시각 \\(t=0\\) 일 때 각각 점 \\(\\mathrm{A}(1)\\) 과 점 \\(\\mathrm{B}(8)\\) 에서 출발하여 수직선 위를 움직인다."},
      {"order":2,"type":"TEXT","content":"두 점 \\(\\mathrm{P},\\ \\mathrm{Q}\\) 의 시각 \\(t(t \\geq 0)\\) 에서의 속도는 각각"},
      {"order":3,"type":"TEXT","content":"\\[ v_1(t)=3t^2+4t-7, \\quad v_2(t)=2t+4 \\]"},
      {"order":4,"type":"TEXT","content":"이다. 출발한 시각부터 두 점 \\(\\mathrm{P},\\ \\mathrm{Q}\\) 사이의 거리가 처음으로 4 가 될 때까지 점 P 가 움직인 거리는?"}
    ]'::jsonb,
    11,
    '[
      {"order":1,"content":"10"},
      {"order":2,"content":"14"},
      {"order":3,"content":"19"},
      {"order":4,"content":"25"},
      {"order":5,"content":"32"}
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
      {"order":1,"type":"TEXT","content":"첫째항이 자연수인 수열 \\(\\{a_n\\}\\) 이 모든 자연수 \\(n\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ a_{n+1}=\\begin{cases} a_n+1 & (a_n \\text{ 이 홀수인 경우}) \\\\ \\frac{1}{2}a_n & (a_n \\text{ 이 짝수인 경우}) \\end{cases} \\]"},
      {"order":3,"type":"TEXT","content":"를 만족시킬 때, \\(a_2+a_4=40\\) 이 되도록 하는 모든 \\(a_1\\) 의 값의 합은?"}
    ]'::jsonb,
    12,
    '[
      {"order":1,"content":"172"},
      {"order":2,"content":"175"},
      {"order":3,"content":"178"},
      {"order":4,"content":"181"},
      {"order":5,"content":"184"}
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
      {"order":1,"type":"TEXT","content":"두 실수 \\(a,b\\) 에 대하여 함수"},
      {"order":2,"type":"TEXT","content":"\\[ f(x)=\\begin{cases} -\\frac{1}{3}x^3-ax^2-bx & (x<0) \\\\ \\frac{1}{3}x^3+ax^2-bx & (x \\ge 0) \\end{cases} \\]"},
      {"order":3,"type":"TEXT","content":"이 구간 \\(( -\\infty,-1 ]\\) 에서 감소하고 구간 \\([ -1,\\infty )\\) 에서 증가할 때, \\(a+b\\) 의 최댓값을 \\(M\\), 최솟값을 \\(m\\) 이라 하자. \\(M-m\\) 의 값은?"}
    ]'::jsonb,
    13,
    '[
      {"order":1,"content":"\\(\\frac{3}{2}+3\\sqrt{2}\\)"},
      {"order":2,"content":"\\(3+3\\sqrt{2}\\)"},
      {"order":3,"content":"\\(\\frac{9}{2}+3\\sqrt{2}\\)"},
      {"order":4,"content":"\\(6+3\\sqrt{2}\\)"},
      {"order":5,"content":"\\(\\frac{15}{2}+3\\sqrt{2}\\)"}
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
      {"order":1,"type":"TEXT","content":"두 자연수 \\(a,b\\) 에 대하여 함수"},
      {"order":2,"type":"TEXT","content":"\\[ f(x)=\\begin{cases} 2^{x+a}+b & (x \\le -8) \\\\ -3^{x-3}+8 & (x>-8) \\end{cases} \\]"},
      {"order":3,"type":"TEXT","content":"이 다음 조건을 만족시킬 때, \\(a+b\\) 의 값은?"},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/alg14p.JPG"}
    ]'::jsonb,
    14,
    '[
      {"order":1,"content":"11"},
      {"order":2,"content":"13"},
      {"order":3,"content":"15"},
      {"order":4,"content":"17"},
      {"order":5,"content":"19"}
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
      {"order":1,"type":"TEXT","content":"최고차항의 계수가 1 인 삼차함수 \\(f(x)\\) 에 대하여 함수 \\(g(x)\\) 를"},
      {"order":2,"type":"TEXT","content":"\\[ g(x)=\\begin{cases} \\frac{f(x+3)\\{f(x)+1\\}}{f(x)} & (f(x) \\neq 0) \\\\ 3 & (f(x)=0) \\end{cases} \\]"},
      {"order":3,"type":"TEXT","content":"이라 하자. \\(\\lim_{x \\rightarrow 3} g(x)=g(3)-1\\) 일 때, \\(g(5)\\) 의 값은?"}
    ]'::jsonb,
    15,
    '[
      {"order":1,"content":"14"},
      {"order":2,"content":"16"},
      {"order":3,"content":"18"},
      {"order":4,"content":"20"},
      {"order":5,"content":"22"}
    ]'::jsonb,
    4,
    'ALG',
    4,
    'MCQ'
)

     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"방정식 \\(\\log_2(x-1)=\\log_4(13+2x)\\) 를 만족시키는 실수 \\(x\\) 의 값을 구하시오."}
    ]'::jsonb,
    16,
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
      {"order":1,"type":"TEXT","content":"두 수열 \\(\\{a_n\\},\\{b_n\\}\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ \\sum_{k=1}^{10}(2a_k-b_k)=34, \\quad \\sum_{k=1}^{10} a_k=10 \\]"},
      {"order":3,"type":"TEXT","content":"일 때, \\(\\sum_{k=1}^{10}(a_k-b_k)\\) 의 값을 구하시오."}
    ]'::jsonb,
    17,
    null,
    24,
    'ALG',
    3,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"함수 \\(f(x)=(x^2+1)(x^2+ax+3)\\) 에 대하여 \\(f^{\\prime}(1)=32\\) 일 때, 상수 \\(a\\) 의 값을 구하시오."}
    ]'::jsonb,
    18,
    null,
    5,
    'ALG',
    3,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"두 곡선 \\(y=3x^3-7x^2\\) 과 \\(y=-x^2\\) 으로 둘러싸인 부분의 넓이를 구하시오."}
    ]'::jsonb,
    19,
    null,
    4,
    'ALG',
    3,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"그림과 같이"},
      {"order":2,"type":"TEXT","content":"\\(\\overline{AB}=2, \\overline{AD}=1, \\angle DAB=\\frac{2}{3}\\pi, \\angle BCD=\\frac{3}{4}\\pi\\)"},
      {"order":3,"type":"TEXT","content":"인 사각형 \\(ABCD\\) 가 있다. 삼각형 \\(BCD\\) 의 외접원의 반지름의 길이를 \\(R_1\\), 삼각형 \\(ABD\\) 의 외접원의 반지름의 길이를 \\(R_2\\) 라 하자."},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/alg20p.JPG"},
      {"order":5,"type":"TEXT","content":"다음은 \\(R_1 \\times R_2\\) 의 값을 구하는 과정이다."},
      {"order":5, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/alg20p2.JPG"},
      {"order":6,"type":"TEXT","content":"위의 (가), (나), (다)에 알맞은 수를 각각 \\(p,q,r\\) 이라 할 때, \\(9\\times(p\\times q\\times r)^2\\) 의 값을 구하시오."}
    ]'::jsonb,
    20,
    null,
    98,
    'ALG',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"모든 항이 자연수인 등차수열 \\(\\{a_n\\}\\) 의 첫째항부터 제 \\(n\\) 항까지의 합을 \\(S_n\\) 이라 하자."},
      {"order":2,"type":"TEXT","content":"\\(a_7\\) 이 13 의 배수이고 \\(\\sum_{k=1}^7 S_k=644\\) 일 때, \\(a_2\\) 의 값을 구하시오."}
    ]'::jsonb,
    21,
    null,
    19,
    'ALG',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"두 다항함수 \\(f(x), g(x)\\) 에 대하여 \\(f(x)\\) 의 한 부정적분을 \\(F(x)\\) 라 하고 \\(g(x)\\) 의 한 부정적분을 \\(G(x)\\) 라 할 때, 이 함수들은 모든 실수 \\(x\\) 에 대하여 다음 조건을 만족시킨다."},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/alg22p.JPG"},
      {"order":3,"type":"TEXT","content":"\\(\\int_1^3 g(x)\\,dx\\) 의 값을 구하시오."}
    ]'::jsonb,
    22,
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
      {"order":1,"type":"TEXT","content":"확률변수 \\(X\\) 가 이항분포 \\(\\mathrm{B}\\left(30, \\frac{1}{5}\\right)\\) 을 따를 때, \\(\\mathrm{E}(X)\\) 의 값은?"}
    ]'::jsonb,
    23,
    '[
      {"order":1,"content":"6"},
      {"order":2,"content":"7"},
      {"order":3,"content":"8"},
      {"order":4,"content":"9"},
      {"order":5,"content":"10"}
    ]'::jsonb,
    1,
    'PROB',
    2,
    'MCQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"그림과 같이 직사각형 모양으로 연결된 도로망이 있다."},
      {"order":2,"type":"TEXT","content":"이 도로망을 따라 A 지점에서 출발하여 P 지점을 거쳐 B 지점까지 최단 거리로 가는 경우의 수는?"},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/prob24p.JPG"}
    ]'::jsonb,
    24,
    '[
      {"order":1,"content":"6"},
      {"order":2,"content":"7"},
      {"order":3,"content":"8"},
      {"order":4,"content":"9"},
      {"order":5,"content":"10"}
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
      {"order":1,"type":"TEXT","content":"두 사건 \\(A,B\\) 에 대하여 \\(A\\) 와 \\(B^C\\) 은 서로 배반사건이고"},
      {"order":2,"type":"TEXT","content":"\\[ \\mathrm{P}(A \\cap B)=\\frac{1}{5}, \\quad \\mathrm{P}(A)+\\mathrm{P}(B)=\\frac{7}{10} \\]"},
      {"order":3,"type":"TEXT","content":"일 때, \\(\\mathrm{P}(A^C \\cap B)\\) 의 값은?"}
    ]'::jsonb,
    25,
    '[
      {"order":1,"content":"\\(\\frac{1}{10}\\)"},
      {"order":2,"content":"\\(\\frac{1}{5}\\)"},
      {"order":3,"content":"\\(\\frac{3}{10}\\)"},
      {"order":4,"content":"\\(\\frac{2}{5}\\)"},
      {"order":5,"content":"\\(\\frac{1}{2}\\)"}
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
      {"order":1,"type":"TEXT","content":"어느 고등학교의 수학 시험에 응시한 수험생의 시험 점수는 평균이 68점, 표준편차가 10점인 정규분포를 따른다고 한다."},
      {"order":2,"type":"TEXT","content":"이 수학 시험에 응시한 수험생 중 임의로 선택한 수험생 한 명의 시험 점수가 55점 이상이고 78점 이하일 확률을 오른쪽 표준정규분포표를 이용하여 구한 것은?"},
      {"order":3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/prob26p.JPG"}
    ]'::jsonb,
    26,
    '[
      {"order":1,"content":"0.7262"},
      {"order":2,"content":"0.7445"},
      {"order":3,"content":"0.7492"},
      {"order":4,"content":"0.7675"},
      {"order":5,"content":"0.7881"}
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
      {"order":1,"type":"TEXT","content":"두 집합 \\(X=\\{1,2,3,4\\},\\; Y=\\{1,2,3,4,5,6,7\\}\\) 에 대하여 \\(X\\) 에서 \\(Y\\) 로의 모든 일대일함수 \\(f\\) 중에서 임의로 하나를 선택할 때, 이 함수가 다음 조건을 만족시킬 확률은?"},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/prob27p.JPG"}
    ]'::jsonb,
    27,
    '[
      {"order":1,"content":"\\(\\frac{1}{14}\\)"},
      {"order":2,"content":"\\(\\frac{3}{35}\\)"},
      {"order":3,"content":"\\(\\frac{1}{10}\\)"},
      {"order":4,"content":"\\(\\frac{4}{35}\\)"},
      {"order":5,"content":"\\(\\frac{9}{70}\\)"}
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
      {"order":1,"type":"TEXT","content":"주머니 A 에는 숫자 \\(1,2,3\\) 이 하나씩 적힌 3 개의 공이 들어 있고, 주머니 B 에는 숫자 \\(1,2,3,4\\) 가 하나씩 적힌 4 개의 공이 들어 있다. 두 주머니 \\(A,B\\) 와 한 개의 주사위를 사용하여 다음 시행을 한다."},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/prob28p.JPG"},
      {"order":3,"type":"TEXT","content":"이 시행을 2 번 반복하여 기록한 두 수의 평균을 \\(\\bar{X}\\) 라 할 때, \\(\\mathrm{P}(\\bar{X}=2)\\) 의 값은?"},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/prob28p2.JPG"}
    ]'::jsonb,
    28,
    '[
      {"order":1,"content":"\\(\\frac{11}{81}\\)"},
      {"order":2,"content":"\\(\\frac{13}{81}\\)"},
      {"order":3,"content":"\\(\\frac{5}{27}\\)"},
      {"order":4,"content":"\\(\\frac{17}{81}\\)"},
      {"order":5,"content":"\\(\\frac{19}{81}\\)"}
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
      {"order":1,"type":"TEXT","content":"앞면에는 문자 \\(A\\), 뒷면에는 문자 \\(B\\) 가 적힌 한 장의 카드가 있다. 이 카드와 한 개의 동전을 사용하여 다음 시행을 한다."},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/prob29p.JPG"},
      {"order":3,"type":"TEXT","content":"처음에 문자 \\(A\\) 가 보이도록 카드가 놓여 있을 때, 이 시행을 5 번 반복한 후 문자 \\(B\\) 가 보이도록 카드가 놓일 확률을 \\(p\\) 라 하자."},
      {"order":4,"type":"TEXT","content":"\\(128\\times p\\) 의 값을 구하시오."},
      {"order":5, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/prob29p2.JPG"}
    ]'::jsonb,
    29,
    null,
    62,
    'PROB',
    4,
    'FRQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"다음 조건을 만족시키는 13 이하의 자연수 \\(a,b,c,d\\) 의 모든 순서쌍 \\((a,b,c,d)\\) 의 개수를 구하시오."},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/prob30p.JPG"}
    ]'::jsonb,
    30,
    null,
    336,
    'PROB',
    4,
    'FRQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"\\(\\lim_{x \\rightarrow 0} \\frac{e^{7x}-1}{e^{2x}-1}\\) 의 값은?"}
    ]'::jsonb,
    23,
    '[
      {"order":1,"content":"\\(\\frac{1}{2}\\)"},
      {"order":2,"content":"\\(\\frac{3}{2}\\)"},
      {"order":3,"content":"\\(\\frac{5}{2}\\)"},
      {"order":4,"content":"\\(\\frac{7}{2}\\)"},
      {"order":5,"content":"\\(\\frac{9}{2}\\)"}
    ]'::jsonb,
    4,
    'CALC',
    2,
    'MCQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"매개변수 \\(t\\) 로 나타내어진 곡선"},
      {"order":2,"type":"TEXT","content":"\\[ x=t+\\cos 2t, \\quad y=\\sin^2 t \\]"},
      {"order":3,"type":"TEXT","content":"에서 \\(t=\\frac{\\pi}{4}\\) 일 때, \\(\\frac{dy}{dx}\\) 의 값은?"}
    ]'::jsonb,
    24,
    '[
      {"order":1,"content":"-2"},
      {"order":2,"content":"-1"},
      {"order":3,"content":"0"},
      {"order":4,"content":"1"},
      {"order":5,"content":"2"}
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
      {"order":1,"type":"TEXT","content":"함수 \\(f(x)=x+\\ln x\\) 에 대하여 \\(\\int_1^e \\left(1+\\frac{1}{x}\\right)f(x)\\,dx\\) 의 값은?"}
    ]'::jsonb,
    25,
    '[
      {"order":1,"content":"\\(\\frac{e^2}{2}+\\frac{e}{2}\\)"},
      {"order":2,"content":"\\(\\frac{e^2}{2}+e\\)"},
      {"order":3,"content":"\\(\\frac{e^2}{2}+2e\\)"},
      {"order":4,"content":"\\(e^2+e\\)"},
      {"order":5,"content":"\\(e^2+2e\\)"}
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
      {"order":1,"type":"TEXT","content":"공차가 양수인 등차수열 \\(\\{a_n\\}\\) 과 등비수열 \\(\\{b_n\\}\\) 에 대하여 \\(a_1=b_1=1,\\ a_2b_2=1\\) 이고"},
      {"order":2,"type":"TEXT","content":"\\[ \\sum_{n=1}^{\\infty}\\left(\\frac{1}{a_na_{n+1}}+b_n\\right)=2 \\]"},
      {"order":3,"type":"TEXT","content":"일 때, \\(\\sum_{n=1}^{\\infty} b_n\\) 의 값은?"}
    ]'::jsonb,
    26,
    '[
      {"order":1,"content":"\\(\\frac{7}{6}\\)"},
      {"order":2,"content":"\\(\\frac{6}{5}\\)"},
      {"order":3,"content":"\\(\\frac{5}{4}\\)"},
      {"order":4,"content":"\\(\\frac{4}{3}\\)"},
      {"order":5,"content":"\\(\\frac{3}{2}\\)"}
    ]'::jsonb,
    5,
    'CALC',
    3,
    'MCQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"\\(x=-\\ln 4\\) 에서 \\(x=1\\) 까지의 곡선 \\(y=\\frac{1}{2}\\left(\\left|e^x-1\\right|-e^{|x|}+1\\right)\\) 의 길이는?"}
    ]'::jsonb,
    27,
    '[
      {"order":1,"content":"\\(\\frac{23}{8}\\)"},
      {"order":2,"content":"\\(\\frac{13}{4}\\)"},
      {"order":3,"content":"\\(\\frac{29}{8}\\)"},
      {"order":4,"content":"4"},
      {"order":5,"content":"\\(\\frac{35}{8}\\)"}
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
      {"order":1,"type":"TEXT","content":"실수 \\(a(0<a<2)\\) 에 대하여 함수 \\(f(x)\\) 를"},
      {"order":2,"type":"TEXT","content":"\\[ f(x)=\\begin{cases} 2|\\sin 4x| & (x<0) \\\\ -\\sin ax & (x \\ge 0) \\end{cases} \\]"},
      {"order":3,"type":"TEXT","content":"이라 하자. 함수"},
      {"order":4,"type":"TEXT","content":"\\[ g(x)=\\left|\\int_{-a\\pi}^x f(t)\\,dt\\right| \\]"},
      {"order":5,"type":"TEXT","content":"가 실수 전체의 집합에서 미분가능할 때, \\(a\\) 의 최솟값은?"}
    ]'::jsonb,
    28,
    '[
      {"order":1,"content":"\\(\\frac{1}{2}\\)"},
      {"order":2,"content":"\\(\\frac{3}{4}\\)"},
      {"order":3,"content":"1"},
      {"order":4,"content":"\\(\\frac{5}{4}\\)"},
      {"order":5,"content":"\\(\\frac{3}{2}\\)"}
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
      {"order":1,"type":"TEXT","content":"두 실수 \\(a,b(a>1,b>1)\\) 이"},
      {"order":2,"type":"TEXT","content":"\\[ \\lim_{n \\rightarrow \\infty} \\frac{3^n+a^{n+1}}{3^{n+1}+a^n}=a, \\quad \\lim_{n \\rightarrow \\infty} \\frac{a^n+b^{n+1}}{a^{n+1}+b^n}=\\frac{9}{a} \\]"},
      {"order":3,"type":"TEXT","content":"를 만족시킬 때, \\(a+b\\) 의 값을 구하시오."}
    ]'::jsonb,
    29,
    null,
    18,
    'CALC',
    4,
    'FRQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"길이가 10 인 선분 AB 를 지름으로 하는 원과 선분 AB 위에 \\(\\overline{AC}=4\\) 인 점 C 가 있다."},
      {"order":2,"type":"TEXT","content":"이 원 위의 점 P 를 \\(\\angle PCB=\\theta\\) 가 되도록 잡고, 점 P 를 지나고 선분 AB 에 수직인 직선이 이 원과 만나는 점 중 P 가 아닌 점을 Q 라 하자."},
      {"order":3,"type":"TEXT","content":"삼각형 PCQ 의 넓이를 \\(S(\\theta)\\) 라 할 때, \\(-7\\times S^{\\prime}\\left(\\frac{\\pi}{4}\\right)\\) 의 값을 구하시오. (단, \\(0<\\theta<\\frac{\\pi}{2}\\))"},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/alg10p.JPG"}
    ]'::jsonb,
    30,
    null,
    32,
    'CALC',
    4,
    'FRQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"좌표공간의 점 \\(A(8,6,2)\\) 를 \\(xy\\) 평면에 대하여 대칭이동한 점을 \\(B\\) 라 할 때, 선분 \\(AB\\) 의 길이는?"}
    ]'::jsonb,
    23,
    '[
      {"order":1,"content":"1"},
      {"order":2,"content":"2"},
      {"order":3,"content":"3"},
      {"order":4,"content":"4"},
      {"order":5,"content":"5"}
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
      {"order":1,"type":"TEXT","content":"쌍곡선 \\(\\frac{x^2}{7}-\\frac{y^2}{6}=1\\) 위의 점 \\((7,6)\\) 에서의 접선의 \\(x\\) 절편은?"}
    ]'::jsonb,
    24,
    '[
      {"order":1,"content":"1"},
      {"order":2,"content":"2"},
      {"order":3,"content":"3"},
      {"order":4,"content":"4"},
      {"order":5,"content":"5"}
    ]'::jsonb,
    1,
    'GEO',
    3,
    'MCQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"좌표평면 위의 점 \\(A(4,3)\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ |\\overrightarrow{OP}|=|\\overrightarrow{OA}| \\]"},
      {"order":3,"type":"TEXT","content":"를 만족시키는 점 \\(P\\) 가 나타내는 도형의 길이는? (단, \\(O\\) 는 원점이다.)"}
    ]'::jsonb,
    25,
    '[
      {"order":1,"content":"\\(2\\pi\\)"},
      {"order":2,"content":"\\(4\\pi\\)"},
      {"order":3,"content":"\\(6\\pi\\)"},
      {"order":4,"content":"\\(8\\pi\\)"},
      {"order":5,"content":"\\(10\\pi\\)"}
    ]'::jsonb,
    5,
    'GEO',
    3,
    'MCQ'
)
, (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"그림과 같이 \\(\\overline{AB}=3, \\overline{AD}=3, \\overline{AE}=6\\) 인 직육면체 \\(ABCD-EFGH\\) 가 있다."},
      {"order":2,"type":"TEXT","content":"삼각형 \\(BEG\\) 의 무게중심을 \\(P\\) 라 할 때, 선분 \\(DP\\) 의 길이는?"},
      {"order":3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/geo26p.JPG"}
    ]'::jsonb,
    26,
    '[
      {"order":1,"content":"\\(2\\sqrt{5}\\)"},
      {"order":2,"content":"\\(2\\sqrt{6}\\)"},
      {"order":3,"content":"\\(2\\sqrt{7}\\)"},
      {"order":4,"content":"\\(4\\sqrt{2}\\)"},
      {"order":5,"content":"6"}
    ]'::jsonb,
    2,
    'GEO',
    3,
    'MCQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"양수 \\(p\\) 에 대하여 좌표평면 위에 초점이 \\(F\\) 인 포물선 \\(y^2=4px\\) 가 있다."},
      {"order":2,"type":"TEXT","content":"이 포물선이 세 직선 \\(x=p,\\ x=2p,\\ x=3p\\) 와 만나는 제 1 사분면 위의 점을 각각 \\(P_1, P_2, P_3\\) 이라 하자."},
      {"order":3,"type":"TEXT","content":"\\(\\overline{FP_1}+\\overline{FP_2}+\\overline{FP_3}=27\\) 일 때, \\(p\\) 의 값은?"}
    ]'::jsonb,
    27,
    '[
      {"order":1,"content":"2"},
      {"order":2,"content":"\\(\\frac{5}{2}\\)"},
      {"order":3,"content":"3"},
      {"order":4,"content":"\\(\\frac{7}{2}\\)"},
      {"order":5,"content":"4"}
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
      {"order":1,"type":"TEXT","content":"좌표공간에 중심이 \\(A(0,0,1)\\) 이고 반지름의 길이가 4 인 구 \\(S\\) 가 있다. 구 \\(S\\) 가 \\(xy\\) 평면과 만나서 생기는 원을 \\(C\\) 라 하고, 점 \\(A\\) 에서 선분 \\(PQ\\) 까지의 거리가 2 가 되도록 원 \\(C\\) 위에 두 점 \\(P,Q\\) 를 잡는다."},
      {"order":2,"type":"TEXT","content":"구 \\(S\\) 가 선분 \\(PQ\\) 를 지름으로 하는 구 \\(T\\) 와 만나서 생기는 원 위에서 점 \\(B\\) 가 움직일 때, 삼각형 \\(BPQ\\) 의 \\(xy\\) 평면 위로의 정사영의 넓이의 최댓값은? (단, 점 \\(B\\) 의 \\(z\\) 좌표는 양수이다.)"},
      {"order":3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/geo28p.JPG"}
    ]'::jsonb,
    28,
    '[
      {"order":1,"content":"6"},
      {"order":2,"content":"\\(3\\sqrt{6}\\)"},
      {"order":3,"content":"\\(6\\sqrt{2}\\)"},
      {"order":4,"content":"\\(3\\sqrt{10}\\)"},
      {"order":5,"content":"\\(6\\sqrt{3}\\)"}
    ]'::jsonb,
    1,
    'GEO',
    4,
    'MCQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"한 초점이 \\(F(c,0)(c>0)\\) 인 타원 \\(\\frac{x^2}{9}+\\frac{y^2}{5}=1\\) 과 중심의 좌표가 \\((2,3)\\) 이고 반지름의 길이가 \\(r\\) 인 원이 있다."},
      {"order":2,"type":"TEXT","content":"타원 위의 점 \\(P\\) 와 원 위의 점 \\(Q\\) 에 대하여 \\(\\overline{PQ}-\\overline{PF}\\) 의 최솟값이 6 일 때, \\(r\\) 의 값을 구하시오."}
    ]'::jsonb,
    29,
    null,
    17,
    'GEO',
    4,
    'FRQ'
)
,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"좌표평면에서 \\(\\overline{AB}=\\overline{AC}\\) 이고 \\(\\angle BAC=\\frac{\\pi}{2}\\) 인 직각삼각형 \\(ABC\\) 에 대하여 두 점 \\(P,Q\\) 가 다음 조건을 만족시킨다."},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/geo30p.JPG"},
      {"order":3,"type":"TEXT","content":"선분 \\(AQ\\) 위의 점 \\(X\\) 에 대하여 \\(|\\overrightarrow{XA}+\\overrightarrow{XB}|\\) 의 최솟값을 \\(m\\) 이라 할 때, \\(m^2\\) 의 값을 구하시오."}
    ]'::jsonb,
    30,
    null,
    27,
    'GEO',
    4,
    'FRQ'
)

;






