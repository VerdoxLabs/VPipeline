package de.verdox.vpipeline.impl.network;

import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.AccessInvalidException;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataAccess;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.LockableAction;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.network.RemoteParticipant;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.impl.messaging.MessagingServiceImpl;
import de.verdox.vpipeline.impl.pipeline.core.PipelineImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public record NetworkParticipantImpl(UUID uuid, String identifier, @Nullable Pipeline pipeline,
                                     @Nullable MessagingService messagingService,
                                     ScheduledExecutorService service) implements de.verdox.vpipeline.api.NetworkParticipant {

    //TODO: Für LoadBefore Daten Pings über das Netzwerk senden, um z.B. Daten nach creation zu laden.


    @Override
    public void connect() {
        if (pipeline instanceof PipelineImpl pipelineImpl)
            pipelineImpl.setNetworkParticipant(this);
        if (messagingService instanceof MessagingServiceImpl messagingServiceImpl)
            messagingServiceImpl.setNetworkParticipant(this);

        NetworkParticipant.super.connect();
        enable();
    }

    void enable() {
        //Check network if there is already a participant online.
        if (pipeline != null) {
            this.pipeline.getDataRegistry().registerType(RemoteParticipantImpl.class);

            if (this.pipeline.exist(RemoteParticipantImpl.class, uuid)) {
                NetworkLogger.warning("There is already a pipeline running with this uuid");
                shutdown();
                return;
            }
        }

        if (this.service != null)
            this.service.scheduleAtFixedRate(() -> {

                if (this.pipeline != null) {
                    try(LockableAction.Write<RemoteParticipantImpl> access = this.pipeline.loadOrCreate(RemoteParticipantImpl.class, uuid).write()){
                        access.get().setIdentifier(identifier);
                        access.commitChanges(true);
                    }
                    catch (AccessInvalidException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (this.messagingService != null) {
                    this.messagingService.sendKeepAlivePing();
                }


            }, 0L, 5, TimeUnit.SECONDS);
        NetworkLogger.info("Network participant up and running");
    }

    @Override
    public Set<DataAccess<? extends RemoteParticipant>> getOnlineNetworkClients() {
        if (this.pipeline == null)
            throw new IllegalStateException("No pipeline was instantiated");
        return pipeline.loadAllData(RemoteParticipantImpl.class);
    }

    @Override
    public DataAccess<RemoteParticipant> getOnlineNetworkClient(@NotNull String identifier) {
        Objects.requireNonNull(pipeline);
        Objects.requireNonNull(identifier);
        return pipeline.load(RemoteParticipantImpl.class, RemoteParticipant.getParticipantUUID(identifier));
    }

    @Override
    public DataAccess<RemoteParticipant> getAsNetworkClient() {
        Objects.requireNonNull(pipeline);
        return pipeline.load(RemoteParticipantImpl.class, uuid);
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
            this.pipeline.delete(RemoteParticipantImpl.class, uuid);
            pipeline.shutdown();
        }
        if (this.messagingService != null)
            messagingService.shutdown();
    }
}
