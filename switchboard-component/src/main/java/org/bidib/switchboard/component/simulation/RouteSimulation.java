package org.bidib.switchboard.component.simulation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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

    private static final int DEFAULT_TRAIN_LENGTH = 3;
    private static final int DEFAULT_TICK_INTERVAL_MS = 200;
    private static final int BLOCK_CLEANUP_EVERY_N_TICKS = 5;

    private final RailwayModel model;
    private final TileGrid tileGrid;
    private final OccupancyFactory occupancyFactory;
    private RouterService routerService;
    private int trainLength = DEFAULT_TRAIN_LENGTH;
    private int tickIntervalMs = DEFAULT_TICK_INTERVAL_MS;

    private RouteService routeService;

    private Route route;
    private String trainId;
    private List<int[]> path;
    private Set<Integer> stops;
    private int currentIndex;
    private boolean running;
    private boolean finished;
    private long startedAt;
    private Runnable onTick;
    private Runnable onComplete;
    private Runnable tickSource;
    private TrainMovement trainMovement;
    private Timer simulationTimer;
    private int tickCount;

    public RouteSimulation(RailwayModel model, TileGrid tileGrid, OccupancyFactory occupancyFactory,
            RouterService routerService, int trainLength) {
        this.model = model;
        this.tileGrid = tileGrid;
        this.occupancyFactory = occupancyFactory;
        this.routerService = routerService;
        this.trainLength = Math.max(1, trainLength);
        this.trainMovement = new TrainMovement(this.trainLength);
    }

    public void setRouteService(RouteService routeService) {
        this.routeService = routeService;
    }

    public RouteService getRouteService() {
        return routeService;
    }

    public void setRouterService(RouterService routerService) {
        this.routerService = routerService;
    }

    public void setOtherTrainGapTileCheck(Predicate<String> check) {
        if (routeService != null) {
            routeService.setOtherTrainGapTileCheck(check);
        }
    }

    public void setTrainLength(int trainLength) {
        this.trainLength = Math.max(1, trainLength);
        this.trainMovement = new TrainMovement(this.trainLength);
    }

    public int getTrainLength() {
        return trainLength;
    }

    public void setSignalResetDistance(int distance) {
        if (routeService != null) {
            routeService.setSignalResetDistance(Math.max(1, distance));
        }
    }

    public int getSignalResetDistance() {
        return routeService != null ? 5 : 5;
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
        if (routeService != null) {
            routeService.setAutoChangeSignal(autoChangeSignal);
        }
    }

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
        this.startedAt = System.currentTimeMillis();
        this.finished = false;
        this.tickCount = 0;

        if (path.isEmpty()) {
            running = false;
            return;
        }

        // Delegate signal/block/gap setup to RouteService
        if (routeService != null) {
            routeService.onStart(path, trainId, startIndex);
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

        // Assign train to first block (sets previousBlock in RouteService)
        if (routeService != null) {
            routeService.assignTrainToFirstBlock(path, trainId);
        }

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
        stopSimulationTimer();
        if (routeService != null) {
            routeService.onStop();
        }
        notifyTick();
    }

    public void reset() {
        running = false;
        finished = false;
        stopSimulationTimer();
        if (routeService != null) {
            routeService.onReset();
        }
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
                LOG.info("Route '{}' completed",
                    route.getName() != null ? route.getName() : route.getId());
                stopSimulationTimer();
            }
            notifyTick();
            if (finished && onComplete != null) {
                onComplete.run();
            }
            return;
        }

        // Delegate pre-movement decisions to RouteService
        if (routeService != null) {
            RouteService.TickDecision decision = routeService.onTickPreMovement(
                currentIndex, path, trainId, startedAt, stops);
            if (decision.shouldReturn()) {
                notifyTick();
                return;
            }
        }

        // --- Train movement ---
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

        // Post-movement: block tracking + signal reset + station detection
        if (routeService != null) {
            routeService.onTickPostMovement(currentIndex, path, trainId, currentBlock, stops, route);
        }

        currentIndex++;
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
        if (tickCount % BLOCK_CLEANUP_EVERY_N_TICKS == 0 && routeService != null) {
            routeService.checkBlockCleanups(trainId);
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
        return routeService != null && routeService.isPausedAtStation();
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

    public java.util.Map<String, String> getReservedGapTiles() {
        return routeService != null ? routeService.getReservedGapTiles() : java.util.Collections.emptyMap();
    }
}
