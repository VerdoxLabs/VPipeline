package de.verdox.vpipeline.api.pipeline.parts;

import de.verdox.vpipeline.api.modules.redis.globalcache.RedisCache;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.locks.Lock;

public interface GlobalCache extends DataProvider {
    static GlobalCache createRedisCache(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        return new RedisCache(clusterMode, addressArray, redisPassword);
    }

    <T extends IPipelineData> Lock acquireGlobalObjectReadLock(@NotNull Class<? extends T> type, @NotNull UUID uuid);
    <T extends IPipelineData> Lock acquireGlobalObjectWriteLock(@NotNull Class<? extends T> type, @NotNull UUID uuid);
}
