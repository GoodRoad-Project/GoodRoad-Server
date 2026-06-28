package goodroad.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {
    public static final String GRAPH_HOPPER_ROUTES = "graphhopperRoutes";
    public static final String REWARD_OFFERS = "rewardOffers";
    public static final String REWARD_LEADERBOARD = "rewardLeaderboard";
    public static final String POINTS_LEADERBOARD = "pointsLeaderboard";

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = redisCacheConfiguration(Duration.ofMinutes(10));

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                GRAPH_HOPPER_ROUTES, redisCacheConfiguration(Duration.ofHours(12)),
                REWARD_OFFERS, redisCacheConfiguration(Duration.ofHours(24)),
                REWARD_LEADERBOARD, redisCacheConfiguration(Duration.ofMinutes(10)),
                POINTS_LEADERBOARD, redisCacheConfiguration(Duration.ofMinutes(10))
        );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean("graphHopperRouteKeyGenerator")
    public KeyGenerator graphHopperRouteKeyGenerator() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        return (Object target, Method method, Object... params) -> {
            String start = normalizeString(valueAt(params, 0), "");
            String end = normalizeString(valueAt(params, 1), "");
            String profile = normalizeString(valueAt(params, 2), "foot").toLowerCase(Locale.ROOT);
            Boolean pointsEncoded = valueAt(params, 3) == null ? Boolean.TRUE : (Boolean) valueAt(params, 3);
            String locale = normalizeString(valueAt(params, 4), "ru").toLowerCase(Locale.ROOT);
            String customModelHash = stableHash(mapper, valueAt(params, 5));

            return String.join("|", start, end, profile, pointsEncoded.toString(), locale, customModelHash);
        };
    }

    private RedisCacheConfiguration redisCacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer()));
    }

    private GenericJackson2JsonRedisSerializer redisValueSerializer() {
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("goodroad.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.util.")
                .build();

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    private static Object valueAt(Object[] params, int index) {
        return params.length > index ? params[index] : null;
    }

    private static String normalizeString(Object value, String defaultValue) {
        if (value == null) return defaultValue;
        String normalized = value.toString().trim();
        return normalized.isEmpty() ? defaultValue : normalized;
    }

    private static String stableHash(ObjectMapper mapper, Object value) {
        if (value == null) return "null";
        try {
            String json = mapper.writeValueAsString(value);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
