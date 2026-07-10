package com.bidib.switchboard.model;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an aspect of a railway signal.
 * Supports up to 8 aspects (ordinals 0–7). A specific signal type uses a subset
 * of these (e.g. a 2-aspect signal uses ASPECT_0 and ASPECT_1, a 3-aspect signal
 * uses ASPECT_0 through ASPECT_2).
 * <p>
 * Each aspect has a unique integer ordinal starting from 0 and can be looked up
 * by {@link #valueOf(String) name} or {@link #of(int) ordinal}.
 */
public enum SignalAspect {

    ASPECT_0(0),
    ASPECT_1(1),
    ASPECT_2(2),
    ASPECT_3(3),
    ASPECT_4(4),
    ASPECT_5(5),
    ASPECT_6(6),
    ASPECT_7(7);

    private static final Map<Integer, SignalAspect> BY_ORDINAL =
            Stream.of(values()).collect(Collectors.toUnmodifiableMap(SignalAspect::getAspect, Function.identity()));

    private final int aspect;

    SignalAspect(int aspect) {
        this.aspect = aspect;
    }

    /**
     * Returns the integer ordinal of this aspect.
     */
    public int getAspect() {
        return aspect;
    }

    /**
     * Returns the aspect for the given integer ordinal.
     *
     * @throws IllegalArgumentException if no aspect matches the ordinal
     */
    public static SignalAspect of(int aspect) {
        SignalAspect result = BY_ORDINAL.get(aspect);
        if (result == null) {
            throw new IllegalArgumentException("Unknown signal aspect ordinal: " + aspect);
        }
        return result;
    }
}
