package org.bidib.switchboard.component.model;

import com.jgoodies.binding.beans.Model;

public abstract class Occupancy extends Model {

    private static final long serialVersionUID = 1L;

    public enum OccupancyState {
        FREE, OCCUPIED
    }

    private OccupancyState state;

    protected Occupancy(OccupancyState state) {
        this.state = state;
    }

    public abstract long getNodeId();

    public abstract int getPortId();

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
        return "Occupancy [nodeId=" + getNodeId() + ", portId=" + getPortId() + ", state=" + state + "]";
    }

}
