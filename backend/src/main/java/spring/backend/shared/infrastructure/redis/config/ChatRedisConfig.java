package spring.backend.shared.infrastructure.redis.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class ChatRedisConfig {

  @Value("${spring.data.redis.chat.host}")
  private String chatRedisHost;

  @Value("${spring.data.redis.chat.port}")
  private int chatRedisPort;

  @Bean
  public RedisConnectionFactory chatRedisConnectionFactory() {
    RedisStandaloneConfiguration config =
            new RedisStandaloneConfiguration(chatRedisHost, chatRedisPort);
    return new LettuceConnectionFactory(config);
  }

  @Bean
  public StringRedisTemplate chatRedisTemplate(
          @Qualifier("chatRedisConnectionFactory")
          RedisConnectionFactory factory) {

    StringRedisTemplate template = new StringRedisTemplate();
    template.setConnectionFactory(factory);

    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new StringRedisSerializer());

    return template;
  }
}
