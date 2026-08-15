package net.onthepixel.anticheat;

import java.util.Collection;

/**
 * A value recorded at a point in time, used by the checks to keep short rolling
 * histories of player state.
 *
 * @param timeMillis when the value was recorded, in {@link System#currentTimeMillis()} terms
 * @param value the recorded value
 * @param <T> the type of the recorded value
 */
public record Sample<T>(long timeMillis, T value) {

    /**
     * Records the given value with the current timestamp.
     */
    public static <T> Sample<T> now(T value) {
        return new Sample<>(System.currentTimeMillis(), value);
    }

    /**
     * Drops every sample older than {@code maxAgeMillis} from the given history.
     *
     * @param samples the history to prune, modified in place
     * @param maxAgeMillis the maximum age a sample may have to be kept
     */
    public static void pruneOlderThan(Collection<? extends Sample<?>> samples, long maxAgeMillis) {
        long cutoff = System.currentTimeMillis() - maxAgeMillis;
        samples.removeIf(sample -> sample.timeMillis() < cutoff);
    }
}
