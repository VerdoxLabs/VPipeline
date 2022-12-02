package de.verdox.vpipeline.api.modules.redis.globalcache;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.RemoteStorage;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBucket;
import org.redisson.api.RReadWriteLock;
import org.redisson.client.codec.StringCodec;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RedisCache extends RedisConnection implements GlobalCache, RemoteStorage {
    private final AttachedPipeline attachedPipeline;

    public RedisCache(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        super(clusterMode, addressArray, redisPassword);
        this.attachedPipeline = new AttachedPipeline(GsonBuilder::create);
    }

    @Override
    public JsonElement loadData(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        verifyInput(dataClass, objectUUID);
        return performOnReadLock(JsonElement.class, dataClass, objectUUID, () -> {
            try {
                return JsonParser.parseString(getObjectCache(dataClass, objectUUID).get()).getAsJsonObject();
            } catch (Exception e) {
                e.printStackTrace();
                remove(dataClass, objectUUID);
                return null;
            }
        });
    }

    @Override
    public boolean dataExist(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        verifyInput(dataClass, objectUUID);
        return performOnReadLock(boolean.class, dataClass, objectUUID, () -> getObjectCache(dataClass, objectUUID).isExists());
    }

    @Override
    public void save(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave) {
        verifyInput(dataClass, objectUUID);
        performOnWriteLock(dataClass, objectUUID, () -> {
            RBucket<String> objectCache = getObjectCache(dataClass, objectUUID);
            NetworkLogger.getLogger().info("Saving to redis " + dataClass.getSimpleName() + " [" + objectUUID + "]");
            objectCache.set(attachedPipeline.getGson().toJson(dataToSave));
            updateExpireTime(dataClass, objectCache);
            return true;
        });
    }

    @Override
    public boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        verifyInput(dataClass, objectUUID);
        return performOnWriteLock(dataClass, objectUUID, () -> {
            NetworkLogger
                    .getLogger()
                    .info("Removing from redis " + dataClass.getSimpleName() + " [" + objectUUID + "]");
            RBucket<String> objectCache = getObjectCache(dataClass, objectUUID);
            return objectCache.delete();
        });
    }

    @Override
    public Set<UUID> getSavedUUIDs(@NotNull Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        return getKeys(dataClass).stream().map(s -> UUID.fromString(s.split(":")[1])).collect(Collectors.toSet());
    }

    @Override
    public AttachedPipeline getAttachedPipeline() {
        return attachedPipeline;
    }

    private synchronized RBucket<String> getObjectCache(@Nonnull Class<? extends IPipelineData> dataClass, @Nonnull @NotNull UUID objectUUID) {
        verifyInput(dataClass, objectUUID);

        String classifier = AnnotationResolver
                .getDataStorageClassifier(dataClass)
                .isEmpty() ? ":" : AnnotationResolver.getDataStorageClassifier(dataClass) + ":";
        String key = "VPipeline:" + classifier + objectUUID + ":" + AnnotationResolver.getDataStorageIdentifier(dataClass);

        RBucket<String> objectCache = redissonClient.getBucket(key, new StringCodec());
        updateExpireTime(dataClass, objectCache);

        return objectCache;
    }

    private Set<String> getKeys(Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        String storageIdentifier = AnnotationResolver.getDataStorageIdentifier(dataClass);
        String classifier = AnnotationResolver.getDataStorageClassifier(dataClass);
        return redissonClient.getKeys().getKeysStream().filter(s -> {
            String[] parts = s.split(":");

            if (classifier.isEmpty())
                return parts[3].equals(storageIdentifier);
            else
                return parts[1].equals(classifier) && parts[3].equals(storageIdentifier);

        }).collect(Collectors.toSet());
    }

    private void updateExpireTime(@NotNull Class<? extends IPipelineData> dataClass, RBucket<?> bucket) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        PipelineDataProperties properties = AnnotationResolver.getDataProperties(dataClass);

        if (bucket == null)
            return;

        if (properties.cleanOnNoUse())
            bucket.expire(java.time.Duration.ofSeconds(properties.timeUnit().toSeconds(properties.time())));
        else
            bucket.expire(Duration.ofHours(12));
    }

    private void verifyInput(@Nonnull Class<? extends IPipelineData> dataClass, @Nonnull @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
    }

    private RReadWriteLock getLock(@Nonnull Class<? extends IPipelineData> dataClass, @Nonnull @NotNull UUID objectUUID) {
        verifyInput(dataClass, objectUUID);
        String storageIdentifier = AnnotationResolver.getDataStorageIdentifier(dataClass);
        return redissonClient.getReadWriteLock(storageIdentifier + ":" + objectUUID);
    }

    private boolean performOnWriteLock(@Nonnull Class<? extends IPipelineData> dataClass, @Nonnull @NotNull UUID objectUUID, Supplier<Boolean> runnable) {
        var lock = getLock(dataClass, objectUUID);
        lock.writeLock().lock();
        NetworkLogger
                .getLogger()
                .info("Performing redis write for " + dataClass.getSimpleName() + " [" + objectUUID + "]");
        try {
            return runnable.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private <T> T performOnReadLock(Class<? extends T> callbackType, @Nonnull Class<? extends IPipelineData> dataClass, @Nonnull @NotNull UUID objectUUID, Supplier<T> runnable) {
        var lock = getLock(dataClass, objectUUID);
        lock.readLock().lock();
        NetworkLogger
                .getLogger()
                .info("Performing redis read for " + dataClass.getSimpleName() + " [" + objectUUID + "]");
        try {
            return runnable.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {
        this.redissonClient.shutdown();
    }

    @Override
    public void shutdown() {
        disconnect();
    }

    @Override
    public <T extends IPipelineData> Lock acquireGlobalObjectReadLock(@NotNull Class<? extends T> dataClass, @NotNull UUID objectUUID) {
        verifyInput(dataClass, objectUUID);
        String storageIdentifier = AnnotationResolver.getDataStorageIdentifier(dataClass);
        return redissonClient.getReadWriteLock(storageIdentifier + ":" + objectUUID).readLock();
    }

    @Override
    public <T extends IPipelineData> Lock acquireGlobalObjectWriteLock(@NotNull Class<? extends T> dataClass, @NotNull UUID objectUUID) {
        verifyInput(dataClass, objectUUID);
        String storageIdentifier = AnnotationResolver.getDataStorageIdentifier(dataClass);
        return redissonClient.getReadWriteLock(storageIdentifier + ":" + objectUUID).writeLock();
    }
}
