package de.verdox.vpipeline.api;

import java.util.logging.Logger;

public interface NetworkLogger {
    static Logger getLogger() {
        var callerName = Thread.currentThread().getStackTrace()[2].getClass().getSimpleName();
        var logger = Logger.getLogger("VPipeline - " + callerName);
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");

        return logger;
    }

    static void info(String message) {
        var caller = Thread.currentThread().getStackTrace()[3];

        getLogger().info("[" + Thread
                .currentThread()
                .getName() + " | " + extractClassName(caller.getClassName()) + ":" + caller.getMethodName()+"["+caller.getLineNumber()+"]" + "] " +  message);
    }

    private static String extractClassName(String longName){
        var split = longName.split("\\.");
        return split[split.length -1];
    }
}
