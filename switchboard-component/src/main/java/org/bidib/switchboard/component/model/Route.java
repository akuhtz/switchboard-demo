package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
