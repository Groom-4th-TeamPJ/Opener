-- 채팅 세션 선점 -> 소유자 기록과 TTL 을 한 왕복으로 처리한다
-- KEYS[1] 세션 해시 · ARGV[1] userId · ARGV[2] TTL(초) · ARGV[3] 생성 시각(ms)
-- 반환 1 = 내 세션(신규 선점 또는 기존 소유자와 일치) · 0 = 다른 사용자가 소유한 세션

-- HSETNX 는 "없을 때만 쓴다" 를 한 명령으로 판정 -> 동시 connect 2건 중 먼저 도착한 쪽만 소유자가 된다
if redis.call('HSETNX', KEYS[1], 'userId', ARGV[1]) == 1 then
  redis.call('HSET', KEYS[1], 'createdAt', ARGV[3])
  redis.call('EXPIRE', KEYS[1], ARGV[2])
  return 1
end

if redis.call('HGET', KEYS[1], 'userId') ~= ARGV[1] then
  return 0
end

-- 분리된 HSET/EXPIRE 시절에 만료 없이 남은 키를 재연결 때 복구
if redis.call('TTL', KEYS[1]) < 0 then
  redis.call('EXPIRE', KEYS[1], ARGV[2])
end
return 1
