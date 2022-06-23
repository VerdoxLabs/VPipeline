package de.verdox.vpipeline.impl.modules.redis.globalcache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.IGlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.IRemoteStorage;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBucket;
import org.redisson.client.codec.StringCodec;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 15:50
 */
public class RedisCache extends RedisConnection implements IGlobalCache, IRemoteStorage {
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public RedisCache(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        super(clusterMode, addressArray, redisPassword);
    }

    @Override
    public JsonElement loadData(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        verifyInput(dataClass, objectUUID);
        try {
            return JsonParser.parseString(getObjectCache(dataClass, objectUUID).get()).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
            remove(dataClass, objectUUID);
            return null;
        }
    }

    @Override
    public boolean dataExist(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        verifyInput(dataClass, objectUUID);
        return getObjectCache(dataClass, objectUUID).isExists();
    }

    @Override
    public void save(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave) {
        verifyInput(dataClass, objectUUID);

        RBucket<String> objectCache = getObjectCache(dataClass, objectUUID);
        objectCache.set(gson.toJson(dataToSave));
        updateExpireTime(dataClass, objectCache);
    }

    @Override
    public boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        verifyInput(dataClass, objectUUID);

        RBucket<String> objectCache = getObjectCache(dataClass, objectUUID);
        return objectCache.delete();
    }

    @Override
    public Set<UUID> getSavedUUIDs(@NotNull Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        return getKeys(dataClass).stream().map(s -> UUID.fromString(s.split(":")[1])).collect(Collectors.toSet());
    }

    private synchronized RBucket<String> getObjectCache(@Nonnull Class<? extends IPipelineData> dataClass, @Nonnull @NotNull UUID objectUUID) {
        verifyInput(dataClass, objectUUID);

        String classifier = AnnotationResolver.getDataStorageClassifier(dataClass).isEmpty() ? ":" : AnnotationResolver.getDataStorageClassifier(dataClass) + ":";
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

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }
}
