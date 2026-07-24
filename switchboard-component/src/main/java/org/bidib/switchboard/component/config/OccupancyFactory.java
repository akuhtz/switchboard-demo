package org.bidib.switchboard.component.config;

import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.Occupancy.OccupancyState;

public interface OccupancyFactory {

    Occupancy create(OccupancyState state);
}
