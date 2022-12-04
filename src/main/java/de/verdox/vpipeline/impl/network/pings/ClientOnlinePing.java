package de.verdox.vpipeline.impl.network.pings;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.instruction.types.Ping;
import de.verdox.vpipeline.impl.network.NetworkClient;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 03.07.2022 23:09
 */
public class ClientOnlinePing extends Ping {
    public ClientOnlinePing(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public List<Class<?>> instructionDataTypes() {
        return List.of(UUID.class);
    }

    @Override
    public List<String> instructionPath() {
        return List.of("clientOnlinePing");
    }

    @Override
    public void onPingReceive(Object[] instructionData) {
        UUID clientUUID = (UUID) instructionData[0];
        getCurrentClient().pipeline().load(NetworkClient.class, clientUUID).whenComplete((networkClient, throwable) -> {
            if (networkClient != null)
                NetworkLogger.info("Client Online: " + instructionData[0]);
        });

    }
}
