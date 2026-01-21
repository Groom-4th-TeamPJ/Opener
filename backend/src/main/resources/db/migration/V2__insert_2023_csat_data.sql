insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2023
       , 'CSAT'
       , '수학능력시험'
       , 30
       , 6000);


WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2023 AND exam_type = 'CSAT' LIMIT 1
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
            {"order": 1, "type": "TEXT", "content":"\\(\\left(\\frac{4}{2^{\\sqrt{2}}}\\right)^{2+\\sqrt{2}}\\) 의 값은?"}
        ]'::jsonb
        , 1
        ,'[
            {"order": 1, "content":"\\(\\frac{1}{4}\\)"},
            {"order": 2, "content":"\\(\\frac{1}{2}\\)"},
            {"order": 3, "content":"1"},
            {"order": 4, "content":"2"},
            {"order": 5, "content":"4"}
        ]'::jsonb
        , 5
        , 'ALG'
        , 2
        , 'MCQ'
    ),
    ((select id from exam)
        , '[
      {"order": 1, "type": "TEXT", "content":"\\(\\lim _{x \\rightarrow \\infty} \\frac{\\sqrt{x^2-2}+3x}{x+5}\\) 의 값은?"}
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
        , ((select id from exam)
        , '[
      {"order": 1, "type": "TEXT", "content":"공비가 양수인 등비수열 \\(\\left\\{a_n\\right\\}\\) 이"},
      {"order": 2, "type": "TEXT", "content":"\\[ a_2+a_4=30, \\quad a_4+a_6=\\frac{15}{2} \\]"},
      {"order": 3, "type": "TEXT", "content":"를 만족시킬 때, \\(a_1\\) 의 값은?"}
    ]'::jsonb
        ,3
        ,'[
  {"order": 1, "content":"48"},
{"order": 2, "content":"56"},
{"order": 3, "content":"64"},
{"order": 4, "content":"72"},
{"order": 5, "content":"80"}
]'::jsonb
        ,1
        , 'ALG'
        , 3
        , 'MCQ'
    )
        , ((SELECT id FROM exam)
        , '[
{"order": 1, "type": "TEXT", "content":"다항함수 \\(f(x)\\) 에 대하여 함수 \\(g(x)\\) 를"},
{"order": 2, "type": "TEXT", "content":"[ g(x)=x^2 f(x) \\]"},
{"order": 3, "type": "TEXT", "content":"라 하자. \\(f(2)=1, f^{\\prime}(2)=3\\) 일 때, \\(g^{\\prime}(2)\\) 의 값은?"}
]'::jsonb
        ,4
        ,'[
{"order": 1, "content":"12"},
{"order": 2, "content":"14"},
{"order": 3, "content":"16"},
{"order": 4, "content":"18"},
{"order": 5, "content":"20"}
]'::jsonb
        ,3
        , 'CALC'
        , 3
        , 'MCQ'
    )
        , ((SELECT id FROM exam)
        , '[
{"order": 1, "type": "TEXT", "content":"\\(\\tan \\theta<0\\) 이고 \\(\\cos \\left(\\frac{\\pi}{2}+\\theta\\right\\)=\\frac{\\sqrt{5}}{5}\\) 일 때, \\(\\cos \\theta\\) 의 값은?"}
]'::jsonb
        ,5
        ,'[
{"order": 1, "content":"\\(-\\frac{2 \\sqrt{5}}{5}\\)"},
{"order": 2, "content":"\\(-\\frac{\\sqrt{5}}{5}\\)"},
{"order": 3, "content":"0"},
{"order": 4, "content":"\\(\\frac{\\sqrt{5}}{5}\\)"},
{"order": 5, "content":"\\(\\frac{2 \\sqrt{5}}{5}\\)"}
]'::jsonb
        ,5
        , 'ALG'
        , 3
        , 'MCQ'
    )

        , ((SELECT id FROM exam)
        , '[
{"order": 1, "type": "TEXT", "content":"함수 \\(f(x)=2 x^3-9 x^2+a x+5\\) 는 \\(x=1\\) 에서 극대이고, \\(x=b\\) 에서 극소이다. \\(a+b\\) 의 값은? (단, \\(a, b\\) 는 상수이다.)"}
]'::jsonb
        ,6
        ,'[
{"order": 1, "content":"12"},
{"order": 2, "content":"14"},
{"order": 3, "content":"16"},
{"order": 4, "content":"18"},
{"order": 5, "content":"20"}
]'::jsonb
        ,2
        , 'ALG'
        , 3
        , 'MCQ'
    )

        , ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"모든 항이 양수이고 첫째항과 공차가 같은 등차수열 \\(\\left\\{a_n\\right\\}\\) 이"},
  {"order": 2, "type": "TEXT", "content":"\\[ \\sum_{k=1}^{15} \\frac{1}{\\sqrt{a_k}+\\sqrt{a_{k+1}}}=2 \\]"},
  {"order": 3, "type": "TEXT", "content":"를 만족시킬 때, \\(a_4\\) 의 값은?"}
]'::jsonb
        ,7
        ,'[
  {"order": 1, "content":"6"},
  {"order": 2, "content":"7"},
  {"order": 3, "content":"8"},
  {"order": 4, "content":"9"},
  {"order": 5, "content":"10"}
]'::jsonb
        ,4
        , 'ALG'
        , 3
        , 'MCQ'
    )
        , ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"점 \\((0,4)\\) 에서 곡선 \\(y=x^3-x+2\\) 에 그은 접선의 \\(x\\) 절편은?"}
]'::jsonb
        ,8
        ,'[
  {"order": 1, "content":"\\(-\\frac{1}{2}\\)"},
  {"order": 2, "content":"-1"},
  {"order": 3, "content":"\\(-\\frac{3}{2}\\)"},
  {"order": 4, "content":"-2"},
  {"order": 5, "content":"\\(-\\frac{5}{2}\\)"}
]'::jsonb
        ,4
        , 'ALG'
        , 3
        , 'MCQ'
    )

        , ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"함수"},
  {"order": 2, "type": "TEXT", "content":"\\[ f(x)=a-\\sqrt{3} \\tan 2x \\]"},
  {"order": 3, "type": "TEXT", "content":"가 닫힌구간 \\(\\left[-\\frac{\\pi}{6}, b\\right]\\) 에서 최댓값 7 , 최솟값 3 을 가질 때, \\(a \\times b\\) 의 값은? (단, \\(a, b\\) 는 상수이다.)"}
]'::jsonb
        ,9
        ,'[
  {"order": 1, "content":"\\(\\frac{\\pi}{2}\\)"},
  {"order": 2, "content":"\\(\\frac{5 \\pi}{12}\\)"},
  {"order": 3, "content":"\\(\\frac{\\pi}{3}\\)"},
  {"order": 4, "content":"\\(\\frac{\\pi}{4}\\)"},
  {"order": 5, "content":"\\(\\frac{\\pi}{6}\\)"}
]'::jsonb
        ,3
        , 'ALG'
        , 4
        , 'MCQ'
    ) ,

    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"두 곡선 \\(y=x^3+x^2\\, y=-x^2+k\\) 와 \\(y\\) 축으로 둘러싸인 부분의 넓이를 \\(A\\), 두 곡선 \\(y=x^3+x^2, y=-x^2+k\\) 와 직선 \\(x=2\\) 로 둘러싸인 부분의 넓이를 \\(B\\) 라 하자. \\(A=B\\) 일 때, 상수 \\(k\\) 의 값은? (단, \\(4<k<5\\) )"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/alg10p.JPG"}
]'::jsonb
        ,10
        ,'[
  {"order": 1, "content":"\\(\\frac{25}{6}\\)"},
  {"order": 2, "content":"\\(\\frac{13}{3}\\)"},
  {"order": 3, "content":"\\(\\frac{9}{2}\\)"},
  {"order": 4, "content":"\\(\\frac{14}{3}\\)"},
  {"order": 5, "content":"\\(\\frac{29}{6}\\)"}
]'::jsonb
        , 4
        , 'ALG'
        , 4
        , 'MCQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"그림과 같이 사각형 ABCD 가 한 원에 내접하고"},
  {"order": 2, "type": "TEXT", "content":"\\[ \\overline{\\mathrm{AB}}=5, \\overline{\\mathrm{AC}}=3 \\sqrt{5}, \\overline{\\mathrm{AD}}=7, \\angle \\mathrm{BAC}=\\angle \\mathrm{CAD} \\]"},
  {"order": 3, "type": "TEXT", "content":"일 때, 이 원의 반지름의 길이는?"},
  {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/alg11p.JPG"}
]'::jsonb
        ,11
        ,'[
  {"order": 1, "content":"\\(\\frac{5 \\sqrt{2}}{2}\\)"},
  {"order": 2, "content":"\\(\\frac{8 \\sqrt{5}}{5}\\)"},
  {"order": 3, "content":"\\(\\frac{5 \\sqrt{5}}{3}\\)"},
  {"order": 4, "content":"\\(\\frac{8 \\sqrt{2}}{3}\\)"},
  {"order": 5, "content":"\\(\\frac{9 \\sqrt{3}}{4}\\)"}
]'::jsonb
        ,1
        , 'ALG'
        , 4
        , 'MCQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"실수 전체의 집합에서 연속인 함수 \\(f(x)\\) 가 다음 조건을 만족시킨다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/alg12p.JPG"},
  {"order": 3, "type": "TEXT", "content":"열린구간 \\((0,4)\\) 에서 정의된 함수"},
  {"order": 4, "type": "TEXT", "content":"\\[ g(x)=\\int_0^x f(t) d t-\\int_x^4 f(t) d t \\]"},
  {"order": 5, "type": "TEXT", "content":"가 \\(x=2\\) 에서 최솟값 0 을 가질 때, \\(\\int_{\\frac{1}{2}}^4 f(x) d x\\) 의 값은?"}
]'::jsonb
        ,12
        ,'[
  {"order": 1, "content":"\\(-\\frac{3}{2}\\)"},
  {"order": 2, "content":"\\(-\\frac{1}{2}\\)"},
  {"order": 3, "content":"\\(\\frac{1}{2}\\)"},
  {"order": 4, "content":"\\(\\frac{3}{2}\\)"},
  {"order": 5, "content":"\\(\\frac{5}{2}\\)"}
]'::jsonb
        ,2
        , 'ALG'
        , 4
        , 'MCQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"자연수 \\(m(m \\geq 2)\\) 에 대하여 \\(m^{12}\\) 의 \\(n\\) 제곱근 중에서 정수가 존재하도록 하는 2 이상의 자연수 \\(n\\) 의 개수를 \\(f(m)\\) 이라 할 때, \\(\\sum_{m=2}^9 f(m)\\) 의 값은?"}
]'::jsonb
        ,13
        ,'[
  {"order": 1, "content":"37"},
  {"order": 2, "content":"42"},
  {"order": 3, "content":"47"},
  {"order": 4, "content":"52"},
  {"order": 5, "content":"57"}
]'::jsonb
        ,3
        , 'ALG'
        , 4
        , 'MCQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"다항함수 \\(f(x)\\) 에 대하여 함수 \\(g(x)\\) 를 다음과 같이 정의한다."},
  {"order": 2, "type": "TEXT", "content":"\\[ g(x)= \\begin{cases}x & (x<-1 \\text { 또는 } x>1) \\\\ f(x) & (-1 \\leq x \\leq 1)\\end{cases} \\]"},
  {"order": 3, "type": "TEXT", "content":"함수 \\(h(x)=\\lim _{t \\rightarrow 0+} g(x+t) \\times \\lim _{t \\rightarrow 2+} g(x+t)\\) 에 대하여 <보기>에서 옳은 것만을 있는 대로 고른 것은?"},
  {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/alg14p.JPG"}
]'::jsonb
        ,14
        ,'[
  {"order": 1, "content":"ᄀ"},
  {"order": 2, "content":"ᄂ"},
  {"order": 3, "content":"ᄀ, ᄂ"},
  {"order": 4, "content":"ᄀ, ᄃ"},
  {"order": 5, "content":"ᄂ, ᄃ"}
]'::jsonb
        ,1
        , 'ALG'
        , 4
        , 'MCQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"모든 항이 자연수이고 다음 조건을 만족시키는 모든 수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 \\(a_9\\) 의 최댓값과 최솟값을 각각 \\(M, m\\) 이라 할 때, \\(M+m\\) 의 값은?"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/alg15p.JPG"}
]'::jsonb
        ,15
        ,'[
  {"order": 1, "content":"216"},
  {"order": 2, "content":"218"},
  {"order": 3, "content":"220"},
  {"order": 4, "content":"222"},
  {"order": 5, "content":"224"}
]'::jsonb
        , 5
        , 'ALG'
        , 4
        , 'MCQ'
    ),

    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"방정식"},
  {"order": 2, "type": "TEXT", "content":"\\[ \\log _2(3 x+2)=2+\\log _2(x-2) \\]"},
  {"order": 3, "type": "TEXT", "content":"를 만족시키는 실수 \\(x\\) 의 값을 구하시오."}
]'::jsonb
        ,16
        ,null
        ,10
        , 'ALG'
        , 3
        , 'FRQ'
    ),

    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)\\) 에 대하여 \\(f^{\\prime}(x)=4 x^3-2 x\\) 이고 \\(f(0)=3\\) 일 때, \\(f(2)\\) 의 값을 구하시오."}
]'::jsonb
        ,17
        ,null
        ,15
        , 'ALG'
        , 3
        , 'FRQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"두 수열 \\(\\left\\{a_n\\right\\},\\left\\{b_n\\right\\}\\) 에 대하여"},
  {"order": 2, "type": "TEXT", "content":"\\[ \\sum_{k=1}^5\\left(3 a_k+5\\right)=55, \\quad \\sum_{k=1}^5\\left(a_k+b_k\\right)=32 \\]"},
  {"order": 3, "type": "TEXT", "content":"일 때, \\(\\sum_{k=1}^5 b_k\\) 의 값을 구하시오."}
]'::jsonb
        ,18
        ,null
        ,22
        , 'ALG'
        , 3
        , 'FRQ'
    ),

    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"방정식 \\(2 x^3-6 x^2+k=0\\) 의 서로 다른 양의 실근의 개수가 2 가 되도록 하는 정수 \\(k\\) 의 개수를 구하시오"}
]'::jsonb
        ,19
        ,null
        ,7
        , 'ALG'
        , 3
        , 'FRQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"수직선 위를 움직이는 점 P 의 시각 \\(t(t \\geq 0)\\) 에서의 속도 \\(v(t)\\) 와 가속도 \\(a(t)\\) 가 다음 조건을 만족시킨다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/alg20p.JPG"},
  {"order": 3, "type": "TEXT", "content":"시각 \\(t=0\\) 에서 \\(t=3\\) 까지 점 P 가 움직인 거리를 구하시오."}
]'::jsonb
        ,20
        ,null
        , 17
        , 'ALG'
        , 4
        , 'FRQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"자연수 \\(n\\) 에 대하여 함수 \\(f(x)\\) 를"},
  {"order": 2, "type": "TEXT", "content":"\\[ f(x)= \\begin{cases}\\left|3^{x+2}-n\\right| & (x<0) \\\\ \\left|\\log _2(x+4)-n\\right| & (x \\geq 0)\\end{cases} \\]"},
  {"order": 3, "type": "TEXT", "content":"이라 하자. 실수 \\(t\\) 에 대하여 \\(x\\) 에 대한 방정식 \\(f(x)=t\\) 의 서로 다른 실근의 개수를 \\(g(t)\\) 라 할 때, 함수 \\(g(t)\\) 의 최댓값이 4 가 되도록 하는 모든 자연수 \\(n\\) 의 값의 합을 구하시오."}
]'::jsonb
        ,21
        ,null
        , 33
        , 'ALG'
        , 4
        , 'FRQ'
    ),

    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"최고차항의 계수가 1 인 삼차함수 \\(f(x)\\) 와 실수 전체의 집합에서 연속인 함수 \\(g(x)\\) 가 다음 조건을 만족시킬 때, \\(f(4)\\) 의 값을 구하시오."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/alg22p.JPG"}
]'::jsonb
        ,22
        ,null
        ,13
        , 'ALG'
        , 4
        , 'FRQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"다항식 \\(\\left(x^3+3\\right)^5\\) 의 전개식에서 \\(x^9\\) 의 계수는?"}
]'::jsonb
        ,23
        ,'[
  {"order": 1, "content":"30"},
  {"order": 2, "content":"60"},
  {"order": 3, "content":"90"},
  {"order": 4, "content":"120"},
  {"order": 5, "content":"150"}
]'::jsonb
        ,3
        , 'PROB'
        , 2
        , 'MCQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"숫자 \\(1,2,3,4,5\\) 중에서 중복을 허락하여 4 개를 택해 일렬로 나열하여 만들 수 있는 네 자리의 자연수 중 4000 이상인 홀수의 개수는?"}
]'::jsonb
        ,24
        ,'[
  {"order": 1, "content":"125"},
  {"order": 2, "content":"150"},
  {"order": 3, "content":"175"},
  {"order": 4, "content":"200"},
  {"order": 5, "content":"225"}
]'::jsonb
        , 2
        , 'PROB'
        , 2
        , 'MCQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"흰색 마스크 5 개, 검은색 마스크 9 개가 들어 있는 상자가 있다. 이 상자에서 임의로 3 개의 마스크를 동시에 꺼낼 때, 꺼낸 3 개의 마스크 중에서 적어도 한 개가 흰색 마스크일 확률은?"}
]'::jsonb
        ,25
        ,'[
  {"order": 1, "content":"\\(\\frac{8}{13}\\)"},
  {"order": 2, "content":"\\(\\frac{17}{13}\\)"},
  {"order": 3, "content":"\\(\\frac{9}{13}\\)"},
  {"order": 4, "content":"\\(\\frac{19}{26}\\)"},
  {"order": 5, "content":"\\(\\frac{10}{13}\\)"}
]'::jsonb
        , 5
        , 'PROB'
        , 3
        , 'MCQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"주머니에 1 이 적힌 흰 공 1 개, 2 가 적힌 흰 공 1 개, 1 이 적힌 검은 공 1 개, 2 가 적힌 검은 공 3 개가 들어 있다. 이 주머니에서 임의로 3 개의 공을 동시에 꺼내는 시행을 한다. 이 시행에서 꺼낸 3 개의 공 중에서 흰 공이 1 개이고 검은 공이 2 개인 사건을 \\(A\\), 꺼낸 3 개의 공에 적혀 있는 수를 모두 곱한 값이 8 인 사건을 \\(B\\) 라 할 때, \\(\\mathrm{P}(A \\cup B)\\) 의 값은?"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/prob26p.JPG"}
]'::jsonb
        ,26
        ,'[
  {"order": 1, "content":"\\(\\frac{11}{20}\\)"},
  {"order": 2, "content":"\\(\\frac{3}{5}\\)"},
  {"order": 3, "content":"\\(\\frac{13}{20}\\)"},
  {"order": 4, "content":"\\(\\frac{7}{10}\\)"},
  {"order": 5, "content":"\\(\\frac{3}{4}\\)"}
]'::jsonb
        ,3
        , 'PROB'
        , 3
        , 'MCQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"어느 회사에서 생산하는 샴푸 1 개의 용량은 정규분포 \\(\\mathrm{N}\\left(m, \\sigma^2\\right)\\) 을 따른다고 한다. 이 회사에서 생산하는 샴푸 중에서 16 개를 임의추출하여 얻은 표본평균을 이용하여 구한 \\(m\\) 에 대한 신뢰도 95 \\% 의 신뢰구간이 \\(746.1 \\leq m \\leq 755.9\\) 이다."},
  {"order": 2, "type": "TEXT", "content":"이 회사에서 생산하는 샴푸 중에서 \\(n\\) 개를 임의추출하여 얻은 표본평균을 이용하여 구하는 \\(m\\) 에 대한 신뢰도 99 \\% 의 신뢰구간이 \\(a \\leq m \\leq b\\) 일 때, \\(b-a\\) 의 값이 6 이하가 되기 위한 자연수 \\(n\\) 의 최솟값은?"},
  {"order": 3, "type": "TEXT", "content":"(단, 용량의 단위는 mL 이고, \\(Z\\) 가 표준정규분포를 따르는 확률변수일 때, \\(\\mathrm{P}(|Z| \\leq 1.96)=0.95\\), \\(\\mathrm{P}(|Z| \\leq 2.58)=0.99\\) 로 계산한다.)"}
]'::jsonb
        ,27
        ,'[
  {"order": 1, "content":"70"},
  {"order": 2, "content":"74"},
  {"order": 3, "content":"78"},
  {"order": 4, "content":"82"},
  {"order": 5, "content":"86"}
]'::jsonb
        ,2
        , 'PROB'
        , 3
        , 'MCQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"연속확률변수 \\(X\\) 가 갖는 값의 범위는 \\(0 \\leq X \\leq a\\) 이고, \\(X\\) 의 확률밀도함수의 그래프가 그림과 같다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/prob28p.JPG"},
  {"order": 3, "type": "TEXT", "content":"\\(\\mathrm{P}(X \\leq b)-\\mathrm{P}(X \\geq b)=\\frac{1}{4}, \\mathrm{P}(X \\leq \\sqrt{5})=\\frac{1}{2}\\) 일 때, \\(a+b+c\\) 의 값은? (단, \\(a, b, c\\) 는 상수이다.)"}
]'::jsonb
        , 28
        , '[
  {"order": 1, "content":"\\(\\frac{11}{2}\\)"},
  {"order": 2, "content":"6"},
  {"order": 3, "content":"\\(\\frac{13}{2}\\)"},
  {"order": 4, "content":"7"},
  {"order": 5, "content":"\\(\\frac{15}{2}\\)"}
]'::jsonb
        , 4
        , 'PROB'
        , 4
        , 'MCQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"앞면에는 1 부터 6 까지의 자연수가 하나씩 적혀 있고 뒷면에는 모두 0 이 하나씩 적혀 있는 6 장의 카드가 있다. 이 6 장의 카드가 그림과 같이 6 이하의 자연수 \\(k\\) 에 대하여 \\(k\\) 번째 자리에 자연수 \\(k\\) 가 보이도록 놓여 있다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/prob29p.JPG"},
  {"order": 3, "type": "TEXT", "content":"이 6 장의 카드와 한 개의 주사위를 사용하여 다음 시행을 한다."},
  {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/prob29p2.JPG"},
  {"order": 5, "type": "TEXT", "content":"주사위를 한 번 던져 나온 눈의 수가 \\(k\\) 이면 \\(k\\) 번째 자리에 놓여 있는 카드를 한 번 뒤집어 제자리에 놓는다."},
  {"order": 6, "type": "TEXT", "content":"위의 시행을 3 번 반복한 후 6 장의 카드에 보이는 모든 수의 합이 짝수일 때, 주사위의 1 의 눈이 한 번만 나왔을 확률은 \\(\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
]'::jsonb
        ,29
        ,null
        ,49
        , 'PROB'
        , 4
        , 'FRQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"집합 \\(X=\\{x \\mid x\\) 는 10 이하의 자연수 \\(\\}\\) 에 대하여 다음 조건을 만족시키는 함수 \\(f: X \\rightarrow X\\) 의 개수를 구하시오."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/prob30p.JPG"}
]'::jsonb
        ,30
        ,null
        ,100
        , 'PROB'
        , 4
        , 'FRQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"\\(\\lim _{x \\rightarrow 0} \\frac{\\ln (x+1)}{\\sqrt{x+4}-2}\\) 의 값은?"}
]'::jsonb
        ,23
        ,'[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
]'::jsonb
        ,4
        , 'CALC'
        , 2
        , 'MCQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"\\(\\lim _{n \\rightarrow \\infty} \\frac{1}{n} \\sum_{k=1}^n \\sqrt{1+\\frac{3 k}{n}}\\) 의 값은?"}
]'::jsonb
        ,24
        ,'[
  {"order": 1, "content":"\\(\\frac{4}{3}\\)"},
  {"order": 2, "content":"\\(\\frac{13}{9}\\)"},
  {"order": 3, "content":"\\(\\frac{14}{9}\\)"},
  {"order": 4, "content":"\\(\\frac{5}{3}\\)"},
  {"order": 5, "content":"\\(\\frac{16}{9}\\)"}
]'::jsonb
        ,3
        , 'CALC'
        , 3
        , 'MCQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"등비수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 \\(\\lim _{n \\rightarrow \\infty} \\frac{a_n+1}{3^n+2^{2 n-1}}=3\\) 일 때, \\(a_2\\) 의 값은?"}
]'::jsonb
        ,25
        ,'[
  {"order": 1, "content":"16"},
  {"order": 2, "content":"18"},
  {"order": 3, "content":"20"},
  {"order": 4, "content":"22"},
  {"order": 5, "content":"24"}
]'::jsonb
        ,5
        , 'CALC'
        , 3
        , 'MCQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"그림과 같이 곡선 \\(y=\\sqrt{\\sec ^2 x+\\tan x}\\left(0 \\leq x \\leq \\frac{\\pi}{3}\\right)\\) 와 \\(x\\) 축, \\(y\\) 축 및 직선 \\(x=\\frac{\\pi}{3}\\) 로 둘러싸인 부분을 밑면으로 하는 입체도형이 있다. 이 입체도형을 \\(x\\) 축에 수직인 평면으로 자른 단면이 모두 정사각형일 때, 이 입체도형의 부피는?"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/calc20p.JPG"}
]'::jsonb
        ,26
        ,'[
  {"order": 1, "content":"\\(\\frac{\\sqrt{3}}{2}+\\frac{\\ln 2}{2}\\)"},
  {"order": 2, "content":"\\(\\frac{\\sqrt{3}}{2}+\\ln 2\\)"},
  {"order": 3, "content":"\\(\\sqrt{3}+\\frac{\\ln 2}{2}\\)"},
  {"order": 4, "content":"\\(\\sqrt{3}+\\ln 2\\)"},
  {"order": 5, "content":"\\(\\sqrt{3}+2 \\ln 2\\)"}
]'::jsonb
        , 4
        , 'CALC'
        , 3
        , 'MCQ'
    ),
       ((SELECT id FROM exam)
       , '[
         {
           "order": 1,
           "type": "TEXT",
           "content": "그림과 같이 중심이 O , 반지름의 길이가 1 이고 중심각의 크기가 \\(\\frac{\\pi}{2}\\) 인 부채꼴 \\(\\mathrm{OA}_1 \\mathrm{~B}_1\\) 이 있다. 호 \\(\\mathrm{A}_1 \\mathrm{~B}_1\\) 위에 점 \\(\\mathrm{P}_1\\), 선분 \\(\\mathrm{OA}_1\\) 위에 점 \\(\\mathrm{C}_1\\), 선분 \\(\\mathrm{OB}_1\\) 위에 점 \\(\\mathrm{D}_1\\) 을 사각형 \\(\\mathrm{OC}_1 \\mathrm{P}_1 \\mathrm{D}_1\\) 이 \\(\\overline{\\mathrm{OC}_1}: \\overline{\\mathrm{OD}_1}=3: 4\\) 인 직사각형이 되도록 잡는다. 부채꼴 \\(\\mathrm{OA}_1 \\mathrm{~B}_1\\) 의 내부에 점 \\(\\mathrm{Q}_1\\) 을 \\(\\overline{\\mathrm{P}_1 \\mathrm{Q}_1}=\\overline{\\mathrm{A}_1 \\mathrm{Q}_1}, \\angle \\mathrm{P}_1 \\mathrm{Q}_1 \\mathrm{~A}_1=\\frac{\\pi}{2}\\) 가 되도록 잡고, 이등변삼각형 \\(\\mathrm{P}_1 \\mathrm{Q}_1 \\mathrm{~A}_1\\) 에 색칠하여 얻은 그림을 \\(R_1\\) 이라 하자."
         },
         {
           "order": 2,
           "type": "TEXT",
           "content": "그림 \\(R_1\\) 에서 선분 \\(\\mathrm{OA}_1\\) 위의 점 \\(\\mathrm{A}_2\\) 와 선분 \\(\\mathrm{OB}_1\\) 위의 점 \\(\\mathrm{B}_2\\) 를 \\(\\overline{\\mathrm{OQ}_1}=\\overline{\\mathrm{OA}_2}=\\overline{\\mathrm{OB}_2}\\) 가 되도록 잡고, 중심이 O , 반지름의 길이가 \\(\\overline{\\mathrm{OQ}_1}\\), 중심각의 크기가 \\(\\frac{\\pi}{2}\\) 인 부채꼴 \\(\\mathrm{OA}_2 \\mathrm{~B}_2\\) 를 그린다. 그림 \\(R_1\\) 을 얻은 것과 같은 방법으로 네 점 \\(\\mathrm{P}_2, \\mathrm{C}_2, \\mathrm{D}_2, \\mathrm{Q}_2\\) 를 잡고, 이등변삼각형 \\(\\mathrm{P}_2 \\mathrm{Q}_2 \\mathrm{~A}_2\\) 에 색칠하여 얻은 그림을 \\(R_2\\) 라 하자. 이와 같은 과정을 계속하여 \\(n\\) 번째 얻은 그림 \\(R_n\\) 에 색칠되어 있는 부분의 넓이를 \\(S_n\\) 이라 할 때, \\(\\lim _{n \\rightarrow \\infty} S_n\\) 의 값은?"
         },
         {
           "order": 3,
           "type": "IMAGE",
           "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/cacl27p.JPG"
         }
       ]'::jsonb
       , 27
       , '[
         {"order": 1, "content":"\\(\\frac{9}{40}\\)"},
  {"order": 2, "content":"\\(\\frac{1}{4}\\)"},
  {"order": 3, "content":"\\(\\frac{11}{40}\\)"},
  {"order": 4, "content":"\\(\\frac{3}{10}\\)"},
  {"order": 5, "content":"\\(\\frac{13}{40}\\)"}
]'::jsonb
        ,2
        , 'CALC'
        , 3
        , 'MCQ'
       ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"그림과 같이 중심이 O 이고 길이가 2 인 선분 AB 를 지름으로 하는 반원 위에 \\(\\angle \\mathrm{AOC}=\\frac{\\pi}{2}\\) 인 점 C 가 있다. 호 BC 위에 점 P 와 호 CA 위에 점 Q 를 \\(\\overline{\\mathrm{PB}}=\\overline{\\mathrm{QC}}\\) 가 되도록 잡고, 선분 AP 위에 점 R 를 \\(\\angle \\mathrm{CQR}=\\frac{\\pi}{2}\\) 가 되도록 잡는다. 선분 AP 와 선분 CO 의 교점을 S 라 하자."},
  {"order": 2, "type": "TEXT", "content":"\\(\\angle \\mathrm{PAB}=\\theta\\) 일 때, 삼각형 POB 의 넓이를 \\(f(\\theta)\\), 사각형 CQRS 의 넓이를 \\(g(\\theta)\\) 라 하자. \\(\\lim _{\\theta \\rightarrow 0+} \\frac{3 f(\\theta)-2 g(\\theta)}{\\theta^2}\\) 의 값은? (단, \\(0<\\theta<\\frac{\\pi}{4}\\) )"},
  {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/cacl28p.JPG"}
]'::jsonb
        ,28
        ,'[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
]'::jsonb
        , 2
        , 'CALC'
        , 4
        , 'MCQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"세 상수 \\(a, b, c\\) 에 대하여 함수 \\(f(x)=a e^{2 x}+b e^x+c\\) 가 다음 조건을 만족시킨다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/calc29p.JPG"},
  {"order": 3, "type": "TEXT", "content":"함수 \\(f(x)\\) 의 역함수를 \\(g(x)\\) 라 할 때, \\(\\int_0^{14} g(x) d x=p+q \\ln 2\\) 이다. \\(p+q\\) 의 값을 구하시오."},
  {"order": 4, "type": "TEXT", "content":"(단, \\(p, q\\) 는 유리수이고, \\(\\ln 2\\) 는 무리수이다.)"}
]'::jsonb
        ,29
        ,null
        , 26
        , 'CALC'
        , 4
        , 'FRQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"최고차항의 계수가 양수인 삼차함수 \\(f(x)\\) 와 함수 \\(g(x)=e^{\\sin \\pi x}-1\\) 에 대하여 실수 전체의 집합에서 정의된 합성함수 \\(h(x)=g(f(x))\\) 가 다음 조건을 만족시킨다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/calc30p.JPG"},
  {"order": 2, "type": "TEXT", "content":"\\(f(3)=\\frac{1}{2}, f^{\\prime}(3)=0\\) 일 때, \\(f(2)=\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
]'::jsonb
        ,30
        ,null
        , 31
        , 'CALC'
        , 4
        , 'FRQ'
    ),
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"좌표공간의 점 \\(\\mathrm{A}(2,2,-1)\\) 을 \\(x\\) 축에 대하여 대칭이동한 점을 B 라 하자. 점 \\(\\mathrm{C}(-2,1,1)\\) 에 대하여 선분 BC 의 길이는?"}
]'::jsonb
        ,23
        ,'[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
]'::jsonb
        ,5
        , 'GEO'
        , 2
        , 'MCQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"초점이 \\(\\mathrm{F}\\left(\\frac{1}{3}, 0\\right)\\) 이고 준선이 \\(x=-\\frac{1}{3}\\) 인 포물선이 점 \\((a, 2)\\) 를 지날 때, \\(a\\) 의 값은?"}
]'::jsonb
        ,24
        ,'[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
]'::jsonb
        ,3
        , 'GEO'
        , 3
        , 'MCQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"타원 \\(\\frac{x^2}{a^2}+\\frac{y^2}{b^2}=1\\) 위의 점 \\((2,1)\\) 에서의 접선의 기울기가 \\(-\\frac{1}{2}\\) 일 때, 이 타원의 두 초점 사이의 거리는? (단, \\(a, b\\) 는 양수이다.)"}
]'::jsonb
        ,25
        ,'[
  {"order": 1, "content":"\\(2 \\sqrt{3}\\)"},
  {"order": 2, "content":"4"},
  {"order": 3, "content":"\\(2 \\sqrt{5}\\)"},
  {"order": 4, "content":"\\(2 \\sqrt{6}\\)"},
  {"order": 5, "content":"\\(2 \\sqrt{7}\\)"}
]'::jsonb
        ,4
        , 'GEO'
        , 3
        , 'MCQ'
    ) , ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"좌표평면에서 세 벡터"},
  {"order": 2, "type": "TEXT", "content":"\\[ \\vec{a}=(2,4), \\quad \\vec{b}=(2,8), \\quad \\vec{c}=(1,0) \\]"},
  {"order": 3, "type": "TEXT", "content":"에 대하여 두 벡터 \\(\\vec{p}, \\vec{q}\\) 가"},
  {"order": 4, "type": "TEXT", "content":"\\[ (\\vec{p}-\\vec{a}) \\cdot(\\vec{p}-\\vec{b})=0, \\quad \\vec{q}=\\frac{1}{2} \\vec{a}+t \\vec{c}(t \\text { 는 실수 }) \\]"},
  {"order": 5, "type": "TEXT", "content":"를 만족시킬 때, \\(|\\vec{p}-\\vec{q}|\\) 의 최솟값은?"}
]'::jsonb
        , 26
        , '[
  {"order": 1, "content":"\\(\\frac{3}{2}\\)"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"\\(\\frac{5}{2}\\)"},
  {"order": 4, "content":"3"},
  {"order": 5, "content":"\\(\\frac{7}{2}\\)"}
]'::jsonb
        , 2
        , 'GEO'
        , 3
        , 'MCQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"좌표공간에 직선 AB 를 포함하는 평면 \\(\\alpha\\) 가 있다. 평면 \\(\\alpha\\) 위에 있지 않은 점 C 에 대하여 직선 AB 와 직선 AC 가 이루는 예각의 크기를 \\(\\theta_1\\) 이라 할 때 \\(\\sin \\theta_1=\\frac{4}{5}\\) 이고, 직선 AC 와 평면 \\(\\alpha\\) 가 이루는 예각의 크기는 \\(\\frac{\\pi}{2}-\\theta_1\\) 이다."},
  {"order": 2, "type": "TEXT", "content":"평면 ABC 와 평면 \\(\\alpha\\) 가 이루는 예각의 크기를 \\(\\theta_2\\) 라 할 때, \\(\\cos \\theta_2\\) 의 값은?"},
  {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/geo27p.JPG"}
]'::jsonb
        ,27
        ,'[
  {"order": 1, "content":"\\(\\frac{\\sqrt{7}}{4}\\)"},
  {"order": 2, "content":"\\(\\frac{\\sqrt{7}}{5}\\)"},
  {"order": 3, "content":"\\(\\frac{\\sqrt{7}}{6}\\)"},
  {"order": 4, "content":"\\(\\frac{\\sqrt{7}}{7}\\)"},
  {"order": 5, "content":"\\(\\frac{\\sqrt{7}}{8}\\)"}
]'::jsonb
        , 1
        , 'GEO'
        , 3
        , 'MCQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"두 초점이 \\(\\mathrm{F}(c, 0), \\mathrm{F}^{\\prime}(-c, 0)(c>0)\\) 인 쌍곡선 \\(C\\) 와 \\(y\\) 축 위의 점 A 가 있다. 쌍곡선 \\(C\\) 가 선분 AF 와 만나는 점을 P , 선분 \\(\\mathrm{AF}^{\\prime}\\) 과 만나는 점을 \\(\\mathrm{P}^{\\prime}\\) 이라 하자."},
  {"order": 2, "type": "TEXT", "content":"직선 AF 는 쌍곡선 \\(C\\) 의 한 점근선과 평행하고"},
  {"order": 3, "type": "TEXT", "content":"\\[ \\overline{\\mathrm{AP}}: \\overline{\\mathrm{PP}^{\\prime}}=5: 6, \\quad \\overline{\\mathrm{PF}}=1 \\]"},
  {"order": 4, "type": "TEXT", "content":"일 때, 쌍곡선 \\(C\\) 의 주축의 길이는?"},
  {"order": 5, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/geo28p.JPG"}
]'::jsonb
        ,28
        ,'[
  {"order": 1, "content":"\\(\\frac{13}{6}\\)"},
  {"order": 2, "content":"\\(\\frac{9}{4}\\)"},
  {"order": 3, "content":"\\(\\frac{7}{3}\\)"},
  {"order": 4, "content":"\\(\\frac{29}{12}\\)"},
  {"order": 5, "content":"\\(\\frac{5}{2}\\)"}
]'::jsonb
        , 2
        , 'GEO'
        , 4
        , 'MCQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"평면 \\(\\alpha\\) 위에 \\(\\overline{\\mathrm{AB}}=\\overline{\\mathrm{CD}}=\\overline{\\mathrm{AD}}=2, \\angle \\mathrm{ABC}=\\angle \\mathrm{BCD}=\\frac{\\pi}{3}\\) 인 사다리꼴 ABCD 가 있다. 다음 조건을 만족시키는 평면 \\(\\alpha\\) 위의 두 점 \\(\\mathrm{P}, \\mathrm{Q}\\) 에 대하여 \\(\\overrightarrow{\\mathrm{CP}} \\cdot \\overrightarrow{\\mathrm{DQ}}\\) 의 값을 구하시오."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/geo29p.JPG"},
  {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/geo29p2.JPG"}
]'::jsonb
        ,29
        ,null
        , 12
        , 'GEO'
        , 4
        , 'FRQ'
    ) ,
    ((SELECT id FROM exam)
        , '[
  {"order": 1, "type": "TEXT", "content":"좌표공간에 정사면체 ABCD 가 있다. 정삼각형 BCD 의 외심을 중심으로 하고 점 B 를 지나는 구를 \\(S\\) 라 하자."},
  {"order": 2, "type": "TEXT", "content":"구 \\(S\\) 와 선분 AB 가 만나는 점 중 B 가 아닌 점을 P , 구 \\(S\\) 와 선분 AC 가 만나는 점 중 C 가 아닌 점을 Q , 구 \\(S\\) 와 선분 AD 가 만나는 점 중 D 가 아닌 점을 R 라 하고, 점 P 에서 구 \\(S\\) 에 접하는 평면을 \\(\\alpha\\) 라 하자."},
  {"order": 3, "type": "TEXT", "content":"구 \\(S\\) 의 반지름의 길이가 6 일 때, 삼각형 PQR 의 평면 \\(\\alpha\\) 위로의 정사영의 넓이는 \\(k\\) 이다. \\(k^2\\) 의 값을 구하시오."},
  {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/geo30p.JPG"}
]'::jsonb
        ,30
        ,null
        ,24
        , 'GEO'
        , 4
        , 'FRQ'
    );




