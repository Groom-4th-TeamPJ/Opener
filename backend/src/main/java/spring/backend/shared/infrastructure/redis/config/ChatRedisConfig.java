package spring.backend.shared.infrastructure.redis.config;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Cluster Configuration for Chat Service
 *
 * <p>채팅 서비스를 위한 Redis Cluster 설정입니다.
 * 3개의 Master 노드와 3개의 Replica 노드로 구성됩니다.</p>
 *
 * <p>주요 기능:
 * <ul>
 *   <li>자동 페일오버 지원</li>
 *   <li>토폴로지 자동 갱신</li>
 *   <li>Replica 우선 읽기로 부하 분산</li>
 * </ul>
 * </p>
 *
 * <p>환경변수 설정 방법 (배포용 .env):
 * <pre>
 * REDIS_CLUSTER_NODE_0=redis-chat-1:6380
 * REDIS_CLUSTER_NODE_1=redis-chat-2:6381
 * ...
 * </pre>
 * </p>
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis.chat")
@Getter
@Setter
public class ChatRedisConfig {

    private Cluster cluster = new Cluster();
    private long timeout = 3000;

    @Getter
    @Setter
    public static class Cluster {
        private List<String> nodes;
        private int maxRedirects = 3;
    }

    /**
     * Redis Cluster 연결 팩토리
     *
     * <p>Lettuce 클라이언트를 사용하여 Redis Cluster에 연결합니다.</p>
     *
     * @return LettuceConnectionFactory
     */
    @Bean(name = "chatRedisConnectionFactory")
    public LettuceConnectionFactory chatRedisConnectionFactory() {
        log.info("Initializing Redis Cluster with nodes: {}", cluster.getNodes());

        // Cluster 설정
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(cluster.getNodes());
        clusterConfig.setMaxRedirects(cluster.getMaxRedirects());

        // 토폴로지 리프레시 설정 (노드 변경 시 자동 감지)
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(30))      // 30초마다 토폴로지 갱신
                .enableAllAdaptiveRefreshTriggers()                 // 모든 적응형 갱신 트리거 활성화
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(30))
                .build();

        // 클러스터 클라이언트 옵션
        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(topologyRefreshOptions)
                .autoReconnect(true)                                // 자동 재연결
                .disconnectedBehavior(ClusterClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .validateClusterNodeMembership(true)
                .build();

        // Lettuce 클라이언트 설정
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .readFrom(ReadFrom.REPLICA_PREFERRED)               // 읽기는 Replica 우선
                .commandTimeout(Duration.ofMillis(this.timeout))
                .clientOptions(clusterClientOptions)
                .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }

    /**
     * Chat용 RedisTemplate
     *
     * <p>모든 직렬화에 StringRedisSerializer를 사용합니다.</p>
     *
     * @param connectionFactory chatRedisConnectionFactory
     * @return StringRedisTemplate
     */
    @Bean(name = "chatRedisTemplate")
    public StringRedisTemplate chatRedisTemplate(
            @Qualifier("chatRedisConnectionFactory")
            RedisConnectionFactory connectionFactory) {

        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);

        // String 직렬화 설정
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
