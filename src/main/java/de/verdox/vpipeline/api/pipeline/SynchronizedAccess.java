package de.verdox.vpipeline.api.pipeline;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class SynchronizedAccess<T extends IPipelineData> {
    private final Pipeline pipeline;
    private final Class<? extends T> type;
    private final UUID uuid;

    public SynchronizedAccess(@NotNull T data){
        this.pipeline = data.getAttachedPipeline().getAttachedPipeline();
        this.type = (Class<? extends T>) data.getClass();
        this.uuid = data.getObjectUUID();
    }
    public SynchronizedAccess(@NotNull Pipeline pipeline, @NotNull Class<? extends T> type, UUID uuid) {
        this.pipeline = pipeline;
        this.type = type;
        this.uuid = uuid;
    }

    public SynchronizedAccess<T> read(Consumer<T> reader) {
        synchronized (pipeline.getLocalCache()) {
            var data = pipeline.getLocalCache().loadObject(type, uuid);
            if (data == null)
                return this;
            if (data.isMarkedForRemoval())
                return this;
            reader.accept(data);
        }
        return this;
    }

    public SynchronizedAccess<T> write(SynchronizedWrite<T> writer, boolean pushChanges) {

        synchronized (pipeline.getLocalCache()) {
            var data = pipeline.getLocalCache().loadObject(type, uuid);
            if (data == null)
                return this;
            if (data.isMarkedForRemoval())
                return this;
            NetworkLogger
                    .getLogger()
                    .info("[SynchronizedAccess] Writing on data " + data
                            .getClass()
                            .getSimpleName() + " [" + data.getObjectUUID() + "]");
            writer.write(data);
            if (pushChanges)
                pushChanges(writer);
        }
        return this;
    }

    public SynchronizedAccess<T> delete(boolean pushRemoval) {

        synchronized (pipeline.getLocalCache()) {
            var data = pipeline.getLocalCache().loadObject(type, uuid);
            if (data == null)
                return this;
            if (data.isMarkedForRemoval())
                return this;
            NetworkLogger
                    .getLogger()
                    .info("[SynchronizedAccess] Deleting data " + data
                            .getClass()
                            .getSimpleName() + " [" + data.getObjectUUID() + "]");
            data.markRemoval();
            data.getAttachedPipeline().getAttachedPipeline().delete(data.getClass(), data.getObjectUUID());
            if (pushRemoval)
                data.getSynchronizer().pushRemoval(data, () -> {
                });
            return this;
        }
    }

    private void pushChanges(SynchronizedWrite<T> writer) {
        synchronized (pipeline.getLocalCache()) {
            var data = pipeline.getLocalCache().loadObject(type, uuid);
            if (data == null)
                return;
            if (data.isMarkedForRemoval())
                return;
            data.getSynchronizer().pushUpdate(data, () -> {
            });
            /*            data.getSynchronizer().pushWrite(data, writer, () -> {});*/
        }
    }

    @FunctionalInterface
    public interface SynchronizedWrite<T> extends Serializable {
        void write(T t);
    }

    public Class<? extends T> getType() {
        return type;
    }

    public UUID getUuid() {
        return uuid;
    }
}
