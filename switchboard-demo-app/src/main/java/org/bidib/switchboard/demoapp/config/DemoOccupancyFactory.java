package org.bidib.switchboard.demoapp.config;

import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.model.Occupancy.OccupancyState;

public class DemoOccupancyFactory implements OccupancyFactory {

    @Override
    public DemoOccupancy create(OccupancyState state) {
        return new DemoOccupancy(0, 0, state);
    }

    public DemoOccupancy create(long nodeId, int portId, OccupancyState state) {
        return new DemoOccupancy(nodeId, portId, state);
    }
}
