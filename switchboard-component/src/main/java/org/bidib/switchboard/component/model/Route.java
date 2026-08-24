package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Route {

    private final String id;
    private final String name;
    private final String sourceElementId;
    private final String targetElementId;
    private final List<int[]> path;
    private final List<StationStop> stops = new ArrayList<>();

    public Route(String name, String sourceElementId, String targetElementId, List<int[]> path) {
        this.id = sourceElementId + "-" + targetElementId;
        this.name = name;
        this.sourceElementId = sourceElementId;
        this.targetElementId = targetElementId;
        this.path = new ArrayList<>(path);
    }

    public Route(String id, String name, String sourceElementId, String targetElementId, List<int[]> path) {
        this.id = id;
        this.name = name;
        this.sourceElementId = sourceElementId;
        this.targetElementId = targetElementId;
        this.path = new ArrayList<>(path);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSourceElementId() {
        return sourceElementId;
    }

    public String getTargetElementId() {
        return targetElementId;
    }

    public List<int[]> getPath() {
        return Collections.unmodifiableList(path);
    }

    public List<StationStop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    public void addStop(int pathIndex, int dwellTimeMs) {
        if (pathIndex < 0 || pathIndex >= path.size()) {
            throw new IndexOutOfBoundsException("pathIndex " + pathIndex + " out of range [0," + path.size() + ")");
        }
        if (dwellTimeMs <= 0) {
            throw new IllegalArgumentException("dwellTimeMs must be positive");
        }
        stops.removeIf(s -> s.getPathIndex() == pathIndex);
        stops.add(new StationStop(pathIndex, dwellTimeMs));
        stops.sort((a, b) -> Integer.compare(a.getPathIndex(), b.getPathIndex()));
    }

    public void removeStop(int pathIndex) {
        stops.removeIf(s -> s.getPathIndex() == pathIndex);
    }

    public StationStop getStopAt(int pathIndex) {
        return stops.stream().filter(s -> s.getPathIndex() == pathIndex).findFirst().orElse(null);
    }

    public boolean hasStops() {
        return !stops.isEmpty();
    }

    public boolean containsTile(int col, int row) {
        for (int[] p : path) {
            if (p[0] == col && p[1] == row) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shifts path coordinates that match keys in the given map by (dCol, dRow).
     * Used when tiles are moved to update route paths accordingly.
     *
     * @param coordMap map of "oldCol,oldRow" -> "newCol,newRow" for moved tiles
     * @param dCol column delta
     * @param dRow row delta
     */
    public void shiftPath(Map<String, String> coordMap, int dCol, int dRow) {
        for (int i = 0; i < path.size(); i++) {
            int[] p = path.get(i);
            String key = p[0] + "," + p[1];
            if (coordMap.containsKey(key)) {
                path.set(i, new int[] { p[0] + dCol, p[1] + dRow });
            }
        }
    }

    /**
     * Replaces the entire path with the given coordinates.
     * Used by MoveTilesCommand.undo() to restore original route paths.
     */
    public void replacePath(List<int[]> newPath) {
        path.clear();
        for (int[] p : newPath) {
            path.add(new int[] { p[0], p[1] });
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public static class StationStop {

        private final int pathIndex;
        private final int dwellTimeMs;

        public StationStop(int pathIndex, int dwellTimeMs) {
            this.pathIndex = pathIndex;
            this.dwellTimeMs = dwellTimeMs;
        }

        public int getPathIndex() {
            return pathIndex;
        }

        public int getDwellTimeMs() {
            return dwellTimeMs;
        }
    }
}
