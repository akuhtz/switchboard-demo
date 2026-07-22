package org.bidib.switchboard.demoapp.config;

import org.bidib.switchboard.component.model.Occupancy;

public class DemoOccupancy extends Occupancy {

    private static final long serialVersionUID = 1L;

    private final long nodeId;

    private final int portId;

    public DemoOccupancy(long nodeId, int portId, OccupancyState state) {
        super(state);
        this.nodeId = nodeId;
        this.portId = portId;
    }

    @Override
    public long getNodeId() {
        return nodeId;
    }

    @Override
    public int getPortId() {
        return portId;
    }

}
