package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connected path of tiles that forms a railway block.
 *
 * <p>A block has a unique id, a user-editable name, and an ordered list of
 * tile coordinates (col, row) that form a connected path. A block never
 * contains turnout tiles.</p>
 *
 * <p>Multiple trains can be assigned to the same block simultaneously (e.g.
 * when trains are coupled). The UI typically assigns one train per block,
 * while the simulation may add additional assignments.</p>
 */
public class Block {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Block.class);

    private final String id;

    private String name;

    private final List<int[]> path;

    private final List<String> predecessorIds = new ArrayList<>();

    private final List<String> successorIds = new ArrayList<>();

    private final Set<String> assignedTrainIds = new LinkedHashSet<>();

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

    /** Returns the IDs of all trains assigned to this block. */
    public Set<String> getAssignedTrainIds() {
        return Collections.unmodifiableSet(assignedTrainIds);
    }

    /** Returns the ID of the first (primary) train assigned to this block, or null. */
    public String getAssignedTrainId() {
        return assignedTrainIds.isEmpty() ? null : assignedTrainIds.iterator().next();
    }

    /** Returns true if the given train is assigned to this block. */
    public boolean isAssignedTo(String trainId) {
        return assignedTrainIds.contains(trainId);
    }

    /** Returns true if any train is assigned to this block. */
    public boolean isReserved() {
        return !assignedTrainIds.isEmpty();
    }

    /** Adds a train assignment to this block. */
    public void addAssignedTrain(String trainId) {
		LOGGER.trace("Add assigned train to block: {}, trainId: {}", this.name, trainId);
        assignedTrainIds.add(trainId);
    }

    /**
     * @deprecated Use {@link #addAssignedTrain(String)} instead.
     * Assigns a single train to this block, replacing any existing assignments.
     */
    @Deprecated
    public void setAssignedTrainId(String trainId) {
		LOGGER.trace("Set assigned train in block: {}, trainId: {}", this.name, trainId);
        assignedTrainIds.clear();
        if (trainId != null) {
            assignedTrainIds.add(trainId);
        }
    }

    /** Removes a specific train assignment from this block. */
    public void removeAssignedTrain(String trainId) {
		LOGGER.trace("Remove assigned train from block: {}, trainId: {}", this.name, trainId);
        assignedTrainIds.remove(trainId);
    }

    /** Clears all train assignments from this block. */
    public void clearAssignedTrains() {
		LOGGER.trace("Clear all assigned trains from block: {}, trainIds: {}", this.name, assignedTrainIds);
        assignedTrainIds.clear();
    }

    /** @deprecated Use {@link #clearAssignedTrains()} instead. */
    @Deprecated
    public void clearAssignedTrain() {
        clearAssignedTrains();
    }

    public boolean containsTile(int col, int row) {
        return path.stream().anyMatch(p -> p[0] == col && p[1] == row);
    }

    public int size() {
        return path.size();
    }
}
