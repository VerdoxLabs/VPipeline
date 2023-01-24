package de.verdox.vpipeline.impl.pipeline.core;

import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.core.PipelineSynchronizer;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.DataProvider;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.impl.util.CallbackUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record PipelineSynchronizerImpl(PipelineImpl pipeline) implements PipelineSynchronizer {
    @Override
    public void synchronize(@NotNull DataSourceType source, @NotNull DataSourceType destination, @NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, Runnable callback) {
        verifyInput(source, destination, dataClass, objectUUID);
        pipeline.getExecutorService()
                .submit(() -> doSynchronize(source, destination, dataClass, objectUUID, callback));
    }

    @Override
    public CompletableFuture<Void> sync(@NotNull IPipelineData iPipelineData, boolean syncWithStorage) {
        var future = new CompletableFuture<Void>();
        pipeline.getExecutorService().submit(() -> {
            doSync(iPipelineData, syncWithStorage, () -> future.complete(null));
        });
        return future;
    }

    boolean doSynchronize(@NotNull DataSourceType source, @NotNull DataSourceType destination, @NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, Runnable callback) {
        if (source.equals(destination)) {
            NetworkLogger.getLogger().warning("Can't sync from " + source + " to " + destination);
            CallbackUtil.runIfNotNull(callback);
            return false;
        }
        if ((pipeline.getGlobalCache() == null || !AnnotationResolver
                .getDataProperties(dataClass)
                .dataContext()
                .isCacheAllowed()) && (source.equals(DataSourceType.GLOBAL_CACHE) || destination.equals(DataSourceType.GLOBAL_CACHE))) {
            CallbackUtil.runIfNotNull(callback);
            return false;
        }
        if ((pipeline.getGlobalStorage() == null || !AnnotationResolver
                .getDataProperties(dataClass)
                .dataContext()
                .isStorageAllowed()) && (source.equals(DataSourceType.GLOBAL_STORAGE) || destination.equals(DataSourceType.GLOBAL_STORAGE))) {
            CallbackUtil.runIfNotNull(callback);
            return false;
        }

        DataProvider sourceProvider = getProvider(source);
        if (!sourceProvider.dataExist(dataClass, objectUUID)) {
            NetworkLogger
                    .warning("Can't sync because data does not exist in " + source + " for " + dataClass.getSimpleName());
            CallbackUtil.runIfNotNull(callback);
            return false;
        }

        JsonElement data = sourceProvider.loadData(dataClass, objectUUID);
        if (data == null) {
            NetworkLogger
                    .warning("Data is null in " + source + " for " + dataClass.getSimpleName());
            CallbackUtil.runIfNotNull(callback);
            return false;
        }
        DataProvider destinationProvider = getProvider(destination);
        NetworkLogger
                .debug("Sync from " + source + " to " + destination + " for " + dataClass.getSimpleName() + " [" + objectUUID + "]");
        destinationProvider.save(dataClass, objectUUID, data);

        CallbackUtil.runIfNotNull(callback);
        return true;
    }

    void doSync(@NotNull IPipelineData iPipelineData, boolean syncWithStorage, Runnable callback) {
        doSynchronize(PipelineSynchronizer.DataSourceType.LOCAL, PipelineSynchronizer.DataSourceType.GLOBAL_CACHE, iPipelineData.getClass(), iPipelineData.getObjectUUID(), () -> {
            if (syncWithStorage)
                doSynchronize(DataSourceType.LOCAL, DataSourceType.GLOBAL_STORAGE, iPipelineData.getClass(), iPipelineData.getObjectUUID(), () -> {
                    syncLocalInstances(iPipelineData.getClass(), iPipelineData.getObjectUUID(), () -> CallbackUtil.runIfNotNull(callback));
                });
            else
                syncLocalInstances(iPipelineData.getClass(), iPipelineData.getObjectUUID(), () -> CallbackUtil.runIfNotNull(callback));
        });
    }

    void syncLocalInstances(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, Runnable callback) {
        var localObject = pipeline.getLocalCache().loadObjectOrThrow(dataClass, objectUUID);
        var synchronizer = localObject.getSynchronizer();
        NetworkLogger
                .debug("Syncing local instances for " + dataClass.getSimpleName() + " [" + objectUUID + "]");
        synchronizer.pushUpdate(localObject, callback);
    }

    @Override
    public void shutdown() {
/*        try {
            pipeline.getExecutorService().shutdown();
            pipeline.getExecutorService().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }

    private DataProvider getProvider(@NotNull DataSourceType destination) {
        return switch (destination) {
            case LOCAL -> pipeline.getLocalCache();
            case GLOBAL_CACHE -> pipeline.getGlobalCache();
            case GLOBAL_STORAGE -> pipeline.getGlobalStorage();
        };
    }


    private void verifyInput(@NotNull DataSourceType source, @NotNull DataSourceType destination, @NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(source, "source can't be null!");
        Objects.requireNonNull(destination, "destination can't be null!");
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
    }
}
