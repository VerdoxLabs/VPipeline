package de.verdox.vpipeline.impl.pipeline.core;

import de.verdox.vpipeline.api.pipeline.core.PipelineTask;
import de.verdox.vpipeline.api.pipeline.core.PipelineTaskScheduler;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

public class PipelineTaskSchedulerImpl implements PipelineTaskScheduler {

    private final PipelineImpl pipeline;
    private final Map<UUID, Map<Class<? extends IPipelineData>, PipelineTask<?>>> pendingTasks = new ConcurrentHashMap<>();

    public PipelineTaskSchedulerImpl(@NotNull PipelineImpl pipeline) {
        Objects.requireNonNull(pipeline, "pipelineManager can't be null!");
        this.pipeline = pipeline;
    }

    @Override
    public synchronized <T extends IPipelineData> PipelineTask<T> schedulePipelineTask(@NotNull Class<? extends T> type, @NotNull UUID uuid, @NotNull TaskType taskType) {
        verifyInput(type, uuid);

        PipelineTask<T> existingTask = getExistingPipelineTask(type, uuid);

        if (existingTask != null) {
            if (existingTask.getTaskType().equals(taskType))
                return existingTask;

            try {
                existingTask.getFutureObject().wait();
                return schedulePipelineTask(type, uuid, taskType);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        future.whenComplete((t, throwable) -> removePipelineTask(type, uuid));
        PipelineTaskImpl<T> task = new PipelineTaskImpl<>(type, uuid, taskType, future);

        pendingTasks.computeIfAbsent(uuid, uuid1 -> new ConcurrentHashMap<>()).put(type, task);
        return task;
    }

    private <T extends IPipelineData> PipelineTask<T> getExistingPipelineTask(@NotNull Class<? extends T> type, @NotNull UUID uuid) {
        verifyInput(type, uuid);

        if (!pendingTasks.containsKey(uuid))
            return null;

        Map<Class<? extends IPipelineData>, PipelineTask<?>> map = pendingTasks.get(uuid);
        if (!map.containsKey(type))
            return null;
        PipelineTask<?> task = map.get(type);
        return (PipelineTask<T>) task;
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
