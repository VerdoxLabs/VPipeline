package de.verdox.vpipeline.api.pipeline.parts.synchronizer.pipeline;

import de.verdox.mccreativelab.serialization.JsonSerializer;
import de.verdox.mccreativelab.serialization.JsonSerializerBuilder;
import de.verdox.mccreativelab.serialization.SerializableField;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.DataSynchronizer;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.cache.global.RedisCache;
import de.verdox.vpipeline.api.pipeline.parts.synchronizer.data.RedisDataDataSynchronizer;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisSynchronizingService implements SynchronizingService {
    public static final JsonSerializer<RedisSynchronizingService> SERIALIZER = JsonSerializerBuilder.create("redis_cache", RedisSynchronizingService.class)
            .constructor(
                    new SerializableField<>("redis_connection", RedisConnection.SERIALIZER, RedisSynchronizingService::getRedisConnection),
                    RedisSynchronizingService::new
            )
            .build();
    private final RedisConnection redisConnection;
    private final Map<Class<? extends IPipelineData>, RedisDataDataSynchronizer> cache;

    public RedisSynchronizingService(@NotNull RedisConnection redisConnection) {
        this.redisConnection = redisConnection;
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public DataSynchronizer getOrCreate(@NotNull Pipeline pipeline, @NotNull Class<? extends IPipelineData> type) {
        cache.computeIfAbsent(type, aClass -> new RedisDataDataSynchronizer(type, pipeline, redisConnection));
        return cache.get(type);
    }

    @Override
    public void shutdown() {
        disconnect();
    }

    public RedisConnection getRedisConnection() {
        return redisConnection;
    }

    @Override
    public void connect() {
        getRedisConnection().connect();
        NetworkLogger.info("Redis Synchronizing Service started");
    }

    @Override
    public void disconnect() {
        getRedisConnection().disconnect();
    }
}
