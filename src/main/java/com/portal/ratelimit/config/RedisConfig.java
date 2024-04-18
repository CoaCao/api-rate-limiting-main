package com.portal.ratelimit.config;


import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.Duration.ofSeconds;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.redis.cluster")
public class RedisConfig {
    private List<String> nodes;
    private int maxRedirects;

//    @Bean
//    public Config config() {
//        Config config = new Config();
//        config.useClusterServers().setNodeAddresses(nodes);
////        config.useSingleServer().setAddress("redis://localhost:6379")
//
//        return config;
//    }
//
//    @Bean(name = "springCM")
//    public CacheManager cacheManager(Config config) {
//        CacheManager manager = Caching.getCachingProvider().getCacheManager();
//        manager.createCache("cache", RedissonConfiguration.fromConfig(config));
//        manager.createCache("userList", RedissonConfiguration.fromConfig(config));
//        return manager;
//    }
//
//    @Bean
//    ProxyManager<String> proxyManager(CacheManager cacheManager) {
//        return new JCacheProxyManager<>(cacheManager.getCache("cache"));
//    }

    @Bean(name = "jedisProxyManager")
    public JedisBasedProxyManager<String> jedisProxyManager() {
        Set<HostAndPort> jedisClusterNodes = this.nodes.stream().map(HostAndPort::from).collect(Collectors.toSet());

        final JedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder()
                .timeoutMillis(60000)
                .connectionTimeoutMillis(90000)
                .build();

        JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes, jedisClientConfig);

        return JedisBasedProxyManager.builderFor(jedisCluster)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(ofSeconds(10)))
                .withKeyMapper(Mapper.STRING)
                .build();
    }

    @Bean(name = "lettuceProxyManager")
    public LettuceBasedProxyManager<String> lettuceProxyManager() {
        Set<HostAndPort> nodes = this.nodes.stream().map(HostAndPort::from).collect(Collectors.toSet());

        List<RedisURI> redisURIS = nodes.stream().map(node -> RedisURI.builder().withHost(node.getHost())
                .withPort(node.getPort()).build()).collect(Collectors.toList());

        RedisClusterClient redisClusterClient = RedisClusterClient.create(redisURIS);
        StatefulRedisClusterConnection<String, byte[]> redisClusterConnection =
                redisClusterClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        return LettuceBasedProxyManager.builderFor(redisClusterConnection)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(ofSeconds(10)))
                .build();
    }
}
