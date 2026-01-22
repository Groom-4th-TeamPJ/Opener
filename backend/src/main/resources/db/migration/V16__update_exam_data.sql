-- 2023 수능
-- 2번 보기 오류
update questions
set options = '[{"order": 1, "content": "1"}, {"order": 2, "content": "2"}, {"order": 3, "content": "3"}, {"order": 4, "content": "4"}, {"order": 5, "content": "5"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2023 and exam_type = 'CSAT')
  and question_no = 2;


-- 2023 년도 수능시험
-- 4번 문제 수정
update questions
   set category = 'ALG'
   , passages = '[{"type": "TEXT", "order": 1, "content": "다항함수 \\(f(x)\\) 에 대하여 함수 \\(g(x)\\) 를"}, {"type": "TEXT", "order": 2, "content": "\\[ g(x)=x^2 f(x) \\]"}, {"type": "TEXT", "order": 3, "content": "라 하자. \\(f(2)=1, f^{\\prime}(2)=3\\) 일 때, \\(g^{\\prime}(2)\\) 의 값은?"}]'::jsonb
 where exam_id = (select id from exams where exam_year = 2023 and exam_type = 'CSAT')
   and question_no = 4;

-- 5번 문제
update questions
set passages = '[{"type": "TEXT", "order": 1, "content": "\\(\\tan \\theta<0\\) 이고 \\(\\cos \\left(\\frac{\\pi}{2}+\\theta\\right)=\\frac{\\sqrt{5}}{5}\\) 일 때, \\(\\cos \\theta\\) 의 값은?"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2023 and exam_type = 'CSAT')
  and question_no = 5;

-- 확률과 통계 23번
update questions
set point = 3
where exam_id = (select id from exams where exam_year = 2023 and exam_type = 'CSAT')
  and question_no = 24
  and category = 'PROB';

-- 확률과 통계 29번
update questions
set passages = '[{"type": "TEXT", "order": 1, "content": "앞면에는 1 부터 6 까지의 자연수가 하나씩 적혀 있고 뒷면에는 모두 0 이 하나씩 적혀 있는 6 장의 카드가 있다. 이 6 장의 카드가 그림과 같이 6 이하의 자연수 \\(k\\) 에 대하여 \\(k\\) 번째 자리에 자연수 \\(k\\) 가 보이도록 놓여 있다."}, {"type": "IMAGE", "order": 2, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/prob29p.JPG"}, {"type": "TEXT", "order": 3, "content": "이 6 장의 카드와 한 개의 주사위를 사용하여 다음 시행을 한다."}, {"type": "IMAGE", "order": 4, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/prob29p2.JPG"}, {"type": "TEXT", "order": 5, "content": "위의 시행을 3 번 반복한 후 6 장의 카드에 보이는 모든 수의 합이 짝수일 때, 주사위의 1 의 눈이 한 번만 나왔을 확률은 \\(\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2023 and exam_type = 'CSAT')
  and question_no = 29
  and category = 'PROB';


-- 미적분 26번
update questions
set passages = '[{"type": "TEXT", "order": 1, "content": "그림과 같이 곡선 \\(y=\\sqrt{\\sec ^2 x+\\tan x}\\left(0 \\leq x \\leq \\frac{\\pi}{3}\\right)\\) 와 \\(x\\) 축, \\(y\\) 축 및 직선 \\(x=\\frac{\\pi}{3}\\) 로 둘러싸인 부분을 밑면으로 하는 입체도형이 있다. 이 입체도형을 \\(x\\) 축에 수직인 평면으로 자른 단면이 모두 정사각형일 때, 이 입체도형의 부피는?"}, {"type": "IMAGE", "order": 2, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/calc26p.JPG"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2023 and exam_type = 'CSAT')
  and question_no = 26   -- 26번
  and category = 'CALC'; -- 미적분
-- 미적분 27번
update questions
set passages = '[{"type": "TEXT", "order": 1, "content": "그림과 같이 중심이 O , 반지름의 길이가 1 이고 중심각의 크기가 \\(\\frac{\\pi}{2}\\) 인 부채꼴 \\(\\mathrm{OA}_1 \\mathrm{~B}_1\\) 이 있다. 호 \\(\\mathrm{A}_1 \\mathrm{~B}_1\\) 위에 점 \\(\\mathrm{P}_1\\), 선분 \\(\\mathrm{OA}_1\\) 위에 점 \\(\\mathrm{C}_1\\), 선분 \\(\\mathrm{OB}_1\\) 위에 점 \\(\\mathrm{D}_1\\) 을 사각형 \\(\\mathrm{OC}_1 \\mathrm{P}_1 \\mathrm{D}_1\\) 이 \\(\\overline{\\mathrm{OC}_1}: \\overline{\\mathrm{OD}_1}=3: 4\\) 인 직사각형이 되도록 잡는다. 부채꼴 \\(\\mathrm{OA}_1 \\mathrm{~B}_1\\) 의 내부에 점 \\(\\mathrm{Q}_1\\) 을 \\(\\overline{\\mathrm{P}_1 \\mathrm{Q}_1}=\\overline{\\mathrm{A}_1 \\mathrm{Q}_1}, \\angle \\mathrm{P}_1 \\mathrm{Q}_1 \\mathrm{~A}_1=\\frac{\\pi}{2}\\) 가 되도록 잡고, 이등변삼각형 \\(\\mathrm{P}_1 \\mathrm{Q}_1 \\mathrm{~A}_1\\) 에 색칠하여 얻은 그림을 \\(R_1\\) 이라 하자."}, {"type": "TEXT", "order": 2, "content": "그림 \\(R_1\\) 에서 선분 \\(\\mathrm{OA}_1\\) 위의 점 \\(\\mathrm{A}_2\\) 와 선분 \\(\\mathrm{OB}_1\\) 위의 점 \\(\\mathrm{B}_2\\) 를 \\(\\overline{\\mathrm{OQ}_1}=\\overline{\\mathrm{OA}_2}=\\overline{\\mathrm{OB}_2}\\) 가 되도록 잡고, 중심이 O , 반지름의 길이가 \\(\\overline{\\mathrm{OQ}_1}\\), 중심각의 크기가 \\(\\frac{\\pi}{2}\\) 인 부채꼴 \\(\\mathrm{OA}_2 \\mathrm{~B}_2\\) 를 그린다. 그림 \\(R_1\\) 을 얻은 것과 같은 방법으로 네 점 \\(\\mathrm{P}_2, \\mathrm{C}_2, \\mathrm{D}_2, \\mathrm{Q}_2\\) 를 잡고, 이등변삼각형 \\(\\mathrm{P}_2 \\mathrm{Q}_2 \\mathrm{~A}_2\\) 에 색칠하여 얻은 그림을 \\(R_2\\) 라 하자. 이와 같은 과정을 계속하여 \\(n\\) 번째 얻은 그림 \\(R_n\\) 에 색칠되어 있는 부분의 넓이를 \\(S_n\\) 이라 할 때, \\(\\lim _{n \\rightarrow \\infty} S_n\\) 의 값은?"}, {"type": "IMAGE", "order": 3, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/calc27p.JPG"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2023 and exam_type = 'CSAT')
  and question_no = 27   -- 27번
  and category = 'CALC'; -- 미적분
-- 미적분 28번
update questions
set passages = '[{"type": "TEXT", "order": 1, "content": "그림과 같이 중심이 O 이고 길이가 2 인 선분 AB 를 지름으로 하는 반원 위에 \\(\\angle \\mathrm{AOC}=\\frac{\\pi}{2}\\) 인 점 C 가 있다. 호 BC 위에 점 P 와 호 CA 위에 점 Q 를 \\(\\overline{\\mathrm{PB}}=\\overline{\\mathrm{QC}}\\) 가 되도록 잡고, 선분 AP 위에 점 R 를 \\(\\angle \\mathrm{CQR}=\\frac{\\pi}{2}\\) 가 되도록 잡는다. 선분 AP 와 선분 CO 의 교점을 S 라 하자."}, {"type": "TEXT", "order": 2, "content": "\\(\\angle \\mathrm{PAB}=\\theta\\) 일 때, 삼각형 POB 의 넓이를 \\(f(\\theta)\\), 사각형 CQRS 의 넓이를 \\(g(\\theta)\\) 라 하자. \\(\\lim _{\\theta \\rightarrow 0+} \\frac{3 f(\\theta)-2 g(\\theta)}{\\theta^2}\\) 의 값은? (단, \\(0<\\theta<\\frac{\\pi}{4}\\) )"}, {"type": "IMAGE", "order": 3, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2023/csat/calc28p.JPG"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2023 and exam_type = 'CSAT')
  and question_no = 28   -- 28번
  and category = 'CALC'; -- 미적분


-- 2023년 9월 모의평가
-- 수학 6번
update questions
set answer = 5
where exam_id = (select id from exams where exam_year = 2023 and exam_type = 'M09')
  and question_no = 6   -- 6
  and category = 'ALG'; -- 수학

-- 기하 25
update questions
set answer = 5
where exam_id = (select id from exams where exam_year = 2023 and exam_type = 'M09')
  and question_no = 25   -- 25
  and category = 'GEO'; -- 기하

-- 2024년 9월 모의평가
-- 확통 25번
update questions
set passages = '[{"type": "TEXT", "order": 1, "content": "두 사건 \\(A,B\\) 에 대하여 \\(A\\) 와 \\(B^C\\) 은 서로 배반사건이고"}, {"type": "TEXT", "order": 2, "content": "\\[ \\mathrm{P}(A \\cap B)=\\frac{1}{5}, \\quad \\mathrm{P}(A)+\\mathrm{P}(B)=\\frac{7}{10} \\]"}, {"type": "TEXT", "order": 3, "content": "일 때, \\(\\mathrm{P}(A^C \\cap B)\\) 의 값은? (단, \\(A^C\\) 은 \\(A\\) 의 여사건이다.)"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2024 and exam_type = 'M09')
  and question_no = 25   -- 25
  and category = 'PROB'; -- 기하

-- 미적분 23번
update questions
set answer = 4
where exam_id = (select id from exams where exam_year = 2024 and exam_type = 'M09')
  and question_no = 23   -- 23
  and category = 'CALC'; -- 미적분

-- 미적분 30번
update questions
set passages = '[{"type": "TEXT", "order": 1, "content": "길이가 10 인 선분 AB 를 지름으로 하는 원과 선분 AB 위에 \\(\\overline{AC}=4\\) 인 점 C 가 있다."}, {"type": "TEXT", "order": 2, "content": "이 원 위의 점 P 를 \\(\\angle PCB=\\theta\\) 가 되도록 잡고, 점 P 를 지나고 선분 AB 에 수직인 직선이 이 원과 만나는 점 중 P 가 아닌 점을 Q 라 하자."}, {"type": "TEXT", "order": 3, "content": "삼각형 PCQ 의 넓이를 \\(S(\\theta)\\) 라 할 때, \\(-7\\times S^{\\prime}\\left(\\frac{\\pi}{4}\\right)\\) 의 값을 구하시오. (단, \\(0<\\theta<\\frac{\\pi}{2}\\))"}, {"type": "IMAGE", "order": 4, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m09/calc30p.JPG"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2024 and exam_type = 'M09')
  and question_no = 30   -- 30
  and category = 'CALC'; -- 미적분

-- 2024년 6월 모의평가
update questions
set passages = '[{"type": "TEXT", "order": 1, "content": "자연수 \\(k\\) 에 대하여 다음 조건을 만족시키는 수열 \\(\\{a_n\\}\\) 이 있다."}, {"type": "IMAGE", "order": 2, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/alg15p.JPG"}, {"type": "TEXT", "order": 3, "content": "\\(a_3\\times a_4\\times a_5\\times a_6<0\\) 이 되도록 하는 모든 \\(k\\) 의 값의 합은?"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2024 and exam_type = 'M06')
  and question_no = 15   -- 15
  and category = 'ALG'; -- 수학

-- 확통 26번이 미적분으로 되어있음
update questions
set category = 'PROB'
where exam_id = (select id from exams where exam_year = 2024 and exam_type = 'M06')
  and question_no = 26   -- 26
  and category = 'CALC'
  and answer = 1; -- 특이사항

-- 확통 30 이미지 누락
update questions
set passages = '[{"type": "TEXT", "order": 1, "content": "주머니에 숫자 \\(1,2,3,4\\) 가 하나씩 적혀 있는 흰 공 4 개와 숫자 \\(4,5,6,7\\) 이 하나씩 적혀 있는 검은 공 4 개가 들어 있다. 이 주머니를 사용하여 다음 규칙에 따라 점수를 얻는 시행을 한다."}, {"type": "IMAGE", "order": 2, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/prob30p.JPG"}, {"type": "TEXT", "order": 3, "content": "이 시행을 한 번 하여 얻은 점수가 24 이하의 짝수일 확률이 \\(\\frac{q}{p}\\) 일 때, \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}, {"type": "IMAGE", "order": 4, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/prob30p2.JPG"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2024 and exam_type = 'M06')
  and question_no = 30   -- 30
  and category = 'PROB';

update questions
set passages = '[{"type": "TEXT", "order": 1, "content": "직선 \\(2x+y=0\\) 위를 움직이는 점 P 와 타원 \\(2x^2+y^2=3\\) 위를 움직이는 점 Q 에 대하여"}, {"type": "TEXT", "order": 2, "content": "\\[ \\overrightarrow{\\mathrm{OX}}=\\overrightarrow{\\mathrm{OP}}+\\overrightarrow{\\mathrm{OQ}} \\]"}, {"type": "TEXT", "order": 3, "content": "를 만족시키고, \\(x\\) 좌표와 \\(y\\) 좌표가 모두 0 이상인 모든 점 X 가 나타내는 영역의 넓이는 \\(\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, O 는 원점이고, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}, {"type": "IMAGE", "order": 5, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2024/m06/geo30p.JPG"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2024 and exam_type = 'M06')
  and question_no = 30
  and category = 'GEO';

-- 2025 수능
update questions
set options = '[{"order": 1, "content": "-5"}, {"order": 2, "content": "\\(-\\sqrt{5}\\)"}, {"order": 3, "content": "0"}, {"order": 4, "content": "\\(\\sqrt{5}\\)"}, {"order": 5, "content": "5"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2025 and exam_type = 'CSAT')
  and question_no = 6
  and category = 'ALG';

update questions
set options = '[{"order": 1, "content": "\\(\\frac{37}{4}\\)"}, {"order": 2, "content": "\\(\\frac{39}{4}\\)"}, {"order": 3, "content": "\\(\\frac{41}{4}\\)"}, {"order": 4, "content": "\\(\\frac{43}{4}\\)"}, {"order": 5, "content": "\\(\\frac{45}{4}\\)"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2025 and exam_type = 'CSAT')
  and question_no = 13
  and category = 'ALG';


update questions
set options = '[{"order": 1, "content": "\\(18+15 \\sqrt{3}\\)"}, {"order": 2, "content": "\\(24+20 \\sqrt{3}\\)"}, {"order": 3, "content": "\\(30+25 \\sqrt{3}\\)"}, {"order": 4, "content": "\\(36+30 \\sqrt{3}\\)"}, {"order": 5, "content": "\\(42+35 \\sqrt{3}\\)"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2025 and exam_type = 'CSAT')
  and question_no = 14
  and category = 'ALG';

-- 2026 수능
update questions
set passages = '[{"type": "TEXT", "order": 1, "content": "두 벡터 \\(\\vec{a}=(4,1), \\vec{b}=(-1,-1)\\) 에 대하여 \\(\\vec{a}+\\vec{b}\\) 의 모든 성분의 합은?"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'CSAT')
  and question_no = 23
  and category = 'GEO';

update questions
set answer = 2
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'CSAT')
  and question_no = 26
  and category = 'GEO';

-- 2026 6모
update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "함수"}, {"type": "TEXT", "order": 2, "content": "\\[ f(x)=\\left\\{\\begin{array}{rr} -x^2+a & (x<3) \\\\5 x-a & (x \\geq 3) \\end{array}\\right. \\]"}, {"type": "TEXT", "order": 3, "content": "이 실수 전체의 집합에서 연속일 때, 상수 \\(a\\) 의 값은?"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 4
  and category = 'ALG';


update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "다항함수 \\(f(x)\\) 에 대하여 함수 \\(g(x)\\) 를"}, {"type": "TEXT", "order": 2, "content": "\\[ g(x)=5 x^2+x f(x) \\]"}, {"type": "TEXT", "order": 3, "content": "라 하자. \\(f(3)=2, f^{\\prime}(3)=1\\) 일 때, \\(g^{\\prime}(3)\\) 의 값은?"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 7
  and category = 'ALG';


update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "함수 \\(f(x)=x^2+a x\\) 에 대하여"}, {"type": "TEXT", "order": 2, "content": "\\[\\int_{-3}^3(x+1) f(x) d x=36+\\int_{-3}^3 f(x) d x\\]"}, {"type": "TEXT", "order": 3, "content": "일 때, 상수 \\(a\\) 의 값은?"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 9
  and category = 'ALG';


update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "시각 \\(t=0\\) 일 때 출발하여 수직선 위를 움직이는 점 P 가 있다. 시각이 \\(t(t \\geq 0)\\) 일 때 점 P 의 위치 \\(x\\) 가"}, {"type": "TEXT", "order": 2, "content": "\\[ x=t^3-t^2-t+1 \\]"}, {"type": "TEXT", "order": 3, "content": "이다. <보기>에서 옳은 것만을 있는 대로 고른 것은?"}, {"type": "IMAGE", "order": 4, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg11p.JPG"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 11
  and category = 'ALG';

update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "그림과 같이 함수 \\(f(x)=3 x^2-7 x+2\\) 에 대하여 곡선 \\(y=f(x)\\) 와 직선 \\(y=\\frac{1}{3} x-\\frac{2}{3}\\) 및 \\(y\\) 축으로 둘러싸인 영역을 \\(A\\), 곡선 \\(y=f(x)\\) 와 직선 \\(y=\\frac{1}{3} x-\\frac{2}{3}\\) 로 둘러싸인 영역을 \\(B\\), 곡선 \\(y=f(x)\\) 와 두 직선 \\(y=\\frac{1}{3} x-\\frac{2}{3}, x=k(k>2)\\) 로 둘러싸인 영역을 \\(C\\) 라 하자."}, {"type": "TEXT", "order": 2, "content": "\\[ (A \\text { 의 넓이 })+(C \\text { 의 넓이 })=(B \\text { 의 넓이 }) \\]"}, {"type": "TEXT", "order": 3, "content": "일 때, 상수 \\(k\\) 의 값은?"}, {"type": "IMAGE", "order": 4, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg13p.JPG"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 13
  and category = 'ALG';

update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "\\(\\overline{\\mathrm{AB}}=2 \\sqrt{7}\\) 인 삼각형 ABC 에서 선분 BC 의 중점을 P , 선분 BC 를 \\(5: 1\\) 로 내분하는 점을 Q 라 하자."}, {"type": "TEXT", "order": 2, "content": "\\[\\overline{\\mathrm{AQ}}=3 \\sqrt{2}, \\sin (\\angle \\mathrm{QAP}): \\sin (\\angle \\mathrm{APQ})=\\sqrt{2}: 3 \\]"}, {"type": "TEXT", "order": 3, "content": "일 때, 삼각형 ABC 의 외접원의 넓이는?"}, {"type": "IMAGE", "order": 4, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg14p.JPG"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 14
  and category = 'ALG';

update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "상수 \\(k\\) 와 \\(f^{\\prime}(0)=6\\) 인 삼차함수 \\(f(x)\\) 에 대하여 함수"}, {"type": "TEXT", "order": 2, "content": "\\[ g(x)= \\begin{cases}f(x)+k & (|x|>1) \\\\ -f(x) & (|x| \\leq 1)\\end{cases} \\]"}, {"type": "TEXT", "order": 3, "content": "이 다음 조건을 만족시킬 때, \\(k+f\\left(\\frac{1}{2}\\right)\\) 의 값은?"}, {"type": "IMAGE", "order": 4, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/alg15p.JPG"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 15
  and category = 'ALG';

update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "\\(k>1\\) 인 실수 \\(k\\) 에 대하여 두 곡선"}, {"type": "TEXT", "order": 2, "content": "\\[ y=2^x+\\frac{k}{2}, \\quad y=k \\times\\left(\\frac{1}{2}\\right)^x+k-2 \\]"}, {"type": "TEXT", "order": 3, "content": "가 만나는 점을 A 라 하고, 점 A 를 지나고 기울기가 -1 인 직선이 곡선 \\(y=2^{x-2}-3\\) 과 만나는 점을 B 라 하자."}, {"type": "TEXT", "order": 4, "content": "삼각형 AOB 의 넓이가 16 일 때, \\(k+\\log _2 k=\\frac{q}{p}\\) 이다."}, {"type": "TEXT", "order": 5, "content": "\\(p+q\\) 의 값을 구하시오. (단, O 는 원점이고, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 22
  and category = 'ALG';

update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "두 사건 \\(A\\) 와 \\(B\\) 는 서로 배반사건이고"}, {"type": "TEXT", "order": 2, "content": "\\[ \\mathrm{P}(A \\cup B)=1, \\mathrm{P}\\left(A^C\\right)=2 \\mathrm{P}(A) \\]"}, {"type": "TEXT", "order": 3, "content": "일 때, \\(\\mathrm{P}(B)\\) 의 값은?"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 24
  and category = 'PROB';

update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "두 정수 \\(\\alpha, \\beta(\\alpha>\\beta)\\) 에 대하여 다음 조건을 만족시키는 수열 \\(\\left\\{a_n\\right\\}\\) 이 있다."}, {"type": "IMAGE", "order": 2, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/calc29p.JPG"}, {"type": "TEXT", "order": 3, "content": "수열 \\(\\left\\{a_n\\right\\}\\) 과 \\(b_1>0\\) 인 등비수열 \\(\\left\\{b_n\\right\\}\\) 에 대하여"}, {"type": "TEXT", "order": 4, "content": "\\[ \\sum_{n=1}^{\\infty}\\left(a_{4 n-2} b_n\\right)=\\sum_{n=1}^{\\infty}\\left(a_{4 n-3} b_{2 n}\\right)=6 \\]"}, {"type": "TEXT", "order": 5, "content": "일 때, \\(b_1 \\times b_3=\\frac{q}{p}\\) 이다. \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 29
  and category = 'CALC';


update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "최고차항의 계수가 1 인 삼차함수 \\(f(x)\\) 에 대하여 함수"}, {"type": "TEXT", "order": 2, "content": "\\[ g(x)=\\left|f\\left(\\frac{2}{1+e^{-x}}\\right)\\right| \\]"}, {"type": "TEXT", "order": 3, "content": "가 실수 전체의 집합에서 미분가능하고 다음 조건을 만족시킨다."}, {"type": "IMAGE", "order": 4, "content": "https://goorm-opener.s3.ap-northeast-2.amazonaws.com/questions/2026/m06/calc30p.JPG"}, {"type": "TEXT", "order": 5, "content": "\\(g(0)\\) 의 최솟값을 \\(\\frac{q}{p}\\) 라 할 때, \\(p+q\\) 의 값을 구하시오. (단, \\(p\\) 와 \\(q\\) 는 서로소인 자연수이다.)"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 30
  and category = 'CALC';

update questions
set passages =  '[{"type": "TEXT", "order": 1, "content": "삼각형 OAB 에 대하여 \\(\\overrightarrow{\\mathrm{OA}}=\\vec{a}, \\overrightarrow{\\mathrm{OB}}=\\vec{b}\\) 라 하자."}, {"type": "TEXT", "order": 2, "content": "\\[ |\\vec{a}+\\vec{b}|=6,|2 \\vec{a}-\\vec{b}|=9,(\\vec{a}+\\vec{b}) \\cdot(\\vec{a}-\\vec{b})=0 \\]"}, {"type": "TEXT", "order": 3, "content": "일 때, 삼각형 OAB 의 넓이는?"}]'::jsonb
where exam_id = (select id from exams where exam_year = 2026 and exam_type = 'M06')
  and question_no = 27
  and category = 'GEO';

