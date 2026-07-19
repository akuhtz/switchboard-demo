package org.bidib.switchboard.component.model;

public enum ElementType {

    TURNOUT_LEFT("TL", true),
    TURNOUT_RIGHT("TR", true),
    TURNOUT_3WAY("T3", true),
    SIGNAL_2("S2", true),
    SIGNAL_3("S3", true),
    STRAIGHT("P", true),
    CURVE_LEFT("CL", true),
    CURVE_RIGHT("CR", true),
    DIAGONAL("DG", true);

    public static final int PORT_LEFT = 0;
    public static final int PORT_TOP = 1;
    public static final int PORT_RIGHT = 2;
    public static final int PORT_BOTTOM = 3;

    public static final int DIAG_RB = 0;
    public static final int DIAG_LB = 1;
    public static final int DIAG_RT = 2;
    public static final int DIAG_LT = 3;

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

    public int[] getActivePorts(int aspect, int rotation) {
        int[] base = getBasePorts(aspect);
        return rotatePorts(base, rotation);
    }

    public int[] getPhysicalPorts(int rotation) {
        int[] base = switch (this) {
            case STRAIGHT -> new int[] { PORT_LEFT, PORT_RIGHT };
            case DIAGONAL -> new int[] { PORT_LEFT, PORT_TOP, PORT_RIGHT, PORT_BOTTOM };
            case CURVE_LEFT -> new int[] { PORT_LEFT, PORT_TOP };
            case CURVE_RIGHT -> new int[] { PORT_LEFT, PORT_BOTTOM };
            case TURNOUT_LEFT -> new int[] { PORT_LEFT, PORT_RIGHT, PORT_TOP };
            case TURNOUT_RIGHT -> new int[] { PORT_LEFT, PORT_RIGHT, PORT_BOTTOM };
            case TURNOUT_3WAY -> new int[] { PORT_LEFT, PORT_RIGHT, PORT_TOP, PORT_BOTTOM };
            case SIGNAL_2, SIGNAL_3 -> new int[] { PORT_LEFT, PORT_RIGHT };
        };
        return rotatePorts(base, rotation);
    }

    private int[] rotatePorts(int[] base, int rotation) {
        int rotSteps = ((rotation / 90) % 4 + 4) % 4;
        if (rotSteps == 0) {
            return base;
        }
        int[] result = new int[base.length];
        for (int i = 0; i < base.length; i++) {
            result[i] = (base[i] + rotSteps) % 4;
        }
        return result;
    }

    public int aspectForRoute(int entryPort, int exitPort, int rotation) {
        for (int a = 0; a < getAspectCount(); a++) {
            int[] ports = getActivePorts(a, rotation);
            boolean hasEntry = false, hasExit = false;
            for (int p : ports) {
                if (p == entryPort) hasEntry = true;
                if (p == exitPort) hasExit = true;
            }
            if (hasEntry && hasExit) return a;
        }
        return 0;
    }

    public int aspectForPort(int port, int rotation) {
        for (int a = 0; a < getAspectCount(); a++) {
            int[] ports = getActivePorts(a, rotation);
            for (int p : ports) {
                if (p == port) return a;
            }
        }
        return 0;
    }

    public boolean hasValidDiagonal(int port1, int port2, int rotation) {
        int rotSteps = ((rotation / 90) % 4 + 4) % 4;
        int base1 = ((port1 - rotSteps) % 4 + 4) % 4;
        int base2 = ((port2 - rotSteps) % 4 + 4) % 4;
        if (base1 > base2) { int t = base1; base1 = base2; base2 = t; }
        int[] basePairs = getBaseDiagonalPairs();
        for (int i = 0; i < basePairs.length; i += 2) {
            if (basePairs[i] == base1 && basePairs[i + 1] == base2) {
                return true;
            }
        }
        return false;
    }

    private int[] getBaseDiagonalPairs() {
        return switch (this) {
            case DIAGONAL -> new int[] { PORT_LEFT, PORT_BOTTOM, PORT_TOP, PORT_RIGHT };
            case CURVE_LEFT -> new int[] { PORT_TOP, PORT_RIGHT };
            case CURVE_RIGHT -> new int[] { PORT_RIGHT, PORT_BOTTOM };
            case TURNOUT_LEFT -> new int[] { PORT_TOP, PORT_RIGHT };
            case TURNOUT_RIGHT -> new int[] { PORT_RIGHT, PORT_BOTTOM };
            case TURNOUT_3WAY -> new int[] { PORT_TOP, PORT_RIGHT, PORT_RIGHT, PORT_BOTTOM };
            default -> new int[] { };
        };
    }

    public boolean isValidThroughPath(int port1, int port2, int rotation) {
        int rotSteps = ((rotation / 90) % 4 + 4) % 4;
        int base1 = ((port1 - rotSteps) % 4 + 4) % 4;
        int base2 = ((port2 - rotSteps) % 4 + 4) % 4;
        if (base1 > base2) { int t = base1; base1 = base2; base2 = t; }
        int[] paths = getBaseThroughPaths();
        for (int i = 0; i < paths.length; i += 2) {
            if (paths[i] == base1 && paths[i + 1] == base2) {
                return true;
            }
        }
        return false;
    }

    private int[] getBaseThroughPaths() {
        return switch (this) {
            case STRAIGHT, SIGNAL_2, SIGNAL_3 -> new int[] { PORT_LEFT, PORT_RIGHT };
            case DIAGONAL -> new int[] { PORT_LEFT, PORT_TOP, PORT_LEFT, PORT_RIGHT, PORT_TOP, PORT_BOTTOM, PORT_RIGHT, PORT_BOTTOM };
            case CURVE_LEFT -> new int[] { PORT_LEFT, PORT_TOP };
            case CURVE_RIGHT -> new int[] { PORT_LEFT, PORT_BOTTOM };
            case TURNOUT_LEFT -> new int[] { PORT_LEFT, PORT_RIGHT, PORT_LEFT, PORT_TOP };
            case TURNOUT_RIGHT -> new int[] { PORT_LEFT, PORT_RIGHT, PORT_LEFT, PORT_BOTTOM };
            case TURNOUT_3WAY -> new int[] { PORT_LEFT, PORT_RIGHT, PORT_LEFT, PORT_TOP, PORT_LEFT, PORT_BOTTOM };
        };
    }

    public int getAspectCount() {
        return switch (this) {
            case STRAIGHT, DIAGONAL, CURVE_LEFT, CURVE_RIGHT -> 1;
            case TURNOUT_LEFT, TURNOUT_RIGHT, SIGNAL_2 -> 2;
            case TURNOUT_3WAY, SIGNAL_3 -> 3;
        };
    }

    private int[] getBasePorts(int aspect) {
        return switch (this) {
            case STRAIGHT -> new int[] { PORT_LEFT, PORT_RIGHT };
            case DIAGONAL -> new int[] { PORT_BOTTOM, PORT_TOP };
            case CURVE_LEFT -> new int[] { PORT_LEFT, PORT_TOP };
            case CURVE_RIGHT -> new int[] { PORT_LEFT, PORT_BOTTOM };
            case TURNOUT_LEFT -> aspect == 0
                ? new int[] { PORT_LEFT, PORT_RIGHT }
                : new int[] { PORT_LEFT, PORT_TOP };
            case TURNOUT_RIGHT -> aspect == 0
                ? new int[] { PORT_LEFT, PORT_RIGHT }
                : new int[] { PORT_LEFT, PORT_BOTTOM };
            case TURNOUT_3WAY -> switch (aspect) {
                case 0 -> new int[] { PORT_LEFT, PORT_RIGHT };
                case 1 -> new int[] { PORT_LEFT, PORT_TOP };
                default -> new int[] { PORT_LEFT, PORT_BOTTOM };
            };
            case SIGNAL_2, SIGNAL_3 -> new int[] { PORT_LEFT, PORT_RIGHT };
        };
    }
}
