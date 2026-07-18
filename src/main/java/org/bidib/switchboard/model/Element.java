package org.bidib.switchboard.model;

import com.jgoodies.binding.beans.Model;

public class Element extends Model {

    private static final long serialVersionUID = 1L;

    private final String id;

    private final long nodeId;

    private final long accessoryId;

    private int currentAspect;

    private Occupancy occupancy;

    public Element(String id, long nodeId, long accessoryId) {
        this.id = id;
        this.nodeId = nodeId;
        this.accessoryId = accessoryId;
        this.currentAspect = 0;
    }

    public String getId() {
        return id;
    }

    public long getNodeId() {
        return nodeId;
    }

    public long getAccessoryId() {
        return accessoryId;
    }

    public int getCurrentAspect() {
        return currentAspect;
    }

    public void setCurrentAspect(int currentAspect) {
        int oldValue = this.currentAspect;
        this.currentAspect = currentAspect;

        firePropertyChange("currentAspect", oldValue, this.currentAspect);
    }

    public Occupancy getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(Occupancy occupancy) {
        Occupancy oldValue = this.occupancy;
        this.occupancy = occupancy;

        firePropertyChange("occupancy", oldValue, this.occupancy);
    }
}
