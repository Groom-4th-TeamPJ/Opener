insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2023
       , 'M09'
       , '9월 모의평가'
       , 30
       , 6000);


WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2023 AND exam_type = 'M09' LIMIT 1
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
  {"order": 1, "type": "TEXT", "content":"\\(\\left(\\frac{2^{\\sqrt{3}}}{2}\\right)^{\\sqrt{3}+1}\\) 의 값은?"}
]'::jsonb
            ,1
            ,'[
  {"order": 1, "content":"\\(\\frac{1}{16}\\)"},
  {"order": 2, "content":"\\(\\frac{1}{4}\\)"},
  {"order": 3, "content":"1"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"16"}
]'::jsonb
            ,4
            , 'ALG'
            , 2
            , 'MCQ'
            )
           ,
        ((SELECT id FROM exam)
            , '[
          {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)=2 x^2+5\\) 에 대하여 \\(\\lim _{x \\rightarrow 2} \\frac{f(x)-f(2)}{x-2}\\) 의 값은?"}
        ]'::jsonb
            ,2
            ,'[
          {"order": 1, "content":"8"},
          {"order": 2, "content":"9"},
          {"order": 3, "content":"10"},
          {"order": 4, "content":"11"},
          {"order": 5, "content":"12"}
        ]'::jsonb
            ,1
            , 'ALG'
            , 2
            , 'MCQ'
            )
           , ((SELECT id FROM exam)
            , '[
    {"order": 1, "type": "TEXT", "content":"\\(\\sin (\\pi-\\theta)=\\frac{5}{13}\\) 이고 \\(\\cos \\theta<0\\) 일 때, \\(\\tan \\theta\\) 의 값은?"}
  ]'::jsonb
            ,3
            ,'[
    {"order": 1, "content":"\\(-\\frac{12}{13}\\)"},
    {"order": 2, "content":"\\(-\\frac{5}{12}\\)"},
    {"order": 3, "content":"0"},
    {"order": 4, "content":"\\(\\frac{5}{12}\\)"},
    {"order": 5, "content":"\\(\\frac{12}{13}\\)"}
  ]'::jsonb
            ,2
            , 'ALG'
            , 3
            , 'MCQ'
            )

           , ((SELECT id FROM exam)
            , '[
    {"order": 1, "type": "TEXT", "content":"함수"},
    {"order": 2, "type": "TEXT", "content":"\\[ f(x)= \\begin{cases}-2 x+a & (x \\leq a) \\\\ a x-6 & (x>a)\\end{cases} \\]"},
    {"order": 3, "type": "TEXT", "content":"가 실수 전체의 집합에서 연속이 되도록 하는 모든 상수 \\(a\\) 의 값의 합은?"}
  ]'::jsonb
            ,4
            ,'[
    {"order": 1, "content":"-1"},
    {"order": 2, "content":"-2"},
    {"order": 3, "content":"-3"},
    {"order": 4, "content":"-4"},
    {"order": 5, "content":"-5"}
  ]'::jsonb
            , 1
            , 'ALG'
            , 3
            , 'MCQ'
            )
           ,
        ((SELECT id FROM exam)
            , '[
          {"order": 1, "type": "TEXT", "content":"등차수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여"},
          {"order": 2, "type": "TEXT", "content":"\\[ a_1=2 a_5, \\quad a_8+a_{12}=-6 \\]"},
          {"order": 3, "type": "TEXT", "content":"일 때, \\(a_2\\) 의 값은?"}
        ]'::jsonb
            ,5
            ,'[
          {"order": 1, "content":"17"},
          {"order": 2, "content":"19"},
          {"order": 3, "content":"21"},
          {"order": 4, "content":"23"},
          {"order": 5, "content":"25"}
        ]'::jsonb
            ,3
            , 'ALG'
            , 3
            , 'MCQ'
            )
           ,
        ((SELECT id FROM exam)
            , '[
          {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)=x^3-3 x^2+k\\) 의 극댓값이 9 일 때, 함수 \\(f(x)\\) 의 극솟값은? (단, \\(k\\) 는 상수이다.)"}
        ]'::jsonb
            ,6
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


           , ((SELECT id FROM exam)
             , '[
  {"order": 1, "type": "TEXT", "content":"수열 \\(\\left\\{a_n\\right\\}\\) 의 첫째항부터 제 \\(n\\) 항까지의 합을 \\(S_n\\) 이라 하자. \\(S_n=\\frac{1}{n(n+1)}\\) 일 때, \\(\\sum_{k=1}^{10}\\left(S_k-a_k\\right)\\) 의 값은?"}
]'::jsonb
             ,7
             ,'[
  {"order": 1, "content":"\\(\\frac{1}{2}\\)"},
  {"order": 2, "content":"\\(\\frac{3}{5}\\)"},
  {"order": 3, "content":"\\(\\frac{7}{10}\\)"},
  {"order": 4, "content":"\\(\\frac{4}{5}\\)"},
  {"order": 5, "content":"\\(\\frac{9}{10}\\)"}
]'::jsonb
             ,5
             , 'ALG'
             , 3
             , 'MCQ'
       )

        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"곡선 \\(y=x^3-4 x+5\\) 위의 점 \\((1,2)\\) 에서의 접선이 곡선 \\(y=x^4+3 x+a\\) 에 접할 때, 상수 \\(a\\) 의 값은?"}
       ]'::jsonb
       ,8
       ,'[
         {"order": 1, "content":"6"},
         {"order": 2, "content":"7"},
         {"order": 3, "content":"8"},
         {"order": 4, "content":"9"},
         {"order": 5, "content":"10"}
       ]'::jsonb
       ,1
       , 'ALG'
       , 3
       , 'MCQ'
       )
       ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"닫힌구간 \\([0,12]\\) 에서 정의된 두 함수"},
         {"order": 2, "type": "TEXT", "content":"\\[ f(x)=\\cos \\frac{\\pi x}{6}, \\quad g(x)=-3 \\cos \\frac{\\pi x}{6}-1 \\]"},
         {"order": 3, "type": "TEXT", "content":"이 있다. 곡선 \\(y=f(x)\\) 와 직선 \\(y=k\\) 가 만나는 두 점의 \\(x\\) 좌표를 \\(\\alpha_1, \\alpha_2\\) 라 할 때, \\(\\left|\\alpha_1-\\alpha_2\\right|=8\\) 이다."},
         {"order": 4, "type": "TEXT", "content":"곡선 \\(y=g(x)\\) 와 직선 \\(y=k\\) 가 만나는 두 점의 \\(x\\) 좌표를 \\(\\beta_1, \\beta_2\\) 라 할 때, \\(\\left|\\beta_1-\\beta_2\\right|\\) 의 값은? (단, \\(k\\) 는 \\(-1<k<1\\) 인 상수이다.)"}
       ]'::jsonb
       ,9
       ,'[
         {"order": 1, "content":"3"},
         {"order": 2, "content":"\\(\\frac{7}{2}\\)"},
         {"order": 3, "content":"4"},
         {"order": 4, "content":"\\(\\frac{9}{2}\\)"},
         {"order": 5, "content":"5"}
       ]'::jsonb
       ,3
       , 'ALG'
       , 4
       , 'MCQ'
       )
        ,
    ((SELECT id FROM exam)
         , '[
  {"order": 1, "type": "TEXT", "content":"수직선 위의 점 \\(\\mathrm{A}(6)\\) 과 시각 \\(t=0\\) 일 때 원점을 출발하여 이 수직선 위를 움직이는 점 P 가 있다. 시각 \\(t(t \\geq 0)\\) 에서의 점 P 의 속도 \\(v(t)\\) 를"},
  {"order": 2, "type": "TEXT", "content":"\\[ v(t)=3 t^2+a t \\quad(a>0) \\]"},
  {"order": 3, "type": "TEXT", "content":"이라 하자. 시각 \\(t=2\\) 에서 점 P 와 점 A 사이의 거리가 10 일 때, 상수 \\(a\\) 의 값은?"}
]'::jsonb
         ,10
         ,'[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
]'::jsonb
         ,4
         , 'ALG'
         , 4
         , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)=-(x-2)^2+k\\) 에 대하여 다음 조건을 만족시키는 자연수 \\(n\\) 의 개수가 2 일 때, 상수 \\(k\\) 의 값은?"},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/alg11p.JPG"}
       ]'::jsonb
       ,11
       ,'[
         {"order": 1, "content":"8"},
         {"order": 2, "content":"9"},
         {"order": 3, "content":"10"},
         {"order": 4, "content":"11"},
         {"order": 5, "content":"12"}
       ]'::jsonb
       ,2
       , 'ALG'
       , 4
       , 'MCQ'
       )

        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"실수 \\(t(t>0)\\) 에 대하여 직선 \\(y=x+t\\) 와 곡선 \\(y=x^2\\) 이 만나는 두 점을 \\(\\mathrm{A}, \\mathrm{B}\\) 라 하자."},
         {"order": 2, "type": "TEXT", "content":"점 A 를 지나고 \\(x\\) 축에 평행한 직선이 곡선 \\(y=x^2\\) 과 만나는 점 중 A 가 아닌 점을 C , 점 B 에서 선분 AC 에 내린 수선의 발을 H 라 하자."},
         {"order": 3, "type": "TEXT", "content":"\\(\\lim _{t \\rightarrow 0+} \\frac{\\overline{\\mathrm{AH}}-\\overline{\\mathrm{CH}}}{t}\\) 의 값은? (단, 점 A 의 \\(x\\) 좌표는 양수이다.)"},
         {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/alg12p.JPG"}
       ]'::jsonb
       ,12
       ,'[
         {"order": 1, "content":"1"},
         {"order": 2, "content":"2"},
         {"order": 3, "content":"3"},
         {"order": 4, "content":"4"},
         {"order": 5, "content":"5"}
       ]'::jsonb
       ,2
       , 'ALG'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"그림과 같이 선분 AB 를 지름으로 하는 반원의 호 AB 위에 두 점 \\(\\mathrm{C}, \\mathrm{D}\\) 가 있다. 선분 AB 의 중점 O 에 대하여 두 선분 \\(\\mathrm{AD}, \\mathrm{CO}\\) 가 점 E 에서 만나고,"},
         {"order": 2, "type": "TEXT", "content":"\\[ \\overline{\\mathrm{CE}}=4, \\quad \\overline{\\mathrm{ED}}=3 \\sqrt{2}, \\quad \\angle \\mathrm{CEA}=\\frac{3}{4} \\pi \\]"},
         {"order": 3, "type": "TEXT", "content":"이다. \\(\\overline{\\mathrm{AC}} \\times \\overline{\\mathrm{CD}}\\) 의 값은?"},
         {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/alg13p.JPG"}
       ]'::jsonb
       ,13
       ,'[
         {"order": 1, "content":"\\(6 \\sqrt{10}\\)"},
         {"order": 2, "content":"\\(10 \\sqrt{5}\\)"},
         {"order": 3, "content":"\\(16 \\sqrt{2}\\)"},
         {"order": 4, "content":"\\(12 \\sqrt{5}\\)"},
         {"order": 5, "content":"\\(20 \\sqrt{2}\\)"}
       ]'::jsonb
       ,5
       , 'ALG'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"최고차항의 계수가 1 이고 \\(f(0)=0, f(1)=0\\) 인 삼차함수 \\(f(x)\\) 에 대하여 함수 \\(g(t)\\) 를"},
         {"order": 2, "type": "TEXT", "content":"\\[ g(t)=\\int_t^{t+1} f(x) d x-\\int_0^1|f(x)| d x \\]"},
         {"order": 3, "type": "TEXT", "content":"라 할 때, <보기>에서 옳은 것만을 있는 대로 고른 것은?"},
         {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/alg14p.JPG"}
       ]'::jsonb
       ,14
       ,'[
         {"order": 1, "content":"ᄀ"},
         {"order": 2, "content":"ᄀ, ᄂ"},
         {"order": 3, "content":"ᄀ, ᄃ"},
         {"order": 4, "content":"ᄂ, ᄃ"},
         {"order": 5, "content":"ᄀ, ᄂ, ᄃ"}
       ]'::jsonb
       ,5
       , 'ALG'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"수열 \\(\\left\\{a_n\\right\\}\\) 이 다음 조건을 만족시킨다."},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/alg15p.JPG"},
         {"order": 3, "type": "TEXT", "content":"\\(\\left|a_m\\right| \\geq 5\\) 를 만족시키는 100 이하의 자연수 \\(m\\) 의 개수를 \\(p\\) 라 할 때, \\(p+a_1\\) 의 값은?"}
       ]'::jsonb
       ,15
       ,'[
         {"order": 1, "content":"8"},
         {"order": 2, "content":"10"},
         {"order": 3, "content":"12"},
         {"order": 4, "content":"14"},
         {"order": 5, "content":"16"}
       ]'::jsonb
       ,3
       , 'ALG'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"방정식 \\(\\log _3(x-4)=\\log _9(x+2)\\) 를 만족시키는 실수 \\(x\\) 의 값을 구하시오."}
       ]'::jsonb
       ,16
       ,null
       ,7
       , 'ALG'
       , 3
       , 'FRQ'
       )
,

       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)\\) 에 대하여 \\(f^{\\prime}(x)=6 x^2-4 x+3\\) 이고 \\(f(1)=5\\) 일 때, \\(f(2)\\) 의 값을 구하시오."}
       ]'::jsonb
       ,17
       ,null
       ,16
       , 'ALG'
       , 3
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 \\(\\sum_{k=1}^5 a_k=10\\) 일 때,"},
         {"order": 2, "type": "TEXT", "content":"\\[ \\sum_{k=1}^5 c a_k=65+\\sum_{k=1}^5 c \\]"},
         {"order": 3, "type": "TEXT", "content":"를 만족시키는 상수 \\(c\\) 의 값을 구하시오."}
       ]'::jsonb
       ,18
       ,null
       ,13
       , 'ALG'
       , 3
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"방정식 \\(3 x^4-4 x^3-12 x^2+k=0\\) 이 서로 다른 4 개의 실근을 갖도록 하는 자연수 \\(k\\) 의 개수를 구하시오."}
       ]'::jsonb
       ,19
       ,null
       ,4
       , 'ALG'
       , 3
       , 'FRQ'
       )
,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"상수 \\(k(k<0)\\) 에 대하여 두 함수"},
         {"order": 2, "type": "TEXT", "content":"\\[ f(x)=x^3+x^2-x, \\quad g(x)=4|x|+k \\]"},
         {"order": 3, "type": "TEXT", "content":"의 그래프가 만나는 점의 개수가 2 일 때, 두 함수의 그래프로 둘러싸인 부분의 넓이를 \\(S\\) 라 하자. \\(30 \\times S\\) 의 값을 구하시오."}
       ]'::jsonb
       ,20
       ,null
       ,80
       , 'ALG'
       , 4
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"그림과 같이 곡선 \\(y=2^x\\) 위에 두 점 \\(\\mathrm{P}\\left(a, 2^a\\right), \\mathrm{Q}\\left(b, 2^b\\right)\\) 이 있다. 직선 PQ 의 기울기를 \\(m\\) 이라 할 때, 점 P 를 지나며 기울기가 \\(-m\\) 인 직선이 \\(x\\) 축, \\(y\\) 축과 만나는 점을 각각 \\(\\mathrm{A}, \\mathrm{B}\\) 라 하고, 점 Q 를 지나며 기울기가 \\(-m\\) 인 직선이 \\(x\\) 축과 만나는 점을 C 라 하자."},
         {"order": 2, "type": "TEXT", "content":"\\[ \\overline{\\mathrm{AB}}=4 \\overline{\\mathrm{~PB}}, \\quad \\overline{\\mathrm{CQ}}=3 \\overline{\\mathrm{AB}} \\]"},
         {"order": 3, "type": "TEXT", "content":"일 때, \\(90 \\times(a+b)\\) 의 값을 구하시오. (단, \\(0<a<b\\) )"},
         {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/alg21p.JPG"}
       ]'::jsonb
       ,21
       ,null
       ,220
       , 'ALG'
       , 4
       , 'FRQ'
       )
        ,

       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"최고차항의 계수가 1 이고 \\(x=3\\) 에서 극댓값 8 을 갖는 삼차함수 \\(f(x)\\) 가 있다. 실수 \\(t\\) 에 대하여 함수 \\(g(x)\\) 를"},
         {"order": 2, "type": "TEXT", "content":"\\[ g(x)= \\begin{cases}f(x) & (x \\geq t) \\\\ -f(x)+2 f(t) & (x<t)\\end{cases} \\]"},
         {"order": 3, "type": "TEXT", "content":"라 할 때, 방정식 \\(g(x)=0\\) 의 서로 다른 실근의 개수를 \\(h(t)\\) 라 하자. 함수 \\(h(t)\\) 가 \\(t=a\\) 에서 불연속인 \\(a\\) 의 값이 두 개일 때, \\(f(8)\\) 의 값을 구하시오."}
       ]'::jsonb
       ,22
       ,null
       ,58
       , 'ALG'
       , 4
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"다항식 \\(\\left(x^2+2\\right)^6\\) 의 전개식에서 \\(x^4\\) 의 계수는?"}
       ]'::jsonb
       ,23
       ,'[
         {"order": 1, "content":"240"},
         {"order": 2, "content":"270"},
         {"order": 3, "content":"300"},
         {"order": 4, "content":"330"},
         {"order": 5, "content":"360"}
       ]'::jsonb
       ,1
       , 'PROB'
       , 2
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"두 사건 \\(A, B\\) 에 대하여"},
         {"order": 2, "type": "TEXT", "content":"\\[ \\mathrm{P}(A \\cup B)=1, \\quad \\mathrm{P}(A \\cap B)=\\frac{1}{4}, \\quad \\mathrm{P}(A \\mid B)=\\mathrm{P}(B \\mid A) \\]"},
         {"order": 3, "type": "TEXT", "content":"일 때, \\(\\mathrm{P}(A)\\) 의 값은?"}
       ]'::jsonb
       ,24
       ,'[
         {"order": 1, "content":"\\(\\frac{1}{2}\\)"},
         {"order": 2, "content":"\\(\\frac{9}{16}\\)"},
         {"order": 3, "content":"\\(\\frac{5}{8}\\)"},
         {"order": 4, "content":"\\(\\frac{11}{16}\\)"},
         {"order": 5, "content":"\\(\\frac{3}{4}\\)"}
       ]'::jsonb
       ,3
       , 'PROB'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"어느 인스턴트 커피 제조 회사에서 생산하는 A 제품 1 개의 중량은 평균이 9 , 표준편차가 0.4 인 정규분포를 따르고, B 제품 1 개의 중량은 평균이 20 , 표준편차가 1 인 정규분포를 따른다고 한다."},
         {"order": 2, "type": "TEXT", "content":"이 회사에서 생산한 A 제품 중에서 임의로 선택한 1 개의 중량이 8.9 이상 9.4 이하일 확률과 B 제품 중에서 임의로 선택한 1 개의 중량이 19 이상 \\(k\\) 이하일 확률이 서로 같다. 상수 \\(k\\) 의 값은? (단, 중량의 단위는 g 이다.)"}
       ]'::jsonb
       ,25
       ,'[
         {"order": 1, "content":"19.5"},
         {"order": 2, "content":"19.75"},
         {"order": 3, "content":"20"},
         {"order": 4, "content":"20.25"},
         {"order": 5, "content":"20.5"}
       ]'::jsonb
       ,4
       , 'PROB'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"세 학생 \\(\\mathrm{A}, \\mathrm{B}, \\mathrm{C}\\) 를 포함한 7 명의 학생이 원 모양의 탁자에 일정한 간격을 두고 임의로 모두 둘러앉을 때, A 가 B 또는 C 와 이웃하게 될 확률은?"},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/prob26p.JPG"}
       ]'::jsonb
       ,26
       ,'[
         {"order": 1, "content":"\\(\\frac{1}{2}\\)"},
         {"order": 2, "content":"\\(\\frac{3}{5}\\)"},
         {"order": 3, "content":"\\(\\frac{7}{10}\\)"},
         {"order": 4, "content":"\\(\\frac{4}{5}\\)"},
         {"order": 5, "content":"\\(\\frac{9}{10}\\)"}
       ]'::jsonb
       ,2
       , 'PROB'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"이산확률변수 \\(X\\) 의 확률분포를 표로 나타내면 다음과 같다."},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/prob27p.JPG"},
         {"order": 3, "type": "TEXT", "content":"\\(\\sigma(X)=\\mathrm{E}(X)\\) 일 때, \\(\\mathrm{E}\\left(X^2\\right)+\\mathrm{E}(X)\\) 의 값은? (단, \\(a>1\\))"}
       ]'::jsonb
       ,27
       ,'[
         {"order": 1, "content":"29"},
         {"order": 2, "content":"33"},
         {"order": 3, "content":"37"},
         {"order": 4, "content":"41"},
         {"order": 5, "content":"45"}
       ]'::jsonb
       ,5
       , 'PROB'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"1 부터 10 까지의 자연수 중에서 임의로 서로 다른 3 개의 수를 선택한다. 선택된 세 개의 수의 곱이 5 의 배수이고 합은 3 의 배수일 확률은?"}
       ]'::jsonb
       ,28
       ,'[
         {"order": 1, "content":"\\(\\frac{3}{20}\\)"},
         {"order": 2, "content":"\\(\\frac{1}{6}\\)"},
         {"order": 3, "content":"\\(\\frac{11}{60}\\)"},
         {"order": 4, "content":"\\(\\frac{1}{5}\\)"},
         {"order": 5, "content":"\\(\\frac{13}{60}\\)"}
       ]'::jsonb
       ,3
       , 'PROB'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"1 부터 6 까지의 자연수가 하나씩 적힌 6 장의 카드가 들어 있는 주머니가 있다. 이 주머니에서 임의로 한 장의 카드를 꺼내어 카드에 적힌 수를 확인한 후 다시 넣는 시행을 한다. 이 시행을 4 번 반복하여 확인한 네 개의 수의 평균을 \\(\\bar{X}\\) 라 할 때, \\(\\mathrm{P}\\left(\\bar{X}=\\frac{11}{4}\\right)=\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/prob29p.JPG"}
       ]'::jsonb
       ,29
       ,null
       ,175
       , 'PROB'
       , 4
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"집합 \\(X=\\{1,2,3,4,5\\}\\) 와 함수 \\(f: X \\rightarrow X\\) 에 대하여 함수 \\(f\\) 의 치역을 \\(A\\), 합성함수 \\(f \\circ f\\) 의 치역을 \\(B\\) 라 할 때, 다음 조건을 만족시키는 함수 \\(f\\) 의 개수를 구하시오."},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/prob30p.JPG"}
       ]'::jsonb
       ,30
       ,null
       ,260
       , 'PROB'
       , 4
       , 'FRQ'
       )
,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"\\(\\lim _{x \\rightarrow 0} \\frac{4^x-2^x}{x}\\) 의 값은?"}
       ]'::jsonb
       ,23
       ,'[
         {"order": 1, "content":"\\(\\ln 2\\)"},
         {"order": 2, "content":"1"},
         {"order": 3, "content":"\\(2 \\ln 2\\)"},
         {"order": 4, "content":"2"},
         {"order": 5, "content":"\\(3 \\ln 2\\)"}
       ]'::jsonb
       ,1
       , 'CALC'
       , 2
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"\\(\\int_0^\\pi x \\cos \\left(\\frac{\\pi}{2}-x\\right) d x\\) 의 값은?"}
       ]'::jsonb
       ,24
       ,'[
         {"order": 1, "content":"\\(\\frac{\\pi}{2}\\)"},
         {"order": 2, "content":"\\(\\pi\\)"},
         {"order": 3, "content":"\\(\\frac{3 \\pi}{2}\\)"},
         {"order": 4, "content":"\\(2 \\pi\\)"},
         {"order": 5, "content":"\\(\\frac{5 \\pi}{2}\\)"}
       ]'::jsonb
       ,2
       , 'CALC'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 \\(\\lim _{n \\rightarrow \\infty} \\frac{a_n+2}{2}=6\\) 일 때, \\(\\lim _{n \\rightarrow \\infty} \\frac{n a_n+1}{a_n+2 n}\\) 의 값은?"}
       ]'::jsonb
       ,25
       ,'[
         {"order": 1, "content":"1"},
         {"order": 2, "content":"2"},
         {"order": 3, "content":"3"},
         {"order": 4, "content":"4"},
         {"order": 5, "content":"5"}
       ]'::jsonb
       ,5
       , 'CALC'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"그림과 같이 양수 \\(k\\) 에 대하여 곡선 \\(y=\\sqrt{\\frac{k x}{2 x^2+1}}\\) 와 \\(x\\) 축 및 두 직선 \\(x=1, x=2\\) 로 둘러싸인 부분을 밑면으로 하고 \\(x\\) 축에 수직인 평면으로 자른 단면이 모두 정사각형인 입체도형의 부피가 \\(2 \\ln 3\\) 일 때, \\(k\\) 의 값은?"},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/calc26p.JPG"}
       ]'::jsonb
       ,26
       ,'[
         {"order": 1, "content":"6"},
         {"order": 2, "content":"7"},
         {"order": 3, "content":"8"},
         {"order": 4, "content":"9"},
         {"order": 5, "content":"10"}
       ]'::jsonb
       ,3
       , 'CALC'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"그림과 같이 \\(\\overline{\\mathrm{A}_1 \\mathrm{~B}_1}=4, \\overline{\\mathrm{~A}_1 \\mathrm{D}_1}=1\\) 인 직사각형 \\(\\mathrm{A}_1 \\mathrm{~B}_1 \\mathrm{C}_1 \\mathrm{D}_1\\) 에서 두 대각선의 교점을 \\(\\mathrm{E}_1\\) 이라 하자."},
         {"order": 2, "type": "TEXT", "content":"\\(\\overline{\\mathrm{A}_2 \\mathrm{D}_1}=\\overline{\\mathrm{D}_1 \\mathrm{E}_1}, \\angle \\mathrm{~A}_2 \\mathrm{D}_1 \\mathrm{E}_1=\\frac{\\pi}{2}\\) 이고 선분 \\(\\mathrm{D}_1 \\mathrm{C}_1\\) 과 선분 \\(\\mathrm{A}_2 \\mathrm{E}_1\\) 이 만나도록 점 \\(\\mathrm{A}_2\\) 를 잡고, \\(\\overline{\\mathrm{B}_2 \\mathrm{C}_1}=\\overline{\\mathrm{C}_1 \\mathrm{E}_1}, \\angle \\mathrm{~B}_2 \\mathrm{C}_1 \\mathrm{E}_1=\\frac{\\pi}{2}\\) 이고 선분 \\(\\mathrm{D}_1 \\mathrm{C}_1\\) 과 선분 \\(\\mathrm{B}_2 \\mathrm{E}_1\\) 이 만나도록 점 \\(\\mathrm{B}_2\\) 를 잡는다."},
         {"order": 3, "type": "TEXT", "content":"두 삼각형 \\(\\mathrm{A}_2 \\mathrm{D}_1 \\mathrm{E}_1, \\mathrm{~B}_2 \\mathrm{C}_1 \\mathrm{E}_1\\) 을 그린 후 \\(\\triangle\\) 모양의 도형에 색칠하여 얻은 그림을 \\(R_1\\) 이라 하자."},
         {"order": 4, "type": "TEXT", "content":"그림 \\(R_1\\) 에서 \\(\\overline{\\mathrm{A}_2 \\mathrm{~B}_2}: \\overline{\\mathrm{A}_2 \\mathrm{D}_2}=4: 1\\) 이고 선분 \\(\\mathrm{D}_2 \\mathrm{C}_2\\) 가 두 선분 \\(\\mathrm{A}_2 \\mathrm{E}_1, \\mathrm{~B}_2 \\mathrm{E}_1\\) 과 만나지 않도록 직사각형 \\(\\mathrm{A}_2 \\mathrm{~B}_2 \\mathrm{C}_2 \\mathrm{D}_2\\) 를 그린다."},
         {"order": 5, "type": "TEXT", "content":"그림 \\(R_1\\) 을 얻은 것과 같은 방법으로 세 점 \\(\\mathrm{E}_2, \\mathrm{~A}_3, \\mathrm{~B}_3\\) 을 잡고 두 삼각형 \\(\\mathrm{A}_3 \\mathrm{D}_2 \\mathrm{E}_2, \\mathrm{~B}_3 \\mathrm{C}_2 \\mathrm{E}_2\\) 를 그린 후 \\(\\triangle\\) 모양의 도형에 색칠하여 얻은 그림을 \\(R_2\\) 라 하자."},
         {"order": 6, "type": "TEXT", "content":"이와 같은 과정을 계속하여 \\(n\\) 번째 얻은 그림 \\(R_n\\) 에 색칠되어 있는 부분의 넓이를 \\(S_n\\) 이라 할 때, \\(\\lim _{n \\rightarrow \\infty} S_n\\) 의 값은?"},
         {"order": 7, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/calc27p.JPG"}
       ]'::jsonb
       ,27
       ,'[
         {"order": 1, "content":"\\(\\frac{68}{5}\\)"},
         {"order": 2, "content":"\\(\\frac{34}{3}\\)"},
         {"order": 3, "content":"\\(\\frac{68}{7}\\)"},
         {"order": 4, "content":"\\(\\frac{17}{2}\\)"},
         {"order": 5, "content":"\\(\\frac{68}{9}\\)"}
       ]'::jsonb
       ,3
       , 'CALC'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"그림과 같이 반지름의 길이가 1 이고 중심각의 크기가 \\(\\frac{\\pi}{2}\\) 인 부채꼴 OAB 가 있다."},
         {"order": 2, "type": "TEXT", "content":"호 AB 위의 점 P 에 대하여 \\(\\overline{\\mathrm{PA}}=\\overline{\\mathrm{PC}}=\\overline{\\mathrm{PD}}\\) 가 되도록 호 PB 위에 점 C 와 선분 OA 위에 점 D 를 잡는다."},
         {"order": 3, "type": "TEXT", "content":"점 D 를 지나고 선분 OP 와 평행한 직선이 선분 PA 와 만나는 점을 E 라 하자."},
         {"order": 4, "type": "TEXT", "content":"\\(\\angle \\mathrm{POA}=\\theta\\) 일 때, 삼각형 CDP 의 넓이를 \\(f(\\theta)\\), 삼각형 EDA 의 넓이를 \\(g(\\theta)\\) 라 하자."},
         {"order": 5, "type": "TEXT", "content":"\\(\\lim _{\\theta \\rightarrow 0+} \\frac{g(\\theta)}{\\theta^2 \\times f(\\theta)}\\) 의 값은? (단, \\(0<\\theta<\\frac{\\pi}{4}\\) )"},
         {"order": 6, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/calc28p.JPG"}
       ]'::jsonb
       ,28
       ,'[
         {"order": 1, "content":"\\(\\frac{1}{8}\\)"},
         {"order": 2, "content":"\\(\\frac{1}{4}\\)"},
         {"order": 3, "content":"\\(\\frac{3}{8}\\)"},
         {"order": 4, "content":"\\(\\frac{1}{2}\\)"},
         {"order": 5, "content":"\\(\\frac{5}{8}\\)"}
       ]'::jsonb
       ,4
       , 'CALC'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)=e^x+x\\) 가 있다. 양수 \\(t\\) 에 대하여 점 \\((t, 0)\\) 과 점 \\((x, f(x))\\) 사이의 거리가 \\(x=s\\) 에서 최소일 때, 실수 \\(f(s)\\) 의 값을 \\(g(t)\\) 라 하자."},
         {"order": 2, "type": "TEXT", "content":"함수 \\(g(t)\\) 의 역함수를 \\(h(t)\\) 라 할 때, \\(h^{\\prime}(1)\\) 의 값을 구하시오."},
         {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/calc29p.JPG"}
       ]'::jsonb
       ,29
       ,null
       ,3
       , 'CALC'
       , 4
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"최고차항의 계수가 1 인 사차함수 \\(f(x)\\) 와 구간 \\((0, \\infty)\\) 에서 \\(g(x) \\geq 0\\) 인 함수 \\(g(x)\\) 가 다음 조건을 만족시킨다."},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/calc30p.JPG"},
         {"order": 3, "type": "TEXT", "content":"\\(\\int_4^5 g(x) d x=\\frac{q}{p}\\) 일 때, \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
       ]'::jsonb
       ,30
       ,null
       ,283
       , 'CALC'
       , 4
       , 'FRQ'
       )
        ,
       (
           ((SELECT id FROM exam)),
           '[
             {"order":1,"type":"TEXT","content":"좌표공간의 두 점 \\((\\mathrm{A}(a,1,-1), \\mathrm{B}(-5,b,3))\\) 에 대하여 선분 AB 의 중점의 좌표가 \\((8,3,1)\\) 일 때, \\(a+b\\) 의 값은?"}
           ]'::jsonb,
           23,
           '[
             {"order":1,"content":"20"},
             {"order":2,"content":"22"},
             {"order":3,"content":"24"},
             {"order":4,"content":"26"},
             {"order":5,"content":"28"}
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
             {"order":1,"type":"TEXT","content":"쌍곡선 \\(\\frac{x^2}{a^2}-y^2=1\\) 위의 점 \\((2a, \\sqrt{3})\\) 에서의 접선이 직선 \\(y=-\\sqrt{3}x+1\\) 과 수직일 때, 상수 \\(a\\) 의 값은?"}
           ]'::jsonb,
           24,
           '[
             {"order":1,"content":"1"},
             {"order":2,"content":"2"},
             {"order":3,"content":"3"},
             {"order":4,"content":"4"},
             {"order":5,"content":"5"}
           ]'::jsonb,
           2,
           'GEO',
           3,
           'MCQ'
       )
        ,(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"타원 \\(\\frac{x^2}{a^2}+\\frac{y^2}{5}=1\\) 의 두 초점을 \\(\\mathrm{F},\\ \\mathrm{F}^{\\prime}\\) 이라 하자. 점 F 를 지나고 \\(x\\) 축에 수직인 직선 위의 점 A 가 \\(\\overline{\\mathrm{AF}^{\\prime}}=5,\\ \\overline{\\mathrm{AF}}=3\\) 을 만족시킨다."},
      {"order":2,"type":"TEXT","content":"선분 \\(\\mathrm{AF}^{\\prime}\\) 과 타원이 만나는 점을 P 라 할 때, 삼각형 \\(\\mathrm{PF}^{\\prime}\\mathrm{F}\\) 의 둘레의 길이는? (단, \\(a\\) 는 \\(a>\\sqrt{5}\\) 인 상수이다.)"},
      {"order":3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/geo25p.JPG"}
    ]'::jsonb,
    25,
    '[
      {"order":1,"content":"8"},
      {"order":2,"content":"\\(\\frac{17}{2}\\)"},
      {"order":3,"content":"9"},
      {"order":4,"content":"\\(\\frac{19}{2}\\)"},
      {"order":5,"content":"10"}
    ]'::jsonb,
    4,
    'GEO',
    3,
    'MCQ'
)
     , (
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"좌표평면 위의 점 \\(\\mathrm{A}(3,0)\\) 에 대하여"},
      {"order":2,"type":"TEXT","content":"\\[ (\\overrightarrow{\\mathrm{OP}}-\\overrightarrow{\\mathrm{OA}}) \\cdot (\\overrightarrow{\\mathrm{OP}}-\\overrightarrow{\\mathrm{OA}})=5 \\] 를 만족시키는 점 P 가 나타내는 도형과 직선 \\(y=\\frac{1}{2}x+k\\) 가 오직 한 점에서 만날 때, 양수 \\(k\\) 의 값은? (단, O 는 원점이다.)"}
    ]'::jsonb,
    26,
    '[
      {"order":1,"content":"\\(\\frac{3}{5}\\)"},
      {"order":2,"content":"\\(\\frac{4}{5}\\)"},
      {"order":3,"content":"1"},
      {"order":4,"content":"\\(\\frac{6}{5}\\)"},
      {"order":5,"content":"\\(\\frac{7}{5}\\)"}
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
      {"order":1,"type":"TEXT","content":"그림과 같이 밑면의 반지름의 길이가 4, 높이가 3 인 원기둥이 있다. 선분 AB 는 이 원기둥의 한 밑면의 지름이고 \\(\\mathrm{C},\\ \\mathrm{D}\\) 는 다른 밑면의 둘레 위의 서로 다른 두 점이다. 네 점 \\(\\mathrm{A},\\ \\mathrm{B},\\ \\mathrm{C},\\ \\mathrm{D}\\) 가 다음 조건을 만족시킬 때, 선분 \\(\\mathrm{CD}\\) 의 길이는?"},
      {"order":2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/geo27p.JPG"}
    ]'::jsonb,
    27,
    '[
      {"order":1,"content":"5"},
      {"order":2,"content":"\\(\\frac{11}{2}\\)"},
      {"order":3,"content":"6"},
      {"order":4,"content":"\\(\\frac{13}{2}\\)"},
      {"order":5,"content":"7"}
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
      {"order":1,"type":"TEXT","content":"실수 \\(p(p \\geq 1)\\) 과 함수 \\(f(x)=(x+a)^2\\) 에 대하여 두 포물선"},
      {"order":2,"type":"TEXT","content":"\\[ C_1: y^2=4x, \\quad C_2:(y-3)^2=4p\\{x-f(p)\\} \\]"},
      {"order":3,"type":"TEXT","content":"가 제 1 사분면에서 만나는 점을 A 라 하자. 두 포물선 \\(C_1, C_2\\) 의 초점을 각각 \\(\\mathrm{F}_1,\\ \\mathrm{F}_2\\) 라 할 때, \\(\\overline{\\mathrm{AF}_1}=\\overline{\\mathrm{AF}_2}\\) 를 만족시키는 \\(p\\) 가 오직 하나가 되도록 하는 상수 \\(a\\) 의 값은?"},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/geo28p.JPG"}
    ]'::jsonb,
    28,
    '[
      {"order":1,"content":"\\(-\\frac{3}{4}\\)"},
      {"order":2,"content":"\\(-\\frac{5}{8}\\)"},
      {"order":3,"content":"\\(-\\frac{1}{2}\\)"},
      {"order":4,"content":"\\(-\\frac{3}{8}\\)"},
      {"order":5,"content":"\\(-\\frac{1}{4}\\)"}
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
      {"order":1,"type":"TEXT","content":"좌표공간에 두 개의 구"},
      {"order":2,"type":"TEXT","content":"\\[ S_1: x^2+y^2+(z-2)^2=4, \\quad S_2: x^2+y^2+(z+7)^2=49 \\]"},
      {"order":3,"type":"TEXT","content":"가 있다. 점 \\(\\mathrm{A}(\\sqrt{5},0,0)\\) 을 지나고 \\(zx\\) 평면에 수직이며, 구 \\(S_1\\) 과 \\(z\\) 좌표가 양수인 한 점에서 접하는 평면을 \\(\\alpha\\) 라 하자."},
      {"order":4,"type":"TEXT","content":"구 \\(S_2\\) 가 평면 \\(\\alpha\\) 와 만나서 생기는 원을 \\(C\\) 라 할 때, 원 \\(C\\) 위의 점 중 \\(z\\) 좌표가 최소인 점을 B 라 하고 구 \\(S_2\\) 와 점 B 에서 접하는 평면을 \\(\\beta\\) 라 하자."},
      {"order":5,"type":"TEXT","content":"원 \\(C\\) 의 평면 \\(\\beta\\) 위로의 정사영의 넓이가 \\(\\frac{q}{p}\\pi\\) 일 때, \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"},
      {"order":6, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/geo29p.JPG"}
    ]'::jsonb,
    29,
    null,
    127,
    'GEO',
    4,
    'FRQ'
)
     ,
(
    ((SELECT id FROM exam)),
    '[
      {"order":1,"type":"TEXT","content":"좌표평면 위에 두 점 \\(\\mathrm{A}(-2,2),\\ \\mathrm{B}(2,2)\\) 가 있다."},
      {"order":2,"type":"TEXT","content":"\\[ (|\\overrightarrow{\\mathrm{AX}}|-2)(|\\overrightarrow{\\mathrm{BX}}|-2)=0, \\quad |\\overrightarrow{\\mathrm{OX}}| \\geq 2 \\]"},
      {"order":3,"type":"TEXT","content":"를 만족시키는 점 X 가 나타내는 도형 위를 움직이는 두 점 \\(\\mathrm{P},\\ \\mathrm{Q}\\) 가 다음 조건을 만족시킨다."},
      {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m09/geo30p.JPG"},
      {"order":5,"type":"TEXT","content":"\\(\\overrightarrow{\\mathrm{OY}}=\\overrightarrow{\\mathrm{OP}}+\\overrightarrow{\\mathrm{OQ}}\\) 를 만족시키는 점 Y 의 집합이 나타내는 도형의 길이가 \\(\\frac{q}{p}\\sqrt{3}\\pi\\) 일 때, \\(p+q\\) 의 값을 구하시오. (단, O 는 원점이고, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
    ]'::jsonb,
    30,
    null,
    17,
    'GEO',
    4,
    'FRQ'
)
;




