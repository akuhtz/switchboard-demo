package org.bidib.switchboard.component.simulation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.bidib.switchboard.component.model.Block;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.SignalTile;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.service.RouterService;
import org.bidib.switchboard.component.view.TileGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteService {

    private static final Logger LOG = LoggerFactory.getLogger(RouteService.class);

    private static final long AUTO_CHANGE_DELAY_MS = 2000;
    private static final long BLOCK_MARKER_CLEAR_DELAY_MS = 2000;
    private static final int DEFAULT_SIGNAL_RESET_DISTANCE = 5;

    private final RailwayModel model;
    private final TileGrid tileGrid;
    private final RouterService routerService;

    private int signalResetDistance = DEFAULT_SIGNAL_RESET_DISTANCE;
    private boolean autoChangeSignal;

    // Signal state
    private boolean startSignalSet;
    private boolean startGreenSet;
    private long signalBlockedSince = -1;
    private int lastGreenSignalIndex = -1;

    // Block reservation state
    private Block previousBlock;
    private final Map<Block, Long> blockDepartures = new HashMap<>();

    // Gap tile state
    private final Map<String, String> reservedGapTiles = new HashMap<>();
    private Predicate<String> otherTrainGapTileCheck;

    // Dwell state
    private boolean pausedAtStation;
    private long stationPausedSince = -1;
    private long stationReservationSince = -1;
    private int dwellTimeMs = 5000;

    // Current train ID (set during onStart)
    private String trainId;

    public record TickDecision(boolean blocked, boolean paused) {
        public static TickDecision proceed() {
            return new TickDecision(false, false);
        }

        public static TickDecision ofBlocked() {
            return new TickDecision(true, false);
        }

        public static TickDecision ofPaused() {
            return new TickDecision(false, true);
        }

        public boolean shouldReturn() {
            return blocked || paused;
        }
    }

    public RouteService(RailwayModel model, TileGrid tileGrid, RouterService routerService) {
        this.model = model;
        this.tileGrid = tileGrid;
        this.routerService = routerService;
    }

    public void setAutoChangeSignal(boolean autoChangeSignal) {
        this.autoChangeSignal = autoChangeSignal;
    }

    public void setSignalResetDistance(int signalResetDistance) {
        this.signalResetDistance = signalResetDistance;
    }

    public void setOtherTrainGapTileCheck(Predicate<String> check) {
        this.otherTrainGapTileCheck = check;
    }

    public boolean isPausedAtStation() {
        return pausedAtStation;
    }

    public Map<String, String> getReservedGapTiles() {
        return Collections.unmodifiableMap(reservedGapTiles);
    }

    /**
     * Called once when the simulation starts. Resets signal/block/gap state,
     * checks the guard block, sets gap tile aspects, and resets signals to red.
     */
    public void onStart(List<int[]> path, String trainId, int startIndex) {
        this.trainId = trainId;
        this.startSignalSet = false;
        this.startGreenSet = false;
        this.lastGreenSignalIndex = -1;
        this.previousBlock = null;
        this.pausedAtStation = false;
        this.stationPausedSince = -1;
        this.stationReservationSince = -1;
        this.reservedGapTiles.clear();
        this.blockDepartures.clear();

        // Check if the guard block is reserved before configuring turnout aspects
        Block nextBlock = findGuardedBlock(0, path);
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

        if (canReserve && routerService != null) {
            setReservedGapTileAspects(path, startIndex, false);
        }

        // Reset all signals on the route to red
        for (int[] p : path) {
            Tile tile = tileGrid.getTile(p[0], p[1]);
            if (tile instanceof ElementTile et && et.getElementId() != null) {
                ElementType type = et.getElementType();
                if (type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED
                    || type == ElementType.SIGNAL_V) {
                    if (model.getElement(et.getElementId()).getCurrentAspect() != 0) {
                        model.setElementAspect(et.getElementId(), 0);
                    }
                }
            }
        }
        syncDistantSignals(path);
    }

    /**
     * Called each tick before train movement. Handles start delay, start signal green,
     * station dwell, and signal blocking. Returns a decision whether to proceed with
     * movement or wait.
     */
    public TickDecision onTickPreMovement(int currentIndex, List<int[]> path, String trainId,
            long startedAt, Set<Integer> stops, boolean autoChangeSignal) {
        this.autoChangeSignal = autoChangeSignal;

        // Wait 2s after start for turnouts to switch before moving
        if (!startSignalSet && System.currentTimeMillis() - startedAt < AUTO_CHANGE_DELAY_MS) {
            return TickDecision.ofPaused();
        }
        startSignalSet = true;

        // Set start signal to green after the 2s delay (only once)
        if (!startGreenSet && currentIndex >= 1) {
            Block nextBlock = findGuardedBlock(0, path);
            if (nextBlock != null) {
                String reservedBy = nextBlock.getAssignedTrainIds().stream()
                    .filter(id -> !id.equals(trainId)).findFirst().orElse(null);
                if (reservedBy != null) {
                    LOG.info("Start signal stays red — block '{}' reserved by train {}",
                        nextBlock.getName(), reservedBy);
                    return TickDecision.ofPaused();
                }
            }
            startGreenSet = true;
            if (routerService != null) {
                setReservedGapTileAspects(path, 0, true);
            }
            int[] startPos = path.get(0);
            Tile startTile = tileGrid.getTile(startPos[0], startPos[1]);
            if (startTile instanceof ElementTile et && et.getElementId() != null) {
                ElementType type = et.getElementType();
                if (type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED) {
                    model.setElementAspect(et.getElementId(), 1);
                    lastGreenSignalIndex = 0;
                    syncDistantSignals(path);
                    LOG.info("Set start signal {} to green after delay", et.getElementId());
                }
            }
        }

        // Handle station stop dwell
        if (pausedAtStation) {
            TickDecision dwellDecision = handleStationDwell(currentIndex, path, trainId, stops);
            if (dwellDecision.shouldReturn()) {
                return dwellDecision;
            }
        }

        // Check if the previous tile is a signal blocking the train
        if (isBlockedAtSignal(currentIndex, path, trainId, stops)) {
            return TickDecision.ofBlocked();
        }

        signalBlockedSince = -1;
        return TickDecision.proceed();
    }

    /**
     * Returns true if the train is blocked at a signal on the previous tile.
     */
    public boolean isBlockedAtSignal(int currentIndex, List<int[]> path, String trainId,
            Set<Integer> stops) {
        int prev = currentIndex - 1;
        if (prev <= 0) {
            return false;
        }
        Tile pt = tileGrid.getTile(path.get(prev)[0], path.get(prev)[1]);
        int[] pp = path.get(prev - 1);
        int dc = path.get(prev)[0] - pp[0];
        int dr = path.get(prev)[1] - pp[1];
        int entryPort = OccupancySimulation.portFromDelta(dc, dr);
        if (!OccupancySimulation.isSignalBlocking(pt, entryPort, model)) {
            return false;
        }

        Block guardBlock = findGuardedBlock(prev, path);
        if (guardBlock != null) {
            // Check gap tiles reserved by another train
            Set<int[]> gapTiles = findGapTiles(prev, guardBlock, path);
            for (int[] coord : gapTiles) {
                String gapBlockId = reservedGapTiles.get(coord[0] + "," + coord[1]);
                if (gapBlockId != null && !gapBlockId.equals(guardBlock.getId())) {
                    LOG.info("Signal at ({},{}) stays red — gap tile ({},{}) reserved by block {}",
                        path.get(prev)[0], path.get(prev)[1], coord[0], coord[1], gapBlockId);
                    return true;
                }
                if (otherTrainGapTileCheck != null
                        && otherTrainGapTileCheck.test(coord[0] + "," + coord[1])) {
                    LOG.info("Signal at ({},{}) stays red — gap tile ({},{}) reserved by another train",
                        path.get(prev)[0], path.get(prev)[1], coord[0], coord[1]);
                    return true;
                }
            }

            String reservedBy = guardBlock.getAssignedTrainIds().stream()
                .filter(id -> !id.equals(trainId)).findFirst().orElse(null);
            if (reservedBy != null) {
                LOG.info("Signal at ({},{}) stays red — block '{}' reserved by train {}",
                    path.get(prev)[0], path.get(prev)[1], guardBlock.getName(), reservedBy);
                return true;
            }

            // Block is free — reserve it and set signal to green
            if (!guardBlock.isReserved()) {
                guardBlock.addAssignedTrain(trainId);
                reserveGapTiles(gapTiles, guardBlock.getId());
                if (routerService != null) {
                    setReservedGapTileAspects(path, prev, false);
                }
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
                    Tile sigTile = tileGrid.getTile(path.get(prev)[0], path.get(prev)[1]);
                    if (sigTile instanceof ElementTile et && et.getElementId() != null) {
                        model.setElementAspect(et.getElementId(), 1);
                        lastGreenSignalIndex = prev;
                        LOG.info("Set signal {} to green", et.getElementId());
                    }
                    signalBlockedSince = -1;
                    syncDistantSignals(path);
                }
            }
        }

        return true;
    }

    /**
     * Clears the signal-blocked timestamp. Called by RouteSimulation when
     * the train is not blocked at a signal.
     */
    public void clearSignalBlocked() {
        signalBlockedSince = -1;
    }

    /**
     * Called after train movement. Handles signal reset to red, station stop detection,
     * and block departure tracking.
     */
    public void onTickPostMovement(int currentIndex, List<int[]> path, String trainId,
            Block currentBlock, Set<Integer> stops, Route route) {
        // Track block departure
        if (previousBlock != null && previousBlock != currentBlock) {
            recordBlockDeparture(previousBlock);
        }
        if (currentBlock != null) {
            previousBlock = currentBlock;
        }

        // Reset signal to red when head is past the signal by the configured distance
        if (lastGreenSignalIndex >= 0 && currentIndex >= lastGreenSignalIndex + signalResetDistance) {
            int[] sigPos = path.get(lastGreenSignalIndex);
            Tile sigTile = tileGrid.getTile(sigPos[0], sigPos[1]);
            if (sigTile instanceof ElementTile et && et.getElementId() != null) {
                model.setElementAspect(et.getElementId(), 0);
                LOG.info("Reset signal {} to red after train passed", et.getElementId());
            }
            lastGreenSignalIndex = -1;
            syncDistantSignals(path);
        }

        // Check if we just arrived at a station stop
        if (stops.contains(currentIndex)) {
            Route.StationStop stop = route.getStopAt(currentIndex);
            if (stop != null) {
                dwellTimeMs = stop.getDwellTimeMs();
                pausedAtStation = true;
                stationPausedSince = System.currentTimeMillis();
                LOG.info("Stopped at station (index {}), dwell {}ms", currentIndex, dwellTimeMs);
            }
        }
    }

    /**
     * Called when the simulation stops. Records block departure and clears gap tiles.
     */
    public void onStop() {
        pausedAtStation = false;
        stationPausedSince = -1;
        stationReservationSince = -1;
        reservedGapTiles.clear();
        if (previousBlock != null) {
            recordBlockDeparture(previousBlock);
        }
        if (trainId != null) {
            checkBlockCleanups(trainId);
        }
    }

    /**
     * Called when the simulation is reset. Clears all state.
     */
    public void onReset() {
        pausedAtStation = false;
        stationPausedSince = -1;
        stationReservationSince = -1;
        startSignalSet = false;
        startGreenSet = false;
        signalBlockedSince = -1;
        lastGreenSignalIndex = -1;
        previousBlock = null;
        blockDepartures.clear();
        reservedGapTiles.clear();
    }

    /**
     * Assigns the train to the first block on the path and sets previousBlock.
     * Called once after occupancy setup during start().
     */
    public void assignTrainToFirstBlock(List<int[]> path, String trainId) {
        if (path.isEmpty()) {
            return;
        }
        int[] first = path.get(0);
        Block block = tileGrid.getBlockModel().getBlockForTile(first[0], first[1]);
        if (block != null) {
            block.addAssignedTrain(trainId);
            previousBlock = block;
        }
    }

    /**
     * Called by the tick timer to clean up block markers after departure delay.
     */
    public void checkBlockCleanups(String trainId) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Block, Long>> it = blockDepartures.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Block, Long> entry = it.next();
            long elapsed = now - entry.getValue();
            if (elapsed >= BLOCK_MARKER_CLEAR_DELAY_MS) {
                Block block = entry.getKey();
                it.remove();
                reservedGapTiles.values().removeIf(v -> v.equals(block.getId()));
                LOG.info("Block '{}' departure delay elapsed ({}ms), clearing assignment",
                    block.getName(), elapsed);
                if (block.isAssignedTo(trainId)) {
                    block.removeAssignedTrain(trainId);
                    LOG.info("Cleared train {} assignment from block '{}'", trainId, block.getName());
                } else {
                    LOG.info("Block '{}' has no assignment from train {}, skipping",
                        block.getName(), trainId);
                }
            }
        }
    }

    // --- Private helpers ---

    private TickDecision handleStationDwell(int currentIndex, List<int[]> path, String trainId,
            Set<Integer> stops) {
        long elapsed = System.currentTimeMillis() - stationPausedSince;
        if (elapsed < dwellTimeMs) {
            return TickDecision.ofPaused();
        }

        int stationIndex = currentIndex - 1;

        // Reset the station signal to red
        if (stationIndex >= 0 && stationIndex < path.size()) {
            Tile stationTile = tileGrid.getTile(path.get(stationIndex)[0], path.get(stationIndex)[1]);
            if (stationTile instanceof ElementTile et && et.getElementId() != null) {
                ElementType type = et.getElementType();
                if ((type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED)
                    && model.getElement(et.getElementId()).getCurrentAspect() != 0) {
                    model.setElementAspect(et.getElementId(), 0);
                    lastGreenSignalIndex = -1;
                    syncDistantSignals(path);
                    LOG.info("Reset station signal {} to red after dwell", et.getElementId());
                }
            }
        }

        // Reserve the guard block if needed
        if (stationIndex >= 0 && stationIndex < path.size()) {
            Block guardBlock = findGuardedBlock(stationIndex, path);
            if (guardBlock != null) {
                String reservedBy = guardBlock.getAssignedTrainIds().stream()
                    .filter(id -> !id.equals(trainId)).findFirst().orElse(null);
                if (reservedBy != null) {
                    pausedAtStation = true;
                    stationPausedSince = System.currentTimeMillis() - dwellTimeMs;
                    LOG.info("Station dwell complete but block '{}' reserved by train {}, waiting",
                        guardBlock.getName(), reservedBy);
                    return TickDecision.ofPaused();
                }
                Set<int[]> gapTiles = findGapTiles(stationIndex, guardBlock, path);
                for (int[] coord : gapTiles) {
                    String blockId = reservedGapTiles.get(coord[0] + "," + coord[1]);
                    if (blockId != null && !blockId.equals(guardBlock.getId())) {
                        pausedAtStation = true;
                        stationPausedSince = System.currentTimeMillis() - dwellTimeMs;
                        LOG.info(
                            "Station dwell complete but gap tile ({},{}) reserved by another block, waiting",
                            coord[0], coord[1]);
                        return TickDecision.ofPaused();
                    }
                    if (otherTrainGapTileCheck != null
                            && otherTrainGapTileCheck.test(coord[0] + "," + coord[1])) {
                        pausedAtStation = true;
                        stationPausedSince = System.currentTimeMillis() - dwellTimeMs;
                        LOG.info(
                            "Station dwell complete but gap tile ({},{}) reserved by another train, waiting",
                            coord[0], coord[1]);
                        return TickDecision.ofPaused();
                    }
                }
                if (!guardBlock.isReserved()) {
                    guardBlock.addAssignedTrain(trainId);
                    reserveGapTiles(gapTiles, guardBlock.getId());
                    if (routerService != null) {
                        setReservedGapTileAspects(path, stationIndex, false);
                    }
                    LOG.info("Reserved block '{}' for train {} after station dwell",
                        guardBlock.getName(), trainId);
                }
            }
        }

        // Wait 2s after block reservation before setting signal to green
        if (stationReservationSince < 0) {
            stationReservationSince = System.currentTimeMillis();
            return TickDecision.ofPaused();
        }
        if (System.currentTimeMillis() - stationReservationSince < AUTO_CHANGE_DELAY_MS) {
            return TickDecision.ofPaused();
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
        syncDistantSignals(path);
        pausedAtStation = false;
        stationPausedSince = -1;

        LOG.info("Station dwell complete, signal set to green");
        return TickDecision.proceed();
    }

    private Block findGuardedBlock(int signalPathIndex, List<int[]> path) {
        Tile signalTile = tileGrid.getTile(
            path.get(signalPathIndex)[0], path.get(signalPathIndex)[1]);
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

    private Set<int[]> findGapTiles(int fromIndex, Block targetBlock, List<int[]> path) {
        Set<int[]> gapTiles = new HashSet<>();
        for (int i = fromIndex + 1; i < path.size(); i++) {
            Block block = tileGrid.getBlockModel().getBlockForTile(path.get(i)[0], path.get(i)[1]);
            if (targetBlock != null && block == targetBlock) {
                break;
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

    private void setReservedGapTileAspects(List<int[]> path, int currentIndex, boolean entirePath) {
        if (routerService == null || path == null) {
            return;
        }
        Set<int[]> gapTiles;
        if (entirePath) {
            gapTiles = findGapTiles(-1, null, path);
        } else {
            int fromIndex = Math.max(0, currentIndex - 1);
            Block guardBlock = findGuardedBlock(fromIndex, path);
            if (guardBlock == null) {
                return;
            }
            gapTiles = findGapTiles(fromIndex, guardBlock, path);
        }
        for (int[] coord : gapTiles) {
            String key = coord[0] + "," + coord[1];
            if (otherTrainGapTileCheck != null && otherTrainGapTileCheck.test(key)) {
                LOG.info("Skipping gap tile ({},{}) — already reserved by another train",
                    coord[0], coord[1]);
                continue;
            }
            for (int i = 0; i < path.size(); i++) {
                if (path.get(i)[0] == coord[0] && path.get(i)[1] == coord[1]) {
                    int[] prev = i > 0 ? path.get(i - 1) : null;
                    int[] next = i < path.size() - 1 ? path.get(i + 1) : null;
                    routerService.setRouteAspectForTile(coord[0], coord[1], prev, next, model);
                    break;
                }
            }
        }
    }

    private void recordBlockDeparture(Block block) {
        if (!blockDepartures.containsKey(block)) {
            blockDepartures.put(block, System.currentTimeMillis());
            LOG.info("Recorded departure from block '{}'", block.getName());
        }
    }

    private void syncDistantSignals(List<int[]> path) {
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
