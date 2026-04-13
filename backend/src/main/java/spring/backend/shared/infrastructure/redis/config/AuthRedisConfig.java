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

@Configuration
public class AuthRedisConfig {

  @Value("${spring.data.redis.auth.host}")
  private String redisHost;

  @Value("${spring.data.redis.auth.port}")
  private int redisPort;

  @Value("${spring.data.redis.auth.timeout:3000}")
  private long timeout;

  @Primary
  @Bean
  public RedisConnectionFactory authRedisConnectionFactory() {
    RedisStandaloneConfiguration config =
            new RedisStandaloneConfiguration(redisHost, redisPort);

    @SuppressWarnings("rawtypes")
    GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    poolConfig.setMaxTotal(20);
    poolConfig.setMaxIdle(10);
    poolConfig.setMinIdle(5);
    poolConfig.setTestOnBorrow(true);

    LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofMillis(timeout))
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
