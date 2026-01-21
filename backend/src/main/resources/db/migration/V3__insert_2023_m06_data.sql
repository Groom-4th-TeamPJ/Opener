insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2023
       , 'M06'
       , '6월 모의평가'
       , 30
       , 6000);


WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2023 AND exam_type = 'M06' LIMIT 1
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
            {"order": 1, "type": "TEXT", "content":"\\((-\\sqrt{2})^4 \\times 8^{-\\frac{2}{3}}\\) 의 값은?"}
        ]'::jsonb
        , 1
        ,'[
            {"order": 1, "content":"1"},
            {"order": 2, "content":"2"},
            {"order": 3, "content":"3"},
            {"order": 4, "content":"4"},
            {"order": 5, "content":"5"}
        ]'::jsonb
        , 1
        , 'ALG'
        , 2
        , 'MCQ'
    ),
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)=x^3+9\\) 에 대하여 \\(\\lim _{h \\rightarrow 0} \\frac{f(2+h)-f(2)}{h}\\) 의 값은?"}
       ]'::jsonb
       ,2
       ,'[
         {"order": 1, "content":"11"},
         {"order": 2, "content":"12"},
         {"order": 3, "content":"13"},
         {"order": 4, "content":"14"},
         {"order": 5, "content":"15"}
       ]'::jsonb
       ,2
       , 'ALG'
       , 2
       , 'MCQ'
       )

        , ((SELECT id FROM exam)
          , '[
  {"order": 1, "type": "TEXT", "content":"\\(\\frac{\\pi}{2}<\\theta<\\pi\\) 인 \\(\\theta\\) 에 대하여 \\(\\cos ^2 \\theta=\\frac{4}{9}\\) 일 때, \\(\\sin ^2 \\theta+\\cos \\theta\\) 의 값은?"}
]'::jsonb
          ,3
          ,'[
  {"order": 1, "content":"\\(-\\frac{4}{9}\\)"},
  {"order": 2, "content":"\\(-\\frac{1}{3}\\)"},
  {"order": 3, "content":"\\(-\\frac{2}{9}\\)"},
  {"order": 4, "content":"\\(-\\frac{1}{9}\\)"},
  {"order": 5, "content":"0"}
]'::jsonb
          ,4
          , 'ALG'
          , 3
          , 'MCQ'
       )
        , ((SELECT id FROM exam)
          , '[
  {"order": 1, "type": "TEXT", "content":"함수 \\(y=f(x)\\) 의 그래프가 그림과 같다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/alg4p.JPG"},
  {"order": 3, "type": "TEXT", "content":"\\(\\lim _{x \\rightarrow 0-} f(x)+\\lim _{x \\rightarrow 1+} f(x)\\) 의 값은?"}
]'::jsonb
          ,4
          ,'[
  {"order": 1, "content":"-2"},
  {"order": 2, "content":"-1"},
  {"order": 3, "content":"0"},
  {"order": 4, "content":"1"},
  {"order": 5, "content":"2"}
]'::jsonb
          ,2
          , 'ALG'
          , 3
          , 'MCQ'
       )

        , ((SELECT id FROM exam)
          , '[
  {"order": 1, "type": "TEXT", "content":"모든 항이 양수인 등비수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여"},
  {"order": 2, "type": "TEXT", "content":"\\[ a_1=\\frac{1}{4}, \\quad a_2+a_3=\\frac{3}{2} \\]"},
  {"order": 3, "type": "TEXT", "content":"일 때, \\(a_6+a_7\\) 의 값은?"}
]'::jsonb
          ,5
          ,'[
  {"order": 1, "content":"16"},
  {"order": 2, "content":"20"},
  {"order": 3, "content":"24"},
  {"order": 4, "content":"28"},
  {"order": 5, "content":"32"}
]'::jsonb
          ,3
          , 'ALG'
          , 3
          , 'MCQ'
       ),
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"두 양수 \\(a, b\\) 에 대하여 함수 \\(f(x)\\) 가"},
         {"order": 2, "type": "TEXT", "content":"\\[ f(x)= \\begin{cases}x+a & (x<-1) \\\\ x & (-1 \\leq x<3) \\\\ b x-2 & (x \\geq 3)\\end{cases} \\]"},
         {"order": 3, "type": "TEXT", "content":"이다. 함수 \\(|f(x)|\\) 가 실수 전체의 집합에서 연속일 때, \\(a+b\\) 의 값은?"}
       ]'::jsonb
       ,6
       ,'[
         {"order": 1, "content":"\\(\\frac{7}{3}\\)"},
         {"order": 2, "content":"\\(\\frac{8}{3}\\)"},
         {"order": 3, "content":"3"},
         {"order": 4, "content":"\\(\\frac{10}{3}\\)"},
         {"order": 5, "content":"\\(\\frac{11}{3}\\)"}
       ]'::jsonb
       ,5
       , 'ALG'
       , 3
       , 'MCQ'
       )


        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"닫힌구간 \\([0, \\pi]\\) 에서 정의된 함수 \\(f(x)=-\\sin 2 x\\) 가 \\(x=a\\) 에서 최댓값을 갖고 \\(x=b\\) 에서 최솟값을 갖는다."},
         {"order": 2, "type": "TEXT", "content":"곡선 \\(y=f(x)\\) 위의 두 점 \\((a, f(a)),(b, f(b))\\) 를 지나는 직선의 기울기는?"}
       ]'::jsonb
       ,7
       ,'[
         {"order": 1, "content":"\\(\\frac{1}{\\pi}\\)"},
         {"order": 2, "content":"\\(\\frac{2}{\\pi}\\)"},
         {"order": 3, "content":"\\(\\frac{3}{\\pi}\\)"},
         {"order": 4, "content":"\\(\\frac{4}{\\pi}\\)"},
         {"order": 5, "content":"\\(\\frac{5}{\\pi}\\)"}
       ]'::jsonb
       ,4
       , 'ALG'
       , 3
       , 'MCQ'
       )
    ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"실수 전체의 집합에서 미분가능하고 다음 조건을 만족시키는 모든 함수 \\(f(x)\\) 에 대하여 \\(f(5)\\) 의 최솟값은?"},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/alg8p.JPG"}
       ]'::jsonb
       ,8
       ,'[
         {"order": 1, "content":"21"},
         {"order": 2, "content":"22"},
         {"order": 3, "content":"23"},
         {"order": 4, "content":"24"},
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
         {"order": 1, "type": "TEXT", "content":"두 함수"},
         {"order": 2, "type": "TEXT", "content":"\\[ f(x)=x^3-x+6, \\quad g(x)=x^2+a \\]"},
         {"order": 3, "type": "TEXT", "content":"가 있다. \\(x \\geq 0\\) 인 모든 실수 \\(x\\) 에 대하여 부등식"},
         {"order": 4, "type": "TEXT", "content":"\\[ f(x) \\geq g(x) \\]"},
         {"order": 5, "type": "TEXT", "content":"가 성립할 때, 실수 \\(a\\) 의 최댓값은?"}
       ]'::jsonb
       ,9
       ,'[
         {"order": 1, "content":"1"},
         {"order": 2, "content":"2"},
         {"order": 3, "content":"3"},
         {"order": 4, "content":"4"},
         {"order": 5, "content":"5"}
       ]'::jsonb
       ,5
       , 'ALG'
       , 4
       , 'MCQ'
       )
        ,((SELECT id FROM exam)
         , '[
  {"order": 1, "type": "TEXT", "content":"그림과 같이 \\(\\overline{\\mathrm{AB}}=3, \\overline{\\mathrm{BC}}=2, \\overline{\\mathrm{AC}}>3\\) 이고 \\(\\cos (\\angle \\mathrm{BAC})=\\frac{7}{8}\\) 인 삼각형 ABC 가 있다."},
  {"order": 2, "type": "TEXT", "content":"선분 AC 의 중점을 M , 삼각형 ABC 의 외접원이 직선 BM 과 만나는 점 중 B 가 아닌 점을 D 라 할 때, 선분 MD 의 길이는?"},
  {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/alg10p.JPG"}
]'::jsonb
         ,10
         ,'[
  {"order": 1, "content":"\\(\\frac{3 \\sqrt{10}}{5}\\)"},
  {"order": 2, "content":"\\(\\frac{7 \\sqrt{10}}{10}\\)"},
  {"order": 3, "content":"\\(\\frac{4 \\sqrt{10}}{5}\\)"},
  {"order": 4, "content":"\\(\\frac{9 \\sqrt{10}}{10}\\)"},
  {"order": 5, "content":"\\(\\sqrt{10}\\)"}
]'::jsonb
         ,3
         , 'ALG'
         , 4
         , 'MCQ'
       )

        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"시각 \\(t=0\\) 일 때 동시에 원점을 출발하여 수직선 위를 움직이는 두 점 \\(\\mathrm{P}, \\mathrm{Q}\\) 의 시각 \\(t(t \\geq 0)\\) 에서의 속도가 각각"},
         {"order": 2, "type": "TEXT", "content":"\\[ v_1(t)=2-t, \\quad v_2(t)=3 t \\]"},
         {"order": 3, "type": "TEXT", "content":"이다. 출발한 시각부터 점 P 가 원점으로 돌아올 때까지 점 Q 가 움직인 거리는?"}
       ]'::jsonb
       ,11
       ,'[
         {"order": 1, "content":"16"},
         {"order": 2, "content":"18"},
         {"order": 3, "content":"20"},
         {"order": 4, "content":"22"},
         {"order": 5, "content":"24"}
       ]'::jsonb
       ,5
       , 'ALG'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"공차가 3 인 등차수열 \\(\\left\\{a_n\\right\\}\\) 이 다음 조건을 만족시킬 때, \\(a_{10}\\) 의 값은?"},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/alg12p.JPG"}
       ]'::jsonb
       ,12
       ,'[
         {"order": 1, "content":"\\(\\frac{21}{2}\\)"},
         {"order": 2, "content":"11"},
         {"order": 3, "content":"\\(\\frac{23}{2}\\)"},
         {"order": 4, "content":"12"},
         {"order": 5, "content":"\\(\\frac{25}{2}\\)"}
       ]'::jsonb
       ,3
       , 'ALG'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"두 곡선 \\(y=16^x, y=2^x\\) 과 한 점 \\(\\mathrm{A}\\left(64,2^{64}\\right)\\) 이 있다. 점 A 를 지나며 \\(x\\) 축과 평행한 직선이 곡선 \\(y=16^x\\) 과 만나는 점을 \\(\\mathrm{P}_1\\) 이라 하고, 점 \\(\\mathrm{P}_1\\) 을 지나며 \\(y\\) 축과 평행한 직선이 곡선 \\(y=2^x\\) 과 만나는 점을 \\(\\mathrm{Q}_1\\) 이라 하자."},
         {"order": 2, "type": "TEXT", "content":"점 \\(\\mathrm{Q}_1\\) 을 지나며 \\(x\\) 축과 평행한 직선이 곡선 \\(y=16^x\\) 과 만나는 점을 \\(\\mathrm{P}_2\\) 라 하고, 점 \\(\\mathrm{P}_2\\) 를 지나며 \\(y\\) 축과 평행한 직선이 곡선 \\(y=2^x\\) 과 만나는 점을 \\(\\mathrm{Q}_2\\) 라 하자."},
         {"order": 3, "type": "TEXT", "content":"이와 같은 과정을 계속하여 \\(n\\) 번째 얻은 두 점을 각각 \\(\\mathrm{P}_n, \\mathrm{Q}_n\\) 이라 하고 점 \\(\\mathrm{Q}_n\\) 의 \\(x\\) 좌표를 \\(x_n\\) 이라 할 때, \\(x_n<\\frac{1}{k}\\) 을 만족시키는 \\(n\\) 의 최솟값이 6 이 되도록 하는 자연수 \\(k\\) 의 개수는?"},
         {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/alg13p.JPG"}
]'::jsonb
       ,13
       ,'[
         {"order": 1, "content":"48"},
         {"order": 2, "content":"51"},
         {"order": 3, "content":"54"},
         {"order": 4, "content":"57"},
         {"order": 5, "content":"60"}
       ]'::jsonb
       ,1
       , 'ALG'
       , 4
       , 'MCQ'
       )

        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"실수 전체의 집합에서 연속인 함수 \\(f(x)\\) 와 최고차항의 계수가 1 인 삼차함수 \\(g(x)\\) 가"},
         {"order": 2, "type": "TEXT", "content":"\\[ g(x)= \\begin{cases}-\\int_0^x f(t) d t & (x<0) \\\\ \\int_0^x f(t) d t & (x \\geq 0)\\end{cases} \\]"},
         {"order": 3, "type": "TEXT", "content":"을 만족시킬 때, <보기>에서 옳은 것만을 있는 대로 고른 것은?"},
         {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/alg14p.JPG"}
       ]'::jsonb
       ,14
       ,'[
         {"order": 1, "content":"ᄀ"},
         {"order": 2, "content":"ᄃ"},
         {"order": 3, "content":"ᄀ, ᄂ"},
         {"order": 4, "content":"ᄀ, ᄃ"},
         {"order": 5, "content":"ᄀ, ᄂ, ᄃ"}
       ]'::jsonb
       ,4
       , 'ALG'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"자연수 \\(k\\) 에 대하여 다음 조건을 만족시키는 수열 \\(\\left\\{a_n\\right\\}\\) 이 있다."},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/alg4p.JPG"},
         {"order": 3, "type": "TEXT", "content":"\\(a_{22}=0\\) 이 되도록 하는 모든 \\(k\\) 의 값의 합은?"}
       ]'::jsonb
       ,15
       ,'[
         {"order": 1, "content":"12"},
         {"order": 2, "content":"14"},
         {"order": 3, "content":"16"},
         {"order": 4, "content":"18"},
         {"order": 5, "content":"20"}
       ]'::jsonb
       ,2
       , 'ALG'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"방정식 \\(\\log _2(x+2)+\\log _2(x-2)=5\\) 를 만족시키는 실수 \\(x\\) 의 값을 구하시오."}
       ]'::jsonb
       ,16
       ,null
       ,6
       , 'ALG'
       , 3
       , 'FRQ'
       )
,

       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)\\) 에 대하여 \\(f^{\\prime}(x)=8 x^3+6 x^2\\) 이고 \\(f(0)=-1\\) 일 때, \\(f(-2)\\) 의 값을 구하시오."}
       ]'::jsonb
       ,17
       ,null
       ,15
       , 'ALG'
       , 3
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"\\(\\sum_{k=1}^{10}(4 k+a)=250\\) 일 때, 상수 \\(a\\) 의 값을 구하시오."}
       ]'::jsonb
       ,18
       ,null
       ,3
       , 'ALG'
       , 3
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)=x^4+a x^2+b\\) 는 \\(x=1\\) 에서 극소이다. 함수 \\(f(x)\\) 의 극댓값이 4 일 때, \\(a+b\\) 의 값을 구하시오. (단, \\(a\\) 와 \\(b\\) 는 상수이다.)"}
       ]'::jsonb
       ,19
       ,null
       ,2
       , 'ALG'
       , 3
       , 'FRQ'
       )
,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"최고차항의 계수가 2 인 이차함수 \\(f(x)\\) 에 대하여 함수 \\(g(x)=\\int_x^{x+1}|f(t)| d t\\) 는 \\(x=1\\) 과 \\(x=4\\) 에서 극소이다. \\(f(0)\\) 의 값을 구하시오."}
       ]'::jsonb
       ,20
       ,null
       ,13
       , 'ALG'
       , 4
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"자연수 \\(n\\) 에 대하여 \\(4 \\log _{64}\\left(\\frac{3}{4 n+16}\\right)\\) 의 값이 정수가 되도록 하는 1000 이하의 모든 \\(n\\) 의 값의 합을 구하시오."}
       ]'::jsonb
       ,21
       ,null
       ,426
       , 'ALG'
       , 4
       , 'FRQ'
       )
        ,

       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"두 양수 \\(a, b(b>3)\\) 과 최고차항의 계수가 1 인 이차함수 \\(f(x)\\) 에 대하여 함수"},
         {"order": 2, "type": "TEXT", "content":"\\[ g(x)= \\begin{cases}(x+3) f(x) & (x<0) \\\\ (x+a) f(x-b) & (x \\geq 0)\\end{cases} \\]"},
         {"order": 3, "type": "TEXT", "content":"이 실수 전체의 집합에서 연속이고 다음 조건을 만족시킬 때, \\(g(4)\\) 의 값을 구하시오."},
         {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/alg22p.JPG"}
       ]'::jsonb
       ,22
       ,null
       ,19
       , 'ALG'
       , 4
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"5 개의 문자 \\(a, a, a, b, c\\) 를 모두 일렬로 나열하는 경우의 수는?"}
       ]'::jsonb
       ,23
       ,'[
         {"order": 1, "content":"16"},
         {"order": 2, "content":"20"},
         {"order": 3, "content":"24"},
         {"order": 4, "content":"28"},
         {"order": 5, "content":"32"}
       ]'::jsonb
       ,2
       , 'PROB'
       , 2
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"주머니 A 에는 1 부터 3 까지의 자연수가 하나씩 적혀 있는 3 장의 카드가 들어 있고, 주머니 B 에는 1 부터 5 까지의 자연수가 하나씩 적혀 있는 5 장의 카드가 들어 있다."},
         {"order": 2, "type": "TEXT", "content":"두 주머니 \\(\\mathrm{A}, \\mathrm{B}\\) 에서 각각 카드를 임의로 한 장씩 꺼낼 때, 꺼낸 두 장의 카드에 적힌 수의 차가 1 일 확률은?"},
         {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/prob24p.JPG"}
       ]'::jsonb
       ,24
       ,'[
         {"order": 1, "content":"\\(\\frac{1}{3}\\)"},
         {"order": 2, "content":"\\(\\frac{2}{5}\\)"},
         {"order": 3, "content":"\\(\\frac{7}{15}\\)"},
         {"order": 4, "content":"\\(\\frac{8}{15}\\)"},
         {"order": 5, "content":"\\(\\frac{3}{5}\\)"}
       ]'::jsonb
       ,1
       , 'PROB'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"수직선의 원점에 점 P 가 있다. 한 개의 주사위를 사용하여 다음 시행을 한다."},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/prob25p.JPG"},
         {"order": 3, "type": "TEXT", "content":"이 시행을 4 번 반복할 때, 4 번째 시행 후 점 P 의 좌표가 2 이상일 확률은?"}
       ]'::jsonb
       ,25
       ,'[
         {"order": 1, "content":"\\(\\frac{13}{18}\\)"},
         {"order": 2, "content":"\\(\\frac{7}{9}\\)"},
         {"order": 3, "content":"\\(\\frac{5}{6}\\)"},
         {"order": 4, "content":"\\(\\frac{8}{9}\\)"},
         {"order": 5, "content":"\\(\\frac{17}{18}\\)"}
       ]'::jsonb
       ,4
       , 'PROB'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"다항식 \\(\\left(x^2+1\\right)^4\\left(x^3+1\\right)^n\\) 의 전개식에서 \\(x^5\\) 의 계수가 12 일 때, \\(x^6\\) 의 계수는? (단, \\(n\\) 은 자연수이다.)"}
       ]'::jsonb
       ,26
       ,'[
         {"order": 1, "content":"6"},
         {"order": 2, "content":"7"},
         {"order": 3, "content":"8"},
         {"order": 4, "content":"9"},
         {"order": 5, "content":"10"}
       ]'::jsonb
       ,2
       , 'PROB'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"네 문자 \\(a, b, X, Y\\) 중에서 중복을 허락하여 6 개를 택해 일렬로 나열하려고 한다. 다음 조건이 성립하도록 나열하는 경우의 수는?"},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/prob27p.JPG"}
       ]'::jsonb
       ,27
       ,'[
         {"order": 1, "content":"384"},
         {"order": 2, "content":"408"},
         {"order": 3, "content":"432"},
         {"order": 4, "content":"456"},
         {"order": 5, "content":"480"}
       ]'::jsonb
       ,3
       , 'PROB'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"숫자 \\(1,2,3,4,5\\) 중에서 서로 다른 4 개를 택해 일렬로 나열하여 만들 수 있는 모든 네 자리의 자연수 중에서 임의로 하나의 수를 택할 때, 택한 수가 5 의 배수 또는 3500 이상일 확률은?"}
       ]'::jsonb
       ,28
       ,'[
         {"order": 1, "content":"\\(\\frac{9}{20}\\)"},
         {"order": 2, "content":"\\(\\frac{1}{2}\\)"},
         {"order": 3, "content":"\\(\\frac{11}{20}\\)"},
         {"order": 4, "content":"\\(\\frac{3}{5}\\)"},
         {"order": 5, "content":"\\(\\frac{13}{20}\\)"}
       ]'::jsonb
       ,4
       , 'PROB'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"집합 \\(X=\\{1,2,3,4,5\\}\\) 에 대하여 다음 조건을 만족시키는 함수 \\(f: X \\rightarrow X\\) 의 개수를 구하시오."},
         {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/prob29p.JPG"}
       ]'::jsonb
       ,29
       ,null
       ,115
       , 'PROB'
       , 4
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"주머니에 1 부터 12 까지의 자연수가 각각 하나씩 적혀 있는 12 개의 공이 들어 있다. 이 주머니에서 임의로 3 개의 공을 동시에 꺼내어 공에 적혀 있는 수를 작은 수부터 크기 순서대로 \\(a, b, c\\) 라 하자."},
         {"order": 2, "type": "TEXT", "content":"\\(b-a \\geq 5\\) 일 때, \\(c-a \\geq 10\\) 일 확률은 \\(\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
       ]'::jsonb
       ,30
       ,null
       ,9
       , 'PROB'
       , 4
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"\\(\\lim _{n \\rightarrow \\infty} \\frac{1}{\\sqrt{n^2+3 n}-\\sqrt{n^2+n}}\\) 의 값은?"}
       ]'::jsonb
       ,23
       ,'[
         {"order": 1, "content":"1"},
         {"order": 2, "content":"\\(\\frac{3}{2}\\)"},
         {"order": 3, "content":"2"},
         {"order": 4, "content":"\\(\\frac{5}{2}\\)"},
         {"order": 5, "content":"3"}
       ]'::jsonb
       ,1
       , 'CALC'
       , 2
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"곡선 \\(x^2-y \\ln x+x=e\\) 위의 점 \\(\\left(e, e^2\\right)\\) 에서의 접선의 기울기는?"}
       ]'::jsonb
       ,24
       ,'[
         {"order": 1, "content":"\\(e+1\\)"},
         {"order": 2, "content":"\\(e+2\\)"},
         {"order": 3, "content":"\\(e+3\\)"},
         {"order": 4, "content":"\\(2 e+1\\)"},
         {"order": 5, "content":"\\(2 e+2\\)"}
       ]'::jsonb
       ,1
       , 'CALC'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)=x^3+2 x+3\\) 의 역함수를 \\(g(x)\\) 라 할 때, \\(g^{\\prime}(3)\\) 의 값은?"}
       ]'::jsonb
       ,25
       ,'[
         {"order": 1, "content":"1"},
         {"order": 2, "content":"\\(\\frac{1}{2}\\)"},
         {"order": 3, "content":"\\(\\frac{1}{3}\\)"},
         {"order": 4, "content":"\\(\\frac{1}{4}\\)"},
         {"order": 5, "content":"\\(\\frac{1}{5}\\)"}
       ]'::jsonb
       ,2
       , 'CALC'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"그림과 같이 \\(\\overline{\\mathrm{A}_1 \\mathrm{~B}_1}=2, \\overline{\\mathrm{~B}_1 \\mathrm{~A}_2}=3\\) 이고 \\(\\angle \\mathrm{A}_1 \\mathrm{~B}_1 \\mathrm{~A}_2=\\frac{\\pi}{3}\\) 인 삼각형 \\(\\mathrm{A}_1 \\mathrm{~A}_2 \\mathrm{~B}_1\\) 과 이 삼각형의 외접원 \\(O_1\\) 이 있다."},
         {"order": 2, "type": "TEXT", "content":"점 \\(\\mathrm{A}_2\\) 를 지나고 직선 \\(\\mathrm{A}_1 \\mathrm{~B}_1\\) 에 평행한 직선이 원 \\(O_1\\) 과 만나는 점 중 \\(\\mathrm{A}_2\\) 가 아닌 점을 \\(\\mathrm{B}_2\\) 라 하자. 두 선분 \\(\\mathrm{A}_1 \\mathrm{~B}_2, \\mathrm{~B}_1 \\mathrm{~A}_2\\) 가 만나는 점을 \\(\\mathrm{C}_1\\) 이라 할 때, 두 삼각형 \\(\\mathrm{A}_1 \\mathrm{~A}_2 \\mathrm{C}_1, \\mathrm{~B}_1 \\mathrm{C}_1 \\mathrm{~B}_2\\) 로 만들어진 모양의 도형에 색칠하여 얻은 그림을 \\(R_1\\) 이라 하자."},
         {"order": 3, "type": "TEXT", "content":"그림 \\(R_1\\) 에서 점 \\(\\mathrm{B}_2\\) 를 지나고 직선 \\(\\mathrm{B}_1 \\mathrm{~A}_2\\) 에 평행한 직선이 직선 \\(\\mathrm{A}_1 \\mathrm{~A}_2\\) 와 만나는 점을 \\(\\mathrm{A}_3\\) 이라 할 때, 삼각형 \\(\\mathrm{A}_2 \\mathrm{~A}_3 \\mathrm{~B}_2\\) 의 외접원을 \\(O_2\\) 라 하자. 그림 \\(R_1\\) 을 얻은 것과 같은 방법으로 두 점 \\(\\mathrm{B}_3, \\mathrm{C}_2\\) 를 잡아 원 \\(O_2\\) 에 모양의 도형을 그리고 색칠하여 얻은 그림을 \\(R_2\\) 라 하자."},
         {"order": 4, "type": "TEXT", "content":"이와 같은 과정을 계속하여 \\(n\\) 번째 얻은 그림 \\(R_n\\) 에 색칠되어 있는 부분의 넓이를 \\(S_n\\) 이라 할 때, \\(\\lim _{n \\rightarrow \\infty} S_n\\) 의 값은?"},
         {"order": 5, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/calc26p.JPG"}
       ]'::jsonb
       ,26
       ,'[
         {"order": 1, "content":"\\(\\frac{11 \\sqrt{3}}{9}\\)"},
         {"order": 2, "content":"\\(\\frac{4 \\sqrt{3}}{3}\\)"},
         {"order": 3, "content":"\\(\\frac{13 \\sqrt{3}}{9}\\)"},
         {"order": 4, "content":"\\(\\frac{14 \\sqrt{3}}{9}\\)"},
         {"order": 5, "content":"\\(\\frac{5 \\sqrt{3}}{3}\\)"}
       ]'::jsonb
       ,2
       , 'CALC'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"첫째항이 4 인 등차수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 급수"},
         {"order": 2, "type": "TEXT", "content":"\\[ \\sum_{n=1}^{\\infty}\\left(\\frac{a_n}{n}-\\frac{3 n+7}{n+2}\\right) \\]"},
         {"order": 3, "type": "TEXT", "content":"이 실수 \\(S\\) 에 수렴할 때, \\(S\\) 의 값은?"}
       ]'::jsonb
       ,27
       ,'[
         {"order": 1, "content":"\\(\\frac{1}{2}\\)"},
         {"order": 2, "content":"1"},
         {"order": 3, "content":"\\(\\frac{3}{2}\\)"},
         {"order": 4, "content":"2"},
         {"order": 5, "content":"\\(\\frac{5}{2}\\)"}
       ]'::jsonb
       ,3
       , 'CALC'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"최고차항의 계수가 \\(\\frac{1}{2}\\) 인 삼차함수 \\(f(x)\\) 에 대하여 함수 \\(g(x)\\) 가"},
         {"order": 2, "type": "TEXT", "content":"\\[ g(x)= \\begin{cases}\\ln |f(x)| & (f(x) \\neq 0) \\\\ 1 & (f(x)=0)\\end{cases} \\]"},
         {"order": 3, "type": "TEXT", "content":"이고 다음 조건을 만족시킬 때, 함수 \\(g(x)\\) 의 극솟값은?"},
         {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/calc28p.JPG"}
       ]'::jsonb
       ,28
       ,'[
         {"order": 1, "content":"\\(\\ln \\frac{13}{27}\\)"},
         {"order": 2, "content":"\\(\\ln \\frac{16}{27}\\)"},
         {"order": 3, "content":"\\(\\ln \\frac{19}{27}\\)"},
         {"order": 4, "content":"\\(\\ln \\frac{22}{27}\\)"},
         {"order": 5, "content":"\\(\\ln \\frac{25}{27}\\)"}
       ]'::jsonb
       ,5
       , 'CALC'
       , 4
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"그림과 같이 반지름의 길이가 1 이고 중심각의 크기가 \\(\\frac{\\pi}{2}\\) 인 부채꼴 OAB 가 있다. 호 AB 위의 점 P 에서 선분 OA 에 내린 수선의 발을 H 라 하고, \\(\\angle \\mathrm{OAP}\\) 를 이등분하는 직선과 세 선분 \\(\\mathrm{HP}, \\mathrm{OP}, \\mathrm{OB}\\) 의 교점을 각각 \\(\\mathrm{Q}, \\mathrm{R}, \\mathrm{S}\\) 라 하자."},
         {"order": 2, "type": "TEXT", "content":"\\(\\angle \\mathrm{APH}=\\theta\\) 일 때, 삼각형 AQH 의 넓이를 \\(f(\\theta)\\), 삼각형 PSR 의 넓이를 \\(g(\\theta)\\) 라 하자. \\(\\lim _{\\theta \\rightarrow 0+} \\frac{\\theta^3 \\times g(\\theta)}{f(\\theta)}=k\\) 일 때, \\(100 k\\) 의 값을 구하시오. (단, \\(0<\\theta<\\frac{\\pi}{4}\\) )"},
         {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/calc29p.JPG"}
       ]'::jsonb
       ,29
       ,null
       ,50
       , 'CALC'
       , 4
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"양수 \\(a\\) 에 대하여 함수 \\(f(x)\\) 는"},
         {"order": 2, "type": "TEXT", "content":"\\[ f(x)=\\frac{x^2-a x}{e^x} \\]"},
         {"order": 3, "type": "TEXT", "content":"이다. 실수 \\(t\\) 에 대하여 \\(x\\) 에 대한 방정식"},
         {"order": 4, "type": "TEXT", "content":"\\[ f(x)=f^{\\prime}(t)(x-t)+f(t) \\]"},
         {"order": 5, "type": "TEXT", "content":"의 서로 다른 실근의 개수를 \\(g(t)\\) 라 하자."},
         {"order": 6, "type": "TEXT", "content":"\\(g(5)+\\lim _{t \\rightarrow 5} g(t)=5\\) 일 때, \\(\\lim _{t \\rightarrow k-} g(t) \\neq \\lim _{t \\rightarrow k+} g(t)\\) 를 만족시키는 모든 실수 \\(k\\) 의 값의 합은 \\(\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오."},
         {"order": 7, "type": "TEXT", "content":"(단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
       ]'::jsonb
       ,30
       ,null
       ,16
       , 'CALC'
       , 4
       , 'FRQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"서로 평행하지 않은 두 벡터 \\(\\vec{a}, \\vec{b}\\) 에 대하여 두 벡터"},
         {"order": 2, "type": "TEXT", "content":"\\[ \\vec{a}+2 \\vec{b}, \\quad 3 \\vec{a}+k \\vec{b} \\]"},
         {"order": 3, "type": "TEXT", "content":"가 서로 평행하도록 하는 실수 \\(k\\) 의 값은? (단, \\(\\vec{a} \\neq \\overrightarrow{0}, \\vec{b} \\neq \\overrightarrow{0}\\) )"}
       ]'::jsonb
       ,23
       ,'[
         {"order": 1, "content":"2"},
         {"order": 2, "content":"4"},
         {"order": 3, "content":"6"},
         {"order": 4, "content":"8"},
         {"order": 5, "content":"10"}
       ]'::jsonb
       ,3
       , 'GEO'
       , 2
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"쌍곡선 \\(\\frac{x^2}{a^2}-\\frac{y^2}{b^2}=1\\) 의 주축의 길이가 6 이고 한 점근선의 방정식이 \\(y=2 x\\) 일 때, 두 초점 사이의 거리는? (단, \\(a\\) 와 \\(b\\) 는 양수이다.)"}
       ]'::jsonb
       ,24
       ,'[
         {"order": 1, "content":"\\(4 \\sqrt{5}\\)"},
         {"order": 2, "content":"\\(6 \\sqrt{5}\\)"},
         {"order": 3, "content":"\\(8 \\sqrt{5}\\)"},
         {"order": 4, "content":"\\(10 \\sqrt{5}\\)"},
         {"order": 5, "content":"\\(12 \\sqrt{5}\\)"}
       ]'::jsonb
       ,2
       , 'GEO'
       , 3
       , 'MCQ'
       )
        ,
       ((SELECT id FROM exam)
       , '[
         {"order": 1, "type": "TEXT", "content":"좌표평면에서 두 직선"},
         {"order": 2, "type": "TEXT", "content":"\\[ \\frac{x-3}{4}=\\frac{y-5}{3}, \\quad x-1=\\frac{2-y}{3} \\]"},
         {"order": 3, "type": "TEXT", "content":"가 이루는 예각의 크기를 \\(\\theta\\) 라 할 때, \\(\\cos \\theta\\) 의 값은?"}
       ]'::jsonb
       ,25
       ,'[
         {"order": 1, "content":"\\(\\frac{\\sqrt{11}}{11}\\)"},
         {"order": 2, "content":"\\(\\frac{\\sqrt{10}}{10}\\)"},
         {"order": 3, "content":"\\(\\frac{1}{3}\\)"},
         {"order": 4, "content":"\\(\\frac{\\sqrt{2}}{4}\\)"},
         {"order": 5, "content":"\\(\\frac{\\sqrt{7}}{7}\\)"}
       ]'::jsonb
       ,2
       , 'GEO'
       , 3
       , 'MCQ'
       )
        , ((SELECT id FROM exam)
          , '[
  {"order": 1, "type": "TEXT", "content":"좌표평면에서 타원 \\(\\frac{x^2}{3}+y^2=1\\) 과 직선 \\(y=x-1\\) 이 만나는 두 점을 \\(\\mathrm{A}, \\mathrm{C}\\) 라 하자."},
  {"order": 2, "type": "TEXT", "content":"선분 AC 가 사각형 ABCD 의 대각선이 되도록 타원 위에 두 점 \\(\\mathrm{B}, \\mathrm{D}\\) 를 잡을 때, 사각형 ABCD 의 넓이의 최댓값은?"},
  {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/geo26p.JPG"}
]'::jsonb
          ,26
          ,'[
  {"order": 1, "content":"2"},
  {"order": 2, "content":"\\(\\frac{9}{4}\\)"},
  {"order": 3, "content":"\\(\\frac{5}{2}\\)"},
  {"order": 4, "content":"\\(\\frac{11}{4}\\)"},
  {"order": 5, "content":"3"}
]'::jsonb
          ,5
          , 'GEO'
          , 3
          , 'MCQ'
       )
        ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"\\(\\overline{\\mathrm{AD}}=2, \\overline{\\mathrm{AB}}=\\overline{\\mathrm{CD}}=\\sqrt{2}, \\angle \\mathrm{ABC}=\\angle \\mathrm{BCD}=45^{\\circ}\\) 인 사다리꼴 ABCD 가 있다."},
  {"order": 2, "type": "TEXT", "content":"두 대각선 AC 와 BD 의 교점을 E , 점 A 에서 선분 BC 에 내린 수선의 발을 H , 선분 AH 와 선분 BD 의 교점을 F 라 할 때, \\(\\overrightarrow{\\mathrm{AF}} \\cdot \\overrightarrow{\\mathrm{CE}}\\) 의 값은?"},
  {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/geo27p.JPG"}
]'::jsonb
,27
,'[
  {"order": 1, "content":"\\(-\\frac{1}{9}\\)"},
  {"order": 2, "content":"\\(-\\frac{2}{9}\\)"},
  {"order": 3, "content":"\\(-\\frac{1}{3}\\)"},
  {"order": 4, "content":"\\(-\\frac{4}{9}\\)"},
  {"order": 5, "content":"\\(-\\frac{5}{9}\\)"}
]'::jsonb
,4
, 'GEO'
, 3
, 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"좌표평면에서 직선 \\(y=2 x-3\\) 위를 움직이는 점 P 가 있다."},
  {"order": 2, "type": "TEXT", "content":"두 점 \\(\\mathrm{A}(c, 0), \\mathrm{B}(-c, 0)(c>0)\\) 에 대하여 \\(\\overline{\\mathrm{PB}}-\\overline{\\mathrm{PA}}\\) 의 값이 최대가 되도록 하는 점 P 의 좌표가 \\((3,3)\\) 일 때, 상수 \\(c\\) 의 값은?"}
]'::jsonb
,28
,'[
  {"order": 1, "content":"\\(\\frac{3 \\sqrt{6}}{2}\\)"},
  {"order": 2, "content":"\\(\\frac{3 \\sqrt{7}}{2}\\)"},
  {"order": 3, "content":"\\(3 \\sqrt{2}\\)"},
  {"order": 4, "content":"\\(\\frac{9}{2}\\)"},
  {"order": 5, "content":"\\(\\frac{3 \\sqrt{10}}{2}\\)"}
]'::jsonb
,1
, 'GEO'
, 4
, 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"초점이 F 인 포물선 \\(y^2=8 x\\) 위의 점 중 제 1 사분면에 있는 점 P 를 지나고 \\(x\\) 축과 평행한 직선이 포물선 \\(y^2=8 x\\) 의 준선과 만나는 점을 \\(\\mathrm{F}^{\\prime}\\) 이라 하자."},
  {"order": 2, "type": "TEXT", "content":"점 \\(\\mathrm{F}^{\\prime}\\) 을 초점, 점 P 를 꼭짓점으로 하는 포물선이 포물선 \\(y^2=8 x\\) 와 만나는 점 중 P 가 아닌 점을 Q 라 하자."},
  {"order": 3, "type": "TEXT", "content":"사각형 \\(\\mathrm{PF}^{\\prime} \\mathrm{QF}\\) 의 둘레의 길이가 12 일 때, 삼각형 \\(\\mathrm{PF}^{\\prime} \\mathrm{Q}\\) 의 넓이는 \\(\\frac{q}{p} \\sqrt{2}\\) 이다. \\(p+q\\) 의 값을 구하시오."},
  {"order": 4, "type": "TEXT", "content":"(단, 점 P 의 \\(x\\) 좌표는 2 보다 작고, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"},
  {"order": 5, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/geo29p.JPG"}
]'::jsonb
,29
,null
,23
, 'GEO'
, 4
, 'FRQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"좌표평면에서 한 변의 길이가 4 인 정육각형 ABCDEF 의 변 위를 움직이는 점 P 가 있고, 점 C 를 중심으로 하고 반지름의 길이가 1 인 원 위를 움직이는 점 Q 가 있다. 두 점 \\(\\mathrm{P}, \\mathrm{Q}\\) 와 실수 \\(k\\) 에 대하여 점 X 가 다음 조건을 만족시킬 때, \\(|\\overrightarrow{\\mathrm{CX}}|\\) 의 값이 최소가 되도록 하는 \\(k\\) 의 값을 \\(\\alpha\\), \\(|\\overrightarrow{\\mathrm{CX}}|\\) 의 값이 최대가 되도록 하는 \\(k\\) 의 값을 \\(\\beta\\) 라 하자."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/geo30p.JPG"},
  {"order": 3, "type": "TEXT", "content":"\\(\\alpha^2+\\beta^2\\) 의 값을 구하시오."},
  {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/m06/geo30p2.JPG"}

]'::jsonb
,30
,null
,8
, 'GEO'
, 4
, 'FRQ'
)
;




