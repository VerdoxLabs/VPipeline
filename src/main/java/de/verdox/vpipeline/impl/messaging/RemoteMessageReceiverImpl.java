package de.verdox.vpipeline.impl.messaging;

import de.verdox.vpipeline.api.messaging.RemoteMessageReceiver;

import java.util.Objects;
import java.util.UUID;

public class RemoteMessageReceiverImpl implements RemoteMessageReceiver {
    private final UUID uuid;
    private final String identifier;
    private long lastKeepAlive = System.currentTimeMillis();

    public RemoteMessageReceiverImpl(UUID uuid, String identifier) {
        this.uuid = uuid;
        this.identifier = identifier;
    }

    void updateKeepAlive() {
        lastKeepAlive = System.currentTimeMillis();
    }

    @Override
    public long getLastKeepAlive() {
        return lastKeepAlive;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteMessageReceiverImpl that)) return false;
        return Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid());
    }

    @Override
    public String toString() {
        return "RemoteMessageReceiver{" + "uuid=" + uuid +
                ", identifier='" + identifier + '\'' +
                ", lastKeepAlive=" + lastKeepAlive +
                '}';
    }
}
