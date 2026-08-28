package org.bidib.switchboard.component.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.jgoodies.binding.beans.Model;

public class Occupancy extends Model {

    private static final long serialVersionUID = 1L;

    private static final AtomicLong nextId = new AtomicLong(1);

    public enum OccupancyState {
        FREE, OCCUPIED
    }

    private String id;

    private OccupancyState state;

    private final Set<String> occupantTrainIds = new LinkedHashSet<>();

    public Occupancy(OccupancyState state) {
        this.id = "occ-" + nextId.getAndIncrement();
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OccupancyState getState() {
        return state;
    }

    public void setState(OccupancyState state) {
        OccupancyState oldValue = this.state;
        this.state = state;
        firePropertyChange("state", oldValue, this.state);
    }

    /** Returns the IDs of all trains currently occupying this tile. */
    public Set<String> getOccupantTrainIds() {
        return Collections.unmodifiableSet(occupantTrainIds);
    }

    /** Returns true if the given train is occupying this tile. */
    public boolean isOccupiedBy(String trainId) {
        return occupantTrainIds.contains(trainId);
    }

    /** Adds a train as an occupant. Sets state to OCCUPIED. */
    public void addOccupant(String trainId) {
        occupantTrainIds.add(trainId);
        setState(OccupancyState.OCCUPIED);
    }

    /** Removes a train occupant. Sets state to FREE only if no trains remain. */
    public void removeOccupant(String trainId) {
        occupantTrainIds.remove(trainId);
        if (occupantTrainIds.isEmpty()) {
            setState(OccupancyState.FREE);
        }
    }

    /** Returns true if any train is occupying this tile. */
    public boolean hasOccupants() {
        return !occupantTrainIds.isEmpty();
    }

    @Override
    public String toString() {
        return "Occupancy [id=" + id + ", state=" + state + ", trains=" + occupantTrainIds + "]";
    }
}
