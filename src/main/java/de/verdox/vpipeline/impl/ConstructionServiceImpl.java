package de.verdox.vpipeline.impl;

import de.verdox.vpipeline.api.ConstructionService;
import de.verdox.vpipeline.api.NetworkParticipantBuilder;
import de.verdox.vpipeline.impl.network.NetworkParticipantBuilderImpl;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 19:24
 */
public class ConstructionServiceImpl implements ConstructionService {
    @Override
    public NetworkParticipantBuilder createNetworkParticipant() {
        return new NetworkParticipantBuilderImpl();
    }
}