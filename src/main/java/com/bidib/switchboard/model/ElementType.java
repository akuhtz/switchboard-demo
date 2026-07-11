package com.bidib.switchboard.model;

public enum ElementType {

    TURNOUT("T"),
    SIGNAL("S"),
    PLAIN("P");

    private final String prefix;

    ElementType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public static ElementType fromPrefix(String prefix) {
        for (ElementType et : values()) {
            if (et.prefix.equals(prefix)) {
                return et;
            }
        }
        throw new IllegalArgumentException("Unknown element type prefix: " + prefix);
    }
}
