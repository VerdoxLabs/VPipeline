package de.verdox.vpipeline.impl.pipeline.core;

import de.verdox.vpipeline.api.pipeline.core.IPipeline;
import de.verdox.vpipeline.api.pipeline.core.IPipelineSynchronizer;
import de.verdox.vpipeline.api.pipeline.core.IPipelineTask;
import de.verdox.vpipeline.api.pipeline.datatypes.ISynchronizingService;
import de.verdox.vpipeline.api.pipeline.datatypes.IDataRegistry;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.IGlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.IGlobalStorage;
import de.verdox.vpipeline.api.pipeline.parts.ILocalCache;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import de.verdox.vpipeline.api.pipeline.enums.PreloadStrategy;
import de.verdox.vpipeline.impl.pipeline.datatypes.DataRegistry;
import de.verdox.vpipeline.impl.util.CatchingRunnable;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:31
 */
public class Pipeline implements IPipeline {

    private final IGlobalStorage globalStorage;
    private final IGlobalCache globalCache;
    private final ILocalCache localCache;
    private final ISynchronizingService synchronizingService;
    private final ExecutorService executorService;
    private final PipelineTaskScheduler pipelineTaskScheduler;
    private final PipelineSynchronizer pipelineSynchronizer;
    private final DataRegistry dataRegistry;

    public Pipeline(@NotNull ILocalCache localCache, @Nullable IGlobalCache globalCache, @Nullable IGlobalStorage globalStorage, @Nullable ISynchronizingService synchronizingService) {
        this.globalStorage = globalStorage;
        this.globalCache = globalCache;
        this.localCache = localCache;
        this.synchronizingService = synchronizingService;
        this.executorService = Executors.newFixedThreadPool(2, new DefaultThreadFactory("VPipeline-ThreadPool"));
        this.pipelineTaskScheduler = new PipelineTaskScheduler(this);
        this.pipelineSynchronizer = new PipelineSynchronizer(this);
        this.dataRegistry = new DataRegistry();
    }

    @Override
    public ILocalCache getLocalCache() {
        return localCache;
    }

    @Override
    public @Nullable ISynchronizingService getSynchronizingService() {
        return synchronizingService;
    }

    @Override
    public @Nullable IGlobalCache getGlobalCache() {
        return globalCache;
    }

    @Override
    public @Nullable IGlobalStorage getGlobalStorage() {
        return globalStorage;
    }

    @Override
    public @NotNull IDataRegistry getDataRegistry() {
        return dataRegistry;
    }

    @Override
    public void saveAll() {
        dataRegistry.getAllTypes().forEach(type -> getLocalCache().getSavedUUIDs(type).forEach(uuid -> sync(type, uuid)));
    }

    @Override
    public void preloadAll() {
        dataRegistry.getAllTypes().forEach(this::preloadData);
    }

    @Override
    public <T extends IPipelineData> T load(@NotNull Class<? extends T> type, @NotNull UUID uuid, @NotNull LoadingStrategy loadingStrategy, boolean createIfNotExist, @Nullable Consumer<T> callback) {
        Objects.requireNonNull(type, "Dataclass can't be null");
        Objects.requireNonNull(uuid, "UUID can't be null");

        IPipelineTask<T> task = pipelineTaskScheduler.schedulePipelineTask(type, uuid);

        return switch (loadingStrategy) {
            case LOAD_LOCAL -> performLoadLocal(type, uuid, loadingStrategy, createIfNotExist, callback, task);
            case LOAD_LOCAL_ELSE_LOAD -> performLoadLocalElseLoad(type, uuid, loadingStrategy, createIfNotExist, callback, task);
            case LOAD_PIPELINE -> performLoadPipeline(type, uuid, createIfNotExist, callback, task);
        };
    }

    @Override
    public @NotNull <T extends IPipelineData> Set<T> loadAllData(@NotNull Class<? extends T> type, @NotNull LoadingStrategy loadingStrategy) {
        Objects.requireNonNull(type, "Dataclass can't be null");
        Set<T> set = new HashSet<>();
        if (loadingStrategy.equals(LoadingStrategy.LOAD_PIPELINE)) syncPipeline(type);
        else if (loadingStrategy.equals(LoadingStrategy.LOAD_LOCAL_ELSE_LOAD))
            executorService.submit(new CatchingRunnable(() -> syncPipeline(type)));
        getLocalCache().getSavedUUIDs(type).forEach(uuid -> set.add(getLocalCache().loadObject(type, uuid)));
        return set;
    }

    @Override
    public <T extends IPipelineData> boolean exist(@NotNull Class<? extends T> type, @NotNull UUID uuid, @NotNull QueryStrategy... strategies) {
        Objects.requireNonNull(type, "Dataclass can't be null");
        Objects.requireNonNull(uuid, "UUID can't be null");
        if (strategies.length == 0) return false;
        Set<QueryStrategy> strategySet = Arrays.stream(strategies).collect(Collectors.toSet());

        if (strategySet.contains(QueryStrategy.ALL) || strategySet.contains(QueryStrategy.LOCAL)) {
            boolean localExist = getLocalCache().dataExist(type, uuid);
            if (localExist) return true;
        }
        if (strategySet.contains(QueryStrategy.ALL) || strategySet.contains(QueryStrategy.GLOBAL_CACHE)) {
            if (getGlobalCache() != null) {
                boolean globalCacheExists = getGlobalCache().dataExist(type, uuid);
                if (globalCacheExists) return true;
            }
        }
        if (strategySet.contains(QueryStrategy.ALL) || strategySet.contains(QueryStrategy.GLOBAL_STORAGE)) {
            if (getGlobalStorage() != null) return getGlobalStorage().dataExist(type, uuid);
        }
        return false;
    }

    @Override
    public <T extends IPipelineData> boolean delete(@NotNull Class<? extends T> type, @NotNull UUID uuid, boolean notifyOthers, @NotNull QueryStrategy... strategies) {
        Objects.requireNonNull(type, "Dataclass can't be null");
        Objects.requireNonNull(uuid, "UUID can't be null");
        Set<QueryStrategy> strategySet = Arrays.stream(strategies).collect(Collectors.toSet());
        if (strategySet.isEmpty()) strategySet.add(QueryStrategy.ALL);
        if (strategySet.contains(QueryStrategy.ALL) || strategySet.contains(QueryStrategy.LOCAL)) {
            T data = getLocalCache().loadObject(type, uuid);
            if (data == null)
                throw new IllegalStateException("Data does not exist in local Cache [" + type.getSimpleName() + " | " + uuid + "]");
            if (!getLocalCache().remove(type, uuid))
                throw new IllegalStateException("Could not delete data from local Cache [" + type.getSimpleName() + " | " + uuid + "]");
            data.onDelete();
            if (notifyOthers) data.getSynchronizer().pushRemoval(data, null);
        }
        if (getGlobalCache() != null && (strategySet.contains(QueryStrategy.ALL) || strategySet.contains(QueryStrategy.GLOBAL_CACHE))) {
            if (!getGlobalCache().remove(type, uuid))
                throw new IllegalStateException("Could not delete data from Global Cache [" + type.getSimpleName() + " | " + uuid + "]");
        }
        if (getGlobalStorage() != null && (strategySet.contains(QueryStrategy.ALL) || strategySet.contains(QueryStrategy.GLOBAL_STORAGE))) {
            if (!getGlobalStorage().remove(type, uuid))
                throw new IllegalStateException("Could not delete data from Global Storage [" + type.getSimpleName() + " | " + uuid + "]");
        }
        return true;
    }

    @Override
    public IPipelineSynchronizer getPipelineSynchronizer() {
        return pipelineSynchronizer;
    }

    @Override
    public void shutdown() {
        pipelineSynchronizer.shutdown();
        executorService.shutdown();
        pipelineTaskScheduler.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    private <T extends IPipelineData> T performLoadLocal(@NotNull Class<? extends T> type, @NotNull UUID uuid, @NotNull LoadingStrategy loadingStrategy, boolean createIfNotExist, @Nullable Consumer<T> callback, IPipelineTask<T> pipelineTask) {
        T data;

        if (!localCache.dataExist(type, uuid)) {
            if (!createIfNotExist) {
                pipelineTask.getFutureObject().complete(null);
                throw new NullPointerException(type + " with uuid " + uuid + " does not exist in local!");
            }
            createNewData(type, uuid);
        }
        data = localCache.loadObject(type, uuid);
        pipelineTask.getFutureObject().complete(data);
        if (callback != null) callback.accept(data);
        return data;
    }

    private <T extends IPipelineData> T performLoadLocalElseLoad(@NotNull Class<? extends T> type, @NotNull UUID uuid, @NotNull LoadingStrategy loadingStrategy, boolean createIfNotExist, @Nullable Consumer<T> callback, IPipelineTask<T> pipelineTask) {
        if (localCache.dataExist(type, uuid))
            return performLoadLocal(type, uuid, loadingStrategy, createIfNotExist, callback, pipelineTask);
        executorService.submit(new CatchingRunnable(() -> performLoadPipeline(type, uuid, createIfNotExist, callback, pipelineTask)));
        return null;
    }

    private <T extends IPipelineData> T performLoadPipeline(@NotNull Class<? extends T> type, @NotNull UUID uuid, boolean createIfNotExist, @Nullable Consumer<T> callback, IPipelineTask<T> pipelineTask) {
        T data = loadFromPipeline(type, uuid, createIfNotExist);
        pipelineTask.getFutureObject().complete(data);
        if (callback != null) callback.accept(data);
        return data;
    }

    private <T extends IPipelineData> T loadFromPipeline(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid, boolean createIfNotExist) {
        Objects.requireNonNull(dataClass, "Dataclass can't be null");
        Objects.requireNonNull(uuid, "UUID can't be null");

        if (localCache.dataExist(dataClass, uuid)) return localCache.loadObject(dataClass, uuid);
        else if (globalStorage != null && globalStorage.dataExist(dataClass, uuid) && AnnotationResolver.getDataProperties(dataClass).dataContext().isStorageAllowed()) {
            pipelineSynchronizer.synchronize(IPipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, IPipelineSynchronizer.DataSourceType.LOCAL, dataClass, uuid, null);
        } else if (globalCache != null && globalCache.dataExist(dataClass, uuid) && AnnotationResolver.getDataProperties(dataClass).dataContext().isCacheAllowed()) {
            pipelineSynchronizer.synchronize(IPipelineSynchronizer.DataSourceType.GLOBAL_CACHE, IPipelineSynchronizer.DataSourceType.LOCAL, dataClass, uuid, null);
        } else {
            if (!createIfNotExist) return null;
            return createNewData(dataClass, uuid);
        }

        return localCache.loadObject(dataClass, uuid);
    }

    private <T extends IPipelineData> T createNewData(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid) {
        Objects.requireNonNull(dataClass, "Dataclass can't be null");
        Objects.requireNonNull(uuid, "UUID can't be null");
        T pipelineData = localCache.instantiateData(dataClass, uuid);
        pipelineData.loadDependentData();
        pipelineData.onCreate();
        localCache.saveObject(pipelineData);

        if (AnnotationResolver.getDataProperties(dataClass).dataContext().isCacheAllowed())
            pipelineSynchronizer.synchronize(IPipelineSynchronizer.DataSourceType.LOCAL, IPipelineSynchronizer.DataSourceType.GLOBAL_CACHE, dataClass, uuid);
        if (AnnotationResolver.getDataProperties(dataClass).dataContext().isStorageAllowed())
            pipelineSynchronizer.synchronize(IPipelineSynchronizer.DataSourceType.LOCAL, IPipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, dataClass, uuid);
        return pipelineData;
    }

    private <T extends IPipelineData> void sync(@NotNull Class<? extends T> type, UUID uuid) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(uuid);
        IPipelineData data = getLocalCache().loadObject(type, uuid);
        if (data == null) return;
        //TODO: Mark for Removal in Pipeline
/*        if (data.isMarkedForRemoval())
            return;*/
        data.cleanUp();
        pipelineSynchronizer.synchronize(IPipelineSynchronizer.DataSourceType.LOCAL, IPipelineSynchronizer.DataSourceType.GLOBAL_CACHE, type, uuid, null);
        pipelineSynchronizer.synchronize(IPipelineSynchronizer.DataSourceType.LOCAL, IPipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, type, uuid, null);
        getLocalCache().remove(type, uuid);
    }

    private <T extends IPipelineData> void syncPipeline(@NotNull Class<? extends T> type) {
        Objects.requireNonNull(type, "Dataclass can't be null");
        if (getGlobalStorage() != null) getGlobalStorage().getSavedUUIDs(type).forEach(uuid -> {
            if (!localCache.dataExist(type, uuid))
                pipelineSynchronizer.synchronize(IPipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, IPipelineSynchronizer.DataSourceType.LOCAL, type, uuid, null);
        });
        if (getGlobalCache() != null) getGlobalCache().getSavedUUIDs(type).forEach(uuid -> {
            if (!localCache.dataExist(type, uuid))
                pipelineSynchronizer.synchronize(IPipelineSynchronizer.DataSourceType.GLOBAL_CACHE, IPipelineSynchronizer.DataSourceType.LOCAL, type, uuid, null);
        });
    }

    private <S extends IPipelineData> void preloadData(Class<? extends S> type) {
        Objects.requireNonNull(type, "Dataclass can't be null");
        PipelineDataProperties dataProperties = AnnotationResolver.getDataProperties(type);
        PreloadStrategy preloadStrategy = dataProperties.preloadStrategy();

        if (!preloadStrategy.equals(PreloadStrategy.LOAD_BEFORE)) return;
        Set<UUID> alreadyLoaded = new HashSet<>();
        if (globalCache != null && dataProperties.dataContext().isStorageAllowed())
            globalCache.getSavedUUIDs(type).forEach(uuid -> {
                if (pipelineSynchronizer.synchronize(IPipelineSynchronizer.DataSourceType.GLOBAL_CACHE, IPipelineSynchronizer.DataSourceType.LOCAL, type, uuid, null))
                    alreadyLoaded.add(uuid);
            });
        if (globalStorage != null && dataProperties.dataContext().isCacheAllowed())
            globalStorage
                    .getSavedUUIDs(type)
                    .stream()
                    .filter(uuid -> !alreadyLoaded.contains(uuid))
                    .forEach(uuid -> pipelineSynchronizer.synchronize(IPipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, IPipelineSynchronizer.DataSourceType.LOCAL, type, uuid, null));
    }
}
