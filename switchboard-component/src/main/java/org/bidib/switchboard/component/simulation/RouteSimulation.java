package org.bidib.switchboard.component.simulation;

import java.util.List;
import java.util.Set;

import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.model.Block;
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

public class RouteSimulation {

    private static final Logger LOG = LoggerFactory.getLogger(RouteSimulation.class);

    private static final long AUTO_CHANGE_DELAY_MS = 2000;

    private final RailwayModel model;
    private final TileGrid tileGrid;
    private final OccupancyFactory occupancyFactory;

    private Route route;
    private String trainId;
    private List<int[]> path;
    private Set<Integer> stops;
    private int currentIndex;
    private boolean running;
    private boolean pausedAtStation;
    private long stationPausedSince = -1;
    private int dwellTimeMs = 5000;
    private boolean autoChangeSignal;
    private long signalBlockedSince = -1;
    private boolean startSignalSet;
    private long startedAt;
    private Runnable onTick;

    public RouteSimulation(RailwayModel model, TileGrid tileGrid, OccupancyFactory occupancyFactory) {
        this.model = model;
        this.tileGrid = tileGrid;
        this.occupancyFactory = occupancyFactory;
    }

    public void setOnTick(Runnable onTick) {
        this.onTick = onTick;
    }

    public void setAutoChangeSignal(boolean autoChangeSignal) {
        this.autoChangeSignal = autoChangeSignal;
    }

    public void start(Route route, String trainId) {
        start(route, trainId, 0);
    }

    public void start(Route route, String trainId, int startIndex) {
        this.route = route;
        this.trainId = trainId;
        this.path = route.getPath();
        this.stops = route.getStops().stream()
            .map(Route.StationStop::getPathIndex)
            .collect(java.util.stream.Collectors.toSet());
        this.currentIndex = Math.max(1, startIndex);
        this.pausedAtStation = false;
        this.stationPausedSince = -1;
        this.startSignalSet = false;
        this.startedAt = System.currentTimeMillis();

        if (path.isEmpty()) {
            running = false;
            return;
        }

        // Clear any existing occupancies along the path
        clearOccupancies();

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
        setOccupied(path.get(0));

        // If there is a signal at the start position, switch it to green immediately
        Tile startTile = tileGrid.getTile(path.get(0)[0], path.get(0)[1]);
        if (startTile instanceof ElementTile et && et.getElementId() != null) {
            ElementType type = et.getElementType();
            if (type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED) {
                model.setElementAspect(et.getElementId(), 1);
                LOG.info("Start signal {} set to green", et.getElementId());
            }
        }

        // Move train to first block on the path
        assignTrainToFirstBlock();

        currentIndex = 1;
        running = true;
        LOG.info("Started route '{}' with train {}", route.getName() != null ? route.getName() : route.getId(), trainId);
        notifyTick();
    }

    public void stop() {
        running = false;
        pausedAtStation = false;
        stationPausedSince = -1;
    }

    public void reset() {
        running = false;
        pausedAtStation = false;
        stationPausedSince = -1;
        clearOccupancies();
        currentIndex = 0;
        notifyTick();
    }

    public void tick() {
        if (!running || path == null || currentIndex >= path.size()) {
            running = false;
            notifyTick();
            return;
        }

        // Wait 2s after start for turnouts to switch before moving
        if (!startSignalSet && System.currentTimeMillis() - startedAt < AUTO_CHANGE_DELAY_MS) {
            notifyTick();
            return;
        }
        startSignalSet = true;

        // Handle station stop dwell
        if (pausedAtStation) {
            long elapsed = System.currentTimeMillis() - stationPausedSince;
            if (elapsed < dwellTimeMs) {
                notifyTick();
                return; // Still dwelling
            }
            pausedAtStation = false;
            stationPausedSince = -1;

            // Set signal at the station stop to green
            int stationIndex = currentIndex - 1;
            if (stationIndex >= 0 && stationIndex < path.size()) {
                Tile stationTile = tileGrid.getTile(path.get(stationIndex)[0], path.get(stationIndex)[1]);
                if (stationTile instanceof ElementTile et && et.getElementId() != null) {
                    ElementType type = et.getElementType();
                    if (type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED) {
                        model.setElementAspect(et.getElementId(), 1);
                    }
                }
            }

            LOG.info("Station dwell complete, continuing");
        }

        // Check if the previous tile is a signal blocking the train
        int prev = currentIndex - 1;
        if (prev > 0) {
            Tile pt = tileGrid.getTile(path.get(prev)[0], path.get(prev)[1]);
            int[] pp = path.get(prev - 1);
            int dc = path.get(prev)[0] - pp[0];
            int dr = path.get(prev)[1] - pp[1];
            int entryPort = OccupancySimulation.portFromDelta(dc, dr);
            if (OccupancySimulation.isSignalBlocking(pt, entryPort, model)) {
                // Auto-change signal if enabled and this signal is NOT a station stop
                if (autoChangeSignal && !stops.contains(prev)) {
                    long now = System.currentTimeMillis();
                    if (signalBlockedSince < 0) {
                        signalBlockedSince = now;
                        LOG.info("Train blocked at signal on tile ({},{})", path.get(prev)[0], path.get(prev)[1]);
                    } else if (now - signalBlockedSince >= AUTO_CHANGE_DELAY_MS) {
                    		LOG.info(">>> auto-change signal elapsed.");
                    		
                        if (pt instanceof ElementTile et && et.getElementId() != null) {
                            model.setElementAspect(et.getElementId(), 1);
                            LOG.info("Auto-changed signal {} to green", et.getElementId());
                        }
                        signalBlockedSince = -1;
                    }
                }
                notifyTick();
                return;
            }
        }

        signalBlockedSince = -1;

        // Advance: set previous tile to FREE
        setFree(path.get(prev));

        // Set current tile to OCCUPIED
        setOccupied(path.get(currentIndex));

        // Move train to the block containing the current tile
        assignTrainToCurrentBlock();

        currentIndex++;

        // Check if we just arrived at a station stop
        if (stops.contains(currentIndex - 1)) {
            Route.StationStop stop = route.getStopAt(currentIndex - 1);
            if (stop != null) {
                dwellTimeMs = stop.getDwellTimeMs();
                pausedAtStation = true;
                stationPausedSince = System.currentTimeMillis();
                LOG.info("Stopped at station (index {}), dwell {}ms", currentIndex - 1, dwellTimeMs);
            }
        }

        if (currentIndex >= path.size()) {
            running = false;
            LOG.info("Route '{}' completed", route.getName() != null ? route.getName() : route.getId());
        }

        notifyTick();
    }

    private void clearOccupancies() {
        if (path == null) {
            return;
        }
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

    private void setOccupied(int[] coord) {
        Tile tile = tileGrid.getTile(coord[0], coord[1]);
        if (tile instanceof ElementTile et && et.getElementId() != null) {
            Element el = model.getElement(et.getElementId());
            if (el != null && el.getOccupancy() != null) {
                el.getOccupancy().setState(Occupancy.OccupancyState.OCCUPIED);
            }
        }
    }

    private void setFree(int[] coord) {
        Tile tile = tileGrid.getTile(coord[0], coord[1]);
        if (tile instanceof ElementTile et && et.getElementId() != null) {
            Element el = model.getElement(et.getElementId());
            if (el != null && el.getOccupancy() != null) {
                el.getOccupancy().setState(Occupancy.OccupancyState.FREE);
            }
        }
    }

    private void assignTrainToFirstBlock() {
        if (path.isEmpty()) return;
        int[] first = path.get(0);
        Block block = tileGrid.getBlockModel().getBlockForTile(first[0], first[1]);
        if (block != null) {
            block.setAssignedTrainId(trainId);
        }
    }

    private void assignTrainToCurrentBlock() {
        if (currentIndex < 0 || currentIndex >= path.size()) return;
        int[] coord = path.get(currentIndex);
        Block block = tileGrid.getBlockModel().getBlockForTile(coord[0], coord[1]);
        if (block != null) {
            block.setAssignedTrainId(trainId);
        }
    }

    private void notifyTick() {
        if (onTick != null) {
            onTick.run();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPausedAtStation() {
        return pausedAtStation;
    }

    public Route getRoute() {
        return route;
    }

    public String getTrainId() {
        return trainId;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
