package de.verdox.vpipeline.impl.util;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Runnable that prints exceptions thrown
 */
public record CatchingRunnable(Runnable delegate) implements Runnable {
    /**
     * @param delegate Runnable to run
     */
    public CatchingRunnable(@NotNull Runnable delegate) {
        Objects.requireNonNull(delegate, "delegate can't be null!");
        this.delegate = delegate;
    }

    @Override
    public void run() {
        try {
            delegate.run();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }
}