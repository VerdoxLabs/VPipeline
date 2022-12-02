package de.verdox.vpipeline.api.messaging.instruction;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:04
 */
public interface Responder  {
    /**
     * Called whenever an instruction needs a response
     * @param instructionData The Data that was received
     * @return The Data that is sent as response
     */
    Object[] answerQuery(Object[] instructionData);

    /**
     * Called when a response is received
     * @param instructionData The original instruction data.
     * @param responseData The response to the original instruction data
     */
    void onQueryAnswerReceive(Object[] instructionData, Object[] responseData);

    /**
     * @return Whether if the instruction should respond to itself or not.
     */
    default boolean respondToItself(){
        return false;
    }
}
