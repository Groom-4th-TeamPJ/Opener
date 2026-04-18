package spring.backend.shared.infrastructure.redis.config;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DnsResolvers;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 채팅 서비스를 위한 Redis Cluster 설정
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis.chat")
@Getter
@Setter
public class ChatRedisConfig {

    private Cluster cluster = new Cluster();
    private Duration timeout = Duration.ofMillis(3000);

    /**
     * Redis Cluster 연결 팩토리
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

        // host.docker.internal → localhost 변환 (Mac 호스트에서는 이 호스트명을 해석할 수 없음)
        // MappingSocketAddressResolver: 클러스터 토폴로지 응답의 호스트명을 연결 직전에 교체
        @SuppressWarnings("deprecation")
        MappingSocketAddressResolver socketAddressResolver = MappingSocketAddressResolver.create(
                DnsResolvers.JVM_DEFAULT,
                hostAndPort -> "host.docker.internal".equals(hostAndPort.getHostText())
                        ? HostAndPort.of("localhost", hostAndPort.getPort())
                        : hostAndPort
        );

        ClientResources clientResources = DefaultClientResources.builder()
                .socketAddressResolver(socketAddressResolver)
                .build();

        // Connection Pool 설정
        @SuppressWarnings("rawtypes")
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);

        // Lettuce 클라이언트 설정 (Connection Pool 포함)
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .readFrom(ReadFrom.REPLICA_PREFERRED)               // 읽기는 Replica 우선
                .commandTimeout(this.timeout)
                .clientOptions(clusterClientOptions)
                .clientResources(clientResources)
                .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }

    /**
     * Chat용 RedisTemplate
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

    @Getter
    @Setter
    public static class Cluster {
        private List<String> nodes;
        private int maxRedirects = 3;
    }
}
