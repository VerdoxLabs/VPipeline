package de.verdox.vpipeline.api.pipeline.parts;

import de.verdox.mccreativelab.serialization.JsonSerializer;
import de.verdox.vpipeline.api.Connection;
import de.verdox.vpipeline.api.pipeline.parts.cache.global.RedisCache;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

public interface GlobalCache extends DataProvider, Connection {
    JsonSerializer<GlobalCache> SERIALIZER = JsonSerializer.Selection.create("global_cache", GlobalCache.class)
            .variant("redis", RedisCache.SERIALIZER, new RedisCache(new RedisConnection(false, new String[]{"redis://localhost:6379"}, "")))
            ;

    static GlobalCache createRedisCache(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        return createRedisCache(new RedisConnection(clusterMode, addressArray, redisPassword));
    }

    static GlobalCache createRedisCache(RedisConnection redisConnection) {
        return new RedisCache(redisConnection);
    }
}
