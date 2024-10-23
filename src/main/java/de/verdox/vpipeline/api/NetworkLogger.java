package de.verdox.vpipeline.api;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public interface NetworkLogger {

    LoggerStats loggerStats = new LoggerStats();
    DebugMode debugMode = new DebugMode(false);

    DebugMode transmitterDebugMode = new DebugMode(false);
    DebugMode messagingServiceDebugMode = new DebugMode(false);

    static Logger getLogger() {
        var callerName = Thread.currentThread().getStackTrace()[2].getClass().getSimpleName();
        var logger = Logger.getLogger("VPipeline - " + callerName);
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");

        logger.setLevel(loggerStats.level);
        return logger;
    }

    static void setLevel(Level newLevel) {
        loggerStats.level = newLevel;
        LogManager.getLogManager().getLogger("").setLevel(loggerStats.level);
    }

    static void info(String message) {
        var caller = Thread.currentThread().getStackTrace()[3];
        getLogger().info("[" + Thread
                .currentThread()
                .getName() + " | " + extractClassName(caller.getClassName()) + ":" + caller.getMethodName() + "[" + caller.getLineNumber() + "]" + "]\n" + message);
    }

    static void debug(String message) {
        if (!debugMode.isDebugMode())
            return;
        var caller = Thread.currentThread().getStackTrace()[3];

        getLogger().info("[" + Thread
                .currentThread()
                .getName() + " | " + extractClassName(caller.getClassName()) + ":" + caller.getMethodName() + "[" + caller.getLineNumber() + "]" + "]\n" + message);
    }

    static void warning(String message) {
        var caller = Thread.currentThread().getStackTrace()[3];
        getLogger().warning("[" + Thread
                .currentThread()
                .getName() + " | " + extractClassName(caller.getClassName()) + ":" + caller.getMethodName() + "[" + caller.getLineNumber() + "]" + "]\n" + message);
    }

    private static String extractClassName(String longName) {
        var split = longName.split("\\.");
        return split[split.length - 1];
    }

    class LoggerStats {
        private Level level = Level.ALL;
    }

    class DebugMode {
        private boolean debugMode;

        public DebugMode(boolean initialValue) {
            this.debugMode = initialValue;
        }

        public boolean isDebugMode() {
            return debugMode;
        }

        public void setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
        }
    }
}
