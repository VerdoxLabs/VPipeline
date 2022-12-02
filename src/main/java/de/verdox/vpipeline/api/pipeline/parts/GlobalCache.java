package de.verdox.vpipeline.api.pipeline.parts;

import de.verdox.vpipeline.api.modules.redis.globalcache.RedisCache;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:13
 */
public interface GlobalCache extends DataProvider {
    static GlobalCache createRedisCache(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        return new RedisCache(clusterMode, addressArray, redisPassword);
    }
}
