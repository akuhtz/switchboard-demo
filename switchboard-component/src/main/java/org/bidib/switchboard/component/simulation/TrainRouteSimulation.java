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
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.model.TrainRoute;
import org.bidib.switchboard.component.view.TileGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrainRouteSimulation {

    private static final Logger LOG = LoggerFactory.getLogger(TrainRouteSimulation.class);

    private final RailwayModel model;
    private final TileGrid tileGrid;
    private final OccupancyFactory occupancyFactory;

    private TrainRoute trainRoute;
    private String trainId;
    private List<int[]> path;
    private Set<Integer> stops;
    private int currentIndex;
    private boolean running;
    private boolean pausedAtStation;
    private long stationPausedSince = -1;
    private int dwellTimeMs = 5000;
    private Runnable onTick;

    public TrainRouteSimulation(RailwayModel model, TileGrid tileGrid, OccupancyFactory occupancyFactory) {
        this.model = model;
        this.tileGrid = tileGrid;
        this.occupancyFactory = occupancyFactory;
    }

    public void setOnTick(Runnable onTick) {
        this.onTick = onTick;
    }

    public void start(TrainRoute trainRoute, String trainId) {
        start(trainRoute, trainId, 0);
    }

    public void start(TrainRoute trainRoute, String trainId, int startIndex) {
        this.trainRoute = trainRoute;
        this.trainId = trainId;
        this.path = trainRoute.getPath();
        this.stops = trainRoute.getStops().stream()
            .map(TrainRoute.StationStop::getPathIndex)
            .collect(java.util.stream.Collectors.toSet());
        this.currentIndex = Math.max(1, startIndex);
        this.pausedAtStation = false;
        this.stationPausedSince = -1;

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

        // Move train to first block on the path
        assignTrainToFirstBlock();

        currentIndex = 1;
        running = true;
        LOG.info("Started train route '{}' with train {}", trainRoute.getName(), trainId);
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

        // Handle station stop dwell
        if (pausedAtStation) {
            long elapsed = System.currentTimeMillis() - stationPausedSince;
            if (elapsed < dwellTimeMs) {
                notifyTick();
                return; // Still dwelling
            }
            pausedAtStation = false;
            stationPausedSince = -1;
            LOG.info("Station dwell complete, continuing");
        }

        // Advance: set previous tile to FREE
        int prev = currentIndex - 1;
        setFree(path.get(prev));

        // Set current tile to OCCUPIED
        setOccupied(path.get(currentIndex));

        // Move train to the block containing the current tile
        assignTrainToCurrentBlock();

        currentIndex++;

        // Check if we just arrived at a station stop
        if (stops.contains(currentIndex - 1)) {
            TrainRoute.StationStop stop = trainRoute.getStopAt(currentIndex - 1);
            if (stop != null) {
                dwellTimeMs = stop.getDwellTimeMs();
                pausedAtStation = true;
                stationPausedSince = System.currentTimeMillis();
                LOG.info("Stopped at station (index {}), dwell {}ms", currentIndex - 1, dwellTimeMs);
            }
        }

        if (currentIndex >= path.size()) {
            running = false;
            LOG.info("Train route '{}' completed", trainRoute.getName());
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

    public TrainRoute getTrainRoute() {
        return trainRoute;
    }

    public String getTrainId() {
        return trainId;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
