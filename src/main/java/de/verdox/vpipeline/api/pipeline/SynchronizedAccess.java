package de.verdox.vpipeline.api.pipeline;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.io.Serializable;
import java.util.function.Consumer;

public class SynchronizedAccess<T extends IPipelineData> {
    private final T data;

    public SynchronizedAccess(T data) {
        this.data = data;
    }

    public SynchronizedAccess<T> read(Consumer<T> reader) {
        synchronized (data) {
            reader.accept(data);
        }
        return this;
    }

    public SynchronizedAccess<T> write(SynchronizedWrite<T> writer, boolean pushChanges) {
        synchronized (data) {
            NetworkLogger
                    .getLogger()
                    .info("[SynchronizedAccess] Writing on data " + data
                            .getClass()
                            .getSimpleName() + " [" + data.getObjectUUID() + "]");
            writer.accept(data);
            if (pushChanges)
                pushChanges(writer);
        }
        return this;
    }

    private void pushChanges(SynchronizedWrite<T> writer) {
        synchronized (data) {
            data.getSynchronizer().pushWrite(data, writer, () -> {
            });
        }
    }

    @FunctionalInterface
    public interface SynchronizedWrite<T> extends Consumer<T>, Serializable {
        void accept(T t);
    }
}
