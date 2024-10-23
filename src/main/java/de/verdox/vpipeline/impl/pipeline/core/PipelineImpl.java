package de.verdox.vpipeline.impl.pipeline.core;

import com.google.gson.GsonBuilder;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataAccess;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import de.verdox.vpipeline.api.pipeline.core.NetworkDataLockingService;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.PipelineSynchronizer;
import de.verdox.vpipeline.api.pipeline.datatypes.DataRegistry;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.enums.PreloadStrategy;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.api.pipeline.parts.LocalCache;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataSubscriber;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.impl.pipeline.datatypes.DataRegistryImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PipelineImpl implements Pipeline {
    private final GlobalStorage globalStorage;
    private final GlobalCache globalCache;
    private final LocalCache localCache;
    private final SynchronizingService synchronizingService;
    private final PipelineSynchronizerImpl pipelineSynchronizer;
    private final DataRegistryImpl dataRegistry;
    private NetworkParticipant networkParticipant;
    private boolean ready;
    private final NetworkDataLockingService networkDataLockingService;
    private final Consumer<GsonBuilder> gsonBuilderConsumer;

    public PipelineImpl(@NotNull LocalCache localCache, @NotNull NetworkDataLockingService networkDataLockingService, @Nullable GlobalCache globalCache, @Nullable GlobalStorage globalStorage, @Nullable SynchronizingService synchronizingService, @Nullable Consumer<GsonBuilder> gsonBuilderConsumer) {
        this.networkDataLockingService = networkDataLockingService;
        this.gsonBuilderConsumer = gsonBuilderConsumer;
        this.globalStorage = globalStorage;
        this.globalCache = globalCache;
        this.localCache = localCache;
        this.synchronizingService = synchronizingService;
        this.pipelineSynchronizer = new PipelineSynchronizerImpl(this);
        this.dataRegistry = new DataRegistryImpl(this);

        this.localCache
                .getAttachedPipeline()
                .attachPipeline(this);
        if (globalCache != null)
            this.globalCache
                    .getAttachedPipeline()
                    .attachPipeline(this);
        if (globalStorage != null)
            this.globalStorage
                    .getAttachedPipeline()
                    .attachPipeline(this);
        this.ready = true;
        NetworkLogger.info("Pipeline started");
    }

    public void setNetworkParticipant(NetworkParticipant networkParticipant) {
        this.networkParticipant = networkParticipant;
    }

    @Override
    public NetworkParticipant getNetworkParticipant() {
        return networkParticipant;
    }

    @Override
    public LocalCache getLocalCache() {
        return localCache;
    }

    @Override
    public @Nullable SynchronizingService getSynchronizingService() {
        return synchronizingService;
    }

    @Override
    public @NotNull PipelineSynchronizer getPipelineSynchronizer() {
        return pipelineSynchronizer;
    }

    @Override
    public @Nullable GlobalCache getGlobalCache() {
        return globalCache;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public @Nullable GlobalStorage getGlobalStorage() {
        return globalStorage;
    }

    @Override
    public @NotNull NetworkDataLockingService getNetworkDataLockingService() {
        return networkDataLockingService;
    }

    @Override
    public @NotNull DataRegistry getDataRegistry() {
        return dataRegistry;
    }

    @Override
    public @NotNull GsonBuilder getGsonBuilder() {
        var builder = new GsonBuilder()
                .serializeNulls();
        if (gsonBuilderConsumer != null)
            gsonBuilderConsumer.accept(builder);
        return builder;
    }


    @Override
    public void saveAll() {
        dataRegistry
                .getAllTypes()
                .forEach(type -> getLocalCache()
                        .getSavedUUIDs(type)
                        .forEach(uuid -> sync(type, uuid)));
    }

    @Override
    public void preloadAll() {
        dataRegistry
                .getAllTypes()
                .forEach(this::preloadData);
    }

    @Override
    public <T extends IPipelineData> @Nullable DataAccess<T> load(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid) {
        Objects.requireNonNull(dataClass, "dataClass can't be null");
        Objects.requireNonNull(uuid, "uuid can't be null");
        if (!getDataRegistry().isTypeRegistered(dataClass))
            throw new IllegalStateException("dataclass " + dataClass.getSimpleName() + " not registered in pipeline data registry");

        Lock readLock = getNetworkDataLockingService().getReadLock(dataClass, uuid);
        readLock.lock();
        try {
            T data = tryLoad(dataClass, uuid);
            if (data == null)
                return null;
            return createAccess(data);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public @NotNull <T extends IPipelineData> DataAccess<T> loadOrCreate(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid, @Nullable Consumer<T> immediateWriteOperation) {
        Objects.requireNonNull(dataClass, "dataClass can't be null");
        Objects.requireNonNull(uuid, "uuid can't be null");
        if (!getDataRegistry().isTypeRegistered(dataClass))
            throw new IllegalStateException("dataclass " + dataClass.getSimpleName() + " not registered in pipeline data registry");

        //First we try to load
        DataAccess<T> access = load(dataClass, uuid);
        if (access != null)
            return access;

        // If no data was found we want to create new data. We have to trigger a load again but this time in write mode.
        // We need to do the load again since someone could have created the data between the read call and this call.
        // Nonetheless, this approach is preferred since the alternative is to always do the loadOrCreate with a write lock which is not preferable performance wise.
        Lock writeLock = getNetworkDataLockingService().getWriteLock(dataClass, uuid);
        writeLock.lock();

        try {
            T loadedData = tryLoad(dataClass, uuid);
            if (loadedData == null) {
                if (AnnotationResolver.getDataProperties(dataClass).debugMode())
                    NetworkLogger.debug("Creating new " + dataClass.getSimpleName() + " [" + uuid + "]");
                loadedData = createNewData(dataClass, uuid, immediateWriteOperation);
                if (immediateWriteOperation != null)
                    immediateWriteOperation.accept(loadedData);
            }
            return createAccess(loadedData);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public <T extends IPipelineData> Set<DataAccess<? extends T>> loadAllData(@NotNull Class<? extends T> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null");
        if (!getDataRegistry().isTypeRegistered(dataClass))
            throw new IllegalStateException("dataclass " + dataClass.getSimpleName() + " not registered in pipeline data registry");


        //Syncing data from storage to local cache
        if (getGlobalStorage() != null && AnnotationResolver.getDataProperties(dataClass).dataContext()
                .isStorageAllowed()) {
            getGlobalStorage()
                    .getSavedUUIDs(dataClass)
                    .parallelStream()
                    .forEach(uuid -> {
                        if (!localCache.dataExist(dataClass, uuid))
                            pipelineSynchronizer.synchronizePipelineData(PipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, PipelineSynchronizer.DataSourceType.LOCAL, dataClass, uuid);
                    });

        }

        //Syncing data from global cache to local cache
        if (getGlobalCache() != null && AnnotationResolver.getDataProperties(dataClass).dataContext()
                .isCacheAllowed()) {
            getGlobalCache()
                    .getSavedUUIDs(dataClass)
                    .parallelStream()
                    .forEach(uuid -> {
                        if (!localCache.dataExist(dataClass, uuid))
                            pipelineSynchronizer.synchronizePipelineData(PipelineSynchronizer.DataSourceType.GLOBAL_CACHE, PipelineSynchronizer.DataSourceType.LOCAL, dataClass, uuid);
                    });
        }
        return getLocalCache().loadAllData(dataClass).stream().map(this::createAccess).collect(Collectors.toSet());
    }

    @Override
    public <T extends IPipelineData> boolean exist(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid) {
        Objects.requireNonNull(dataClass, "dataClass can't be null");
        Objects.requireNonNull(uuid, "uuid can't be null");
        if (!getDataRegistry().isTypeRegistered(dataClass))
            throw new IllegalStateException("dataclass " + dataClass.getSimpleName() + " not registered in pipeline data registry");
        Lock lock = getNetworkDataLockingService().getReadLock(dataClass, uuid);
        lock.lock();
        try {
            return checkExistence(dataClass, uuid);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T extends IPipelineData> boolean delete(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid) {
        if (!getDataRegistry().isTypeRegistered(dataClass))
            throw new IllegalStateException("dataclass " + dataClass.getSimpleName() + " not registered in pipeline data registry");
        Lock lock = getNetworkDataLockingService().getWriteLock(dataClass, uuid);
        lock.lock();

        try {
            var deleted = getLocalCache().remove(dataClass, uuid);
            if (getSynchronizingService() != null) {
                getSynchronizingService()
                        .getOrCreate(this, dataClass)
                        .pushRemoval(uuid);
            }

            if (getGlobalCache() != null && getGlobalCache().dataExist(dataClass, uuid))
                deleted &= getGlobalCache().remove(dataClass, uuid);
            if (getGlobalStorage() != null && getGlobalStorage().dataExist(dataClass, uuid))
                deleted &= getGlobalStorage().remove(dataClass, uuid);

            if (AnnotationResolver.getDataProperties(dataClass).debugMode())
                NetworkLogger.debug("Deleted: " + dataClass + " with " + uuid);
            return deleted;
        } finally {
            lock.unlock();
        }
    }

    private <T extends IPipelineData> T tryLoad(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid) {
        if (!getDataRegistry().isTypeRegistered(dataClass))
            throw new IllegalStateException("dataclass " + dataClass.getSimpleName() + " not registered in pipeline data registry");
        if (localCache.dataExist(dataClass, uuid)) {
            return localCache.loadObject(dataClass, uuid);
        } else if (globalCache != null && globalCache.dataExist(dataClass, uuid) && AnnotationResolver
                .getDataProperties(dataClass)
                .dataContext()
                .isCacheAllowed()) {
            pipelineSynchronizer.synchronizePipelineData(PipelineSynchronizer.DataSourceType.GLOBAL_CACHE, PipelineSynchronizer.DataSourceType.LOCAL, dataClass, uuid);
            if (AnnotationResolver.getDataProperties(dataClass).debugMode())
                NetworkLogger.debug("CACHE -> Local | " + dataClass + " [" + uuid + "]");
        } else if (globalStorage != null && globalStorage.dataExist(dataClass, uuid) && AnnotationResolver
                .getDataProperties(dataClass)
                .dataContext()
                .isStorageAllowed()) {
            pipelineSynchronizer.synchronizePipelineData(PipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, PipelineSynchronizer.DataSourceType.LOCAL, dataClass, uuid);
            if (AnnotationResolver.getDataProperties(dataClass).debugMode())
                NetworkLogger
                        .debug("GLOBAL -> Local | " + dataClass.getSimpleName() + " [" + uuid + "]");
        } else
            return null;
        return localCache.loadObject(dataClass, uuid);
    }

    private <T extends IPipelineData> boolean checkExistence(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid) {
        boolean localExist = getLocalCache().dataExist(dataClass, uuid);
        if (localExist) return true;

        if (getGlobalCache() != null) {
            boolean globalCacheExists = getGlobalCache().dataExist(dataClass, uuid);
            if (globalCacheExists) return true;
        }

        if (getGlobalStorage() != null) return getGlobalStorage().dataExist(dataClass, uuid);
        return false;
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
        pipelineSynchronizer.synchronizePipelineData(PipelineSynchronizer.DataSourceType.LOCAL, PipelineSynchronizer.DataSourceType.GLOBAL_CACHE, type, uuid);
        pipelineSynchronizer.synchronizePipelineData(PipelineSynchronizer.DataSourceType.LOCAL, PipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, type, uuid);
        getLocalCache().remove(type, uuid);
    }

    private <S extends IPipelineData> void preloadData(Class<? extends S> type) {
        Objects.requireNonNull(type, "Dataclass can't be null");
        PipelineDataProperties dataProperties = AnnotationResolver.getDataProperties(type);
        PreloadStrategy preloadStrategy = dataProperties.preloadStrategy();

        if (!preloadStrategy.equals(PreloadStrategy.LOAD_BEFORE)) return;
        Set<UUID> alreadyLoaded = new HashSet<>();
        if (globalCache != null && dataProperties
                .dataContext()
                .isStorageAllowed())
            globalCache
                    .getSavedUUIDs(type)
                    .forEach(uuid -> {
                        if (pipelineSynchronizer.synchronizePipelineData(PipelineSynchronizer.DataSourceType.GLOBAL_CACHE, PipelineSynchronizer.DataSourceType.LOCAL, type, uuid))
                            alreadyLoaded.add(uuid);
                    });
        if (globalStorage != null && dataProperties
                .dataContext()
                .isCacheAllowed())
            globalStorage
                    .getSavedUUIDs(type)
                    .stream()
                    .filter(uuid -> !alreadyLoaded.contains(uuid))
                    .forEach(uuid -> pipelineSynchronizer.synchronizePipelineData(PipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, PipelineSynchronizer.DataSourceType.LOCAL, type, uuid));
    }

    private <T extends IPipelineData> T createNewData(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid, @Nullable Consumer<T> immediateWriteOperation) {
        Objects.requireNonNull(dataClass, "Dataclass can't be null");
        Objects.requireNonNull(uuid, "UUID can't be null");
        if (AnnotationResolver.getDataProperties(dataClass).debugMode())
            NetworkLogger
                    .debug("[Pipeline] Creating new data of type: " + dataClass.getSimpleName() + " [" + uuid + "]");


        T pipelineData = localCache.instantiateData(dataClass, uuid);
        pipelineData.loadDependentData();
        pipelineData.onCreate();
        if (immediateWriteOperation != null)
            immediateWriteOperation.accept(pipelineData);
        localCache.saveObject(pipelineData);

        if (AnnotationResolver
                .getDataProperties(dataClass)
                .dataContext()
                .isCacheAllowed())
            pipelineSynchronizer.synchronizePipelineData(PipelineSynchronizer.DataSourceType.LOCAL, PipelineSynchronizer.DataSourceType.GLOBAL_CACHE, dataClass, uuid);
        if (AnnotationResolver
                .getDataProperties(dataClass)
                .dataContext()
                .isStorageAllowed())
            pipelineSynchronizer.synchronizePipelineData(PipelineSynchronizer.DataSourceType.LOCAL, PipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, dataClass, uuid);

        if (getSynchronizingService() != null)
            getSynchronizingService()
                    .getOrCreate(this, dataClass)
                    .pushCreation(pipelineData);

        return pipelineData;

    }

    @Override
    public synchronized void shutdown() {
        if (!this.ready) {
            NetworkLogger.info("Pipeline is already offline");
            return;
        }
        this.ready = false;

        NetworkLogger.info("Saving all data");
        saveAll();

        NetworkLogger.info("Shutting down pipeline synchronizer");
        getPipelineSynchronizer().shutdown();

        NetworkLogger.info("Shutting down data providers");

        if (getGlobalStorage() != null)
            getGlobalStorage().shutdown();
        if (getGlobalCache() != null)
            getGlobalCache().shutdown();
        if (getSynchronizingService() != null)
            getSynchronizingService().shutdown();
        getLocalCache().shutdown();
        NetworkLogger.info("Pipeline offline");
    }

    @Override
    public <T extends IPipelineData> boolean saveAndRemoveFromLocalCache(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid) {
        pipelineSynchronizer.sync(dataClass, uuid, true);
        return getLocalCache().remove(dataClass, uuid);
    }

    @Override
    public <T extends IPipelineData> void subscribe(@NotNull Class<? extends T> type, @NotNull UUID uuid, DataSubscriber<T, ?> subscriber) {
        getLocalCache().subscribe(type, uuid, subscriber);
    }

    @Override
    public <T extends IPipelineData> void removeSubscriber(DataSubscriber<T, ?> subscriber) {
        getLocalCache().removeSubscriber(subscriber);
    }

    private <T extends IPipelineData> DataAccess<T> createAccess(@NotNull T data) {
        return getLocalCache().createAccess((Class<? extends T>) data.getClass(), data.getObjectUUID());
    }
}
