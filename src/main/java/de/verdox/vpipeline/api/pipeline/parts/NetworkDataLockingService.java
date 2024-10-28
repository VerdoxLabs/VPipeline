package de.verdox.vpipeline.api.pipeline.parts;

import de.verdox.vpipeline.api.Connection;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.lock.DummyNetworkDataLockingService;
import de.verdox.vpipeline.api.pipeline.parts.lock.RedisNetworkDataLockingService;
import de.verdox.vpipeline.impl.util.RedisConnection;
import de.verdox.vserializer.generic.Serializer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.locks.Lock;

/**
 * Provides network locks for {@link IPipelineData} read/write operations
 */
public interface NetworkDataLockingService extends Connection {
    Serializer<NetworkDataLockingService> SERIALIZER = Serializer.Selection.create("network_lock", NetworkDataLockingService.class)
            .variant("dummy", Serializer.Dummy.create(new DummyNetworkDataLockingService()))
            .variant("redis", RedisNetworkDataLockingService.SERIALIZER, new RedisNetworkDataLockingService(new RedisConnection(false, new String[]{"redis://localhost:6379"}, "")))
            ;

    /**
     * Returns the network {@link Lock} for read operations on the {@link IPipelineData} object specified by its type and {@link UUID}
     *
     * @param type the type
     * @param uuid the uuid
     * @param <T>  the generic pipeline data type
     * @return the lock
     */
    <T extends IPipelineData> Lock getReadLock(@NotNull Class<? extends T> type, @NotNull UUID uuid);

    /**
     * Returns the network {@link Lock} for write operations on the {@link IPipelineData} object specified by its type and {@link UUID}
     *
     * @param type the type
     * @param uuid the uuid
     * @param <T>  the generic pipeline data type
     * @return the lock
     */
    <T extends IPipelineData> Lock getWriteLock(@NotNull Class<? extends T> type, @NotNull UUID uuid);

    /**
     * Creates a dummy {@link NetworkDataLockingService} that only holds local locks that are not shared across the network.
     * If you only have one game server node you won't need network locks since you only have to ensure read/write thread-safety across
     * the gameserver.
     *
     * @return the networkDataLockingService
     */
    static NetworkDataLockingService createDummy() {
        return new DummyNetworkDataLockingService();
    }

    /**
     * Creates a redis-based {@link NetworkDataLockingService} that holds global locks that are shared across the network.
     * If you have more than one game server node you will need network locks since you have to ensure read/write thread-safety across.
     *
     * @return the networkDataLockingService
     */
    static NetworkDataLockingService createRedis(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        return createRedis(new RedisConnection(clusterMode, addressArray, redisPassword));
    }

    static NetworkDataLockingService createRedis(RedisConnection redisConnection) {
        return new RedisNetworkDataLockingService(redisConnection);
    }
}
