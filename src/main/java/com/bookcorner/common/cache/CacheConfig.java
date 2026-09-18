package com.bookcorner.common.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Multi-Tier Caching Architecture.
 * Configures Redis distributed caching for catalog browsing, product details,
 * and categories with TTL policies, and provides in-memory fallback for local development.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_BOOKS = "books";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_AUTHORS = "authors";
    public static final String CACHE_PUBLISHERS = "publishers";

    @Bean
    @Primary
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_BOOKS, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put(CACHE_CATEGORIES, defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put(CACHE_AUTHORS, defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put(CACHE_PUBLISHERS, defaultConfig.entryTtl(Duration.ofHours(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple", matchIfMissing = true)
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(CACHE_BOOKS, CACHE_CATEGORIES, CACHE_AUTHORS, CACHE_PUBLISHERS);
    }
}
