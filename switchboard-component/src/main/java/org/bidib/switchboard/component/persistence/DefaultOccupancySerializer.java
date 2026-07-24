package org.bidib.switchboard.component.persistence;

import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.Occupancy.OccupancyState;

public class DefaultOccupancySerializer implements OccupancySerializer {

    @Override
    public void writeOccupancy(Occupancy occ, LayoutData.OccupancyData data) {
    }

    @Override
    public Occupancy createOccupancy(LayoutData.OccupancyData data, OccupancyState state) {
        return new Occupancy(state);
    }
}
