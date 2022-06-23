package de.verdox.vpipeline.api.messaging.instruction;

import java.util.concurrent.CompletableFuture;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:04
 */
public interface ISender<T> {
    /**
     * Called before the instruction is sent.
     * @param instructionData The data that will be sent.
     * @return Whether the instruction should be sent or not.
     */
    boolean onSend(Object[] instructionData);

    CompletableFuture<T> getFuture();
}
