package de.verdox.vpipeline.api.pipeline.parts.cache.local;

import com.google.common.collect.ImmutableList;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
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
    private Class<? extends D> dataClass;
    private UUID objectUUID;

    DataSubscriber(Function<D, V> getter, V defaultValue) {
        this.getter = getter;
        this.currentValue = defaultValue;
    }

    final void update(IPipelineData afterUpdate) {
        this.currentValue = getter.apply((D) afterUpdate);
        onUpdate(this.currentValue);
    }

    protected void onUpdate(V newValue) {
    }

    DataSubscriber<D, V> setCurrentValue(V currentValue) {
        this.currentValue = currentValue;
        return this;
    }

    void linkToLocalCache(@NotNull Class<? extends D> dataClass, @NotNull UUID objectUUID){
        if(this.dataClass != null || this.objectUUID != null)
            throw new IllegalArgumentException("DataSubscriber already linked to an object");
        this.dataClass = dataClass;
        this.objectUUID = objectUUID;
    }

    void unlinkFromLocalCache(){
        this.dataClass = null;
        this.objectUUID = null;
    }

    @Nullable
    public Class<? extends D> getDataClass() {
        return dataClass;
    }

    @Nullable
    public UUID getObjectUUID() {
        return objectUUID;
    }

    public V getCurrentValue() {
        return currentValue;
    }

    private static <D extends IPipelineData, V> DataSubscriber<D, V> createSubscriber(Function<D, V> getter, Consumer<V> onUpdate, V defaultValue) {
        return new DataSubscriber<>(getter, defaultValue) {
            @Override
            protected void onUpdate(V newValue) {
                onUpdate.accept(newValue);
            }
        };
    }

    public static <D extends IPipelineData, V extends Number> DataSubscriber<D, V> observeNumber(Function<D, V> getter, Consumer<V> onUpdate, V defaultValue) {
        return createSubscriber(getter, onUpdate, defaultValue);
    }

    public static <D extends IPipelineData, V extends String> DataSubscriber<D, V> observeString(Function<D, V> getter, Consumer<V> onUpdate, V defaultValue) {
        return createSubscriber(getter, onUpdate, defaultValue);
    }

    public static <D extends IPipelineData, V extends Character> DataSubscriber<D, V> observeCharacter(Function<D, V> getter, Consumer<V> onUpdate, V defaultValue) {
        return createSubscriber(getter, onUpdate, defaultValue);
    }

    public static <D extends IPipelineData, V extends Boolean> DataSubscriber<D, V> observeBoolean(Function<D, V> getter, Consumer<V> onUpdate, V defaultValue) {
        return createSubscriber(getter, onUpdate, defaultValue);
    }

    public static <D extends IPipelineData> DataSubscriber<D, boolean[]> observeBooleanArray(Function<D, boolean[]> getter, Consumer<boolean[]> onUpdate) {
        return createSubscriber(getter, onUpdate, new boolean[0]);
    }

    public static <D extends IPipelineData> DataSubscriber<D, byte[]> observeByteArray(Function<D, byte[]> getter, Consumer<byte[]> onUpdate) {
        return createSubscriber(getter, onUpdate, new byte[0]);
    }

    public static <D extends IPipelineData> DataSubscriber<D, short[]> observeShortArray(Function<D, short[]> getter, Consumer<short[]> onUpdate) {
        return createSubscriber(getter, onUpdate, new short[0]);
    }

    public static <D extends IPipelineData> DataSubscriber<D, int[]> observeIntArray(Function<D, int[]> getter, Consumer<int[]> onUpdate) {
        return createSubscriber(getter, onUpdate, new int[0]);
    }

    public static <D extends IPipelineData> DataSubscriber<D, long[]> observeLongArray(Function<D, long[]> getter, Consumer<long[]> onUpdate) {
        return createSubscriber(getter, onUpdate, new long[0]);
    }

    public static <D extends IPipelineData> DataSubscriber<D, float[]> observeFloatArray(Function<D, float[]> getter, Consumer<float[]> onUpdate) {
        return createSubscriber(getter, onUpdate, new float[0]);
    }

    public static <D extends IPipelineData> DataSubscriber<D, double[]> observeDoubleArray(Function<D, double[]> getter, Consumer<double[]> onUpdate) {
        return createSubscriber(getter, onUpdate, new double[0]);
    }

    public static <D extends IPipelineData> DataSubscriber<D, char[]> observeCharArray(Function<D, char[]> getter, Consumer<char[]> onUpdate) {
        return createSubscriber(getter, onUpdate, new char[0]);
    }

    public static <D extends IPipelineData> DataSubscriber<D, String[]> observeStringArray(Function<D, String[]> getter, Consumer<String[]> onUpdate) {
        return createSubscriber(getter, onUpdate, new String[0]);
    }

    public static <D extends IPipelineData> DataSubscriber<D, UUID> observeUUID(Function<D, UUID> getter, Consumer<UUID> onUpdate, UUID defaultValue) {
        return createSubscriber(getter, onUpdate, defaultValue);
    }

    public static <D extends IPipelineData> DataSubscriber<D, UUID[]> observeUUIDs(Function<D, UUID[]> getter, Consumer<UUID[]> onUpdate) {
        return createSubscriber(getter, onUpdate, new UUID[0]);
    }

    public static <D extends IPipelineData> DataSubscriber<D, ImmutableList<? extends Number>> observeNumbers(Function<D, ImmutableList<? extends Number>> getter, Consumer<ImmutableList<? extends Number>> onUpdate) {
        return createSubscriber(getter, onUpdate, ImmutableList.of());
    }

    public static <D extends IPipelineData> DataSubscriber<D, ImmutableList<? extends String>> observeStrings(Function<D, ImmutableList<? extends String>> getter, Consumer<ImmutableList<? extends String>> onUpdate) {
        return createSubscriber(getter, onUpdate, ImmutableList.of());
    }

    public static <D extends IPipelineData> DataSubscriber<D, ImmutableList<? extends Character>> observeChars(Function<D, ImmutableList<? extends Character>> getter, Consumer<ImmutableList<? extends Character>> onUpdate) {
        return createSubscriber(getter, onUpdate, ImmutableList.of());
    }

    public static <D extends IPipelineData> DataSubscriber<D, ImmutableList<? extends Boolean>> observeBooleans(Function<D, ImmutableList<? extends Boolean>> getter, Consumer<ImmutableList<? extends Boolean>> onUpdate) {
        return createSubscriber(getter, onUpdate, ImmutableList.of());
    }

    public static <D extends IPipelineData, R extends Record> DataSubscriber<D, R> observeRecord(Function<D, R> getter, Consumer<R> onUpdate, R defaultValue) {
        return createSubscriber(getter, onUpdate, defaultValue);
    }

    public static <D extends IPipelineData> DataSubscriber<D, ImmutableList<? extends Record>> observeRecords(Function<D, ImmutableList<? extends Record>> getter, Consumer<ImmutableList<? extends Record>> onUpdate) {
        return createSubscriber(getter, onUpdate, ImmutableList.of());
    }
}
