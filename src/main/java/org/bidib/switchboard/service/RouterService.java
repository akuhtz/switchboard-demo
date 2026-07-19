package org.bidib.switchboard.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bidib.switchboard.model.ElementTile;
import org.bidib.switchboard.model.ElementType;
import org.bidib.switchboard.model.RailwayModel;
import org.bidib.switchboard.model.Route;
import org.bidib.switchboard.model.RouteModel;
import org.bidib.switchboard.model.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouterService {

    private static final Logger LOG = LoggerFactory.getLogger(RouterService.class);

    public static final int MAX_ALTERNATIVES = 10;

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

    public Map<String, Route> getRoutes() {
        return routeModel.getRoutes();
    }

    public List<int[]> bfsRoute(int startCol, int startRow, int endCol, int endRow) {
        String startKey = tileKey(startCol, startRow);
        String endKey = tileKey(endCol, endRow);
        if (!tiles.containsKey(startKey) || !tiles.containsKey(endKey)) {
            return null;
        }
        return bfsRouteInternal(startCol, startRow, endCol, endRow, new HashSet<>());
    }

    public List<List<int[]>> bfsAlternativeRoutes(int startCol, int startRow, int endCol, int endRow, List<int[]> primaryPath) {
        return bfsAlternativeRoutes(startCol, startRow, endCol, endRow, primaryPath, false);
    }

    public List<List<int[]>> bfsAlternativeRoutes(int startCol, int startRow, int endCol, int endRow, List<int[]> primaryPath, boolean exhaustive) {
        String startKey = tileKey(startCol, startRow);
        String endKey = tileKey(endCol, endRow);
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
                List<int[]> result = bfsRouteInternal(startCol, startRow, endCol, endRow, block);
                if (result != null) {
                    if (!isDuplicatePath(alts, result)) {
                        alts.add(result);
                        if (exhaustive) {
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
                aspect = type.aspectForRoute(entryPort, exitPort, tile.getRotation());
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

    private List<int[]> bfsRouteInternal(int startCol, int startRow, int endCol, int endRow, Set<String> blockedEdges) {
        String startKey = tileKey(startCol, startRow);
        String endKey = tileKey(endCol, endRow);

        Deque<int[]> queue = new ArrayDeque<>();
        Map<String, int[]> cameFrom = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Map<String, int[]> entryPorts = new HashMap<>();

        queue.add(new int[] { startCol, startRow });
        visited.add(startKey);
        cameFrom.put(startKey, null);
        entryPorts.put(startKey, new int[] { -1, -1 });

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int c = current[0];
            int r = current[1];
            String cKey = tileKey(c, r);

            if (c == endCol && r == endRow) {
                List<int[]> path = new ArrayList<>();
                String cur = endKey;
                while (cur != null) {
                    String[] parts = cur.split(",");
                    path.add(0, new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) });
                    cur = cameFrom.get(cur) != null ? tileKey(cameFrom.get(cur)[0], cameFrom.get(cur)[1]) : null;
                }
                return path;
            }

            Tile tile = getTile(c, r);
            int[] cEntry = entryPorts.get(cKey);

            List<int[]> connected = getConnectedNeighbors(c, r);
            LOG.info("BFS at ({},{}) entryPorts={} neighbors={}", c, r, cEntry != null ? Arrays.toString(cEntry) : "null",
                connected.stream().map(n -> "(" + n[0] + "," + n[1] + ")").toList());
            for (int[] neighbor : connected) {
                int nc = neighbor[0];
                int nr = neighbor[1];
                String nKey = tileKey(nc, nr);

                if (visited.contains(nKey) || !tiles.containsKey(nKey) || routeModel.isTileReserved(nc, nr, null) || blockedEdges.contains(edgeKey(c, r, nc, nr))) {
                    continue;
                }

                int ndc = nc - c;
                int ndr = nr - r;
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

                boolean validThrough = true;
                if (cEntry[0] != -1) {
                    validThrough = canTraverse(tile, cEntry[0], cEntry[1], exit1, exit2);
                }

                if (!validThrough) {
                    continue;
                }

                int dc = nc - c;
                int dr = nr - r;
                int ne1 = -1, ne2 = -1;
                if (dc == 1) {
                    ne1 = ElementType.PORT_LEFT;
                }
                else if (dc == -1) {
                    ne1 = ElementType.PORT_RIGHT;
                }
                if (dr == 1) {
                    ne2 = ElementType.PORT_TOP;
                }
                else if (dr == -1) {
                    ne2 = ElementType.PORT_BOTTOM;
                }

                entryPorts.put(nKey, new int[] { ne1, ne2 });
                visited.add(nKey);
                cameFrom.put(nKey, new int[] { c, r });
                queue.add(new int[] { nc, nr });
            }
        }

        return null;
    }

    private void findAdditionalAlternatives(int startCol, int startRow, int endCol, int endRow, Set<String> baseBlock, List<int[]> altPath, List<List<int[]>> alts) {
        for (int j = 1; j < altPath.size() - 1 && alts.size() < MAX_ALTERNATIVES; j++) {
            int[] f = altPath.get(j);
            int[] t = altPath.get(j + 1);
            String ek = edgeKey(f[0], f[1], t[0], t[1]);
            if (baseBlock.contains(ek)) continue;

            Set<String> block = new HashSet<>(baseBlock);
            block.add(ek);
            List<int[]> result = bfsRouteInternal(startCol, startRow, endCol, endRow, block);
            if (result != null && !isDuplicatePath(alts, result)) {
                alts.add(result);
            }
        }
    }

    private boolean isDuplicatePath(List<List<int[]>> alts, List<int[]> candidate) {
        for (List<int[]> existing : alts) {
            if (existing.size() == candidate.size()) {
                boolean same = true;
                for (int j = 0; j < existing.size(); j++) {
                    if (existing.get(j)[0] != candidate.get(j)[0] || existing.get(j)[1] != candidate.get(j)[1]) {
                        same = false;
                        break;
                    }
                }
                if (same) return true;
            }
        }
        return false;
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

    private List<int[]> getConnectedNeighbors(int col, int row) {
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

    private boolean hasPhysicalPort(int col, int row, int port) {
        Tile t = getTile(col, row);
        if (t == null) {
            return false;
        }
        int[] ports = getPhysicalPorts(t);
        if (ports == null) {
            return false;
        }
        for (int p : ports) {
            if (p == port) {
                return true;
            }
        }
        return false;
    }

    private int[] getPhysicalPorts(Tile tile) {
        if (tile instanceof ElementTile et) {
            return et.getElementType().getPhysicalPorts(tile.getRotation());
        }
        return null;
    }

    private Tile getTile(int col, int row) {
        return tiles.get(tileKey(col, row));
    }

    static String tileKey(int col, int row) {
        return col + "," + row;
    }

    static String edgeKey(int fromCol, int fromRow, int toCol, int toRow) {
        return fromCol + "," + fromRow + "->" + toCol + "," + toRow;
    }
}
