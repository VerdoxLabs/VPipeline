package de.verdox.vpipeline.impl;

import de.verdox.vpipeline.api.ConstructionService;
import de.verdox.vpipeline.api.NetworkParticipantBuilder;
import de.verdox.vpipeline.impl.network.NetworkParticipantBuilderImpl;

public class ConstructionServiceImpl implements ConstructionService {
    @Override
    public NetworkParticipantBuilder createNetworkParticipant() {
        return new NetworkParticipantBuilderImpl();
    }
}