package de.verdox.vpipeline.api.modules.redis.synchronizer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.SynchronizedAccess;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.PipelineSynchronizer;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.Synchronizer;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 16:11
 */
public class RedisDataSynchronizer implements Synchronizer {
    private final RTopic dataTopic;
    private final MessageListener<DataBlock> messageListener;
    private final UUID senderUUID = UUID.randomUUID();
    private final AttachedPipeline attachedPipeline;

    RedisDataSynchronizer(@NotNull Pipeline pipeline, @NotNull RedisConnection redisConnection, @NotNull Class<? extends IPipelineData> dataClass) {
        this.attachedPipeline = new AttachedPipeline(GsonBuilder::create);
        this.attachedPipeline.attachPipeline(pipeline);
        Objects.requireNonNull(redisConnection, "redisConnection can't be null!");
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        this.dataTopic = redisConnection.getTopic(AnnotationResolver.getDataStorageClassifier(dataClass), dataClass);
        this.messageListener = (channel, dataBlock) -> {
            if (dataBlock.senderUUID.equals(senderUUID))
                return;
            IPipelineData data = pipeline.getLocalCache().loadObject(dataClass, dataBlock.dataUUID);
            if (data == null)
                return;
            if (dataBlock instanceof UpdateDataBlock updateDataBlock) {
                NetworkLogger
                        .getLogger()
                        .fine("Received network sync for " + dataClass.getSimpleName() + " [" + data + " | " + dataBlock.dataUUID + "]");
                data.onSync(data.deserialize(JsonParser
                        .parseString(updateDataBlock.dataToUpdate)
                        .getAsJsonObject()));
            } else if (dataBlock instanceof RemoveDataBlock) {
                data.markRemoval();
                pipeline.getLocalCache().remove(dataClass, data.getObjectUUID());
            }
        };
        dataTopic.addListener(DataBlock.class, messageListener);
    }

    @Override
    public void cleanUp() {
        dataTopic.removeListener(messageListener);
    }

    @Override
    public void pushUpdate(@NotNull IPipelineData data, @Nullable Runnable callback) {
        Objects.requireNonNull(data, "vCoreData can't be null!");
        if (data.isMarkedForRemoval())
            return;
        attachedPipeline.getAttachedPipeline()
                        .getPipelineSynchronizer()
                        .synchronize(PipelineSynchronizer.DataSourceType.LOCAL, PipelineSynchronizer.DataSourceType.GLOBAL_CACHE, data.getClass(), data.getObjectUUID(), () -> {
                            var count = dataTopic.publish(new UpdateDataBlock(senderUUID, data.getObjectUUID(), attachedPipeline
                                    .getGson()
                                    .toJson(data.serialize())));
                            NetworkLogger
                                    .getLogger()
                                    .fine("Pushed network sync to " + count + " clients for " + data
                                            .getClass()
                                            .getSimpleName() + " [" + data + " | " + data.getObjectUUID() + "]");
                            if (callback != null)
                                callback.run();
                        });
    }

    @Override
    public void pushRemoval(@NotNull IPipelineData data, @Nullable Runnable callback) {
        Objects.requireNonNull(data, "vCoreData can't be null!");
        attachedPipeline
                .getAttachedPipeline()
                .load(data.getClass(), data.getObjectUUID())
                .thenApply(pipelineLock -> pipelineLock.runOnWriteLock(() -> {
                    dataTopic.publish(new RemoveDataBlock(senderUUID, data.getObjectUUID()));
                    NetworkLogger
                            .getLogger().fine("Pushed network removal for " + data
                                    .getClass()
                                    .getSimpleName() + " [" + data + " | " + data.getObjectUUID() + "]");
                    data.markRemoval();
                    if (callback != null)
                        callback.run();
                }));
    }

    @Override
    public void shutdown() {

    }

    public abstract static class DataBlock implements Serializable {
        protected final UUID senderUUID;
        protected final UUID dataUUID;

        DataBlock(@NotNull UUID senderUUID, @NotNull UUID dataUUID) {
            this.senderUUID = senderUUID;
            this.dataUUID = dataUUID;
        }
    }

    public static class RemoveDataBlock extends DataBlock {
        RemoveDataBlock(@NotNull UUID senderUUID, @NotNull UUID dataUUID) {
            super(senderUUID, dataUUID);
        }
    }

    public static class UpdateDataBlock extends DataBlock {
        private final String dataToUpdate;

        UpdateDataBlock(@NotNull UUID senderUUID, @NotNull UUID dataUUID, String dataToUpdate) {
            super(senderUUID, dataUUID);
            this.dataToUpdate = dataToUpdate;
        }
    }
}
