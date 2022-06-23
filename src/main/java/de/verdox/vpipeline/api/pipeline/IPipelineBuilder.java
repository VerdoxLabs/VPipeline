package de.verdox.vpipeline.api.pipeline;

import com.zaxxer.hikari.HikariDataSource;
import de.verdox.vpipeline.api.pipeline.core.IPipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.ISynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.IGlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.IGlobalStorage;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 14:35
 */
public interface IPipelineBuilder {
    IPipelineBuilder useRedisCache(boolean clusterMode, @NotNull String[] addressArray, String redisPassword);
    IPipelineBuilder useRedisSynchronizationService(boolean clusterMode, @NotNull String[] addressArray, String redisPassword);
    IPipelineBuilder useMongoStorage(String host, String database, int port, String user, String password);
    IPipelineBuilder useJsonStorage(Path path);
    IPipelineBuilder useSQLStorage(HikariDataSource hikariDataSource);

    IPipelineBuilder useCustomGlobalCache(IGlobalCache globalCache);
    IPipelineBuilder useCustomGlobalStorage(IGlobalStorage globalStorage);
    IPipelineBuilder useCustomSynchronizingService(ISynchronizingService synchronizingService);
    IPipeline buildPipeline();
}
