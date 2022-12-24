package de.verdox.vpipeline.api;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 19:21
 */
public interface ConstructionService {

    /**
     * Used to create a network Participant
     *
     * @return NetworkParticipantBuilder
     */
    NetworkParticipantBuilder createNetworkParticipant();
}
