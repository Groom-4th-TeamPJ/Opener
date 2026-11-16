-- 채팅 메시지 저장 -> push 와 TTL 갱신 2회를 한 왕복으로 처리한다
-- KEYS[1] 메시지 목록 · KEYS[2] 세션 해시 · ARGV[1] 메시지 JSON · ARGV[2] TTL(초)
-- 반환 = push 직후 목록 길이 (호출자가 질문 직전까지의 히스토리 범위를 계산하는 데 쓴다)

local n = redis.call('RPUSH', KEYS[1], ARGV[1])
redis.call('EXPIRE', KEYS[1], ARGV[2])
-- 세션 키가 없으면 EXPIRE 는 0 을 돌려줄 뿐 키를 만들지 않는다
redis.call('EXPIRE', KEYS[2], ARGV[2])
return n
