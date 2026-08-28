package org.bidib.switchboard.component.simulation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.bidib.switchboard.component.model.SignalTile;
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
    private static final int DEFAULT_SIGNAL_RESET_DISTANCE = 5;
    private static final int DEFAULT_TICK_INTERVAL_MS = 200;
    private static final int BLOCK_CLEANUP_EVERY_N_TICKS = 5;

    private final RailwayModel model;
    private final TileGrid tileGrid;
    private final OccupancyFactory occupancyFactory;
    private RouterService routerService;
    private int trainLength = DEFAULT_TRAIN_LENGTH;
    private int signalResetDistance = DEFAULT_SIGNAL_RESET_DISTANCE;
    private int tickIntervalMs = DEFAULT_TICK_INTERVAL_MS;

    private Route route;
    private String trainId;
    private List<int[]> path;
    private Set<Integer> stops;
    private int currentIndex;
    private boolean running;
    private boolean finished;
    private boolean pausedAtStation;
    private long stationPausedSince = -1;
    private long stationReservationSince = -1;
    private int dwellTimeMs = 5000;
    private boolean autoChangeSignal;
    private long signalBlockedSince = -1;
    private boolean startSignalSet;
    private boolean startGreenSet;
    private long startedAt;
    private int lastGreenSignalIndex = -1;
    private Runnable onTick;
    private Runnable onComplete;
    private Runnable tickSource;
    private TrainMovement trainMovement;
    private Block previousBlock;
    private final Map<Block, Long> blockDepartures = new HashMap<>();
    private final Map<String, String> reservedGapTiles = new HashMap<>();
    private Timer simulationTimer;
    private int tickCount;

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

    public void setTickInterval(int tickIntervalMs) {
        this.tickIntervalMs = Math.max(50, tickIntervalMs);
    }

    public int getTickInterval() {
        return tickIntervalMs;
    }

    public void setOnTick(Runnable onTick) {
        this.onTick = onTick;
    }

    public void setOnComplete(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public void setAutoChangeSignal(boolean autoChangeSignal) {
        this.autoChangeSignal = autoChangeSignal;
    }

    /**
     * Sets a custom tick source. When set, the simulation does not create an internal
     * Swing Timer. The caller is responsible for calling {@link #simulationTick()} at
     * the desired interval. When not set (default), a Swing Timer drives the tick loop.
     */
    public void setTickSource(Runnable tickSource) {
        this.tickSource = tickSource;
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
        this.stationReservationSince = -1;
        this.startSignalSet = false;
        this.startGreenSet = false;
        this.startedAt = System.currentTimeMillis();
        this.finished = false;
        this.previousBlock = null;
        this.lastGreenSignalIndex = -1;
        this.tickCount = 0;
        this.reservedGapTiles.clear();

        if (path.isEmpty()) {
            running = false;
            return;
        }

        // Check if the guard block is reserved before configuring turnout aspects
        Block nextBlock = findGuardedBlock(0);
        boolean canReserve = false;
        if (nextBlock != null) {
            String reservedBy = nextBlock.getAssignedTrainIds().stream()
                .filter(id -> !id.equals(trainId)).findFirst().orElse(null);
            if (reservedBy != null) {
                LOG.info("Block '{}' reserved by train {} — train {} will wait at start signal",
                    nextBlock.getName(), reservedBy, trainId);
            } else if (!nextBlock.isReserved()) {
                canReserve = true;
            }
        } else {
            canReserve = true;
        }

        // Only set reserved gap tile aspects if the guard block is free — turnouts
        // must not be switched when another train holds the next block
        if (canReserve && routerService != null) {
            setReservedGapTileAspects();
        }

        // Reset all signals on the route to red — the simulation manages them
        for (int[] p : path) {
            Tile tile = tileGrid.getTile(p[0], p[1]);
            if (tile instanceof ElementTile et && et.getElementId() != null) {
                ElementType type = et.getElementType();
                if (type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED) {
                    model.setElementAspect(et.getElementId(), 0);
                }
            }
        }

        // Clear any existing occupancies along the path (only this train's)
        clearOccupancies();

        // Ensure occupancies exist on all path tiles (reuse existing if present)
        for (int[] p : path) {
            Tile tile = tileGrid.getTile(p[0], p[1]);
            if (tile instanceof ElementTile et && et.getElementId() != null) {
                Element el = model.getElement(et.getElementId());
                if (el != null) {
                    Occupancy occ = el.getOccupancy();
                    if (occ == null) {
                        occ = occupancyFactory.create(Occupancy.OccupancyState.FREE);
                        model.addOccupancy(occ);
                        el.setOccupancy(occ);
                    }
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

        // Reserve the next block if it's free
        if (canReserve && nextBlock != null) {
            nextBlock.addAssignedTrain(trainId);
            reserveGapTiles(findGapTiles(0, nextBlock), nextBlock.getId());
            LOG.info("Reserved block '{}' for train {} at start", nextBlock.getName(), trainId);
        }

        // Ensure signal at start position is red — tick() will set it to green after the delay
        Tile startTile = tileGrid.getTile(headPos[0], headPos[1]);
        if (startTile instanceof ElementTile et && et.getElementId() != null) {
            ElementType type = et.getElementType();
            if (type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED) {
                model.setElementAspect(et.getElementId(), 0);
                LOG.info("Start signal {} set to red (will turn green after delay)", et.getElementId());
            }
        }
        syncDistantSignals();

        // Move train to first block on the path
        assignTrainToFirstBlock();

        running = true;
        startSimulationTimer();
        LOG.info("Started route '{}' with train {} (length {})",
            route.getName() != null ? route.getName() : route.getId(), trainId, trainLength);
        notifyTick();
    }

    public void stop() {
        LOG.info("Stop route simulation.");
        running = false;
        finished = false;
        pausedAtStation = false;
        stationPausedSince = -1;
        stationReservationSince = -1;
        stopSimulationTimer();
        reservedGapTiles.clear();
        // Record departure for the current block so it gets cleaned up
        if (previousBlock != null) {
            recordBlockDeparture(previousBlock);
        }
        checkBlockCleanups();
    }

    public void reset() {
        running = false;
        finished = false;
        pausedAtStation = false;
        stationPausedSince = -1;
        stationReservationSince = -1;
        stopSimulationTimer();
        blockDepartures.clear();
        reservedGapTiles.clear();
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
                stopSimulationTimer();
            }
            notifyTick();
            if (finished && onComplete != null) {
                onComplete.run();
            }
            return;
        }

        // Wait 2s after start for turnouts to switch before moving
        if (!startSignalSet && System.currentTimeMillis() - startedAt < AUTO_CHANGE_DELAY_MS) {
            notifyTick();
            return;
        }
        startSignalSet = true;

        // Set start signal to green after the 2s delay (only once)
        if (!startGreenSet && currentIndex >= 1) {
            // Check if the next block is reserved by another train — wait if so
            Block nextBlock = findGuardedBlock(0);
            if (nextBlock != null) {
                String reservedBy = nextBlock.getAssignedTrainIds().stream()
                    .filter(id -> !id.equals(trainId)).findFirst().orElse(null);
                if (reservedBy != null) {
                    LOG.info("Start signal stays red — block '{}' reserved by train {}", nextBlock.getName(), reservedBy);
                    notifyTick();
                    return;
                }
            }
            startGreenSet = true;
            // Configure turnout aspects only for the gap tiles this train has reserved
            if (routerService != null) {
                setReservedGapTileAspects();
            }
            int[] startPos = path.get(0);
            Tile startTile = tileGrid.getTile(startPos[0], startPos[1]);
            if (startTile instanceof ElementTile et && et.getElementId() != null) {
                ElementType type = et.getElementType();
                if (type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED) {
                    model.setElementAspect(et.getElementId(), 1);
                    lastGreenSignalIndex = 0;
                    syncDistantSignals();
                    LOG.info("Set start signal {} to green after delay", et.getElementId());
                }
            }
        }

        // Handle station stop dwell
        if (pausedAtStation) {
            long elapsed = System.currentTimeMillis() - stationPausedSince;
            if (elapsed < dwellTimeMs) {
                notifyTick();
                return; // Still dwelling
            }

            // After dwell: reserve next block, then set signal to green after 2s
            int stationIndex = currentIndex - 1;

            // First, reset the station signal to red
            if (stationIndex >= 0 && stationIndex < path.size()) {
                Tile stationTile = tileGrid.getTile(path.get(stationIndex)[0], path.get(stationIndex)[1]);
                if (stationTile instanceof ElementTile et && et.getElementId() != null) {
                    ElementType type = et.getElementType();
                    if ((type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED)
                        && model.getElement(et.getElementId()).getCurrentAspect() != 0) {
                        model.setElementAspect(et.getElementId(), 0);
                        lastGreenSignalIndex = -1;
                        syncDistantSignals();
                        LOG.info("Reset station signal {} to red after dwell", et.getElementId());
                    }
                }
            }

                    // Reserve the guard block if needed
                    if (stationIndex >= 0 && stationIndex < path.size()) {
                        Block guardBlock = findGuardedBlock(stationIndex);
                        if (guardBlock != null) {
                            String reservedBy = guardBlock.getAssignedTrainIds().stream()
                                .filter(id -> !id.equals(trainId)).findFirst().orElse(null);
                            if (reservedBy != null) {
                                // Another train has this block — stay paused, retry next tick
                                pausedAtStation = true;
                                stationPausedSince = System.currentTimeMillis() - dwellTimeMs;
                                LOG.info("Station dwell complete but block '{}' reserved by train {}, waiting",
                                    guardBlock.getName(), reservedBy);
                                notifyTick();
                                return;
                            }
                            // Check if any gap tiles are reserved by another train
                            Set<int[]> gapTiles = findGapTiles(stationIndex, guardBlock);
                            for (int[] coord : gapTiles) {
                                String blockId = reservedGapTiles.get(coord[0] + "," + coord[1]);
                                if (blockId != null && !blockId.equals(guardBlock.getId())) {
                                    pausedAtStation = true;
                                    stationPausedSince = System.currentTimeMillis() - dwellTimeMs;
                                    LOG.info("Station dwell complete but gap tile ({},{}) reserved by another block, waiting",
                                        coord[0], coord[1]);
                                    notifyTick();
                                    return;
                                }
                            }
                            if (!guardBlock.isReserved()) {
                                guardBlock.addAssignedTrain(trainId);
                                reserveGapTiles(gapTiles, guardBlock.getId());
                                LOG.info("Reserved block '{}' for train {} after station dwell",
                                    guardBlock.getName(), trainId);
                            }
                        }
                    }

            // Wait 2s after block reservation before setting signal to green
            if (stationReservationSince < 0) {
                stationReservationSince = System.currentTimeMillis();
                notifyTick();
                return;
            }
            if (System.currentTimeMillis() - stationReservationSince < AUTO_CHANGE_DELAY_MS) {
                notifyTick();
                return;
            }
            stationReservationSince = -1;

            // Set signal to green
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
            syncDistantSignals();
            pausedAtStation = false;
            stationPausedSince = -1;

            LOG.info("Station dwell complete, signal set to green");
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
                // Check if this signal guards a block boundary
                Block guardBlock = findGuardedBlock(prev);
                if (guardBlock != null) {
                    // Check if any gap tiles between signal and guard block are reserved by another train
                    Set<int[]> gapTiles = findGapTiles(prev, guardBlock);
                    for (int[] coord : gapTiles) {
                        String gapBlockId = reservedGapTiles.get(coord[0] + "," + coord[1]);
                        if (gapBlockId != null && !gapBlockId.equals(guardBlock.getId())) {
                            LOG.info("Signal at ({},{}) stays red — gap tile ({},{}) reserved by block {}",
                                path.get(prev)[0], path.get(prev)[1], coord[0], coord[1], gapBlockId);
                            notifyTick();
                            return;
                        }
                    }
                    String reservedBy = guardBlock.getAssignedTrainIds().stream()
                        .filter(id -> !id.equals(trainId)).findFirst().orElse(null);
                    if (reservedBy != null) {
                        // Another train has reserved this block — signal stays red
                        LOG.info("Signal at ({},{}) stays red — block '{}' reserved by train {}",
                            path.get(prev)[0], path.get(prev)[1], guardBlock.getName(), reservedBy);
                        notifyTick();
                        return;
                    }
                    // Block is free — reserve it and set signal to green
                    if (!guardBlock.isReserved()) {
                        guardBlock.addAssignedTrain(trainId);
                        reserveGapTiles(gapTiles, guardBlock.getId());
                        LOG.info("Reserved block '{}' for train {} at signal ({},{})",
                            guardBlock.getName(), trainId, path.get(prev)[0], path.get(prev)[1]);
                    }
                }
                // Auto-change signal if enabled (or if guard block was free/already ours)
                if (!stops.contains(prev)) {
                    long now = System.currentTimeMillis();
                    if (signalBlockedSince < 0) {
                        signalBlockedSince = now;
                        LOG.info("Train blocked at signal on tile ({},{})", path.get(prev)[0], path.get(prev)[1]);
                    } else if (autoChangeSignal || guardBlock != null) {
                        if (now - signalBlockedSince >= AUTO_CHANGE_DELAY_MS) {
                            if (pt instanceof ElementTile et && et.getElementId() != null) {
                                model.setElementAspect(et.getElementId(), 1);
                                lastGreenSignalIndex = prev;
                                LOG.info("Set signal {} to green", et.getElementId());
                            }
                            signalBlockedSince = -1;
                            syncDistantSignals();
                        }
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
            currentBlock.addAssignedTrain(trainId);
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
            syncDistantSignals();
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

    /**
     * Advances the simulation by one tick. When using a custom tick source,
     * the caller should invoke this method at the desired interval.
     * Handles both movement and block cleanup.
     */
    public void simulationTick() {
        if (running) {
            tick();
        }
        tickCount++;
        if (tickCount % BLOCK_CLEANUP_EVERY_N_TICKS == 0) {
            checkBlockCleanups();
        }
    }

    private void startSimulationTimer() {
        if (tickSource != null) {
            LOG.info("Using custom tick source");
            return;
        }
        if (simulationTimer != null) {
            return;
        }
        simulationTimer = new Timer(tickIntervalMs, e -> simulationTick());
        simulationTimer.setRepeats(true);
        simulationTimer.start();
    }

    private void stopSimulationTimer() {
        if (simulationTimer != null) {
            LOG.info("Stop simulation timer.");
            simulationTimer.stop();
            simulationTimer = null;
        }
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
                    el.getOccupancy().removeOccupant(trainId);
                }
            }
        }
    }

    private void setOccupied(int[] coord) {
        Tile tile = tileGrid.getTile(coord[0], coord[1]);
        if (tile instanceof ElementTile et && et.getElementId() != null) {
            Element el = model.getElement(et.getElementId());
            if (el != null) {
                Occupancy occ = el.getOccupancy();
                if (occ == null) {
                    occ = occupancyFactory.create(Occupancy.OccupancyState.FREE);
                    model.addOccupancy(occ);
                    el.setOccupancy(occ);
                }
                occ.addOccupant(trainId);
            }
        }
    }

    private void setFree(int[] coord) {
        Tile tile = tileGrid.getTile(coord[0], coord[1]);
        if (tile instanceof ElementTile et && et.getElementId() != null) {
            Element el = model.getElement(et.getElementId());
            if (el != null && el.getOccupancy() != null) {
                el.getOccupancy().removeOccupant(trainId);
            }
        }
    }

    private void assignTrainToFirstBlock() {
        if (path.isEmpty()) return;
        int[] first = path.get(0);
        Block block = tileGrid.getBlockModel().getBlockForTile(first[0], first[1]);
        if (block != null) {
            block.addAssignedTrain(trainId);
            previousBlock = block;
        }
    }

    private void clearBlockAssignment(Block block) {
        block.removeAssignedTrain(trainId);
        notifyTick();
        LOG.info("Cleared train {} assignment from block '{}'", trainId, block.getName());
    }

    private void recordBlockDeparture(Block block) {
        if (!blockDepartures.containsKey(block)) {
            blockDepartures.put(block, System.currentTimeMillis());
            LOG.info("Recorded departure from block '{}'", block.getName());
        }
    }

    private void checkBlockCleanups() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Block, Long>> it = blockDepartures.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Block, Long> entry = it.next();
            long elapsed = now - entry.getValue();
            if (elapsed >= BLOCK_MARKER_CLEAR_DELAY_MS) {
                Block block = entry.getKey();
                it.remove();
                // Release gap tiles belonging to this block
                reservedGapTiles.values().removeIf(v -> v.equals(block.getId()));
                LOG.info("Block '{}' departure delay elapsed ({}ms), clearing assignment", block.getName(), elapsed);
                if (block.isAssignedTo(trainId)) {
                    clearBlockAssignment(block);
                } else {
                    LOG.info("Block '{}' has no assignment from train {}, skipping", block.getName(), trainId);
                }
            }
        }
    }

    /**
     * Finds the block that a signal at the given path index is guarding.
     * Walks forward from the signal along the route path to find the first tile
     * belonging to a different block than the signal's own block.
     */
    private Block findGuardedBlock(int signalPathIndex) {
        Tile signalTile = tileGrid.getTile(path.get(signalPathIndex)[0], path.get(signalPathIndex)[1]);
        Block signalBlock = tileGrid.getBlockModel().getBlockForTile(
            path.get(signalPathIndex)[0], path.get(signalPathIndex)[1]);
        for (int i = signalPathIndex + 1; i < path.size(); i++) {
            Block block = tileGrid.getBlockModel().getBlockForTile(path.get(i)[0], path.get(i)[1]);
            if (block != null && block != signalBlock) {
                return block;
            }
        }
        return null;
    }

    /**
     * Finds non-block tiles (turnouts, straights) between the signal at {@code fromIndex}
     * and the first tile of {@code targetBlock}. These are the gap tiles that need to be
     * reserved alongside the guarded block.
     */
    private Set<int[]> findGapTiles(int fromIndex, Block targetBlock) {
        Set<int[]> gapTiles = new HashSet<>();
        for (int i = fromIndex + 1; i < path.size(); i++) {
            Block block = tileGrid.getBlockModel().getBlockForTile(path.get(i)[0], path.get(i)[1]);
            if (block == targetBlock) {
                break; // reached the target block — stop
            }
            if (block == null) {
                gapTiles.add(path.get(i));
            }
        }
        return gapTiles;
    }

    private void reserveGapTiles(Set<int[]> gapTiles, String blockId) {
        for (int[] coord : gapTiles) {
            reservedGapTiles.put(coord[0] + "," + coord[1], blockId);
        }
    }

    public Map<String, String> getReservedGapTiles() {
        return Collections.unmodifiableMap(reservedGapTiles);
    }

    /**
     * Sets turnout aspects only on gap tiles this train has reserved.
     * Does not touch turnouts owned by other trains.
     */
    private void setReservedGapTileAspects() {
        if (routerService == null || path == null) return;
        for (int i = 0; i < path.size(); i++) {
            int[] coord = path.get(i);
            String key = coord[0] + "," + coord[1];
            if (!reservedGapTiles.containsKey(key)) {
                continue; // not a reserved gap tile — skip
            }
            Tile tile = tileGrid.getTile(coord[0], coord[1]);
            if (!(tile instanceof ElementTile et) || et.getElementId() == null) continue;
            ElementType type = et.getElementType();
            if (type.getAspectCount() <= 1) continue;
            int[] prev = i > 0 ? path.get(i - 1) : null;
            int[] next = i < path.size() - 1 ? path.get(i + 1) : null;
            routerService.setRouteAspectForTile(coord[0], coord[1], prev, next, model);
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

    /**
     * Keeps every distant signal (SIGNAL_V) on the route in sync with its linked main signal.
     * Each distant signal has a configured {@code mainSignalId} on its SignalTile that identifies
     * which main signal it previews. For combined signals (SIGNAL_COMBINED), the DISTANT PLATE
     * on the signal's own mast mirrors the linked main signal.
     */
    private void syncDistantSignals() {
        if (path == null) {
            return;
        }
        for (int[] p : path) {
            Tile tile = tileGrid.getTile(p[0], p[1]);
            if (!(tile instanceof SignalTile st)) {
                continue;
            }
            if (st.getElementType() == ElementType.SIGNAL_COMBINED) {
                syncCombinedPlate(st);
                continue;
            }
            if (st.getElementType() != ElementType.SIGNAL_V || st.getElementId() == null) {
                continue;
            }
            String mainId = st.getMainSignalId();
            if (mainId == null) {
                continue;
            }
            Element mainEl = model.getElement(mainId);
            if (mainEl != null) {
                int distantAspect = OccupancySimulation.distantAspectForMainSignal(
                    ElementType.SIGNAL_M3, mainEl.getCurrentAspect());
                model.setElementAspect(st.getElementId(), distantAspect);
            }
        }
    }

    private void syncCombinedPlate(SignalTile combined) {
        String mainId = combined.getMainSignalId();
        if (mainId == null) {
            return;
        }
        Element mainEl = model.getElement(mainId);
        if (mainEl != null) {
            combined.setPlateAspect(mainEl.getCurrentAspect());
        }
    }
}
