package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A connected path of tiles that forms a railway block.
 *
 * <p>A block has a unique id, a user-editable name, and an ordered list of
 * tile coordinates (col, row) that form a connected path. A block never
 * contains turnout tiles.</p>
 */
public class Block {

    private final String id;

    private String name;

    private final List<int[]> path;

    public Block(String id, String name, List<int[]> path) {
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

    /** Returns the ordered path of [col, row] tile coordinates. */
    public List<int[]> getPath() {
        return Collections.unmodifiableList(path);
    }

    public boolean containsTile(int col, int row) {
        return path.stream().anyMatch(p -> p[0] == col && p[1] == row);
    }

    public int size() {
        return path.size();
    }
}
