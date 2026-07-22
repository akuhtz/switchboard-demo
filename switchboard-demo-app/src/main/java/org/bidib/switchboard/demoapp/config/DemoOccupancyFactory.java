package org.bidib.switchboard.demoapp.config;

import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.Occupancy.OccupancyState;

public class DemoOccupancyFactory implements OccupancyFactory {

	public Occupancy create(long nodeId, int portId, OccupancyState state) {
		return Occupancy.create(nodeId, portId, state);
	}
}
