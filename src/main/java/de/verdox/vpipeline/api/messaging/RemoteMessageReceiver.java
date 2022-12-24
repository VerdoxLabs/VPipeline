package de.verdox.vpipeline.api.messaging;

import java.util.UUID;

public interface RemoteMessageReceiver {
    long getLastKeepAlive();

    UUID getUuid();

    String getIdentifier();
}
