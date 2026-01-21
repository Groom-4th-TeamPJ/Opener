insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2025
       , 'M06'
       , '6월 모의평가'
       , 30
       , 6000);


WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2025 AND exam_type = 'M06' LIMIT 1
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
  {"order": 1, "type": "TEXT", "content":"\\(\\left(\\frac{5}{\\sqrt[3]{25}}\\right)^{\\frac{3}{2}}\\) 의 값은?"}
]'::jsonb
       , 1
       , '[
  {"order": 1, "content":"\\(\\frac{1}{5}\\)"},
  {"order": 2, "content":"\\(\\frac{\\sqrt{5}}{5}\\)"},
  {"order": 3, "content":"1"},
  {"order": 4, "content":"\\(\\sqrt{5}\\)"},
  {"order": 5, "content":"5"}
]'::jsonb
       , 4
       , 'ALG'
       , 2
       , 'MCQ'
       )
        ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)=x^2+x+2\\) 에 대하여 \\(\\lim _{h \\rightarrow 0} \\frac{f(2+h)-f(2)}{h}\\) 의 값은?"}
]'::jsonb
, 2
, '[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
]'::jsonb
, 5
, 'ALG'
, 2
, 'MCQ'
)
, ((SELECT id FROM exam)
  , '[
  {"order": 1, "type": "TEXT", "content":"수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 \\(\\sum_{k=1}^5\\left(a_k+1\\right)=9\\) 이고 \\(a_6=4\\) 일 때, \\(\\sum_{k=1}^6 a_k\\) 의 값은?"}
]'::jsonb
  , 3
  , '[
  {"order": 1, "content":"6"},
  {"order": 2, "content":"7"},
  {"order": 3, "content":"8"},
  {"order": 4, "content":"9"},
  {"order": 5, "content":"10"}
]'::jsonb
  , 3
  , 'ALG'
  , 3
  , 'MCQ'
)
     , ((SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content":"함수 \\(y=f(x)\\) 의 그래프가 그림과 같다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/alg4p.JPG"},
  {"order": 3, "type": "TEXT", "content":"\\(\\lim _{x \\rightarrow 0+} f(x)+\\lim _{x \\rightarrow 1-} f(x)\\) 의 값은?"}
]'::jsonb
       , 4
       , '[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
]'::jsonb
       , 3
       , 'ALG'
       , 3
       , 'MCQ'
)
     , ((SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)=\\left(x^2-1\\right)\\left(x^2+2 x+2\\right)\\) 에 대하여 \\(f^{\\prime}(1)\\) 의 값은?"}
]'::jsonb
       , 5
       , '[
  {"order": 1, "content":"6"},
  {"order": 2, "content":"7"},
  {"order": 3, "content":"8"},
  {"order": 4, "content":"9"},
  {"order": 5, "content":"10"}
]'::jsonb
       , 5
       , 'ALG'
       , 3
       , 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"\\(\\pi<\\theta<\\frac{3}{2} \\pi\\) 인 \\(\\theta\\) 에 대하여 \\(\\sin \\left(\\theta-\\frac{\\pi}{2}\\right)=\\frac{3}{5}\\) 일 때, \\(\\sin \\theta\\) 의 값은?"}
]'::jsonb
, 6
, '[
  {"order": 1, "content":"\\(-\\frac{4}{5}\\)"},
  {"order": 2, "content":"\\(-\\frac{3}{5}\\)"},
  {"order": 3, "content":"\\(\\frac{3}{5}\\)"},
  {"order": 4, "content":"\\(\\frac{3}{4}\\)"},
  {"order": 5, "content":"\\(\\frac{4}{5}\\)"}
]'::jsonb
, 1
, 'ALG'
, 3
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"\\(x\\) 에 대한 방정식 \\(x^3-3 x^2-9 x+k=0\\) 의 서로 다른 실근의 개수가 2 가 되도록 하는 모든 실수 \\(k\\) 의 값의 합은?"}
]'::jsonb
, 7
, '[
  {"order": 1, "content":"13"},
  {"order": 2, "content":"16"},
  {"order": 3, "content":"19"},
  {"order": 4, "content":"22"},
  {"order": 5, "content":"25"}
]'::jsonb
, 4
, 'ALG'
, 3
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"\\(a_1 a_2<0\\) 인 등비수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여"},
  {"order": 2, "type": "TEXT", "content":"\\[a_6=16, \\quad 2 a_8-3 a_7=32\\]"},
  {"order": 3, "type": "TEXT", "content":"일 때, \\(a_9+a_{11}\\) 의 값은?"}
]'::jsonb
, 8
, '[
  {"order": 1, "content":"\\(-\\frac{5}{2}\\)"},
  {"order": 2, "content":"\\(-\\frac{3}{2}\\)"},
  {"order": 3, "content":"\\(-\\frac{1}{2}\\)"},
  {"order": 4, "content":"\\(\\frac{1}{2}\\)"},
  {"order": 5, "content":"\\(\\frac{3}{2}\\)"}
]'::jsonb
, 1
, 'ALG'
, 3
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"함수"},
  {"order": 2, "type": "TEXT", "content":"\\[f(x)=\\left\\{\\begin{array}{rr}\nx-\\frac{1}{2} & (x<0) \\\\\n-x^2+3 & (x \\geq 0)\n\\end{array}\\right.\\]"},
  {"order": 3, "type": "TEXT", "content":"에 대하여 함수 \\((f(x)+a)^2\\) 이 실수 전체의 집합에서 연속일 때, 상수 \\(a\\) 의 값은?"}
]'::jsonb
, 9
, '[
  {"order": 1, "content":"\\(-\\frac{9}{4}\\)"},
  {"order": 2, "content":"\\(-\\frac{7}{4}\\)"},
  {"order": 3, "content":"\\(-\\frac{5}{4}\\)"},
  {"order": 4, "content":"\\(-\\frac{3}{4}\\)"},
  {"order": 5, "content":"\\(-\\frac{1}{4}\\)"}
]'::jsonb
, 3
, 'ALG'
, 4
, 'MCQ'
)
,((SELECT id FROM exam)
 , '[
  {"order": 1, "type": "TEXT", "content":"다음 조건을 만족시키는 삼각형 ABC 의 외접원의 넓이가 \\(9 \\pi\\) 일 때, 삼각형 ABC 의 넓이는?"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/alg10p.JPG"}
]'::jsonb
 , 10
 , '[
  {"order": 1, "content":"\\(\\frac{32}{9} \\sqrt{2}\\)"},
  {"order": 2, "content":"\\(\\frac{40}{9} \\sqrt{2}\\)"},
  {"order": 3, "content":"\\(\\frac{16}{3} \\sqrt{2}\\)"},
  {"order": 4, "content":"\\(\\frac{56}{9} \\sqrt{2}\\)"},
  {"order": 5, "content":"\\(\\frac{64}{9} \\sqrt{2}\\)"}
]'::jsonb
 , 5
 , 'ALG'
 , 4
 , 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"최고차항의 계수가 1 이고 \\(f(0)=0\\) 인 삼차함수 \\(f(x)\\) 가"},
  {"order": 2, "type": "TEXT", "content":"\\[\\lim _{x \\rightarrow a} \\frac{f(x)-1}{x-a}=3\\]"},
  {"order": 3, "type": "TEXT", "content":"을 만족시킨다. 곡선 \\(y=f(x)\\) 위의 점 \\((a, f(a))\\) 에서의 접선의 \\(y\\) 절편이 4 일 때, \\(f(1)\\) 의 값은? (단, \\(a\\) 는 상수이다.)"}
]'::jsonb
, 11
, '[
  {"order": 1, "content":"-1"},
  {"order": 2, "content":"-2"},
  {"order": 3, "content":"-3"},
  {"order": 4, "content":"-4"},
  {"order": 5, "content":"-5"}
]'::jsonb
, 5
, 'ALG'
, 4
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"그림과 같이 곡선 \\(y=1-2^{-x}\\) 위의 제 1 사분면에 있는 점 A 를 지나고 \\(y\\) 축에 평행한 직선이 곡선 \\(y=2^x\\) 과 만나는 점을 B 라 하자. 점 A 를 지나고 \\(x\\) 축에 평행한 직선이 곡선 \\(y=2^x\\) 과 만나는 점을 C , 점 C 를 지나고 \\(y\\) 축에 평행한 직선이 곡선 \\(y=1-2^{-x}\\) 과 만나는 점을 D 라 하자. \\(\\overline{\\mathrm{AB}}=2 \\overline{\\mathrm{CD}}\\) 일 때, 사각형 ABCD 의 넓이는?"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/alg12p.JPG"}
]'::jsonb
, 12
, '[
  {"order": 1, "content":"\\(\\frac{5}{2} \\log _2 3-\\frac{5}{4}\\)"},
  {"order": 2, "content":"\\(3 \\log _2 3-\\frac{3}{2}\\)"},
  {"order": 3, "content":"\\(\\frac{7}{2} \\log _2 3-\\frac{7}{4}\\)"},
  {"order": 4, "content":"\\(4 \\log _2 3-2\\)"},
  {"order": 5, "content":"\\(\\frac{9}{2} \\log _2 3-\\frac{9}{4}\\)"}
]'::jsonb
, 3
, 'ALG'
, 4
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"곡선 \\(y=\\frac{1}{4} x^3+\\frac{1}{2} x\\) 와 직선 \\(y=m x+2\\) 및 \\(y\\) 축으로 둘러싸인 부분의 넓이를 \\(A\\), 곡선 \\(y=\\frac{1}{4} x^3+\\frac{1}{2} x\\) 와 두 직선 \\(y=m x+2, x=2\\) 로 둘러싸인 부분의 넓이를 \\(B\\) 라 하자. \\(B-A=\\frac{2}{3}\\) 일 때, 상수 \\(m\\) 의 값은? (단, \\(m<-1\\) )"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/alg13p.JPG"}
]'::jsonb
, 13
, '[
  {"order": 1, "content":"\\(-\\frac{3}{2}\\)"},
  {"order": 2, "content":"\\(-\\frac{17}{12}\\)"},
  {"order": 3, "content":"\\(-\\frac{4}{3}\\)"},
  {"order": 4, "content":"\\(-\\frac{5}{4}\\)"},
  {"order": 5, "content":"\\(-\\frac{7}{6}\\)"}
]'::jsonb
, 3
, 'ALG'
, 4
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"다음 조건을 만족시키는 모든 자연수 \\(k\\) 의 값의 합은?"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/alg14p.JPG"}
]'::jsonb
, 14
, '[
  {"order": 1, "content":"6"},
  {"order": 2, "content":"7"},
  {"order": 3, "content":"8"},
  {"order": 4, "content":"9"},
  {"order": 5, "content":"10"}
]'::jsonb
, 4
, 'ALG'
, 4
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"최고차항의 계수가 1 인 삼차함수 \\(f(x)\\) 와 상수 \\(k(k \\geq 0)\\) 에 대하여 함수"},
  {"order": 2, "type": "TEXT", "content":"\\[g(x)= \\begin{cases}2 x-k & (x \\leq k) \\\\ f(x) & (x>k)\\end{cases}\\]"},
  {"order": 3, "type": "TEXT", "content":"가 다음 조건을 만족시킨다."},
  {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/alg15p.JPG"},
  {"order": 5, "type": "TEXT", "content":"\\(g(k+1)\\) 의 최솟값은?"}
]'::jsonb
, 15
, '[
  {"order": 1, "content":"\\(4-\\sqrt{6}\\)"},
  {"order": 2, "content":"\\(5-\\sqrt{6}\\)"},
  {"order": 3, "content":"\\(6-\\sqrt{6}\\)"},
  {"order": 4, "content":"\\(7-\\sqrt{6}\\)"},
  {"order": 5, "content":"\\(8-\\sqrt{6}\\)"}
]'::jsonb
, 2
, 'ALG'
, 4
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"방정식 \\(\\log _2(x+1)-5=\\log _{\\frac{1}{2}}(x-3)\\) 을 만족시키는 실수 \\(x\\) 의 값을 구하시오."}
]'::jsonb
, 16
, null
, 7
, 'ALG'
, 3
, 'FRQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)\\) 에 대하여 \\(f^{\\prime}(x)=6 x^2+2\\) 이고 \\(f(0)=3\\) 일 때, \\(f(2)\\) 의 값을 구하시오."}
]'::jsonb
, 17
, null
, 23
, 'ALG'
, 3
, 'FRQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"\\(\\sum_{k=1}^9\\left(a k^2-10 k\\right)=120\\) 일 때, 상수 \\(a\\) 의 값을 구하시오."}
]'::jsonb
, 18
, null
, 2
, 'ALG'
, 3
, 'FRQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"시각 \\(t=0\\) 일 때 원점을 출발하여 수직선 위를 움직이는 점 P 의 시각 \\(t(t \\geq 0)\\) 에서의 속도 \\(v(t)\\) 가"},
  {"order": 2, "type": "TEXT", "content":"\\[v(t)= \\begin{cases}-t^2+t+2 & (0 \\leq t \\leq 3) \\\\ k(t-3)-4 & (t>3)\\end{cases}\\]"},
  {"order": 3, "type": "TEXT", "content":"이다. 출발한 후 점 P 의 운동 방향이 두 번째로 바뀌는 시각에서의 점 P 의 위치가 1 일 때, 양수 \\(k\\) 의 값을 구하시오."}
]'::jsonb
, 19
, null
, 16
, 'ALG'
, 3
, 'FRQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"5 이하의 두 자연수 \\(a, b\\) 에 대하여 열린구간 \\((0,2 \\pi)\\) 에서 정의된 함수 \\(y=a \\sin x+b\\) 의 그래프가 직선 \\(x=\\pi\\) 와 만나는 점의 집합을 \\(A\\) 라 하고, 두 직선 \\(y=1, y=3\\) 과 만나는 점의 집합을 각각 \\(B, C\\) 라 하자. \\(n(A \\cup B \\cup C)=3\\) 이 되도록 하는 \\(a, b\\) 의 순서쌍 \\((a, b)\\) 에 대하여 \\(a+b\\) 의 최댓값을 \\(M\\), 최솟값을 \\(m\\) 이라 할 때, \\(M \\times m\\) 의 값을 구하시오."}
]'::jsonb
, 20
, null
, 24
, 'ALG'
, 4
, 'FRQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"최고차항의 계수가 1 인 사차함수 \\(f(x)\\) 가 다음 조건을 만족시킨다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/alg21p.JPG"},
  {"order": 3, "type": "TEXT", "content":"\\(f(0)=0, f^{\\prime}(1)=0\\) 일 때, \\(f(3)\\) 의 값을 구하시오."}
]'::jsonb
, 21
, null
, 15
, 'ALG'
, 4
, 'FRQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"수열 \\(\\left\\{a_n\\right\\}\\) 은"},
  {"order": 2, "type": "TEXT", "content":"\\[a_2=-a_1\\]"},
  {"order": 3, "type": "TEXT", "content":"이고, \\(n \\geq 2\\) 인 모든 자연수 \\(n\\) 에 대하여"},
  {"order": 4, "type": "TEXT", "content":"\\[a_{n+1}= \\begin{cases}a_n-\\sqrt{n} \\times a_{\\sqrt{n}} & \\left(\\sqrt{n} \\text { 이 자연수이고 } a_n>0 \\text { 인 경우 }\\right) \\\\ a_n+1 & (\\text { 그 외의 경우 })\\end{cases}\\]"},
  {"order": 5, "type": "TEXT", "content":"를 만족시킨다. \\(a_{15}=1\\) 이 되도록 하는 모든 \\(a_1\\) 의 값의 곱을 구하시오."}
]'::jsonb
, 22
, null
, 231
, 'ALG'
, 4
, 'FRQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"네 개의 숫자 \\(1,1,2,3\\) 을 모두 일렬로 나열하는 경우의 수는?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content":"8"},
  {"order": 2, "content":"10"},
  {"order": 3, "content":"12"},
  {"order": 4, "content":"14"},
  {"order": 5, "content":"16"}
]'::jsonb
, 3
, 'PROB'
, 2
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"두 사건 \\(A, B\\) 는 서로 배반사건이고"},
  {"order": 2, "type": "TEXT", "content":"\\[\\mathrm{P}\\left(A^C\\right)=\\frac{5}{6}, \\quad \\mathrm{P}(A \\cup B)=\\frac{3}{4}\\]"},
  {"order": 3, "type": "TEXT", "content":"일 때, \\(\\mathrm{P}\\left(B^C\\right)\\) 의 값은?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content":"\\(\\frac{3}{8}\\)"},
  {"order": 2, "content":"\\(\\frac{5}{12}\\)"},
  {"order": 3, "content":"\\(\\frac{11}{24}\\)"},
  {"order": 4, "content":"\\(\\frac{1}{2}\\)"},
  {"order": 5, "content":"\\(\\frac{13}{24}\\)"}
]'::jsonb
, 2
, 'PROB'
, 3
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"다항식 \\(\\left(x^2-2\\right)^5\\) 의 전개식에서 \\(x^6\\) 의 계수는?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content":"-50"},
  {"order": 2, "content":"-20"},
  {"order": 3, "content":"10"},
  {"order": 4, "content":"40"},
  {"order": 5, "content":"70"}
]'::jsonb
, 4
, 'PROB'
, 3
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"문자 \\(a, b, c, d\\) 중에서 중복을 허락하여 4 개를 택해 일렬로 나열하여 만들 수 있는 모든 문자열 중에서 임의로 하나를 선택할 때, 문자 \\(a\\) 가 한 개만 포함되거나 문자 \\(b\\) 가 한 개만 포함된 문자열이 선택될 확률은?"}
]'::jsonb
, 26
, '[
  {"order": 1, "content":"\\(\\frac{5}{8}\\)"},
  {"order": 2, "content":"\\(\\frac{41}{64}\\)"},
  {"order": 3, "content":"\\(\\frac{21}{32}\\)"},
  {"order": 4, "content":"\\(\\frac{43}{64}\\)"},
  {"order": 5, "content":"\\(\\frac{11}{16}\\)"}
]'::jsonb
, 3
, 'PROB'
, 3
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"1 부터 6 까지의 자연수가 하나씩 적혀 있는 6 개의 의자가 있다. 이 6 개의 의자를 일정한 간격을 두고 원형으로 배열할 때, 서로 이웃한 2 개의 의자에 적혀 있는 수의 합이 11 이 되지 않도록 배열하는 경우의 수는?"},
  {"order": 2, "type": "TEXT", "content":"(단, 회전하여 일치하는 것은 같은 것으로 본다.)"},
  {"order": 3, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/prob27p.JPG"}
]'::jsonb
, 27
, '[
  {"order": 1, "content":"72"},
  {"order": 2, "content":"78"},
  {"order": 3, "content":"84"},
  {"order": 4, "content":"90"},
  {"order": 5, "content":"96"}
]'::jsonb
, 1
, 'PROB'
, 3
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"탁자 위에 놓인 4 개의 동전에 대하여 다음 시행을 한다."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/prob28p.JPG"},
  {"order": 3, "type": "TEXT", "content":"처음에 3 개의 동전은 앞면이 보이도록, 1 개의 동전은 뒷면이 보이도록 놓여 있다. 위의 시행을 5 번 반복한 후 4 개의 동전이 모두 같은 면이 보이도록 놓여 있을 때, 모두 앞면이 보이도록 놓여 있을 확률은?"},
  {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/prob28p2.JPG"}
]'::jsonb
, 28
, '[
  {"order": 1, "content":"\\(\\frac{17}{32}\\)"},
  {"order": 2, "content":"\\(\\frac{35}{64}\\)"},
  {"order": 3, "content":"\\(\\frac{9}{16}\\)"},
  {"order": 4, "content":"\\(\\frac{37}{64}\\)"},
  {"order": 5, "content":"\\(\\frac{19}{32}\\)"}
]'::jsonb
, 1
, 'PROB'
, 4
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"40 개의 공이 들어 있는 주머니가 있다. 각각의 공은 흰 공 또는 검은 공 중 하나이다."},
  {"order": 2, "type": "TEXT", "content":"이 주머니에서 임의로 2 개의 공을 동시에 꺼낼 때, 흰 공 2 개를 꺼낼 확률을 \\(p\\), 흰 공 1 개와 검은 공 1 개를 꺼낼 확률을 \\(q\\), 검은 공 2 개를 꺼낼 확률을 \\(r\\) 이라 하자."},
  {"order": 3, "type": "TEXT", "content":"\\(p=q\\) 일 때, \\(60 r\\) 의 값을 구하시오. (단, \\(p>0\\))"}
]'::jsonb
, 29
, null
, 6
, 'PROB'
, 4
, 'FRQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"집합 \\(X=\\{-2,-1,0,1,2\\}\\) 에 대하여 다음 조건을 만족시키는 함수 \\(f: X \\rightarrow X\\) 의 개수를 구하시오."},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/prob30p.JPG"}
]'::jsonb
, 30
, null
, 108
, 'PROB'
, 4
, 'FRQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"\\(\\lim _{n \\rightarrow \\infty} \\frac{\\left(\\frac{1}{2}\\right)^n+\\left(\\frac{1}{3}\\right)^{n+1}}{\\left(\\frac{1}{2}\\right)^{n+1}+\\left(\\frac{1}{3}\\right)^n}\\) 의 값은?"}
]'::jsonb
, 23
, '[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
]'::jsonb
, 2
, 'CALC'
, 2
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"곡선 \\(x \\sin 2 y+3 x=3\\) 위의 점 \\(\\left(1, \\frac{\\pi}{2}\\right)\\) 에서의 접선의 기울기는?"}
]'::jsonb
, 24
, '[
  {"order": 1, "content":"\\(\\frac{1}{2}\\)"},
  {"order": 2, "content":"1"},
  {"order": 3, "content":"\\(\\frac{3}{2}\\)"},
  {"order": 4, "content":"2"},
  {"order": 5, "content":"\\(\\frac{5}{2}\\)"}
]'::jsonb
, 3
, 'CALC'
, 3
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"수열 \\(\\left\\{a_n\\right\\}\\) 이"},
  {"order": 2, "type": "TEXT", "content":"\\[\\sum_{n=1}^{\\infty}\\left(a_n-\\frac{3 n^2-n}{2 n^2+1}\\right)=2\\]"},
  {"order": 3, "type": "TEXT", "content":"를 만족시킬 때, \\(\\lim _{n \\rightarrow \\infty}\\left(a_n^2+2 a_n\\right)\\) 의 값은?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content":"\\(\\frac{17}{4}\\)"},
  {"order": 2, "content":"\\(\\frac{19}{4}\\)"},
  {"order": 3, "content":"\\(\\frac{21}{4}\\)"},
  {"order": 4, "content":"\\(\\frac{23}{4}\\)"},
  {"order": 5, "content":"\\(\\frac{25}{4}\\)"}
]'::jsonb
, 3
, 'CALC'
, 3
, 'MCQ'
)
,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"양수 \\(t\\) 에 대하여 곡선 \\(y=e^{x^2}-1(x \\geq 0)\\) 이 두 직선 \\(y=t\\), \\(y=5 t\\) 와 만나는 점을 각각 \\(\\mathrm{A}, \\mathrm{B}\\) 라 하고, 점 B 에서 \\(x\\) 축에 내린 수선의 발을 C 라 하자. 삼각형 ABC 의 넓이를 \\(S(t)\\) 라 할 때, \\(\\lim _{t \\rightarrow 0+} \\frac{S(t)}{t \\sqrt{t}}\\) 의 값은?"},
  {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/calc26p.JPG"}
]'::jsonb
, 26
, '[
  {"order": 1, "content":"\\(\\frac{5}{4}(\\sqrt{5}-1)\\)"},
  {"order": 2, "content":"\\(\\frac{5}{2}(\\sqrt{5}-1)\\)"},
  {"order": 3, "content":"\\(5(\\sqrt{5}-1)\\)"},
  {"order": 4, "content":"\\(\\frac{5}{4}(\\sqrt{5}+1)\\)"},
  {"order": 5, "content":"\\(\\frac{5}{2}(\\sqrt{5}+1)\\)"}
]'::jsonb
, 2
, 'CALC'
, 3
, 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"상수 \\(a(a>1)\\) 과 실수 \\(t(t>0)\\) 에 대하여 곡선 \\(y=a^x\\) 위의 점 \\(\\mathrm{A}\\left(t, a^t\\right)\\) 에서의 접선을 \\(l\\) 이라 하자. 점 A 를 지나고 직선 \\(l\\) 에 수직인 직선이 \\(x\\) 축과 만나는 점을 \\(\\mathrm{B}\\), \\(y\\) 축과 만나는 점을 C 라 하자. \\(\\frac{\\overline{\\mathrm{AC}}}{\\overline{\\mathrm{AB}}}\\) 의 값이 \\(t=1\\) 에서 최대일 때, \\(a\\) 의 값은?"}
]'::jsonb
, 27
, '[
  {"order": 1, "content":"\\(\\sqrt{2}\\)"},
  {"order": 2, "content":"\\(\\sqrt{e}\\)"},
  {"order": 3, "content":"2"},
  {"order": 4, "content":"\\(\\sqrt{2 e}\\)"},
  {"order": 5, "content":"\\(e\\)"}
]'::jsonb
, 2
, 'CALC'
, 3
, 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)\\) 가"},
  {"order": 2, "type": "TEXT", "content":"\\[f(x)= \\begin{cases}(x-a-2)^2 e^x & (x \\geq a) \\\\ e^{2 a}(x-a)+4 e^a & (x<a)\\end{cases}\\]"},
  {"order": 3, "type": "TEXT", "content":"일 때, 실수 \\(t\\) 에 대하여 \\(f(x)=t\\) 를 만족시키는 \\(x\\) 의 최솟값을 \\(g(t)\\) 라 하자."},
  {"order": 4, "type": "TEXT", "content":"함수 \\(g(t)\\) 가 \\(t=12\\) 에서만 불연속일 때, \\(\\frac{g^{\\prime}(f(a+2))}{g^{\\prime}(f(a+6))}\\) 의 값은? (단, \\(a\\) 는 상수이다.)"}
]'::jsonb
, 28
, '[
  {"order": 1, "content":"\\(6 e^4\\)"},
  {"order": 2, "content":"\\(9 e^4\\)"},
  {"order": 3, "content":"\\(12 e^4\\)"},
  {"order": 4, "content":"\\(8 e^6\\)"},
  {"order": 5, "content":"\\(10 e^6\\)"}
]'::jsonb
, 4
, 'CALC'
, 4
, 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"함수 \\(f(x)=\\frac{1}{3} x^3-x^2+\\ln \\left(1+x^2\\right)+a\\) ( \\(a\\) 는 상수 ) 와 두 양수 \\(b, c\\) 에 대하여 함수"},
  {"order": 2, "type": "TEXT", "content":"\\[g(x)=\\left\\{\\begin{array}{cc}\nf(x) & (x \\geq b) \\\\\n-f(x-c) & (x<b)\n\\end{array}\\right.\\]"},
  {"order": 3, "type": "TEXT", "content":"는 실수 전체의 집합에서 미분가능하다. \\(a+b+c=p+q \\ln 2\\) 일 때, \\(30(p+q)\\) 의 값을 구하시오. (단, \\(p, q\\) 는 유리수이고, \\(\\ln 2\\) 는 무리수이다.)"}
]'::jsonb
, 29
, null
, 55
, 'CALC'
, 4
, 'FRQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"함수 \\(y=\\frac{\\sqrt{x}}{10}\\) 의 그래프와 함수 \\(y=\\tan x\\) 의 그래프가 만나는 모든 점의 \\(x\\) 좌표를 작은 수부터 크기순으로 나열할 때, \\(n\\) 번째 수를 \\(a_n\\) 이라 하자."},
  {"order": 2, "type": "TEXT", "content":"\\[\\frac{1}{\\pi^2} \\times \\lim _{n \\rightarrow \\infty} a_n^3 \\tan ^2\\left(a_{n+1}-a_n\\right)\\]"},
  {"order": 3, "type": "TEXT", "content":"의 값을 구하시오."}
]'::jsonb
, 30
, null
, 25
, 'CALC'
, 4
, 'FRQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"두 벡터 \\(\\vec{a}\\) 와 \\(\\vec{b}\\) 에 대하여"},
  {"order": 2, "type": "TEXT", "content":"\\[\\vec{a}+3(\\vec{a}-\\vec{b})=k \\vec{a}-3 \\vec{b}\\]"},
  {"order": 3, "type": "TEXT", "content":"이다. 실수 \\(k\\) 의 값은? (단, \\(\\vec{a} \\neq \\overrightarrow{0}, \\vec{b} \\neq \\overrightarrow{0}\\))"}
]'::jsonb
, 23
, '[
  {"order": 1, "content":"1"},
  {"order": 2, "content":"2"},
  {"order": 3, "content":"3"},
  {"order": 4, "content":"4"},
  {"order": 5, "content":"5"}
]'::jsonb
, 4
, 'GEO'
, 2
, 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"타원 \\(\\frac{x^2}{18}+\\frac{y^2}{b^2}=1\\) 위의 점 \\((3, \\sqrt{5})\\) 에서의 접선의 \\(y\\) 절편은? (단, \\(b\\) 는 양수이다.)"}
]'::jsonb
, 24
, '[
  {"order": 1, "content":"\\(\\frac{3}{2} \\sqrt{5}\\)"},
  {"order": 2, "content":"\\(2 \\sqrt{5}\\)"},
  {"order": 3, "content":"\\(\\frac{5}{2} \\sqrt{5}\\)"},
  {"order": 4, "content":"\\(3 \\sqrt{5}\\)"},
  {"order": 5, "content":"\\(\\frac{7}{2} \\sqrt{5}\\)"}
]'::jsonb
, 2
, 'GEO'
, 3
, 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"좌표평면에서 두 벡터 \\(\\vec{a}=(-3,3), \\vec{b}=(1,-1)\\) 에 대하여 벡터 \\(\\vec{p}\\) 가"},
  {"order": 2, "type": "TEXT", "content":"\\[|\\vec{p}-\\vec{a}|=|\\vec{b}|\\]"},
  {"order": 3, "type": "TEXT", "content":"를 만족시킬 때, \\(|\\vec{p}-\\vec{b}|\\) 의 최솟값은?"}
]'::jsonb
, 25
, '[
  {"order": 1, "content":"\\(\\frac{3}{2} \\sqrt{2}\\)"},
  {"order": 2, "content":"\\(2 \\sqrt{2}\\)"},
  {"order": 3, "content":"\\(\\frac{5}{2} \\sqrt{2}\\)"},
  {"order": 4, "content":"\\(3 \\sqrt{2}\\)"},
  {"order": 5, "content":"\\(\\frac{7}{2} \\sqrt{2}\\)"}
]'::jsonb
, 4
, 'GEO'
, 3
, 'MCQ'
)
     , ((SELECT id FROM exam)
       , '[
  {"order": 1, "type": "TEXT", "content":"쌍곡선 \\(\\frac{x^2}{a^2}-\\frac{y^2}{b^2}=1\\) 의 한 초점 \\(\\mathrm{F}(c, 0)(c>0)\\) 을 지나고 \\(y\\) 축에 평행한 직선이 쌍곡선과 만나는 두 점을 각각 \\(\\mathrm{P}, \\mathrm{Q}\\) 라 하자. 쌍곡선의 한 점근선의 방정식이 \\(y=x\\) 이고 \\(\\overline{\\mathrm{PQ}}=8\\) 일 때, \\(a^2+b^2+c^2\\) 의 값은? (단, \\(a\\) 와 \\(b\\) 는 양수이다.)"}
]'::jsonb
       , 26
       , '[
  {"order": 1, "content":"56"},
  {"order": 2, "content":"60"},
  {"order": 3, "content":"64"},
  {"order": 4, "content":"68"},
  {"order": 5, "content":"72"}
]'::jsonb
       , 3
       , 'GEO'
       , 3
       , 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"그림과 같이 직사각형 ABCD 의 네 변의 중점 \\(\\mathrm{P}, \\mathrm{Q}, \\mathrm{R}, \\mathrm{S}\\) 를 꼭짓점으로 하는 타원의 두 초점을 \\(\\mathrm{F}, \\mathrm{F}^{\\prime}\\) 이라 하자. 점 F 를 초점, 직선 AB 를 준선으로 하는 포물선이 세 점 \\(\\mathrm{F}^{\\prime}, \\mathrm{Q}, \\mathrm{S}\\) 를 지난다. 직사각형 ABCD 의 넓이가 \\(32 \\sqrt{2}\\) 일 때, 선분 \\(\\mathrm{FF}^{\\prime}\\) 의 길이는?"},
  {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2025/m06/geo27p.JPG"}
]'::jsonb
, 27
, '[
  {"order": 1, "content":"\\(\\frac{7}{6} \\sqrt{3}\\)"},
  {"order": 2, "content":"\\(\\frac{4}{3} \\sqrt{3}\\)"},
  {"order": 3, "content":"\\(\\frac{3}{2} \\sqrt{3}\\)"},
  {"order": 4, "content":"\\(\\frac{5}{3} \\sqrt{3}\\)"},
  {"order": 5, "content":"\\(\\frac{11}{6} \\sqrt{3}\\)"}
]'::jsonb
, 2
, 'GEO'
, 3
, 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"좌표평면에서 두 점 \\(\\mathrm{A}(1,0), \\mathrm{B}(1,1)\\) 에 대하여 두 점 \\(\\mathrm{P}, \\mathrm{Q}\\) 가"},
  {"order": 2, "type": "TEXT", "content":"\\[|\\overrightarrow{\\mathrm{OP}}|=1, \\quad|\\overrightarrow{\\mathrm{BQ}}|=3, \\quad \\overrightarrow{\\mathrm{AP}} \\cdot(\\overrightarrow{\\mathrm{QA}}+\\overrightarrow{\\mathrm{QP}})=0\\]"},
  {"order": 3, "type": "TEXT", "content":"을 만족시킨다. \\(|\\overrightarrow{\\mathrm{PQ}}|\\) 의 값이 최소가 되도록 하는 두 점 \\(\\mathrm{P}, \\mathrm{Q}\\) 에 대하여 \\(\\overrightarrow{\\mathrm{AP}} \\cdot \\overrightarrow{\\mathrm{BQ}}\\) 의 값은?"},
  {"order": 4, "type": "TEXT", "content":"(단, O 는 원점이고, \\(|\\overrightarrow{\\mathrm{AP}}|>0\\) 이다.)"}
]'::jsonb
, 28
, '[
  {"order": 1, "content":"\\(\\frac{6}{5}\\)"},
  {"order": 2, "content":"\\(\\frac{9}{5}\\)"},
  {"order": 3, "content":"\\(\\frac{12}{5}\\)"},
  {"order": 4, "content":"3"},
  {"order": 5, "content":"\\(\\frac{18}{5}\\)"}
]'::jsonb
, 3
, 'GEO'
, 4
, 'MCQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"좌표평면에 곡선 \\(\\left|y^2-1\\right|=\\frac{x^2}{a^2}\\) 과 네 점 \\(\\mathrm{A}(0, c+1)\\), \\(\\mathrm{B}(0,-c-1)\\), \\(\\mathrm{C}(c, 0)\\), \\(\\mathrm{D}(-c, 0)\\) 이 있다. 곡선 위의 점 중 \\(y\\) 좌표의 절댓값이 1 보다 작거나 같은 모든 점 P 에 대하여 \\(\\overline{\\mathrm{PC}}+\\overline{\\mathrm{PD}}=\\sqrt{5}\\) 이다. 곡선 위의 점 Q 가 제 1 사분면에 있고 \\(\\overline{\\mathrm{AQ}}=10\\) 일 때, 삼각형 ABQ 의 둘레의 길이를 구하시오. (단, \\(a\\) 와 \\(c\\) 는 양수이다.)"}
]'::jsonb
, 29
, null
, 25
, 'GEO'
, 4
, 'FRQ'
)
     ,
((SELECT id FROM exam)
, '[
  {"order": 1, "type": "TEXT", "content":"두 초점이 \\(\\mathrm{F}(5,0), \\mathrm{F}^{\\prime}(-5,0)\\) 이고, 주축의 길이가 6 인 쌍곡선이 있다. 쌍곡선 위의 \\(\\overline{\\mathrm{PF}}<\\overline{\\mathrm{PF}^{\\prime}}\\) 인 점 P 에 대하여 점 Q 가"},
  {"order": 2, "type": "TEXT", "content":"\\[(|\\overrightarrow{\\mathrm{FP}}|+1) \\overrightarrow{\\mathrm{F}^{\\prime} \\mathrm{Q}}=5 \\overrightarrow{\\mathrm{QP}}\\]"},
  {"order": 3, "type": "TEXT", "content":"를 만족시킨다. 점 \\(\\mathrm{A}(-9,-3)\\) 에 대하여 \\(|\\overrightarrow{\\mathrm{AQ}}|\\) 의 최댓값을 구하시오."}
]'::jsonb
, 30
, null
, 10
, 'GEO'
, 4
, 'FRQ'
)


;




