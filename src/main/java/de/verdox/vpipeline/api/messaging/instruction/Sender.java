package de.verdox.vpipeline.api.messaging.instruction;

import de.verdox.vpipeline.api.messaging.instruction.types.Query;
import de.verdox.vpipeline.api.messaging.instruction.types.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:04
 */
public interface Sender<T> {
    /**
     * Called before the instruction is sent.
     *
     * @param instructionData The data that will be sent.
     * @return Whether the instruction should be sent or not.
     */
    boolean onSend(TransmittedData instructionData);
    void onReceive(TransmittedData transmittedData);

    Response<T> getResponse();

    class FutureResponse<T> extends CompletableFuture<T> {

        public T getOrDefault(long timeOut, TimeUnit timeUnit, T defaultValue) {
            try {
                return get(timeOut, timeUnit);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                return defaultValue;
            }
        }
    }
}
