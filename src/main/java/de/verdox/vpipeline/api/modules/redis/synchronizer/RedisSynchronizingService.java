package de.verdox.vpipeline.api.modules.redis.synchronizer;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.Synchronizer;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 16:11
 */
public class RedisSynchronizingService implements SynchronizingService {

    private final RedisConnection redisConnection;
    private final Map<Class<? extends IPipelineData>, RedisDataSynchronizer> cache;

    public RedisSynchronizingService(@NotNull RedisConnection redisConnection) {
        this.redisConnection = redisConnection;
        this.cache = new ConcurrentHashMap<>();
        NetworkLogger.getLogger().info("Redis Synchronizing Service started");
    }

    @Override
    public Synchronizer getSynchronizer(Pipeline pipeline, IPipelineData data) {
        cache.computeIfAbsent(data.getClass(), aClass -> new RedisDataSynchronizer(pipeline, redisConnection, data.getClass()));
        return cache.get(data.getClass());
    }

    @Override
    public void shutdown() {
        this.redisConnection.shutdown();
    }
}
