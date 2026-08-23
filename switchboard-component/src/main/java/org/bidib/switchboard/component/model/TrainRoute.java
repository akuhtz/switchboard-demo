package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrainRoute {

    private final String id;

    private String name;

    private final List<int[]> path;

    private final List<StationStop> stops = new ArrayList<>();

    public TrainRoute(String id, String name, List<int[]> path) {
        this.id = id;
        this.name = name;
        this.path = new ArrayList<>(path);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        // Replace existing stop at same index
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

    public boolean containsTile(int col, int row) {
        for (int[] p : path) {
            if (p[0] == col && p[1] == row) {
                return true;
            }
        }
        return false;
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
