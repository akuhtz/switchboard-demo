package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connected path of tiles that forms a railway block.
 *
 * <p>A block has a unique id, a user-editable name, and an ordered list of
 * tile coordinates (col, row) that form a connected path. A block never
 * contains turnout tiles.</p>
 */
public class Block {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Block.class);

    private final String id;

    private String name;

    private final List<int[]> path;

    private final List<String> predecessorIds = new ArrayList<>();

    private final List<String> successorIds = new ArrayList<>();

    private String assignedTrainId;

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

    /** Returns the IDs of blocks that precede this block. */
    public List<String> getPredecessorIds() {
        return Collections.unmodifiableList(predecessorIds);
    }

    /** Returns the IDs of blocks that follow this block. */
    public List<String> getSuccessorIds() {
        return Collections.unmodifiableList(successorIds);
    }

    public void addPredecessor(String id) {
        if (!predecessorIds.contains(id)) {
            predecessorIds.add(id);
        }
    }

    public void addSuccessor(String id) {
        if (!successorIds.contains(id)) {
            successorIds.add(id);
        }
    }

    public void removePredecessor(String id) {
        predecessorIds.remove(id);
    }

    public void removeSuccessor(String id) {
        successorIds.remove(id);
    }

    /** Returns the ID of the train assigned to this block, or null. */
    public String getAssignedTrainId() {
        return assignedTrainId;
    }

    /** Assigns a train to this block. */
    public void setAssignedTrainId(String trainId) {
		LOGGER.info("Set assigned train in block: {}, trainId: {}", this.name, trainId);
        this.assignedTrainId = trainId;
    }

    /** Clears the assigned train from this block. */
    public void clearAssignedTrain() {
    		LOGGER.info("Clear train from block: {}, assignedTrainId: {}", this.name, this.assignedTrainId);
        this.assignedTrainId = null;
    }

    public boolean containsTile(int col, int row) {
        return path.stream().anyMatch(p -> p[0] == col && p[1] == row);
    }

    public int size() {
        return path.size();
    }
}
