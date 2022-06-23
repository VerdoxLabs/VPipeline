package de.verdox.vpipeline.api;

import de.verdox.vpipeline.api.pipeline.IPipelineBuilder;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 19:04
 */
public final class VPipeline {

    private static IConstructionService pipelineService;

    private VPipeline() {
    }

    public static void setConstructionService(@NotNull IConstructionService pipelineService) {
        if (VPipeline.pipelineService != null) {
            throw new UnsupportedOperationException("Cannot redefine singleton PipelineService");
        }
        VPipeline.pipelineService = pipelineService;
    }

    public static IPipelineBuilder createPipeline() {
        return pipelineService.createPipelineConstructor();
    }

    static {
        setConstructionService(instantiateSingleton(IConstructionService.class));
    }

    private static <T> T instantiateSingleton(@NotNull Class<? extends T> type) {
        Reflections reflections = new Reflections("de.verdox.vpipeline.impl");
        Class<? extends T> pipelineServiceClass = reflections.getSubTypesOf(type).stream().findAny().orElse(null);
        try {
            if (pipelineServiceClass == null)
                throw new NullPointerException("Could not find service implementation class for " + type);
            return pipelineServiceClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }
}
