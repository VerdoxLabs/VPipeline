package de.verdox.vpipeline.api.modules.redis.synchronizer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.Synchronizer;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.impl.util.CallbackUtil;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

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
    private Pipeline pipeline;
    private final Class<? extends IPipelineData> dataClass;

    RedisDataSynchronizer(@NotNull Class<? extends IPipelineData> dataClass, @NotNull Pipeline pipeline, @NotNull RedisConnection redisConnection) {
        this.dataClass = dataClass;
        Objects.requireNonNull(dataClass, "data can't be null!");
        Objects.requireNonNull(pipeline, "pipeline can't be null!");
        Objects.requireNonNull(redisConnection, "redisConnection can't be null!");
        this.pipeline = pipeline;
        this.attachedPipeline = new AttachedPipeline(GsonBuilder::create);
        this.attachedPipeline.attachPipeline(pipeline);
        this.dataTopic = redisConnection.getTopic(AnnotationResolver.getDataStorageClassifier(dataClass), dataClass);
        this.messageListener = (channel, dataBlock) -> {
            if (dataBlock.senderUUID.equals(senderUUID))
                return;
            IPipelineData remoteDataObject = pipeline.getLocalCache().loadObject(dataClass, dataBlock.dataUUID);
            if (remoteDataObject == null)
                return;
            if (dataBlock instanceof UpdateDataBlock updateDataBlock) {
                NetworkLogger
                        .info("Received network sync for " + dataClass.getSimpleName() + " [" + remoteDataObject + " | " + dataBlock.dataUUID + "]");
                remoteDataObject.onSync(remoteDataObject.deserialize(JsonParser
                        .parseString(updateDataBlock.dataToUpdate)
                        .getAsJsonObject()));
            } else if (dataBlock instanceof RemoveDataBlock) {
                NetworkLogger.info("Received network removal for " + dataClass.getSimpleName() + " [" + remoteDataObject + " | " + dataBlock.dataUUID + "]");
                if (!pipeline.getLocalCache().remove(dataClass, dataBlock.dataUUID))
                    NetworkLogger
                            .getLogger()
                            .warning("Could not remove after network removal instruction [" + pipeline
                                    .getLocalCache()
                                    .dataExist(dataClass, remoteDataObject.getObjectUUID()) + "]");
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
        try {
            Objects.requireNonNull(dataClass, "vCoreData can't be null!");
            Objects.requireNonNull(data, "data can't be null!");
            Objects.requireNonNull(callback, "callback can't be null!");
/*            if (data.isMarkedForRemoval()) {
                NetworkLogger
                        .getLogger()
                        .warning("Pushupdate on old / removed data clients for " + data + " [" + data.getObjectUUID() + "]");
                return;
            }*/
            var count = dataTopic.publish(new UpdateDataBlock(senderUUID, data.getObjectUUID(), attachedPipeline
                    .getGson()
                    .toJson(data.serialize())));
            NetworkLogger
                    .info("Pushed network sync to " + count + " clients for " + data + " [" + data.getObjectUUID() + "]");

        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            CallbackUtil.runIfNotNull(callback);
        }
    }

    @Override
    public void pushRemoval(@NotNull UUID uuid, @Nullable Runnable callback) {
        Objects.requireNonNull(uuid, "uuid can't be null!");
        try {
            dataTopic.publish(new RemoveDataBlock(senderUUID, uuid));
            NetworkLogger.info("Pushed network removal for " + dataClass.getSimpleName() + " [" + uuid + "]");
            /*            data.markRemoval();*/
        } finally {
            if (callback != null)
                callback.run();
        }
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
