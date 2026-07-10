package com.bidib.switchboard.model;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an aspect of a railway signal.
 * Each aspect has a unique integer ordinal (starting from 0) and can be
 * looked up by {@link #valueOf(String) name} or {@link #of(int) ordinal}.
 */
public enum SignalAspect {

    RED(0),
    GREEN(1);

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
