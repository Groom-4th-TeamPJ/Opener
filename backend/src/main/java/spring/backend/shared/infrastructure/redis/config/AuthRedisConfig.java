package spring.backend.shared.infrastructure.redis.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class AuthRedisConfig {

  @Value("${spring.data.redis.auth.host}")
  private String redisHost;

  @Value("${spring.data.redis.auth.port}")
  private int redisPort;

  @Primary
  @Bean
  public RedisConnectionFactory authRedisConnectionFactory() {
    RedisStandaloneConfiguration config =
            new RedisStandaloneConfiguration(redisHost, redisPort);
    return new LettuceConnectionFactory(config);
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
