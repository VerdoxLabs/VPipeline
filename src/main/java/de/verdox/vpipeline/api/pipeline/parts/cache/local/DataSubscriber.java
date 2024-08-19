package de.verdox.vpipeline.api.pipeline.parts.cache.local;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Used to subscribe on {@link de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData} to have read-only access to data.
 *
 * @param <D> The Pipeline Data type
 * @param <V> The value type that is subscribed to
 */
public abstract class DataSubscriber<D extends IPipelineData, V> {
    private final Function<D, V> getter;
    private V currentValue;

    DataSubscriber(Function<D, V> getter) {
        this.getter = getter;
    }

    final void update(D afterUpdate) {
        this.currentValue = getter.apply(afterUpdate);
        onUpdate(this.currentValue);
    }

    void onUpdate(V newValue) {
    }

    DataSubscriber<D, V> setCurrentValue(V currentValue) {
        this.currentValue = currentValue;
        return this;
    }

    public V getCurrentValue() {
        return currentValue;
    }

    private static <D extends IPipelineData, V> DataSubscriber<D, V> createSubscriber(Function<D, V> getter, Consumer<V> onUpdate) {
        return new DataSubscriber<>(getter) {
            @Override
            void onUpdate(V newValue) {
                onUpdate.accept(newValue);
            }
        };
    }

    public static <D extends IPipelineData, V extends Number> DataSubscriber<D, V> observeNumber(Function<D, V> getter, Consumer<V> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData, V extends String> DataSubscriber<D, V> observeString(Function<D, V> getter, Consumer<V> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData, V extends Character> DataSubscriber<D, V> observeCharacter(Function<D, V> getter, Consumer<V> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData, V extends Boolean> DataSubscriber<D, V> observeBoolean(Function<D, V> getter, Consumer<V> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData> DataSubscriber<D, boolean[]> observeBooleanArray(Function<D, boolean[]> getter, Consumer<boolean[]> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData> DataSubscriber<D, byte[]> observeByteArray(Function<D, byte[]> getter, Consumer<byte[]> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData> DataSubscriber<D, short[]> observeShortArray(Function<D, short[]> getter, Consumer<short[]> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData> DataSubscriber<D, int[]> observeIntArray(Function<D, int[]> getter, Consumer<int[]> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData> DataSubscriber<D, long[]> observeLongArray(Function<D, long[]> getter, Consumer<long[]> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData> DataSubscriber<D, float[]> observeFloatArray(Function<D, float[]> getter, Consumer<float[]> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData> DataSubscriber<D, double[]> observeDoubleArray(Function<D, double[]> getter, Consumer<double[]> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData> DataSubscriber<D, char[]> observeCharArray(Function<D, char[]> getter, Consumer<char[]> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }

    public static <D extends IPipelineData> DataSubscriber<D, String[]> observeStringArray(Function<D, String[]> getter, Consumer<String[]> onUpdate) {
        return createSubscriber(getter, onUpdate);
    }
}
