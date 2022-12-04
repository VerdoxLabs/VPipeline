package de.verdox.vpipeline.impl.util;

import de.verdox.vpipeline.api.NetworkLogger;

public class CallbackUtil {
    public static void runIfNotNull(Runnable runnable) {
        NetworkLogger.getLogger().fine("[" + Thread.currentThread().getName() + "] Executing Callback from "+Thread.currentThread().getStackTrace()[2].getMethodName());
        if (runnable != null)
            runnable.run();
    }
}
