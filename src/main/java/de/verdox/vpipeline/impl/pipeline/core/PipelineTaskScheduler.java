package de.verdox.vpipeline.impl.pipeline.core;

import de.verdox.vpipeline.api.pipeline.core.IPipelineTask;
import de.verdox.vpipeline.api.pipeline.core.IPipelineTaskScheduler;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:37
 */
public class PipelineTaskScheduler implements IPipelineTaskScheduler {

    private final Pipeline pipeline;
    private final Map<UUID, Map<Class<? extends IPipelineData>, IPipelineTask<?>>> pendingTasks = new ConcurrentHashMap<>();

    public PipelineTaskScheduler(@NotNull Pipeline pipeline) {
        Objects.requireNonNull(pipeline, "pipelineManager can't be null!");
        this.pipeline = pipeline;
    }

    @Override
    public synchronized <T extends IPipelineData> IPipelineTask<T> schedulePipelineTask(@NotNull Class<? extends T> type, @NotNull UUID uuid) {
        verifyInput(type, uuid);

        IPipelineTask<T> existingTask = getExistingPipelineTask(type, uuid);
        if (existingTask != null)
            return existingTask;

        CompletableFuture<T> future = new CompletableFuture<>();
        future.whenComplete((t, throwable) -> removePipelineTask(type, uuid));
        PipelineTask<T> task = new PipelineTask<>(type, uuid, future);

        pendingTasks.computeIfAbsent(uuid, uuid1 -> new ConcurrentHashMap<>()).put(type, task);
        return task;
    }

    private <T extends IPipelineData> IPipelineTask<T> getExistingPipelineTask(@NotNull Class<? extends T> type, @NotNull UUID uuid) {
        verifyInput(type, uuid);

        if (!pendingTasks.containsKey(uuid))
            return null;

        Map<Class<? extends IPipelineData>, IPipelineTask<?>> map = pendingTasks.get(uuid);
        if (!map.containsKey(type))
            return null;
        IPipelineTask<?> task = map.get(type);
        return (IPipelineTask<T>) task;
    }

    private <T extends IPipelineData> void removePipelineTask(@NotNull Class<? extends T> type, @NotNull UUID uuid) {
        verifyInput(type, uuid);
        if (!pendingTasks.containsKey(uuid))
            return;
        pendingTasks.get(uuid).remove(type);
        if (pendingTasks.get(uuid).isEmpty())
            pendingTasks.remove(uuid);
    }

    private void verifyInput(@NotNull Class<? extends IPipelineData> type, @NotNull UUID uuid) {
        Objects.requireNonNull(type, "type can't be null!");
        Objects.requireNonNull(uuid, "uuid can't be null!");
    }

    @Override
    public void shutdown() {
        //TODO: Print Log Message
        pendingTasks.forEach((uuid, pipelineTasks) -> {
            pipelineTasks.forEach((aClass, pipelineTask) -> {
                try {
                    pipelineTask.getFutureObject().get(1, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();
                }
            });
        });
    }
}
