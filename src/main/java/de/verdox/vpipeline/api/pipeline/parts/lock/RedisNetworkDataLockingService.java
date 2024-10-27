package de.verdox.vpipeline.api.pipeline.parts.lock;

import de.verdox.vserializer.json.JsonSerializer;
import de.verdox.vserializer.json.JsonSerializerBuilder;
import de.verdox.vserializer.SerializableField;
import de.verdox.vpipeline.api.pipeline.parts.NetworkDataLockingService;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.locks.Lock;

public class RedisNetworkDataLockingService implements NetworkDataLockingService {
    public static final JsonSerializer<RedisNetworkDataLockingService> SERIALIZER = JsonSerializerBuilder.create("redis_network_data_locking_service", RedisNetworkDataLockingService.class)
            .constructor(
                    new SerializableField<>("redis_connection", RedisConnection.SERIALIZER, RedisNetworkDataLockingService::getRedisConnection),
                    RedisNetworkDataLockingService::new
            )
            .build();
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

    public RedisConnection getRedisConnection() {
        return redisConnection;
    }

    @Override
    public void connect() {
        this.redisConnection.connect();
    }

    @Override
    public void disconnect() {
        this.redisConnection.disconnect();
    }
}
