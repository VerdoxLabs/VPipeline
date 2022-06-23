package de.verdox.vpipeline.impl.modules.redis.synchronizer;

import de.verdox.vpipeline.api.pipeline.core.IPipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.ISynchronizer;
import de.verdox.vpipeline.api.pipeline.datatypes.ISynchronizingService;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 16:11
 */
public class RedisSynchronizingService implements ISynchronizingService {

    private final RedisConnection redisConnection;
    private final Map<Class<? extends IPipelineData>, RedisDataSynchronizer> cache;

    public RedisSynchronizingService(@NotNull RedisConnection redisConnection) {
        this.redisConnection = redisConnection;
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public ISynchronizer getSynchronizer(IPipeline pipeline, IPipelineData data) {
        cache.computeIfAbsent(data.getClass(), aClass -> new RedisDataSynchronizer(pipeline, redisConnection, data.getClass()));
        return cache.get(data.getClass());
    }
}
