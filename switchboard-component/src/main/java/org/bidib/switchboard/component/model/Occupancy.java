package org.bidib.switchboard.component.model;

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

    @Override
    public String toString() {
        return "Occupancy [id=" + id + ", state=" + state + "]";
    }
}
