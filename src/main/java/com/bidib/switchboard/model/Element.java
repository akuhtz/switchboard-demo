package com.bidib.switchboard.model;

public class Element {

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
        this.currentAspect = currentAspect;
    }

    public Occupancy getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(Occupancy occupancy) {
        this.occupancy = occupancy;
    }
}
