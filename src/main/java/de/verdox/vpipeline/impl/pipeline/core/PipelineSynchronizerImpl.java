package de.verdox.vpipeline.impl.pipeline.core;

import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.pipeline.core.PipelineSynchronizer;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.DataProvider;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public record PipelineSynchronizerImpl(PipelineImpl pipeline) implements PipelineSynchronizer {
    @Override
    public void synchronize(@NotNull DataSourceType source, @NotNull DataSourceType destination, @NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, Runnable callback) {
        verifyInput(source, destination, dataClass, objectUUID);
        pipeline
                .getExecutorService()
                .submit(() -> pipeline
                        .createPipelineLock(dataClass, objectUUID)
                        .runOnWriteLock(() -> doSynchronisation(source, destination, dataClass, objectUUID, callback)));
    }

    boolean doSynchronisation(@NotNull DataSourceType source, @NotNull DataSourceType destination, @NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, Runnable callback) {

        if (source.equals(destination))
            return false;
        if ((pipeline.getGlobalCache() == null || !AnnotationResolver
                .getDataProperties(dataClass)
                .dataContext()
                .isCacheAllowed()) && (source.equals(DataSourceType.GLOBAL_CACHE) || destination.equals(DataSourceType.GLOBAL_CACHE)))
            return false;
        if ((pipeline.getGlobalStorage() == null || !AnnotationResolver
                .getDataProperties(dataClass)
                .dataContext()
                .isStorageAllowed()) && (source.equals(DataSourceType.GLOBAL_STORAGE) || destination.equals(DataSourceType.GLOBAL_STORAGE)))
            return false;

        DataProvider sourceProvider = getProvider(source);
        if (!sourceProvider.dataExist(dataClass, objectUUID))
            return false;
        JsonElement data = sourceProvider.loadData(dataClass, objectUUID);
        if (data == null)
            return false;
        DataProvider destinationProvider = getProvider(destination);
        destinationProvider.save(dataClass, objectUUID, data);

        //pipelineManager.getPlugin().consoleMessage("&eDone syncing &b" + System.currentTimeMillis(), true);
        if (callback != null)
            callback.run();
        return true;
    }

    @Override
    public void shutdown() {
        try {
            pipeline.getExecutorService().shutdown();
            pipeline.getExecutorService().awaitTermination(5, TimeUnit.SECONDS);
            //TODO: Print Log Message
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
