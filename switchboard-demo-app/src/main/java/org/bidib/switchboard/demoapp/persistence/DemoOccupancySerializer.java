package org.bidib.switchboard.demoapp.persistence;

import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.Occupancy.OccupancyState;
import org.bidib.switchboard.component.persistence.LayoutData;
import org.bidib.switchboard.component.persistence.OccupancySerializer;
import org.bidib.switchboard.demoapp.config.DemoOccupancy;

public class DemoOccupancySerializer implements OccupancySerializer {

    @Override
    public void writeOccupancy(Occupancy occ, LayoutData.OccupancyData data) {
        if (occ instanceof DemoOccupancy d) {
            data.setNodeId(d.getNodeId());
            data.setPortId(d.getPortId());
        }
    }

    @Override
    public Occupancy createOccupancy(LayoutData.OccupancyData data, OccupancyState state) {
        return new DemoOccupancy(data.getNodeId(), data.getPortId(), state);
    }
}
