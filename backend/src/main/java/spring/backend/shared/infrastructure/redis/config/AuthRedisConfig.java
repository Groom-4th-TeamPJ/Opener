package spring.backend.shared.infrastructure.redis.config;

import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// 인증 토큰 전용 standalone Redis -> 채팅 클러스터와 분리해 장애 영향 격리
@Configuration
public class AuthRedisConfig {

  @Value("${spring.data.redis.auth.host}")
  private String redisHost;

  @Value("${spring.data.redis.auth.port}")
  private int redisPort;

  @Value("${spring.data.redis.auth.timeout:3000ms}")
  private Duration timeout;

  // @Primary -> Redis 빈이 둘이라 기본 주입 대상을 인증용으로 지정, chat 은 @Qualifier 로 명시 선택
  @Primary
  @Bean
  public RedisConnectionFactory authRedisConnectionFactory() {
    RedisStandaloneConfiguration config =
            new RedisStandaloneConfiguration(redisHost, redisPort);

    // 커넥션 풀 -> 토큰 검증이 매 요청 발생하므로 연결 재사용으로 지연 최소화
    @SuppressWarnings("rawtypes")
    GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    poolConfig.setMaxTotal(20);
    poolConfig.setMaxIdle(10);
    poolConfig.setMinIdle(5);
    poolConfig.setTestOnBorrow(true);                               // 빌릴 때 검증 -> 끊긴 커넥션 사용 차단

    LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(timeout)
            .build();

    return new LettuceConnectionFactory(config, clientConfig);
  }

  @Primary
  @Bean
  public StringRedisTemplate authRedisTemplate(
          @Qualifier("authRedisConnectionFactory")
          RedisConnectionFactory factory) {

    StringRedisTemplate template = new StringRedisTemplate();
    template.setConnectionFactory(factory);

    // 명시적 Serializer 설정
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new StringRedisSerializer());

    return template;
  }
}
