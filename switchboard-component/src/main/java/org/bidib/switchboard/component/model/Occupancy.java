package org.bidib.switchboard.component.model;

import com.jgoodies.binding.beans.Model;

public class Occupancy extends Model {

    private static final long serialVersionUID = 1L;

    public enum OccupancyState {
        FREE, OCCUPIED
    }

    private final long nodeId;

    private final int portId;

    private OccupancyState state;

    private Occupancy(long nodeId, int portId) {
        this.nodeId = nodeId;
        this.portId = portId;
        this.state = OccupancyState.FREE;
    }

    private Occupancy(long nodeId, int portId, OccupancyState state) {
        this.nodeId = nodeId;
        this.portId = portId;
        this.state = state;
    }

    public static Occupancy create(long nodeId, int portId) {
        return new Occupancy(nodeId, portId);
    }

    public static Occupancy create(long nodeId, int portId, OccupancyState state) {
        return new Occupancy(nodeId, portId, state);
    }

    public long getNodeId() {
        return nodeId;
    }

    public int getPortId() {
        return portId;
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
        return "Occupancy [nodeId=" + nodeId + ", portId=" + portId + ", state=" + state + "]";
    }

}
