package org.bidib.switchboard.component.simulation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Timer;

import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.model.Block;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.service.RouterService;
import org.bidib.switchboard.component.view.TileGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteSimulation {

    private static final Logger LOG = LoggerFactory.getLogger(RouteSimulation.class);

    private static final long AUTO_CHANGE_DELAY_MS = 2000;
    private static final long BLOCK_MARKER_CLEAR_DELAY_MS = 2000;
    private static final int DEFAULT_TRAIN_LENGTH = 3;
    private static final int DEFAULT_SIGNAL_RESET_DISTANCE = 3;

    private final RailwayModel model;
    private final TileGrid tileGrid;
    private final OccupancyFactory occupancyFactory;
    private RouterService routerService;
    private int trainLength = DEFAULT_TRAIN_LENGTH;
    private int signalResetDistance = DEFAULT_SIGNAL_RESET_DISTANCE;

    private Route route;
    private String trainId;
    private List<int[]> path;
    private Set<Integer> stops;
    private int currentIndex;
    private boolean running;
    private boolean finished;
    private boolean pausedAtStation;
    private long stationPausedSince = -1;
    private int dwellTimeMs = 5000;
    private boolean autoChangeSignal;
    private long signalBlockedSince = -1;
    private boolean startSignalSet;
    private long startedAt;
    private int lastGreenSignalIndex = -1;
    private Runnable onTick;
    private TrainMovement trainMovement;
    private Block previousBlock;
    private final Map<Block, Long> blockDepartures = new HashMap<>();
    private Timer blockCleanupTimer;

    public RouteSimulation(RailwayModel model, TileGrid tileGrid, OccupancyFactory occupancyFactory) {
        this(model, tileGrid, occupancyFactory, null, DEFAULT_TRAIN_LENGTH);
    }

    public RouteSimulation(RailwayModel model, TileGrid tileGrid, OccupancyFactory occupancyFactory,
            RouterService routerService, int trainLength) {
        this.model = model;
        this.tileGrid = tileGrid;
        this.occupancyFactory = occupancyFactory;
        this.routerService = routerService;
        this.trainLength = Math.max(1, trainLength);
        this.trainMovement = new TrainMovement(this.trainLength);
    }

    public void setRouterService(RouterService routerService) {
        this.routerService = routerService;
    }

    public void setTrainLength(int trainLength) {
        this.trainLength = Math.max(1, trainLength);
        this.trainMovement = new TrainMovement(this.trainLength);
    }

    public int getTrainLength() {
        return trainLength;
    }

    public void setSignalResetDistance(int distance) {
        this.signalResetDistance = Math.max(1, distance);
    }

    public int getSignalResetDistance() {
        return signalResetDistance;
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
        this.finished = false;
        this.previousBlock = null;
        this.lastGreenSignalIndex = -1;

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

        // Initialize train movement: head at path[0], cars behind on physical track
        int[] headPos = path.get(0);
        trainMovement.initialize(headPos, tileGrid, routerService);

        // Set all train positions to OCCUPIED (including off-path backward tiles)
        for (int[] pos : trainMovement.getPositions()) {
            setOccupied(pos);
        }

        // If there is a signal at the start position, switch it to green immediately
        Tile startTile = tileGrid.getTile(headPos[0], headPos[1]);
        if (startTile instanceof ElementTile et && et.getElementId() != null) {
            ElementType type = et.getElementType();
            if (type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED) {
                model.setElementAspect(et.getElementId(), 1);
                lastGreenSignalIndex = 0;
                LOG.info("Start signal {} set to green", et.getElementId());
            }
        }

        // Move train to first block on the path
        assignTrainToFirstBlock();

        running = true;
        startBlockCleanupTimer();
        LOG.info("Started route '{}' with train {} (length {})",
            route.getName() != null ? route.getName() : route.getId(), trainId, trainLength);
        notifyTick();
    }

    public void stop() {
        running = false;
        finished = false;
        pausedAtStation = false;
        stationPausedSince = -1;
        // Record departure for the current block so it gets cleaned up
        if (previousBlock != null) {
            recordBlockDeparture(previousBlock);
        }
        startBlockCleanupTimer();
    }

    public void reset() {
        running = false;
        finished = false;
        pausedAtStation = false;
        stationPausedSince = -1;
        stopBlockCleanupTimer();
        blockDepartures.clear();
        previousBlock = null;
        // Free all train positions
        for (int[] pos : trainMovement.getPositions()) {
            setFree(pos);
        }
        trainMovement.clear();
        currentIndex = 0;
        notifyTick();
    }

    public void tick() {
        if (!running || path == null || currentIndex >= path.size()) {
            running = false;
            finished = (currentIndex >= path.size());
            if (finished) {
                LOG.info("Route '{}' completed", route.getName() != null ? route.getName() : route.getId());
                // Don't record departure — the train is still in the last block
            }
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
                        lastGreenSignalIndex = stationIndex;
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
                            lastGreenSignalIndex = prev;
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

        // Advance train: get new head position
        int[] newHead = path.get(currentIndex);
        List<int[]> freed = trainMovement.advance(newHead);

        // Free the tail positions that were left behind
        for (int[] freedPos : freed) {
            setFree(freedPos);
        }

        // Set new head to OCCUPIED
        setOccupied(newHead);

        // Assign train to the block containing the head
        Block currentBlock = tileGrid.getBlockModel().getBlockForTile(newHead[0], newHead[1]);
        if (currentBlock != null) {
            currentBlock.setAssignedTrainId(trainId);
        }

        // Check if the head has left the previous block
        if (previousBlock != null && previousBlock != currentBlock) {
            recordBlockDeparture(previousBlock);
        }
        // Keep previousBlock even if currentBlock is null (head on unregistered tile)
        if (currentBlock != null) {
            previousBlock = currentBlock;
        }

        currentIndex++;

        // Reset signal to red when head is past the signal by the configured distance
        if (lastGreenSignalIndex >= 0 && currentIndex >= lastGreenSignalIndex + signalResetDistance) {
            int[] sigPos = path.get(lastGreenSignalIndex);
            Tile sigTile = tileGrid.getTile(sigPos[0], sigPos[1]);
            if (sigTile instanceof ElementTile et && et.getElementId() != null) {
                model.setElementAspect(et.getElementId(), 0);
                LOG.info("Reset signal {} to red after train passed", et.getElementId());
            }
            lastGreenSignalIndex = -1;
        }

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
            previousBlock = block;
        }
    }

    private void clearBlockAssignment(Block block) {
        block.setAssignedTrainId(null);
        notifyTick();
        LOG.info("Cleared train assignment from block '{}'", block.getName());
    }

    private void recordBlockDeparture(Block block) {
        if (!blockDepartures.containsKey(block)) {
            blockDepartures.put(block, System.currentTimeMillis());
            LOG.info("Recorded departure from block '{}'", block.getName());
        }
    }

    private void startBlockCleanupTimer() {
        if (blockCleanupTimer != null) {
            return; // already running
        }
        LOG.info("Starting block cleanup timer ({} blocks pending)", blockDepartures.size());
        blockCleanupTimer = new Timer(1000, e -> checkBlockCleanups());
        blockCleanupTimer.setRepeats(true);
        blockCleanupTimer.start();
    }

    private void stopBlockCleanupTimer() {
        if (blockCleanupTimer != null) {
	    		LOG.info(">>> stopBlockCleanupTimer");
	
	    		blockCleanupTimer.stop();
            blockCleanupTimer = null;
        }
    }

    private void checkBlockCleanups() {
        long now = System.currentTimeMillis();
        
//        LOG.info("Check block cleanups.");
        
        boolean anyRemaining = false;
        Iterator<Map.Entry<Block, Long>> it = blockDepartures.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Block, Long> entry = it.next();
            long elapsed = now - entry.getValue();
            if (elapsed >= BLOCK_MARKER_CLEAR_DELAY_MS) {
                Block block = entry.getKey();
                it.remove();
                LOG.info("Block '{}' departure delay elapsed ({}ms), clearing assignment", block.getName(), elapsed);
                if (block.getAssignedTrainId() != null) {
                    clearBlockAssignment(block);
                } else {
                    LOG.info("Block '{}' already has no assignment, skipping", block.getName());
                }
            } else {
                anyRemaining = true;
            }
        }
//        if (!anyRemaining) {
//            LOG.info("No more blocks pending cleanup, stopping timer");
//            stopBlockCleanupTimer();
//        }
    }

    private void notifyTick() {
        if (onTick != null) {
            onTick.run();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
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

    public List<int[]> getTrainPositions() {
        return trainMovement.getPositions();
    }
}