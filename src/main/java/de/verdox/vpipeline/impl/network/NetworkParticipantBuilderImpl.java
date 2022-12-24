package de.verdox.vpipeline.impl.network;

import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.NetworkParticipantBuilder;
import de.verdox.vpipeline.api.messaging.builder.MessagingServiceBuilder;
import de.verdox.vpipeline.api.network.RemoteParticipant;
import de.verdox.vpipeline.api.pipeline.builder.PipelineBuilder;
import de.verdox.vpipeline.impl.messaging.MessagingServiceImpl;
import de.verdox.vpipeline.impl.messaging.builder.MessagingServiceBuilderImpl;
import de.verdox.vpipeline.impl.pipeline.builder.PipelineBuilderImpl;
import de.verdox.vpipeline.impl.pipeline.core.PipelineImpl;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class NetworkParticipantBuilderImpl implements NetworkParticipantBuilder {
    private PipelineBuilder pipelineBuilder;
    private MessagingServiceBuilder messagingServiceBuilder;
    private ScheduledExecutorService service;
    private String name;

    @Override
    public NetworkParticipantBuilder withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public NetworkParticipantBuilder withPipeline(Consumer<PipelineBuilder> pipelineBuilder) {
        this.pipelineBuilder = new PipelineBuilderImpl();
        pipelineBuilder.accept(this.pipelineBuilder);
        return this;
    }

    @Override
    public NetworkParticipantBuilder withMessagingService(Consumer<MessagingServiceBuilder> pipelineBuilder) {
        this.messagingServiceBuilder = new MessagingServiceBuilderImpl();
        pipelineBuilder.accept(this.messagingServiceBuilder);
        return this;
    }

    @Override
    public NetworkParticipantBuilder withExecutorService(ScheduledExecutorService service) {
        this.service = service;
        return this;
    }

    public NetworkParticipant build() {
        if (this.name == null)
            throw new IllegalArgumentException("A network participant needs a name.");
        if (this.service == null)
            throw new IllegalArgumentException("A network participant needs a ScheduledExecutorService.");

        if (this.messagingServiceBuilder != null)
            this.messagingServiceBuilder.withIdentifier(name);
        if (this.pipelineBuilder != null)
            this.pipelineBuilder.withExecutorService(service);

        var pipeline = this.pipelineBuilder != null ? this.pipelineBuilder.buildPipeline() : null;
        var messagingService = this.messagingServiceBuilder != null ? this.messagingServiceBuilder.buildMessagingService() : null;

        var participant = new NetworkParticipantImpl(RemoteParticipant.getParticipantUUID(name), name, pipeline, messagingService, service);
        if (pipeline instanceof PipelineImpl pipelineImpl)
            pipelineImpl.setNetworkParticipant(participant);
        if (messagingService instanceof MessagingServiceImpl messagingServiceImpl)
            messagingServiceImpl.setNetworkParticipant(participant);

        participant.enable();

        return participant;
    }
}
