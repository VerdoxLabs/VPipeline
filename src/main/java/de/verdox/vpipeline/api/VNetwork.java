package de.verdox.vpipeline.api;

import de.verdox.vpipeline.impl.ConstructionServiceImpl;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public final class VNetwork {

    private static ConstructionService pipelineService;

    private VNetwork() {
    }

    public static ConstructionService getConstructionService() {
        return pipelineService;
    }

    static {
        setConstructionService(new ConstructionServiceImpl());
    }

    private static void setConstructionService(@NotNull ConstructionService pipelineService) {
        if (VNetwork.pipelineService != null) {
            {
                throw new UnsupportedOperationException("Cannot redefine singleton PipelineService");
            }
        }
        NetworkLogger.getLogger().setLevel(Level.ALL);
        VNetwork.pipelineService = pipelineService;
    }
}
