insert into exams (exam_year
                  , exam_type
                  , name
                  , quantity
                  , time_limit)
values (2026
       , 'M06'
       , '6월 모의평가'
       , 30
       , 6000);


WITH exam AS (
    SELECT id FROM exams WHERE exam_year = 2026 AND exam_type = 'M06' LIMIT 1
    )
insert into questions (exam_id
                      , passages
                      , question_no
                      , options
                      , answer
                      , category
                      , point
                      , question_type)
values
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "\\(4^{\\frac{1}{4}} \\times 2^{\\frac{1}{2}}\\) 의 값은?"}
          ]'::jsonb
              , 1
              , '[
            {"order": 1, "content": "1"}
          , {"order": 2, "content": "2"}
          , {"order": 3, "content": "3"}
          , {"order": 4, "content": "4"}
          , {"order": 5, "content": "5"}
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
            {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=x^2-x+1\\) 에 대하여 \\(\\lim _{h \\rightarrow 0} \\frac{f(1+h)-f(1)}{h}\\) 의 값은?"}
          ]'::jsonb
              , 2
              , '[
            {"order": 1, "content": "1"}
          , {"order": 2, "content": "2"}
          , {"order": 3, "content": "3"}
          , {"order": 4, "content": "4"}
          , {"order": 5, "content": "5"}
          ]'::jsonb
              , 1
              , 'ALG'
              , 2
              , 'MCQ'
              )

     , (
        (SELECT id FROM exam)
              , '[
  {"order": 1, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 \\(\\sum_{k=1}^7 a_k=8\\) 일 때, \\(\\sum_{k=1}^7\\left(2 a_k+1\\right)\\) 의 값은?"}
]'::jsonb
              , 3
              , '[
  {"order": 1, "content": "21"}
, {"order": 2, "content": "22"}
, {"order": 3, "content": "23"}
, {"order": 4, "content": "24"}
, {"order": 5, "content": "25"}
]'::jsonb
              , 3
              , 'ALG'
              , 3
              , 'MCQ'
              )

       , (
          (SELECT id FROM exam)
              , '[
  {"order": 1, "type": "TEXT", "content": "함수"}
, {"order": 2, "type": "TEXT", "content": "\\[\\nf(x)=\\left\\{\\begin{array}{rr}\\n-x^2+a & (x<3) \\\\\\n5 x-a & (x \\geq 3)\\n\\end{array}\\right.\\n\\]"}
, {"order": 3, "type": "TEXT", "content": "이 실수 전체의 집합에서 연속일 때, 상수 \\(a\\) 의 값은?"}
]'::jsonb
              , 4
              , '[
  {"order": 1, "content": "10"}
, {"order": 2, "content": "11"}
, {"order": 3, "content": "12"}
, {"order": 4, "content": "13"}
, {"order": 5, "content": "14"}
]'::jsonb
              , 3
              , 'ALG'
              , 3
              , 'MCQ'
              )

       , (
          (SELECT id FROM exam)
              , '[
  {"order": 1, "type": "TEXT", "content": "\\(\\int_0^2\\left(6 x^2-2 x+1\\right) d x\\) 의 값은?"}
]'::jsonb
              , 5
              , '[
  {"order": 1, "content": "12"}
, {"order": 2, "content": "14"}
, {"order": 3, "content": "16"}
, {"order": 4, "content": "18"}
, {"order": 5, "content": "20"}
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
            {"order": 1, "type": "TEXT", "content": "두 양수 \\(a, b\\) 에 대하여 함수 \\(f(x)=a \\cos b x+1\\) 의 최댓값이 8 이고 주기가 \\(\\pi\\) 일 때, \\(a+b\\) 의 값은?"}
          ]'::jsonb
              , 6
              , '[
            {"order": 1, "content": "\\(\\frac{15}{2}\\)"}
          , {"order": 2, "content": "8"}
          , {"order": 3, "content": "\\(\\frac{17}{2}\\)"}
          , {"order": 4, "content": "9"}
          , {"order": 5, "content": "\\(\\frac{19}{2}\\)"}
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
            {"order": 1, "type": "TEXT", "content": "다항함수 \\(f(x)\\) 에 대하여 함수 \\(g(x)\\) 를"}
          , {"order": 2, "type": "TEXT", "content": "\\[\\ng(x)=5 x^2+x f(x)\\n\\]"}
          , {"order": 3, "type": "TEXT", "content": "라 하자. \\(f(3)=2, f^{\\prime}(3)=1\\) 일 때, \\(g^{\\prime}(3)\\) 의 값은?"}
          ]'::jsonb
              , 7
              , '[
            {"order": 1, "content": "31"}
          , {"order": 2, "content": "32"}
          , {"order": 3, "content": "33"}
          , {"order": 4, "content": "34"}
          , {"order": 5, "content": "35"}
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
            {"order": 1, "type": "TEXT", "content": "\\(\\sin (\\pi-\\theta)>0\\) 이고 \\(2 \\cos \\theta=\\sin \\theta\\) 일 때, \\(\\cos \\theta\\) 의 값은?"}
          ]'::jsonb
              , 8
              , '[
            {"order": 1, "content": "\\(-\\frac{\\sqrt{5}}{5}\\)"}
          , {"order": 2, "content": "\\(-\\frac{\\sqrt{5}}{10}\\)"}
          , {"order": 3, "content": "0"}
          , {"order": 4, "content": "\\(\\frac{\\sqrt{5}}{10}\\)"}
          , {"order": 5, "content": "\\(\\frac{\\sqrt{5}}{5}\\)"}
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
  {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=x^2+a x\\) 에 대하여"}
, {"order": 2, "type": "TEXT", "content": "\\[\\n\\int_{-3}^3(x+1) f(x) d x=36+\\int_{-3}^3 f(x) d x\\n\\]"}
, {"order": 3, "type": "TEXT", "content": "일 때, 상수 \\(a\\) 의 값은?"}
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

       ,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "실수 \\(a(a>1)\\) 에 대하여"}
          , {"order": 2, "type": "TEXT", "content": "곡선 \\(y=\\log _a(x+3)\\) 이 곡선 \\(y=\\log _a(-x+3)\\) 과 만나는 점을 A , 곡선 \\(y=\\log _a(x+3)\\) 이 \\(x\\) 축과 만나는 점을 B , 곡선 \\(y=\\log _a(-x+3)\\) 이 \\(x\\) 축과 만나는 점을 C 라 하자. 삼각형 ABC 가 정삼각형일 때, \\(a\\) 의 값은?"}
          ]'::jsonb
              , 10
              , '[
            {"order": 1, "content": "\\(3^{\\frac{\\sqrt{3}}{6}}\\)"}
          , {"order": 2, "content": "\\(3^{\\frac{\\sqrt{3}}{4}}\\)"}
          , {"order": 3, "content": "\\(3^{\\frac{\\sqrt{3}}{3}}\\)"}
          , {"order": 4, "content": "\\(3^{\\frac{5 \\sqrt{3}}{12}}\\)"}
          , {"order": 5, "content": "\\(3^{\\frac{\\sqrt{3}}{2}}\\)"}
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
            {"order": 1, "type": "TEXT", "content": "시각 \\(t=0\\) 일 때 출발하여 수직선 위를 움직이는 점 P 가 있다. 시각이 \\(t(t \\geq 0)\\) 일 때 점 P 의 위치 \\(x\\) 가"}
          , {"order": 2, "type": "TEXT", "content": "\\[\\nx=t^3-t^2-t+1\\n\\]"}
          , {"order": 3, "type": "TEXT", "content": "이다. <보기>에서 옳은 것만을 있는 대로 고른 것은?"}
          , {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg11p.JPG"}
          ]'::jsonb
              , 11
              , '[
            {"order": 1, "content": "ᄀ"}
          , {"order": 2, "content": "ᄂ"}
          , {"order": 3, "content": "ᄃ"}
          , {"order": 4, "content": "ᄀ, ᄃ"}
          , {"order": 5, "content": "ᄂ, ᄃ"}
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
            {"order": 1, "type": "TEXT", "content": "다음 조건을 만족시키는 모든 수열 \\(\\left\\{a_n\\right\\}\\) 에 대하여 \\(a_4\\) 의 최댓값은?"}
          , {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg12p.JPG"}
          ]'::jsonb
              , 12
              , '[
            {"order": 1, "content": "9"}
          , {"order": 2, "content": "12"}
          , {"order": 3, "content": "15"}
          , {"order": 4, "content": "18"}
          , {"order": 5, "content": "21"}
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
            {"order": 1, "type": "TEXT", "content": "그림과 같이 함수 \\(f(x)=3 x^2-7 x+2\\) 에 대하여 곡선 \\(y=f(x)\\) 와 직선 \\(y=\\frac{1}{3} x-\\frac{2}{3}\\) 및 \\(y\\) 축으로 둘러싸인 영역을 \\(A\\), 곡선 \\(y=f(x)\\) 와 직선 \\(y=\\frac{1}{3} x-\\frac{2}{3}\\) 로 둘러싸인 영역을 \\(B\\), 곡선 \\(y=f(x)\\) 와 두 직선 \\(y=\\frac{1}{3} x-\\frac{2}{3}, x=k(k>2)\\) 로 둘러싸인 영역을 \\(C\\) 라 하자."}
          , {"order": 2, "type": "TEXT", "content": "\\[\\n(A \\text { 의 넓이 })+(C \\text { 의 넓이 })=(B \\text { 의 넓이 })\\n\\]"}
          , {"order": 3, "type": "TEXT", "content": "일 때, 상수 \\(k\\) 의 값은?"}
          , {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg13p.JPG"}
          ]'::jsonb
              , 13
              , '[
            {"order": 1, "content": "\\(\\frac{29}{12}\\)"}
          , {"order": 2, "content": "\\(\\frac{5}{2}\\)"}
          , {"order": 3, "content": "\\(\\frac{31}{12}\\)"}
          , {"order": 4, "content": "\\(\\frac{8}{3}\\)"}
          , {"order": 5, "content": "\\(\\frac{11}{4}\\)"}
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
            {"order": 1, "type": "TEXT", "content": "\\(\\overline{\\mathrm{AB}}=2 \\sqrt{7}\\) 인 삼각형 ABC 에서 선분 BC 의 중점을 P , 선분 BC 를 \\(5: 1\\) 로 내분하는 점을 Q 라 하자."}
          , {"order": 2, "type": "TEXT", "content": "\\[\\n\\overline{\\mathrm{AQ}}=3 \\sqrt{2}, \\sin (\\angle \\mathrm{QAP}): \\sin (\\angle \\mathrm{APQ})=\\sqrt{2}: 3\\n\\]"}
          , {"order": 3, "type": "TEXT", "content": "일 때, 삼각형 ABC 의 외접원의 넓이는?"}
          , {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg14p.JPG"}
          ]'::jsonb
              , 14
              , '[
            {"order": 1, "content": "\\(\\frac{85}{9} \\pi\\)"}
          , {"order": 2, "content": "\\(\\frac{88}{9} \\pi\\)"}
          , {"order": 3, "content": "\\(\\frac{91}{9} \\pi\\)"}
          , {"order": 4, "content": "\\(\\frac{94}{9} \\pi\\)"}
          , {"order": 5, "content": "\\(\\frac{97}{9} \\pi\\)"}
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
            {"order": 1, "type": "TEXT", "content": "상수 \\(k\\) 와 \\(f^{\\prime}(0)=6\\) 인 삼차함수 \\(f(x)\\) 에 대하여 함수"}
          , {"order": 2, "type": "TEXT", "content": "\\[\\ng(x)= \\begin{cases}f(x)+k & (|x|>1) \\\\ -f(x) & (|x| \\leq 1)\\end{cases}\\n\\]"}
          , {"order": 3, "type": "TEXT", "content": "이 다음 조건을 만족시킬 때, \\(k+f\\left(\\frac{1}{2}\\right)\\) 의 값은?"}
          , {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg15p.JPG"}
          ]'::jsonb
              , 15
              , '[
            {"order": 1, "content": "\\(\\frac{15}{4}\\)"}
          , {"order": 2, "content": "\\(\\frac{27}{4}\\)"}
          , {"order": 3, "content": "\\(\\frac{39}{4}\\)"}
          , {"order": 4, "content": "\\(\\frac{51}{4}\\)"}
          , {"order": 5, "content": "\\(\\frac{63}{4}\\)"}
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
            {"order": 1, "type": "TEXT", "content": "방정식 \\(\\log _5(x+1)+\\log _5(x-1)=\\log _{25} 9\\) 를 만족시키는 실수 \\(x\\) 의 값을 구하시오."}
          ]'::jsonb
              , 16
              , null
              , 2
              , 'ALG'
              , 3
              , 'FRQ'
              )

     ,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "다항함수 \\(f(x)\\) 에 대하여 \\(f^{\\prime}(x)=3 x^2+4 x\\) 이고 \\(f(0)=3\\) 일 때, \\(f(1)\\) 의 값을 구하시오."}
          ]'::jsonb
              , 17
              , null
              , 6
              , 'ALG'
              , 3
              , 'FRQ'
              )

,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "\\(\\sum_{k=1}^6\\left(k^2+2 k\\right)\\) 의 값을 구하시오."}
          ]'::jsonb
              , 18
              , null
              , 133
              , 'ALG'
              , 3
              , 'FRQ'
              )

     ,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "상수 \\(a\\) 에 대하여 함수 \\(f(x)=3 x^3-9 x^2+a\\) 의 극댓값이 20 일 때, 함수 \\(f(x)\\) 의 극솟값을 구하시오."}
          ]'::jsonb
              , 19
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
            {"order": 1, "type": "TEXT", "content": "실수 전체의 집합에서 정의된 함수 \\(f(x)\\) 가 다음 조건을 만족시킨다."}
          , {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg20p.JPG"}
          , {"order": 3, "type": "TEXT", "content": "방정식 \\(f(f(x))=f(x)\\) 의 0 이상인 모든 실근을 작은 수부터 크기순으로 나열할 때, \\(n\\) 번째 수를 \\(a_n\\) 이라 하자. 다음은 \\(a_{20}+a_{21}+a_{22}\\) 의 값을 구하는 과정이다."}
          , {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg20p2.JPG"}
          , {"order": 5, "type": "TEXT", "content": "위의 (가), (나), (다)에 알맞은 수를 각각 \\(p, q, r\\) 이라 할 때, \\(p+q+r\\) 의 값을 구하시오."}
          ]'::jsonb
              , 20
              , null
              , 85
              , 'ALG'
              , 4
              , 'FRQ'
              )

     ,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=(x-1)(x-2)\\) 와 최고차항의 계수가 1 인 사차함수 \\(g(x)\\) 가 다음 조건을 만족시킨다."}
          , {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg21p.JPG"}
          , {"order": 3, "type": "TEXT", "content": "\\(g(-1)\\) 의 값을 구하시오."}
          ]'::jsonb
              , 21
              , null
              , 42
              , 'ALG'
              , 4
              , 'FRQ'
              )

     ,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "\\(k>1\\) 인 실수 \\(k\\) 에 대하여 두 곡선"}
          , {"order": 2, "type": "TEXT", "content": "\\[\\ny=2^x+\\frac{k}{2}, \\quad y=k \\times\\left(\\frac{1}{2}\\right)^x+k-2\\n\\]"}
          , {"order": 3, "type": "TEXT", "content": "가 만나는 점을 A 라 하고, 점 A 를 지나고 기울기가 -1 인 직선이 곡선 \\(y=2^{x-2}-3\\) 과 만나는 점을 B 라 하자."}
          , {"order": 4, "type": "TEXT", "content": "삼각형 AOB 의 넓이가 16 일 때, \\(k+\\log _2 k=\\frac{q}{p}\\) 이다."}
          , {"order": 5, "type": "TEXT", "content": "\\(p+q\\) 의 값을 구하시오. (단, O 는 원점이고, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
          ]'::jsonb
              , 22
              , null
              , 38
              , 'ALG'
              , 4
              , 'FRQ'
              )

,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "6 개의 문자 \\(a, a, a, a, b, c\\) 를 모두 일렬로 나열하는 경우의 수는?"}
          ]'::jsonb
              , 23
              , '[
            {"order": 1, "content": "18"}
          , {"order": 2, "content": "24"}
          , {"order": 3, "content": "30"}
          , {"order": 4, "content": "36"}
          , {"order": 5, "content": "42"}
          ]'::jsonb
              , 3
              , 'PROB'
              , 2
              , 'MCQ'
              )

     ,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "두 사건 \\(A\\) 와 \\(B\\) 는 서로 배반사건이고"}
          , {"order": 2, "type": "TEXT", "content": "\\[\\n\\mathrm{P}(A \\cup B)=1, \\mathrm{P}\\left(A^C\\right)=2 \\mathrm{P}(A)\\n\\]"}
          , {"order": 3, "type": "TEXT", "content": "일 때, \\(\\mathrm{P}(B)\\) 의 값은?"}
          ]'::jsonb
              , 24
              , '[
            {"order": 1, "content": "\\(\\frac{1}{6}\\)"}
          , {"order": 2, "content": "\\(\\frac{1}{3}\\)"}
          , {"order": 3, "content": "\\(\\frac{1}{2}\\)"}
          , {"order": 4, "content": "\\(\\frac{2}{3}\\)"}
          , {"order": 5, "content": "\\(\\frac{5}{6}\\)"}
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
            {"order": 1, "type": "TEXT", "content": "다항식 \\((2 x-1)^5(x+1)\\) 의 전개식에서 \\(x^3\\) 의 계수는?"}
          ]'::jsonb
              , 25
              , '[
            {"order": 1, "content": "30"}
          , {"order": 2, "content": "35"}
          , {"order": 3, "content": "40"}
          , {"order": 4, "content": "45"}
          , {"order": 5, "content": "50"}
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
            {"order": 1, "type": "TEXT", "content": "숫자 \\(1,2,3,4,5,6,7\\) 이 하나씩 적혀 있는 7 장의 카드가 있다. 이 7 장의 카드를 모두 한 번씩 사용하여 일렬로 임의로 나열할 때, 양 끝에 놓인 카드에 적힌 두 수의 곱이 짝수가 되도록 카드가 놓일 확률은?"}
          , {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/prob26p.JPG"}
          ]'::jsonb
              , 26
              , '[
            {"order": 1, "content": "\\(\\frac{3}{7}\\)"}
          , {"order": 2, "content": "\\(\\frac{1}{2}\\)"}
          , {"order": 3, "content": "\\(\\frac{4}{7}\\)"}
          , {"order": 4, "content": "\\(\\frac{9}{14}\\)"}
          , {"order": 5, "content": "\\(\\frac{5}{7}\\)"}
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
            {"order": 1, "type": "TEXT", "content": "5 명이 둘러앉을 수 있는 원 모양의 탁자와 남학생 5 명, 여학생 3 명이 있다. 이 8 명의 학생 중에서 4 명 이상의 남학생을 포함하여 5 명의 학생을 선택하고 이 5 명의 학생 모두를 일정한 간격으로 탁자에 둘러앉게 하는 경우의 수는? (단, 회전하여 일치하는 것은 같은 것으로 본다.)"}
          , {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/prob27p.JPG"}
          ]'::jsonb
              , 27
              , '[
            {"order": 1, "content": "384"}
          , {"order": 2, "content": "408"}
          , {"order": 3, "content": "432"}
          , {"order": 4, "content": "456"}
          , {"order": 5, "content": "480"}
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
            {"order": 1, "type": "TEXT", "content": "공 15 개와 비어 있는 세 상자 \\(\\mathrm{A}, \\mathrm{B}, \\mathrm{C}\\) 가 있다. 한 개의 주사위를 사용하여 다음 규칙에 따라 세 상자 \\(\\mathrm{A}, \\mathrm{B}, \\mathrm{C}\\) 에 공을 넣는 시행을 한다."}
          , {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/prob28p.JPG"}
          , {"order": 3, "type": "TEXT", "content": "이 시행을 5 번 반복한 후 상자 B 에 들어 있는 공의 개수가 홀수일 때, 상자 A 에 들어 있는 공의 개수와 상자 C 에 들어 있는 공의 개수의 합이 8 이상일 확률은?"}
          , {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/prob28p2.JPG"}
          ]'::jsonb
              , 28
              , '[
            {"order": 1, "content": "\\(\\frac{44}{61}\\)"}
          , {"order": 2, "content": "\\(\\frac{47}{61}\\)"}
          , {"order": 3, "content": "\\(\\frac{50}{61}\\)"}
          , {"order": 4, "content": "\\(\\frac{53}{61}\\)"}
          , {"order": 5, "content": "\\(\\frac{56}{61}\\)"}
          ]'::jsonb
              , 5
              , 'PROB'
              , 4
              , 'MCQ'
              )

     ,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "한 개의 주사위를 세 번 던져서 나오는 눈의 수를 차례로 \\(a, b, c\\) 라 할 때, \\(a+b=8\\) 또는 \\(b \\geq c\\) 일 확률은 \\(\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
          ]'::jsonb
              , 29
              , null
              , 44
              , 'PROB'
              , 4
              , 'FRQ'
              )

     ,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "집합 \\(X=\\{1,2,3,4,5\\}\\) 에 대하여 다음 조건을 만족시키는 함수 \\(f: X \\rightarrow X\\) 의 개수를 구하시오."}
          , {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/prob30p.JPG"}
          ]'::jsonb
              , 30
              , null
              , 115
              , 'PROB'
              , 4
              , 'FRQ'
              )

     ,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "\\(\\lim _{n \\rightarrow \\infty} \\frac{4 \\times 3^{n+1}}{2^n+3^n}\\) 의 값은?"}
          ]'::jsonb
              , 23
              , '[
            {"order": 1, "content": "12"}
          , {"order": 2, "content": "13"}
          , {"order": 3, "content": "14"}
          , {"order": 4, "content": "15"}
          , {"order": 5, "content": "16"}
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
            {"order": 1, "type": "TEXT", "content": "곡선 \\(3 x+y+\\cos (x y)=2\\) 위의 점 \\((0,1)\\) 에서의 접선의 \\(x\\) 절편은?"}
          ]'::jsonb
              , 24
              , '[
            {"order": 1, "content": "\\(\\frac{1}{6}\\)"}
          , {"order": 2, "content": "\\(\\frac{1}{3}\\)"}
          , {"order": 3, "content": "\\(\\frac{1}{2}\\)"}
          , {"order": 4, "content": "\\(\\frac{2}{3}\\)"}
          , {"order": 5, "content": "\\(\\frac{5}{6}\\)"}
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
            {"order": 1, "type": "TEXT", "content": "양수 \\(a\\) 에 대하여 급수 \\(\\sum_{n=1}^{\\infty}\\left(\\frac{a-3 n}{n}+\\frac{a n+6}{n+a}\\right)\\) 이 실수 \\(S\\) 에 수렴할 때, \\(a+S\\) 의 값은?"}
          ]'::jsonb
              , 25
              , '[
            {"order": 1, "content": "7"}
          , {"order": 2, "content": "\\(\\frac{15}{2}\\)"}
          , {"order": 3, "content": "8"}
          , {"order": 4, "content": "\\(\\frac{17}{2}\\)"}
          , {"order": 5, "content": "9"}
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
            {"order": 1, "type": "TEXT", "content": "함수 \\(f(x)=e^{3 x}-3 e^{2 x}+4 e^x\\) 의 역함수를 \\(g(x)\\) 라 하자. \\(g^{\\prime}(a)=\\frac{1}{8}\\) 이 되도록 하는 실수 \\(a\\) 에 대하여 \\(a+f^{\\prime}(g(a))\\) 의 값은?"}
          ]'::jsonb
              , 26
              , '[
            {"order": 1, "content": "11"}
          , {"order": 2, "content": "12"}
          , {"order": 3, "content": "13"}
          , {"order": 4, "content": "14"}
          , {"order": 5, "content": "15"}
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
            {"order": 1, "type": "TEXT", "content": "그림과 같이 길이가 2 인 선분 AB 를 지름으로 하는 반원의 호 AB 위의 점 P 에 대하여 \\(\\angle \\mathrm{BAP}=\\theta\\left(\\frac{\\pi}{4}<\\theta<\\frac{\\pi}{2}\\right)\\) 라 하고, 점 P 를 지나고 선분 AB 에 평행한 직선이 호 AB 와 만나는 점 중 P 가 아닌 점을 Q 라 하자. 사각형 ABQP 의 넓이를 \\(f(\\theta)\\) 라 하고, \\(\\overline{\\mathrm{AP}}: \\overline{\\mathrm{BP}}=1: 3\\) 이 되도록 하는 \\(\\theta\\) 의 값을 \\(a\\) 라 할 때, \\(f^{\\prime}(a)\\) 의 값은?"}
          , {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/calc27p.JPG"}
          ]'::jsonb
              , 27
              , '[
            {"order": 1, "content": "\\(-\\frac{64}{25}\\)"}
          , {"order": 2, "content": "\\(-\\frac{59}{25}\\)"}
          , {"order": 3, "content": "\\(-\\frac{54}{25}\\)"}
          , {"order": 4, "content": "\\(-\\frac{49}{25}\\)"}
          , {"order": 5, "content": "\\(-\\frac{44}{25}\\)"}
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
            {"order": 1, "type": "TEXT", "content": "실수 전체의 집합에서 이계도함수를 갖는 함수 \\(f(x)\\) 와 두 상수 \\(a, b\\) 가 다음 조건을 만족시킬 때, \\(a \\times e^b\\) 의 값은?"}
          , {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/calc28p.JPG"}
          ]'::jsonb
              , 28
              , '[
            {"order": 1, "content": "\\(-3 e^{-\\frac{4}{3}}\\)"}
          , {"order": 2, "content": "\\(-\\frac{5}{3} e^{-\\frac{4}{3}}\\)"}
          , {"order": 3, "content": "\\(-\\frac{1}{3} e^{-\\frac{4}{3}}\\)"}
          , {"order": 4, "content": "\\(e^{-\\frac{4}{3}}\\)"}
          , {"order": 5, "content": "\\(\\frac{7}{3} e^{-\\frac{4}{3}}\\)"}
          ]'::jsonb
              , 1
              , 'CALC'
              , 4
              , 'MCQ'
              )

       ,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "두 정수 \\(\\alpha, \\beta(\\alpha>\\beta)\\) 에 대하여 다음 조건을 만족시키는 수열 \\(\\left\\{a_n\\right\\}\\) 이 있다."}
          , {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/calc29p.JPG"}
          , {"order": 3, "type": "TEXT", "content": "수열 \\(\\left\\{a_n\\right\\}\\) 과 \\(b_1>0\\) 인 등비수열 \\(\\left\\{b_n\\right\\}\\) 에 대하여"}
          , {"order": 4, "type": "TEXT", "content": "\\[\\n\\sum_{n=1}^{\\infty}\\left(a_{4 n-2} b_n\\right)=\\sum_{n=1}^{\\infty}\\left(a_{4 n-3} b_{2 n}\\right)=6\\n\\]"}
          , {"order": 5, "type": "TEXT", "content": "일 때, \\(b_1 \\times b_3=\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
          ]'::jsonb
              , 29
              , null
              , 109
              , 'CALC'
              , 4
              , 'FRQ'
              )


       ,
          (
           (SELECT id FROM exam)
              , '[
            {"order": 1, "type": "TEXT", "content": "최고차항의 계수가 1 인 삼차함수 \\(f(x)\\) 에 대하여 함수"}
          , {"order": 2, "type": "TEXT", "content": "\\[\\ng(x)=\\left|f\\left(\\frac{2}{1+e^{-x}}\\right)\\right|\\n\\]"}
          , {"order": 3, "type": "TEXT", "content": "가 실수 전체의 집합에서 미분가능하고 다음 조건을 만족시킨다."}
          , {"order": 4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/calc30p.JPG"}
          , {"order": 5, "type": "TEXT", "content": "\\(g(0)\\) 의 최솟값을 \\(\\frac{q}{p}\\) 라 할 때, \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}
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
            {"order": 1, "type": "TEXT", "content": "두 벡터 \\(\\vec{a}=(2,6), \\vec{b}=(k,-6)\\) 에 대하여 \\(\\vec{a}+\\vec{b}\\) 의 모든 성분의 합이 4 일 때, \\(k\\) 의 값은?"}
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
            {"order": 1, "type": "TEXT", "content": "포물선 \\(y^2=12 x\\) 위의 점 \\((3,6)\\) 에서의 접선이 점 \\((1, a)\\) 를 지날 때, \\(a\\) 의 값은?"}
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
            {"order": 1, "type": "TEXT", "content": "좌표평면 위의 두 점 \\(\\mathrm{A}(4,0), \\mathrm{B}(2,-4)\\) 에 대하여 점 A 를 지나고 법선벡터가 \\(\\overrightarrow{\\mathrm{AB}}\\) 인 직선의 \\(y\\) 절편은?"}
          ]'::jsonb
              , 25
              , '[
            {"order": 1, "content": "1"}
          , {"order": 2, "content": "2"}
          , {"order": 3, "content": "3"}
          , {"order": 4, "content": "4"}
          , {"order": 5, "content": "5"}
          ]'::jsonb
              , 2
              , 'GEO'
              , 3
              , 'MCQ'
              )

       , (
          (SELECT id FROM exam)
              , '[
  {"order": 1, "type": "TEXT", "content": "쌍곡선 \\(\\frac{x^2}{a^2}-\\frac{y^2}{b^2}=1\\) 의 한 점근선의 방정식이 \\(y=\\frac{1}{2} x\\) 이다. 쌍곡선이 직선 \\(y=1\\) 과 만나는 두 점을 각각 \\(\\mathrm{P}, \\mathrm{Q}\\) 라 하자. 쌍곡선 위의 점 P 에서의 접선과 쌍곡선 위의 점 Q 에서의 접선이 서로 수직일 때, \\(a^2+b^2\\) 의 값은? (단, \\(a, b\\) 는 양수이다.)"}
]'::jsonb
              , 26
              , '[
  {"order": 1, "content": "15"}
, {"order": 2, "content": "\\(\\frac{35}{2}\\)"}
, {"order": 3, "content": "20"}
, {"order": 4, "content": "\\(\\frac{45}{2}\\)"}
, {"order": 5, "content": "25"}
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
            {"order": 1, "type": "TEXT", "content": "삼각형 OAB 에 대하여 \\(\\overrightarrow{\\mathrm{OA}}=\\vec{a}, \\overrightarrow{\\mathrm{OB}}=\\vec{b}\\) 라 하자."}
          , {"order": 2, "type": "TEXT", "content": "\\[\\n|\\vec{a}+\\vec{b}|=6,|2 \\vec{a}-\\vec{b}|=9,(\\vec{a}+\\vec{b}) \\cdot(\\vec{a}-\\vec{b})=0\\n\\]"}
          , {"order": 3, "type": "TEXT", "content": "일 때, 삼각형 OAB 의 넓이는?"}
          ]'::jsonb
              , 27
              , '[
            {"order": 1, "content": "\\(4 \\sqrt{2}\\)"}
          , {"order": 2, "content": "\\(5 \\sqrt{2}\\)"}
          , {"order": 3, "content": "\\(6 \\sqrt{2}\\)"}
          , {"order": 4, "content": "\\(7 \\sqrt{2}\\)"}
          , {"order": 5, "content": "\\(8 \\sqrt{2}\\)"}
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
            {"order": 1, "type": "TEXT", "content": "그림과 같이 두 점 \\(\\mathrm{F}(c, 0), \\mathrm{F}^{\\prime}(-c, 0)(c>0)\\) 을 초점으로 하는 타원 \\(C_1: \\frac{x^2}{a^2}+y^2=1\\) 과 두 점 \\(\\mathrm{G}(0, d), \\mathrm{G}^{\\prime}(0,-d)(d>1)\\) 을 초점으로 하고 타원 \\(C_1\\) 의 두 꼭짓점을 지나는 타원 \\(C_2\\) 가 있다. 직선 FG 가 타원 \\(C_1\\) 과 제 1 사분면에서 만나는 점을 P 라 하고, 직선 \\(\\mathrm{F}^{\\prime} \\mathrm{P}\\) 가 타원 \\(C_2\\) 와 제 1 사분면에서 만나는 점을 Q 라 하자. \\(\\overline{\\mathrm{GP}}=\\overline{\\mathrm{PF}}\\) 이고 \\(\\overline{\\mathrm{GP}}+\\overline{\\mathrm{PF}^{\\prime}}=2 \\sqrt{2}\\) 일 때, \\(\\overline{\\mathrm{QG}}+\\overline{\\mathrm{QG}^{\\prime}}\\) 의 값은? (단, \\(a\\) 는 양수이다.)"}
          , {"order": 2, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/geo28p.JPG"}
          ]'::jsonb
              , 28
              , '[
            {"order": 1, "content": "\\(\\sqrt{19}\\)"}
          , {"order": 2, "content": "\\(2 \\sqrt{5}\\)"}
          , {"order": 3, "content": "\\(\\sqrt{21}\\)"}
          , {"order": 4, "content": "\\(\\sqrt{22}\\)"}
          , {"order": 5, "content": "\\(\\sqrt{23}\\)"}
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
            {"order": 1, "type": "TEXT", "content": "그림과 같이 두 점 \\(\\mathrm{F}(c, 0), \\mathrm{F}^{\\prime}(-c, 0)(c>0)\\) 을 초점으로 하는 쌍곡선이 있다. 이 쌍곡선 위의 점 중 제 1 사분면에 있는 점 P 에 대하여 선분 \\(\\mathrm{F}^{\\prime} \\mathrm{P}\\) 가 \\(y\\) 축과 만나는 점을 Q 라 하고, 원점 O 를 지나고 선분 \\(\\mathrm{F}^{\\prime} \\mathrm{P}\\) 와 평행한 직선이 이 쌍곡선과 만나는 점 중 제 1 사분면에 있는 점을 R 이라 하자."}
          , {"order": 2, "type": "TEXT", "content": "\\(\\overline{\\mathrm{F}^{\\prime} \\mathrm{Q}}=\\overline{\\mathrm{QP}}, \\overline{\\mathrm{OQ}}=2\\) 이고 삼각형 PQR 의 넓이가 3 일 때, 이 쌍곡선의 주축의 길이는 \\(p+q \\sqrt{13}\\) 이다."}
          , {"order": 3, "type": "TEXT", "content": "\\(p^2+q^2\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 유리수이다.)"}
          , {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/geo29p.JPG"}
          ]'::jsonb
              , 29
              , null
              , 20
              , 'GEO'
              , 4
              , 'FRQ'
              )

       ,
          (
               (SELECT id FROM exam), '[
            {
              "order": 1,
              "type": "TEXT",
              "content": "좌표평면에 \\(\\overline{\\mathrm{AB}}=6, \\overline{\\mathrm{AD}}=8\\) 인 직사각형 ABCD 와 \\(2 \\overrightarrow{\\mathrm{BE}}=3 \\overrightarrow{\\mathrm{BC}}-\\overrightarrow{\\mathrm{BA}}\\) 를 만족시키는 점 E 가 있다. 선분 BC 위를 움직이는 점 P 에 대하여 점 Q 가"
            },
            {
              "order": 2,
              "type": "TEXT",
              "content": "\\[\\n\\overrightarrow{\\mathrm{PQ}} \\cdot(\\overrightarrow{\\mathrm{PQ}}-\\overrightarrow{\\mathrm{AB}})=0\\n\\]"
            },
            {
              "order": 3,
              "type": "TEXT",
              "content": "을 만족시킬 때, \\(\\overrightarrow{\\mathrm{AE}} \\cdot \\overrightarrow{\\mathrm{AQ}}\\) 의 최솟값을 구하시오."
            },
            {"order":4, "type": "IMAGE", "content":"https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/geo30p.JPG"}
          ]'::jsonb, 30, null, 36, 'GEO', 4, 'FRQ'
              );




