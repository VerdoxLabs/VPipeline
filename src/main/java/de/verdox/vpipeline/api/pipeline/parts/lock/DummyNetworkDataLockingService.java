package de.verdox.vpipeline.api.pipeline.parts.lock;

import de.verdox.vpipeline.api.pipeline.parts.NetworkDataLockingService;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DummyNetworkDataLockingService implements NetworkDataLockingService {
    private final Map<String, ReentrantReadWriteLock> localLocks = new ConcurrentHashMap<>();
    @Override
    public <T extends IPipelineData> Lock getReadLock(@NotNull Class<? extends T> type, @NotNull UUID uuid) {
        return localLocks.computeIfAbsent(createLockString(type, uuid), s -> new ReentrantReadWriteLock()).readLock();
    }

    @Override
    public <T extends IPipelineData> Lock getWriteLock(@NotNull Class<? extends T> type, @NotNull UUID uuid) {
        return localLocks.computeIfAbsent(createLockString(type, uuid), s -> new ReentrantReadWriteLock()).writeLock();
    }

    private <T extends IPipelineData> String createLockString(@NotNull Class<? extends T> type, @NotNull UUID uuid){
        String classifier = AnnotationResolver
                .getDataStorageClassifier(type)
                .isEmpty() ? "" : AnnotationResolver.getDataStorageClassifier(type) + ":";
        return classifier + uuid + ":" + AnnotationResolver.getDataStorageIdentifier(type);
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }
}
