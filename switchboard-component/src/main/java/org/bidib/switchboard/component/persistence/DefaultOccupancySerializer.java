package org.bidib.switchboard.component.persistence;

import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.Occupancy.OccupancyState;

public class DefaultOccupancySerializer implements OccupancySerializer {

    @Override
    public void writeOccupancy(Occupancy occ, LayoutData.OccupancyData data) {
        data.setLength(occ.getLength());
    }

    @Override
    public Occupancy createOccupancy(LayoutData.OccupancyData data, OccupancyState state) {
        Occupancy occ = new Occupancy(state);
        occ.setLength(data.getLength());
        return occ;
    }
}
