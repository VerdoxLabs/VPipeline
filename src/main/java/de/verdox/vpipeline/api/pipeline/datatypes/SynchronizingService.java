package de.verdox.vpipeline.api.pipeline.datatypes;

import de.verdox.vpipeline.api.pipeline.parts.synchronizer.pipeline.RedisSynchronizingService;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

public interface SynchronizingService extends SystemPart {
    default DataSynchronizer getOrCreate(@NotNull Pipeline pipeline, @NotNull IPipelineData data) {
        return getOrCreate(pipeline, data.getClass());
    }

    DataSynchronizer getOrCreate(@NotNull Pipeline pipeline, @NotNull Class<? extends IPipelineData> type);

    static SynchronizingService buildRedisService(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        return new RedisSynchronizingService(new RedisConnection(clusterMode, addressArray, redisPassword));
    }
}