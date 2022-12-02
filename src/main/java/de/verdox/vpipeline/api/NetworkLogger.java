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
}
