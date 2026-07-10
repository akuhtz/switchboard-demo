package com.bidib.switchboard.model;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an aspect of a railway turnout (points).
 * <ul>
 *   <li>2-way turnout uses: STRAIGHT (0), DIVERTED (1)</li>
 *   <li>3-way turnout uses: STRAIGHT (0), DIVERTED_LEFT (1), DIVERTED_RIGHT (2)</li>
 * </ul>
 * Each aspect has a unique integer ordinal starting from 0 and can be looked up
 * by {@link #valueOf(String) name} or {@link #of(int) ordinal}.
 */
public enum TurnoutAspect {

    STRAIGHT(0),
    DIVERTED_LEFT(1),
    DIVERTED_RIGHT(2);

    private static final Map<Integer, TurnoutAspect> BY_ORDINAL =
            Stream.of(values()).collect(Collectors.toUnmodifiableMap(TurnoutAspect::getAspect, Function.identity()));

    private final int aspect;

    TurnoutAspect(int aspect) {
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
    public static TurnoutAspect of(int aspect) {
        TurnoutAspect result = BY_ORDINAL.get(aspect);
        if (result == null) {
            throw new IllegalArgumentException("Unknown turnout aspect ordinal: " + aspect);
        }
        return result;
    }
}
