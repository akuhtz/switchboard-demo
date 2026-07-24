package org.bidib.switchboard.component.persistence;

import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.Occupancy.OccupancyState;

public interface OccupancySerializer {

    void writeOccupancy(Occupancy occ, LayoutData.OccupancyData data);

    Occupancy createOccupancy(LayoutData.OccupancyData data, OccupancyState state);
}
