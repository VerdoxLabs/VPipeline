package de.verdox.vpipeline.impl.network.pings;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.instruction.types.Ping;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 03.07.2022 23:18
 */
public class ClientOfflinePing extends Ping {
    public ClientOfflinePing(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public List<Class<?>> instructionDataTypes() {
        return List.of(UUID.class);
    }

    @Override
    public List<String> instructionPath() {
        return List.of("clientOfflinePing");
    }

    @Override
    public void onPingReceive(Object[] instructionData) {
        NetworkLogger.getLogger().info("Client Online: " + instructionData[0]);
    }
}
