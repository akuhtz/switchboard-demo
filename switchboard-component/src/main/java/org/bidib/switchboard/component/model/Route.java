package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Route {

    private final String id;
    private final String sourceElementId;
    private final String targetElementId;
    private final List<int[]> path;

    public Route(String sourceElementId, String targetElementId, List<int[]> path) {
        this.id = sourceElementId + "-" + targetElementId;
        this.sourceElementId = sourceElementId;
        this.targetElementId = targetElementId;
        this.path = new ArrayList<>(path);
    }

    public Route(String id, String sourceElementId, String targetElementId, List<int[]> path) {
        this.id = id;
        this.sourceElementId = sourceElementId;
        this.targetElementId = targetElementId;
        this.path = new ArrayList<>(path);
    }

    public String getId() {
        return id;
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
}
