package de.verdox.vpipeline.api.modules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public class AttachedPipeline {
    private final Function<GsonBuilder, Gson> gsonBuilder;
    private Pipeline pipeline;
    private Gson gson;

    public AttachedPipeline(Function<GsonBuilder, Gson> gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
    }

    public void attachPipeline(@NotNull Pipeline pipeline) {
        Objects.requireNonNull(pipeline);
        if (this.pipeline != null)
            throw new IllegalStateException("A new pipeline can't be attached");
        this.pipeline = pipeline;
        var gson = this.pipeline.getGsonBuilder().create();
        var buildCopy = gson.newBuilder();
        this.gson = gsonBuilder.apply(buildCopy);
    }

    public Gson getGson() {
        Objects.requireNonNull(this.gson, "No gson specified for attached pipeline because no pipeline was attached");
        return gson;
    }

    @NotNull
    public Pipeline getAttachedPipeline() {
        Objects.requireNonNull(this.pipeline, "Not attached to a pipeline");
        return pipeline;
    }
}
