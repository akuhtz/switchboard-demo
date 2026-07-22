package org.bidib.switchboard.component.config;

import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.Occupancy.OccupancyState;

public class TestOccupancyFactory implements OccupancyFactory {

	public Occupancy create(long nodeId, int portId, OccupancyState state) {
		return Occupancy.create(nodeId, portId, state);
	}

}
