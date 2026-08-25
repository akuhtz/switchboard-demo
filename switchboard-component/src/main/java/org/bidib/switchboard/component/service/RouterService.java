package org.bidib.switchboard.component.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.bidib.switchboard.component.model.Block;
import org.bidib.switchboard.component.model.BlockModel;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.RouteModel;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.model.TileDirection;
import org.bidib.switchboard.component.view.TileGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouterService {

    private static final Logger LOG = LoggerFactory.getLogger(RouterService.class);

    public static final int MAX_ALTERNATIVES = 10;

    private boolean exhaustiveRouting = false;

    private static final int REVISIT_CAP = 8;

    public boolean isExhaustiveRouting() {
        return exhaustiveRouting;
    }

    public void setExhaustiveRouting(boolean exhaustiveRouting) {
        this.exhaustiveRouting = exhaustiveRouting;
    }

    private final Map<String, Tile> tiles;

    private final int cols;

    private final int rows;

    private final RouteModel routeModel;

    public RouterService(Map<String, Tile> tiles, int cols, int rows, RouteModel routeModel) {
        this.tiles = tiles;
        this.cols = cols;
        this.rows = rows;
        this.routeModel = routeModel;
    }

    public static RouterService createDefault() {
        return new RouterService(new java.util.LinkedHashMap<>(), 60, 30, new RouteModel());
    }

    public Map<String, Tile> getTiles() {
        return tiles;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public RouteModel getRouteModel() {
        return routeModel;
    }

    public Map<String, Route> getRoutes() {
        return routeModel.getRoutes();
    }

    public List<int[]> bfsRoute(int startCol, int startRow, int endCol, int endRow) {
        String startKey = Tile.key(startCol, startRow);
        String endKey = Tile.key(endCol, endRow);
        if (!tiles.containsKey(startKey) || !tiles.containsKey(endKey)) {
            return null;
        }
        List<int[]> result = bfsRouteInternal(startCol, startRow, endCol, endRow, new HashSet<>(), false);
        if (result == null) {
            result = bfsRouteInternal(startCol, startRow, endCol, endRow, new HashSet<>(), true);
        }
        return result;
    }

    /**
     * Finds a connected path from start to end for a block. Unlike route
     * finding, blocks never pass through turnout tiles. Tiles belonging to
     * other blocks (passed via {@code excludedTiles}) are also avoided.
     * Returns null if no such path exists.
     */
    public List<int[]> bfsBlockPath(int startCol, int startRow, int endCol, int endRow, Set<String> excludedTiles) {
        String startKey = Tile.key(startCol, startRow);
        String endKey = Tile.key(endCol, endRow);
        if (!tiles.containsKey(startKey) || !tiles.containsKey(endKey)) {
            return null;
        }

        Deque<int[]> queue = new ArrayDeque<>();
        Map<String, int[]> cameFrom = new HashMap<>();
        Set<String> visited = new HashSet<>();

        queue.add(new int[] { startCol, startRow });
        visited.add(startKey);
        cameFrom.put(startKey, null);

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int col = current[0];
            int row = current[1];
            String cKey = Tile.key(col, row);

            if (col == endCol && row == endRow) {
                return reconstructPath(endKey, cameFrom);
            }

            List<int[]> connected = getConnectedNeighbors(col, row);
            for (int[] neighbor : connected) {
                int neighborCol = neighbor[0];
                int neighborRow = neighbor[1];
                String neighborKey = Tile.key(neighborCol, neighborRow);

                if (visited.contains(neighborKey)) {
                    continue;
                }
                if (!tiles.containsKey(neighborKey)) {
                    continue;
                }
                if (excludedTiles != null && excludedTiles.contains(neighborKey)) {
                    continue;
                }
                Tile neighborTile = getTile(neighborCol, neighborRow);
                if (neighborTile instanceof ElementTile et && isTurnout(et.getElementType())) {
                    continue;
                }

                visited.add(neighborKey);
                cameFrom.put(neighborKey, new int[] { col, row });
                queue.add(new int[] { neighborCol, neighborRow });
            }
        }

        return null;
    }

    private static boolean isTurnout(ElementType type) {
        return type == ElementType.TURNOUT_LEFT || type == ElementType.TURNOUT_RIGHT || type == ElementType.TURNOUT_3WAY;
    }

    private static boolean isTurnoutOrDiagonalTurnout(ElementType type) {
        return isTurnout(type)
            || type == ElementType.DIAGONAL_TURNOUT_RIGHT
            || type == ElementType.DIAGONAL_TURNOUT_LEFT;
    }

    /**
     * Returns {@code true} if block A and block B are physically adjacent,
     * i.e. an endpoint of A connects to an endpoint of B through tiles that
     * do not belong to any block.
     */
    public boolean areBlocksAdjacent(Block a, Block b) {
        return areBlocksAdjacent(a, b, null);
    }

    /**
     * Returns {@code true} if block A and block B are physically adjacent,
     * i.e. an endpoint of A connects to an endpoint of B through tiles that
     * do not belong to any block. When a {@code blockModel} is provided, the
     * BFS traverses any tile not assigned to a block (turnouts, short gaps of
     * straight track, etc.). Without a block model, only turnout tiles are
     * traversed.
     */
    public boolean areBlocksAdjacent(Block a, Block b, BlockModel blockModel) {
        Set<String> bEndpoints = new HashSet<>();
        List<int[]> bPath = b.getPath();
        bEndpoints.add(Tile.key(bPath.get(0)[0], bPath.get(0)[1]));
        bEndpoints.add(Tile.key(bPath.get(bPath.size() - 1)[0], bPath.get(bPath.size() - 1)[1]));

        List<int[]> aPath = a.getPath();
        int[][] aEndpoints = { aPath.get(0), aPath.get(aPath.size() - 1) };

        for (int[] endpoint : aEndpoints) {
            if (bfsToBlock(endpoint[0], endpoint[1], bEndpoints, blockModel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * BFS from the neighbors of (startCol, startRow) through tiles not belonging
     * to any block. Returns true if a target endpoint tile is reached.
     */
    private boolean bfsToBlock(int startCol, int startRow, Set<String> targets, BlockModel blockModel) {
        Deque<int[]> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        visited.add(Tile.key(startCol, startRow)); // don't revisit start

        for (int[] neighbor : getConnectedNeighbors(startCol, startRow)) {
            String key = Tile.key(neighbor[0], neighbor[1]);
            if (targets.contains(key)) {
                return true;
            }
            if (canTraverse(neighbor[0], neighbor[1], blockModel)) {
                if (visited.add(key)) {
                    queue.add(neighbor);
                }
            }
        }

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            for (int[] neighbor : getConnectedNeighbors(current[0], current[1])) {
                String key = Tile.key(neighbor[0], neighbor[1]);
                if (targets.contains(key)) {
                    return true;
                }
                if (visited.contains(key)) {
                    continue;
                }
                if (canTraverse(neighbor[0], neighbor[1], blockModel)) {
                    visited.add(key);
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    /**
     * A tile can be traversed during block adjacency BFS if it does not belong
     * to any block. When no block model is available, falls back to turnout-only
     * traversal.
     */
    private boolean canTraverse(int col, int row, BlockModel blockModel) {
        if (blockModel != null) {
            return blockModel.blockIdForTile(col, row) == null;
        }
        // Fallback: only traverse turnout tiles
        Tile t = getTile(col, row);
        return t instanceof ElementTile et && isTurnoutOrDiagonalTurnout(et.getElementType());
    }

    public List<List<int[]>> bfsAlternativeRoutes(int startCol, int startRow, int endCol, int endRow, List<int[]> primaryPath) {
        return bfsAlternativeRoutesInternal(startCol, startRow, endCol, endRow, primaryPath);
    }

    private List<List<int[]>> bfsAlternativeRoutesInternal(int startCol, int startRow, int endCol, int endRow, List<int[]> primaryPath) {
        String startKey = Tile.key(startCol, startRow);
        String endKey = Tile.key(endCol, endRow);
        if (!tiles.containsKey(startKey) || !tiles.containsKey(endKey) || primaryPath == null || primaryPath.size() < 2) {
            return List.of();
        }

        List<List<int[]>> alts = new ArrayList<>();

        for (int i = 0; i < primaryPath.size() - 1 && alts.size() < MAX_ALTERNATIVES; i++) {
            int[] from = primaryPath.get(i);
            int[] to = primaryPath.get(i + 1);
            List<int[]> neighbors = getConnectedNeighbors(from[0], from[1]);
            boolean hasAlt = false;
            for (int[] n : neighbors) {
                if (n[0] == to[0] && n[1] == to[1]) continue;
                if (i > 0) {
                    int[] prev = primaryPath.get(i - 1);
                    int entryDir1 = from[0] - prev[0];
                    int entryDir2 = from[1] - prev[1];
                    int ne1 = -1, ne2 = -1;
                    if (entryDir1 == 1) ne1 = ElementType.PORT_LEFT;
                    else if (entryDir1 == -1) ne1 = ElementType.PORT_RIGHT;
                    if (entryDir2 == 1) ne2 = ElementType.PORT_TOP;
                    else if (entryDir2 == -1) ne2 = ElementType.PORT_BOTTOM;
                    int ndc = n[0] - from[0];
                    int ndr = n[1] - from[1];
                    int exit1 = -1, exit2 = -1;
                    if (ndc == 1) exit1 = ElementType.PORT_RIGHT;
                    else if (ndc == -1) exit1 = ElementType.PORT_LEFT;
                    if (ndr == 1) exit2 = ElementType.PORT_BOTTOM;
                    else if (ndr == -1) exit2 = ElementType.PORT_TOP;
                    if ((ne1 != -1 && (exit1 == ne1 || exit2 == ne1))
                        || (ne2 != -1 && (exit1 == ne2 || exit2 == ne2))) {
                        continue;
                    }
                }
                hasAlt = true;
                break;
            }
            if (hasAlt) {
                Set<String> block = new HashSet<>();
                block.add(edgeKey(from[0], from[1], to[0], to[1]));
                List<int[]> result = bfsRouteInternal(startCol, startRow, endCol, endRow, block, false);
                if (result != null) {
                    if (!isDuplicatePath(alts, result)) {
                        alts.add(result);
                        if (exhaustiveRouting) {
                            findAdditionalAlternatives(startCol, startRow, endCol, endRow, block, result, alts);
                        }
                    }
                }
            }
        }

        return alts;
    }

    public void setRouteAspects(List<int[]> path, RailwayModel model) {
        for (int i = 0; i < path.size(); i++) {
            int[] curr = path.get(i);
            Tile tile = getTile(curr[0], curr[1]);
            if (!(tile instanceof ElementTile et)) continue;
            ElementType type = et.getElementType();
            if (type.getAspectCount() <= 1) continue;
            String id = et.getElementId();
            if (id == null) continue;
            int aspect;
            if (i == 0) {
                int[] next = path.get(i + 1);
                aspect = type.aspectForPort(diagonalAwarePort(curr[0], curr[1], next[0], next[1], false), tile.getRotation());
            } else if (i == path.size() - 1) {
                int[] prev = path.get(i - 1);
                aspect = type.aspectForPort(diagonalAwarePort(prev[0], prev[1], curr[0], curr[1], true), tile.getRotation());
            } else {
                int[] prev = path.get(i - 1);
                int[] next = path.get(i + 1);
                int entryPort = diagonalAwarePort(prev[0], prev[1], curr[0], curr[1], true);
                int exitPort = diagonalAwarePort(curr[0], curr[1], next[0], next[1], false);

                int dc = next[0] - curr[0];
                int dr = next[1] - curr[1];
                boolean diagonalTurnout = type == ElementType.DIAGONAL_TURNOUT_RIGHT
                    || type == ElementType.DIAGONAL_TURNOUT_LEFT;
                if (dc != 0 && dr != 0 && !diagonalTurnout) {
                    boolean entryIsHorizontal = entryPort == ElementType.PORT_LEFT || entryPort == ElementType.PORT_RIGHT;
                    boolean exitIsHorizontal = exitPort == ElementType.PORT_LEFT || exitPort == ElementType.PORT_RIGHT;
                    if (entryIsHorizontal == exitIsHorizontal) {
                        exitPort = exitIsHorizontal
                            ? (dr > 0 ? ElementType.PORT_BOTTOM : ElementType.PORT_TOP)
                            : (dc > 0 ? ElementType.PORT_RIGHT : ElementType.PORT_LEFT);
                    }
                }

                int prevDc = curr[0] - prev[0];
                int prevDr = curr[1] - prev[1];
                aspect = type.aspectForRoute(entryPort, exitPort, tile.getRotation());
                if (prevDc != 0 && prevDr != 0 && type.getAspectCount() > 1 && !diagonalTurnout) {
                    int altEntry = prevDc > 0 ? ElementType.PORT_LEFT : ElementType.PORT_RIGHT;
                    int altAspect = type.aspectForRoute(altEntry, exitPort, tile.getRotation());
                    if (altAspect > aspect) {
                        aspect = altAspect;
                    }
                }
            }
            model.setElementAspect(id, aspect);
        }
    }

    public int diagonalAwarePort(int fromCol, int fromRow, int toCol, int toRow, boolean isEntry) {
        int dc = toCol - fromCol;
        int dr = toRow - fromRow;
        if (dc != 0 && dr != 0) {
            return isEntry ? (dr > 0 ? ElementType.PORT_TOP : ElementType.PORT_BOTTOM) : (dr > 0 ? ElementType.PORT_BOTTOM : ElementType.PORT_TOP);
        }
        if (dc == 1) {
            return isEntry ? ElementType.PORT_LEFT : ElementType.PORT_RIGHT;
        }
        if (dc == -1) {
            return isEntry ? ElementType.PORT_RIGHT : ElementType.PORT_LEFT;
        }
        if (dr == 1) {
            return isEntry ? ElementType.PORT_TOP : ElementType.PORT_BOTTOM;
        }
        return isEntry ? ElementType.PORT_BOTTOM : ElementType.PORT_TOP;
    }

    // --- Internal route finding ---

    private List<int[]> bfsRouteInternal(int startCol, int startRow, int endCol, int endRow, Set<String> blockedEdges, boolean allowOverride) {
        String startKey = Tile.key(startCol, startRow);
        String endKey = Tile.key(endCol, endRow);

        Deque<int[]> queue = new ArrayDeque<>();
        Map<String, int[]> cameFrom = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Map<String, int[]> entryPorts = new HashMap<>();
        Map<String, Integer> overrideCount = null;
        if (allowOverride) {
            overrideCount = new HashMap<>();
        }

        queue.add(new int[] { startCol, startRow });
        visited.add(startKey);
        cameFrom.put(startKey, null);
        entryPorts.put(startKey, new int[] { -1, -1 });

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int col = current[0];
            int row = current[1];
            String cKey = Tile.key(col, row);

            if (col == endCol && row == endRow) {
                return reconstructPath(endKey, cameFrom);
            }

            Tile tile = getTile(col, row);
            int[] cEntry = entryPorts.get(cKey);

            List<int[]> connected = getConnectedNeighbors(col, row);
            LOG.info("BFS at ({},{}) entryPorts={} neighbors={}", col, row, cEntry != null ? Arrays.toString(cEntry) : "null",
                connected.stream().map(n -> "(" + n[0] + "," + n[1] + ")").toList());
            for (int[] neighbor : connected) {
                int neighborCol = neighbor[0];
                int neighborRow = neighbor[1];
                String neighborKey = Tile.key(neighborCol, neighborRow);

                if (!tiles.containsKey(neighborKey) || blockedEdges.contains(edgeKey(col, row, neighborCol, neighborRow))) {
                    continue;
                }

                int ndc = neighborCol - col;
                int ndr = neighborRow - row;
                int exit1 = -1, exit2 = -1;
                if (ndc == 1) {
                    exit1 = ElementType.PORT_RIGHT;
                }
                else if (ndc == -1) {
                    exit1 = ElementType.PORT_LEFT;
                }
                if (ndr == 1) {
                    exit2 = ElementType.PORT_BOTTOM;
                }
                else if (ndr == -1) {
                    exit2 = ElementType.PORT_TOP;
                }

                int ne1 = -1, ne2 = -1;
                if (ndc == 1) {
                    ne1 = ElementType.PORT_LEFT;
                }
                else if (ndc == -1) {
                    ne1 = ElementType.PORT_RIGHT;
                }
                if (ndr == 1) {
                    ne2 = ElementType.PORT_TOP;
                }
                else if (ndr == -1) {
                    ne2 = ElementType.PORT_BOTTOM;
                }

                if (visited.contains(neighborKey)) {
                    if (!allowOverride) {
                        continue;
                    }
                    int ov = overrideCount.getOrDefault(neighborKey, 0);
                    if (ov >= REVISIT_CAP) {
                        continue;
                    }
                    int[] existingEntry = entryPorts.get(neighborKey);
                    if (existingEntry != null && existingEntry[0] == ne1 && existingEntry[1] == ne2) {
                        continue;
                    }
                    overrideCount.put(neighborKey, ov + 1);
                }

                boolean validThrough = true;
                if (cEntry[0] != -1 || cEntry[1] != -1) {
                    validThrough = canTraverse(tile, cEntry[0], cEntry[1], exit1, exit2);
                }

                if (!validThrough) {
                    continue;
                }

                if (!isAllowedDirection(tile, cEntry[0], cEntry[1], exit1, exit2)) {
                    continue;
                }

                entryPorts.put(neighborKey, new int[] { ne1, ne2 });
                visited.add(neighborKey);
                cameFrom.put(neighborKey, new int[] { col, row });
                queue.add(new int[] { neighborCol, neighborRow });
            }
        }

        return null;
    }

    private static List<int[]> reconstructPath(String endKey, Map<String, int[]> cameFrom) {
        List<int[]> path = new ArrayList<>();
        String cur = endKey;
        while (cur != null) {
            String[] parts = cur.split(",");
            path.add(0, new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) });
            cur = cameFrom.get(cur) != null ? Tile.key(cameFrom.get(cur)[0], cameFrom.get(cur)[1]) : null;
        }
        return path;
    }
    
    private void findAdditionalAlternatives(int startCol, int startRow, int endCol, int endRow, Set<String> baseBlock, List<int[]> altPath, List<List<int[]>> alts) {
        for (int j = 1; j < altPath.size() - 1 && alts.size() < MAX_ALTERNATIVES; j++) {
            int[] f = altPath.get(j);
            int[] t = altPath.get(j + 1);
            String ek = edgeKey(f[0], f[1], t[0], t[1]);
            if (baseBlock.contains(ek)) continue;

            Set<String> block = new HashSet<>(baseBlock);
            block.add(ek);
            List<int[]> result = bfsRouteInternal(startCol, startRow, endCol, endRow, block, false);
            if (result != null && !isDuplicatePath(alts, result)) {
                alts.add(result);
            }
        }
    }

    private boolean isDuplicatePath(List<List<int[]>> alts, List<int[]> candidate) {
        return alts.stream().anyMatch(existing ->
            existing.size() == candidate.size()
            && IntStream.range(0, existing.size()).allMatch(i ->
                existing.get(i)[0] == candidate.get(i)[0]
                && existing.get(i)[1] == candidate.get(i)[1]));
    }

    private boolean canTraverse(Tile tile, int entry1, int entry2, int exit1, int exit2) {
        if (!(tile instanceof ElementTile et)) {
            return false;
        }
        ElementType type = et.getElementType();
        int rotation = tile.getRotation();
        if (exit1 != -1) {
            if (entry1 != -1 && type.isValidThroughPath(entry1, exit1, rotation)) {
                return true;
            }
            if (entry2 != -1 && type.isValidThroughPath(entry2, exit1, rotation)) {
                return true;
            }
        }
        if (exit2 != -1) {
            if (entry1 != -1 && type.isValidThroughPath(entry1, exit2, rotation)) {
                return true;
            }
            if (entry2 != -1 && type.isValidThroughPath(entry2, exit2, rotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether traversal through a tile is allowed given its direction constraint.
     * Only applies to STRAIGHT and DIAGONAL tiles with direction != BOTH.
     * Returns true if traversal is permitted.
     */
    private boolean isAllowedDirection(Tile tile, int entry1, int entry2, int exit1, int exit2) {
        if (!(tile instanceof ElementTile et)) {
            return true;
        }
        TileDirection dir = tile.getDirection();
        if (dir == TileDirection.BOTH) {
            return true;
        }
        ElementType type = et.getElementType();
        if (type != ElementType.STRAIGHT && type != ElementType.DIAGONAL) {
            return true;
        }
        // No entry port known yet (start tile) — allow
        if (entry1 == -1 && entry2 == -1) {
            return true;
        }

        int rotSteps = (tile.getRotation() / 90) % 4;

        if (type == ElementType.STRAIGHT) {
            // Forward: LEFT→RIGHT (rotated). Entry port for forward = (LEFT + rotSteps) % 4
            int forwardEntry = (ElementType.PORT_LEFT + rotSteps) % 4;
            int actualEntry = entry1 != -1 ? entry1 : entry2;
            boolean isForward = (actualEntry == forwardEntry);
            return dir == TileDirection.FORWARD ? isForward : !isForward;
        }

        if (type == ElementType.DIAGONAL) {
            // Forward: from {LEFT, BOTTOM} corner to {TOP, RIGHT} corner (rotated)
            int fwdEntry1 = (ElementType.PORT_LEFT + rotSteps) % 4;
            int fwdEntry2 = (ElementType.PORT_BOTTOM + rotSteps) % 4;
            int actualEntry = entry1 != -1 ? entry1 : entry2;
            boolean isForward = (actualEntry == fwdEntry1 || actualEntry == fwdEntry2);
            return dir == TileDirection.FORWARD ? isForward : !isForward;
        }

        return true;
    }

    private boolean hasPhysicalPort(int col, int row, int port) {
        Tile t = getTile(col, row);
        if (t == null) return false;
        int[] ports = getPhysicalPorts(t);
        if (ports == null) return false;
        return Arrays.stream(ports).anyMatch(p -> p == port);
    }

    private int[] getPhysicalPorts(Tile tile) {
        if (tile instanceof ElementTile et) {
            return et.getElementType().getPhysicalPorts(tile.getRotation());
        }
        return null;
    }

    private Tile getTile(int col, int row) {
        return tiles.get(Tile.key(col, row));
    }

    static String edgeKey(int fromCol, int fromRow, int toCol, int toRow) {
        return fromCol + "," + fromRow + "->" + toCol + "," + toRow;
    }

    // --- Public utilities for train movement ---

    /**
     * Returns all physically connected neighbor tiles for the given coordinates.
     * This is a public wrapper around the internal neighbor finding logic.
     */
    public List<int[]> getConnectedNeighbors(int col, int row) {
        return getConnectedNeighborsInternal(col, row);
    }

    /**
     * Finds the best neighbor tile behind the given tile, relative to the tile
     * we came from (the "head side"). Used to walk backward along the physical
     * track to place train cars.
     *
     * @param grid        the tile grid for tile lookup
     * @param col         current tile column
     * @param row         current tile row
     * @param fromCol     column of the tile we came from (head side), or -1 if none
     * @param fromRow     row of the tile we came from, or -1 if none
     * @return coordinates of the backward neighbor, or null if none exists
     */
    public int[] pickBackwardNeighbor(TileGrid grid, int col, int row, int[] fromCoord) {
        List<int[]> neighbors = getConnectedNeighborsInternal(col, row);
        if (neighbors.isEmpty()) {
            return null;
        }

        // If we have a from-coord, exclude it and prefer straight-through continuation
        if (fromCoord != null) {
            int fromCol = fromCoord[0];
            int fromRow = fromCoord[1];

            // Compute entry port: the port on current tile facing the from-tile
            int dc = col - fromCol;
            int dr = row - fromRow;
            int entryPort = portFromDelta(dc, dr);

            // Filter out the from-tile
            java.util.List<int[]> candidates = new java.util.ArrayList<>();
            for (int[] n : neighbors) {
                if (n[0] == fromCol && n[1] == fromRow) {
                    continue;
                }
                candidates.add(n);
            }

            if (candidates.isEmpty()) {
                return null;
            }

            // Prefer the neighbor that forms a valid through-path with the entry port
            Tile currentTile = grid.getTile(col, row);
            if (currentTile instanceof ElementTile et) {
                int rotation = currentTile.getRotation();
                ElementType type = et.getElementType();
                for (int[] n : candidates) {
                    int ndc = n[0] - col;
                    int ndr = n[1] - row;
                    int exitPort = portFromDelta(ndc, ndr);
                    if (type.isValidThroughPath(entryPort, exitPort, rotation)) {
                        return n;
                    }
                }
            }

            // No straight-through: return first candidate
            return candidates.get(0);
        }

        // No from-coord (first step from head): just return first neighbor
        return neighbors.get(0);
    }

    /**
     * Internal method to get connected neighbors. Extracted to be called by
     * both the public wrapper and pickBackwardNeighbor.
     */
    private List<int[]> getConnectedNeighborsInternal(int col, int row) {
        List<int[]> neighbors = new ArrayList<>();
        Tile tile = getTile(col, row);
        if (tile == null) {
            return neighbors;
        }

        int[] ports = getPhysicalPorts(tile);
        if (ports == null) {
            return neighbors;
        }

        Set<Integer> portSet = new HashSet<>();
        for (int p : ports) {
            portSet.add(p);
        }

        ElementType elemType = null;
        int rotation = 0;
        if (tile instanceof ElementTile et) {
            elemType = et.getElementType();
            rotation = tile.getRotation();
        }

        if (portSet.contains(ElementType.PORT_LEFT) && col > 0 && hasPhysicalPort(col - 1, row, ElementType.PORT_RIGHT)) {
            neighbors.add(new int[] { col - 1, row });
        }
        if (portSet.contains(ElementType.PORT_TOP) && row > 0 && hasPhysicalPort(col, row - 1, ElementType.PORT_BOTTOM)) {
            neighbors.add(new int[] { col, row - 1 });
        }
        if (portSet.contains(ElementType.PORT_RIGHT) && col < cols - 1 && hasPhysicalPort(col + 1, row, ElementType.PORT_LEFT)) {
            neighbors.add(new int[] { col + 1, row });
        }
        if (portSet.contains(ElementType.PORT_BOTTOM) && row < rows - 1 && hasPhysicalPort(col, row + 1, ElementType.PORT_TOP)) {
            neighbors.add(new int[] { col, row + 1 });
        }

        if (elemType != null && (portSet.contains(ElementType.PORT_RIGHT) || portSet.contains(ElementType.PORT_BOTTOM))
            && elemType.hasValidDiagonal(ElementType.PORT_RIGHT, ElementType.PORT_BOTTOM, rotation) && col < cols - 1 && row < rows - 1
            && (hasPhysicalPort(col + 1, row + 1, ElementType.PORT_LEFT) || hasPhysicalPort(col + 1, row + 1, ElementType.PORT_TOP))) {
            LOG.info("  DR diagonal added for ({},{}) type={} rot={}", col, row, elemType, rotation);
            neighbors.add(new int[] { col + 1, row + 1 });
        }
        if (elemType != null && (portSet.contains(ElementType.PORT_LEFT) || portSet.contains(ElementType.PORT_BOTTOM))
            && elemType.hasValidDiagonal(ElementType.PORT_LEFT, ElementType.PORT_BOTTOM, rotation) && col > 0 && row < rows - 1
            && (hasPhysicalPort(col - 1, row + 1, ElementType.PORT_RIGHT) || hasPhysicalPort(col - 1, row + 1, ElementType.PORT_TOP))) {
            LOG.info("  DL diagonal added for ({},{}) type={} rot={}", col, row, elemType, rotation);
            neighbors.add(new int[] { col - 1, row + 1 });
        }
        if (elemType != null && (portSet.contains(ElementType.PORT_RIGHT) || portSet.contains(ElementType.PORT_TOP))
            && elemType.hasValidDiagonal(ElementType.PORT_RIGHT, ElementType.PORT_TOP, rotation) && col < cols - 1 && row > 0
            && (hasPhysicalPort(col + 1, row - 1, ElementType.PORT_LEFT) || hasPhysicalPort(col + 1, row - 1, ElementType.PORT_BOTTOM))) {
            LOG.info("  UR diagonal added for ({},{}) type={} rot={}", col, row, elemType, rotation);
            neighbors.add(new int[] { col + 1, row - 1 });
        }
        if (elemType != null && (portSet.contains(ElementType.PORT_LEFT) || portSet.contains(ElementType.PORT_TOP))
            && elemType.hasValidDiagonal(ElementType.PORT_LEFT, ElementType.PORT_TOP, rotation) && col > 0 && row > 0
            && (hasPhysicalPort(col - 1, row - 1, ElementType.PORT_RIGHT) || hasPhysicalPort(col - 1, row - 1, ElementType.PORT_BOTTOM))) {
            LOG.info("  UL diagonal added for ({},{}) type={} rot={}", col, row, elemType, rotation);
            neighbors.add(new int[] { col - 1, row - 1 });
        }

        return neighbors;
    }

    /**
     * Static helper to compute port from delta, matching OccupancySimulation.portFromDelta.
     */
    static int portFromDelta(int dc, int dr) {
        if (dc == 1) return ElementType.PORT_LEFT;
        if (dc == -1) return ElementType.PORT_RIGHT;
        if (dr == 1) return ElementType.PORT_TOP;
        if (dr == -1) return ElementType.PORT_BOTTOM;
        return -1;
    }
}
