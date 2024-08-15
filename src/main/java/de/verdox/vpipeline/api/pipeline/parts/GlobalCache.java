package de.verdox.vpipeline.api.pipeline.parts;

import de.verdox.vpipeline.api.pipeline.parts.cache.global.RedisCache;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.locks.Lock;

public interface GlobalCache extends DataProvider {
    static GlobalCache createRedisCache(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        return new RedisCache(new RedisConnection(clusterMode, addressArray, redisPassword));
    }
}
