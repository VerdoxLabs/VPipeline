package de.verdox.vpipeline.impl.pipeline.core;

import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.PipelineSynchronizer;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.DataSynchronizer;
import de.verdox.vpipeline.api.pipeline.parts.DataProvider;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public record PipelineSynchronizerImpl(Pipeline pipeline) implements PipelineSynchronizer {
    @Override
    public boolean synchronizePipelineData(@NotNull DataSourceType source, @NotNull DataSourceType destination, @NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        verifyInput(source, destination, dataClass, objectUUID);
        if (source.equals(destination)) {
            NetworkLogger.getLogger().warning("Can't sync from " + source + " to " + destination);
            return false;
        }
        if ((pipeline.getGlobalCache() == null || !AnnotationResolver
                .getDataProperties(dataClass)
                .dataContext()
                .isCacheAllowed()) && (source.equals(DataSourceType.GLOBAL_CACHE) || destination.equals(DataSourceType.GLOBAL_CACHE))) {
            return false;
        }
        if ((pipeline.getGlobalStorage() == null || !AnnotationResolver
                .getDataProperties(dataClass)
                .dataContext()
                .isStorageAllowed()) && (source.equals(DataSourceType.GLOBAL_STORAGE) || destination.equals(DataSourceType.GLOBAL_STORAGE))) {
            return false;
        }

        DataProvider sourceProvider = getProvider(source);
        DataProvider destinationProvider = getProvider(destination);

        if(sourceProvider.equals(destinationProvider))
            return true;

        if (!sourceProvider.dataExist(dataClass, objectUUID)) {
            NetworkLogger
                    .warning("Can't sync because data does not exist in " + source + " for " + dataClass.getSimpleName());
            return false;
        }

        JsonElement data = sourceProvider.loadData(dataClass, objectUUID);
        if (data == null) {
            NetworkLogger
                    .warning("Data is null in " + source + " for " + dataClass.getSimpleName());
            return false;
        }

        if (AnnotationResolver.getDataProperties(dataClass).debugMode())
            NetworkLogger
                    .debug("Sync from " + source + " to " + destination + " for " + dataClass.getSimpleName() + " [" + objectUUID + "]");
        destinationProvider.save(dataClass, objectUUID, data);
        return true;
    }

    @Override
    public void sync(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, boolean syncWithStorage) {
        if(!pipeline.getLocalCache().dataExist(dataClass, objectUUID))
            return;
        synchronizePipelineData(PipelineSynchronizer.DataSourceType.LOCAL, PipelineSynchronizer.DataSourceType.GLOBAL_CACHE, dataClass, objectUUID);
        if(syncWithStorage)
            synchronizePipelineData(PipelineSynchronizer.DataSourceType.LOCAL, DataSourceType.GLOBAL_STORAGE, dataClass, objectUUID);
        syncLocalInstances(dataClass, objectUUID);
    }

    void syncLocalInstances(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        IPipelineData localObject = pipeline.getLocalCache().loadObjectOrThrow(dataClass, objectUUID);
        DataSynchronizer dataSynchronizer = localObject.getSynchronizer();
        if (AnnotationResolver.getDataProperties(dataClass).debugMode())
            NetworkLogger
                    .debug("Syncing local instances for " + dataClass.getSimpleName() + " [" + objectUUID + "]");
        dataSynchronizer.pushUpdate(localObject);
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
