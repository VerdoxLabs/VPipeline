package de.verdox.vpipeline.api.pipeline.parts;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:13
 */
public interface IRemoteStorage {
    void connect();
    void disconnect();
}
