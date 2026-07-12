package com.bidib.switchboard.model;

public enum ElementType {

    TURNOUT_LEFT("TL", true),
    TURNOUT_RIGHT("TR", true),
    TURNOUT_3WAY("T3", true),
    SIGNAL_2("S2", true),
    SIGNAL_3("S3", true),
    STRAIGHT("P", true),
    CURVE_LEFT("CL", true),
    CURVE_RIGHT("CR", true);

    private final String prefix;
    private final boolean visible;

    ElementType(String prefix, boolean visible) {
        this.prefix = prefix;
        this.visible = visible;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isVisible() {
        return visible;
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
