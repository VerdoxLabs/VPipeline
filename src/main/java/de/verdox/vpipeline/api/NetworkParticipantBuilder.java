package de.verdox.vpipeline.api;

import de.verdox.vpipeline.api.messaging.builder.MessagingServiceBuilder;
import de.verdox.vpipeline.api.pipeline.builder.PipelineBuilder;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public interface NetworkParticipantBuilder {

    NetworkParticipantBuilder withName(String name);
    default NetworkParticipantBuilder withPipeline(){
        return withPipeline(pipelineBuilder -> {});
    }
    NetworkParticipantBuilder withPipeline(Consumer<PipelineBuilder> pipelineBuilder);
    NetworkParticipantBuilder withMessagingService(Consumer<MessagingServiceBuilder> pipelineBuilder);

    NetworkParticipantBuilder withExecutorService(ScheduledExecutorService service);
    NetworkParticipant build();
}
