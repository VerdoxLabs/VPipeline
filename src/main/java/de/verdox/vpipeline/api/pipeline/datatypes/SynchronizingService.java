package de.verdox.vpipeline.api.pipeline.datatypes;

import de.verdox.vpipeline.api.Connection;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.api.pipeline.parts.synchronizer.pipeline.DummySynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.synchronizer.pipeline.RedisSynchronizingService;
import de.verdox.vpipeline.impl.util.RedisConnection;
import de.verdox.vserializer.generic.Serializer;
import org.jetbrains.annotations.NotNull;

public interface SynchronizingService extends SystemPart, Connection {

    Serializer<SynchronizingService> SERIALIZER = Serializer.Selection.create("synchronizingService", SynchronizingService.class)
            .variant("redis", RedisSynchronizingService.SERIALIZER, new RedisSynchronizingService(new RedisConnection(false, new String[]{"redis://localhost:6379"}, "")))
            .variant("dummy", Serializer.Dummy.create(new DummySynchronizingService()));
    default DataSynchronizer getOrCreate(@NotNull Pipeline pipeline, @NotNull IPipelineData data) {
        return getOrCreate(pipeline, data.getClass());
    }

    DataSynchronizer getOrCreate(@NotNull Pipeline pipeline, @NotNull Class<? extends IPipelineData> type);

    static SynchronizingService buildRedisService(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        return buildRedisService(new RedisConnection(clusterMode, addressArray, redisPassword));
    }

    static SynchronizingService buildRedisService(RedisConnection redisConnection) {
        return new RedisSynchronizingService(redisConnection);
    }
}