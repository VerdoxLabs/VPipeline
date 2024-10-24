package de.verdox.vpipeline.impl.network;

import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.NetworkParticipantBuilder;
import de.verdox.vpipeline.api.messaging.Transmitter;
import de.verdox.vpipeline.api.messaging.builder.MessagingServiceBuilder;
import de.verdox.vpipeline.api.network.RemoteParticipant;
import de.verdox.vpipeline.api.pipeline.builder.PipelineBuilder;
import de.verdox.vpipeline.api.pipeline.parts.NetworkDataLockingService;
import de.verdox.vpipeline.impl.messaging.MessagingServiceImpl;
import de.verdox.vpipeline.impl.messaging.builder.MessagingServiceBuilderImpl;
import de.verdox.vpipeline.impl.pipeline.builder.PipelineBuilderImpl;
import de.verdox.vpipeline.impl.pipeline.core.PipelineImpl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class NetworkParticipantBuilderImpl implements NetworkParticipantBuilder {
    private final PipelineBuilder pipelineBuilder = new PipelineBuilderImpl().withNetworkDataLockingService(NetworkDataLockingService.createDummy());
    private final MessagingServiceBuilder messagingServiceBuilder =
            new MessagingServiceBuilderImpl()
                    .withTransmitter(Transmitter.createDummyTransmitter());
    private ScheduledExecutorService service;
    private String name;

    @Override
    public NetworkParticipantBuilder withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public NetworkParticipantBuilder withPipeline(Consumer<PipelineBuilder> pipelineBuilder) {
        pipelineBuilder.accept(this.pipelineBuilder);
        return this;
    }

    @Override
    public NetworkParticipantBuilder withMessagingService(Consumer<MessagingServiceBuilder> pipelineBuilder) {
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
/*        if (this.service == null)
            throw new IllegalArgumentException("A network participant needs a ScheduledExecutorService.");*/

        this.messagingServiceBuilder.withIdentifier(name);

        var pipeline = ((PipelineBuilderImpl) this.pipelineBuilder).buildPipeline();
        var messagingService = this.messagingServiceBuilder.buildMessagingService();

        return new NetworkParticipantImpl(RemoteParticipant.getParticipantUUID(name), name, pipeline, messagingService, service);
    }
}
