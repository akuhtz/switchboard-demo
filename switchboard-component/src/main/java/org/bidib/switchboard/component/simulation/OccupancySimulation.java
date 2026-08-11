package org.bidib.switchboard.component.simulation;

import java.util.List;

import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.view.TileGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulates a train moving along a route by advancing occupancy state tile-by-tile.
 * <p>
 * The simulation is driven by an external tick source — call {@link #tick()} at the desired
 * interval (e.g., every 200ms). This decouples the simulation from Swing timers, allowing
 * headless usage and direct testing.
 * <p>
 * Signal handling: when a train reaches a main signal (SIGNAL_M3) at aspect 0 (red)
 * from the signal's facing direction, the train stops. If {@link #setAutoChangeSignal(boolean)}
 * is enabled, the signal auto-switches to aspect 1 after {@value #AUTO_CHANGE_DELAY_MS}ms.
 * Distant signals (SIGNAL_V) never stop the train; they mirror the aspect of the next main
 * signal ahead in the path.
 */
public class OccupancySimulation {

    private static final Logger LOG = LoggerFactory.getLogger(OccupancySimulation.class);
    private static final long AUTO_CHANGE_DELAY_MS = 2000;

    private final RailwayModel model;
    private final TileGrid tileGrid;
    private final OccupancyFactory occupancyFactory;

    private Route route;
    private List<int[]> path;
    private int currentIndex;
    private boolean running;
    private boolean autoChangeSignal;
    private long signalBlockedSince = -1;
    private Runnable onTick;

    public OccupancySimulation(RailwayModel model, TileGrid tileGrid, OccupancyFactory occupancyFactory) {
        this.model = model;
        this.tileGrid = tileGrid;
        this.occupancyFactory = occupancyFactory;
    }

    // --- Configuration ---

    public void setAutoChangeSignal(boolean autoChangeSignal) {
        this.autoChangeSignal = autoChangeSignal;
    }

    public boolean isAutoChangeSignal() {
        return autoChangeSignal;
    }

    /**
     * Sets a callback invoked after each tick (e.g., {@code panel::repaint}).
     */
    public void setOnTick(Runnable onTick) {
        this.onTick = onTick;
    }

    // --- Lifecycle ---

    /**
     * Starts (or restarts) the simulation on the given route. Creates occupancies on all path
     * tiles, sets the first tile to OCCUPIED, and marks the simulation as running.
     */
    public void start(Route route) {
        this.route = route;
        this.path = route.getPath();
        this.signalBlockedSince = -1;

        if (path.isEmpty()) {
            running = false;
            return;
        }

        // Create occupancies on all path tiles
        for (int[] p : path) {
            Tile tile = tileGrid.getTile(p[0], p[1]);
            if (tile instanceof ElementTile et && et.getElementId() != null) {
                Element el = model.getElement(et.getElementId());
                if (el != null) {
                    Occupancy occ = occupancyFactory.create(Occupancy.OccupancyState.FREE);
                    model.addOccupancy(occ);
                    el.setOccupancy(occ);
                }
            }
        }

        // Set first tile to OCCUPIED
        int[] first = path.get(0);
        Tile ft = tileGrid.getTile(first[0], first[1]);
        if (ft instanceof ElementTile fet && fet.getElementId() != null) {
            Element fel = model.getElement(fet.getElementId());
            if (fel != null && fel.getOccupancy() != null) {
                fel.getOccupancy().setState(Occupancy.OccupancyState.OCCUPIED);
            }
        }

        currentIndex = 1;
        running = true;

        syncDistantSignals();
    }

    /**
     * Pauses the simulation without clearing state. Can be resumed by calling {@link #tick()}.
     */
    public void stop() {
        running = false;
        signalBlockedSince = -1;
    }

    /**
     * Resets the simulation: sets all occupancies along the route to FREE, resets index.
     */
    public void reset() {
        running = false;
        signalBlockedSince = -1;
        if (path != null) {
            for (int[] p : path) {
                Tile tile = tileGrid.getTile(p[0], p[1]);
                if (tile instanceof ElementTile et && et.getElementId() != null) {
                    Element el = model.getElement(et.getElementId());
                    if (el != null && el.getOccupancy() != null) {
                        el.getOccupancy().setState(Occupancy.OccupancyState.FREE);
                    }
                }
            }
        }
        currentIndex = 0;
    }

    // --- Tick ---

    /**
     * Advances the simulation by one step. Call this from your tick source (e.g., Swing Timer,
     * ScheduledExecutorService, or directly in tests).
     */
    public void tick() {
        if (!running || path == null || currentIndex >= path.size()) {
            running = false;
            return;
        }

        syncDistantSignals();

        int prev = currentIndex - 1;
        int[] pp = path.get(prev);
        Tile pt = tileGrid.getTile(pp[0], pp[1]);

        // Check if current tile is a signal blocking this direction
        boolean blocked = false;
        if (prev > 0) {
            int[] before = path.get(prev - 1);
            int entryPort = portFromDelta(pp[0] - before[0], pp[1] - before[1]);
            blocked = isSignalBlocking(pt, entryPort, model);
        } else {
            blocked = isSignalAtRed(pt, model);
        }

        if (blocked) {
            if (autoChangeSignal) {
                long now = System.currentTimeMillis();
                if (signalBlockedSince < 0) {
                    signalBlockedSince = now;
                    LOG.info("Train blocked at signal on tile ({},{})", pp[0], pp[1]);
                } else if (now - signalBlockedSince >= AUTO_CHANGE_DELAY_MS) {
                    // Auto-change signal to green
                    Element signalEl = model.getElement(pt.getElementId());
                    if (signalEl != null) {
                        model.setElementAspect(signalEl.getId(), 1);
                        LOG.info("Auto-changed signal {} to green", signalEl.getId());
                    }
                    signalBlockedSince = -1;
                }
            }
            notifyTick();
            return; // Don't advance
        }

        // Clear blocked state if we moved past
        signalBlockedSince = -1;

        // Advance: set previous tile to FREE
        if (pt instanceof ElementTile pet && pet.getElementId() != null) {
            Element pel = model.getElement(pet.getElementId());
            if (pel != null && pel.getOccupancy() != null) {
                pel.getOccupancy().setState(Occupancy.OccupancyState.FREE);
            }
        }

        // Set current tile to OCCUPIED
        int[] cp = path.get(currentIndex);
        Tile ct = tileGrid.getTile(cp[0], cp[1]);
        if (ct instanceof ElementTile cet && cet.getElementId() != null) {
            Element cel = model.getElement(cet.getElementId());
            if (cel != null && cel.getOccupancy() != null) {
                cel.getOccupancy().setState(Occupancy.OccupancyState.OCCUPIED);
            }
        }

        currentIndex++;
        if (currentIndex >= path.size()) {
            running = false;
        }

        notifyTick();
    }

    private void notifyTick() {
        if (onTick != null) {
            onTick.run();
        }
    }

    /**
     * Keeps every distant signal (SIGNAL_V) on the route in sync with the next main signal
     * (SIGNAL_M3) ahead in the path, so the distant signal previews the upcoming aspect. For
     * combined signals (SIGNAL_COMBINED) the DISTANT PLATE on the signal's own mast mirrors the
     * next main signal ahead, while the main head keeps its own operator-set aspect.
     */
    private void syncDistantSignals() {
        if (path == null) {
            return;
        }
        for (int i = 0; i < path.size(); i++) {
            int[] p = path.get(i);
            Tile tile = tileGrid.getTile(p[0], p[1]);
            if (!(tile instanceof ElementTile et)) {
                continue;
            }
            if (et.getElementType() == ElementType.SIGNAL_COMBINED) {
                syncCombinedPlate(i, et);
                continue;
            }
            if (et.getElementType() != ElementType.SIGNAL_V || et.getElementId() == null) {
                continue;
            }
            int nextAspect = findNextSignalAspect(i);
            if (nextAspect >= 0) {
                model.setElementAspect(et.getElementId(), nextAspect);
            }
        }
    }

    /** Updates the distant plate of the combined signal at path index i to mirror the next main ahead. */
    private void syncCombinedPlate(int fromIndex, ElementTile combined) {
        int nextAspect = findNextSignalAspect(fromIndex);
        if (nextAspect >= 0) {
            combined.setPlateAspect(nextAspect);
        }
    }

    private int findNextSignalAspect(int fromIndex) {
        for (int i = fromIndex + 1; i < path.size(); i++) {
            int[] p = path.get(i);
            Tile tile = tileGrid.getTile(p[0], p[1]);
            if (tile instanceof ElementTile et && et.getElementId() != null
                && (et.getElementType() == ElementType.SIGNAL_M3
                    || et.getElementType() == ElementType.SIGNAL_COMBINED)) {
                Element el = model.getElement(et.getElementId());
                if (el != null) {
                    return distantAspectForMainSignal(et.getElementType(), el.getCurrentAspect());
                }
            }
        }
        return -1;
    }

    /**
     * Maps a next main signal's aspect to the distant signal (SIGNAL_V) aspect that previews it.
     * SIGNAL_M3 (canonical order red, green, yellow): red -> orange (Halt erwarten),
     * green -> green (Frei erwarten), yellow -> orange+green (Langsamfahrt erwarten).
     * Aspect indices match: 0 -> orange, 1 -> green, 2 -> orange+green.
     */
    public static int distantAspectForMainSignal(ElementType type, int mainAspect) {
        return mainAspect;
    }

    // --- Query ---

    public boolean isRunning() {
        return running;
    }

    public Route getRoute() {
        return route;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isBlockedAtSignal() {
        return signalBlockedSince >= 0;
    }

    // --- Static signal utilities ---

    /**
     * Returns true if the tile is a main signal (SIGNAL_M3 or SIGNAL_COMBINED) with aspect 0 (red).
     * Distant signals (SIGNAL_V) never block a train.
     */
    public static boolean isSignalAtRed(Tile tile, RailwayModel model) {
        if (!(tile instanceof ElementTile et)) {
            return false;
        }
        ElementType type = et.getElementType();
        if (type != ElementType.SIGNAL_M3 && type != ElementType.SIGNAL_COMBINED) {
            return false;
        }
        Element el = model.getElement(et.getElementId());
        return el != null && el.getCurrentAspect() == 0;
    }

    /**
     * Returns true if the tile is a signal at red AND the train entered from the signal's
     * facing direction.
     * <p>
     * Signal facing convention: at rotation 0 the signal faces LEFT, meaning it stops trains
     * entering from port LEFT (i.e., trains moving LEFT→RIGHT).
     */
    public static boolean isSignalBlocking(Tile tile, int entryPort, RailwayModel model) {
        if (!isSignalAtRed(tile, model)) {
            return false;
        }
        int rotSteps = (tile.getRotation() / 90) % 4;
        int facingPort = (ElementType.PORT_LEFT + rotSteps) % 4;
        return entryPort == facingPort;
    }

    /**
     * Computes the port through which a train enters a tile, given the movement delta
     * from the previous tile to the current tile.
     * <p>
     * If the train moved right (dc=1), it entered the current tile from the LEFT port.
     */
    public static int portFromDelta(int dc, int dr) {
        if (dc == 1) return ElementType.PORT_LEFT;
        if (dc == -1) return ElementType.PORT_RIGHT;
        if (dr == 1) return ElementType.PORT_TOP;
        if (dr == -1) return ElementType.PORT_BOTTOM;
        return -1;
    }
}
