package de.verdox.vpipeline.api;

public interface ConstructionService {

    /**
     * Used to create a network Participant
     *
     * @return NetworkParticipantBuilder
     */
    NetworkParticipantBuilder createNetworkParticipant();
}
