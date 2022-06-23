package de.verdox.vpipeline.impl.pipeline;

import com.zaxxer.hikari.HikariDataSource;
import de.verdox.vpipeline.api.pipeline.IPipelineBuilder;
import de.verdox.vpipeline.api.pipeline.core.IPipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.ISynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.IGlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.IGlobalStorage;
import de.verdox.vpipeline.impl.modules.json.JsonFileStorage;
import de.verdox.vpipeline.impl.modules.mongo.MongoDBStorage;
import de.verdox.vpipeline.impl.modules.redis.globalcache.RedisCache;
import de.verdox.vpipeline.impl.modules.redis.synchronizer.RedisSynchronizingService;
import de.verdox.vpipeline.impl.modules.sql.mysql.MySQLStorage;
import de.verdox.vpipeline.impl.pipeline.core.Pipeline;
import de.verdox.vpipeline.impl.pipeline.parts.LocalCache;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 18:23
 */
public class PipelineBuilder implements IPipelineBuilder {
    static Logger log = Logger.getLogger(PipelineBuilder.class.getName());

    private IGlobalCache globalCache;
    private IGlobalStorage globalStorage;
    private ISynchronizingService synchronizingService;

    @Override
    public IPipelineBuilder useRedisCache(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        checkCache();
        globalCache = new RedisCache(clusterMode, addressArray, redisPassword);
        return this;
    }

    @Override
    public IPipelineBuilder useRedisSynchronizationService(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        checkSynchronizingService();
        synchronizingService = new RedisSynchronizingService(new RedisConnection(clusterMode, addressArray, redisPassword));
        return this;
    }

    @Override
    public IPipelineBuilder useMongoStorage(String host, String database, int port, String user, String password) {
        checkStorage();
        globalStorage = new MongoDBStorage(host, database, port, user, password);
        return this;
    }

    @Override
    public IPipelineBuilder useJsonStorage(Path path) {
        checkStorage();
        globalStorage = new JsonFileStorage(path);
        return this;
    }

    @Override
    public IPipelineBuilder useSQLStorage(HikariDataSource hikariDataSource) {
        checkStorage();
        globalStorage = new MySQLStorage(hikariDataSource);
        return this;
    }

    @Override
    public IPipelineBuilder useCustomGlobalCache(IGlobalCache globalCache) {
        checkCache();
        this.globalCache = globalCache;
        return this;
    }

    @Override
    public IPipelineBuilder useCustomGlobalStorage(IGlobalStorage globalStorage) {
        checkStorage();
        this.globalStorage = globalStorage;
        return this;
    }

    @Override
    public IPipelineBuilder useCustomSynchronizingService(ISynchronizingService synchronizingService) {
        checkSynchronizingService();
        this.synchronizingService = synchronizingService;
        return this;
    }

    @Override
    public IPipeline buildPipeline() {
        LocalCache localCache = new LocalCache();
        if (globalStorage == null && globalCache == null)
            log.warning("Both globalCache and globalStorage were not set during pipeline building phase.");
        if(synchronizingService == null && globalCache != null)
            log.warning("A globalCache but no synchronizing service was set during pipeline building phase.");
        IPipeline pipeline = new Pipeline(localCache, globalCache, globalStorage, synchronizingService);
        localCache.setPipeline(pipeline);
        return pipeline;
    }

    private void checkStorage() {
        if (globalStorage != null)
            throw new RuntimeException("GlobalStorage already set in PipelineBuilder");
    }

    private void checkCache() {
        if (globalCache != null)
            throw new RuntimeException("GlobalCache already set in PipelineBuilder");
    }

    private void checkSynchronizingService() {
        if (synchronizingService != null)
            throw new RuntimeException("GlobalCache already set in PipelineBuilder");
    }
}
