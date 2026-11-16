-- RAG 유사 문서 검색이 HNSW 인덱스를 타게 한다
-- 플래너가 1536 차원 코사인 거리 계산을 기본 함수 비용 1 로 셈해 전체 스캔(5,152행, 30~87ms)을 인덱스 검색(1~5ms)보다 싸다고 판단했다
-- S1 부하(동시 1,000 세션)에서 이 전체 스캔이 커넥션 20개를 모두 쥐어 커넥션 대기가 412건까지 쌓였다 (r128 진단 실행)
-- 100 은 5,152행에서 계획이 Index Scan 으로 바뀌는 실측값 -> 몇백 행짜리 작은 테이블은 그대로 전체 스캔을 고른다
-- shortcut: 함수 소유자 계정만 실행할 수 있고 ALTER EXTENSION vector UPDATE 가 비용을 1 로 되돌릴 수 있다, 확장을 올린 뒤에는 EXPLAIN 으로 Index Scan 을 다시 확인한다
ALTER FUNCTION cosine_distance(vector, vector) COST 100;
