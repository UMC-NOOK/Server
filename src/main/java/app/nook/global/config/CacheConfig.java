package app.nook.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {
    public static final String ALADIN_SEARCH_CACHE = "aladinSearchResults";
    public static final String WEEKLY_BESTSELLERS_CACHE = "weeklyBestsellers";
    public static final String PERSONALIZED_BESTSELLERS_CACHE = "personalizedBestsellers";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(5))
                        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("libraryStatusFirstPage",
                defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigs.put(ALADIN_SEARCH_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put(WEEKLY_BESTSELLERS_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put(PERSONALIZED_BESTSELLERS_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
