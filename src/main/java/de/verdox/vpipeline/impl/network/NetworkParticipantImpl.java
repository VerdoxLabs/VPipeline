package de.verdox.vpipeline.impl.network;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.network.RemoteParticipant;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.customtypes.DataReference;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public record NetworkParticipantImpl(UUID uuid, String identifier, @Nullable Pipeline pipeline,
                                     @Nullable MessagingService messagingService,
                                     ScheduledExecutorService service) implements de.verdox.vpipeline.api.NetworkParticipant {

    //TODO: Für LoadBefore Daten Pings über das Netzwerk senden, um z.B. Daten nach creation zu laden.

    void enable() {
        //Check network if there is already a participant online.
        if (pipeline != null) {
            this.pipeline.getDataRegistry().registerType(RemoteParticipantImpl.class);

            if (this.pipeline.exist(RemoteParticipantImpl.class, uuid).join()) {
                NetworkLogger.warning("There is already a pipeline running with this uuid");
                shutdown();
                return;
            }
        }

        this.service.scheduleAtFixedRate(() -> {

            if (this.pipeline != null) {
                this.pipeline.loadOrCreate(RemoteParticipantImpl.class, uuid).thenApply(pipelineLock -> {
                    pipelineLock.performWriteOperation(remoteParticipant -> remoteParticipant.setIdentifier(identifier));
                    return pipelineLock;
                });
            }

            if (this.messagingService != null) {
                this.messagingService.sendKeepAlivePing();
            }


        }, 0L, 5, TimeUnit.SECONDS);
        NetworkLogger.info("Network participant up and running");
    }

    @Override
    public CompletableFuture<Set<DataReference<RemoteParticipant>>> getOnlineNetworkClients() {
        if (this.pipeline == null)
            return CompletableFuture.completedFuture(new HashSet<>());
        return pipeline.loadAllData(RemoteParticipantImpl.class);
    }

    @Override
    public DataReference<RemoteParticipant> getOnlineNetworkClient(String identifier) {
        Objects.requireNonNull(pipeline);
        return pipeline.createDataReference(RemoteParticipantImpl.class, RemoteParticipant.getParticipantUUID(identifier));
    }

    @Override
    public DataReference<RemoteParticipant> getAsNetworkClient() {
        Objects.requireNonNull(pipeline);
        return pipeline.createDataReference(RemoteParticipantImpl.class, uuid);
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void shutdown() {
        if (this.pipeline != null) {
            this.pipeline.delete(RemoteParticipantImpl.class, uuid).join();
            pipeline.shutdown();
        }
        if (this.messagingService != null)
            messagingService.shutdown();
    }
}
