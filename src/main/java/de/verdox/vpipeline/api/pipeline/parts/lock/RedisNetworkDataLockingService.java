package de.verdox.vpipeline.api.pipeline.parts.lock;

import de.verdox.vpipeline.api.pipeline.core.NetworkDataLockingService;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.locks.Lock;

public class RedisNetworkDataLockingService implements NetworkDataLockingService {
    private final RedisConnection redisConnection;
    public RedisNetworkDataLockingService(RedisConnection redisConnection){
        this.redisConnection = redisConnection;
    }
    @Override
    public <T extends IPipelineData> Lock getReadLock(@NotNull Class<? extends T> type, @NotNull UUID uuid) {
        return this.redisConnection.getRedissonClient().getReadWriteLock(getLockName(type, uuid)).readLock();
    }

    @Override
    public <T extends IPipelineData> Lock getWriteLock(@NotNull Class<? extends T> type, @NotNull UUID uuid) {
        return this.redisConnection.getRedissonClient().getReadWriteLock(getLockName(type, uuid)).writeLock();
    }

    private <T extends IPipelineData> String getLockName(@NotNull Class<? extends T> type, @NotNull UUID uuid){
        String classifier = AnnotationResolver
                .getDataStorageClassifier(type)
                .isEmpty() ? "" : AnnotationResolver.getDataStorageClassifier(type) + ":";
        return "Lock:" + classifier + uuid + ":" + AnnotationResolver.getDataStorageIdentifier(type);
    }
}
