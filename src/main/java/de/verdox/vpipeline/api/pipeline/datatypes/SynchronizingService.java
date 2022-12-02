package de.verdox.vpipeline.api.pipeline.datatypes;

import de.verdox.vpipeline.api.modules.redis.synchronizer.RedisSynchronizingService;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:28
 */
public interface SynchronizingService extends SystemPart {
    Synchronizer getSynchronizer(Pipeline pipeline, IPipelineData data);

    static SynchronizingService buildRedisService(boolean clusterMode, @NotNull String[] addressArray, String redisPassword){
        return new RedisSynchronizingService(new RedisConnection(clusterMode, addressArray, redisPassword));
    }
}